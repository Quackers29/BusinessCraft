package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-007: Resource Storage Operations (Test + Docs Loop).
 *
 * Only the null-guard paths (add null, consume null/non-positive) and the
 * completely pure population logic in TownEconomyComponent can execute
 * without triggering Minecraft bootstrap.
 *
 * Any test that constructs or passes a real Item (new Item(Properties) or
 * Items.XXX) causes:
 *   ExceptionInInitializerError: Not bootstrapped
 *   (via ResourceKey -> Registries -> BuiltInRegistries etc.)
 *
 * This is the same limitation that made T-002 and T-003 NEEDS-MC for their
 * ItemStack/Item-using paths. Therefore the add/remove/overflow math,
 * zero-retention, consume-with-items, and all NBT roundtrips are documented
 * in the vault note but not unit-tested here.
 *
 * Documentation: vault/Town/Resources/Resource Storage Operations.md
 */
class TownResourcesTest {

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

    // --- test double (kept for structural parity with T-002; not reached by safe tests) ---

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
}
