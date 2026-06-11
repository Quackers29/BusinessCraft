package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-007: Resource Storage Operations (Test + Docs Loop).
 *
 * McBootstrap (re-opened T-007): enables real Item (Items.EMERALD etc.) for
 * exercising the core math in TownResources (add/remove/overflow/clamp/zero-retention)
 * and NBT save/load roundtrips (which require RL <-> Item via PlatformAccess.registry).
 *
 * A TestRegistryHelper (installed per-test) delegates getItem/getItemKey to
 * BuiltInRegistries after bootstrap so both directions work and debug-path
 * getItemKey calls in add/remove do not NPE.
 *
 * The original 4 guard tests remain (they never needed Items). New tests cover
 * every rule and edge documented in the vault note, with hand-computed expectations.
 *
 * Documentation: vault/Town/Resources/Resource Storage Operations.md
 */
class TownResourcesTest {

    @BeforeAll
    static void boot() {
        // Initializes vanilla registries so Items.EMERALD, Items.BREAD, etc. can be
        // constructed/resolved and used in addResource / consumeResource / NBT paths.
        McBootstrap.init();
    }

    private TownResources resources;
    private TownEconomyComponent economy;
    private RegistryHelper savedRegistry;

    @BeforeEach
    void setUp() {
        resources = new TownResources();
        economy = new TownEconomyComponent();
        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new TestRegistryHelper();
    }

    @AfterEach
    void tearDown() {
        PlatformAccess.registry = savedRegistry;
    }

    // --- test double ---
    // Delegates getItem/getItemKey to BuiltInRegistries (post-McBootstrap) so that
    // TownResources.add/remove (which call getItemKey for debug logs) and
    // save (getItemKey) + load (getItem) roundtrips work with real Items.
    // Other registration methods are no-ops (not exercised by this unit).

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
            if (location == null) return null;
            // Post-bootstrap: resolves "minecraft:emerald" etc. to the real registered Item.
            return BuiltInRegistries.ITEM.get(location);
        }

        @Override
        public ResourceLocation getItemKey(Item item) {
            if (item == null) return null;
            // Enables save() to emit proper "minecraft:xxx" keys, and makes debug
            // log expressions in add/remove produce real RLs instead of null.
            return BuiltInRegistries.ITEM.getKey(item);
        }

        @Override
        public Iterable<Item> getItems() {
            return List.of();
        }
    }

    // --- only Item-free guards (these never construct or pass a non-null Item) ---

    @Test
    void addResource_nullItem_ignored() {
        resources.addResource(null, 100);
        assertEquals(0, resources.getResourceCount(null));
    }

    @Test
    void consumeResource_nullOrNonPositive_returnsFalse() {
        // All paths here use null item so no bootstrap
        assertFalse(resources.consumeResource(null, 1));
        assertFalse(resources.consumeResource(null, 0));
        assertFalse(resources.consumeResource(null, -3));
    }

    // --- TownEconomyComponent population (pure int logic, no resources, no Item, no registry) ---

    @Test
    void economyComponent_population_setAndRemove() {
        economy.setPopulation(25);
        assertEquals(25, economy.getPopulation());

        economy.removePopulation(7);
        // 25 >= 7 -> 18
        assertEquals(18, economy.getPopulation());

        economy.removePopulation(100);
        // insufficient -> no change
        assertEquals(18, economy.getPopulation());

        economy.setPopulation(0);
        assertEquals(0, economy.getPopulation());

        economy.setPopulation(-3); // guard
        assertEquals(0, economy.getPopulation());
    }

    @Test
    void economyComponent_getPopulation_defaultsToZero() {
        // fresh component has 0 (from field init, load would override)
        assertEquals(0, economy.getPopulation());
    }

    // --- addResource positive / accumulate / zero / overflow (core mutation rules) ---

    @Test
    void addResource_positive_incrementsCount() {
        resources.addResource(Items.EMERALD, 5);
        assertEquals(5, resources.getResourceCount(Items.EMERALD));
    }

    @Test
    void addResource_positive_accumulatesAcrossCalls() {
        resources.addResource(Items.EMERALD, 3);
        resources.addResource(Items.EMERALD, 4);
        // 3 + 4 = 7
        assertEquals(7, resources.getResourceCount(Items.EMERALD));
    }

    @Test
    void addResource_countZero_isNoop() {
        resources.addResource(Items.DIAMOND, 2);
        resources.addResource(Items.DIAMOND, 0);
        // still 2; 0 is early return before any map write or logging
        assertEquals(2, resources.getResourceCount(Items.DIAMOND));
    }

    @Test
    void addResource_overflow_capsAtLongMax() {
        // Hand-computed: start near MAX so second add triggers addExact overflow path
        long nearMax = Long.MAX_VALUE - 5;
        resources.addResource(Items.BREAD, nearMax);
        assertEquals(nearMax, resources.getResourceCount(Items.BREAD));

        resources.addResource(Items.BREAD, 10);
        // nearMax + 10 would overflow -> catch ArithmeticException, store Long.MAX_VALUE
        assertEquals(Long.MAX_VALUE, resources.getResourceCount(Items.BREAD));
    }

    // --- addResource negative (remove) paths: clamp, zero retention, no-op when absent ---

    @Test
    void addResource_negative_clampsAtZero_andRetainsZeroEntry() {
        resources.addResource(Items.EMERALD, 7);
        resources.addResource(Items.EMERALD, -10);
        // 7 + (-10) = -3 -> Math.max(0, -3) = 0; because pre-op >0, put(0) happens
        assertEquals(0, resources.getResourceCount(Items.EMERALD));

        Map<Item, Long> all = resources.getAllResources();
        assertTrue(all.containsKey(Items.EMERALD));
        assertEquals(0L, all.get(Items.EMERALD));
    }

    @Test
    void addResource_negative_whenAlreadyZero_doesNotInsert_andNoPut() {
        // currentAmount==0 path: if (currentAmount > 0) false, so no put, no map entry created
        resources.addResource(Items.EMERALD, -3);
        assertEquals(0, resources.getResourceCount(Items.EMERALD));
        assertFalse(resources.getAllResources().containsKey(Items.EMERALD));
    }

    @Test
    void addResource_negative_partialReduce() {
        resources.addResource(Items.DIAMOND, 20);
        resources.addResource(Items.DIAMOND, -7);
        // 20 -7 =13 >0 so stored
        assertEquals(13, resources.getResourceCount(Items.DIAMOND));
    }

    // --- consumeResource happy + failure paths (distinct from negative-add) ---

    @Test
    void consumeResource_sufficient_returnsTrue_andSubtractsExact() {
        resources.addResource(Items.BREAD, 10);
        boolean ok = resources.consumeResource(Items.BREAD, 4);
        assertTrue(ok);
        // 10 - 4 =6
        assertEquals(6, resources.getResourceCount(Items.BREAD));
    }

    @Test
    void consumeResource_exactAmount_returnsTrue_leavesZeroInMap() {
        resources.addResource(Items.EMERALD, 5);
        boolean ok = resources.consumeResource(Items.EMERALD, 5);
        assertTrue(ok);
        assertEquals(0, resources.getResourceCount(Items.EMERALD));
        // consume does unconditional put(current - count) even when result==0
        assertTrue(resources.getAllResources().containsKey(Items.EMERALD));
    }

    @Test
    void consumeResource_insufficient_returnsFalse_noMutation() {
        resources.addResource(Items.DIAMOND, 3);
        boolean ok = resources.consumeResource(Items.DIAMOND, 10);
        assertFalse(ok);
        assertEquals(3, resources.getResourceCount(Items.DIAMOND));
    }

    @Test
    void consumeResource_nonPositive_returnsFalse_immediately() {
        resources.addResource(Items.BREAD, 5);
        assertFalse(resources.consumeResource(Items.BREAD, 0));
        assertFalse(resources.consumeResource(Items.BREAD, -2));
        assertEquals(5, resources.getResourceCount(Items.BREAD));
    }

    // --- getAllResources view behavior ---

    @Test
    void getAllResources_returnsUnmodifiable_andReflectsLiveChanges() {
        resources.addResource(Items.EMERALD, 1);
        Map<Item, Long> view = resources.getAllResources();
        assertEquals(1, view.get(Items.EMERALD));

        resources.addResource(Items.EMERALD, 2);
        // live wrapper: mutation visible through prior reference
        assertEquals(3, view.get(Items.EMERALD));

        // unmodifiable contract
        assertThrows(UnsupportedOperationException.class, () -> view.put(Items.DIAMOND, 1L));
    }

    // --- NBT save/load roundtrips and sanitization ---

    @Test
    void saveLoad_roundtrip_preservesPositive_andZeroEntries() {
        resources.addResource(Items.EMERALD, 42);
        resources.addResource(Items.DIAMOND, 1);
        resources.addResource(Items.BREAD, 5);
        resources.addResource(Items.BREAD, -5); // zero it; entry retained with 0

        CompoundTag tag = new CompoundTag();
        resources.save(tag);

        TownResources loaded = new TownResources();
        loaded.load(tag);

        assertEquals(42, loaded.getResourceCount(Items.EMERALD));
        assertEquals(1, loaded.getResourceCount(Items.DIAMOND));
        assertEquals(0, loaded.getResourceCount(Items.BREAD));
        assertTrue(loaded.getAllResources().containsKey(Items.BREAD));
    }

    @Test
    void load_sanitizesNegativeAmount_toZero() {
        CompoundTag tag = new CompoundTag();
        CompoundTag resTag = new CompoundTag();
        resTag.putLong("minecraft:emerald", -99L);
        tag.put("resources", resTag);

        resources.load(tag);

        assertEquals(0, resources.getResourceCount(Items.EMERALD));
    }

    @Test
    void load_skipsAir_andRegistryMissItems() {
        CompoundTag tag = new CompoundTag();
        CompoundTag resTag = new CompoundTag();
        resTag.putLong("minecraft:air", 64L);
        resTag.putLong("minecraft:nonexistent_item_xyz", 10L); // resolves to AIR or null -> skipped by load guard
        tag.put("resources", resTag);

        resources.load(tag);

        // nothing should have been inserted
        assertTrue(resources.getAllResources().isEmpty());
        assertEquals(0, resources.getResourceCount(Items.AIR));
    }

    // --- TownEconomyComponent delegation + combined save/load ---

    @Test
    void economyComponent_addResource_delegatesToInner_andGetResourcesExposesIt() {
        economy.addResource(Items.EMERALD, 15);
        assertEquals(15, economy.getResourceCount(Items.EMERALD));
        assertEquals(15, economy.getResources().getResourceCount(Items.EMERALD));
    }

    @Test
    void economyComponent_saveLoad_roundtripsPopulation_andResourcesTogether() {
        economy.setPopulation(123);
        economy.addResource(Items.EMERALD, 7);
        economy.addResource(Items.BREAD, 2);

        CompoundTag tag = new CompoundTag();
        economy.save(tag);

        TownEconomyComponent loaded = new TownEconomyComponent();
        loaded.load(tag);

        assertEquals(123, loaded.getPopulation());
        assertEquals(7, loaded.getResourceCount(Items.EMERALD));
        assertEquals(2, loaded.getResourceCount(Items.BREAD));
    }

    // --- pinning test for documented quirk (harmless, dead code) ---

    @Test
    void economyComponent_removePopulation_negativeIncreases_popQuirkPinned() {
        // QUIRK (pinned, see vault note Open questions): removePopulation only guards
        // "if (population >= amount)", which is true for negative amount. Then pop -= negative
        // acts as an add. This path is dead in prod (no call sites) but current behavior
        // is asserted here so a future change would be noticed.
        economy.setPopulation(10);
        economy.removePopulation(-3);
        // 10 >= -3 (true) ; 10 - (-3) = 13
        assertEquals(13, economy.getPopulation());
    }
}
