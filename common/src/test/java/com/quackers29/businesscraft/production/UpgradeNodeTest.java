package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.data.parsers.Effect;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T-016: Effect Value Calculation (Test + Docs Loop).
 *
 * Covers the pure scaling formula in UpgradeNode.calculateEffectValue():
 *   - level <= 0 → 0
 *   - benefitMultiplier ≈ 1.0 (default or explicit) → linear: baseValue * level
 *   - otherwise → exponential: baseValue * (benefitMult ^ (level-1))
 *
 * The method is public and depends only on Effect (plain data) + int level + the
 * node's parsed benefitMultiplier. No registries, no Level, no Town, no static
 * config — pure logic.
 *
 * Documentation:
 * vault/Production/Upgrades/Effect Value Calculation.md
 */
class UpgradeNodeTest {

    // --- helpers to construct a node with a specific benefit multiplier via repeatConfig ---

    private UpgradeNode nodeWithBenefitMult(float mult) {
        // repeatConfig format: "count:costMult:benefitMult" (we only care about benefit segment)
        String repeat = "1:1:" + mult;
        List<String> prereqs = Collections.emptyList();
        List<Effect> effects = Collections.emptyList();
        return new UpgradeNode("test_node", "test", "Test Node", repeat, prereqs, "desc", effects);
    }

    private UpgradeNode nodeLinearDefault() {
        // No benefit segment or 1.0 → linear
        List<String> prereqs = Collections.emptyList();
        List<Effect> effects = Collections.emptyList();
        return new UpgradeNode("lin", "cat", "Linear", "1", prereqs, "desc", effects);
    }

    private UpgradeNode nodeWithExplicitLinear() {
        List<String> prereqs = Collections.emptyList();
        List<Effect> effects = Collections.emptyList();
        return new UpgradeNode("lin2", "cat", "Linear2", "1:1:1.0", prereqs, "desc", effects);
    }

    private Effect effect(float value) {
        return new Effect("some_target", value, false);
    }

    private Effect effectPercent(float value) {
        return new Effect("pct_target", value, true);
    }

    // --- happy paths ---

    @Test
    void calculateEffectValue_levelOne_returnsBaseValue_linear() {
        UpgradeNode node = nodeLinearDefault();
        // level 1 always base, linear path
        assertEquals(10.0f, node.calculateEffectValue(effect(10.0f), 1), 1e-6f);
    }

    @Test
    void calculateEffectValue_levelOne_returnsBaseValue_exponential() {
        UpgradeNode node = nodeWithBenefitMult(2.0f);
        // 10 * 2.0^(1-1) = 10 * 1 = 10
        assertEquals(10.0f, node.calculateEffectValue(effect(10.0f), 1), 1e-6f);
    }

    @Test
    void calculateEffectValue_linearDefault_multipliesByLevel() {
        UpgradeNode node = nodeLinearDefault();
        // base 7 * level 4 = 28
        assertEquals(28.0f, node.calculateEffectValue(effect(7.0f), 4), 1e-6f);
    }

    @Test
    void calculateEffectValue_linearExplicitOne_multipliesByLevel() {
        UpgradeNode node = nodeWithExplicitLinear();
        // base 3 * 5 = 15
        assertEquals(15.0f, node.calculateEffectValue(effect(3.0f), 5), 1e-6f);
    }

    @Test
    void calculateEffectValue_exponential_compoundGrowth() {
        UpgradeNode node = nodeWithBenefitMult(1.5f);
        Effect e = effect(10.0f);
        // level 3: 10 * 1.5^(3-1) = 10 * (1.5 * 1.5) = 10 * 2.25 = 22.5
        assertEquals(22.5f, node.calculateEffectValue(e, 3), 1e-6f);
        // level 2: 10 * 1.5^1 = 15.0
        assertEquals(15.0f, node.calculateEffectValue(e, 2), 1e-6f);
    }

    @Test
    void calculateEffectValue_exponential_doubleMultiplier_sequence() {
        // Matches the example comment in UpgradeNode: base 2, mult 2 → 2, 4, 8 ...
        UpgradeNode node = nodeWithBenefitMult(2.0f);
        Effect e = effect(2.0f);
        // L1: 2 * 2^0 = 2
        assertEquals(2.0f, node.calculateEffectValue(e, 1), 1e-6f);
        // L2: 2 * 2^1 = 4
        assertEquals(4.0f, node.calculateEffectValue(e, 2), 1e-6f);
        // L3: 2 * 2^2 = 8
        assertEquals(8.0f, node.calculateEffectValue(e, 3), 1e-6f);
    }

    // --- edges from vault note ---

    @Test
    void calculateEffectValue_levelZero_returnsZero() {
        UpgradeNode node = nodeLinearDefault();
        assertEquals(0.0f, node.calculateEffectValue(effect(99.0f), 0), 1e-6f);
    }

    @Test
    void calculateEffectValue_negativeLevel_returnsZero() {
        UpgradeNode node = nodeWithBenefitMult(3.0f);
        assertEquals(0.0f, node.calculateEffectValue(effect(50.0f), -1), 1e-6f);
    }

    @Test
    void calculateEffectValue_zeroBaseValue_alwaysZero() {
        UpgradeNode node = nodeWithBenefitMult(1.5f);
        assertEquals(0.0f, node.calculateEffectValue(effect(0.0f), 5), 1e-6f);
        assertEquals(0.0f, node.calculateEffectValue(effect(0.0f), 1), 1e-6f);
    }

    @Test
    void calculateEffectValue_fractionalBase_linear() {
        UpgradeNode node = nodeLinearDefault();
        // 2.5 * 3 = 7.5
        assertEquals(7.5f, node.calculateEffectValue(effect(2.5f), 3), 1e-6f);
    }

    @Test
    void calculateEffectValue_fractionalBase_exponential() {
        UpgradeNode node = nodeWithBenefitMult(1.1f);
        // 2.5 * 1.1^(4-1) = 2.5 * (1.1^3) = 2.5 * 1.331 = 3.3275
        assertEquals(3.3275f, node.calculateEffectValue(effect(2.5f), 4), 1e-6f);
    }

    @Test
    void calculateEffectValue_isPercentageFlag_doesNotAffectNumericResult() {
        UpgradeNode node = nodeLinearDefault();
        // isPercentage true vs false with same value must return identical scaled number
        assertEquals(12.0f, node.calculateEffectValue(effectPercent(3.0f), 4), 1e-6f);
        assertEquals(12.0f, node.calculateEffectValue(effect(3.0f), 4), 1e-6f);
    }

    @Test
    void calculateEffectValue_benefitMultiplierVeryCloseToOne_usesExponentialPath() {
        // The epsilon check: abs(mult - 1.0) > 0.0001 triggers exponential even for "almost 1"
        UpgradeNode node = nodeWithBenefitMult(1.0002f);
        // 10 * 1.0002^(2-1) = 10 * 1.0002 = 10.002
        assertEquals(10.002f, node.calculateEffectValue(effect(10.0f), 2), 1e-6f);
    }
}
