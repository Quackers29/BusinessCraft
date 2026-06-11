package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-025: Estimated Effort Calculation (Test + Docs Loop).
 *
 * Covers the production-based effort/price estimator in ProductionRegistry:
 *   - calculateEstimatedValues builds reverse producer index then per-output recursiveGetEffort
 *   - effort = min over recipes( (cycleTime + sum(q*inputEffort)) / outputQty )
 *   - estimatedValue = effort * 20.0f
 *   - 0 for base/non-produced, 1.0f fallback on cycles/empty/all-invalid, resolveQuantity heuristic
 *
 * Private statics (RECIPES, ESTIMATED_VALUES, EFFORT_VALUES) and private methods are
 * driven via reflection. No McBootstrap required — pure data objects only.
 * Documentation: vault/Production/Recipes/Estimated Effort Calculation.md
 */
class ProductionRegistryTest {

    // Snapshots for isolation (static mutable state)
    private Map<String, ProductionRecipe> savedRecipes;
    private Map<String, Float> savedEstimated;
    private Map<String, Float> savedEffort;

    @BeforeEach
    void setUp() throws Exception {
        // Snapshot the three private static maps so we can restore exact prior state
        savedRecipes = snapshotMap("RECIPES");
        savedEstimated = snapshotMap("ESTIMATED_VALUES");
        savedEffort = snapshotMap("EFFORT_VALUES");
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore exactly (clear + putAll) so any other tests see original contents
        restoreMap("RECIPES", savedRecipes);
        restoreMap("ESTIMATED_VALUES", savedEstimated);
        restoreMap("EFFORT_VALUES", savedEffort);
    }

    // --- reflection helpers ---

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> snapshotMap(String fieldName) throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Map<K, V> cur = (Map<K, V>) f.get(null);
        return new HashMap<>(cur); // shallow copy of entries is enough (we replace contents)
    }

    @SuppressWarnings("unchecked")
    private <K, V> void restoreMap(String fieldName, Map<K, V> snapshot) throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Map<K, V> cur = (Map<K, V>) f.get(null);
        cur.clear();
        cur.putAll(snapshot);
    }

    private void putRecipe(ProductionRecipe r) throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField("RECIPES");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ProductionRecipe> recipes = (Map<String, ProductionRecipe>) f.get(null);
        recipes.put(r.getId(), r);
    }

    private void clearRecipes() throws Exception {
        Field f = ProductionRegistry.class.getDeclaredField("RECIPES");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ProductionRecipe> recipes = (Map<String, ProductionRecipe>) f.get(null);
        recipes.clear();
    }

    private void invokeCalculateEstimatedValues() throws Exception {
        Method m = ProductionRegistry.class.getDeclaredMethod("calculateEstimatedValues");
        m.setAccessible(true);
        m.invoke(null);
    }

    private float invokeResolveQuantity(ResourceAmount ra) throws Exception {
        Method m = ProductionRegistry.class.getDeclaredMethod("resolveQuantity", ResourceAmount.class);
        m.setAccessible(true);
        return (float) m.invoke(null, ra);
    }

    // --- recipe builders for tests ---

    private ProductionRecipe simple(String id, float cycle, List<ResourceAmount> ins, List<ResourceAmount> outs) {
        return new ProductionRecipe(id, id, cycle, ins, outs, Collections.emptyList());
    }

    private ResourceAmount amt(String id, float q) {
        return new ResourceAmount(id, q);
    }

    private ResourceAmount amtExpr(String id, String expr) {
        return new ResourceAmount(id, expr);
    }

    // --- formula tests ---

    @Test
    void getEstimatedValue_unknownResource_returnsDefaultOne() throws Exception {
        clearRecipes();
        invokeCalculateEstimatedValues();
        assertEquals(1.0f, ProductionRegistry.getEstimatedValue("nonexistent"));
        assertEquals(1.0f, ProductionRegistry.getEffort("nonexistent"));
    }

    @Test
    void calculate_simpleNoInputRecipe_effortIsCycleOverYield() throws Exception {
        clearRecipes();
        // basic_farming: cycle=1, inputs=[], outputs=food:4 → effort = 1/4 = 0.25, est=5.0
        putRecipe(simple("basic_farming", 1.0f, List.of(), List.of(amt("food", 4f))));
        invokeCalculateEstimatedValues();
        // 1 / 4 = 0.25; 0.25 * 20 = 5
        assertEquals(0.25f, ProductionRegistry.getEffort("food"));
        assertEquals(5.0f, ProductionRegistry.getEstimatedValue("food"));
    }

    @Test
    void calculate_chainedInput_effortAccumulates() throws Exception {
        clearRecipes();
        // wood (base input, effort 0 when looked up as non-output)
        // wood_to_planks: cycle 0.5, wood:4, planks:16 → effort = (0.5 + 4*0) / 16 = 0.03125, est ~0.625
        putRecipe(simple("wood_to_planks", 0.5f, List.of(amt("wood", 4f)), List.of(amt("planks", 16f))));
        invokeCalculateEstimatedValues();
        // 0.5 / 16 = 0.03125; 0.03125 * 20 = 0.625
        assertEquals(0.03125f, ProductionRegistry.getEffort("planks"), 1e-6f);
        assertEquals(0.625f, ProductionRegistry.getEstimatedValue("planks"), 1e-6f);
        // wood itself has no producing recipe → 1.0 default
        assertEquals(1.0f, ProductionRegistry.getEffort("wood"));
    }

    @Test
    void calculate_multipleRecipesForSameOutput_choosesMinEffort() throws Exception {
        clearRecipes();
        // Two ways to make "widget":
        // cheap: cycle 2, no inputs, yield 10 → 2/10=0.2
        // expensive: cycle 5, no inputs, yield 10 → 5/10=0.5
        putRecipe(simple("cheap_widget", 2.0f, List.of(), List.of(amt("widget", 10f))));
        putRecipe(simple("expensive_widget", 5.0f, List.of(), List.of(amt("widget", 10f))));
        invokeCalculateEstimatedValues();
        // min(0.2, 0.5) = 0.2; 0.2*20=4
        assertEquals(0.2f, ProductionRegistry.getEffort("widget"));
        assertEquals(4.0f, ProductionRegistry.getEstimatedValue("widget"));
    }

    @Test
    void calculate_zeroOutputQty_treatedAsOne() throws Exception {
        clearRecipes();
        // yield listed as 0 (or dynamic 0) → guard sets to 1; effort = cycle / 1
        putRecipe(simple("zero_yield", 3.0f, List.of(), List.of(amt("thing", 0f))));
        invokeCalculateEstimatedValues();
        // total=3, outQty=1 → effort=3; est=60
        assertEquals(3.0f, ProductionRegistry.getEffort("thing"));
        assertEquals(60.0f, ProductionRegistry.getEstimatedValue("thing"));
    }

    @Test
    void calculate_inputWithDynamicExpr_resolvesViaHeuristic() throws Exception {
        clearRecipes();
        // input qty expr "2*foo" → resolve takes "2" → q=2.0
        // recipe: cycle 4, input bar:2 (expr), output baz:1 → effort = (4 + 2*0) / 1 = 4 (bar is base, recursive returns 0)
        putRecipe(simple("expr_input", 4.0f, List.of(amtExpr("bar", "2*foo")), List.of(amt("baz", 1f))));
        invokeCalculateEstimatedValues();
        assertEquals(4.0f, ProductionRegistry.getEffort("baz"));
        assertEquals(80.0f, ProductionRegistry.getEstimatedValue("baz"));
    }

    @Test
    void resolveQuantity_exprWithLeadingNumber_parsesPrefix() throws Exception {
        // Direct on helper for the heuristic (still private, via reflection)
        assertEquals(1.0f, invokeResolveQuantity(amtExpr("x", "1*pop")));
        assertEquals(4.5f, invokeResolveQuantity(amtExpr("x", "4.5*pop")));
        assertEquals(10.0f, invokeResolveQuantity(amtExpr("x", "10")));
        // static amount path
        assertEquals(7.0f, invokeResolveQuantity(amt("x", 7f)));
        // Leading non-digit expr (e.g. "*pop") hits split[0] AIOOBE in current resolveQuantity impl.
        // This path is not reached via normal productions.csv parsing (amount exprs come from the value side of k:v and are numeric or parsable "1").
        // Pin the crash behavior for the bad case so future changes to the heuristic are noticed.
        // Note: m.invoke wraps the real exception; we assert on the InvocationTargetException + cause.
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> invokeResolveQuantity(amtExpr("x", "*pop")));
        assertTrue(ite.getCause() instanceof ArrayIndexOutOfBoundsException, "root cause should be the split[0] AIOOBE");
    }

    @Test
    void calculate_cycleInRecipeGraph_fallsBackToOne() throws Exception {
        clearRecipes();
        // A produces B, B produces A → recursion hits visiting -> MAX for the back edge.
        // The "inner" resource (b when entered from a) sees only the cycling recipe as invalid,
        // so its minEffort==MAX → fallback sets 1.0 and returns it.
        // The "outer" (a) then computes using that fallback-dep: total = 1(cycle) + 1*1 = 2, /1 = 2.
        // QUIRK (pinned): pure cycle pair does not yield 1.0 at every level; the entry point gets
        // cycleTime + depFallback(1). This test pins CURRENT behavior; if it changes the vault note
        // must be updated. Harmless for estimator (still finite, not free 0).
        putRecipe(simple("make_b", 1.0f, List.of(amt("a", 1f)), List.of(amt("b", 1f))));
        putRecipe(simple("make_a", 1.0f, List.of(amt("b", 1f)), List.of(amt("a", 1f))));
        invokeCalculateEstimatedValues();
        assertEquals(2.0f, ProductionRegistry.getEffort("a"));
        assertEquals(1.0f, ProductionRegistry.getEffort("b")); // the "deeper" in this insertion order gets the raw fallback
    }

    @Test
    void calculate_partialCycle_prefersNonCyclingPath() throws Exception {
        clearRecipes();
        // good: C produces D directly, effort = 2/1 = 2
        // bad: D also has self-ref path (cycle) which is skipped
        putRecipe(simple("good_d", 2.0f, List.of(), List.of(amt("d", 1f))));
        putRecipe(simple("self_d", 99.0f, List.of(amt("d", 1f)), List.of(amt("d", 1f))));
        invokeCalculateEstimatedValues();
        assertEquals(2.0f, ProductionRegistry.getEffort("d"));
        assertEquals(40.0f, ProductionRegistry.getEstimatedValue("d"));
    }

    @Test
    void calculate_zeroCycleZeroInputs_yieldsZeroEffort() throws Exception {
        clearRecipes();
        putRecipe(simple("instant", 0.0f, List.of(), List.of(amt("magic", 1f))));
        invokeCalculateEstimatedValues();
        assertEquals(0.0f, ProductionRegistry.getEffort("magic"));
        assertEquals(0.0f, ProductionRegistry.getEstimatedValue("magic"));
    }

    @Test
    void calculate_mixedBaseAndProduced_accumulatesOnlyProducedCosts() throws Exception {
        clearRecipes();
        // raw "ore" is input-only (effort 0 when recursed)
        // smelt: cycle 5, ore:2, output ingot:1 → effort = 5 + 2*0 = 5
        putRecipe(simple("smelt", 5.0f, List.of(amt("ore", 2f)), List.of(amt("ingot", 1f))));
        invokeCalculateEstimatedValues();
        assertEquals(5.0f, ProductionRegistry.getEffort("ingot"));
        assertEquals(100.0f, ProductionRegistry.getEstimatedValue("ingot"));
        assertEquals(1.0f, ProductionRegistry.getEffort("ore")); // not a produced output
    }

    @Test
    void getAllEstimatedValues_returnsOnlyComputedOutputs_unmodifiable() throws Exception {
        clearRecipes();
        putRecipe(simple("one", 1.0f, List.of(), List.of(amt("alpha", 1f))));
        invokeCalculateEstimatedValues();
        Map<String, Float> all = ProductionRegistry.getAllEstimatedValues();
        assertEquals(1, all.size());
        assertEquals(20.0f, all.get("alpha"));
        assertThrows(UnsupportedOperationException.class, () -> all.put("x", 9f));
    }

    @Test
    void calculate_afterClear_previousValuesAreGone() throws Exception {
        clearRecipes();
        putRecipe(simple("r1", 1.0f, List.of(), List.of(amt("temp", 2f))));
        invokeCalculateEstimatedValues();
        assertEquals(10.0f, ProductionRegistry.getEstimatedValue("temp"));

        // Now clear and recalc with different content
        clearRecipes();
        putRecipe(simple("r2", 4.0f, List.of(), List.of(amt("temp", 1f))));
        invokeCalculateEstimatedValues();
        // effort now 4/1=4, est=80 (old value must not leak)
        assertEquals(4.0f, ProductionRegistry.getEffort("temp"));
        assertEquals(80.0f, ProductionRegistry.getEstimatedValue("temp"));
    }
}
