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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-012: Reward Claims — RewardEntry portion (Test + Docs Loop).
 *
 * Covers the data model and pure decision logic for individual reward entries:
 * eligibility (ALL vs personal UUID), expiry, status transitions, NBT (de)serialization
 * with defensive handling for bad enums and empty stacks, fromNetwork, equals-by-id,
 * and display helpers.
 *
 * McBootstrap initializes registries so ItemStack ctors and ItemStack.of in fromNBT work.
 * A TestRegistryHelper double is installed (matching T-002/T-007 pattern) for any
 * indirect registry calls during NBT or display.
 *
 * Documentation: vault/Town/Payment Board/Reward Claims.md
 */
class RewardEntryTest {

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    private RegistryHelper savedRegistry;

    @BeforeEach
    void setUp() {
        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new TestRegistryHelper();
    }

    @AfterEach
    void tearDown() {
        PlatformAccess.registry = savedRegistry;
    }

    // --- test double for registry (enables NBT fidelity + any debug paths) ---

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

    // --- canBeClaimed matrix ---

    @Test
    void canBeClaimed_allEligibility_anyClaimerMatches() {
        RewardEntry entry = new RewardEntry(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 2)), "ALL");
        assertTrue(entry.canBeClaimed("player-uuid-1"));
        assertTrue(entry.canBeClaimed("player-uuid-2"));
        assertTrue(entry.canBeClaimed(""));
    }

    @Test
    void canBeClaimed_personalEligibility_exactMatchOnly() {
        RewardEntry entry = new RewardEntry(RewardSource.COURIER_DELIVERY, List.of(new ItemStack(Items.IRON_INGOT, 4)), "p-uuid-123");
        assertTrue(entry.canBeClaimed("p-uuid-123"));
        assertFalse(entry.canBeClaimed("p-uuid-123-extra"));
        assertFalse(entry.canBeClaimed("other-player"));
        assertFalse(entry.canBeClaimed("ALL")); // "ALL" is the special token, not a claimer
    }

    @Test
    void canBeClaimed_claimedStatus_shortCircuits() {
        RewardEntry entry = new RewardEntry(RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        entry.setStatus(ClaimStatus.CLAIMED);
        assertFalse(entry.canBeClaimed("anyone"));
    }

    @Test
    void canBeClaimed_expiredStatusOrTime_shortCircuits() {
        RewardEntry entry = new RewardEntry(RewardSource.MILESTONE, List.of(new ItemStack(Items.GOLD_INGOT, 1)), "ALL");
        entry.setStatus(ClaimStatus.UNCLAIMED);
        entry.setExpirationTime(System.currentTimeMillis() - 1);
        assertFalse(entry.canBeClaimed("anyone"));
        assertTrue(entry.isExpired());
    }

    // --- isExpired semantics (strict greater) ---

    @Test
    void isExpired_exactNow_isNotExpired() {
        RewardEntry entry = new RewardEntry(RewardSource.OTHER, List.of(new ItemStack(Items.DIAMOND, 1)), "ALL");
        long now = System.currentTimeMillis();
        entry.setExpirationTime(now);
        // strict > means == now is still valid
        assertFalse(entry.isExpired());
    }

    @Test
    void isExpired_oneMsPast_isExpired() {
        RewardEntry entry = new RewardEntry(RewardSource.OTHER, List.of(new ItemStack(Items.DIAMOND, 1)), "ALL");
        entry.setExpirationTime(System.currentTimeMillis() - 1);
        assertTrue(entry.isExpired());
    }

    // --- ctor default expiry vs board override (pinned) ---

    @Test
    void directCtor_defaultExpiryIs24Hours() {
        long before = System.currentTimeMillis();
        RewardEntry entry = new RewardEntry(RewardSource.TRADE, List.of(new ItemStack(Items.EMERALD, 5)), "ALL");
        long delta = entry.getExpirationTime() - before;
        // 24 h in ms (allow small execution skew)
        assertTrue(delta >= 24 * 60 * 60 * 1000L - 2000 && delta <= 24 * 60 * 60 * 1000L + 2000,
                "Expected ~24 h default, got " + delta);
    }

    // --- fromNetwork / getters / status mutation ---

    @Test
    void fromNetwork_roundtripsScalarsAndItems() {
        UUID id = UUID.randomUUID();
        long ts = 1_700_000_000_000L;
        long exp = ts + 7 * 24 * 60 * 60 * 1000L;
        List<ItemStack> stacks = List.of(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.BREAD, 2));
        RewardEntry entry = RewardEntry.fromNetwork(id, ts, exp, RewardSource.MILESTONE, stacks, ClaimStatus.UNCLAIMED, "ALL");

        assertEquals(id, entry.getId());
        assertEquals(ts, entry.getTimestamp());
        assertEquals(exp, entry.getExpirationTime());
        assertEquals(RewardSource.MILESTONE, entry.getSource());
        assertEquals(ClaimStatus.UNCLAIMED, entry.getStatus());
        assertEquals("ALL", entry.getEligibility());
        assertEquals(2, entry.getRewards().size());
        assertEquals(3, entry.getRewards().get(0).getCount());
    }

    @Test
    void setStatusAndExpiry_mutationVisibleToCanBeClaimed() {
        RewardEntry entry = new RewardEntry(RewardSource.JOB_COMPLETION, List.of(new ItemStack(Items.IRON_INGOT, 1)), "player-xyz");
        assertTrue(entry.canBeClaimed("player-xyz"));

        entry.setStatus(ClaimStatus.CLAIMED);
        assertFalse(entry.canBeClaimed("player-xyz"));

        entry.setStatus(ClaimStatus.UNCLAIMED);
        entry.setExpirationTime(System.currentTimeMillis() + 10000);
        assertTrue(entry.canBeClaimed("player-xyz"));
    }

    // --- equals / hash by id only ---

    @Test
    void equalsAndHash_byIdOnly_evenIfOtherFieldsDiffer() {
        UUID id = UUID.randomUUID();
        RewardEntry a = RewardEntry.fromNetwork(id, 1000L, 2000L, RewardSource.OTHER, List.of(new ItemStack(Items.EMERALD, 1)), ClaimStatus.UNCLAIMED, "ALL");
        RewardEntry b = RewardEntry.fromNetwork(id, 9999L, 8888L, RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.DIAMOND, 9)), ClaimStatus.EXPIRED, "someone");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // --- NBT defensive behavior (from code, not comments) ---

    @Test
    void fromNbt_unknownSourceDefaultsToOther_andUnknownStatusToUnclaimed() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", UUID.randomUUID());
        tag.putLong("timestamp", 1_000_000L);
        tag.putLong("expirationTime", 2_000_000L);
        tag.putString("source", "NOT_A_REAL_SOURCE");
        tag.putString("status", "NOT_A_REAL_STATUS");
        tag.putString("eligibility", "ALL");
        tag.put("rewards", new net.minecraft.nbt.ListTag());
        tag.put("metadata", new CompoundTag());

        RewardEntry entry = RewardEntry.fromNBT(tag);
        assertNotNull(entry);
        assertEquals(RewardSource.OTHER, entry.getSource());
        assertEquals(ClaimStatus.UNCLAIMED, entry.getStatus());
    }

    @Test
    void fromNbt_emptyEligibilityDefaultsToAll() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", UUID.randomUUID());
        tag.putLong("timestamp", 1_000_000L);
        tag.putLong("expirationTime", 2_000_000L);
        tag.putString("source", "OTHER");
        tag.putString("status", "UNCLAIMED");
        tag.putString("eligibility", "");
        tag.put("rewards", new net.minecraft.nbt.ListTag());
        tag.put("metadata", new CompoundTag());

        RewardEntry entry = RewardEntry.fromNBT(tag);
        assertEquals("ALL", entry.getEligibility());
    }

    @Test
    void fromNbt_dropsEmptyAndAirStacks_insideRewardsList() {
        CompoundTag tag = new CompoundTag();
        UUID id = UUID.randomUUID();
        tag.putUUID("id", id);
        tag.putLong("timestamp", 1_000_000L);
        tag.putLong("expirationTime", 2_000_000L);
        tag.putString("source", "OTHER");
        tag.putString("status", "UNCLAIMED");
        tag.putString("eligibility", "ALL");

        net.minecraft.nbt.ListTag rewardsList = new net.minecraft.nbt.ListTag();
        // A real stack
        CompoundTag real = new CompoundTag();
        new ItemStack(Items.EMERALD, 2).save(real);
        rewardsList.add(real);
        // An empty one (no "id")
        rewardsList.add(new CompoundTag());
        // Air
        CompoundTag airTag = new CompoundTag();
        new ItemStack(Items.AIR, 1).save(airTag);
        rewardsList.add(airTag);

        tag.put("rewards", rewardsList);
        tag.put("metadata", new CompoundTag());

        RewardEntry entry = RewardEntry.fromNBT(tag);
        assertNotNull(entry);
        // Only the real emerald survives
        assertEquals(1, entry.getRewards().size());
        assertEquals(Items.EMERALD, entry.getRewards().get(0).getItem());
        assertEquals(2, entry.getRewards().get(0).getCount());
    }

    @Test
    void toNbt_fromNbt_roundtripsPersonalRewardWithMetadata() {
        RewardEntry original = new RewardEntry(RewardSource.COURIER_PICKUP, List.of(new ItemStack(Items.GOLD_INGOT, 7)), "courier-uuid-abc");
        original.addMetadata("contractId", "ct-42");
        original.addMetadata("origin", "TownA");

        CompoundTag tag = original.toNBT();
        RewardEntry loaded = RewardEntry.fromNBT(tag);

        assertNotNull(loaded);
        assertEquals(original.getId(), loaded.getId());
        assertEquals("courier-uuid-abc", loaded.getEligibility());
        assertEquals(7, loaded.getRewards().get(0).getCount());
        assertEquals("ct-42", loaded.getMetadata().get("contractId"));
        assertEquals("TownA", loaded.getMetadata().get("origin"));
    }

    // --- display helpers (post-bootstrap) ---

    @Test
    void getRewardsDisplay_combinesSameItems_andFormatsCounts() {
        List<ItemStack> stacks = List.of(
                new ItemStack(Items.EMERALD, 3),
                new ItemStack(Items.EMERALD, 2),
                new ItemStack(Items.BREAD, 1)
        );
        RewardEntry entry = new RewardEntry(RewardSource.TOURIST_ARRIVAL, stacks, "ALL");
        String display = entry.getRewardsDisplay();
        // Expect stable order (emeralds first as encountered) and "5x Emerald, Bread"
        assertTrue(display.contains("5x Emerald") || display.contains("5x Emerald"));
        assertTrue(display.contains("Bread"));
    }

    @Test
    void getRewardsDisplay_emptyList() {
        RewardEntry entry = new RewardEntry(RewardSource.OTHER, List.of(), "ALL");
        assertEquals("No rewards", entry.getRewardsDisplay());
    }
}
