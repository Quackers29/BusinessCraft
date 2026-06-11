package com.quackers29.businesscraft.data.parsers;

import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-021: Config Data Parsing (Test + Docs Loop).
 *
 * Covers the three pure static parsers in DataParser:
 *   - parseEffects: bare flags (value 1.0), key*float, key:value, key:value% → Effect list
 *   - parseConditions: key:opvalue (op in >= <= > < =), % flag preserved in value, tolerant
 *   - parseResources: key:amount with pop/population_cap/tourist_count aliases, expr preserved
 *
 * All are string-to-structured-data transformations. No MC bootstrap, no registries,
 * no Level, no Town, no static config mutation. Pure logic only.
 *
 * Documentation:
 * vault/Config/Data Parsing.md
 */
class DataParserTest {

    // --- parseEffects ---

    @Test
    void parseEffects_defaultUpgradesExample_producesSevenEffectsWithCorrectValuesAndFlags() {
        // From UpgradeRegistry.createDefaultUpgrades: pop_cap:10;tourist_cap:2;storage_cap_all:200;happiness:50;population_maintenance;population_growth;basic_taxes
        // 4x "key:NN" (isPct=false) + 3x bare → value 1.0, isPct=false
        String packed = "pop_cap:10;tourist_cap:2;storage_cap_all:200;happiness:50;population_maintenance;population_growth;basic_taxes";
        List<Effect> effects = DataParser.parseEffects(packed);

        assertEquals(7, effects.size());
        assertEffect(effects.get(0), "pop_cap", 10f, false);
        assertEffect(effects.get(1), "tourist_cap", 2f, false);
        assertEffect(effects.get(2), "storage_cap_all", 200f, false);
        assertEffect(effects.get(3), "happiness", 50f, false);
        assertEffect(effects.get(4), "population_maintenance", 1.0f, false);
        assertEffect(effects.get(5), "population_growth", 1.0f, false);
        assertEffect(effects.get(6), "basic_taxes", 1.0f, false);
    }

    @Test
    void parseEffects_bareFlag_becomesValueOneNotPercent() {
        List<Effect> effects = DataParser.parseEffects("population_maintenance");
        assertEquals(1, effects.size());
        assertEffect(effects.get(0), "population_maintenance", 1.0f, false);
    }

    @Test
    void parseEffects_starForm_parsesMultiplierAsValueWithIsPctFalse() {
        // "key*mult" form (currently unused in defaults but implemented)
        List<Effect> effects = DataParser.parseEffects("speed*1.5;power*2");
        assertEquals(2, effects.size());
        assertEffect(effects.get(0), "speed", 1.5f, false);
        assertEffect(effects.get(1), "power", 2f, false);
    }

    @Test
    void parseEffects_percentValue_setsIsPctTrueAndStripsPercentFromStoredValue() {
        List<Effect> effects = DataParser.parseEffects("bonus:25%;tax:10%");
        assertEquals(2, effects.size());
        assertEffect(effects.get(0), "bonus", 25f, true);
        assertEffect(effects.get(1), "tax", 10f, true);
    }

    @Test
    void parseEffects_mixedFormsAndSpaces_handlesAllAndTrims() {
        String packed = " pop_cap : 10 ; happiness : > 50% ; bareflag ; mult * 3.0 ";
        // Note: "happiness:> 50%" goes to effects parser as no bare * and has :, so key="happiness", valStr="> 50%" → parseFloat fails → skipped
        // The space after * is inside the no-":" * branch split which trims.
        List<Effect> effects = DataParser.parseEffects(packed);
        // Only the valid ones survive: pop_cap:10 and "bareflag" (1.0) and "mult*3.0" (after trim)
        assertEquals(3, effects.size());
        assertEffect(effects.get(0), "pop_cap", 10f, false);
        assertEffect(effects.get(1), "bareflag", 1.0f, false);
        assertEffect(effects.get(2), "mult", 3.0f, false);
    }

    @Test
    void parseEffects_nullOrEmptyOrOnlySemicolons_returnsEmptyList() {
        assertTrue(DataParser.parseEffects(null).isEmpty());
        assertTrue(DataParser.parseEffects("").isEmpty());
        assertTrue(DataParser.parseEffects(" ; ;  ").isEmpty());
    }

    @Test
    void parseEffects_badFloatInValue_skipsOnlyThatSegment() {
        // 10 is good, "abc" bad (warn+skip), 5 good
        List<Effect> effects = DataParser.parseEffects("a:10;b:abc;c:5");
        assertEquals(2, effects.size());
        assertEffect(effects.get(0), "a", 10f, false);
        assertEffect(effects.get(1), "c", 5f, false);
    }

    @Test
    void parseEffects_colonButNotExactlyOnePair_skips() {
        List<Effect> effects = DataParser.parseEffects("bad::value;also:bad:extra");
        assertTrue(effects.isEmpty());
    }

    // --- parseConditions ---

    @Test
    void parseConditions_variousOperatorsAndPercent_preserveOpAndValueWithPctFlag() {
        // Note: "key:42" (no explicit =) yields operator="=" and clean value "42".
        // Using explicit "key:=42" leaves the "=" attached to value (observed current behavior, pinned).
        String packed = "happiness:>50;pop:>=100;surplus:>0%;tax:<=10%;exact:42;withEq:=99";
        List<Condition> conds = DataParser.parseConditions(packed);

        assertEquals(6, conds.size());
        assertCondition(conds.get(0), "happiness", ">", "50", false);
        assertCondition(conds.get(1), "pop", ">=", "100", false);
        assertCondition(conds.get(2), "surplus", ">", "0%", true);
        assertCondition(conds.get(3), "tax", "<=", "10%", true);
        assertCondition(conds.get(4), "exact", "=", "42", false);
        // Current parser behavior for explicit := prefix on equals case: value keeps the "="
        assertCondition(conds.get(5), "withEq", "=", "=99", false);
    }

    @Test
    void parseConditions_nullOrEmpty_returnsEmpty() {
        assertTrue(DataParser.parseConditions(null).isEmpty());
        assertTrue(DataParser.parseConditions("").isEmpty());
        assertTrue(DataParser.parseConditions(";;;").isEmpty());
    }

    @Test
    void parseConditions_missingColonOrExtra_warnsAndSkips() {
        List<Condition> conds = DataParser.parseConditions("no-colon;key:val:extra");
        assertTrue(conds.isEmpty());
    }

    @Test
    void parseConditions_valueKeepsPercentCharWhenFlagTrue() {
        List<Condition> conds = DataParser.parseConditions("thresh:>=75%");
        assertEquals(1, conds.size());
        Condition c = conds.get(0);
        assertEquals(">=", c.getOperator());
        assertEquals("75%", c.getValue());
        assertTrue(c.isPercentage());
    }

    // --- parseResources ---

    @Test
    void parseResources_defaultCostExample_normalizesAndParses() {
        // From default: wood:10
        List<ResourceAmount> res = DataParser.parseResources("wood:10;iron:5");
        assertEquals(2, res.size());
        assertResource(res.get(0), "wood", 10f, "10");
        assertResource(res.get(1), "iron", 5f, "5");
    }

    @Test
    void parseResources_populationAliases_areRewritten() {
        List<ResourceAmount> res = DataParser.parseResources("population:100;population_cap:200;tourist_count:5");
        assertEquals(3, res.size());
        assertResource(res.get(0), "pop", 100f, "100");
        assertResource(res.get(1), "pop_cap", 200f, "200");
        assertResource(res.get(2), "tourist", 5f, "5");
    }

    @Test
    void parseResources_nonNumericExpr_setsAmountZeroButPreservesExpression() {
        List<ResourceAmount> res = DataParser.parseResources("surplus:excess;level:current*2");
        assertEquals(2, res.size());
        assertResource(res.get(0), "surplus", 0f, "excess");
        assertResource(res.get(1), "level", 0f, "current*2");
    }

    @Test
    void parseResources_nullOrEmpty_returnsEmpty() {
        assertTrue(DataParser.parseResources(null).isEmpty());
        assertTrue(DataParser.parseResources("").isEmpty());
    }

    @Test
    void parseResources_badPairWithoutExactlyOneColon_isDroppedSilently() {
        List<ResourceAmount> res = DataParser.parseResources("wood:10;badpair;also:bad:extra");
        // Only the good pair survives
        assertEquals(1, res.size());
        assertResource(res.get(0), "wood", 10f, "10");
    }

    // --- helpers ---

    private void assertEffect(Effect e, String target, float value, boolean isPct) {
        assertEquals(target, e.getTarget());
        assertEquals(value, e.getValue(), 0.0001f);
        assertEquals(isPct, e.isPercentage());
    }

    private void assertCondition(Condition c, String target, String op, String value, boolean isPct) {
        assertEquals(target, c.getTarget());
        assertEquals(op, c.getOperator());
        assertEquals(value, c.getValue());
        assertEquals(isPct, c.isPercentage());
    }

    private void assertResource(ResourceAmount ra, String id, float amount, String expr) {
        assertEquals(id, ra.resourceId);
        assertEquals(amount, ra.amount, 0.0001f);
        assertEquals(expr, ra.amountExpression);
    }
}
