package com.quackers29.businesscraft.config;

import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.testutil.ProductionTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Review item A2: one WARN per bad reference, nothing else changes. */
class ConfigValidatorTest {

    private static final String ITEMS = "item_id,display_name,mc_item_id\nwood,Wood,minecraft:oak_log\nfood,Food,minecraft:bread\n";
    private static final String PROD_HEADER = "prod_id,display_name,base_cycle_time_minutes,inputs,outputs\n";
    private static final String UP_HEADER = "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n";
    private static final String BIOME_HEADER = "biome_ids,variant_id,variant_name,description,weight,starting_nodes,starting_values\n";

    @TempDir
    Path tempDir;

    private ProductionTestEnv env;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        env = ProductionTestEnv.install(tempDir);
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private List<String> loadAndValidate(String productions, String upgrades, String biomes) throws Exception {
        env.writeCsv("items.csv", ITEMS);
        env.writeCsv("productions.csv", PROD_HEADER + productions);
        env.writeCsv("upgrades.csv", UP_HEADER + upgrades);
        env.writeCsv("biomes.csv", BIOME_HEADER + biomes);
        env.loadBundledRegistries(); // reads the files written above
        ConfigValidator.validate();
        return ConfigWarnings.all();
    }

    private static void assertOneWarningContaining(List<String> warnings, String... fragments) {
        long hits = warnings.stream().filter(w -> {
            for (String f : fragments) {
                if (!w.contains(f))
                    return false;
            }
            return true;
        }).count();
        assertEquals(1, hits, "expected exactly one warning containing " + List.of(fragments) + " in " + warnings);
    }

    @Test
    void cleanConfig_hasNoWarnings() throws Exception {
        List<String> w = loadAndValidate(
                "farming,Farming,1,,food:4\ntaxes,Taxes,1,wood:1*pop,food:2\nrun,Run,1,happiness:>60;surplus:food;wood:>min,food:1\n",
                "base,housing,Base,,,d,0,,pop_cap:10;farming;taxes;run\nnext,housing,Next,5:1.2,base,d,1,wood:5;tourism:2,storage_cap_food:5\n",
                "minecraft:plains,a,A,\"Desc, with comma\",10,base;next:2,pop:5;food:100;happiness:70;border:10\n");
        assertEquals(List.of(), w);
    }

    @Test
    void unknownResourceInInputOutputAndCost() throws Exception {
        List<String> w = loadAndValidate(
                "bad,Bad,1,irn:2,gold:1\n",
                "n,housing,N,,,d,1,irn:5,pop_cap:1\n",
                "");
        assertOneWarningContaining(w, "productions.csv", "[bad]", "input 'irn'");
        assertOneWarningContaining(w, "productions.csv", "[bad]", "output 'gold'");
        assertOneWarningContaining(w, "upgrades.csv", "[n]", "cost 'irn'");
    }

    @Test
    void unknownExpressionToken_typoInAmount() throws Exception {
        List<String> w = loadAndValidate("typo,Typo,1,,food:1*popp\n", "", "");
        assertOneWarningContaining(w, "[typo]", "unknown token 'popp'");
    }

    @Test
    void expressionMayUseUpgradeModifiersRecipeIdsAndNodeIds() throws Exception {
        List<String> w = loadAndValidate(
                "e,E,1,,food:2*pop*happiness*wu*pop_cap*e\n",
                "base,housing,Base,,,d,0,,e\n", "");
        assertEquals(List.of(), w);
    }

    @Test
    void effectTargetNotRecipeNodeOrStat() throws Exception {
        List<String> w = loadAndValidate("", "n,housing,N,,,d,1,,pop_capp:5;pop_cap:5\n", "");
        assertOneWarningContaining(w, "[n]", "effect target 'pop_capp'");
        assertEquals(1, w.size());
    }

    @Test
    void unknownPrerequisite() throws Exception {
        List<String> w = loadAndValidate("", "n,housing,N,,ghost,d,1,,pop_cap:5\n", "");
        assertOneWarningContaining(w, "[n]", "prerequisite 'ghost'");
    }

    @Test
    void unknownBiomeStartingNode_levelSuffixIgnored_andUnknownValueKey() throws Exception {
        List<String> w = loadAndValidate("",
                "base,housing,Base,,,d,0,,pop_cap:1\n",
                "minecraft:plains,k,K,d,10,base:2;border_basic,pop:5;gold:3\n");
        assertOneWarningContaining(w, "biomes.csv", "[k]", "starting node 'border_basic'");
        assertOneWarningContaining(w, "[k]", "starting value 'gold'");
        assertEquals(2, w.size());
    }

    @Test
    void conditionTargetsAndValues() throws Exception {
        List<String> w = loadAndValidate(
                "c,C,1,happiness:>abc;mood:>5;surplus:gold,food:1\n", "", "");
        assertOneWarningContaining(w, "[c]", "value 'abc'");
        assertOneWarningContaining(w, "[c]", "condition target 'mood'");
        assertOneWarningContaining(w, "[c]", "surplus condition names unknown resource 'gold'");
    }

    @Test
    void shortRowsAreReportedWithLineNumbers() throws Exception {
        List<String> w = loadAndValidate("ok,Ok,1,,food:1\nshort,Short,1\n", "n,housing,N\n", "a,b,c\n");
        assertOneWarningContaining(w, "productions.csv line 3", "expected 5 columns");
        assertOneWarningContaining(w, "upgrades.csv line 2", "expected 9 columns");
        assertOneWarningContaining(w, "biomes.csv line 2", "expected 7 columns");
    }

    @Test
    void cycleTimeZeroOrNegative_andInvalid() throws Exception {
        List<String> w = loadAndValidate("zero,Zero,0,,food:1\nneg,Neg,-2,,food:1\nbad,Bad,fast,,food:1\n", "", "");
        assertOneWarningContaining(w, "line 2", "must be above 0");
        assertOneWarningContaining(w, "line 3", "must be above 0");
        assertOneWarningContaining(w, "line 4", "invalid cycle time 'fast'");
    }

    @Test
    void duplicateIds() throws Exception {
        List<String> w = loadAndValidate("a,A,1,,food:1\na,A2,1,,food:2\n",
                "n,housing,N,,,d,1,,pop_cap:1\nn,housing,N2,,,d,1,,pop_cap:2\n", "");
        assertOneWarningContaining(w, "productions.csv line 3", "duplicate prod_id 'a'");
        assertOneWarningContaining(w, "upgrades.csv line 3", "duplicate node_id 'n'");
    }

    @Test
    void malformedOutputsAndCosts() throws Exception {
        List<String> w = loadAndValidate("m,M,1,,food\n", "n,housing,N,,,d,1,wood,pop_cap:1\n", "");
        assertOneWarningContaining(w, "productions.csv", "outputs 'food'");
        assertOneWarningContaining(w, "upgrades.csv", "required_items 'wood'");
    }

    @Test
    void invalidInputToken() throws Exception {
        List<String> w = loadAndValidate("t,T,1,wood,food:1\n", "", "");
        assertOneWarningContaining(w, "invalid input token 'wood'");
    }

    @Test
    void validationDoesNotChangeWhatTheRegistriesHold() throws Exception {
        loadAndValidate("bad,Bad,1,irn:2,gold:1\n", "n,housing,N,,ghost,d,1,,nothing:5\n", "");
        assertNotNull(com.quackers29.businesscraft.production.ProductionRegistry.get("bad"));
        assertNotNull(com.quackers29.businesscraft.production.UpgradeRegistry.get("n"));
    }
}
