package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.production.ProductionRecipe;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.components.TownProductionComponent;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Option C: tourist system owns the tourism production recipe.
 *
 * - productions.csv defines rate/cost/cap behaviour
 * - TownProductionComponent.tick() skips "tourism"
 * - Town.tick() calls tickTourismRecipe() when touristSystemEnabled
 * - production.enabled does not gate tourist generation
 *
 * Documentation: vault/Tourists/Tourists Overview.md + tasks/todo.md Ship Plan Step 1.
 */
class TownTourismRecipeOwnershipTest {

    private static final UUID TOWN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final BlockPos TOWN_POS = new BlockPos(10, 64, 20);
    /** One game tick of progress equals this many "minutes" in recipe math (1/1200). */
    private static final float ONE_TICK_MINUTES = 1.0f / 1200.0f;

    private Town town;
    private Map<String, ProductionRecipe> savedRecipes;
    private boolean savedTouristSystemEnabled;
    private boolean savedProductionEnabled;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() throws Exception {
        savedTouristSystemEnabled = ConfigLoader.touristSystemEnabled;
        savedProductionEnabled = ConfigLoader.productionEnabled;
        ConfigLoader.touristSystemEnabled = true;
        ConfigLoader.productionEnabled = false;

        savedRecipes = snapshotRecipes();
        clearRecipes();

        town = new Town(TOWN_ID, TOWN_POS, "TourismOwnTown");
        // Unlock tourism recipe + give headroom under tourist_cap
        town.getUpgrades().addFlatModifier(TownProductionComponent.TOURISM_RECIPE_ID, 1.0f);
        town.getUpgrades().addFlatModifier("tourist_cap", 5.0f);

        putFreeTourismRecipe(ONE_TICK_MINUTES);
    }

    @AfterEach
    void tearDown() throws Exception {
        ConfigLoader.touristSystemEnabled = savedTouristSystemEnabled;
        ConfigLoader.productionEnabled = savedProductionEnabled;
        restoreRecipes(savedRecipes);
    }

    // --- registry helpers ---

    @SuppressWarnings("unchecked")
    private Map<String, ProductionRecipe> snapshotRecipes() throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField("RECIPES");
        f.setAccessible(true);
        return new HashMap<>((Map<String, ProductionRecipe>) f.get(null));
    }

    @SuppressWarnings("unchecked")
    private void restoreRecipes(Map<String, ProductionRecipe> snap) throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField("RECIPES");
        f.setAccessible(true);
        Map<String, ProductionRecipe> cur = (Map<String, ProductionRecipe>) f.get(null);
        cur.clear();
        cur.putAll(snap);
    }

    @SuppressWarnings("unchecked")
    private void clearRecipes() throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField("RECIPES");
        f.setAccessible(true);
        ((Map<String, ProductionRecipe>) f.get(null)).clear();
    }

    @SuppressWarnings("unchecked")
    private void putRecipe(ProductionRecipe r) throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField("RECIPES");
        f.setAccessible(true);
        ((Map<String, ProductionRecipe>) f.get(null)).put(r.getId(), r);
    }

    private void putFreeTourismRecipe(float cycleMinutes) throws Exception {
        putRecipe(new ProductionRecipe(
                TownProductionComponent.TOURISM_RECIPE_ID,
                "Tourist Generation",
                cycleMinutes,
                Collections.emptyList(),
                List.of(new ResourceAmount("tourist", "1")),
                Collections.emptyList()));
    }

    // --- Option C gating ---

    @Test
    void townTick_touristsOnProductionOff_grantsPendingSpawn() {
        town.tick();
        assertEquals(1, town.getPendingTouristSpawns());
    }

    @Test
    void townTick_touristsOff_grantsNothing() {
        ConfigLoader.touristSystemEnabled = false;
        ConfigLoader.productionEnabled = true; // even with production on
        town.tick();
        assertEquals(0, town.getPendingTouristSpawns());
    }

    @Test
    void productionTickAlone_doesNotRunTourismRecipe() {
        ConfigLoader.touristSystemEnabled = false;
        town.getProduction().tick();
        town.getProduction().tick();
        town.getProduction().tick();
        assertEquals(0, town.getPendingTouristSpawns());
    }

    @Test
    void townTick_bothSystemsOn_grantsOnlyOncePerCycle() {
        // Double-run guard: production.tick skips tourism; tourist path runs it once
        ConfigLoader.touristSystemEnabled = true;
        ConfigLoader.productionEnabled = true;
        town.tick();
        assertEquals(1, town.getPendingTouristSpawns());
    }

    @Test
    void tickTourismRecipe_direct_respectsUnlock() {
        // Fresh town: tourist_cap set, tourism recipe NOT unlocked
        town = new Town(TOWN_ID, TOWN_POS, "LockedTourism");
        town.getUpgrades().addFlatModifier("tourist_cap", 5.0f);
        town.getProduction().tickTourismRecipe();
        assertEquals(0, town.getPendingTouristSpawns());
    }

    @Test
    void tourism_stallsAtTouristCap() {
        town = new Town(TOWN_ID, TOWN_POS, "CapTown");
        town.getUpgrades().addFlatModifier(TownProductionComponent.TOURISM_RECIPE_ID, 1.0f);
        town.getUpgrades().addFlatModifier("tourist_cap", 2.0f);

        town.tick();
        town.tick();
        town.tick();
        town.tick();
        assertEquals(2, town.getPendingTouristSpawns());
    }

    @Test
    void tourism_countsActiveTouristsAgainstCap() {
        town = new Town(TOWN_ID, TOWN_POS, "ActiveCapTown");
        town.getUpgrades().addFlatModifier(TownProductionComponent.TOURISM_RECIPE_ID, 1.0f);
        town.getUpgrades().addFlatModifier("tourist_cap", 3.0f);
        town.setTouristCount(2);

        town.tick();
        town.tick();
        town.tick();
        assertEquals(1, town.getPendingTouristSpawns());
    }

    @Test
    void tourism_accumulatesAcrossCycles() {
        town.tick();
        town.tick();
        town.tick();
        assertEquals(3, town.getPendingTouristSpawns());
    }

    @Test
    void tourism_freeRecipeNeedsNoTownResources() {
        assertEquals(0, town.getAllResources().size());
        town.tick();
        assertEquals(1, town.getPendingTouristSpawns());
        assertEquals(0, town.getAllResources().size());
    }
}
