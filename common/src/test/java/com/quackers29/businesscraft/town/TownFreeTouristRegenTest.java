package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.components.TownUpgradeComponent;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Free tourist regeneration for tourism-only mode (0.9 Ship Plan Step 1).
 *
 * Covers Town.tickFreeTouristRegen(): when the tourist system is enabled and
 * production is DISABLED, a town gains 1 pending tourist spawn every
 * FREE_TOURIST_REGEN_INTERVAL_TICKS (240), capped so that
 * touristCount + pendingTouristSpawns never exceeds the tourist_cap modifier.
 * No resources are consumed. When production is enabled the path is inert
 * (the tourism recipe handles generation, at food+money cost).
 *
 * tourist_cap is injected via reflection into TownUpgradeComponent's
 * activeModifiers map (same pattern as TownTest.setWuCap) — no production
 * code changed for testing.
 * Documentation: vault/Tourists/... + tasks/todo.md Ship Plan Step 1.
 */
class TownFreeTouristRegenTest {

    private static final UUID TOWN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final BlockPos TOWN_POS = new BlockPos(0, 64, 0);
    private static final int INTERVAL = Town.FREE_TOURIST_REGEN_INTERVAL_TICKS;

    private Town town;
    private int savedDefaultPop;
    private boolean savedTouristSystemEnabled;
    private boolean savedProductionEnabled;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        savedTouristSystemEnabled = ConfigLoader.touristSystemEnabled;
        savedProductionEnabled = ConfigLoader.productionEnabled;

        ConfigLoader.defaultStartingPopulation = 5;
        // Tourism-only mode: the configuration this path exists for
        ConfigLoader.touristSystemEnabled = true;
        ConfigLoader.productionEnabled = false;

        town = new Town(TOWN_ID, TOWN_POS, "RegenTown");
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
        ConfigLoader.touristSystemEnabled = savedTouristSystemEnabled;
        ConfigLoader.productionEnabled = savedProductionEnabled;
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private void setTouristCap(float capValue) {
        try {
            Field upgradesField = Town.class.getDeclaredField("upgrades");
            upgradesField.setAccessible(true);
            TownUpgradeComponent upgrades = (TownUpgradeComponent) upgradesField.get(town);

            Field modsField = TownUpgradeComponent.class.getDeclaredField("activeModifiers");
            modsField.setAccessible(true);
            Map<String, Float> mods = (Map<String, Float>) modsField.get(upgrades);
            mods.put("tourist_cap", capValue);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inject tourist_cap", e);
        }
    }

    private void regenTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            town.tickFreeTouristRegen();
        }
    }

    // --- happy path: tourism-only mode regenerates pending spawns ---

    @Test
    void regen_grantsOnePendingSpawnPerInterval() {
        setTouristCap(5);
        regenTicks(INTERVAL);
        // Exactly one full interval -> one pending spawn
        assertEquals(1, town.getPendingTouristSpawns());
    }

    @Test
    void regen_nothingBeforeIntervalElapses() {
        setTouristCap(5);
        regenTicks(INTERVAL - 1);
        assertEquals(0, town.getPendingTouristSpawns());
        regenTicks(1); // 240th tick completes the interval
        assertEquals(1, town.getPendingTouristSpawns());
    }

    @Test
    void regen_accumulatesUpToCapThenStops() {
        setTouristCap(3);
        regenTicks(INTERVAL * 10); // enough for 10 grants, cap allows 3
        assertEquals(3, town.getPendingTouristSpawns());
    }

    @Test
    void regen_countsActiveTouristsAgainstCap() {
        setTouristCap(3);
        town.setTouristCount(2); // 2 already out in the world
        regenTicks(INTERVAL * 10);
        // 2 active + 1 pending = cap of 3
        assertEquals(1, town.getPendingTouristSpawns());
    }

    @Test
    void regen_resumesWhenTouristsLeave() {
        setTouristCap(2);
        regenTicks(INTERVAL * 5);
        assertEquals(2, town.getPendingTouristSpawns());

        // Simulate both pending spawns becoming active tourists, then one arriving
        // at a destination (removed from origin's count)
        town.setPendingTouristSpawns(0);
        town.setTouristCount(2);
        regenTicks(INTERVAL * 5);
        assertEquals(0, town.getPendingTouristSpawns()); // still full

        town.setTouristCount(1); // one tourist arrived somewhere
        regenTicks(INTERVAL);
        assertEquals(1, town.getPendingTouristSpawns()); // slot refilled
    }

    // --- gating ---

    @Test
    void regen_inertWhenProductionEnabled() {
        // Full mod: the tourism recipe (food+money cost) owns tourist generation
        ConfigLoader.productionEnabled = true;
        setTouristCap(5);
        regenTicks(INTERVAL * 10);
        assertEquals(0, town.getPendingTouristSpawns());
    }

    @Test
    void regen_inertWhenTouristSystemDisabled() {
        ConfigLoader.touristSystemEnabled = false;
        setTouristCap(5);
        regenTicks(INTERVAL * 10);
        assertEquals(0, town.getPendingTouristSpawns());
    }

    @Test
    void regen_inertWhenPerTownSpawningDisabled() {
        setTouristCap(5);
        town.setTouristSpawningEnabled(false);
        regenTicks(INTERVAL * 10);
        assertEquals(0, town.getPendingTouristSpawns());
    }

    @Test
    void regen_resumesAfterPerTownSpawningReenabled() {
        setTouristCap(5);
        town.setTouristSpawningEnabled(false);
        regenTicks(INTERVAL * 3);
        assertEquals(0, town.getPendingTouristSpawns());

        town.setTouristSpawningEnabled(true);
        regenTicks(INTERVAL);
        assertEquals(1, town.getPendingTouristSpawns());
    }

    @Test
    void regen_zeroCap_grantsNothing() {
        // No tourist_cap modifier (fresh town without biome kit / basic_settlement) -> cap 0
        regenTicks(INTERVAL * 10);
        assertEquals(0, town.getPendingTouristSpawns());
    }

    // --- no resource cost ---

    @Test
    void regen_consumesNoResources() {
        setTouristCap(5);
        // Town starts with zero resources; regen must work anyway and not go negative
        regenTicks(INTERVAL * 5);
        assertEquals(5, town.getPendingTouristSpawns());
        assertEquals(0, town.getAllResources().size());
    }
}
