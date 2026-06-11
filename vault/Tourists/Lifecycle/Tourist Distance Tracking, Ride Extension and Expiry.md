---
tags:
  - detail
  - tourists
---
# Tourist Distance Tracking, Ride Extension and Expiry

**Breadcrumb**: Tourists > Lifecycle > Tourist Distance Tracking, Ride Extension and Expiry
**TL;DR**: TouristEntity tracks real cumulative distance traveled from position deltas (server tick, 2 s sampling window), only decrements its expiry timer while stationary (if enabled), resets the full timer once when first boarding a minecart or Create carriage, and derives cosmetic level (1–3) and skin tier directly from distance traveled (20 m per level); the live distance is what arrival processing uses for payments and milestones instead of town-to-town straight-line.

## What it does
Tourists are not teleported — they physically move (or ride) through the world. This system makes their journey "real": the mod measures how far each one actually went, uses that for the destination town's earnings, lets riders extend the tourist's lifetime by putting it on transport, and automatically expires tourists that get stuck so they don't pile up forever. The distance also drives a simple leveling system that changes the tourist's appearance (skin tier) and trading UI flavor over long journeys.

## How it works (process view)
- On construction (or first setPos) the entity snapshots its spawn position and "recent" position used for the periodic sampler.
- Every tick (server only): if not yet "hasMoved", check Euclidean distance from spawn > 2.0 blocks; once true it stays true and enables departure notifications on death/expiry.
- Every 40 ticks (2 s): compute delta from the last sample point, convert to blocks moved, add to totalDistanceTraveled, update the synced DATA_DISTANCE_TRAVELED float for clients, recompute isCurrentlyStationary (delta < 0.5 blocks in the window), derive speed (blocks / 2 s), and if fast enough and off cooldown with 50 % RNG, play celebrate + arm swing.
- If enableTouristExpiry and currently stationary: decrement expiryTicks; when it hits 0, notify origin (only if hasMoved) and discard the entity.
- When startRiding succeeds and the vehicle is AbstractMinecart or its class name contains "create.content.trains" and the one-time flag is not set: recompute full expiry from the live ConfigLoader.touristExpiryMinutes, set the flag so it only happens once per tourist lifetime.
- Leveling is recomputed after each distance sample: targetLevel = min(3, 1 + floor(total / 20)); if higher than current villager level, set it and sync the skin tier (level-1 clamped).
- The public getTotalDistanceTraveled() returns the entity-synced value on client, the live field on server — this value is read by VisitorProcessingHelper on arrival to feed the real-path payment and milestone checks.
- All tracking state (distance, flags, positions, timers, origin/dest, spawn time) is round-tripped through NBT on save/load so a tourist that rides a long train, gets chunk-unloaded, and reloads still has the correct accumulated distance and remaining life.

**Worked example**: A tourist spawns with 120 min expiry (default). It stands still for 30 s (900 ticks of stationary decrement) then boards a stopped minecart. Ride extension fires once: expiry is reset to the current 120 min value. While riding it accumulates distance in the periodic sampler. After traveling 47 blocks it has targetLevel = 1 + floor(47/20) = 3, skin tier 2 (luxury). On arrival the destination reads ~47 m via getTotalDistanceTraveled(), which (with count=1, 50 m/emerald) yields the payment.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TouristEntity.tick()` | `common/src/main/java/com/quackers29/businesscraft/entity/TouristEntity.java` (~184-311) | Main driver: hasMoved detection, 2 s stationary/speed/distance sampler, expiry countdown (only when stationary), level update |
| `TouristEntity.startRiding(Entity, boolean)` / startRiding(Entity) | same (~559-581) | One-time ride extension: full expiry reset + flag when boarding minecart or Create carriage |
| `TouristEntity.setPos(double,double,double)` | same (~584-595) | Captures initial spawnPos* and recentPos* on first call (the 0,0,0 sentinel) |
| `updateLevelFromDistance()` (private) | same (~732-743) | target = min(MAX_LEVEL, 1 + (int)(total / DISTANCE_PER_LEVEL)); set if higher and sync skin |
| `skinTierForLevel(int)` (private static) | same (~411-413) | `Math.min(Math.max(level,1), MAX_LEVEL) - 1` |
| `getVillagerXp()` (override) | same (~696-715) | Distance-into-level scaled to vanilla XP thresholds (10/70/150) for the progress bar |
| `getTotalDistanceTraveled()` | same (~407-409) | Server field or client entityData float; consumed by arrival payment logic |
| `formatTicks(int)` (private) | same (~855-860) | `"%dm %ds"` used in the info paper lore |
| `addAdditionalSaveData` / `readAdditionalSaveData` | same (~420-520) | Full persistence of all tracking fields, flags, origin/dest, spawnTime |

## Rules & formulas (exact)
From TouristEntity.java:

Distance accumulation & stationary (tick, server, every 40 ticks):
```java
double distanceMoved = Math.sqrt(dx*dx + dy*dy + dz*dz);
double speed = distanceMoved / (POSITION_UPDATE_INTERVAL / 20.0); // blocks per second
isCurrentlyStationary = recentMovementSquared < (STATIONARY_THRESHOLD * STATIONARY_THRESHOLD);
totalDistanceTraveled += distanceMoved;
this.entityData.set(DATA_DISTANCE_TRAVELED, (float) totalDistanceTraveled);
updateLevelFromDistance();
```

Expiry countdown (server, every tick):
```java
if (ConfigLoader.enableTouristExpiry && isCurrentlyStationary) {
    expiryTicks--;
    if (expiryTicks <= 0) { notify...; discard(); }
}
```

Ride extension (only once):
```java
if (result && !hasReceivedRideExtension) {
    if (entity instanceof AbstractMinecart || entity.getClass().getName().contains("create.content.trains")) {
        this.expiryTicks = (int)(ConfigLoader.touristExpiryMinutes * 60 * 20);
        hasReceivedRideExtension = true;
    }
}
```

Level from distance:
```java
int targetLevel = 1 + (int)(totalDistanceTraveled / DISTANCE_PER_LEVEL); // DISTANCE_PER_LEVEL = 20.0
targetLevel = Math.min(targetLevel, MAX_LEVEL); // 3
if (targetLevel > currentLevel ...) { setLevel(targetLevel); syncSkinTierFromLevel(); }
```

Skin tier:
```java
private static int skinTierForLevel(int level) {
    return Math.min(Math.max(level, 1), MAX_LEVEL) - 1;  // 1→0, 2→1, 3→2
}
```

XP shown in trading UI (client uses synced distance):
```java
double distanceIntoCurrentLevel = distance - ((currentLevel-1) * 20.0);
int maxXpForLevel = (currentLevel==1 ? 10 : currentLevel==2 ? 70 : 150);
int xp = (int)((distanceIntoCurrentLevel / 20.0) * maxXpForLevel);
return Math.max(0, Math.min(maxXpForLevel, xp));
```

Spawn/ctor/ride expiry injection (duplicated):
```java
this.expiryTicks = (int)(ConfigLoader.touristExpiryMinutes * 60 * 20);
```

hasMoved (first movement gate for notifications):
```java
if (distanceSquared > MOVEMENT_THRESHOLD * MOVEMENT_THRESHOLD) hasMoved = true;  // 2.0
```

Constants (all private static final unless noted):
- MOVEMENT_THRESHOLD = 2.0
- STATIONARY_THRESHOLD = 0.5
- POSITION_UPDATE_INTERVAL = 40
- SPEED_THRESHOLD = 0.5 (blocks/s)
- SPEED_REACTION_COOLDOWN = 100 ticks
- DISTANCE_PER_LEVEL = 20.0
- MAX_LEVEL = 3
- ANY_TOWN_DESTINATION = new UUID(0, 0)  (public)

## Edge cases & behaviors
- `totalDistanceTraveled` only increases on server; client reads the synced float.
- Expiry only decrements while `isCurrentlyStationary` (within 0.5 blocks in a 2 s window) AND the global enable flag is true. Moving (even slowly) or config disabled pauses the countdown.
- Ride extension is strictly one-time per tourist (`hasReceivedRideExtension`). Boarding a second minecart later does nothing to the timer.
- Non-minecart / non-Create vehicles (e.g. boats, other mobs) never trigger the reset even if the tourist rides them.
- First setPos (the 0,0,0 guard) is what seeds both the "hasMoved from spawn" reference and the recent sampler. If an entity is constructed but never positioned, these stay 0.
- hasMoved starts false; a tourist that expires without ever moving >2 m from spawn position suppresses the visual departure notification (see notifyOriginTownOfQuitting).
- Level and skin are derived; setting via other means is overridden by the distance rule on next sample.
- Speed celebrate has its own 10 s cooldown independent of expiry; 50 % random chance even when eligible.
- Distance is a double and can grow without bound in theory (no cap); the level math just clamps at 3.
- NBT restores every tracking field; a tourist that was mid-journey keeps its exact accumulated meters and remaining ticks across restarts.
- Client-side tick still runs the movement math for the local entityData sync, but expiry decrement is guarded by !isClientSide.
- The "real path" distance (this field) replaced the old Euclidean town-to-town calc for payments (see VisitorProcessingHelper processIndividualVisitor and the comment "CORE CHANGE").

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/entity/TouristEntityTest.java`
- Covered: expiry injection formula (with ConfigLoader save/restore in @BeforeEach/@AfterEach), distance-to-level formula (1 + floor(d/20) capped at 3), skin tier mapping expression (exact body pinned), XP scaling per level, formatTicks, movement/stationary/speed window constants + derived math (thresholds, 2 s window, bps calc), all with hand-computed expects in comments.
- Pinned quirks (current behavior): ride extension is one-shot only; expiry decrements only on stationary; hasMoved requires strict >2.0 from captured spawn pos.
- Not covered (NEEDS-MC): full tick loop, startRiding side effects, setPos capture, actual entity construction (requires EntityType + Level + registries), periodic sampler integration, celebrate RNG/sound, death/expiry paths, NBT on a live TouristEntity, client/server entityData sync, and any reflection on TouristEntity private members (class load triggers EntityDataAccessor/Synched statics that go beyond McBootstrap). The pure math bodies are pinned directly in tests instead.
- No production code was modified. Tests stay green and provide executable spec for every formula listed in "Rules & formulas (exact)".

## Open questions
- The ride detection `entity.getClass().getName().contains("create.content.trains")` is a string heuristic rather than an interface or tag; it may miss renamed or relocated Create classes after updates.
- Expiry reset on ride always uses the current ConfigLoader value (good for hot reload) but means two tourists boarding at different config settings can have different lifetimes even on the same train.
- hasMoved and the 2.0 threshold affect only notifications, not payments or expiry. A tourist that is pushed 1.9 blocks and then rides will still be treated as "never moved" for the quit notification.
- The speed sampler always uses a fixed 2 s wall of POSITION_UPDATE_INTERVAL regardless of actual tick rate or lag; very low TPS can produce misleading "stationary" or speed values.
- Skin tier and level are purely cosmetic (and trading-offer flavor); nothing in the economy or capacity systems reads them.
- formatTicks and the info paper lore are only visible when a player trades with the tourist; the same data is available via get* accessors for other UIs.

## Related
- [[Tourists/Tourists Overview]]
- [[Tourists/Capacity/Tourist Allocation]] (T-009)
- [[Tourists/Capacity/Tourist Capacity Calculation]] (T-027)
- [[Tourists/Spawning/Tourist Spawning and Destination Selection]] (T-037)
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — consumes getTotalDistanceTraveled)
- [[Town/Visits/Visit Buffer]] (T-010 — stores the per-origin distance captured from the entity)
- [[Economy/Milestones/Distance Milestone Resolution]] (T-002 — same distance drives milestone item rewards)
