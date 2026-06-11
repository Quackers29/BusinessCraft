---
tags:
  - detail
  - production
---
# Upgrade Cost and Research Time Scaling

**Breadcrumb**: Production > Upgrades > Upgrade Cost and Research Time Scaling
**TL;DR**: The resources and time required to research the next level (or repeat) of an upgrade are the base amounts multiplied by (costMultiplier raised to the town's current level for that node); resource costs are rounded up to the next whole item while research minutes keep the fractional scale. Current level 0 (never unlocked) always uses exactly the base (×1.0).

## What it does
Upgrades are defined once in a data file with a base cost in resources and a base research time in minutes. To prevent the same cheap cost applying forever, the town system automatically makes each subsequent tier more expensive and slower. The scaling keeps the CSV files short and readable while still producing reasonable progression curves for players and the town's research AI.

## How it works (process view)
- The town keeps a level counter per upgrade node (0 = not yet unlocked).
- When computing what it will cost to gain the next level, the code asks "what is the cost at my current level?"
- A multiplier is computed as costMultiplier ^ currentLevel. For the very first unlock (currentLevel=0) this is always 1.0, so you pay the base amounts listed in the CSV.
- Each base resource cost is multiplied by the multiplier and then ceiled (rounded up) so you never pay a fraction of an item.
- Research time is multiplied the same way but stays a float (the research progress accumulator handles fractions of a minute).
- Before showing or charging a cost, the system also checks repeatability rules: non-repeatable nodes cannot be started once level ≥ 1; repeatable nodes stop after their maxRepeats cap (if set).
- **Worked example (costMultiplier 1.1, base wood:10, research 5 min)**:
  - Current level 0 → multiplier = 1.1^0 = 1.0 → wood cost = ceil(10 × 1.0) = 10, research = 5 × 1.0 = 5 min.
  - After first unlock (now level 1) → multiplier = 1.1^1 = 1.1 → wood cost = ceil(10 × 1.1) = 11, research = 5 × 1.1 = 5.5 min.
  - After second (level 2) → 1.1^2 = 1.21 → wood = ceil(10 × 1.21) = 13 (12.1 → 13), research ≈ 6.05 min.
- The same multiplier is intentionally reused for both resource costs and research time so that "harder tiers" cost more and take longer in a consistent way.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownUpgradeComponent.getUpgradeCost(String nodeId)` | `common/src/main/java/com/quackers29/businesscraft/town/components/TownUpgradeComponent.java` (lines 96-124) | Returns the list of ResourceAmount costs for the *next* level, applying pow + ceil scaling. |
| `TownUpgradeComponent.getScaledResearchMinutes(String nodeId)` | same file (lines 151-160) | Returns the research duration (in minutes) for the next unlock, using the identical multiplier. |
| `TownUpgradeComponent.getUpgradeLevel(String)` / `unlockNode` / `canAffordResearch` | same file | Current level lookup (defaults to 0), the method that actually spends the scaled costs, and the pre-flight affordability + repeatability gate that calls the cost calculator. |
| `UpgradeNode.getCostMultiplier()` / `getCosts()` / `getResearchMinutes()` / `isRepeatable()` / `getMaxRepeats()` | `common/src/main/java/com/quackers29/businesscraft/production/UpgradeNode.java` | Supplies the base numbers and rules that the scaler multiplies and checks. |
| `recalculateModifiers()` (internal) | TownUpgradeComponent | After any level change, re-applies scaled *effects* (see Effect Value Calculation) plus any flat modifiers. |

## Rules & formulas (exact)
From the code (TownUpgradeComponent:116 and 121):

```java
float multiplier = (float) Math.pow(node.getCostMultiplier(), currentLevel);

for (ResourceAmount ra : costs) {
    effectiveCosts.add(new ResourceAmount(ra.resourceId, (int) Math.ceil(ra.amount * multiplier)));
}
```

and for time (line 158-159):

```java
float multiplier = (float) Math.pow(node.getCostMultiplier(), currentLevel);
return node.getResearchMinutes() * multiplier;
```

- `currentLevel` comes from the component's internal map (getOrDefault 0). The comment explicitly states this is "cost of *next* level": current=0 means you are buying level 1 (mult^0 = 1.0).
- `Math.pow(..., 0)` is defined as 1.0 for any costMultiplier (including 0 or negative, though configs are positive >0).
- Resource amounts are cast to int after `Math.ceil`; negative or NaN results are not expected in normal play.
- Research minutes are returned as float (no ceil) so sub-minute progress can accumulate in `researchProgress`.
- Repeatability guard (used by canAffordResearch and startResearch):
  - If `!node.isRepeatable()` and currentLevel >= 1 → blocked.
  - Else if `maxRepeats != -1` and currentLevel >= maxRepeats → blocked.
- `canAffordResearch` first runs the repeatability gate, then fetches the *scaled* costs via getUpgradeCost, then compares against `town.getTrading().getStock(resourceId)`.
- After a successful startResearch the scaled resources are deducted via `adjustStock` (tourism_* stats are explicitly skipped even if listed in costs).

## Edge cases & behaviors
- Node not in registry or null → getUpgradeCost returns empty list; getScaledResearchMinutes returns 0; canAffordResearch returns false.
- Empty or null cost list on the node → returns empty list (no cost to pay).
- currentLevel = 0 → always exactly base (×1.0) after pow.
- costMultiplier = 1.0 (or within float noise of 1.0) → every level costs exactly the base amounts.
- Fractional intermediate (e.g. 10 × 1.15 = 11.5) → ceil gives 12 for resources.
- Very large level or multiplier → (int) cast after ceil can lose precision or go negative if > Integer.MAX_VALUE; not guarded (production data is expected to stay reasonable).
- Research time can become fractional (5.5 min); the tick accumulator adds speedModifier/1200 per tick and compares >= scaled value.
- Non-repeatable nodes with level 0 still allow the first unlock; once level 1 they are hard-blocked even if you have the resources.
- Wanted resources or tourism_* pseudo-resources may appear in costs but tourism_* are never actually deducted.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/components/TownUpgradeComponentTest.java`
- Covered (14 tests): base cost/time passthrough at level 0, pow+ceil resource scaling at level 1/2/4, float research minutes scaling (fractional preserved), default getUpgradeLevel=0, null/unknown node guards, empty costs guard, repeatability (non-repeatable + maxReached) gates, canAffordResearch stock comparison using virtual resource ids + flat storage_cap_all injection. All formulas asserted with hand-computed comments.
- Not covered in unit tests: full startResearch/completeResearch (notifications, TownManager, cost deduction side effects), AI selection, tick progress, chained unlocks, NBT save/load of levels (indirect), real ResourceRegistry CSV load (T-030). canAfford paths use virtual resource ids to avoid needing a full platform+registry double in this test.
- Reflection used to inject upgradeLevels (and unlockedNodes) directly; McBootstrap + TestPlatformHelper + @TempDir + NODES snapshot/restore (pattern from T-030).

## Open questions
- The comment "For Level 1 (current=0)" is slightly confusing on first read but the math is correct (pow^0). A future rename of the variable or a clearer javadoc would help readers.
- Resource costs are ceiled while research time is not — intentional (you can't pay half a log) but worth confirming it never produces surprising "13 instead of 12" jumps for players on common 1.1 or 1.2 multipliers.
- No overflow guard on the final (int) cast for huge scaled costs; in practice limited by town storage caps and sell quantity hard-cap of 10000 elsewhere.
- Tourism_* resources listed in costs are silently ignored on deduction but still appear in the getUpgradeCost list returned to UI/AI — this asymmetry is documented in code but easy to miss.

## Related
- [[Production/Upgrades/Effect Value Calculation]] (T-016 — the sibling scaling formula that turns unlocked levels into active stat modifiers)
- [[Production/Upgrades/Upgrade Registry Loading and Lookup]] (T-030 — where the base costMultiplier, researchMinutes, repeatability flags, and ResourceAmount lists come from)
- [[Production/Production Overview]]
- [[Town/Trading/Stock and Capacity Resolution]] (T-031 — the virtual stock used by canAffordResearch checks)
