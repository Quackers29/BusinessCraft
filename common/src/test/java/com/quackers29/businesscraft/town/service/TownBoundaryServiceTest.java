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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-008: Town Distance Validation (Test + Docs Loop) — boundary math half.
 *
 * Covers TownBoundaryService:
 *   calculateBoundaryRadius (pop 1:1 or default on null; distinct from Town.getBoundaryRadius())
 *   checkTownPlacement (new-town defaultStartingPopulation + existing.getBoundaryRadius(),
 *                       euclidean via sqrt(distSqr), < is conflict, equal allowed)
 *   getMinimumDistanceRequired (null handling + sum of radii)
 *   checkBoundaryExpansion (newPopulation as proposed radius on expander side,
 *                           others use their getBoundaryRadius(), skip-self by id,
 *                           no-op when not actually increasing pop)
 *
 * With McBootstrap, real Town instances (public 3-arg ctor) are constructible.
 * Fresh towns have getBoundaryRadius()=50 (border upgrade fallback); placement
 * math therefore uses 5 (defaultStartingPop for "new") + 50 = 55 threshold.
 * Service's calculateBoundaryRadius follows population (settable for tests).
 *
 * Documentation: vault/Town/Boundaries/Town Distance Validation.md
 */
class TownBoundaryServiceTest {

    private TownBoundaryService service;
    private int savedDefaultPop;
    private RegistryHelper savedRegistry;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

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

    // --- helpers (real Town construction now possible with McBootstrap) ---
    private Town makeTown(int x, int y, int z, String name) {
        return new Town(UUID.randomUUID(), new BlockPos(x, y, z), name);
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

    // McBootstrap (in @BeforeAll) + the vanilla Bootstrap now allows `new Town(UUID, BlockPos, name)`
    // to succeed for these tests (components initialize without crashing). The TestRegistryHelper
    // stub is retained for @BeforeEach determinism and any PlatformAccess paths hit during
    // Town construction or component setup. Config defaultStartingPopulation is forced to 5.

    // --- calculateBoundaryRadius (null + real Town paths) ---

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

    // ============================================================
    // calculateBoundaryRadius with real Town (pop 1:1 path)
    // ============================================================

    @Test
    void calculateBoundaryRadius_realTown_returnsPopulationAsInt() {
        Town t = makeTown(10, 64, 20, "PopTown");
        // default ctor sets pop = defaultStartingPopulation = 5
        assertEquals(5, service.calculateBoundaryRadius(t));
    }

    @Test
    void calculateBoundaryRadius_afterSetPopulation_returnsNewPop_notBorderRadius() {
        Town t = makeTown(0, 64, 0, "GrowTown");
        t.setPopulation(0);
        assertEquals(0, service.calculateBoundaryRadius(t)); // service uses getPopulation()

        t.setPopulation(42);
        assertEquals(42, service.calculateBoundaryRadius(t));

        // Contrast: Town's own getBoundaryRadius() is still the upgrade "border" (50 for fresh)
        // This discrepancy is intentional in current design (documented in vault note).
        assertEquals(50, t.getBoundaryRadius());
    }

    // ============================================================
    // getMinimumDistanceRequired with real Towns
    // ============================================================

    @Test
    void getMinimumDistanceRequired_twoFreshTowns_sumsTheirBoundaryRadii() {
        Town a = makeTown(0, 64, 0, "A");
        Town b = makeTown(100, 64, 0, "B");
        // Both fresh → getBoundaryRadius()=50 each (border fallback)
        // 50 + 50 = 100.0
        assertEquals(100.0, service.getMinimumDistanceRequired(a, b), 0.0001);
    }

    @Test
    void getMinimumDistanceRequired_realTownPlusNull_usesDefaultForNullSide() {
        Town real = makeTown(0, 64, 0, "Real");
        // real (50) + null (default 5) = 55.0
        assertEquals(55.0, service.getMinimumDistanceRequired(real, null), 0.0001);
        // symmetric
        assertEquals(55.0, service.getMinimumDistanceRequired(null, real), 0.0001);
    }

    // ============================================================
    // checkTownPlacement with real existing Towns (core distance math)
    // Fresh existing use 50; new-town side always uses Config.defaultStartingPopulation (5 here)
    // Required = 5 + 50 = 55; conflict only on strict <
    // ============================================================

    @Test
    void checkTownPlacement_realTowns_farEnough_accepts() {
        Town existing = makeTown(0, 64, 0, "Existing");
        // Place new at euclidean distance 100 (>55)
        // dist = 100; req=5+50=55; 100 < 55? no → success
        BlockPos far = new BlockPos(100, 64, 0);
        assertTrue(service.checkTownPlacement(far, List.of(existing)).isSuccess());
    }

    @Test
    void checkTownPlacement_realTowns_exactlyAtThreshold_accepts() {
        Town existing = makeTown(0, 64, 0, "Existing");
        // dist exactly 55 (e.g. along Z)
        // sqrt(0+0+55*55) = 55; 55 < (5+50)=55 ? false → allowed (only < conflicts)
        BlockPos atLimit = new BlockPos(0, 64, 55);
        assertTrue(service.checkTownPlacement(atLimit, List.of(existing)).isSuccess());
    }

    @Test
    void checkTownPlacement_realTowns_oneBlockTooClose_conflicts() {
        Town existing = makeTown(0, 64, 0, "Existing");
        // dist=54 < 55 → BOUNDARY_CONFLICT
        BlockPos tooClose = new BlockPos(0, 64, 54);
        Result<Void, BCError.TownError> r = service.checkTownPlacement(tooClose, List.of(existing));
        assertTrue(r.isFailure());
        assertEquals("BOUNDARY_CONFLICT", r.getError().getCode());
    }

    @Test
    void checkTownPlacement_realTowns_samePosition_conflicts() {
        Town existing = makeTown(5, 64, 5, "Existing");
        // dist=0 < 55 → conflict
        Result<Void, BCError.TownError> r = service.checkTownPlacement(new BlockPos(5, 64, 5), List.of(existing));
        assertTrue(r.isFailure());
        assertEquals("BOUNDARY_CONFLICT", r.getError().getCode());
    }

    @Test
    void checkTownPlacement_realTowns_multipleExisting_firstConflictShortCircuits() {
        Town far1 = makeTown(0, 64, 1000, "Far1");
        Town close = makeTown(0, 64, 0, "Close");
        Town far2 = makeTown(0, 64, 2000, "Far2");
        BlockPos p = new BlockPos(1, 64, 1); // dist ~1.4 to "Close" → conflict
        Result<Void, BCError.TownError> r = service.checkTownPlacement(p, List.of(far1, close, far2));
        assertTrue(r.isFailure());
        assertEquals("BOUNDARY_CONFLICT", r.getError().getCode());
        // error message should reference the close town's name (implementation detail, not asserted strictly)
    }

    @Test
    void checkTownPlacement_customDefaultStartingPopulation_affectsNewSideThreshold() {
        // Temporarily raise new-town boundary contribution
        int saved = ConfigLoader.defaultStartingPopulation;
        try {
            ConfigLoader.defaultStartingPopulation = 10; // new side now 10
            Town existing = makeTown(0, 64, 0, "Existing"); // still 50
            // req = 10 + 50 = 60
            // place at dist 59 → conflict; 60 → ok
            BlockPos d59 = new BlockPos(0, 64, 59);
            assertTrue(service.checkTownPlacement(d59, List.of(existing)).isFailure());
            BlockPos d60 = new BlockPos(0, 64, 60);
            assertTrue(service.checkTownPlacement(d60, List.of(existing)).isSuccess());
        } finally {
            ConfigLoader.defaultStartingPopulation = saved;
        }
    }

    // ============================================================
    // checkBoundaryExpansion with real Towns
    // Uses *newPopulation* directly as proposed radius for the expander (1:1 comment in code)
    // Other towns contribute their current getBoundaryRadius() (50 for fresh)
    // Skips self by id; no-op if newPop <= current pop
    // ============================================================

    @Test
    void checkBoundaryExpansion_noIncrease_noOpSuccess() {
        Town t = makeTown(0, 64, 0, "Static");
        t.setPopulation(12);
        // newPopulation == current → success, no distance math
        assertTrue(service.checkBoundaryExpansion(t, 12, List.of()).isSuccess());
        assertTrue(service.checkBoundaryExpansion(t, 11, List.of()).isSuccess()); // <
    }

    @Test
    void checkBoundaryExpansion_increaseButFarEnough_accepts() {
        Town expander = makeTown(0, 64, 0, "Expander");
        expander.setPopulation(5);
        Town other = makeTown(0, 64, 200, "Other"); // dist=200
        // propose pop 30 → newB=30 (per code: int newBoundaryRadius = newPopulation;)
        // otherB=50; req=80; 200 < 80 ? no → ok
        assertTrue(service.checkBoundaryExpansion(expander, 30, List.of(other)).isSuccess());
    }

    @Test
    void checkBoundaryExpansion_increaseTooClose_conflicts() {
        Town expander = makeTown(0, 64, 0, "Expander");
        expander.setPopulation(5);
        Town other = makeTown(0, 64, 60, "Other"); // dist=60
        // newPop=20 → newB=20 + other 50 = 70; 60 < 70 → EXPANSION_CONFLICT
        Result<Void, BCError.TownError> r = service.checkBoundaryExpansion(expander, 20, List.of(other));
        assertTrue(r.isFailure());
        assertEquals("EXPANSION_CONFLICT", r.getError().getCode());
    }

    @Test
    void checkBoundaryExpansion_collectionIncludesSelf_skipsById_noSelfConflict() {
        Town expander = makeTown(10, 64, 10, "Self");
        expander.setPopulation(5);
        // Even though dist to self=0, the code does: if (other.getId().equals(expanding.getId())) continue;
        // So proposing a huge pop (newB=1000) against a collection containing self should still pass
        // (the only other is self, which is skipped).
        assertTrue(service.checkBoundaryExpansion(expander, 1000, List.of(expander)).isSuccess());
    }

    @Test
    void checkBoundaryExpansion_realisticGrowthFrom5to10_withCloseNeighbor_conflicts() {
        Town expander = makeTown(0, 64, 0, "Home");
        expander.setPopulation(5); // current getBoundaryRadius()=50 but expansion uses proposed pop as radius
        Town neighbor = makeTown(0, 64, 40, "Neighbor"); // dist=40
        // Growing to pop=10: newB=10 + neighborB=50 = 60; 40 < 60 → conflict
        Result<Void, BCError.TownError> r = service.checkBoundaryExpansion(expander, 10, List.of(neighbor));
        assertTrue(r.isFailure());
        assertEquals("EXPANSION_CONFLICT", r.getError().getCode());
    }
}
