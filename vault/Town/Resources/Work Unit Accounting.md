---
tags:
  - detail
  - town
---
# Work Unit Accounting

**Breadcrumb**: Town > Resources > Work Unit Accounting
**TL;DR**: Work units (WU) are a virtual counter that production recipes can consume and produce; addWorkUnits and setWorkUnits clamp to >= 0 and (when a wu_cap upgrade modifier > 0) to the cap, using Math.addExact with explicit overflow handling to MAX/0 before the clamps.

## What it does
Work units act as a special "currency" inside a town for production balance — recipes can require WU as an input cost or generate WU as an output yield. The accounting methods guarantee the counter can never go negative and cannot silently wrap on integer overflow. An optional cap (provided by the "wu_cap" upgrade modifier) prevents a town from accumulating unlimited work units unless the player has researched the relevant upgrade.

## How it works (process view)
- Production recipes declare "wu" as a resourceId for inputs or outputs.
- When a recipe runs, the TownProductionComponent reads current WU via getWorkUnits(), checks against required (for inputs), then calls addWorkUnits(-amount) to consume or addWorkUnits(+amount) to produce.
- The cap is read live via getWorkUnitCap() which returns the "wu_cap" float modifier cast to long (0 when no upgrade provides it).
- **Worked example**: A town has 950 WU and a wu_cap of 1000. A recipe tries to add 100 WU → addExact(950, 100) = 1050 → post-clamp because cap>0 and 1050>1000 → stored value becomes 1000. If instead the current was Long.MAX_VALUE-10 and +100 arrives, overflow path sets to Long.MAX_VALUE, then clamp to cap if applicable.
- The rate/config for WU does not exist; the only tunables are the wu_cap modifier values on upgrade nodes and the recipe definitions that name "wu".

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `Town.getWorkUnits()` | `common/src/main/java/com/quackers29/businesscraft/town/Town.java` | Returns the current long WU counter (implements ITownDataProvider). |
| `Town.setWorkUnits(long amount)` | same | Direct assign after `Math.max(0, amount)`; used for load/overrides. |
| `Town.addWorkUnits(long amount)` | same | The core mutator. Can be negative (consume). Does addExact + overflow rewrite + post-clamps to 0 and optional cap; always calls markDirty(). |
| `Town.getWorkUnitCap()` | same | Returns `(long) upgrades.getModifier("wu_cap")` (0.0f when absent → 0L). |
| `TownProductionComponent` (wu branches) | `common/src/main/java/com/quackers29/businesscraft/town/components/TownProductionComponent.java` | Treats "wu" specially for stock checks, virtual current/cap, consume via negative add, produce via positive add; also multiplies expressions by current WU when "wu" appears in rate formulas. |

## Rules & formulas (exact)
From Town.java:

```java
public void setWorkUnits(long amount) {
    this.workUnits = Math.max(0, amount);
    markDirty();
}
```

```java
public void addWorkUnits(long amount) {
    long cap = getWorkUnitCap();
    try {
        this.workUnits = Math.addExact(this.workUnits, amount);
    } catch (ArithmeticException e) {
        this.workUnits = amount > 0 ? Long.MAX_VALUE : 0;
    }

    if (this.workUnits < 0)
        this.workUnits = 0;
    if (cap > 0 && this.workUnits > cap)
        this.workUnits = cap;

    markDirty();
}
```

```java
@Override
public long getWorkUnitCap() {
    // Cap determined by upgrades
    return (long) upgrades.getModifier("wu_cap");
}
```

- `getWorkUnits()` simply returns the private field (initial 0 in ctor).
- Cap is a float modifier from the upgrade system, truncated toward zero on cast to long. Only values > 0 trigger the upper clamp.
- Overflow handling happens *before* the <0 / cap clamps. Positive overflow → Long.MAX_VALUE (then possibly capped). Negative overflow/underflow in addExact → 0 (then the <0 line is redundant but present).
- Order: addExact (or rewrite) → floor at 0 → (optional) floor at cap.
- No rounding; everything is integer (long). Production callers cast amounts to int when calling add (e.g. `addWorkUnits(-(int) amount)`), which can lose high bits for |amount| > Integer.MAX but is outside this unit.

## Edge cases & behaviors
- amount = 0 to addWorkUnits or setWorkUnits → no change (stays at current, still >=0).
- setWorkUnits(negative) or add that drives below 0 → result 0.
- add positive when current + amount overflows Long → result Long.MAX_VALUE (or cap if cap>0 and MAX>cap).
- add negative when current + amount underflows → result 0.
- cap == 0 (default, no wu_cap modifier) → no upper clamp; value can reach Long.MAX_VALUE via repeated adds or set.
- cap > 0 → upper clamp applies after every add (including sets? no — set ignores cap and only does max(0, amount); only addWorkUnits respects cap).
- setWorkUnits can bypass cap (it does not read cap at all). This allows load or admin paths to force a value above cap; subsequent adds will then clamp it down.
- getWorkUnitCap() reflects live upgrade modifier; if an upgrade that grants wu_cap is added/removed the cap for future adds changes immediately.
- Orphan Town (never registered with a TownManager) causes markDirty() to emit a one-time WARN on every WU mutation — harmless for behavior, only logging side effect.
- Production expression multiplier "wu" uses the pre-clamp current value (see TownProductionComponent).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/TownTest.java`
- Covered: set/add happy paths, zero, negative amounts, positive overflow to MAX, negative underflow to 0, cap clamp when cap>0 (via reflection setup of activeModifiers), no-cap (cap=0) allows growth, set bypasses cap, getWorkUnitCap default 0, getWorkUnits.
- Not covered: actual upgrade research flow that populates "wu_cap" (that is in upgrade component + production tests), persistence of WU (covered indirectly via Town save/load in other tests), the int-cast truncation in production callers (pinned as quirk if relevant).
- Uses McBootstrap so Town can be constructed; ConfigLoader defaultStartingPopulation saved/restored for ctor determinism (even though WU math does not depend on it).

## Open questions
- setWorkUnits bypasses the cap while addWorkUnits enforces it — is this intentional (e.g. for loading a town that researched then lost the upgrade) or a latent "above-cap after load" state? Tests pin current behavior.
- Production code casts WU amounts to int before add/sub (e.g. `addWorkUnits(-(int) amount)`). For realistic recipe quantities this is invisible, but in principle a recipe with >2B WU delta would truncate. Harmless quirk today; recorded here.
- markDirty() WARN noise for test Towns that are not TownManager-registered — expected and ignored; no behavior impact.

## Related
- [[Town/Town Overview]]
- [[Town/Resources/Resource Storage Operations]] (T-007 — same Math.addExact + clamp-to-0-and-cap pattern for ordinary items)
- [[Town/Production/Recipe Execution and Dynamic Evaluation]] (T-029 — primary consumer/producer of WU via "wu" resourceId and expression factors)
- [[Production/Upgrades/Upgrade Cost and Research Time Scaling]] (T-032 — how "wu_cap" and other modifiers are scaled by level)
