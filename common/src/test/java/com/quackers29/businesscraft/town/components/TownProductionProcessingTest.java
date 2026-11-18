package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.testutil.ProductionTestEnv;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tick-driven tests of TownProductionComponent recipe processing against real registries loaded from
 * CSV: cycle timing, consume and produce, upgrade speed, conditions, expression amounts, output
 * stalls, starvation, special outputs (pop, tourist, wu, stat) and NBT.
 *
 * Cycle times in the test CSVs are chosen half a tick below a whole number of ticks
 * (1 tick = 1/1200 minute) so float rounding cannot move the completion tick.
 */
class TownProductionProcessingTest {

    private static final String HEADER = "prod_id,display_name,base_cycle_time_minutes,inputs,outputs\n";
    /** 6 ticks. */
    private static final String SIX = "0.0046";

    @TempDir
    Path tempDir;

    private ProductionTestEnv env;
    private Town town;
    private TownProductionComponent comp;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        env = ProductionTestEnv.install(tempDir);
        env.loadBundledItems(); // wood, food, money, planks, ...
        ConfigLoader.minStockPercent = 60;
        ConfigLoader.excessStockPercent = 80;
        town = new Town(UUID.fromString("44444444-4444-4444-4444-444444444444"), new BlockPos(0, 64, 0), "ProcTown");
        comp = town.getProduction();
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private void recipes(String rows) throws Exception {
        env.writeCsv("productions.csv", HEADER + rows);
        ProductionRegistry.load();
    }

    private void unlock(String... recipeIds) {
        for (String id : recipeIds) {
            town.getUpgrades().addFlatModifier(id, 1f);
        }
    }

    private void tick(int n) {
        for (int i = 0; i < n; i++) {
            comp.tick();
        }
    }

    /** Tourism is ticked by the tourist system, not by tick() (Option C). */
    private void tourismTicks(int n) {
        for (int i = 0; i < n; i++) {
            comp.tickTourismRecipe();
        }
    }

    private long count(Item item) {
        return town.getResourceCount(item);
    }

    // --- cycle timing, consume, produce ---

    @Test
    void recipe_completesOnTheCycleTick_andProgressResets() throws Exception {
        recipes("farm,Farm," + SIX + ",,food:4\n");
        unlock("farm");

        tick(5);
        assertEquals(0, count(Items.BREAD));
        assertEquals(5f / 1200f / Float.parseFloat(SIX), comp.getActiveRecipes().get("farm"), 1e-3f);

        tick(1);
        assertEquals(4, count(Items.BREAD));
        assertEquals(0f, comp.getActiveRecipes().get("farm"), 1e-6f, "progress resets after a cycle");

        tick(6);
        assertEquals(8, count(Items.BREAD));
    }

    @Test
    void lockedRecipe_neverRuns() throws Exception {
        recipes("farm,Farm," + SIX + ",,food:4\n");
        tick(30);
        assertEquals(0, count(Items.BREAD));
        assertTrue(comp.getActiveRecipes().isEmpty());
    }

    @Test
    void upgradeModifierIsTheSpeedMultiplier() throws Exception {
        recipes("farm,Farm," + SIX + ",,food:4\n");
        town.getUpgrades().addFlatModifier("farm", 2f); // double speed: 3 ticks per cycle

        tick(3);
        assertEquals(4, count(Items.BREAD));
        tick(3);
        assertEquals(8, count(Items.BREAD));
    }

    @Test
    void inputsAreConsumedAndOutputsProduced() throws Exception {
        recipes("mill,Mill," + SIX + ",wood:4,planks:16\n");
        unlock("mill");
        town.addResource(Items.OAK_LOG, 10);

        tick(6);

        assertEquals(6, count(Items.OAK_LOG));
        assertEquals(16, count(Items.OAK_PLANKS));
    }

    @Test
    void missingInput_blocksProgressAndResetsIt() throws Exception {
        recipes("mill,Mill," + SIX + ",wood:4,planks:16\n");
        unlock("mill");
        town.addResource(Items.OAK_LOG, 3);

        tick(10);
        assertEquals(3, count(Items.OAK_LOG));
        assertEquals(0, count(Items.OAK_PLANKS));
        assertEquals(0f, comp.getActiveRecipes().get("mill"), 1e-6f);

        // partial progress is lost when an input disappears mid-cycle
        town.addResource(Items.OAK_LOG, 1);
        tick(3);
        assertTrue(comp.getActiveRecipes().get("mill") > 0f);
        town.addResource(Items.OAK_LOG, -4);
        tick(1);
        assertEquals(0f, comp.getActiveRecipes().get("mill"), 1e-6f);
    }

    @Test
    void multipleOutputsAndInputs_allApplied() throws Exception {
        recipes("combo,Combo," + SIX + ",wood:2;food:1,planks:3;money:5\n");
        unlock("combo");
        town.addResource(Items.OAK_LOG, 2);
        town.addResource(Items.BREAD, 1);

        tick(6);

        assertEquals(0, count(Items.OAK_LOG));
        assertEquals(0, count(Items.BREAD));
        assertEquals(3, count(Items.OAK_PLANKS));
        assertEquals(5, count(Items.EMERALD));
    }

    // --- expression amounts ---

    @Test
    void outputAmountExpression_scalesWithPopulation() throws Exception {
        recipes("taxes,Taxes," + SIX + ",,money:2*pop\n");
        unlock("taxes");
        town.setPopulation(7);

        tick(6);

        assertEquals(14, count(Items.EMERALD));
    }

    @Test
    void inputAmountExpression_scalesWithPopulation_andIsRequired() throws Exception {
        recipes("eat,Eat," + SIX + ",food:1*pop,money:1\n");
        unlock("eat");
        town.setPopulation(5);
        town.addResource(Items.BREAD, 4);

        tick(12);
        assertEquals(4, count(Items.BREAD), "needs 5 food for 5 pop");
        assertEquals(0, count(Items.EMERALD));

        town.addResource(Items.BREAD, 3);
        tick(6);
        assertEquals(2, count(Items.BREAD));
        assertEquals(1, count(Items.EMERALD));
    }

    @Test
    void amountExpression_usesUpgradeModifier() throws Exception {
        recipes("dig,Dig," + SIX + ",,wood:1*bonus\n");
        unlock("dig");
        town.getUpgrades().addFlatModifier("bonus", 3f);

        tick(6);

        assertEquals(3, count(Items.OAK_LOG));
    }

    @Test
    void unknownExpressionToken_makesOutputZero_whichValidationReports() throws Exception {
        recipes("typo,Typo," + SIX + ",,wood:1*popp\n");
        unlock("typo");
        town.setPopulation(5);

        tick(6);

        assertEquals(0, count(Items.OAK_LOG), "unknown token evaluates to 0");
    }

    // --- conditions ---

    @Test
    void conditionNotMet_resetsProgress_conditionMet_runs() throws Exception {
        recipes("grow,Grow," + SIX + ",pop:>10,food:1\n");
        unlock("grow");
        town.setPopulation(10);

        tick(12);
        assertEquals(0, count(Items.BREAD));

        town.setPopulation(11);
        tick(6);
        assertEquals(1, count(Items.BREAD));
    }

    @Test
    void minCondition_usesConfigPercentOfStorageCap() throws Exception {
        recipes("mill,Mill," + SIX + ",wood:1;min,planks:1\n");
        unlock("mill");
        town.addResource(Items.OAK_LOG, 599); // cap 1000, min 60% -> needs > 600

        tick(6);
        assertEquals(0, count(Items.OAK_PLANKS));

        town.addResource(Items.OAK_LOG, 2);
        tick(6);
        assertEquals(1, count(Items.OAK_PLANKS));
    }

    @Test
    void surplusCondition_requiresProductionAboveConsumption() throws Exception {
        recipes("farm,Farm,1,,food:10\n"
                + "mouths,Mouths,1,food:20,money:1\n"
                + "dep,Dep," + SIX + ",surplus:food,wood:1\n");
        unlock("farm", "dep");
        tick(6);
        assertEquals(1, count(Items.OAK_LOG), "farm alone is a surplus");

        unlock("mouths"); // consumption 20/min vs production 10/min
        tick(6);
        assertEquals(1, count(Items.OAK_LOG), "no surplus once mouths run");
    }

    // --- output stalls ---

    @Test
    void outputFull_stallsAndKeepsProgress() throws Exception {
        recipes("farm,Farm," + SIX + ",,food:4\n");
        unlock("farm");
        town.getUpgrades().addFlatModifier("storage_cap_all", 10f); // cap 10
        town.addResource(Items.BREAD, 8);

        tick(10);
        assertEquals(8, count(Items.BREAD), "8 + 4 would exceed the cap of 10");

        // space frees up: the recipe resumes
        town.addResource(Items.BREAD, -4);
        tick(6);
        assertEquals(8, count(Items.BREAD));
    }

    @Test
    void outputStall_afterPartialProgress_keepsThatProgress() throws Exception {
        recipes("farm,Farm," + SIX + ",,food:4\n");
        unlock("farm");
        town.getUpgrades().addFlatModifier("storage_cap_all", 10f);
        tick(3);
        float before = comp.getActiveRecipes().get("farm");
        assertTrue(before > 0f);

        town.addResource(Items.BREAD, 8); // now full for a 4-unit output
        tick(5);

        assertEquals(before, comp.getActiveRecipes().get("farm"), 1e-6f);
        assertEquals(8, count(Items.BREAD));
    }

    @Test
    void touristOutput_stallsAtTouristCap_andWhenSpawningDisabled() throws Exception {
        recipes("tourism,Tourism," + SIX + ",,tourist:1\n");
        unlock("tourism");
        town.getUpgrades().addFlatModifier("tourist_cap", 2f);
        town.setTouristSpawningEnabled(true);

        tourismTicks(6);
        assertEquals(1, town.getPendingTouristSpawns());
        tourismTicks(6);
        assertEquals(2, town.getPendingTouristSpawns());
        tourismTicks(12);
        assertEquals(2, town.getPendingTouristSpawns(), "stalled at tourist_cap");
    }

    @Test
    void touristOutput_notProducedWhenSpawningDisabled() throws Exception {
        recipes("tourism,Tourism," + SIX + ",,tourist:1\n");
        unlock("tourism");
        town.getUpgrades().addFlatModifier("tourist_cap", 5f);
        town.setTouristSpawningEnabled(true);
        for (int i = 0; i < 6; i++) {
            comp.tickTourismRecipe();
        }
        assertEquals(1, town.getPendingTouristSpawns());

        town.setTouristSpawningEnabled(false);
        for (int i = 0; i < 12; i++) {
            comp.tickTourismRecipe();
        }
        assertEquals(1, town.getPendingTouristSpawns());
    }

    // --- special inputs and outputs ---

    @Test
    void workUnits_areConsumedAndProduced_andCappedByWuCap() throws Exception {
        recipes("make,Make," + SIX + ",,wu:5\nuse,Use," + SIX + ",wu:3,wood:1\n");
        unlock("make");
        town.getUpgrades().addFlatModifier("wu_cap", 7f);

        tick(6);
        assertEquals(5, town.getWorkUnits());
        tick(12);
        assertEquals(5, town.getWorkUnits(), "5 + 5 > wu_cap 7 stalls");

        unlock("use");
        tick(6);
        assertEquals(2, town.getWorkUnits() , "make stalled, use consumed 3");
        assertEquals(1, count(Items.OAK_LOG));
    }

    @Test
    void populationOutput_addsToPopulation() throws Exception {
        recipes("grow,Grow," + SIX + ",,population:2\n");
        unlock("grow");
        town.setPopulation(10);

        tick(6);

        assertEquals(12, town.getPopulation());
    }

    @Test
    void statOutput_accumulatesIntoUpgradeModifier() throws Exception {
        recipes("expand,Expand," + SIX + ",,border:1\n");
        unlock("expand");

        tick(6);
        assertEquals(1f, town.getUpgrades().getModifier("border"), 1e-6f);
        tick(6);
        assertEquals(2f, town.getUpgrades().getModifier("border"), 1e-6f);
    }

    // --- starvation ---

    @Test
    void population_maintenance_withFood_consumesFoodAndKeepsPopulation() throws Exception {
        recipes("population_maintenance,Food Consumption," + SIX + ",food:1*pop,\n");
        unlock("population_maintenance");
        town.setPopulation(4);
        town.addResource(Items.BREAD, 10);

        tick(6);

        assertEquals(6, count(Items.BREAD));
        assertEquals(4, town.getPopulation());
    }

    @Test
    void population_maintenance_withoutFood_killsOneVillagerPerCycle() throws Exception {
        recipes("population_maintenance,Food Consumption," + SIX + ",food:1*pop,\n");
        unlock("population_maintenance");
        town.setPopulation(4);

        tick(5);
        assertEquals(4, town.getPopulation());
        tick(1);
        assertEquals(3, town.getPopulation());
        tick(6);
        assertEquals(2, town.getPopulation());
    }

    @Test
    void population_maintenance_starvation_eatsTheRemainingPartialStack() throws Exception {
        recipes("population_maintenance,Food Consumption," + SIX + ",food:1*pop,\n");
        unlock("population_maintenance");
        town.setPopulation(5);
        town.addResource(Items.BREAD, 2); // needs 5

        tick(6);

        assertEquals(4, town.getPopulation());
        assertEquals(0, count(Items.BREAD));
    }

    @Test
    void starvation_neverGoesBelowZeroPopulation() throws Exception {
        recipes("population_maintenance,Food Consumption," + SIX + ",food:1*pop,\n");
        unlock("population_maintenance");
        town.setPopulation(1);

        tick(30);

        assertEquals(0, town.getPopulation());
    }

    @Test
    void otherRecipesWithMissingInputs_doNotStarve() throws Exception {
        recipes("eat,Eat," + SIX + ",food:1*pop,money:1\n");
        unlock("eat");
        town.setPopulation(4);

        tick(30);

        assertEquals(4, town.getPopulation());
    }

    // --- happiness ---

    @Test
    void happiness_followsFoodStockAgainstMinStockLevel() throws Exception {
        recipes("noop,Noop,1,,food:1\n");
        // cap 1000, min 60% -> 600 food = full base 50
        town.addResource(Items.BREAD, 600);
        tick(20);
        assertEquals(50f, town.getHappiness(), 1e-3f);

        town.addResource(Items.BREAD, -300); // half of the min level -> 25
        tick(20);
        assertEquals(25f, town.getHappiness(), 1e-3f);

        town.addResource(Items.BREAD, 1000); // above the min level is clamped at 50
        tick(20);
        assertEquals(50f, town.getHappiness(), 1e-3f);
    }

    @Test
    void happiness_modifierFromUpgradesIsAdded() throws Exception {
        recipes("noop,Noop,1,,food:1\n");
        town.getUpgrades().addFlatModifier("happiness", 50f);
        town.addResource(Items.BREAD, 600);
        tick(20);
        assertEquals(100f, town.getHappiness(), 1e-3f);
    }

    // --- persistence ---

    @Test
    void progressSurvivesNbtRoundTrip() throws Exception {
        recipes("farm,Farm," + SIX + ",,food:4\n");
        unlock("farm");
        tick(3);
        float progress = comp.getActiveRecipes().get("farm");

        CompoundTag tag = new CompoundTag();
        comp.save(tag);

        TownProductionComponent restored = new TownProductionComponent(town);
        restored.load(tag);

        assertEquals(progress, restored.getActiveRecipes().get("farm"), 1e-6f);
    }

    @Test
    void load_withNoSavedProgress_clearsPreviousProgress() throws Exception {
        recipes("farm,Farm," + SIX + ",,food:4\n");
        unlock("farm");
        tick(3);
        comp.load(new CompoundTag());
        assertTrue(comp.getActiveRecipes().isEmpty());
    }
}
