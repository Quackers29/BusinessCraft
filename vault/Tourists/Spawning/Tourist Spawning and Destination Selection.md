---
tags:
  - detail
  - tourists
---
# Tourist Spawning and Destination Selection

**Breadcrumb**: Tourists > Spawning > Tourist Spawning and Destination Selection
**TL;DR**: When a town is eligible, TouristSpawningHelper spawns a TouristEntity on a platform path if under per-platform limit (ConfigLoader.maxTouristsPerTown); destination is chosen from the platform's enabled UUID set or falls back to the special ANY_TOWN (UUID 0-0); selection among concrete towns uses population-weighted fairness via TouristAllocationTracker after excluding the origin; spawn point is a random point along the platform segment (lerp by random progress), retried up to 3 times to avoid near-overlap with existing villagers in the path AABB; successful spawn sets full expiry from config and increments the origin's touristCount.

## What it does
This is the core spawning engine that turns eligible towns + configured platforms into live tourists in the world. It enforces capacity before creating entities, picks where each tourist is going (specific towns or "any"), places them on the physical platform path without immediate collision, gives them an expiry lifetime (configurable, extendable by riding), and wires origin/destination metadata so arrival processing, payments, notifications, and removal tracking all work later. Without this, the tourism economy never starts.

## How it works (process view)
- TouristSpawningHelper.spawnTouristOnPlatform is called (typically from a block entity's tick or scheduled spawn) with the origin Town, a Platform, and the origin's UUID.
- First gate: `town.canAddMoreTourists()` (which itself checks spawning enabled + pop >= minPopForTourists + local count vs. tourist_cap + global maxTouristsPerTown).
- Count current Villagers (any) inside an AABB expanded 1 block around the platform segment at platform Y to Y+2. If that count >= ConfigLoader.maxTouristsPerTown, skip (treated as "per platform" limit in this code even though the config key name says per-town).
- Pick destination: `selectTouristDestination` looks at the platform's getDestinations() map (UUID -> enabled boolean).
  - If empty or `hasNoEnabledDestinations()` → return the magic ANY_TOWN_DESTINATION (new UUID(0,0)).
  - Else collect enabled UUIDs; if none left → ANY; else `selectFairTownByPopulation`.
- Fair select: asks TownManager for all towns, removes the origin, intersects with the allowed list if present, builds a Map<UUID,Integer> of population (note: `(int) town.getPopulation()` — long truncated), then delegates entirely to `TouristAllocationTracker.selectFairDestination(originId, popMap)`.
- With a destination in hand (or ANY), try up to 3 times:
  - progress = random.nextDouble()
  - exactX = startX + (endX - startX) * progress; same for Z
  - spawnPos = round(exact) at Y+1
  - occupancy test: any existing tourist in the pre-counted list is within <1 block in X and Z of the candidate.
  - If clear and the two blocks (pos and above) are air → proceed to spawn.
- spawnTourist (private): creates a TouristEntity via PlatformAccess entity registry, sets its pos to center of block, computes expiryTicks = (int)(ConfigLoader.touristExpiryMinutes * 60 * 20), adds to level. On success calls originTown.addTourist() (which goes through TownService) and logs.
- The TouristEntity itself receives origin/dest UUID+name, stores spawn pos for the "hasMoved" detector, and will handle its own distance accumulation, stationary expiry countdown, ride-based reset, and on-discard notification/payment side effects elsewhere.

**Worked example**: Town A (pop 12, spawning enabled, tourist_cap=30 from upgrades, current touristCount=2) has a platform from (10,64,10) to (30,64,20) targeting two other towns B (pop 40) and C (pop 10). maxTouristsPerTown=1000. At spawn time the platform path AABB has 0 villagers. select sees two enabled dests, builds pop map excluding A, tracker returns B (say most under-allocated). progress=0.37 → spawn at roughly X=17, Z=14, Y=65. Blocks are air, no overlap → TouristEntity created with 120-minute expiry (default), origin=A, dest=B. Town A touristCount becomes 3.

The rate and expiry are hot from ConfigLoader; platform destinations are live from the Platform data object.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TouristSpawningHelper.spawnTouristOnPlatform(Level, Town, Platform, UUID)` | `common/src/main/java/com/quackers29/businesscraft/town/data/TouristSpawningHelper.java` (lines 48-121) | Main entry; capacity + per-platform headcount gate, destination choice, 3-attempt positioned spawn |
| `TouristSpawningHelper.selectTouristDestination(Level, Platform, UUID)` | same (~133-161) | Decides ANY vs concrete; filters platform.getDestinations() for enabled; delegates fair select |
| `TouristSpawningHelper.selectFairTownByPopulation(ServerLevel, List<UUID>, UUID)` | same (~172-197) | Strips origin + allowed filter, builds int population map via TownManager, calls tracker |
| `TouristSpawningHelper.spawnTourist(...)` (private) | same (~209-243) | Air check, TouristEntity construction + expiry + addFreshEntity + origin.addTourist on success |
| `Platform.getStartPos/getEndPos/getDestinations()/hasNoEnabledDestinations()` | `common/src/main/java/com/quackers29/businesscraft/platform/Platform.java` | Supplies the geometric line and the enabled-destination map used for choice |
| `TouristAllocationTracker.selectFairDestination(UUID, Map<UUID,Integer>)` | `common/src/main/java/com/quackers29/businesscraft/town/utils/TouristAllocationTracker.java` | The actual weighted choice (already covered T-009); called after map prep here |
| `Town.canAddMoreTourists()` / `addTourist()` | `common/src/main/java/com/quackers29/businesscraft/town/Town.java` + TownService | Pre-spawn gate and post-spawn counter (delegates) |
| `ConfigLoader.maxTouristsPerTown`, `touristExpiryMinutes` | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` | Tunables read live |
| `TouristEntity` ctor + `setExpiryTicks` | `common/src/main/java/com/quackers29/businesscraft/entity/TouristEntity.java` | The thing that actually gets spawned and carries the metadata/distance/expiry |

## Rules & formulas (exact)
From TouristSpawningHelper.java:

```java
// capacity gate
if (!town.canAddMoreTourists()) return false;

// per-platform count (note config name)
AABB pathBounds = new AABB( minX-1, y, minZ-1, maxX+1, y+2, maxZ+1 );
List<Villager> existing = level.getEntitiesOfClass(Villager.class, pathBounds);
if (existingTourists.size() < ConfigLoader.maxTouristsPerTown) { ... }

// destination decision
Map<UUID,Boolean> dests = platform.getDestinations();
if (dests.isEmpty() || platform.hasNoEnabledDestinations()) {
    return ANY_TOWN_DESTINATION;   // UUID(0,0)
}
List<UUID> enabled = ... filter value==true ...
if (enabled.isEmpty()) return ANY_TOWN_DESTINATION;
return selectFairTownByPopulation(... enabled ...);
```

```java
// fair select prep (note long->int)
Map<UUID, Town> possible = ... copy, remove(origin), retainAll(allowed if present);
Map<UUID,Integer> populationMap = new HashMap<>();
for (...) populationMap.put(id, (int) town.getPopulation() );
return TouristAllocationTracker.selectFairDestination(originTownId, populationMap);
```

```java
// spawn point selection (3 attempts)
for (int attempt=0; attempt<3; attempt++) {
    double progress = random.nextDouble();
    double exactX = start.getX() + (end.getX()-start.getX()) * progress;
    ... same for Z ...
    BlockPos spawnPos = new BlockPos( (int)Math.round(exactX), startY+1, (int)Math.round(exactZ) );
    boolean occupied = existing.stream().anyMatch(v -> abs(v.x - spawn.x)<1 && abs(v.z-spawn.z)<1 );
    if (!occupied && air && above air) {
        ... create TouristEntity ...
        tourist.setPos(x+0.5, y, z+0.5);
        tourist.setExpiryTicks( (int)(ConfigLoader.touristExpiryMinutes * 60 * 20) );
        if (level.addFreshEntity(tourist)) { origin.addTourist(); return true; }
    }
}
```

- ANY_TOWN_DESTINATION is a special sentinel (duplicated constant also in TouristEntity as public).
- The occupancy test only looks at X/Z within <1 (so same-block or adjacent centers are blocked); Y ignored beyond the initial AABB filter.
- Expiry is recomputed from current config at spawn time (not captured at platform creation).
- On spawn success the origin town's tourist counter goes up immediately; the allocation tracker `recordTouristSpawn` is **not** called from this helper (see Open questions and overview note).
- If destination lookup (for the tag name) fails via TownManager the tourist still spawns with "Destination N/A".

## Edge cases & behaviors
- `canAddMoreTourists()` false (pop too low, spawning off, at cap, at global max) → no spawn, debug log.
- Platform has no start/end pos → immediate false.
- No enabled destinations on platform (or all disabled) → spawns with ANY_TOWN_DESTINATION; the entity will pick a real destination on arrival or wander?
- Allowed list after origin removal + filter is empty → returns null from fair select → spawnTouristOnPlatform returns false (no spawn).
- All 3 position attempts collide or are non-air → false (no spawn that tick).
- `getPopulation()` returns long > Integer.MAX_VALUE → truncated by (int) cast (theoretical; populations are small).
- Existing tourists list includes regular villagers too (any Villager.class), not just TouristEntity — conservative count.
- The per-platform check uses the town-wide config value `maxTouristsPerTown` (1000 default) — if you have many platforms this is a very loose per-platform throttle.
- Spawn only happens server-side (the ServerLevel cast is inside select; spawnTouristOnPlatform can be called on client but will early exit or fail).
- TouristEntity creation uses the platform-registered EntityType via PlatformAccess — must be present or ClassCastException.
- After spawn the TouristEntity's tick() will start the stationary expiry countdown (unless riding immediately).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/TouristSpawningHelperTest.java`
- Covered: trivial instantiation smoke test (McBootstrap for safety); the class itself and its constants/ctor paths.
- Not covered (NEEDS-MC): selectTouristDestination, selectFairTownByPopulation, spawnTouristOnPlatform, the 3-attempt lerp+occupancy, air checks, expiry injection, addTourist side effect, any interaction with real TownManager/ServerLevel/Platform data or entity addition. These require a live server world with registered towns and platforms.
- No reflection tests on private spawnTourist because it performs world mutation (addFreshEntity + Town mutation) and is not a pure calculation.
- Formulas and edge rules are exhaustively described in this note for future test or manual verification work.

## Open questions
- `ConfigLoader.maxTouristsPerTown` is documented and used as a "per platform" concurrent limit inside the helper (see comment "max tourists per platform") yet the key name and other uses treat it as a global town cap. This is a naming vs. usage discrepancy; changing it affects both the global hard gate in canAddMoreTourists and the local head-count here.
- `TouristAllocationTracker.recordTouristSpawn` / `recordTouristRemoval` are never called from TouristSpawningHelper or the arrival path in the current code (the overview note for T-009 already pins the tracker as "inert"). Destinations chosen here are not fed back into the fairness state, so the 10% random + most-under-allocated bias has no persistent effect across spawns.
- The ANY_TOWN sentinel (UUID 0-0) is defined both here (private) and publicly on TouristEntity; on arrival the "any" case must be resolved by some other system (VisitorProcessingHelper or TownInterfaceEntity?) — that resolution path is outside this class.
- Population is cast to int for the tracker map; if a town ever legitimately has >2B population the value wraps negative and would break fairness math.
- The occupancy test uses a raw <1 block manhattan-style check on X/Z only; two tourists whose centers are 0.9 blocks apart in X but on different Y or with Z diff will still block. Combined with the AABB that is only 2 blocks tall, this can produce surprising "can't spawn" results on vertical platforms or crowded paths.
- Spawning still goes through the deprecated-feeling Town.canAddMoreTourists + addTourist even though TownService is the new home; TouristSpawningHelper has not been updated to call the service directly for the pre/post checks.

## Related
- [[Tourists/Tourists Overview]]
- [[Tourists/Capacity/Tourist Allocation]] (T-009 — the delegate that receives the prepared population map)
- [[Tourists/Capacity/Tourist Capacity Calculation]] (T-027 — the canAddMoreTourists gate called first)
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — what a successfully spawned and traveled tourist eventually pays on arrival)
- [[Town/Visits/Visit Buffer]] (T-010 — arrivals feed the buffer after this spawn succeeds)
