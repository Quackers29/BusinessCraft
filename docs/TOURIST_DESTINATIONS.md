# Tourist destination modes (Any / Auto / Specific)

## Plan (implementation checklist)

1. Add `DestinationMode` on each `Platform` (saved in world NBT) plus `PlatformLearnedRoutes` (per-platform rolling scores).
2. Extend `businesscraft.toml` and `ConfigLoader` with defaults and AUTO tuning; validate bad values with warnings.
3. Change `TouristSpawningHelper` to branch on mode; AUTO uses learning + exploration roll.
4. On arrival, `VisitorProcessingHelper` credits the origin platform (from existing tourist `platform_` tag) when payment would be non-zero.
5. On tourist expiry/despawn without arrival, penalize the learned route when AUTO spawned a concrete destination.
6. When a town is deleted, drop that town id from every platform’s allow-list and learned tables.
7. Packets + Platform Interface UI (mode picker, read-only learned list, forget button); Town Centre list shows mode.
8. Unit tests for learning math, save/load, spawn selection, config validation; extend showcase if cheap.

## Why three modes

| Mode | Player intent | Spawn behaviour |
|------|----------------|-----------------|
| **Any** | “Send tourists with no fixed ticket; first valid arrival pays.” | Always `Any Town` (UUID 0-0), same as legacy empty allow-list. |
| **Auto** | “Learn which towns this platform can actually reach by rail.” | Mix of learned destinations (weighted by score) and `Any Town` exploration. |
| **Specific** | “Only these towns.” | Population-fair pick from enabled allow-list (legacy non-empty list). |

Selecting a town on the interface switches to **Specific**. The allow-list is kept when switching away so players can return without re-picking.

## Why multi-stop lines and loops under-learned (root cause)

Arrival detection runs when a tourist is **stationary inside a town’s platform search capsule** (same path used for boarding). For an **Any Town** ticket, the handler treats **every non-origin stop as a valid alight point** and completes the visit immediately.

On a line **A → B → C** or a loop **A → B → C → D → A**, that means:

1. **First-stop wins** — Almost every open-ticket trip never reaches C or D; platform A only records arrivals (and full learning credit) at **B**. This is the primary bug, not decay or pruning.
2. **Exploration share** — AUTO still spawns ~30% Any Town even when B is well learned; those trips also alight at **B** first, so they do not discover farther towns.
3. **Learned spawn bias** — Once B has score, up to `autoMaxLearnedShare` of spawns target B explicitly; those tourists carry a **fixed** destination id and never visit C/D unless the line is wired that way.
4. **Score-only weights** — Before distance bias, learned picks ignored how far a town is; nearer learned towns stayed overweight even when farther routes were partially known.
5. **Secondary effects** — `autoMinScoreForLearning`, pruning, expiry penalties, and population-fair **Specific** mode do not cause the loop gap; they can slow or cap tables but do not replace the first-stop alight rule.

**Fix (current behaviour):** configurable **ride-on** at intermediate stops (`autoRideOn*`), partial **pass reach** credit (`autoPassReachWeight`), and **distance-biased** learned spawn weights (`autoDistanceBias`, `autoRandomness`). Fares and payment distance are unchanged.

### Stop detection note

There is no separate “train passed platform without stopping” hook. Stops are inferred the same way as arrivals: tourist in the platform AABB and not moving between ticks (and minecart boarding uses the existing stopped-vehicle threshold). Ride-on is decided at that moment: either process a full arrival or mark the stop as passed and leave the entity on the vehicle.

AUTO tourists spawned with a **concrete learned destination** still only alight when their ticket town matches; they naturally stay on at earlier stops without ride-on rolls.

## AUTO learning (plain language)

Each platform keeps a small table: destination town → **score** and a rolling **average trip duration** (in-game ticks). Scores fade when the world clock advances. Decay is exponential and uses **game ticks**, not wall clock, so nothing decays while the server is off.

**Per-route fade (trip-aware):** Each `(platform → town)` entry remembers how long trips usually take (exponential moving average, updated on every paid arrival). That entry’s score half-life is:

`max(autoScoreHalfLife, averageTripTicks × autoFadeTripMultiplier)`

So a route whose tourists typically take two in-game hours fades much more slowly than one that takes five minutes, even when the base half-life is the same. Trip time comes from the tourist’s saved **spawn world time** (`SpawnTime` in entity NBT) subtracted from arrival game time.

- **New platform / empty table:** AUTO behaves like Any until arrivals teach routes.
- **Successful arrival:** A tourist spawned from platform P at town A that finishes at town B (and would earn payment by distance rules) adds weight to P→B. Trip length is `arrivalGameTime − tourist.spawnTime` (both world ticks; `SpawnTime` in entity NBT). That updates a rolling average trip duration for P→B. Weight is at least 1, optionally boosted by travel distance (capped).
- **Ride-on pass:** If an Any-Town tourist skips a stop, P→that town gets a smaller `autoPassReachWeight` credit (no extra fare).
- **Exploration:** Each spawn rolls whether to use learning. The roll’s chance is `maxLearnedShare × confidence`, capped by config. Confidence rises as total learned score crosses `minScoreForLearning`. If the roll fails, spawn `Any Town` so broken or new routes stay discoverable.
- **Learned pick weights:** `score × (1 + autoDistanceBias × relativeDistance)` with optional jitter `autoRandomness` (does not change emeralds paid).
- **Broken route:** If a tourist despawns on expiry while still holding a learned destination, that route’s score is reduced (route probably broken).
- **Cleanup:** Scores below a floor are removed; only the top N towns by score are kept (N from config). Removed towns are purged everywhere.

Tourists already carry the origin **platform id** in entity tags (`platform_<uuid>`); arrival processing uses that to credit the correct platform.

## Defaults (see config comments)

New platforms use `tourists.defaultDestinationMode` (**auto**). AUTO tuning defaults favour stable routes without freezing out discovery: ~70% max learned share once confidence is full, 7-day in-game base half-life (`autoScoreHalfLifeMinutes` = 10080), `autoFadeTripMultiplier` = 2.0 (half-life at least 2× average trip time), prune floor 0.05, max 12 learned towns, minimum total score 2.0 before learning affects spawns, expiry penalty 1.0.

**Ride-on (Any Town at intermediate stops):** `autoRideOnBaseChance` 0.45, distance bonus scale 0.35 per reference block (`autoRideOnDistanceReferenceBlocks` 500), skip penalty 0.12 per town skipped, max stay chance 0.92, max skips 4, pass reach weight 0.35. Chosen so a typical first intermediate stop is near 50/50 stay/go, longer trips skew toward staying on, and tourists cannot skip indefinitely.

**Distance-biased learned spawns:** `autoDistanceBias` 0.75, `autoRandomness` 0.35 — nudge AUTO toward farther known towns without changing fares.

## Tests

- `TouristRouteDiscoverySimulatorTest` — documents first-stop failure on A→B→C and A→B→C→D loops vs ride-on.
- `TouristAlightDecisionTest`, `PlatformLearnedRoutesTest` (distance bias), existing learning/NBT tests.

## Files (main touch points)

- `platform/DestinationMode.java`, `platform/PlatformLearnedRoutes.java`, `platform/Platform.java`
- `town/data/TouristDestinationLearning.java`, `TouristAlightDecision.java`, `TouristRouteDiscoverySimulator.java`, `TouristSpawningHelper.java`, `VisitorProcessingHelper.java`
- `config/ConfigLoader.java`, `assets/businesscraft/config/businesscraft.toml`
- UI: `PlatformInterfaceScreen.java`, `PlatformManagementScreenV2.java`, destination packets
- Tests: `PlatformLearnedRoutesTest.java`, `TouristRouteDiscoverySimulatorTest.java`, updates to `PlatformTest`
