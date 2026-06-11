package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-012: Reward Claims (Test + Docs Loop).
 *
 * Full coverage for TownPaymentBoard (addReward guards + 7 d force expiry + 100-cap trim,
 * getUnclaimedRewards, claimReward toBuffer vs inventory paths + buffer space semantics,
 * cleanupExpiredRewards mark+30 d prune, getStats, getters, NBT) plus integration with
 * RewardEntry eligibility/expiry and SlotBasedStorage 18-slot buffer.
 *
 * McBootstrap + TestRegistryHelper double (pattern from T-002/T-007) unblocks all ItemStack
 * and registry-dependent paths (NBT, buffer, display names).
 *
 * Two @Disabled tests document real production bugs discovered while covering the toBuffer
 * claim path (partial commits on failure + silent loss on single-stack partial fits).
 * Per protocol these are not fixed; ledger row is marked BUG-FOUND.
 *
 * Documentation: vault/Town/Payment Board/Reward Claims.md
 */
class TownPaymentBoardTest {

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000L;
    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000L;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    private TownPaymentBoard board;
    private RegistryHelper savedRegistry;

    @BeforeEach
    void setUp() {
        board = new TownPaymentBoard();
        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new TestRegistryHelper();
    }

    @AfterEach
    void tearDown() {
        PlatformAccess.registry = savedRegistry;
    }

    // --- test double (delegates item lookups to post-bootstrap BuiltInRegistries) ---

    private static class TestRegistryHelper implements RegistryHelper {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.Block> java.util.function.Supplier<T> registerBlock(String name, java.util.function.Supplier<T> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.item.Item> java.util.function.Supplier<T> registerBlockItem(String name, java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.entity.EntityType<?>> java.util.function.Supplier<T> registerEntityType(String name, java.util.function.Supplier<T> entityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.entity.BlockEntityType<?>> java.util.function.Supplier<T> registerBlockEntityType(String name, java.util.function.Supplier<T> blockEntityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.MenuType<?>> java.util.function.Supplier<T> registerMenuType(String name, java.util.function.Supplier<T> menuType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.AbstractContainerMenu> java.util.function.Supplier<net.minecraft.world.inventory.MenuType<T>> registerExtendedMenuType(String name, MenuFactory<T> factory) {
            return () -> null;
        }

        @Override
        public Item getItem(ResourceLocation location) {
            return BuiltInRegistries.ITEM.get(location);
        }

        @Override
        public ResourceLocation getItemKey(Item item) {
            return BuiltInRegistries.ITEM.getKey(item);
        }

        @Override
        public Iterable<Item> getItems() {
            return List.of();
        }
    }

    // --- reflection helper to exercise trim/prune edge cases deterministically ---

    @SuppressWarnings("unchecked")
    private List<RewardEntry> getInternalRewards(TownPaymentBoard b) throws Exception {
        Field f = TownPaymentBoard.class.getDeclaredField("rewards");
        f.setAccessible(true);
        return (List<RewardEntry>) f.get(b);
    }

    // --- addReward guards ---

    @Test
    void addReward_nullList_returnsNull_andDoesNotGrow() {
        int before = board.getAllRewards().size();
        UUID id = board.addReward(RewardSource.TOURIST_ARRIVAL, null, "ALL");
        assertNull(id);
        assertEquals(before, board.getAllRewards().size());
    }

    @Test
    void addReward_emptyList_returnsNull_andDoesNotGrow() {
        int before = board.getAllRewards().size();
        UUID id = board.addReward(RewardSource.MILESTONE, List.of(), "ALL");
        assertNull(id);
        assertEquals(before, board.getAllRewards().size());
    }

    // --- addReward success + 7 d forced expiry (overriding RewardEntry 24 h default) ---

    @Test
    void addReward_nonEmpty_forcesSevenDayExpiry_andReturnsId() {
        long before = System.currentTimeMillis();
        List<ItemStack> reward = List.of(new ItemStack(Items.EMERALD, 12), new ItemStack(Items.DIAMOND, 1));
        UUID id = board.addReward(RewardSource.TOURIST_ARRIVAL, reward, "ALL");
        assertNotNull(id);

        Optional<RewardEntry> entry = board.getRewardById(id);
        assertTrue(entry.isPresent());
        long delta = entry.get().getExpirationTime() - before;
        // Hand-computed: board always does now + 7 d regardless of the 24 h written inside RewardEntry ctor
        assertTrue(delta >= SEVEN_DAYS_MS - 2000 && delta <= SEVEN_DAYS_MS + 2000,
                "Expected ~7 d forced expiry, got delta=" + delta);
        assertEquals(ClaimStatus.UNCLAIMED, entry.get().getStatus());
        assertEquals("ALL", entry.get().getEligibility());
        assertEquals(2, entry.get().getRewards().size());
    }

    // --- getUnclaimedRewards (cleans, filters, newest-first) ---

    @Test
    void getUnclaimedRewards_filtersToUnclaimedNonExpired_andNewestFirst() throws Exception {
        UUID id1 = board.addReward(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        UUID id2 = board.addReward(RewardSource.MILESTONE, List.of(new ItemStack(Items.GOLD_INGOT, 1)), "ALL");

        // Make id1 "old" by touching its timestamp via internal list (ts is final; we re-create scenario via add order)
        // Instead: expire id1 via setter after add
        board.getRewardById(id1).ifPresent(e -> e.setExpirationTime(System.currentTimeMillis() - 1));

        List<RewardEntry> unclaimed = board.getUnclaimedRewards();
        assertEquals(1, unclaimed.size());
        assertEquals(id2, unclaimed.get(0).getId()); // id2 is the only valid unclaimed
    }

    // --- claimReward paths ---

    @Test
    void claimReward_notFound_returnsFailureMessage() {
        TownPaymentBoard.ClaimResult r = board.claimReward(UUID.randomUUID(), "anyone", true);
        assertFalse(r.isSuccess());
        assertEquals("Reward not found", r.getMessage());
    }

    @Test
    void claimReward_alreadyClaimed_returnsAlreadyClaimed() {
        UUID id = board.addReward(RewardSource.TRADE, List.of(new ItemStack(Items.EMERALD, 2)), "ALL");
        board.getRewardById(id).ifPresent(e -> e.setStatus(ClaimStatus.CLAIMED));

        TownPaymentBoard.ClaimResult r = board.claimReward(id, "anyone", true);
        assertFalse(r.isSuccess());
        assertTrue(r.getMessage().contains("already claimed"));
    }

    @Test
    void claimReward_expired_returnsExpired() {
        UUID id = board.addReward(RewardSource.OTHER, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        board.getRewardById(id).ifPresent(e -> e.setExpirationTime(System.currentTimeMillis() - 1));

        TownPaymentBoard.ClaimResult r = board.claimReward(id, "anyone", true);
        assertFalse(r.isSuccess());
        assertTrue(r.getMessage().contains("expired"));
    }

    @Test
    void claimReward_wrongPlayerForPersonal_returnsNotEligible() {
        UUID id = board.addReward(RewardSource.COURIER_DELIVERY, List.of(new ItemStack(Items.IRON_INGOT, 3)), "p-uuid-specific");
        TownPaymentBoard.ClaimResult r = board.claimReward(id, "someone-else", true);
        assertFalse(r.isSuccess());
        assertTrue(r.getMessage().contains("not eligible"));
    }

    @Test
    void claimReward_toBuffer_success_whenSpaceAvailable_marksClaimed_andItemsInBuffer() {
        List<ItemStack> reward = List.of(new ItemStack(Items.EMERALD, 4));
        UUID id = board.addReward(RewardSource.TOURIST_ARRIVAL, reward, "ALL");

        TownPaymentBoard.ClaimResult r = board.claimReward(id, "claimer-1", true);
        assertTrue(r.isSuccess());
        assertEquals("Claimed to buffer storage", r.getMessage());
        assertEquals(1, r.getClaimedItems().size());
        assertEquals(4, r.getClaimedItems().get(0).getCount());

        // Entry now claimed
        assertEquals(ClaimStatus.CLAIMED, board.getRewardById(id).get().getStatus());
        // Buffer received it
        assertEquals(4, board.getBufferStorageSlots().getTotalCount(Items.EMERALD));
    }

    @Test
    void claimReward_toBuffer_failsWithFullMessage_whenNoRoomForReward_andStatusUnchanged() {
        // Fill all 18 slots with stone (different item, 1 each) so no room and no stacking match for emeralds
        for (int i = 0; i < 18; i++) {
            board.getBufferStorageSlots().setSlot(i, new ItemStack(Items.STONE, 1));
        }
        assertEquals(18, board.getBufferStorageSlots().getSlotCount());

        UUID id = board.addReward(RewardSource.MILESTONE, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        assertEquals(ClaimStatus.UNCLAIMED, board.getRewardById(id).get().getStatus());

        TownPaymentBoard.ClaimResult r = board.claimReward(id, "anyone", true);
        assertFalse(r.isSuccess());
        assertEquals("Buffer storage is full", r.getMessage());
        // Status must be unchanged
        assertEquals(ClaimStatus.UNCLAIMED, board.getRewardById(id).get().getStatus());
        // Buffer still only stones
        assertEquals(0, board.getBufferStorageSlots().getTotalCount(Items.EMERALD));
        assertEquals(18, board.getBufferStorageSlots().getTotalCount(Items.STONE));
    }

    @Test
    void claimReward_notToBuffer_alwaysMarksClaimed_evenIfCallerWouldOverflow() {
        UUID id = board.addReward(RewardSource.JOB_COMPLETION, List.of(new ItemStack(Items.BREAD, 2)), "player-x");
        TownPaymentBoard.ClaimResult r = board.claimReward(id, "player-x", false);
        assertTrue(r.isSuccess());
        assertEquals(ClaimStatus.CLAIMED, board.getRewardById(id).get().getStatus());
        // Items are returned for the caller to place (we don't simulate player inv here)
        assertEquals(2, r.getClaimedItems().get(0).getCount());
    }

    // --- cleanupExpiredRewards: mark + 30 d prune (by creation ts) ---

    @Test
    void cleanupExpiredRewards_marksQualifyingEntriesExpired_butDoesNotPruneRecentOnes() {
        UUID id = board.addReward(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        board.getRewardById(id).ifPresent(e -> e.setExpirationTime(System.currentTimeMillis() - 1));

        board.cleanupExpiredRewards();

        RewardEntry e = board.getRewardById(id).get();
        assertEquals(ClaimStatus.EXPIRED, e.getStatus());
        // Still present (not 30 d old by ts)
        assertEquals(1, board.getAllRewards().size());
    }

    @Test
    void cleanupExpiredRewards_prunesExpiredEntriesOlderThan30DaysByCreationTimestamp() throws Exception {
        // Use fromNetwork + internal list injection so we control creation ts precisely
        UUID oldId = UUID.randomUUID();
        long oldTs = System.currentTimeMillis() - THIRTY_DAYS_MS - 1000; // just over 30 d
        long farFutureExp = System.currentTimeMillis() + SEVEN_DAYS_MS;

        RewardEntry oldExpired = RewardEntry.fromNetwork(
                oldId, oldTs, farFutureExp,
                RewardSource.OTHER, List.of(new ItemStack(Items.EMERALD, 1)),
                ClaimStatus.EXPIRED, "ALL"
        );

        List<RewardEntry> internal = getInternalRewards(board);
        internal.add(oldExpired);
        assertEquals(1, board.getAllRewards().size());

        board.cleanupExpiredRewards();

        // The old EXPIRED by creation ts must be pruned
        assertEquals(0, board.getAllRewards().size());
        assertFalse(board.getRewardById(oldId).isPresent());
    }

    // --- MAX_REWARDS trim (100 cap, oldest-first after cleanup) ---

    @Test
    void addReward_whenOver100_trimsTo100_oldestFirst() throws Exception {
        // Add 100 normal rewards
        List<UUID> firstHundred = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID id = board.addReward(RewardSource.OTHER, List.of(new ItemStack(Items.STONE, 1)), "ALL");
            firstHundred.add(id);
        }
        assertEquals(100, board.getAllRewards().size());

        // Add 101st — triggers trim path
        UUID newest = board.addReward(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 5)), "ALL");
        assertNotNull(newest);
        assertEquals(100, board.getAllRewards().size());

        // The newest must be present
        assertTrue(board.getRewardById(newest).isPresent());

        // To deterministically assert "oldest dropped", inject one with a distinctly older ts
        UUID veryOld = UUID.randomUUID();
        long veryOldTs = 1000L;
        RewardEntry ancient = RewardEntry.fromNetwork(
                veryOld, veryOldTs, veryOldTs + SEVEN_DAYS_MS,
                RewardSource.OTHER, List.of(new ItemStack(Items.DIAMOND, 1)),
                ClaimStatus.UNCLAIMED, "ALL"
        );
        List<RewardEntry> internal = getInternalRewards(board);
        internal.add(ancient);
        // Now manually exceed and let add trigger? Or call add which will see 101 and trim
        UUID trigger = board.addReward(RewardSource.OTHER, List.of(new ItemStack(Items.BREAD, 1)), "ALL");
        assertEquals(100, board.getAllRewards().size());
        // The ancient (lowest ts) should have been the one removed by the oldest-first logic
        assertFalse(board.getRewardById(veryOld).isPresent());
        assertTrue(board.getRewardById(trigger).isPresent());
    }

    // --- getStats (after cleanup) ---

    @Test
    void getStats_countsByStatus_afterCleanup() {
        UUID u1 = board.addReward(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        UUID u2 = board.addReward(RewardSource.MILESTONE, List.of(new ItemStack(Items.GOLD_INGOT, 1)), "ALL");
        UUID c1 = board.addReward(RewardSource.TRADE, List.of(new ItemStack(Items.IRON_INGOT, 1)), "ALL");
        board.getRewardById(c1).ifPresent(e -> e.setStatus(ClaimStatus.CLAIMED));
        UUID e1 = board.addReward(RewardSource.OTHER, List.of(new ItemStack(Items.BREAD, 1)), "ALL");
        board.getRewardById(e1).ifPresent(e -> e.setExpirationTime(System.currentTimeMillis() - 1));

        TownPaymentBoard.PaymentBoardStats stats = board.getStats();
        assertEquals(2, stats.getUnclaimedCount()); // u1, u2 (e1 cleaned to expired)
        assertEquals(1, stats.getClaimedCount());
        assertEquals(1, stats.getExpiredCount());
        assertEquals(4, stats.getTotalCount());
    }

    // --- getAllRewards / getRewardsBySource do not clean ---

    @Test
    void getAllRewards_includesExpiredAndClaimed_andNewestFirst() {
        UUID id = board.addReward(RewardSource.OTHER, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        board.getRewardById(id).ifPresent(e -> e.setStatus(ClaimStatus.EXPIRED));

        List<RewardEntry> all = board.getAllRewards();
        assertEquals(1, all.size());
        assertEquals(ClaimStatus.EXPIRED, all.get(0).getStatus());
    }

    @Test
    void getRewardsBySource_filtersAndSortsNewestFirst() {
        board.addReward(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        board.addReward(RewardSource.MILESTONE, List.of(new ItemStack(Items.GOLD_INGOT, 1)), "ALL");
        board.addReward(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.DIAMOND, 1)), "ALL");

        List<RewardEntry> arrivals = board.getRewardsBySource(RewardSource.TOURIST_ARRIVAL);
        assertEquals(2, arrivals.size());
        // Newest-first by timestamp (desc); when ms tie (common in tight loop) relative order after stable sort
        // still satisfies non-increasing timestamps. Verify the contract + that the filter picked the right source.
        for (int i = 0; i < arrivals.size() - 1; i++) {
            assertTrue(arrivals.get(i).getTimestamp() >= arrivals.get(i + 1).getTimestamp(),
                    "getRewardsBySource must return newest-first");
        }
        var itemSet = arrivals.stream()
                .map(e -> e.getRewards().get(0).getItem())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(java.util.Set.of(Items.EMERALD, Items.DIAMOND), itemSet);
    }

    // --- NBT roundtrip for the board (rewards + buffer) ---

    @Test
    void toNbt_fromNbt_roundtripsRewardsAndBuffer_andSkipsBadEntries() {
        UUID id1 = board.addReward(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 5)), "ALL");
        UUID id2 = board.addReward(RewardSource.COURIER_DELIVERY, List.of(new ItemStack(Items.IRON_INGOT, 2)), "p-uuid");
        board.claimReward(id2, "p-uuid", true); // moves to buffer + marks claimed

        CompoundTag tag = board.toNBT();

        TownPaymentBoard loaded = new TownPaymentBoard();
        loaded.fromNBT(tag);

        assertEquals(2, loaded.getAllRewards().size());
        assertEquals(ClaimStatus.UNCLAIMED, loaded.getRewardById(id1).get().getStatus());
        assertEquals(ClaimStatus.CLAIMED, loaded.getRewardById(id2).get().getStatus());
        // Buffer contents survived
        assertEquals(2, loaded.getBufferStorageSlots().getTotalCount(Items.IRON_INGOT));
    }

    // --- buffer accessors (legacy + new) ---

    @Test
    void bufferStorageSlots_exposesThe18SlotBacking_andLegacyMapIsConsistent() {
        board.addToBuffer(Items.EMERALD, 3);
        assertEquals(3, board.getBufferStorageSlots().getTotalCount(Items.EMERALD));
        assertEquals(3L, board.getBufferStorage().getOrDefault(Items.EMERALD, 0L));
    }

    // --- BUG-FOUND: partial side-effects on toBuffer claim failure (multi-stack) ---
    // Current impl commits adds for stacks that fit, then aborts on a later stack that doesn't.
    // Failure leaves entry UNCLAIMED (can be claimed again) while buffer now holds the partials → dupe vector.

    @Disabled("BUG: partial items added to buffer on multi-stack reward claim failure; entry remains claimable → potential duplicate payout. See vault/Town/Payment Board/Reward Claims.md Open questions and ledger T-012")
    @Test
    void claimReward_toBuffer_multiStackReward_partialFitOnFirstStack_leaksItemsWhileLeavingEntryClaimable() {
        // Arrange: 17 slots stone (blockers), 1 slot with 60 emerald (room for 4 more)
        for (int i = 0; i < 17; i++) {
            board.getBufferStorageSlots().setSlot(i, new ItemStack(Items.STONE, 1));
        }
        board.getBufferStorageSlots().setSlot(17, new ItemStack(Items.EMERALD, 60));

        // Reward has two stacks: emeralds (will partially stack), then a diamond (no room at all)
        List<ItemStack> reward = List.of(new ItemStack(Items.EMERALD, 5), new ItemStack(Items.DIAMOND, 1));
        UUID id = board.addReward(RewardSource.TRADE, reward, "ALL");

        TownPaymentBoard.ClaimResult r = board.claimReward(id, "claimer", true);
        assertFalse(r.isSuccess());
        assertEquals("Buffer storage is full", r.getMessage());
        // Entry still claimable
        assertEquals(ClaimStatus.UNCLAIMED, board.getRewardById(id).get().getStatus());
        // Leak: emeralds were added (at least the partial amount that fit)
        int emeraldInBuffer = board.getBufferStorageSlots().getTotalCount(Items.EMERALD);
        assertTrue(emeraldInBuffer > 60, "Expected emerald count to have increased from partial add, was " + emeraldInBuffer);
        // Diamond not present (the failing stack)
        assertEquals(0, board.getBufferStorageSlots().getTotalCount(Items.DIAMOND));
    }

    // --- BUG-FOUND: single-stack partial fit still "succeeds" the claim but loses excess ---
    // addItem returns true on "placed at least some"; board treats the whole reward as claimable success.

    @Disabled("BUG: toBuffer claim of a single stack that only partially fits succeeds and marks CLAIMED, but excess count is lost (never reaches buffer or player). See vault note Open questions.")
    @Test
    void claimReward_toBuffer_singleOversizeStack_partialFit_marksClaimed_butExcessCountVanishes() {
        // One slot with 60 emerald (room 4), all other slots full of stone
        for (int i = 0; i < 17; i++) {
            board.getBufferStorageSlots().setSlot(i, new ItemStack(Items.STONE, 1));
        }
        board.getBufferStorageSlots().setSlot(17, new ItemStack(Items.EMERALD, 60));

        List<ItemStack> reward = List.of(new ItemStack(Items.EMERALD, 10)); // only 4 will fit
        UUID id = board.addReward(RewardSource.MILESTONE, reward, "ALL");

        TownPaymentBoard.ClaimResult r = board.claimReward(id, "claimer", true);
        assertTrue(r.isSuccess(), "Current (buggy) impl treats partial single-stack add as success");
        assertEquals(ClaimStatus.CLAIMED, board.getRewardById(id).get().getStatus());
        // Only 4 arrived in buffer; the declared 10 are gone from the claimable side
        assertEquals(64, board.getBufferStorageSlots().getTotalCount(Items.EMERALD)); // 60+4
        // The result still advertises the original 10 (misleading)
        assertEquals(10, r.getClaimedItems().get(0).getCount());
    }
}
