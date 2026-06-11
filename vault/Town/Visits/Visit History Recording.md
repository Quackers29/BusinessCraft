---
tags:
  - detail
  - town
---
# Visit History Recording

**Breadcrumb**: Town > Visits > Visit History Recording
**TL;DR**: recordVisit appends a VisitHistoryRecord (newest-first) to a town's capped list (max 50), always adds the batch count to totalTouristsArrived, and if a non-null non-ZERO origin BlockPos is supplied adds (euclidean distance between town center and that pos) × count to totalTouristDistance; both totals and the trimmed history list are persisted via NBT and exposed for UIs and stats.

## What it does
Every time a group of tourists from the same origin town arrives (after the Visit Buffer has coalesced them), the destination town records a single history entry. This keeps a rolling "who visited recently" log for the visitor history screen and accumulates two simple running totals: how many tourists have ever come here, and a rough sum of "how far away their starting towns were" (straight-line at arrival moment). The history is deliberately bounded so it never grows without limit; old entries silently drop off the back when the 50-entry cap is hit.

## How it works (process view)
- Visitor processing (after tourists are detected on platforms) calls provider.recordVisit(originTownId, aggregatedCount, originPos) for each distinct origin in a flushed batch.
- recordVisit always:
  - Takes a wall-clock timestamp (System.currentTimeMillis()).
  - Builds a VisitHistoryRecord and inserts it at the front of the internal list (newest first).
  - While the list is longer than MAX_HISTORY_SIZE (50), drops the last (oldest) entry.
  - Adds the count to totalTouristsArrived unconditionally.
  - Only if originPos is non-null AND not the constant BlockPos.ZERO: computes straight-line distance = sqrt(town.position.distSqr(originPos)), then adds (distance × count) to totalTouristDistance.
  - Calls markDirty() so the change saves.
- The list is returned to callers as an unmodifiable view (getVisitHistory).
- On save, only non-empty history is written (each record's timestamp, origin UUID, count, and optional pos).
- On load, records are reconstructed and appended in the order they appear in the tag (the list order from save time is preserved; newest-first convention is maintained by how they were inserted).
- The two totals are simple long/double fields, saved unconditionally when present, and readable by leaderboards, debug, or other stats consumers.

**Worked example**: Town "Dest" is at BlockPos(0, 64, 0).
- Batch arrives: 3 tourists from origin town whose platform pos was (120, 64, 40). recordVisit(U, 3, (120,64,40)).
  - dist = sqrt(120^2 + 0 + 40^2) = sqrt(14400 + 1600) = sqrt(16000) ≈ 126.49
  - totalTouristsArrived = 0 + 3 = 3
  - totalTouristDistance = 0 + 126.49 × 3 ≈ 379.47
  - history = [ Record(ts=..., U, 3, (120,64,40)) ]
- Second batch 5 minutes later: 2 tourists from V at (0, 64, 200).
  - dist = 200
  - totals become arrived=5 , distance≈379.47 + 400 = 779.47
  - history = [ Record for V (newest), Record for U ]
- If 46 more single-tourist records arrive from various places, the list hits size 48. One more record pushes it to 49. A 50th is still kept. The 51st causes the very first U record (now at the end) to be dropped. The list always contains at most the 50 most recent arrivals.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `Town.recordVisit(UUID, int, BlockPos)` | `common/src/main/java/com/quackers29/businesscraft/town/Town.java` (~1001-1025) | The entry point: creates record, newest-first insert + trim, accumulators, dirty |
| `Town.getVisitHistory()` | same | Returns Collections.unmodifiableList of the internal visitHistory |
| `Town.getTotalTouristsArrived()` / `getTotalTouristDistance()` | same (~914-920) | Simple accessors for the two running totals |
| `Town.save` / `load` (visitHistory + totals) | same (~509-510, 648-654, 557-582, 742-748) | NBT roundtrip for the list (only non-empty written) and the two totals; load reconstructs VisitHistoryRecord objects directly |
| `ITownDataProvider.VisitHistoryRecord` | `common/src/main/java/com/quackers29/businesscraft/api/ITownDataProvider.java` (~74-102) | Simple immutable value object: timestamp (ms), originTownId, count, originPos (nullable) |
| `VisitBuffer.processVisits()` | `common/src/main/java/com/quackers29/businesscraft/town/data/VisitBuffer.java` | The main caller that produces the aggregated records passed to recordVisit |

## Rules & formulas (exact)
From Town.java:

```java
public void recordVisit(UUID originTownId, int count, BlockPos originPos) {
    long timestamp = System.currentTimeMillis();
    VisitHistoryRecord record = new VisitHistoryRecord(timestamp, originTownId, count, originPos);
    visitHistory.add(0, record);                    // newest first
    while (visitHistory.size() > MAX_HISTORY_SIZE) { // 50
        visitHistory.remove(visitHistory.size() - 1);
    }
    this.totalTouristsArrived += count;
    if (originPos != null && originPos != BlockPos.ZERO) {
        double distance = Math.sqrt(this.position.distSqr(originPos));
        this.totalTouristDistance += distance * count;
    }
    markDirty();
}
```

- Timestamp is wall-clock milliseconds (System.currentTimeMillis()), not level game time.
- Insert is always at front; removal is always from the tail when over limit.
- Accumulation of totalTouristsArrived is unconditional (`+= count`); negative or zero counts are accepted by the arithmetic.
- Distance accumulation only for "valid" origin positions: the guard is reference inequality against the constant `BlockPos.ZERO` (plus null check). A newly constructed BlockPos(0,0,0) would be a different object and would pass the guard.
- `distSqr` returns a long (squared Euclidean in block units); the sqrt produces a double; the multiplication by count and the += are ordinary double arithmetic (no rounding or scaling applied here).
- getVisitHistory always wraps with unmodifiableList; callers see a snapshot of current order but cannot mutate it.
- MAX_HISTORY_SIZE is a private static final int 50 in Town (hard-coded, not configurable).

## Edge cases & behaviors
- count == 0: a record is still created and inserted (and may cause a trim), totalTouristsArrived unchanged, distance skipped or added as 0.
- count < 0: totalTouristsArrived decreases (can become negative); a history record is still created with the negative count.
- originPos == null: record created, totals count added, distance contribution skipped.
- originPos == BlockPos.ZERO (the constant): same as null for distance (guard fails).
- A constructed BlockPos(0,0,0) (different instance): the `!= BlockPos.ZERO` is true, so distance would be computed (0.0) and added.
- History trim only triggers on insert when size would exceed 50 after the add(0,...); exactly 50 is kept.
- Adding the 51st drops exactly one (the oldest); adding many at once drops as many as needed from the tail.
- Empty history on save is omitted from the tag entirely; on load, absence means the list stays empty (the field initializer).
- The two totals are written on every save (even if 0); they survive round-trips.
- Load path for history (the loop that builds records from the "visitHistory" ListTag) appends in file order, so if the saved order was newest-first it stays that way.
- No validation on originTownId (can be null); records with null IDs are still stored and returned.
- markDirty is called even for no-op-ish cases (e.g. count=0 with null pos).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/TownTest.java` (extended for T-039)
- Covered (all with hand-computed expectations): happy path insert + order + totals with realistic distance (50 m exact), count=0 (record created, totals not increased), negative count (accepted, totals decrease), null originPos (skips distance), BlockPos.ZERO (skips distance), multiple inserts, trim behavior when exceeding 50 (oldest dropped), getVisitHistory returns unmodifiable view, direct construction of VisitHistoryRecord for verification.
- Not covered here: full NBT save/load roundtrip of VisitHistoryRecord list (covered in other integration paths and VisitBufferTest patterns); UI consumption of the history; callers that pass live entity positions.

## Open questions
- The distance accumulated here (`totalTouristDistance`) is always the straight-line Euclidean between the destination town's fixed position and the *origin platform/visitor pos at arrival time*. This is different from (and not used by) the real-path traveled distance that TouristEntity tracks and that VisitorProcessingHelper / payments / milestones actually consume. Two distinct "tourist distance" concepts exist in the system; the one here appears to be legacy / stat / leaderboard flavor only.
- count can be 0 or negative with no guard in recordVisit; a zero-count record still occupies a history slot and can push older entries out. (Pinned by test as current behavior.)
- Timestamp uses real wall time; if the server's clock jumps or two arrivals are processed in the same millisecond the ordering within that ms is insertion order, not guaranteed by time value.
- The guard `originPos != BlockPos.ZERO` is an identity check against the constant. Any equivalent (0,0,0) instance created by deserialization or math would be treated as "valid" and contribute a 0.0 distance term.
- MAX_HISTORY_SIZE=50 is not exposed or configurable; changing it would be a data migration concern for existing worlds.
- No test of the exact save/load format for visitHistory here (the detail note for Visit Buffer and ClientSyncHelper touch related paths).

## Related
- [[Town/Visits/Visit Buffer]] (T-010 — the producer of the aggregated records that get passed here)
- [[Town/Leaderboard/Ranking Calculation]] (T-011 — may surface total tourism numbers)
- [[Town/Payment Board/Reward Claims]] (T-012 — payments are issued in the same processing pass that eventually calls recordVisit)
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — uses the *real* traveled distance, not the euclidean accumulated here)
- [[Tourists/Lifecycle/Tourist Distance Tracking, Ride Extension and Expiry]] (T-038 — source of the real-path value used for actual economy)
