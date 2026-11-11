# AUTO destination learning

**TL;DR:** Platforms in **Auto** mode learn reachable towns from paid arrivals. Each route stores a score plus a rolling average **trip duration** (in-game ticks). Score decay uses a per-route half-life of `max(base half-life, avgTrip × autoFadeTripMultiplier)`. Any-Town tourists can **ride on** past intermediate stops (config `autoRideOn*`) so lines and loops teach farther towns; skipped stops get partial credit (`autoPassReachWeight`). Learned spawns use score × distance bias (`autoDistanceBias`, `autoRandomness`); fares unchanged.

## Related

- Design doc: `docs/TOURIST_DESTINATIONS.md`
- Code: `PlatformLearnedRoutes`, `TouristDestinationLearning`, `TouristAlightDecision`, `VisitorProcessingHelper`, `TouristSpawningHelper.selectTouristDestination`
- Config: `tourists.auto*` keys in `businesscraft.toml`

## Behaviour

| Event | Effect |
|--------|--------|
| Paid arrival (distance ≥ `minPayoutDistance`) | Increase score; EMA-update `avgTripDurationTicks` from `gameTime − tourist.spawnTime` |
| Any-Town stop, ride-on roll succeeds | Partial score via `autoPassReachWeight`; tourist stays on train (`RideOnSkips` / `RideThroughTownId` on entity) |
| Spawn (AUTO) | With probability `autoMaxLearnedShare × confidence`, pick learned town by score × distance factor ± randomness; else Any Town |
| Expiry without arrival (learned dest) | Subtract `autoExpiryPenalty` from that route’s score |
| Town removed | Drop town from all platforms’ learned tables and allow-lists |
| Time passes | Each route’s score decays with its own half-life (trip-aware) |

## Multi-stop topology

Open tickets used to alight at the **first** platform stop after origin, so A never learned C/D on A→B→C(→D) lines. See `TouristRouteDiscoverySimulatorTest` and the “Why multi-stop lines under-learned” section in `docs/TOURIST_DESTINATIONS.md`.

## Tests

- `PlatformLearnedRoutesTest` — NBT, decay, distance-biased picks
- `TouristRouteDiscoverySimulatorTest` — first-stop vs ride-on on line/loop
- `TouristAlightDecisionTest` — max skips and stay probability
