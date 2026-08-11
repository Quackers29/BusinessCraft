---
tags:
  - detail
  - economy
---
# Distance Payment Calculation

**Breadcrumb**: Economy > Tourist Payments > Distance Payment Calculation
**TL;DR**: An arrival batch pays the destination town `floor(max(1, avgDistanceTraveled ÷ metersPerEmerald × touristCount))` emeralds — real traveled distance (capped at 2× the straight-line origin→destination distance), default rate 1000 m/emerald, minimum 1 per batch, 0 if no distance recorded **or** if the average distance is below `minPayoutDistance` (default 100 blocks).

## What it does
When tourists arrive at a destination town, the town earns emeralds based on how far the tourists actually traveled. This is the core formula of the entire mod's economy. The payment lands on the destination town's Payment Board as part of a bundled "tourist arrival" reward (fare plus any milestone bonuses), which players can then claim.

## How it works (process view)
- Tourists arriving within ~1 second of each other from the same origin town are grouped into one batch.
- The distance used is the **real path each tourist traveled** (tracked by the tourist entity itself), averaged across the batch — not the straight-line distance between towns.
- **Anti-cheat cap**: each tourist's traveled distance is clamped to at most **2× the straight-line distance** between origin town and destination block before it enters the batch average. Winding routes still pay more than direct ones, but loop-back track farming (riding circles to inflate the counter) is capped.
- Payment = average distance ÷ rate × number of tourists, rounded down, with a minimum of 1 emerald per batch.
- **Minimum payout distance**: if the batch's average distance is below `minPayoutDistance` (default 100 blocks), the batch pays **nothing** — this blocks farming the 1-emerald floor with adjacent towns (real minimum town spacing is ~55 blocks).
- **Worked example**: 2 tourists from Town A traveled 4000 and 6000 blocks → average 5000 → (5000 ÷ 1000) × 2 = **10 emeralds**.
- The rate (`metersPerEmerald`, default 1000) and `minPayoutDistance` are configurable in `businesscraft.toml` under `[economy]` and hot-reload live.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `VisitorProcessingHelper.calculatePayment(VisitBuffer, VisitHistoryRecord)` | `common/src/main/java/com/quackers29/businesscraft/town/data/VisitorProcessingHelper.java` (~line 305) | **The formula.** Private method; computes emerald payment for one batch of arrivals from one origin town |
| `VisitorProcessingHelper.processIndividualVisitor(...)` | same file (~line 128) | Determines the distance per tourist: uses `TouristEntity.getTotalDistanceTraveled()` (real path traveled), clamped via `clampTraveledDistance`, when the villager is a `TouristEntity`; otherwise falls back to straight-line origin-town → destination-block distance |
| `VisitorProcessingHelper.clampTraveledDistance(traveled, straightLine)` | same file | **Anti-cheat clamp** (public static, pure): `min(traveled, MAX_TRAVELED_DISTANCE_MULTIPLIER × straightLine)`; multiplier constant = 2.0 |
| `VisitorProcessingHelper.processVisitBuffer(...)` | same file (~line 242) | Orchestrates: records visits, captures average distance (before payment clears it), calls `calculatePayment`, checks milestones, creates the bundled reward |
| `VisitBuffer.addVisitor` / `updateVisitorDistance` / `getAverageDistance` | `common/src/main/java/com/quackers29/businesscraft/town/data/VisitBuffer.java` | Batches arrivals per origin town for ~1s; accumulates total distance and computes the per-batch average that feeds the formula |
| `ConfigLoader.metersPerEmerald` | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` (line 44) | The rate. Public static int, default `1000`, loaded from `[economy] metersPerEmerald` in `businesscraft.toml` |
| `ConfigLoader.minPayoutDistance` | same file (just below `metersPerEmerald`) | Minimum average batch distance for any payout. Public static int, default `100`, loaded from `[economy] minPayoutDistance` |

## Rules & formulas (exact)
The formula (VisitorProcessingHelper.java line ~321):

```java
int payment = (int) Math.max(1, (averageDistance / ConfigLoader.metersPerEmerald) * record.getCount());
```

Per-tourist distance clamp (applied in `processIndividualVisitor` before the distance enters the batch):

```java
distance = Math.min(traveledDistance, 2.0 * straightLineDistance); // clampTraveledDistance
```

Min payout guard (start of `calculatePayment`, before the formula):

```java
if (averageDistance > 0 && averageDistance < ConfigLoader.minPayoutDistance) {
    visitBuffer.clearSavedDistance(record.getOriginTownId());
    return 0;
}
```

- `averageDistance` — mean **clamped** distance in blocks (= meters) across all tourists in the batch from one origin town.
- `record.getCount()` — number of tourists in the batch.
- **Order of operations matters**: `Math.max(1, x)` is applied BEFORE the `(int)` cast. So the result is `floor(max(1, distance/rate × count))` for positive values.
- **Minimum payment**: any batch with `averageDistance >= minPayoutDistance` pays at least 1 emerald **total** (not per tourist).
- **Minimum payout distance**: `averageDistance < minPayoutDistance` (and > 0) → pays 0 and the saved distance is cleared (so a later config change can't retro-pay the batch). The threshold is inclusive: exactly `minPayoutDistance` pays. Milestones are unaffected by this guard (they use the captured average directly), but the clamp applies to both since it happens upstream.
- **Truncation**: fractional emeralds are dropped. 99 blocks, 1 tourist, rate 50, min 0 → 1.98 → pays 1.
- **Zero/no distance**: if `averageDistance <= 0` the method returns 0 (no payment, logs a warning) — the `max(1, ...)` floor does NOT apply.
- **Clamp edge**: if origin and destination are at the same position (straight line 0), the clamp forces distance 0 → no payment.
- After a successful payment, the batch's saved distance is cleared (`VisitBuffer.clearSavedDistance`) so it can't be double-paid.

### Batching context
- `VisitBuffer.shouldProcess()` triggers processing once the buffer is non-empty and 1000ms have passed since the last arrival.
- The average distance is captured by the caller BEFORE `calculatePayment` runs, because payment clears the stored distance and milestones (`DistanceMilestoneHelper`) need it too.
- Payment is issued as emerald ItemStacks inside a bundled Payment Board reward (`RewardSource.TOURIST_ARRIVAL`) claimable by "ALL", with metadata: origin town, tourist count, fare amount, milestone info.

## Edge cases & behaviors
- `averageDistance == 0` → payment 0, warning logged ("skipping payment").
- Tiny distance (e.g. 1 block) → pays exactly 1 emerald (minimum floor).
- Fractional result → truncated toward zero (1.98 → 1).
- `count == 0` with distance > 0 → formula yields `max(1, 0) = 1` emerald. Cannot occur in practice (buffer records always have count ≥ 1), but the formula does not guard against it — see Open questions.
- `metersPerEmerald` is read live from the static field at calculation time, so config hot-reload affects in-flight payments immediately.
- Distance lookup falls back to `VisitBuffer.distanceMap` (persisted across `processVisits()`) since the visitors map is cleared before payment runs — this two-map dance is why distance survives between buffering and payment.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/VisitorProcessingHelperTest.java`
- Covered: basic formula, multi-tourist multiplication, buffer averaging of mixed distances, truncation, 1-emerald minimum, zero-distance → 0, custom config rate, distance cleared after payment (no double-pay), count=0 quirk.
- Anti-cheat coverage (July 2026): min-payout cutoff (below/exact/above threshold, many-tourist case, saved-distance cleared on skip) and `clampTraveledDistance` (direct route, winding-within-cap, loop-farming capped at 2×, exact 2× boundary, zero straight-line). Formula tests run with `minPayoutDistance = 0` so they exercise the pure formula.
- Not covered: the distance *measurement* itself (`TouristEntity.getTotalDistanceTraveled()` — needs MC entity bootstrap), reward board delivery, notifications.
- Note: `calculatePayment` is private, so tests invoke it via reflection. Pure logic otherwise.

## Open questions
- **count=0 quirk**: a record with 0 tourists and positive distance would pay 1 emerald due to `max(1, ...)`. Unreachable today (pinned by test), but if record construction ever changes, this becomes a free-emerald bug. A guard or extracting the formula into a public pure static method would harden it.
- The minimum payment is 1 emerald per *batch*, not per tourist — likely intentional. **(Resolved July 2026)** The very-short-route farming concern is now handled by `minPayoutDistance`: routes under the threshold pay 0.
- `calculatePayment` being private forces reflection in tests; consider extracting the formula to a public static pure method (post-v1 refactor candidate). The anti-cheat clamp `clampTraveledDistance` was extracted as public static pure from the start.
- The expiry timer still pauses while a tourist is moving (`TouristEntity.tick`), so loop-track tourists never expire — but the 2× clamp makes the accumulated distance economically worthless, so this is a performance/cosmetic concern only.

## Related
- [[Economy/Economy Overview]]
- [[Economy/Milestones/Distance Milestone Resolution]] (T-002 — consumes the same average distance)
- [[Town/Visits/Visit Buffer]] (T-010 — full VisitBuffer behavior)
- [[Town/Payment Board/Reward Claims]] (T-012 — where the payment lands)
