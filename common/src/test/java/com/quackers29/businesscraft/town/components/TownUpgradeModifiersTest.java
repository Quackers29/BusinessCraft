package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.testutil.ProductionTestEnv;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
 * TownUpgradeComponent against the bundled upgrades.csv and small custom files: unlocking, the
 * modifier map (caps, speed multipliers, unlock flags), repeatable-node scaling, chained unlocks,
 * flat modifiers, research start / progress / completion, and NBT.
 */
class TownUpgradeModifiersTest {

    private static final String HEADER =
            "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n";
    /** 6 ticks at 1 tick = 1/1200 minute (half a tick of slack against float rounding). */
    private static final String SIX = "0.0046";

    @TempDir
    Path tempDir;

    private ProductionTestEnv env;
    private Town town;
    private TownUpgradeComponent up;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        env = ProductionTestEnv.install(tempDir);
        town = new Town(UUID.fromString("55555555-5555-5555-5555-555555555555"), new BlockPos(0, 64, 0), "UpTown");
        up = town.getUpgrades();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private void bundled() {
        env.loadBundledRegistries();
    }

    private void nodes(String rows) throws Exception {
        env.writeCsv("items.csv", "item_id,display_name,mc_item_id\nwood,Wood,minecraft:oak_log\nfood,Food,minecraft:bread\n");
        env.writeCsv("upgrades.csv", HEADER + rows);
        env.loadBundledItems();
        UpgradeRegistry.load();
        up.addFlatModifier("storage_cap_all", 100000f);
    }

    private void tick(int n) {
        for (int i = 0; i < n; i++) {
            up.tick();
        }
    }

    // --- bundled design ---

    @Test
    void basicSettlement_setsCapsAndUnlocksRecipesAndChainedFarming() {
        bundled();

        up.unlockNode("basic_settlement");

        assertEquals(100f, up.getModifier("pop_cap"));
        assertEquals(5f, up.getModifier("tourist_cap"));
        assertEquals(2000f, up.getModifier("storage_cap_all"));
        assertEquals(50f, up.getModifier("happiness"));
        for (String recipe : new String[] { "tourism", "population_maintenance", "population_growth", "taxes" }) {
            assertEquals(1f, up.getModifier(recipe), recipe);
            assertNotNull(ProductionRegistry.get(recipe), recipe);
        }
        // farming_basic is an effect target that is itself a node: chained unlock, which in turn unlocks farming
        assertTrue(up.isUnlocked("farming_basic"));
        assertEquals(1, up.getUpgradeLevel("farming_basic"));
        assertEquals(1f, up.getModifier("farming"));
        assertEquals(2, up.getUnlockedNodes().size());
    }

    @Test
    void unknownModifier_isZero_andLockedRecipesStayLocked() {
        bundled();
        assertEquals(0f, up.getModifier("nonsense"));
        assertEquals(0f, up.getModifier("woodcutting"));
        assertFalse(up.isUnlocked("woodcutting_basic"));
    }

    @Test
    void repeatableNode_withBenefitMultiplier_replacesRatherThanAdds() {
        bundled();
        // cap_upgrade: 5:1.1:1.2, storage_cap_all:4000 -> base * 1.2^(level-1)
        up.unlockNode("cap_upgrade");
        assertEquals(4000f, up.getModifier("storage_cap_all"), 1e-2f);
        up.unlockNode("cap_upgrade");
        assertEquals(4800f, up.getModifier("storage_cap_all"), 1e-2f);
        up.unlockNode("cap_upgrade");
        assertEquals(5760f, up.getModifier("storage_cap_all"), 1e-1f);
    }

    @Test
    void repeatableNode_withoutBenefitMultiplier_accumulatesLinearly() {
        bundled();
        up.unlockNode("basic_settlement");
        up.unlockNode("pop_cap_upgrade"); // infinite:1.2, pop_cap:100
        assertEquals(200f, up.getModifier("pop_cap"));
        up.unlockNode("pop_cap_upgrade");
        up.unlockNode("pop_cap_upgrade");
        assertEquals(400f, up.getModifier("pop_cap"));
        assertEquals(3, up.getUpgradeLevel("pop_cap_upgrade"));
    }

    @Test
    void speedEffect_addsToRecipeModifier() {
        bundled();
        up.unlockNode("basic_settlement"); // farming = 1
        up.unlockNode("farming_advanced"); // farming*0.5
        assertEquals(1.5f, up.getModifier("farming"), 1e-6f);
        up.unlockNode("farming_advanced");
        assertEquals(2f, up.getModifier("farming"), 1e-6f);
    }

    @Test
    void researchSpeedEffect_isAModifier() {
        bundled();
        up.unlockNode("research_basic");
        assertEquals(0.25f, up.getModifier("research"), 1e-6f);
    }

    @Test
    void workUnitNodes_unlockRecipeAndRaiseCap() {
        bundled();
        up.unlockNode("work_units");
        assertEquals(1f, up.getModifier("work_unit_production"));
        assertEquals(1000f, up.getModifier("wu_cap"));
    }

    // --- flat modifiers ---

    @Test
    void flatModifiers_sumWithNodeEffects_andSurviveRecalculation() {
        bundled();
        up.addFlatModifier("border", 10f);
        up.accumulateFlatModifier("border", 2f);
        up.accumulateFlatModifier("border", 3f);
        assertEquals(15f, up.getModifier("border"));

        up.unlockNode("border_expansion"); // border:2 at level 1
        assertEquals(17f, up.getModifier("border"), 1e-6f);
    }

    @Test
    void addFlatModifier_replacesEarlierValue() {
        up.addFlatModifier("pop_cap", 10f);
        up.addFlatModifier("pop_cap", 25f);
        assertEquals(25f, up.getModifier("pop_cap"));
    }

    // --- chained unlocks ---

    @Test
    void chainedUnlock_effectNamingAnotherNodeUnlocksIt() throws Exception {
        nodes("a,cat,A,,,d,0,,b;pop_cap:1\nb,cat,B,,,d,0,,happiness:5\n");
        up.unlockNode("a");
        assertTrue(up.isUnlocked("b"));
        assertEquals(5f, up.getModifier("happiness"));
    }

    @Test
    void unlockNode_unknownIdIsAccepted_currentBehaviour_validatorWarnsInstead() throws Exception {
        nodes("a,cat,A,,,d,0,,pop_cap:1\n");
        up.unlockNode("ghost");
        assertEquals(1, up.getUpgradeLevel("ghost"));
        assertEquals(0f, up.getModifier("ghost"));
    }

    // --- research ---

    @Test
    void startResearch_deductsCost_andCompletesAfterScaledTime() throws Exception {
        nodes("farm,cat,Farm,,,d," + SIX + ",wood:10,farming\n");
        town.addResource(Items.OAK_LOG, 25);

        up.startResearch("farm");

        assertEquals("farm", up.getCurrentResearchNode());
        assertEquals(15, town.getResourceCount(Items.OAK_LOG));

        tick(5);
        assertEquals("farm", up.getCurrentResearchNode());
        assertEquals(0, up.getUpgradeLevel("farm"));
        tick(1);
        assertNull(up.getCurrentResearchNode());
        assertEquals(1, up.getUpgradeLevel("farm"));
        assertEquals(1f, up.getModifier("farming"));
        assertEquals(0f, up.getResearchProgress());
    }

    @Test
    void researchSpeedModifier_shortensResearch_andIsFlooredAtTenPercent() throws Exception {
        nodes("farm,cat,Farm,,,d," + SIX + ",,farming\n");
        up.addFlatModifier("research", 1f); // speed x2 -> 3 ticks
        up.startResearch("farm");
        tick(3);
        assertEquals(1, up.getUpgradeLevel("farm"));

        // a big negative modifier cannot stall research completely: speed floor 10% -> 56 ticks
        nodes("slow,cat,Slow,,,d," + SIX + ",,farming\n");
        up.addFlatModifier("research", -5f);
        up.startResearch("slow");
        tick(55);
        assertEquals("slow", up.getCurrentResearchNode());
        tick(1);
        assertNull(up.getCurrentResearchNode());
    }

    @Test
    void startResearch_refusedWhenPrereqMissing_unaffordable_orMaxed() throws Exception {
        nodes("base,cat,Base,,,d,0,,pop_cap:1\n"
                + "need,cat,Need,,base,d,1,,farming\n"
                + "pricey,cat,Pricey,,,d,1,wood:100,farming\n"
                + "twice,cat,Twice,2:1.0,,d,1,,pop_cap:1\n");
        town.addResource(Items.OAK_LOG, 10);

        up.startResearch("need");
        assertNull(up.getCurrentResearchNode(), "prerequisite not unlocked");

        up.startResearch("pricey");
        assertNull(up.getCurrentResearchNode(), "cannot afford");
        assertEquals(10, town.getResourceCount(Items.OAK_LOG), "nothing deducted on refusal");

        up.unlockNode("base");
        up.startResearch("base");
        assertNull(up.getCurrentResearchNode(), "non-repeatable node already unlocked");

        up.unlockNode("twice");
        up.unlockNode("twice");
        up.startResearch("twice");
        assertNull(up.getCurrentResearchNode(), "repeat limit reached");

        up.startResearch("missing");
        assertNull(up.getCurrentResearchNode());
    }

    @Test
    void repeatableResearch_costAndTimeScaleWithLevel() throws Exception {
        nodes("rep,cat,Rep,3:2.0,,d,0.001,wood:10,pop_cap:5\n");
        town.addResource(Items.OAK_LOG, 1000);

        up.startResearch("rep");
        assertEquals(990, town.getResourceCount(Items.OAK_LOG));
        up.completeResearch();
        up.startResearch("rep");
        assertEquals(970, town.getResourceCount(Items.OAK_LOG), "level 2 costs 10 * 2");
        assertEquals(0.002f, up.getScaledResearchMinutes("rep"), 1e-6f);
        up.completeResearch();
        assertEquals(2, up.getUpgradeLevel("rep"));
    }

    @Test
    void idleTick_startsAffordableResearchOnTheAiCooldown() throws Exception {
        nodes("farm,cat,Farm,,,d," + SIX + ",,farming\n");

        up.tick(); // first idle tick runs the AI immediately

        assertEquals("farm", up.getCurrentResearchNode());
        assertTrue(up.getAiScores().containsKey("farm"));
    }

    @Test
    void idleTick_doesNothingWhenNothingAffordable() throws Exception {
        nodes("pricey,cat,Pricey,,,d,1,wood:100,farming\n");
        tick(5);
        assertNull(up.getCurrentResearchNode());
    }

    // --- persistence ---

    @Test
    void nbtRoundTrip_restoresLevelsResearchFlatModifiersAndRecomputesModifiers() throws Exception {
        bundled();
        up.unlockNode("basic_settlement");
        up.unlockNode("pop_cap_upgrade");
        up.addFlatModifier("border", 10f);
        town.addResource(Items.OAK_LOG, 50);
        up.startResearch("pop_cap_upgrade");
        CompoundTag tag = new CompoundTag();
        up.save(tag);

        TownUpgradeComponent restored = new TownUpgradeComponent(town);
        restored.load(tag);

        assertEquals(up.getUpgradeLevels(), restored.getUpgradeLevels());
        assertEquals(up.getUnlockedNodes(), restored.getUnlockedNodes());
        assertEquals(200f, restored.getModifier("pop_cap"));
        assertEquals(10f, restored.getModifier("border"));
        assertEquals(up.getCurrentResearchNode(), restored.getCurrentResearchNode());
    }

    @Test
    void nbtRoundTrip_savesResearchProgress() throws Exception {
        nodes("farm,cat,Farm,,,d,1,,farming\n");
        up.startResearch("farm");
        tick(10);
        CompoundTag tag = new CompoundTag();
        up.save(tag);

        TownUpgradeComponent restored = new TownUpgradeComponent(town);
        restored.load(tag);

        assertEquals("farm", restored.getCurrentResearchNode());
        assertEquals(10f / 1200f, restored.getResearchProgress(), 1e-6f);
    }

    @Test
    void load_emptyTag_resetsEverything() throws Exception {
        bundled();
        up.unlockNode("basic_settlement");
        up.load(new CompoundTag());
        assertTrue(up.getUpgradeLevels().isEmpty());
        assertEquals(0f, up.getModifier("pop_cap"));
        assertNull(up.getCurrentResearchNode());
    }
}
