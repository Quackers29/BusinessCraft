# AUTO destination learning

**TL;DR:** Platforms in **Auto** mode learn reachable towns from paid arrivals. Each route stores a score plus a rolling average **trip duration** (in-game ticks). Score decay uses a per-route half-life of `max(base half-life, avgTrip × autoFadeTripMultiplier)`. Tourists record spawn world time in NBT (`SpawnTime`) so trip length is known on arrival.

## Related

- Design doc: `docs/TOURIST_DESTINATIONS.md`
- Code: `PlatformLearnedRoutes`, `TouristDestinationLearning`, `TouristSpawningHelper.selectTouristDestination`
- Config: `tourists.auto*` keys in `businesscraft.toml`

## Behaviour

| Event | Effect |
|--------|--------|
| Paid arrival (distance ≥ `minPayoutDistance`) | Increase score; EMA-update `avgTripDurationTicks` from `gameTime − tourist.spawnTime` |
| Spawn (AUTO) | With probability `autoMaxLearnedShare × confidence`, pick learned town by score; else Any Town |
| Expiry without arrival (learned dest) | Subtract `autoExpiryPenalty` from that route’s score |
| Town removed | Drop town from all platforms’ learned tables and allow-lists |
| Time passes | Each route’s score decays with its own half-life (trip-aware) |

## Tests

- `PlatformLearnedRoutesTest` — NBT round-trip including avg trip ticks; long-trip route outlasts short-trip after same elapsed time
