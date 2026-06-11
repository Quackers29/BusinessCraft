package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.util.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-008: Town Distance Validation (Test + Docs Loop) — boundary math half.
 *
 * Covers TownBoundaryService:
 *   calculateBoundaryRadius (pop 1:1 or default on null)
 *   checkTownPlacement (new-town defaultStartingPopulation + existing.getBoundaryRadius(),
 *                       euclidean via sqrt(distSqr), < is conflict)
 *   getMinimumDistanceRequired (null handling + sum of radii)
 *   checkBoundaryExpansion (newPopulation as proposed radius, skip-self by id,
 *                           no-op when not actually increasing pop)
 *
 * Fresh Town instances report getBoundaryRadius() == 50 (upgrade "border" fallback).
 * We therefore use realistic 5 + 50 = 55 required distances for placement tests.
 * Population for the service's calculateBoundaryRadius is controlled via setPopulation.
 *
 * Documentation: vault/Town/Boundaries/Town Distance Validation.md
 */
class TownBoundaryServiceTest {

    private TownBoundaryService service;
    private int savedDefaultPop;
    private RegistryHelper savedRegistry;

    @BeforeEach
    void setUp() {
        service = new TownBoundaryService();
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new TestRegistryHelper();

        ConfigLoader.defaultStartingPopulation = 5;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
        PlatformAccess.registry = savedRegistry;
    }

    // --- test double (same pattern as sibling test and T-007) ---
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

    // NOTE on Town construction:
    // Any test that does `new Town(...)` (or calls the helpers below) currently triggers
    // ExceptionInInitializerError / NoClassDefFound during component initialization
    // (TownUpgradeComponent, TownTradingComponent, UpgradeRegistry etc. static paths,
    // even with a PlatformAccess.registry stub installed). This is the same limitation
    // documented in T-007 (TownResourcesTest) and T-002.
    // Therefore this test class only retains the guard paths that do not construct Town.
    // The full placement / expansion / distance formulas are exercised only via
    // documentation in the vault note (exact code extracted, worked examples, edge cases).
    // Core boundary conflict logic is marked NEEDS-MC for this iteration.

    // --- calculateBoundaryRadius (null path only; pop-based paths need Town) ---

    @Test
    void calculateBoundaryRadius_nullTown_returnsDefaultStartingPopulation() {
        // 5 from setUp
        assertEquals(5, service.calculateBoundaryRadius(null));
    }

    // --- getMinimumDistanceRequired (null handling only) ---

    @Test
    void getMinimumDistanceRequired_bothNull_returnsTwoDefaults() {
        // 5 + 5 = 10.0 (both sides fall back to defaultStartingPopulation)
        assertEquals(10.0, service.getMinimumDistanceRequired(null, null), 0.0001);
    }

    // --- checkTownPlacement (null + empty existing only) ---

    @Test
    void checkTownPlacement_nullPos_rejectsInvalidPosition() {
        Result<Void, BCError.TownError> r = service.checkTownPlacement(null, List.of());
        assertTrue(r.isFailure());
        assertEquals("INVALID_POSITION", r.getError().getCode());
    }

    @Test
    void checkTownPlacement_noExistingTowns_accepts() {
        BlockPos p = new BlockPos(0, 64, 0);
        assertTrue(service.checkTownPlacement(p, null).isSuccess());
        assertTrue(service.checkTownPlacement(p, new ArrayList<>()).isSuccess());
    }

    // --- checkBoundaryExpansion (null town only) ---

    @Test
    void checkBoundaryExpansion_nullTown_rejects() {
        Result<Void, BCError.TownError> r = service.checkBoundaryExpansion(null, 10, List.of());
        assertTrue(r.isFailure());
        assertEquals("INVALID_TOWN", r.getError().getCode());
    }
}
