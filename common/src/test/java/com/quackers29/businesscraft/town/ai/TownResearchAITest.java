package com.quackers29.businesscraft.town.ai;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.testutil.ProductionTestEnv;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Research AI: which nodes are scored, how scores follow town needs, and which node gets picked. */
class TownResearchAITest {

    private static final String UP_HEADER =
            "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n";
    private static final String PROD_HEADER = "prod_id,display_name,base_cycle_time_minutes,inputs,outputs\n";

    @TempDir
    Path tempDir;

    private ProductionTestEnv env;
    private Town town;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        env = ProductionTestEnv.install(tempDir);
        town = new Town(UUID.fromString("66666666-6666-6666-6666-666666666666"), new BlockPos(0, 64, 0), "AiTown");
        ConfigLoader.dailyTickInterval = 24000;
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private void load(String productions, String upgrades) throws Exception {
        env.writeCsv("items.csv", "item_id,display_name,mc_item_id\nwood,Wood,minecraft:oak_log\nfood,Food,minecraft:bread\n");
        env.writeCsv("productions.csv", PROD_HEADER + productions);
        env.writeCsv("upgrades.csv", UP_HEADER + upgrades);
        env.loadBundledItems();
        ProductionRegistry.load();
        UpgradeRegistry.load();
    }

    private double score(String nodeId) {
        return TownResearchAI.calculateScore(town, UpgradeRegistry.get(nodeId));
    }

    // --- which nodes are scored ---

    @Test
    void priorities_includeOnlyNodesWithMetPrereqsThatAreNotMaxed() throws Exception {
        load("", "open,c,Open,,,d,1,,pop_cap:1\n"
                + "gated,c,Gated,,open,d,1,,pop_cap:1\n"
                + "done,c,Done,,,d,1,,pop_cap:1\n"
                + "twice,c,Twice,2:1.0,,d,1,,pop_cap:1\n");
        town.getUpgrades().unlockNode("done");
        town.getUpgrades().unlockNode("twice");

        Map<String, Float> scores = TownResearchAI.calculatePriorities(town);

        assertTrue(scores.containsKey("open"));
        assertFalse(scores.containsKey("gated"), "prerequisite missing");
        assertFalse(scores.containsKey("done"), "non-repeatable and unlocked");
        assertTrue(scores.containsKey("twice"), "level 1 of 2");

        town.getUpgrades().unlockNode("twice");
        assertFalse(TownResearchAI.calculatePriorities(town).containsKey("twice"), "maxed");
        town.getUpgrades().unlockNode("open");
        assertTrue(TownResearchAI.calculatePriorities(town).containsKey("gated"));
    }

    @Test
    void infiniteNode_isNeverMaxed() throws Exception {
        load("", "inf,c,Inf,infinite:1.2,,d,1,,pop_cap:1\n");
        for (int i = 0; i < 20; i++) {
            town.getUpgrades().unlockNode("inf");
        }
        assertTrue(TownResearchAI.calculatePriorities(town).containsKey("inf"));
    }

    // --- capacity scores: 100 * fullness^2 ---

    @Test
    void storageCapScore_isQuadraticInTheFullestResource() throws Exception {
        load("", "cap,c,Cap,,,d,1,,storage_cap_all:100\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        assertEquals(0.0, score("cap"), 1e-6);

        town.addResource(Items.BREAD, 500); // 50% full
        assertEquals(25.0, score("cap"), 1e-3);

        town.addResource(Items.BREAD, 500); // 100% full
        assertEquals(100.0, score("cap"), 1e-3);
    }

    @Test
    void popCapScore_followsPopulationOverCap() throws Exception {
        load("", "pc,c,PC,,,d,1,,pop_cap:10\n");
        town.getUpgrades().addFlatModifier("pop_cap", 100f);
        town.setPopulation(50);
        assertEquals(25.0, score("pc"), 1e-3);
        town.setPopulation(100);
        assertEquals(100.0, score("pc"), 1e-3);
    }

    @Test
    void touristCapScore_followsTouristsOverCap() throws Exception {
        load("", "tc,c,TC,,,d,1,,tourist_cap:2\n");
        town.getUpgrades().addFlatModifier("tourist_cap", 4f);
        town.addPendingTouristSpawns(2);
        assertEquals(25.0, score("tc"), 1e-3);
    }

    @Test
    void perResourceStorageCapScore_usesThatResourceOnly() throws Exception {
        load("", "wc,c,WC,,,d,1,,storage_cap_wood:100\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.addResource(Items.BREAD, 1000); // food full, wood empty
        assertEquals(0.0, score("wc"), 1e-6);
        town.addResource(Items.OAK_LOG, 500);
        assertEquals(25.0, score("wc"), 1e-3);
    }

    // --- production speed scores ---

    @Test
    void recipeSpeedScore_isHighWhenOutputIsConsumedButNotProduced() throws Exception {
        load("farm,Farm,1,,food:10\neat,Eat,1,food:10,\n",
                "farm_up,c,Farm up,,,d,1,,farm\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.getUpgrades().addFlatModifier("eat", 1f); // consumption 10/min, farm still locked: ratio 0

        assertEquals(100.0, score("farm_up"), 1e-3);
    }

    @Test
    void recipeSpeedScore_fallsAsProductionCatchesConsumption() throws Exception {
        load("farm,Farm,1,,food:10\neat,Eat,1,food:10,\n",
                "farm_up,c,Farm up,,,d,1,,farm\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.getUpgrades().addFlatModifier("eat", 1f);

        town.getUpgrades().addFlatModifier("farm", 1f); // prod == cons -> 50
        assertEquals(50.0, score("farm_up"), 1e-3);

        town.getUpgrades().addFlatModifier("farm", 2f); // prod = 2 * cons -> 0
        assertEquals(0.0, score("farm_up"), 1e-3);
    }

    @Test
    void recipeSpeedScore_withoutConsumption_isZeroWhenProducing_andBalancedWhenNot() throws Exception {
        load("farm,Farm,1,,food:10\n", "farm_up,c,Farm up,,,d,1,,farm\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        assertEquals(50.0, score("farm_up"), 1e-3, "no production, no consumption");
        town.getUpgrades().addFlatModifier("farm", 1f);
        assertEquals(0.0, score("farm_up"), 1e-3, "producing, nobody eats");
    }

    @Test
    void recipeSpeedScore_isDeprioritisedWhenStorageIsFull() throws Exception {
        load("farm,Farm,1,,food:10\neat,Eat,1,food:10,\n", "farm_up,c,Farm up,,,d,1,,farm\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.getUpgrades().addFlatModifier("eat", 1f);
        town.addResource(Items.BREAD, 1000);

        assertEquals(0.0, score("farm_up"), 1e-3, "full output clamps the score to 0");
    }

    @Test
    void accumulatedOutputs_popAndTourist_scoreByEmptiness() throws Exception {
        load("grow,Grow,1,,pop:1\n", "grow_up,c,Grow up,,,d,1,,grow\n");
        town.getUpgrades().addFlatModifier("pop_cap", 100f);
        town.setPopulation(25);
        assertEquals(75.0, score("grow_up"), 1e-3);
        town.setPopulation(100);
        assertEquals(0.0, score("grow_up"), 1e-3);
    }

    // --- research speed score ---

    @Test
    void researchSpeedScore_isAverageTimeOfTheTopHalfOfOtherUpgrades() throws Exception {
        load("", "speed,c,Speed,,,d,1,,research:0.25\n"
                + "a,c,A,,,d,4,,pop_cap:1\n"
                + "b,c,B,,,d,2,,pop_cap:1\n");
        town.getUpgrades().addFlatModifier("pop_cap", 100f);
        town.setPopulation(100); // both a and b score 100; ties keep both in the top half

        // two candidates, top half = ceil(2/2) = 1 -> time of the first of equal scores (4 or 2)
        double s = score("speed");
        assertTrue(s == 4.0 || s == 2.0, "average of the top half of {4, 2}: " + s);
    }

    @Test
    void researchSpeedScore_ignoresOtherResearchUpgrades_andIsZeroWithNoCandidates() throws Exception {
        load("", "speed,c,Speed,,,d,1,,research:0.25\nspeed2,c,Speed2,,,d,9,,research:0.5\n");
        assertEquals(0.0, score("speed"), 1e-6);
    }

    @Test
    void researchSpeedScore_usesScaledTimeForRepeatedNodes() throws Exception {
        load("", "speed,c,Speed,,,d,1,,research:0.25\nrep,c,Rep,5:2.0,,d,3,,pop_cap:1\n");
        town.getUpgrades().unlockNode("rep");
        town.getUpgrades().unlockNode("rep"); // next is level 3: 3 * 2^2 = 12 minutes
        assertEquals(12.0, score("speed"), 1e-3);
    }

    @Test
    void scoreNeverNegative() throws Exception {
        load("farm,Farm,1,,food:10\n", "farm_up,c,Farm up,,,d,1,,farm\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.addResource(Items.BREAD, 1000); // allFull -> -50 -> clamped
        assertEquals(0.0, score("farm_up"), 1e-6);
    }

    // --- selection ---

    @Test
    void getBestUpgradeTarget_isTheHighestScoreRegardlessOfAffordability() throws Exception {
        load("", "low,c,Low,,,d,1,,pop_cap:1\nhigh,c,High,,,d,1,wood:1000,storage_cap_all:1\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.addResource(Items.BREAD, 1000);
        // high: storage full -> 100; low: pop_cap 0 -> 0
        assertEquals("high", TownResearchAI.getBestUpgradeTarget(town));
    }

    @Test
    void getBestUpgradeTarget_nullWhenNothingToResearch() throws Exception {
        load("", "");
        assertNull(TownResearchAI.getBestUpgradeTarget(town));
    }

    @Test
    void selectBest_waitsForTheBestNodeUntilPatienceRunsOut() throws Exception {
        load("", "low,c,Low,,,d,1,,pop_cap:1\nhigh,c,High,,,d,1,wood:1000,storage_cap_all:1\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.addResource(Items.BREAD, 1000);
        town.setPopulation(0);
        Map<String, Float> scores = TownResearchAI.calculatePriorities(town);
        assertTrue(scores.get("high") > scores.get("low"));

        // just started waiting: only the top score qualifies and it is unaffordable
        assertNull(TownResearchAI.selectBestResearch(town, scores, 0));
        assertNull(TownResearchAI.selectBestResearch(town, scores, 100));

        // after a full day the threshold has decayed to 0 and the affordable node is taken
        assertEquals("low", TownResearchAI.selectBestResearch(town, scores, ConfigLoader.dailyTickInterval));
    }

    @Test
    void selectBest_takesTheBestAffordableNodeImmediately() throws Exception {
        load("", "top,c,Top,,,d,1,,storage_cap_all:1\nlow,c,Low,,,d,1,,pop_cap:1\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.addResource(Items.BREAD, 1000);
        Map<String, Float> scores = TownResearchAI.calculatePriorities(town);

        assertEquals("top", TownResearchAI.selectBestResearch(town, scores, 0));
    }

    @Test
    void selectBest_patienceDependsOnDailyTickInterval() throws Exception {
        load("", "low,c,Low,,,d,1,,pop_cap:1\nhigh,c,High,,,d,1,wood:1000,storage_cap_all:1\n");
        town.getUpgrades().addFlatModifier("storage_cap_all", 1000f);
        town.addResource(Items.BREAD, 1000);
        Map<String, Float> scores = TownResearchAI.calculatePriorities(town);

        ConfigLoader.dailyTickInterval = 100;
        assertEquals("low", TownResearchAI.selectBestResearch(town, scores, 100));
    }

    @Test
    void selectBest_emptyScores_returnsNull() throws Exception {
        load("", "");
        assertNull(TownResearchAI.selectBestResearch(town, Map.of(), 1_000_000));
    }
}
