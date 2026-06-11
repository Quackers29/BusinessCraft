package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-002: Distance Milestone Resolution (Test + Docs Loop).
 *
 * Covers:
 *  - Public config/UI helpers: getMilestoneDistances (sorted), getMilestoneRewards
 *  - checkMilestones guard paths (disabled, empty map, distance below all thresholds,
 *    zero/negative distance) that early-return milestoneAchieved=-1 without parsing.
 *  - Positive resolution paths (added this iteration): threshold selection (highest wins),
 *    reward string parsing (default count + explicit :count), scaling by touristCount,
 *    mixed good/bad reward strings, and MilestoneResult shape for achieved cases.
 *
 * McBootstrap is used (@BeforeAll) so that vanilla Item registries are initialized,
 * allowing parseRewardString to succeed for "minecraft:bread", "minecraft:experience_bottle"
 * etc. A TestRegistryHelper installed in @BeforeEach delegates getItem(ResourceLocation)
 * to BuiltInRegistries after bootstrap (matching the contract of the real platform helpers).
 *
 * Private parse helpers are exercised via the public checkMilestones entry point.
 * deliverRewards side-effects are out of scope for this unit test (require real Town).
 *
 * Documentation: vault/Economy/Milestones/Distance Milestone Resolution.md
 */
class DistanceMilestoneHelperTest {

    @BeforeAll
    static void boot() {
        // Initializes vanilla registries (Items, etc.) so ItemStack + registry lookups
        // inside parseRewardString succeed for minecraft:* reward strings.
        McBootstrap.init();
    }

    private boolean savedEnableMilestones;
    private Map<Integer, List<String>> savedMilestoneRewards;
    private RegistryHelper savedRegistry;

    @BeforeEach
    void setUp() {
        savedEnableMilestones = ConfigLoader.enableMilestones;
        savedMilestoneRewards = new HashMap<>();
        for (var e : ConfigLoader.milestoneRewards.entrySet()) {
            savedMilestoneRewards.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        savedRegistry = PlatformAccess.registry;

        // Install a stub whose getItem(...) delegates to BuiltInRegistries (after McBootstrap)
        // so the reward parse + scaling path succeeds for positive-milestone tests.
        PlatformAccess.registry = new TestRegistryHelper();

        // Start each test from a clean, deterministic config state
        ConfigLoader.enableMilestones = true;
        ConfigLoader.milestoneRewards.clear();
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.enableMilestones = savedEnableMilestones;
        ConfigLoader.milestoneRewards.clear();
        ConfigLoader.milestoneRewards.putAll(savedMilestoneRewards);
        PlatformAccess.registry = savedRegistry;
    }

    // --- test double for registry lookup ---
    // Delegates to BuiltInRegistries after McBootstrap so positive checkMilestones
    // paths (which exercise parseRewardString) can construct real ItemStacks for
    // configured "minecraft:*" reward strings. Other registration methods remain no-ops.

    private static class TestRegistryHelper implements RegistryHelper {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.item.Item> Supplier<T> registerBlockItem(String name, Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.entity.EntityType<?>> Supplier<T> registerEntityType(String name, Supplier<T> entityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.entity.BlockEntityType<?>> Supplier<T> registerBlockEntityType(String name, Supplier<T> blockEntityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<net.minecraft.world.inventory.MenuType<T>> registerExtendedMenuType(String name, MenuFactory<T> factory) {
            return () -> null;
        }

        @Override
        public Item getItem(ResourceLocation location) {
            // After McBootstrap.init(), vanilla content (bread, experience_bottle, etc.)
            // is registered in BuiltInRegistries. Delegate so parseRewardString succeeds
            // for the positive-milestone test cases. Matches the lookup contract used by
            // the real Fabric/Forge RegistryHelper implementations.
            return BuiltInRegistries.ITEM.get(location);
        }

        @Override
        public ResourceLocation getItemKey(Item item) {
            return null;
        }

        @Override
        public Iterable<Item> getItems() {
            return List.of();
        }
    }

    // --- helpers ---

    private DistanceMilestoneHelper.MilestoneResult check(double distance, int count) {
        return DistanceMilestoneHelper.checkMilestones(distance, count);
    }

    // --- config getter tests ---

    @Test
    void getMilestoneDistances_returnsEmptyWhenNoMilestonesConfigured() {
        // map is cleared in setUp
        assertTrue(DistanceMilestoneHelper.getMilestoneDistances().isEmpty());
    }

    @Test
    void getMilestoneDistances_returnsSortedAscending() {
        ConfigLoader.milestoneRewards.put(200, List.of("minecraft:gold:1"));
        ConfigLoader.milestoneRewards.put(10, List.of("minecraft:bread:1"));
        ConfigLoader.milestoneRewards.put(50, List.of("minecraft:emerald:2"));

        List<Integer> dists = DistanceMilestoneHelper.getMilestoneDistances();
        assertEquals(List.of(10, 50, 200), dists);
    }

    @Test
    void getMilestoneRewards_returnsConfiguredListOrEmpty() {
        ConfigLoader.milestoneRewards.put(100, List.of("minecraft:bread:1", "minecraft:experience_bottle:2"));
        assertEquals(List.of("minecraft:bread:1", "minecraft:experience_bottle:2"),
                DistanceMilestoneHelper.getMilestoneRewards(100));
        assertTrue(DistanceMilestoneHelper.getMilestoneRewards(999).isEmpty());
    }

    // --- checkMilestones: guard / no-match paths (early return -1, no parse) ---

    @Test
    void checkMilestones_disabled_returnsNegativeAndEmpty() {
        ConfigLoader.enableMilestones = false;
        ConfigLoader.milestoneRewards.put(10, List.of("minecraft:bread:1"));

        var r = check(500.0, 3);
        assertEquals(500.0, r.actualDistance);
        assertEquals(-1, r.milestoneAchieved);
        assertTrue(r.rewards.isEmpty());
        assertFalse(r.hasRewards());
        assertEquals(3, r.touristCount);
    }

    @Test
    void checkMilestones_noMilestonesConfigured_returnsNegative() {
        // map empty
        var r = check(100.0, 2);
        assertEquals(-1, r.milestoneAchieved);
        assertTrue(r.rewards.isEmpty());
    }

    @Test
    void checkMilestones_distanceBelowAllThresholds_returnsNegative() {
        ConfigLoader.milestoneRewards.put(100, List.of("minecraft:bread:1"));
        ConfigLoader.milestoneRewards.put(50, List.of("minecraft:emerald:1"));

        var r = check(49.9, 1);
        assertEquals(-1, r.milestoneAchieved);
        assertTrue(r.rewards.isEmpty());
    }

    @Test
    void checkMilestones_zeroOrNegativeDistance_returnsNegative() {
        ConfigLoader.milestoneRewards.put(10, List.of("minecraft:bread:1"));

        assertEquals(-1, check(0.0, 5).milestoneAchieved);
        assertEquals(-1, check(-5.0, 1).milestoneAchieved);
    }

    // --- result shape (safe path) ---

    @Test
    void milestoneResult_hasRewards_falseWhenNoRewards() {
        var r = check(1.0, 1);
        assertFalse(r.hasRewards());
    }

    // --- positive resolution paths (enabled by McBootstrap + real registry lookup) ---
    // Every test below exercises the "highest milestone <= distance" selection,
    // parseRewardString (default count vs explicit, bad strings), and the
    // post-parse scaling in parseRewards (count *= touristCount per stack).
    // Expected values are hand-computed in the comments.

    @Test
    void checkMilestones_singleMilestone_distanceAtOrAbove_thresholdAchieved_scaledByCount() {
        // Default-like config: 10 blocks → 1 bread + 2 exp bottles per tourist
        ConfigLoader.milestoneRewards.put(10, List.of("minecraft:bread:1", "minecraft:experience_bottle:2"));

        // 15 >= 10, count=3
        // bread:1 *3 =3; exp:2 *3=6
        var r = check(15.0, 3);
        assertEquals(15.0, r.actualDistance);
        assertEquals(10, r.milestoneAchieved);
        assertEquals(3, r.touristCount);
        assertTrue(r.hasRewards());
        assertEquals(2, r.rewards.size());

        // Hand-computed expectations
        assertEquals(Items.BREAD, r.rewards.get(0).getItem());
        assertEquals(1 * 3, r.rewards.get(0).getCount());   // 3
        assertEquals(Items.EXPERIENCE_BOTTLE, r.rewards.get(1).getItem());
        assertEquals(2 * 3, r.rewards.get(1).getCount());   // 6
    }

    @Test
    void checkMilestones_multipleThresholds_selectsHighestQualifying() {
        ConfigLoader.milestoneRewards.put(200, List.of("minecraft:gold_ingot:1"));
        ConfigLoader.milestoneRewards.put(50, List.of("minecraft:iron_ingot:1"));
        ConfigLoader.milestoneRewards.put(10, List.of("minecraft:bread:1"));

        // 120 >= 10 and >=50, but not 200 → highest is 50
        var r120 = check(120.0, 1);
        assertEquals(50, r120.milestoneAchieved);
        assertEquals(Items.IRON_INGOT, r120.rewards.get(0).getItem());

        // 50 exactly → 50 ( >= )
        var r50 = check(50.0, 2);
        assertEquals(50, r50.milestoneAchieved);
        assertEquals(1 * 2, r50.rewards.get(0).getCount());

        // 199 → still 50
        var r199 = check(199.0, 1);
        assertEquals(50, r199.milestoneAchieved);

        // 200 → 200
        var r200 = check(200.0, 1);
        assertEquals(200, r200.milestoneAchieved);
        assertEquals(Items.GOLD_INGOT, r200.rewards.get(0).getItem());
    }

    @Test
    void checkMilestones_exactThresholdMatch_achievesMilestone() {
        ConfigLoader.milestoneRewards.put(100, List.of("minecraft:emerald:1"));
        var r = check(100.0, 5);
        assertEquals(100, r.milestoneAchieved);
        assertEquals(1 * 5, r.rewards.get(0).getCount());
    }

    @Test
    void checkMilestones_rewardString_defaultCountAndExplicitCount() {
        // "minecraft:bread" → count defaults to 1; "minecraft:emerald:3" → explicit 3
        ConfigLoader.milestoneRewards.put(10, List.of("minecraft:bread", "minecraft:emerald:3"));

        var r = check(10.0, 2);
        assertEquals(2, r.rewards.size());
        // bread:1 (default) *2 = 2
        assertEquals(Items.BREAD, r.rewards.get(0).getItem());
        assertEquals(2, r.rewards.get(0).getCount());
        // emerald:3 *2 = 6
        assertEquals(Items.EMERALD, r.rewards.get(1).getItem());
        assertEquals(6, r.rewards.get(1).getCount());
    }

    @Test
    void checkMilestones_mixedGoodAndBadRewardStrings_onlyGoodContribute_milestoneStillRecorded() {
        // One good, one bad namespace/item, one good — partial success path
        ConfigLoader.milestoneRewards.put(25, List.of(
                "minecraft:bread:1",
                "minecraft:nonexistent_item_xyz:10",   // will warn + drop
                "minecraft:experience_bottle:2"
        ));

        var r = check(30.0, 4);
        assertEquals(25, r.milestoneAchieved);  // still records the achieved threshold
        assertEquals(4, r.touristCount);
        // Only the two good ones survive parse + scale
        assertEquals(2, r.rewards.size());
        assertEquals(Items.BREAD, r.rewards.get(0).getItem());
        assertEquals(1 * 4, r.rewards.get(0).getCount());
        assertEquals(Items.EXPERIENCE_BOTTLE, r.rewards.get(1).getItem());
        assertEquals(2 * 4, r.rewards.get(1).getCount());
    }

    @Test
    void checkMilestones_achievedMilestone_zeroTouristCount_producesZeroCountStacks_hasRewardsTrue() {
        // QUIRK (pinned): count=0 still selects milestone and produces scaled stacks
        // (0 * baseCount), list is non-empty so hasRewards()=true. Unreachable via
        // normal VisitBuffer (records have count>=1), but the math has no early guard.
        // This pins current behavior; if it changes, update vault note.
        ConfigLoader.milestoneRewards.put(10, List.of("minecraft:bread:1", "minecraft:emerald:1"));

        var r = check(15.0, 0);
        assertEquals(10, r.milestoneAchieved);
        assertEquals(0, r.touristCount);
        assertTrue(r.hasRewards());                 // list non-empty even though counts are 0
        assertEquals(2, r.rewards.size());
        assertEquals(0, r.rewards.get(0).getCount()); // 1*0
        assertEquals(0, r.rewards.get(1).getCount()); // 1*0
        // Note: in MC, ItemStack with count=0 reports isEmpty()=true, but the
        // MilestoneResult wrapper only checks !rewards.isEmpty().
    }

    @Test
    void checkMilestones_positiveResult_hasRewards_true() {
        ConfigLoader.milestoneRewards.put(5, List.of("minecraft:bread:1"));
        var r = check(5.0, 1);
        assertTrue(r.hasRewards());
        assertFalse(r.rewards.isEmpty());
    }

    @Test
    void checkMilestones_belowThreshold_evenWithItemsConfigured_returnsNegative() {
        ConfigLoader.milestoneRewards.put(1000, List.of("minecraft:diamond:1"));
        var r = check(50.0, 10);
        assertEquals(-1, r.milestoneAchieved);
        assertTrue(r.rewards.isEmpty());
        assertFalse(r.hasRewards());
    }
}
