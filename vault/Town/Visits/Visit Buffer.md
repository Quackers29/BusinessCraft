---
tags:
  - detail
  - town
---
# Visit Buffer

**Breadcrumb**: Town > Visits > Visit Buffer
**TL;DR**: VisitBuffer coalesces arrivals from the same origin town over a shared ~1s quiet-period window, accumulating counts and traveled distances into per-origin aggregates; on flush it emits VisitHistoryRecords, snapshots positive average distances into a persistent distanceMap (so post-flush payment can still read them), and clears only the live buffer.

## What it does
Tourists often arrive in small groups within a second or two. Without batching, every single tourist would trigger its own visit record, notification, and reward entry. The Visit Buffer groups arrivals that share an origin town and happen close together in time, producing one batched record per origin with the total headcount and the average real distance traveled by that group. This record feeds the destination town's visit history, population stats, distance-based emerald payment, and milestone rewards.

## How it works (process view)
- Detection (in VisitorProcessingHelper, called periodically from TownInterfaceEntity) finds stationary TouristEntities (or fallback villagers) on enabled platforms. For each qualifying arrival it calls:
  1. `visitBuffer.addVisitor(originTownId, originPos)`
  2. `visitBuffer.updateVisitorDistance(originTownId, realTraveledDistance)`
- If the same origin appears again while the buffer is "live", `addVisitor` increments the count for that UUID and re-uses the originPos from the first arrival in the current window; distances are summed.
- A single `lastVisitTime` (updated on every add, any origin) tracks the most recent arrival across the whole buffer.
- `shouldProcess()` returns true only when the buffer has entries AND more than 1000 ms have elapsed since the last add (global quiet period).
- When the caller sees shouldProcess true, it calls `processVisits()`:
  - For every origin that has a positive average distance, the avg is copied into the private `distanceMap`.
  - One `VisitHistoryRecord` (flush timestamp, origin UUID, aggregated count, originPos) is created per origin present.
  - The live `visitors` map is cleared; `distanceMap` is left intact.
  - The list of records is returned.
- The caller (processVisitBuffer) then, for each record:
  - Calls `provider.recordVisit(...)` (Town stores it in its capped history and accumulates totals — using Euclidean for its own stats).
  - Calls `visitBuffer.getAverageDistance(origin)` — this now falls back to the just-saved value in distanceMap.
  - Passes the record + distance to `calculatePayment(...)` (T-001) and `DistanceMilestoneHelper`.
  - After using the distance for payment, calls `clearSavedDistance(origin)` so it cannot be paid twice.
- Config: the 1000 ms timeout is a hard-coded constant (`BUFFER_TIMEOUT_MS`).

**Worked example** (times are illustrative wall-clock ms):
- t=10 000: Tourist A1 from TownX (pos Px) detected, traveled 480 blocks. addVisitor(X, Px), updateDistance(480). lastVisitTime=10000. visitors: X→count1/dist480/avg480
- t=10 180: Tourist A2 from TownX, traveled 520. addVisitor → count=2, totalDist=1000, avg=500. lastVisitTime=10180.
- t=10 300: Tourist B1 from TownY (Py), traveled 310. addVisitor(Y, Py), update(310). last=10300. Now two origins alive.
- t=10 300 + 1100 = 11400: No new adds for >1 s since last arrival (any town). shouldProcess() == true.
- processVisits(): stores X:500 and Y:310 into distanceMap; emits two records with timestamp≈11400 (X,2,Px) and (Y,1,Py); clears visitors.
- Caller for X: getAverageDistance(X)→500 (from distanceMap), calculatePayment yields floor(max(1,500/50*2))=20 emeralds, then clearSavedDistance(X).
- Same for Y. Both records also written to Town visitHistory (newest first).

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `VisitBuffer.addVisitor(UUID, BlockPos)` | `common/src/main/java/com/quackers29/businesscraft/town/data/VisitBuffer.java` | Insert or increment count for origin; capture originPos on first insert in window; bump the shared lastVisitTime |
| `VisitBuffer.updateVisitorDistance(UUID, double)` | same | Add traveled distance to the running total for that origin (only if still in the live visitors map) |
| `VisitBuffer.getAverageDistance(UUID)` | same | Prefer live visitors map avg if >0; else fall back to distanceMap; 0 if neither |
| `VisitBuffer.getVisitorCount()` | same | Number of distinct origins currently in the live buffer |
| `VisitBuffer.shouldProcess()` | same | `!visitors.isEmpty() && (now - lastVisitTime > 1000)` — global quiet-period gate |
| `VisitBuffer.processVisits()` | same | Snapshot positive avgs → distanceMap, build VisitHistoryRecord list (flush-time ts), clear visitors only, return records |
| `VisitBuffer.clearSavedDistance(UUID)` | same | Remove the avg from distanceMap (called after payment/milestones consume it) |
| `VisitBuffer.VisitorInfo` (private static) | same | Per-origin accumulator: count, originPos (frozen), totalDistance; `incrementCount()`, `addDistance(d)`, `getAverageDistance()` = (count>0 ? total/count : 0) |
| `ITownDataProvider.VisitHistoryRecord` | `common/src/main/java/com/quackers29/businesscraft/api/ITownDataProvider.java` | Immutable value passed out of flush and into Town.recordVisit |

## Rules & formulas (exact)
From VisitBuffer.java:

- `BUFFER_TIMEOUT_MS = 1000` (hard constant, not configurable).
- `addVisitor`:
  ```java
  visitors.compute(townId, (id, info) -> info == null ? new VisitorInfo(1, originPos) : info.incrementCount());
  lastVisitTime = System.currentTimeMillis();
  ```
  - originPos for a given townId is taken from the first add in the current buffer cycle and never overwritten by later adds for the same id.
- `updateVisitorDistance`:
  ```java
  visitors.computeIfPresent(townId, (id, info) -> info.addDistance(distance));
  ```
  - No-op if the townId is not currently in the live visitors map (e.g. after processVisits or for a town never added this cycle).
- `VisitorInfo.getAverageDistance()`:
  ```java
  return count > 0 ? totalDistance / count : 0;
  ```
- `getAverageDistance(UUID)` (two-tier lookup):
  ```java
  VisitorInfo info = visitors.get(townId);
  if (info != null && info.getAverageDistance() > 0) return info.getAverageDistance();
  return distanceMap.getOrDefault(townId, 0.0);
  ```
- `shouldProcess()`:
  ```java
  return !visitors.isEmpty() &&
         System.currentTimeMillis() - lastVisitTime > BUFFER_TIMEOUT_MS;
  ```
- `processVisits()` (the flush):
  - If empty → return Collections.emptyList() immediately (no timestamp side-effect).
  - Before clearing: `visitors.forEach((id, info) -> { double avg = info.getAverageDistance(); if (avg > 0) distanceMap.put(id, avg); })`
  - Then build records:
    ```java
    long now = System.currentTimeMillis();
    records = visitors.entrySet().stream()
      .map(e -> new VisitHistoryRecord(now, e.getKey(), e.getValue().count, e.getValue().originPos))
      .collect(...)
    ```
  - `visitors.clear();` (distanceMap untouched)
  - Return the list.
- `clearSavedDistance(UUID)`: `distanceMap.remove(townId)` (the return value is only used for debug logging).
- All timestamps written into VisitHistoryRecord by the buffer are the flush instant (one shared "now" for the whole batch). Individual arrival times are not retained.
- The live `visitors` map and the `distanceMap` are completely independent after a flush; an update after clear affects only a future buffer cycle.

## Edge cases & behaviors
- Empty buffer: `processVisits()` → empty list; `shouldProcess()` → false; `getVisitorCount()` → 0; `getAverageDistance(any)` → 0.
- First arrival for an origin: count=1, totalDistance=0 until updateVisitorDistance is called; avg will be 0 until distance is supplied.
- Multiple arrivals, same origin, <1 s apart: single aggregated record on flush (dedup + sum count + sum distance).
- Arrivals from N distinct origins inside the same quiet window: N records emitted on the next flush; they share the same flush timestamp and the global lastVisitTime.
- Update distance for a townId not in the current live visitors: silently ignored (computeIfPresent).
- getAverageDistance while live entry exists with avg==0: returns 0 (the >0 guard), even if distanceMap has an old value for the same id.
- getAverageDistance after flush (visitors cleared) but before clearSavedDistance: returns the value from distanceMap.
- processVisits stores an avg into distanceMap only when `avg > 0`.
- After `clearSavedDistance(id)`, subsequent getAverageDistance(id) returns 0 (until a new buffer cycle writes it again).
- Global timer: any addVisitor (even from a different town) resets the 1 s countdown for everyone. A continuous trickle from TownY can indefinitely delay flushing a pending batch from TownX.
- Timestamp in emitted records is wall time at flush, not per-tourist arrival time.
- originPos captured at first add for the batch; if the "origin town" block somehow moved between first and second tourist in a 1 s window, the record still carries the first-seen pos (in practice origin town positions are stable).
- count in a produced record is always ≥ 1 when emitted from normal addVisitor flow (VisitorInfo never starts at 0).
- lastVisitTime starts at 0; first add sets it to a real ms value. shouldProcess can only become true after at least one add + 1000 ms quiet.
- toString and debug logs are present but have no effect on logic.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/VisitBufferTest.java`
- Covered: add/accumulate (single + multi same-origin), dedup across origins, distance sum + avg calc, getAverageDistance two-tier (live vs distanceMap), getVisitorCount, shouldProcess (immediate false + timeout true via sleep), processVisits on empty and non-empty (records, distance snapshot only for >0, visitors cleared), clearSavedDistance, update on absent town ignored, post-process get/clear behavior, global timer effect via mixed-origin timing.
- Not covered (by design): actual wall-clock scheduling in the entity tick loop, interaction with TouristEntity distance measurement (needs MC), downstream recordVisit side-effects on Town history/stats (those are in other classes), debug log content.
- Note: one test uses a short Thread.sleep to exercise the real 1000 ms timeout path; it is the only non-instant test.

## Open questions
- The global shared lastVisitTime + quiet-period design means cross-origin arrival traffic can suppress flushes for other pending origins. Is this the intended "coalesce everything when busy" behavior, or would per-origin timers be more precise? Current behavior is pinned by tests.
- No per-arrival timestamps are kept — only the flush instant. If later features want "time of first/last tourist in batch", that data is lost at process time.
- VisitorInfo is a private static class with mutable totalDistance via addDistance returning this for chaining. The mutation is contained inside the buffer's maps; external code never sees the mutable object.
- The buffer never prunes old entries from distanceMap except via explicit clearSavedDistance per town after payment. In normal play this is fine (clear happens in the same processVisitBuffer call), but a crash between processVisits and the clear would leave a stale distance that a future unrelated batch for the same origin could accidentally read. (Unlikely in practice because processing is synchronous.)
- BUFFER_TIMEOUT_MS is private static final; tests observe its effect rather than reading the constant. If the value ever changes, the "after timeout" test expectation would need updating (or the constant made package-visible for tests).

## Related
- [[Town/Town Overview]]
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — consumes the average distance captured here)
- [[Economy/Milestones/Distance Milestone Resolution]] (T-002 — also consumes the pre-clear avg)
- [[Town/Visits/Visit Buffer]] (self)
- [[Town/Payment Board/Reward Claims]] (T-012 — where the resulting bundled reward lands)
