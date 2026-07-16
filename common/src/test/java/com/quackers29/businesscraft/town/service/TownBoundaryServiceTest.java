package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.util.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-008: Town Distance Validation — boundary math half.
 *
 * Radii are border-modifier based (ConfigLoader.defaultBorderRadius fallback).
 * Population is never used as a boundary. Placement threshold for two fresh
 * towns is defaultBorder + defaultBorder (100 with default 50).
 *
 * Documentation: vault/Town/Boundaries/Town Distance Validation.md
 */
class TownBoundaryServiceTest {

    private TownBoundaryService service;
    private int savedDefaultBorder;
    private RegistryHelper savedRegistry;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        service = new TownBoundaryService();
        savedDefaultBorder = ConfigLoader.defaultBorderRadius;
        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new TestRegistryHelper();

        ConfigLoader.defaultBorderRadius = 50;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultBorderRadius = savedDefaultBorder;
        PlatformAccess.registry = savedRegistry;
    }

    private Town makeTown(int x, int y, int z, String name) {
        return new Town(UUID.randomUUID(), new BlockPos(x, y, z), name);
    }

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

    // --- calculateBoundaryRadius ---

    @Test
    void calculateBoundaryRadius_nullTown_returnsDefaultBorderRadius() {
        assertEquals(50, service.calculateBoundaryRadius(null));
    }

    @Test
    void calculateBoundaryRadius_freshTown_returnsBorderFallback_notPopulation() {
        Town t = makeTown(10, 64, 20, "BorderTown");
        t.setPopulation(5);
        assertEquals(50, service.calculateBoundaryRadius(t));
        assertEquals(50, t.getBoundaryRadius());

        t.setPopulation(999);
        assertEquals(50, service.calculateBoundaryRadius(t)); // pop ignored
    }

    // --- getMinimumDistanceRequired ---

    @Test
    void getMinimumDistanceRequired_bothNull_returnsTwoDefaults() {
        assertEquals(100.0, service.getMinimumDistanceRequired(null, null), 0.0001);
    }

    @Test
    void getMinimumDistanceRequired_twoFreshTowns_sumsTheirBoundaryRadii() {
        Town a = makeTown(0, 64, 0, "A");
        Town b = makeTown(100, 64, 0, "B");
        assertEquals(100.0, service.getMinimumDistanceRequired(a, b), 0.0001);
    }

    @Test
    void getMinimumDistanceRequired_realTownPlusNull_usesDefaultForNullSide() {
        Town real = makeTown(0, 64, 0, "Real");
        assertEquals(100.0, service.getMinimumDistanceRequired(real, null), 0.0001);
        assertEquals(100.0, service.getMinimumDistanceRequired(null, real), 0.0001);
    }

    // --- checkTownPlacement guards ---

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

    // Fresh existing use 50; new-town side uses Config.defaultBorderRadius (50)
    // Required = 50 + 50 = 100; conflict only on strict <

    @Test
    void checkTownPlacement_realTowns_farEnough_accepts() {
        Town existing = makeTown(0, 64, 0, "Existing");
        BlockPos far = new BlockPos(150, 64, 0); // dist 150 > 100
        assertTrue(service.checkTownPlacement(far, List.of(existing)).isSuccess());
    }

    @Test
    void checkTownPlacement_realTowns_exactlyAtThreshold_accepts() {
        Town existing = makeTown(0, 64, 0, "Existing");
        BlockPos atLimit = new BlockPos(0, 64, 100); // dist=100; 100 < 100? false
        assertTrue(service.checkTownPlacement(atLimit, List.of(existing)).isSuccess());
    }

    @Test
    void checkTownPlacement_realTowns_oneBlockTooClose_conflicts() {
        Town existing = makeTown(0, 64, 0, "Existing");
        BlockPos tooClose = new BlockPos(0, 64, 99); // dist=99 < 100
        Result<Void, BCError.TownError> r = service.checkTownPlacement(tooClose, List.of(existing));
        assertTrue(r.isFailure());
        assertEquals("BOUNDARY_CONFLICT", r.getError().getCode());
    }

    @Test
    void checkTownPlacement_realTowns_samePosition_conflicts() {
        Town existing = makeTown(5, 64, 5, "Existing");
        Result<Void, BCError.TownError> r = service.checkTownPlacement(new BlockPos(5, 64, 5), List.of(existing));
        assertTrue(r.isFailure());
        assertEquals("BOUNDARY_CONFLICT", r.getError().getCode());
    }

    @Test
    void checkTownPlacement_realTowns_multipleExisting_firstConflictShortCircuits() {
        Town far1 = makeTown(0, 64, 1000, "Far1");
        Town close = makeTown(0, 64, 0, "Close");
        Town far2 = makeTown(0, 64, 2000, "Far2");
        BlockPos p = new BlockPos(1, 64, 1);
        Result<Void, BCError.TownError> r = service.checkTownPlacement(p, List.of(far1, close, far2));
        assertTrue(r.isFailure());
        assertEquals("BOUNDARY_CONFLICT", r.getError().getCode());
    }

    @Test
    void checkTownPlacement_customDefaultBorderRadius_affectsNewSideThreshold() {
        int saved = ConfigLoader.defaultBorderRadius;
        try {
            ConfigLoader.defaultBorderRadius = 60; // new side 60; fresh town still falls back to 60 via getBoundaryRadius
            // Existing town was constructed while default was 50 — force its border via modifier path:
            // with defaultBorderRadius=60, fresh getBoundaryRadius() also returns 60.
            Town existing = makeTown(0, 64, 0, "Existing");
            assertEquals(60, existing.getBoundaryRadius());
            // req = 60 + 60 = 120
            assertTrue(service.checkTownPlacement(new BlockPos(0, 64, 119), List.of(existing)).isFailure());
            assertTrue(service.checkTownPlacement(new BlockPos(0, 64, 120), List.of(existing)).isSuccess());
        } finally {
            ConfigLoader.defaultBorderRadius = saved;
        }
    }

    // --- checkBorderExpansion ---

    @Test
    void checkBorderExpansion_nullTown_rejects() {
        Result<Void, BCError.TownError> r = service.checkBorderExpansion(null, 10, List.of());
        assertTrue(r.isFailure());
        assertEquals("INVALID_TOWN", r.getError().getCode());
    }

    @Test
    void checkBorderExpansion_noIncrease_noOpSuccess() {
        Town t = makeTown(0, 64, 0, "Static");
        // current border fallback 50
        assertTrue(service.checkBorderExpansion(t, 50, List.of()).isSuccess());
        assertTrue(service.checkBorderExpansion(t, 40, List.of()).isSuccess());
    }

    @Test
    void checkBorderExpansion_increaseButFarEnough_accepts() {
        Town expander = makeTown(0, 64, 0, "Expander");
        Town other = makeTown(0, 64, 200, "Other"); // dist=200
        // propose 60; other 50; req=110; 200 < 110? no
        assertTrue(service.checkBorderExpansion(expander, 60, List.of(other)).isSuccess());
    }

    @Test
    void checkBorderExpansion_increaseTooClose_conflicts() {
        Town expander = makeTown(0, 64, 0, "Expander");
        Town other = makeTown(0, 64, 90, "Other"); // dist=90
        // propose 60 + other 50 = 110; 90 < 110 → conflict
        Result<Void, BCError.TownError> r = service.checkBorderExpansion(expander, 60, List.of(other));
        assertTrue(r.isFailure());
        assertEquals("EXPANSION_CONFLICT", r.getError().getCode());
    }

    @Test
    void checkBorderExpansion_collectionIncludesSelf_skipsById_noSelfConflict() {
        Town expander = makeTown(10, 64, 10, "Self");
        assertTrue(service.checkBorderExpansion(expander, 1000, List.of(expander)).isSuccess());
    }

    @Test
    void checkBorderExpansion_growthNearNeighbor_conflicts() {
        Town expander = makeTown(0, 64, 0, "Home");
        Town neighbor = makeTown(0, 64, 80, "Neighbor"); // dist=80
        // propose 60 + neighbor 50 = 110; 80 < 110 → conflict
        Result<Void, BCError.TownError> r = service.checkBorderExpansion(expander, 60, List.of(neighbor));
        assertTrue(r.isFailure());
        assertEquals("EXPANSION_CONFLICT", r.getError().getCode());
    }
}
