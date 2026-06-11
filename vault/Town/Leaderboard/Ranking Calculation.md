---
tags:
  - detail
  - town
---
# Ranking Calculation

**Breadcrumb**: Town > Leaderboard > Ranking Calculation
**TL;DR**: Client receives an unsorted list of all towns (pop, money=emeralds, happiness 0-100, tourism=cumulative arrivals); the TownLeaderboardScreen sorts by one of 6 modes (default closest distance) with numeric metrics shown highest-first and dynamic columns that hide all-zero metrics and adapt to panel width, prioritizing the active sort column when only 3 columns fit.

## What it does
The in-game Town Leaderboard (opened from the Town Interface) lets players see every registered town on the server at a glance, compare them on key metrics, and quickly navigate to details for any town. It is the primary discovery and "prestige" surface for the tourism economy — a town with high tourism count or population stands out when players cycle the sort. All computation for display order and visible stats happens client-side after the server sends a flat snapshot; there is no server-side ranking or persistent leaderboard state.

## How it works (process view)
1. Player clicks "Leaderboard" (or equivalent) in a Town Interface.
2. Client sends `LeaderboardDataRequestPacket` carrying the current town's name (for gold highlighting the row).
3. Server (in the packet handler) iterates `TownManager.getAllTowns().values()` (arbitrary HashMap order), builds a `TownLeaderboardData` record for each using live values:
   - population, emerald count (as "money"), `getHappiness()`, `getTotalTouristsArrived()` (the tourism metric).
4. The list (still unsorted) is sent back in `LeaderboardDataResponsePacket`.
5. Client constructs a `TownLeaderboardScreen` with the player's current `BlockPos` (for distance calculations) and the reference town name, then calls `setTownData(...)` which immediately sorts using the default `SortMode.DISTANCE`.
6. The screen renders a scrollable grid. A "Sort" button cycles through NAME → DISTANCE → POPULATION → HAPPINESS → TOURISM → MONEY (and wraps). Each click re-sorts and rebuilds the grid.
7. Columns are dynamic: Name and Distance are always shown; Population/Happiness/Tourism/Money only appear if at least one town has a positive value for that metric. When the panel only allows 3 columns total, the currently-sorted optional column is preferred.
8. Clicking any data row opens a `TownDetailScreen` for that town (still using the same DTO), which re-uses `TownLeaderboardData.distanceTo` + `formatDistance` for the "Distance" line.

**Worked example** (numbers chosen for easy arithmetic):
- Player is standing at Town "Alpha" located at (0, 64, 0).
- Three other towns exist:
  - "Beta" at (300, 64, 0), pop=42, money=120, happiness=67.5, tourism=19 (19 arrivals recorded since founding)
  - "Gamma" at (1200, 64, 0), pop=105, money=0, happiness=81.0, tourism=47
  - "Delta" at (800, 64, 0), pop=7, money=5, happiness=50.0, tourism=0
- Default open (DISTANCE sort, ascending): Beta (300m), Delta (800m), Gamma (1.2km). Delta's tourism=0 so if panel is narrow the Tourism column may be suppressed or show "-".
- Player clicks Sort once → NAME (A→Z): Alpha (gold), Beta, Delta, Gamma.
- Clicks again → POPULATION (high→low): Gamma 105, Beta 42, Alpha (whatever), Delta 7.
- The "Tourism" column only renders the number when >0; otherwise "-". Money appends " ✰".

All values are a point-in-time snapshot from when the request was handled; they do not live-update while the screen is open.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownLeaderboardData` (record) | `common/src/main/java/com/quackers29/businesscraft/town/data/TownLeaderboardData.java` | Immutable DTO carrying the 7 fields shown in the grid (id, name, position, population, money, happiness, tourism). |
| `distanceTo(BlockPos)` | same | Instance: returns Euclidean distance in blocks via `Math.sqrt(position.distSqr(otherPos))`. |
| `formatDistance(double)` | same | Static: `<1000` → `"%dm"` with `(int)` truncation; `>=1000` → `"%.1fkm"`. |
| `LeaderboardDataRequestPacket.handle(...)` | `.../network/packets/ui/LeaderboardDataRequestPacket.java` | Server: builds the unsorted list from `TownManager.getAllTowns()`, reads live `getTotalTouristsArrived()`, emerald count via `getResourceCount(Items.EMERALD)`, etc. No sort. |
| `LeaderboardDataResponsePacket` (ctor + encode/decode) | `.../network/packets/ui/LeaderboardDataResponsePacket.java` | Wire format: manual FriendlyByteBuf roundtrip for the list of records + currentTownName. |
| `TownLeaderboardScreen` | `.../ui/screens/town/TownLeaderboardScreen.java` | All ranking + presentation logic. Holds `SortMode`, performs `townData.sort(getComparatorForMode(...))`, `hasAnyNonZeroValues`, `calculateVisibleColumns`, grid build, current-town gold highlight. |
| `getComparatorForMode(SortMode)` | same (lines ~91-100) | Returns the active Comparator. DISTANCE is the only ascending sort; the four numeric metrics are `.reversed()`. NAME is plain string compare. |
| `hasAnyNonZeroValues(...)` + `calculateVisibleColumns()` | same (~105-212) | Decide which of the 4 optional columns are eligible and how many actually fit given `(panelWidth-30)/100` min 100 px per col, with special 3-col "prioritize sorted" rule. |
| `Town.recordVisit(...)` / `getTotalTouristsArrived()` | `.../town/Town.java` (~1016, ~914) | Source of the tourism number: `totalTouristsArrived += count` on every processed arrival batch. Also maintains a separate weighted `totalTouristDistance`. |
| `Town.getHappiness()` | same (~133) | Returns base + upgrade modifier, clamped to `[0.0f, 100.0f]`. |
| `TownDetailScreen` | `.../ui/screens/town/TownDetailScreen.java` | Consumer of a single `TownLeaderboardData` for the detail view; re-uses the distance/format helpers. |

## Rules & formulas (exact)
- **Distance formula** (TownLeaderboardData:25):
  ```java
  return Math.sqrt(position.distSqr(otherPos));
  ```
  `distSqr` returns a `long` (exact for BlockPos integer coords). `sqrt` yields a `double`. No clamping, caching, or special casing for `BlockPos.ZERO` or null (null → NPE).

- **Display formatting** (TownLeaderboardData:35):
  ```java
  if (distance < 1000) {
      return String.format("%dm", (int) distance);
  } else {
      return String.format("%.1fkm", distance / 1000.0);
  }
  ```
  - Threshold is hard 1000 (blocks).
  - Under threshold: truncated toward zero via `(int)`, then "m". 999.9 → "999m".
  - At/above: division by 1000.0 (double) + one decimal place via `%.1f`. 1000.0 → "1.0km", 1499.0 → "1.5km", 12345 → "12.3km".
  - The method is pure and public static; callers (screen column extractor, TownDetailScreen) always pass a non-negative value from `distanceTo`.

- **Sort rules** (TownLeaderboardScreen:91):
  ```java
  case NAME -> Comparator.comparing(TownLeaderboardData::name);
  case DISTANCE -> Comparator.comparingDouble(data -> data.distanceTo(currentTownPosition));
  case POPULATION -> Comparator.comparingLong(TownLeaderboardData::population).reversed();
  case HAPPINESS -> Comparator.comparingDouble(TownLeaderboardData::happiness).reversed();
  case TOURISM -> Comparator.comparingLong(TownLeaderboardData::tourism).reversed();
  case MONEY -> Comparator.comparingLong(TownLeaderboardData::money).reversed();
  ```
  - Single-key only; no `thenComparing` secondary key.
  - `sortData()` is called on set and on every mode cycle; it mutates the screen's `townData` list in place.
  - Default on open: `DISTANCE` (ascending = closest towns first).

- **Tourism metric**: the `tourism` field is exactly `Town.getTotalTouristsArrived()` — the sum of the `count` parameter over every `recordVisit` call the town has ever processed. It is a lifetime cumulative arrival counter (batches, not unique tourists or origins). It is persisted in the town's NBT as `totalTouristsArrived`.

- **Column visibility & layout math** (simplified):
  - Always emit Name + Distance.
  - Optional columns (in fixed declaration order): Population, Happiness, Tourism, Money — only those for which `hasAnyNonZeroValues` returns true (strict > 0 after `doubleValue()`).
  - `maxColumns = Math.max(2, (panelWidth - 30) / 100)` (integer division, min 100 px/col).
  - When `maxColumns == 3` and ≥2 optionals: if one of the optionals has `isSorted`, prefer it; else take the first available.
  - Otherwise take up to `maxColumns-2` from the head of the available list.
  - Sorted column header gets " ▼" suffix + highlight color.

- **Current town highlight**: row name is drawn in `0xFFFFAA00` (gold) when `town.name().equals(currentTownName)` (exact string match, case sensitive). Only affects column 0.

- Wire: all longs/floats are sent verbatim; no scaling or sanitization in the packets beyond the normal FriendlyByteBuf methods. The client DTO list is a fresh `ArrayList` copy in `setTownData`.

## Edge cases & behaviors
- Zero distance (player at the town's own position or a town at identical BlockPos): `distanceTo` returns 0.0, format produces "0m". In DISTANCE sort, such a town appears first.
- Exactly 1000 blocks: "1.0km".
- Just under 1000 (999.999...): "999m" due to `(int)` cast inside the branch.
- Fractional km rounding: standard `%.1f` (1.234 → 1.2, 1.25 → 1.3? — floating point and format rules apply).
- Negative distance passed to `formatDistance` (possible only via direct call; `distanceTo` never produces <0): because the guard is the signed comparison `distance < 1000` (not `abs`), **every negative value** takes the integer-meter branch, producing strings such as "-42m" or "-1500m". The `%.1fkm` branch is unreachable for any negative input. Current behavior pinned by test.
- `distanceTo(null)`: NullPointerException (BlockPos.distSqr on null). No defensive check in the DTO (contrast with Town.recordVisit which guards against ZERO but not null).
- Empty town list from server: screen shows a single centered "No towns to display" label.
- All optional metrics zero: only Name + Distance columns are rendered regardless of width.
- Ties on primary sort key: order is stable relative to the list received from the server (which is `Map.values()` iteration order over a HashMap — not guaranteed, not insertion order, can vary between requests or after GC). No secondary sort (e.g. by name) is applied.
- Happiness values: source is already clamped [0,100]; display uses `>= 0 ? "%.0f%%" : "-"` (the >=0 is largely defensive).
- Money display: when >0 appends literal " ✰" (star); zero/negative shows "-".
- The SortMode enum cycles with `next()` using ordinal wrap: 6 modes.
- Panel width/height are computed each init from screen dimensions (`width * 0.8`, `height * 0.7 - 40`); column decisions are re-evaluated on every sort cycle because `init()` is called again.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/TownLeaderboardDataTest.java`
- Covered (pure DTO surface only, 15 tests): record construction/accessors; `distanceTo` (same position → 0.0, exact integer results like 3.0/10.0 from 6-8-10, sqrt(2) double case, 1200-block case); `formatDistance` (0/500/999.9 under-threshold with int truncation, exactly 1000 and 1234/10000/123456 producing *.?km, plus two pinning tests for negative inputs always hitting the signed `<1000` → meters branch).
- Not covered / intentionally out of scope for this iteration: the full ranking + column selection logic (lives in `TownLeaderboardScreen`, tightly coupled to Minecraft Screen/GuiGraphics/UIGridBuilder and live panel dimensions — requires client bootstrap), server list assembly (needs TownManager + Item.EMERALD + full Town), packet encode/decode roundtrips (network), UI interaction and gold highlighting, SortMode cycling.
- Note: All tests are pure logic. BlockPos is used (explicitly permitted per protocol). No production code was modified. The richer "sort orders, tie-breaking" described in the ledger row lives outside this DTO in client UI code.

## Open questions
- **Unstable tie order**: because the server iterates an unordered map and the client comparators have no secondary key, towns with identical population (or identical tourism, etc.) can appear in different relative order on different opens of the leaderboard. This is harmless for "best first" browsing but surprising if a player expects a stable secondary sort (e.g. by name). The current behavior is pinned by the lack of `thenComparing` in the comparators.
- **formatDistance on negative**: because the `< 1000` test is signed, every negative input (small or large magnitude) produces a negative-meter string via the `(int)` cast path, e.g. "-42m" or even "-1500m". The km formatting branch is unreachable for negatives. Pinned by test `formatDistance_largeMagnitudeNegative_stillUsesMetersBranch`. If a future caller wants "human friendly absolute distance", an abs-based helper would be a clearer addition. Current (quirky but consistent) behavior left as-is.
- **distanceTo(null)** throws NPE with no caller-friendly message. All current call sites pass valid positions, but a null guard + 0.0 or Double.NaN would make the helper more robust.
- The "tourism" value on the leaderboard is a raw lifetime arrival count. It does not decay, weight recent activity, or distinguish "unique visitors" vs. repeat tourists on short loops. Whether this is the intended prestige signal is outside the code.
- Column priority when max=3: the code walks the available list to find the `isSorted` one rather than using the current sortMode directly; this works because `isSorted` is set from the mode at column-build time. A future change to how `isSorted` is computed could affect the 3-col rule.

## Related
- [[Town/Town Overview]]
- [[Town/Visits/Visit Buffer]] (T-010 — the source of the cumulative tourism counts via recordVisit batches)
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — related but separate use of real traveled distance)
- [[Town/Boundaries/Town Distance Validation]] (T-008 — other distance math in the Town system)
