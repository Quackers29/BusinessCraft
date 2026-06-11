---
tags:
  - overview
  - town
---
# Town Overview

**TL;DR**: A Town is the central persistent entity holding identity, population, multi-tier resource storage (on-hand via TownResources + escrow + personal + payment board), production/trading/contracts state, visit history, and boundary logic; tourism drives population growth which in turn unlocks capacity and upgrades.

## Processes in this area
- **[[Town/Resources/Resource Storage Operations|Resource Storage Operations]]** (T-007) — `TownResources` (delegated from `TownEconomyComponent`) is the core long-count Item bag: `addResource` with `Math.addExact` overflow → `Long.MAX_VALUE` on positive, `max(0, ...)` clamp on negative (0-valued entries retained in map), `consumeResource` for atomic spend checks, full NBT roundtrip via `ResourceLocation` string keys + negative sanitization + AIR skip; population lives in the same component (set-if-≥0, remove-if-sufficient) and is persisted with resources.
- **[[Town/Visits/Visit Buffer]]** (T-010 — pending) — arrival batching (~1s window per origin), distance accumulation, dedup, flush to history + payment.
- **[[Town/Leaderboard/Ranking Calculation]]** (T-011 — pending) — sort + tie-break on tourism metrics.
- **[[Town/Payment Board/Reward Claims]]** (T-012 — pending) — claimable bundled rewards (tourist fares + milestones), eligibility, expiry, multiplayer.
- **[[Town/Boundaries/Town Distance Validation|Town Distance Validation]]** (T-008) — placement/expansion gates (euclidean dist < (newB + other.getBoundaryRadius())), TownBoundaryService 1:1 pop vs Town 50-default border discrepancy documented; TownValidationService name/pos/radius/tourist/resource guards (pure paths + BlockPos).
- **[[Town/Storage/Slot-Based Storage]]** (T-013 — pending) — per-player / communal slot UI layer on top of the raw maps.
- [[Tourists/Tourists Overview|Tourists/Capacity]] (T-009 — see Tourist Allocation) — population-gated spawning, upgrade-driven max via tourist_cap, fair pop-proportional destination selection (note: recordSpawn wiring missing today), production cycles, upgrade application, contract lifecycle integration.

## How it connects
Towns are created on first placement of a Town Interface block (see TownInterfaceEntity + TownManager). The low-level resource bag (this note) backs production recipes (consume inputs, add outputs), trading (stock views + transfer), contracts (payout on delivery, escrow during auction), and tourist economy (fares credited on arrival). Population (also in TownEconomyComponent) drives tourist spawn rates and some upgrade caps. Higher storage (escrow in Town, personal maps, payment board, work units) live directly on Town and use similar overflow/clamp patterns but with different zero-pruning and dirty-marking policies. All persistent state funnels through the component save/load and Town's larger NBT.

Boundaries, visit history, and the payment board are the other primary town-owned systems surfaced to players.
