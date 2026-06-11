package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.util.Result;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-027: Tourist Capacity Calculation (Test + Docs Loop).
 *
 * Covers the core spawn eligibility and max-tourist rules now living in
 * TownService (canSpawnTourists + calculateMaxTourists) plus the orchestration
 * in addTourist/removeTourist. The actual cap value comes from the "tourist_cap"
 * upgrade modifier; legacy pop-based math has been replaced.
 *
 * Documentation:
 * vault/Tourists/Capacity/Tourist Capacity Calculation.md
 */
class TownServiceTest {

    private static final UUID TOWN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final BlockPos TOWN_POS = new BlockPos(100, 64, 200);
    private static final String TOWN_NAME = "CapTestTown";

    private TownService service;
    private Town town;
    private int savedMinPop;
    private int savedMaxPerTown;

    @BeforeAll
    static void boot() {
        // Town construction and some modifier paths are safe with registries initialized
        // (pattern from T-008 Town boundary tests and T-002/007 McBootstrap usage).
        com.quackers29.businesscraft.testutil.McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        service = new TownService(new TownValidationService());
        town = new Town(TOWN_ID, TOWN_POS, TOWN_NAME);

        savedMinPop = ConfigLoader.minPopForTourists;
        savedMaxPerTown = ConfigLoader.maxTouristsPerTown;

        // Deterministic baseline config (matches common test pattern)
        ConfigLoader.minPopForTourists = 5;
        ConfigLoader.maxTouristsPerTown = 30;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.minPopForTourists = savedMinPop;
        ConfigLoader.maxTouristsPerTown = savedMaxPerTown;
    }

    // --- helpers for modifier / state ---

    private void setPop(int pop) {
        town.setPopulation(pop);
    }

    private void setSpawning(boolean enabled) {
        town.setTouristSpawningEnabled(enabled);
    }

    private void setTouristCap(float value) {
        // addFlatModifier populates activeModifiers and is the simplest way to
        // inject a tourist_cap without loading full upgrade CSV or research.
        town.getUpgrades().addFlatModifier("tourist_cap", value);
    }

    // --- canSpawnTourists tests ---

    @Test
    void canSpawnTourists_enabledAndPopAtMin_returnsTrue() {
        setSpawning(true);
        setPop(5);
        // 5 >= 5 && enabled
        Result<Boolean, BCError.TownError> r = service.canSpawnTourists(town);
        assertTrue(r.isSuccess());
        assertTrue(r.getValue());
    }

    @Test
    void canSpawnTourists_enabledAndPopAboveMin_returnsTrue() {
        setSpawning(true);
        setPop(12);
        Result<Boolean, BCError.TownError> r = service.canSpawnTourists(town);
        assertTrue(r.isSuccess());
        assertTrue(r.getValue());
    }

    @Test
    void canSpawnTourists_popJustBelowMin_returnsFalse() {
        setSpawning(true);
        setPop(4);
        // 4 < 5
        Result<Boolean, BCError.TownError> r = service.canSpawnTourists(town);
        assertTrue(r.isSuccess());
        assertFalse(r.getValue());
    }

    @Test
    void canSpawnTourists_spawningDisabled_evenWithHighPop_returnsFalse() {
        setSpawning(false);
        setPop(100);
        Result<Boolean, BCError.TownError> r = service.canSpawnTourists(town);
        assertTrue(r.isSuccess());
        assertFalse(r.getValue());
    }

    @Test
    void canSpawnTourists_minPopZero_configAllowsSpawnAtPopZero() {
        ConfigLoader.minPopForTourists = 0;
        setSpawning(true);
        setPop(0);
        Result<Boolean, BCError.TownError> r = service.canSpawnTourists(town);
        assertTrue(r.isSuccess());
        assertTrue(r.getValue());
    }

    // --- calculateMaxTourists tests ---

    @Test
    void calculateMaxTourists_noModifier_returnsZero() {
        // Fresh town: tourist_cap never set -> getModifier returns 0f
        Result<Integer, BCError.TownError> r = service.calculateMaxTourists(town);
        assertTrue(r.isSuccess());
        assertEquals(0, r.getValue());
    }

    @Test
    void calculateMaxTourists_positiveIntegerModifier_returnsExactValue() {
        setTouristCap(25.0f);
        // (int) 25.0 = 25
        Result<Integer, BCError.TownError> r = service.calculateMaxTourists(town);
        assertTrue(r.isSuccess());
        assertEquals(25, r.getValue());
    }

    @Test
    void calculateMaxTourists_fractionalModifier_truncatesTowardZero() {
        setTouristCap(12.9f);
        // (int) 12.9 = 12 (truncation, not round)
        Result<Integer, BCError.TownError> r = service.calculateMaxTourists(town);
        assertTrue(r.isSuccess());
        assertEquals(12, r.getValue());
    }

    @Test
    void calculateMaxTourists_smallFractionalBelowOne_returnsZero() {
        setTouristCap(0.7f);
        // (int) 0.7 = 0
        Result<Integer, BCError.TownError> r = service.calculateMaxTourists(town);
        assertTrue(r.isSuccess());
        assertEquals(0, r.getValue());
    }

    // --- add/remove orchestration (uses the two calcs internally) ---

    @Test
    void addTourist_belowCap_succeedsAndIncrementsCount() {
        setSpawning(true);
        setPop(6);
        setTouristCap(10.0f);
        // Start at 0 < 10
        Result<Void, BCError.TownError> r = service.addTourist(town);
        assertTrue(r.isSuccess());
        assertEquals(1, town.getTouristCount());
    }

    @Test
    void addTourist_atOrAboveCap_failsWithLimitError() {
        setSpawning(true);
        setPop(10);
        setTouristCap(2.0f);
        // Force current to the cap via direct set (service path would have prevented)
        town.setTouristCount(2);
        Result<Void, BCError.TownError> r = service.addTourist(town);
        assertTrue(r.isFailure());
        assertEquals("TOURIST_LIMIT_REACHED", r.getError().getCode());
    }

    @Test
    void addTourist_spawningDisabled_failsWithSpawningDisabled() {
        setSpawning(false);
        setPop(100);
        setTouristCap(50.0f);
        Result<Void, BCError.TownError> r = service.addTourist(town);
        assertTrue(r.isFailure());
        assertEquals("SPAWNING_DISABLED", r.getError().getCode());
    }

    @Test
    void removeTourist_whenCountPositive_succeedsAndDecrements() {
        town.setTouristCount(3);
        Result<Void, BCError.TownError> r = service.removeTourist(town);
        assertTrue(r.isSuccess());
        assertEquals(2, town.getTouristCount());
    }

    @Test
    void removeTourist_whenCountZero_failsWithNoTourists() {
        town.setTouristCount(0);
        Result<Void, BCError.TownError> r = service.removeTourist(town);
        assertTrue(r.isFailure());
        assertEquals("NO_TOURISTS", r.getError().getCode());
    }

    @Test
    void canAddMoreTourists_respectsGlobalHardCap_evenWhenTouristCapHigher() {
        // Global maxTouristsPerTown=30 (from setUp), tourist_cap=100
        setSpawning(true);
        setPop(50);
        setTouristCap(100.0f);
        // Push count to the global cap
        town.setTouristCount(30);
        // The legacy Town.canAddMoreTourists still enforces the global before calling service calc
        assertFalse(town.canAddMoreTourists());
    }
}
