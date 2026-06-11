# Distance Payment Calculation

**Breadcrumb**: Economy > Tourist Payments > Distance Payment Calculation

## What it does
When tourists arrive at a destination town, the town earns emeralds based on how far the tourists actually traveled. This is the core formula of the entire mod's economy: distance traveled ÷ a configurable rate (`metersPerEmerald`, default 50) × number of tourists in the arrival batch. The payment is delivered to the destination town's Payment Board as part of a bundled `TOURIST_ARRIVAL` reward (fare + any milestone rewards combined).

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `VisitorProcessingHelper.calculatePayment(VisitBuffer, VisitHistoryRecord)` | `common/src/main/java/com/quackers29/businesscraft/town/data/VisitorProcessingHelper.java` (~line 305) | **The formula.** Private method; computes emerald payment for one batch of arrivals from one origin town |
| `VisitorProcessingHelper.processIndividualVisitor(...)` | same file (~line 128) | Determines the distance per tourist: uses `TouristEntity.getTotalDistanceTraveled()` (real path traveled) when the villager is a `TouristEntity`, otherwise falls back to straight-line origin-town → destination-block distance |
| `VisitorProcessingHelper.processVisitBuffer(...)` | same file (~line 242) | Orchestrates: records visits, captures average distance (before payment clears it), calls `calculatePayment`, checks milestones, creates the bundled reward |
| `VisitBuffer.addVisitor` / `updateVisitorDistance` / `getAverageDistance` | `common/src/main/java/com/quackers29/businesscraft/town/data/VisitBuffer.java` | Batches arrivals per origin town for ~1s; accumulates total distance and computes the per-batch average that feeds the formula |
| `ConfigLoader.metersPerEmerald` | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` (line 44) | The rate. Public static int, default `50`, loaded from `[economy] metersPerEmerald` in `businesscraft.toml` |

## Rules & formulas
The formula (VisitorProcessingHelper.java line ~321):

```java
int payment = (int) Math.max(1, (averageDistance / ConfigLoader.metersPerEmerald) * record.getCount());
```

- `averageDistance` — mean distance in blocks (= meters) across all tourists in the batch from one origin town. Per-tourist distance is the **real traveled path** from `TouristEntity.getTotalDistanceTraveled()`, not town-to-town displacement (fallback to Euclidean only for non-TouristEntity villagers).
- `record.getCount()` — number of tourists in the batch.
- **Order of operations matters**: `Math.max(1, x)` is applied BEFORE the `(int)` cast. So the result is `floor(max(1, distance/rate × count))` for positive values.
- **Minimum payment**: any batch with `averageDistance > 0` pays at least 1 emerald **total** (not per tourist). E.g. 10 blocks at rate 50 → 0.2 → pays 1.
- **Truncation**: fractional emeralds are dropped. 99 blocks, 1 tourist, rate 50 → 1.98 → pays 1.
- **Zero/no distance**: if `averageDistance <= 0` the method returns 0 (no payment, logs a warning) — the `max(1, ...)` floor does NOT apply.
- **Worked example**: 2 tourists from Town A, traveled 400 and 600 blocks → average 500 → (500 / 50) × 2 = **20 emeralds**.
- After a successful payment, the batch's saved distance is cleared (`VisitBuffer.clearSavedDistance`) so it can't be double-paid.

### Batching context
- Tourists arriving within ~1 second of each other from the same origin town are grouped into one `VisitHistoryRecord` (see `VisitBuffer.shouldProcess`, 1000ms timeout).
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
- Not covered: the distance *measurement* itself (`TouristEntity.getTotalDistanceTraveled()` — needs MC entity bootstrap), reward board delivery, notifications.
- Note: `calculatePayment` is private, so tests invoke it via reflection. Pure logic otherwise.

## Open questions
- **count=0 quirk**: a record with 0 tourists and positive distance would pay 1 emerald due to `max(1, ...)`. Unreachable today, but if record construction ever changes, this becomes a free-emerald bug. A guard or extracting the formula into a public pure static method would harden it.
- The minimum payment is 1 emerald per *batch*, not per tourist — likely intentional, but worth confirming it matches design intent for very short routes with many tourists (e.g. 30 tourists × 5 blocks still pays just `max(1, (5/50)×30) = 3` emeralds... actually pays 3; whereas 30 tourists × 1 block pays `max(1, 0.6)` = 1 total).
- `calculatePayment` being private forces reflection in tests; consider extracting the formula to a public static pure method (post-v1 refactor, see `tasks/toImprove.md` candidate).

## Related
- [[Economy/Milestones/Distance Milestone Resolution]] (T-002 — consumes the same average distance)
- [[Town/Visits/Visit Buffer]] (T-010 — full VisitBuffer behavior)
- [[Town/Payment Board/Reward Claims]] (T-012 — where the payment lands)
