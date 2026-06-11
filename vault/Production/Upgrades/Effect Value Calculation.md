---
tags:
  - detail
  - production
---
# Effect Value Calculation

**Breadcrumb**: Production > Upgrades > Effect Value Calculation
**TL;DR**: An upgrade effect at a given level is `baseValue × level` (linear, default) or `baseValue × benefitMult^(level-1)` (exponential when benefitMult != 1.0); level ≤ 0 always returns 0. The same pure function is used for server-side application and client UI display.

## What it does
Upgrade nodes define "effects" (e.g. +10 to pop_cap, +2 to tourist_cap, +50 to happiness). Because upgrades can be leveled (or repeated), the actual numeric benefit at level N is not just the base — it is scaled by this formula. This ensures that when the town has "Basic Settlement at level 3", the displayed and applied bonus matches exactly, whether you're looking at the upgrade screen or the town stats are being recalculated.

## How it works (process view)
- Each UpgradeNode carries a benefitMultiplier (parsed from the repeat column in upgrades.csv as the third ":" segment, defaults to 1.0).
- When the town's upgrade level for that node is queried (e.g. in TownUpgradeComponent or viewmodel builders), the node is asked: "what is the effective value of this Effect at level L?"
- If benefitMultiplier is 1.0 (within 0.0001 tolerance), the effect uses simple linear scaling: the base value is multiplied by the integer level.
- Otherwise it uses compound/exponential scaling: base × (mult raised to level-1). Level 1 always yields exactly the base (any mult ^ 0 = 1).
- **Worked example (exponential)**: Effect value=10, benefitMult=1.5, level=3 → 10 × (1.5 ^ 2) = 10 × 2.25 = 22.5. At level 1: 10 × 1.0 = 10. At level 2: 10 × 1.5 = 15.
- **Worked example (linear/default)**: Effect value=5, benefitMult=1.0, level=4 → 5 × 4 = 20.
- The isPercentage flag on Effect is ignored by the scaler (it only affects how the value is later rendered or interpreted as +X or +X%).

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `UpgradeNode.calculateEffectValue(Effect effect, int level)` | `common/src/main/java/com/quackers29/businesscraft/production/UpgradeNode.java` (lines 136-158) | The scaling formula. Public so viewmodels and game logic can share identical math without duplication. |
| `UpgradeNode` (ctor + parseRepeatConfig) | same file | Parses the "repeat:costMult:benefitMult" config segment; benefitMultiplier defaults to 1.0f (linear) when absent. |
| `Effect` | `common/src/main/java/com/quackers29/businesscraft/data/parsers/Effect.java` | Simple value carrier: target id, base float value, isPercentage flag (the scaler only reads getValue()). |
| `TownUpgradeComponent`, `UpgradeStatusViewModelBuilder`, `TownResourceViewModelBuilder` | various | Callers that pass the current town/node level and an Effect to obtain the scaled number for application or display. |

## Rules & formulas (exact)
From UpgradeNode.java:136:

```java
public float calculateEffectValue(Effect effect, int level) {
    if (level <= 0)
        return 0f;

    boolean useExponentialBenefit = Math.abs(benefitMultiplier - 1.0f) > 0.0001f;

    if (useExponentialBenefit) {
        return effect.getValue() * (float) Math.pow(benefitMultiplier, level - 1);
    } else {
        return effect.getValue() * level;
    }
}
```

- `level <= 0` → 0 (guards bad data or "not unlocked").
- Detection of "exponential mode": `abs(mult - 1.0f) > 0.0001f`. Anything sufficiently different from 1.0 (including 0.999 or 1.0002) triggers pow path.
- Exponential: `base × mult^(level-1)` cast to float after pow. Level 1 → mult^0 = 1.0 × base.
- Linear (mult ≈ 1): `base × level` (accumulation style).
- All arithmetic is 32-bit float; no explicit rounding or clamping inside the method.
- The method does not look at Effect.isPercentage(); callers decide whether the returned number is an absolute delta or a percent.

## Edge cases & behaviors
- level = 0 or negative → exactly 0.0f (even if base value is huge).
- level = 1 → always exactly the Effect's base value (any valid mult).
- benefitMultiplier = 1.0f (or within 1e-4) → linear path, even if the CSV intended "1" as a no-op.
- benefitMultiplier = 0.0f → exponential path yields 0 for level > 1 (level 1 still base).
- benefitMultiplier < 1.0 (e.g. 0.9) → diminishing returns in exponential mode.
- benefitMultiplier > 1.0 (e.g. 2.0) → rapid growth (example in code comment: base 2, mult 2 → 2, 4, 8, ...).
- Very high level (e.g. 100 with mult 1.1) → can produce very large or Infinity float values (no guard in the scaler).
- Fractional base values are preserved (e.g. 2.5 × 3 = 7.5 in linear).
- NaN/Inf in base value propagates (Math.pow and * behave per IEEE).
- The isPercentage flag on the Effect has no effect on the numeric result.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/production/UpgradeNodeTest.java`
- Covered: linear default path, explicit exponential, level 0/negative/1 boundaries, different multipliers (including near-1 epsilon case), hand-computed expectations with arithmetic comments for each (14 tests); isPercentage flag confirmed to have no numeric impact; fractional bases; zero base.
- Not covered: integration with actual upgrade application or recipe conditions (those live in components that require a Town/Level); parsing of UpgradeNode from CSV (covered indirectly via registry tests if added later); floating-point precision at extreme levels.
- No McBootstrap required — pure arithmetic on plain data objects (Effect is a simple 3-field class).

## Open questions
- Why does the exponential check use a 0.0001f epsilon instead of exact `!= 1.0f`? Prevents accidental float noise from CSV parse, but means a deliberately set 1.00005 would be treated as exponential.
- The isPercentage flag lives on Effect but is never consulted by calculateEffectValue. Is the "value" field already the final number to add (absolute or percent), or do callers multiply by 100 later? (Current behavior: value is used as-is.)
- No upper bound or sanitization on the returned value. If a town somehow reaches level 50 on a 2× multiplier node, the effect can explode — is that intended or should there be a clamp at the caller?
- ProductionRegistry contains a much more complex recursive effort/price estimator (recursiveGetEffort) that is currently untested and private; this T-016 note only covers the simpler, public, per-upgrade effect scaler.

## Related
- [[Production/Production Overview]]
- [[Town/Town Overview]] (upgrade effects are applied to town stats)
