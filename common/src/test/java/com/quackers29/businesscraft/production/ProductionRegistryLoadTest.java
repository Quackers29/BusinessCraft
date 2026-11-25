package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.config.ConfigWarnings;
import com.quackers29.businesscraft.data.parsers.Condition;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.testutil.ProductionTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** ProductionRegistry.load(): install of the bundled file and parsing of the mixed inputs column. */
class ProductionRegistryLoadTest {

    private static final String HEADER = "prod_id,display_name,base_cycle_time_minutes,inputs,outputs\n";

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

    private ProductionRecipe loadOne(String row, String id) throws Exception {
        env.writeCsv("productions.csv", HEADER + row + "\n");
        ProductionRegistry.load();
        ProductionRecipe r = ProductionRegistry.get(id);
        assertNotNull(r, id);
        return r;
    }

    @Test
    void missingFile_installsBundledNineRecipes() {
        ProductionRegistry.load();

        assertTrue(Files.exists(env.businesscraftDir().resolve("productions.csv")));
        assertEquals(9, ProductionRegistry.getAll().size());
        assertNotNull(ProductionRegistry.get("tourism"));
        assertNull(ProductionRegistry.get("basic_farming"));

        boolean growthStopsAtCap = ProductionRegistry.get("population_growth").getConditions().stream()
                .anyMatch(c -> c.getTarget().equals("pop") && "<".equals(c.getOperator())
                        && "pop_cap".equals(c.getValue()));
        assertTrue(growthStopsAtCap, "population growth is gated on pop < pop_cap");
        boolean borderStopsWhenRoom = ProductionRegistry.get("border_expansion_production").getConditions().stream()
                .anyMatch(c -> c.getTarget().equals("crowding") && ">".equals(c.getOperator())
                        && "1".equals(c.getValue()));
        assertTrue(borderStopsWhenRoom, "expand border is gated on crowding > 1");
    }

    @Test
    void existingFile_isNotOverwritten() throws Exception {
        String custom = HEADER + "only,Only,1,,food:1\n";
        env.writeCsv("productions.csv", custom);

        ProductionRegistry.load();

        assertEquals(custom, Files.readString(env.businesscraftDir().resolve("productions.csv")));
        assertEquals(1, ProductionRegistry.getAll().size());
    }

    @Test
    void simpleRecipe_resourceInputsAndOutputs() throws Exception {
        ProductionRecipe r = loadOne("sawmill,Sawmill,0.5,wood:4,planks:16", "sawmill");

        assertEquals("Sawmill", r.getDisplayName());
        assertEquals(0.5f, r.getBaseCycleTimeMinutes(), 1e-6f);
        assertEquals(1, r.getInputs().size());
        assertEquals("wood", r.getInputs().get(0).resourceId);
        assertEquals(4f, r.getInputs().get(0).amount, 1e-6f);
        assertEquals("planks", r.getOutputs().get(0).resourceId);
        assertEquals(16f, r.getOutputs().get(0).amount, 1e-6f);
        assertTrue(r.getConditions().isEmpty());
    }

    @Test
    void conditionsMixedIntoInputsColumn_areSplitOut() throws Exception {
        ProductionRecipe r = loadOne("growth,Growth,0.6,happiness:>60;surplus:food;pop:<pop_cap,pop:1", "growth");

        assertTrue(r.getInputs().isEmpty());
        List<Condition> c = r.getConditions();
        assertEquals(3, c.size());
        assertEquals("happiness", c.get(0).getTarget());
        assertEquals(">", c.get(0).getOperator());
        assertEquals("60", c.get(0).getValue());
        assertEquals("surplus", c.get(1).getTarget());
        assertEquals("food", c.get(1).getValue());
        assertEquals("pop", c.get(2).getTarget());
        assertEquals("<", c.get(2).getOperator());
        assertEquals("pop_cap", c.get(2).getValue());
    }

    @Test
    void explicitOperatorOnResource_isCondition_notInput() throws Exception {
        ProductionRecipe r = loadOne("mill,Mill,1,wu:20;wood:25;wood:>min;money:1,planks:100", "mill");

        assertEquals(List.of("wu", "wood", "money"), r.getInputs().stream().map(a -> a.resourceId).toList());
        assertEquals(1, r.getConditions().size());
        assertEquals("wood", r.getConditions().get(0).getTarget());
        assertEquals("min", r.getConditions().get(0).getValue());
    }

    @Test
    void globalModifierMin_addsGreaterThanConditionForEveryResourceInput() throws Exception {
        ProductionRecipe r = loadOne("g,G,1,wood:25;food:2;min,planks:1", "g");

        assertEquals(2, r.getInputs().size());
        assertEquals(2, r.getConditions().size());
        for (Condition c : r.getConditions()) {
            assertEquals(">", c.getOperator());
            assertEquals("min", c.getValue());
        }
        assertEquals("wood", r.getConditions().get(0).getTarget());
        assertEquals("food", r.getConditions().get(1).getTarget());
    }

    @Test
    void globalModifierPercentAndExcess() throws Exception {
        ProductionRecipe pct = loadOne("p,P,1,wood:5;10%,planks:1", "p");
        assertEquals("10%", pct.getConditions().get(0).getValue());
        ProductionRecipe exc = loadOne("x,X,1,wood:5;Excess,planks:1", "x");
        assertEquals("Excess", exc.getConditions().get(0).getValue());
    }

    @Test
    void expressionAmounts_keepExpressionAndZeroNumericAmount() throws Exception {
        ProductionRecipe r = loadOne("tax,Tax,0.4,food:1*pop,money:2*pop*happiness", "tax");

        ResourceAmount in = r.getInputs().get(0);
        assertEquals("1*pop", in.amountExpression);
        assertEquals(0f, in.amount, 1e-6f);
        assertEquals("2*pop*happiness", r.getOutputs().get(0).amountExpression);
    }

    @Test
    void outputAliases_populationAndTouristCount() throws Exception {
        ProductionRecipe r = loadOne("a,A,1,,population:1;tourist_count:2", "a");
        assertEquals("pop", r.getOutputs().get(0).resourceId);
        assertEquals("tourist", r.getOutputs().get(1).resourceId);
    }

    @Test
    void multipleOutputs_andEmptyOutputsAllowed() throws Exception {
        env.writeCsv("productions.csv", HEADER + "two,Two,1,,food:1;wood:2\neat,Eat,1,food:1,\n");
        ProductionRegistry.load();
        assertEquals(2, ProductionRegistry.get("two").getOutputs().size());
        assertTrue(ProductionRegistry.get("eat").getOutputs().isEmpty());
        assertTrue(ConfigWarnings.all().isEmpty(), ConfigWarnings.all().toString());
    }

    @Test
    void quotedDisplayNameWithComma_keepsColumnsAligned() throws Exception {
        ProductionRecipe r = loadOne("q,\"Mill, big\",2,wood:1,planks:2", "q");
        assertEquals("Mill, big", r.getDisplayName());
        assertEquals(2f, r.getBaseCycleTimeMinutes(), 1e-6f);
        assertEquals("planks", r.getOutputs().get(0).resourceId);
    }

    @Test
    void commentAndBlankLinesSkipped_shortRowDroppedWithWarning() throws Exception {
        env.writeCsv("productions.csv", HEADER + "# note\n\nok,Ok,1,,food:1\nshort,Short,1\n");
        ProductionRegistry.load();

        assertEquals(1, ProductionRegistry.getAll().size());
        assertEquals(1, ConfigWarnings.all().size());
        assertTrue(ConfigWarnings.all().get(0).contains("line 5"));
    }

    @Test
    void reload_replacesPreviousRecipes() throws Exception {
        env.writeCsv("productions.csv", HEADER + "one,One,1,,food:1\n");
        ProductionRegistry.load();
        env.writeCsv("productions.csv", HEADER + "two,Two,1,,food:1\n");
        ProductionRegistry.load();
        assertNull(ProductionRegistry.get("one"));
        assertNotNull(ProductionRegistry.get("two"));
    }

    @Test
    void estimatedValuesCalculatedFromLoadedRecipes() throws Exception {
        env.writeCsv("productions.csv", HEADER + "farm,Farm,2,,food:4\n");
        ProductionRegistry.load();
        // effort = cycle / yield = 0.5 min per unit, price = 20 * effort
        assertEquals(0.5f, ProductionRegistry.getEffort("food"), 1e-6f);
        assertEquals(10f, ProductionRegistry.getEstimatedValue("food"), 1e-4f);
    }
}
