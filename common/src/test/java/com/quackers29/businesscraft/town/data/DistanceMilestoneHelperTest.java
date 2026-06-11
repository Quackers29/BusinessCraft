package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
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
 * Covers the public config accessors and the "no milestone achieved" guard paths
 * inside DistanceMilestoneHelper.checkMilestones() (disabled, empty config,
 * distance below thresholds, zero/negative). These paths are pure and safe.
 *
 * The core resolution logic (selecting a positive milestoneAchieved and
 * producing scaled rewards) lives after the threshold loop and always calls
 * into parseRewards, which references ItemStack (and transitively other MC
 * classes) whose static initializers are unsafe in a pure JUnit environment.
 * Therefore only guard + getter surface is tested here; see vault note for
 * full formulas and why this item is NEEDS-MC.
 *
 * Documentation: vault/Economy/Milestones/Distance Milestone Resolution.md
 */
class DistanceMilestoneHelperTest {

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

        // Install a stub so getItem(...) returns a real (bare) Item; this lets
        // the reward parse + scaling path succeed for positive-milestone tests.
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

    // --- test double for registry lookup (pure data, no MC bootstrap) ---

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
            // Not reached by the tests retained in this class (only guard paths that
            // return early with milestoneAchieved=-1 before any parseRewards call).
            // The stub is still required because setUp always installs it and the
            // inner class definition references the Item/RL types from the interface.
            return null;
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

    // --- checkMilestones: guard / no-match paths (these avoid the parseRewards branch
    //     that touches ItemStack and other MC classes whose static init is unsafe here) ---

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
}
