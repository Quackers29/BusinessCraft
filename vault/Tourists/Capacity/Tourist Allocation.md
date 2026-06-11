---
tags:
  - detail
  - tourists
---
# Tourist Allocation

**Breadcrumb**: Tourists > Capacity > Tourist Allocation
**TL;DR**: From an origin town, the next tourist's destination among options is chosen to keep allocations proportional to each destination's population: the tracker computes (current - target) fairness gaps and picks the most under-allocated (most negative gap), with a 10% chance to pick randomly among under-allocated towns instead.

## What it does
This system tries to fairly distribute tourists originating from one town across possible destination towns according to the relative sizes (populations) of the destinations. Larger towns should receive proportionally more tourists over time. The tracker maintains a running count of "currently allocated" tourists per destination (from the origin's perspective) and uses that vs. a population-weighted target to bias the next choice toward whichever town is currently "behind" its fair share.

## How it works (process view)
- When a platform at an origin town is about to spawn a tourist and has specific enabled destination towns, `TouristSpawningHelper` builds a map of {destTownId → (int) destPopulation} (excluding the origin itself) and calls `TouristAllocationTracker.selectFairDestination(originId, popMap)`.
- Inside, for the origin's `DestinationTracker`:
  - If only one option → return it immediately.
  - If total population of options is 0 → pick uniformly at random.
  - Otherwise compute a "fairness gap" for every option: `currentCount - (pop / totalPop × totalActiveTouristsFromThisOrigin)`.
  - 10% of the time, if any towns have negative gap (under their target), pick one of them uniformly at random (adds variety).
  - Otherwise (90% + when no unders) pick the town with the lowest (most negative) gap — the most under-allocated.
- Every time a tourist is successfully spawned in production the intent is to call `recordTouristSpawn(origin, dest)` so the tracker's "current" counters go up; on arrival/expiry at the destination `recordTouristRemoval(origin, dest)` decrements them (this call *is* present).
- **Worked example (intended behavior)**: Origin O, destinations A (pop 10) and B (pop 90), totalPop=100. No tourists yet (totalTourists=0) → gaps are defined as 0. First selection picks the first in map iteration order (see quirks). Suppose A is chosen and `recordSpawn(O, A)` is called → totalTourists=1, current[A]=1. Next selection: gapA = 1 - (10/100 × 1) = +0.9 (over); gapB = 0 - 0.9 = -0.9 (under) → B is chosen (lowest gap). With the 10% roll it would still pick B because the only under-allocated town is B.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TouristAllocationTracker.selectFairDestination(UUID origin, Map<UUID,Integer> destIdToPop)` | `common/src/main/java/com/quackers29/businesscraft/town/utils/TouristAllocationTracker.java` | Public entry; returns the chosen dest UUID or null on guard. Delegates to a per-origin `DestinationTracker`. |
| `TouristAllocationTracker.recordTouristSpawn(UUID origin, UUID dest)` | same | Increments current count and totalTourists for that origin→dest pair (never called from production code today). |
| `TouristAllocationTracker.recordTouristRemoval(UUID origin, UUID dest)` | same | Decrements (clamped) the current count and total for the pair (called on tourist arrival processing). |
| `DestinationTracker.selectNextDestination(Map<UUID,Integer>)` (private) | same (inner) | Core selection with single-option fast path, totalPop==0 random, 10% under-random, and min-gap deterministic pick. |
| `DestinationTracker.calculateFairnessGap(UUID, int pop, int totalPop)` (private) | same (inner) | `if (totalPop==0 \|\| totalTourists==0) return 0; target = (pop/(double)totalPop) * totalTourists; return current - target;` (also caches target in targetAllocations map for debug stats). |
| `DestinationTracker.getAllocationStats()` | same (inner) | Debug string "Total tourists: N, Allocations: {uuid8: cur/target, ...}". |

## Rules & formulas (exact)
Selection entry guards (lines 60-62):
```java
if (originTownId == null || destinationOptions == null || destinationOptions.isEmpty()) {
    return null;
}
```

Inside `selectNextDestination`:
- `if (size == 1) return the only key;`
- `totalPopulation = sum(values);`
- `if (totalPopulation == 0) { List<UUID> keys = new ArrayList<>(keySet); return keys.get(new Random().nextInt(size)); }`
- For each option compute `fairnessGaps.put(id, calculateFairnessGap(id, pop, totalPop));`
- `if (random.nextDouble() < 0.10) { collect all with gap < 0; if (!empty) return random one from that list; }`
- Then linear scan: `lowestGap = MAX_VALUE; for each if (gap < lowestGap) { lowest= gap; selected = id; }` (note: on exact ties keeps the *first* seen in iteration order because strict `<`).
- Return selected (may still be null only if map was empty after guards, which shouldn't happen).

Fairness gap (lines 112-127):
```java
if (totalPopulation == 0 || totalTourists == 0) return 0;
double targetProportion = (double) population / totalPopulation;
double targetCount = targetProportion * totalTourists;
int currentCount = currentAllocations.getOrDefault(townId, 0);
targetAllocations.put(townId, targetCount);
return currentCount - targetCount;
```
- Gap > 0 ⇒ over-allocated relative to pop share of current active.
- Gap < 0 ⇒ under-allocated (the primary signal for preference).
- The `targetAllocations` map is write-only for stats; never read by selection logic.

Record spawn/removal simply mutate `currentAllocations` (+1 or -1 clamped) and `totalTourists++` / `max(0, --)` . Null ids are no-ops at the public API.

`recordTouristRemoval` when count already 0 or absent: silently does nothing (no negative counts).

## Edge cases & behaviors
- Null origin / null or empty options map → immediate null return (no selection).
- Single destination option → always returned, regardless of population or prior allocations.
- All destination pops = 0 → uniform random choice among the keys (new Random() each call, unseeded).
- First call ever (or after clears) with positive pops → totalTourists==0 inside gap calc → all gaps=0 → deterministic "first in entrySet order" of the passed map (HashMap order undefined in practice).
- After `recordSpawn` calls have built positive totalTourists, gaps become meaningful and bias kicks in.
- 10% random-under only triggers when there exists at least one gap<0; otherwise falls through to the min-gap scan (which will be some 0 or positive).
- When multiple towns tie for the lowest gap, the first in map iteration order wins (because of `<` not `<=`).
- Removal of a non-tracked dest or when its count is already 0 leaves state unchanged (and total clamped at 0).
- The tracker is global static state keyed by origin UUID; concurrent towns from different origins are isolated, but same-origin spawns share the counters.
- `getAllocationStats()` only formats; used solely in DebugConfig logs.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/utils/TouristAllocationTrackerTest.java`
- Covered: all public API guards (null/empty), single-option fast path, zero-pop random choice (pinned as "one of"), deterministic most-under selection after seeding via recordSpawn (including cases where 10% random-under would still pick the same unique under), equal-gap tie behavior (first-in-order via LinkedHashMap), recordRemoval no-underflow + isolation, origin isolation, null safety on records, gap formula (via reflection on private inner method) with hand-computed values, and @BeforeEach state reset via reflection on private static final map.
- 12 tests total. All formulas from Rules & formulas section have corresponding assertions.
- Not covered (by design): stochastic distribution of the 10% branch, live MC integration (Town, Platform, TouristSpawningHelper require world/bootstrap), Debug side-effects.
- Note: `selectNextDestination` / `calculateFairnessGap` are private (inner); exercised through public API + targeted reflection. Matches T-001 style (reflection + Config/static save-restore pattern adapted to tracker map clear). Full suite was green after these tests.
- Test file link: common/src/test/java/com/quackers29/businesscraft/town/utils/TouristAllocationTrackerTest.java

## Open questions
- **recordTouristSpawn is dead in production**: the method and the increment side of the fairness model exist and are documented, but no caller ever invokes `TouristAllocationTracker.recordTouristSpawn(...)`. Only `selectFairDestination` (from TouristSpawningHelper) and `recordTouristRemoval` (from VisitorProcessingHelper on arrival) are wired. Consequence: `totalTourists` and `currentAllocations` stay at 0 for every origin in real play; the gap math is never active; selection always reduces to "first entry in the HashMap iteration order of possible destinations" (or random when all pops reported 0). This is a partial-implementation quirk/bug — the fairness intent is not realized today. Pinned by tests that deliberately drive recordSpawn; if a future change wires the spawn record, update this note and re-verify.
- When pops are equal and no prior allocations the choice is the first key in the caller's map order. Since callers build via `new HashMap<>()` + puts from `getAllTowns()` (itself a HashMap), the order is not guaranteed — effectively "arbitrary but stable per JVM run." A LinkedHashMap or explicit sort would make it deterministic.
- The 10% "explore under-allocated" uses a fresh `new Random()` with no seed and no way for tests to force the branch. Tests therefore only assert stable outcomes (unique most-under cases always pick it; multi-under "one-of" cases are not asserted for exact id).
- The class javadoc and field names talk about "capacity" and "fair distribution", but the actual per-town tourist *capacity* (max concurrent) is now supplied by the production upgrade system (`town.getUpgrades().getModifier("tourist_cap")`) inside `TownService.calculateMaxTourists`, not by this tracker. Population still matters for (a) spawn eligibility (`pop >= ConfigLoader.minPopForTourists`) and (b) the weights passed into this allocation. Old pop-based formulas (`populationPerTourist`, `maxPopBasedTourists`) remain in ConfigLoader but are unused by the current max calc.
- `targetAllocations` map is populated as a side effect of gap calculation but only consumed by the debug stats string. It is never cleared on removal, so targets can be stale relative to current totalTourists after many cycles.

## Related
- [[Tourists/Tourists Overview]]
- [[Town/Town Overview]] (population, canAddMoreTourists, touristCount vs. max)
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001) and milestones consume the same tourist flow
- [[Town/Visits/Visit Buffer]] (T-010 — pending) — the other half of arrival batching
