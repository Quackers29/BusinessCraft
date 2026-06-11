---
tags:
  - detail
  - town
---
# Platform Data Model

**Breadcrumb**: Town > Platforms > Platform Data Model
**TL;DR**: A Platform is a named, enableable path (start/end BlockPos) with a set of target destination town UUIDs (or "any town" when empty); it supports NBT roundtrip, defensive copies, a compat Map view of destinations, and isComplete check used to gate tourist spawning and visualization.

## What it does
Platforms define the physical rail/road paths and allowed destination towns for tourists leaving a given town. Up to 10 platforms per town (enforced by PlatformManager). Each platform can be toggled on/off by players, have its path endpoints set via the UI, and target specific other towns (or accept any). The data model is pure and persisted with the town's NBT; the isEnabled + isComplete predicate controls whether the platform is active for spawning and 3D path rendering.

## How it works (process view)
- Player opens Platform Management UI on a town, adds a new platform (name auto "Platform N", enabled=true, no path yet).
- Player enters path creation mode for a platform and right-clicks two blocks to set start and end positions (these become the bounds for spawning tourists along the line).
- Optionally, player selects specific destination towns from the list (or leaves empty = "any town" special case).
- Enabled + complete platforms are used by spawning logic and rendered in world with particles/lines.
- On save, the entire list of platforms (with their paths and dest sets) is written into the TownInterfaceEntity's NBT under the town data.
- **Worked example**: New platform after add: id=random, name="Platform 1", enabled=true, start=null, end=null, dests=empty. After setting path (10,64,20)→(40,64,20) and enabling two dest UUIDs D1/D2: isComplete()=true, hasNoEnabledDestinations()=false, getDestinations() returns {D1:true, D2:true}, getEnabledDestinations() returns copy of {D1,D2}. Saving to NBT produces compound with Id/Name/Enabled/StartX.. /Destinations:{Count:2,Dest0:D1,...}. Loading back yields equal Platform.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `Platform()` / `Platform(UUID)` / `Platform(String, boolean, BlockPos, BlockPos)` | `common/src/main/java/com/quackers29/businesscraft/platform/Platform.java` | Primary constructors; random ID for new, full state for legacy recreation |
| `Platform(CompoundTag)` / `fromNBT(CompoundTag)` / `save()` / `toNBT()` | same | Full NBT serialization roundtrip (core fields always written; positions and destinations optional on load) |
| `isComplete()` | same | Returns true only if both startPos and endPos are non-null — used as spawn/visual gate |
| `hasNoEnabledDestinations()` | same | True when enabledDestinations set is empty → signals "accept any town" (ANY_TOWN_DESTINATION special in spawning) |
| `enableDestination(UUID)` / `disableDestination(UUID)` / `isDestinationEnabled(UUID)` / `getEnabledDestinations()` / `clearEnabledDestinations()` / `setDestinationEnabled(UUID, boolean)` | same | Set management for target towns; get returns defensive copy |
| `getDestinations()` | same | Data transform: returns fresh `Map<UUID, Boolean>` with `true` for every enabled dest (for old Map-based call sites in packets/UI) |
| `equals(Object)` / `hashCode()` | same | Full structural equality including the mutable dest set contents |

## Rules & formulas (exact)
No numeric formulas; the "logic" is state machine + serialization + defensive data access.

- **Construction invariants**: Every Platform has a non-null UUID id (random in new ctors, loaded in tag ctor). Name and enabled always initialized (defaults "New Platform"/true).
- **Path completeness**: `isComplete() == (startPos != null && endPos != null)`. Partial paths (only start or only end) are incomplete and ignored for spawning/render.
- **Destination "any" semantics**: `hasNoEnabledDestinations() == enabledDestinations.isEmpty()`. Empty set means the platform accepts tourists going to any town (special UUID(0,0) sentinel used upstream in TouristSpawningHelper).
- **Compat transform**: `getDestinations()` always allocates a new HashMap and populates it with `true` values for the current enabled set. It never returns the internal set directly and never contains `false` entries (post-refactor all entries mean "enabled").
- **SetDestinationEnabled**: convenience that delegates to add or remove on the internal set; no-op on remove-missing.
- **NBT save (save/toNBT)**:
  - Always: "Id"(UUID), "Name"(String), "Enabled"(boolean)
  - If pos != null: "StartX/Y/Z" (int), "EndX/Y/Z" (int)
  - Always: "Destinations" compound { "Count": N, "Dest0": UUID, ... "DestN-1": UUID }
- **NBT load (tag ctor / fromNBT)**:
  - Core three fields read unconditionally (getUUID/getString/getBoolean will throw on absent key in MC CompoundTag — prod code always writes them).
  - Positions: guarded by `tag.contains("StartX")` etc → null if missing.
  - Destinations: if contains("Destinations") then read Count and loop "Dest"+i ; else remains empty set.
- **Defensive copies on read**: `getEnabledDestinations()` returns `new HashSet<>(enabledDestinations)`; `getDestinations()` builds fresh map. Callers can mutate the returned collections safely.
- **Equality**: includes id, name, enabled, startPos, endPos, and set equality on enabledDestinations. Two platforms are equal if they have identical state (including same dest set contents).

## Edge cases & behaviors
- Null positions: isComplete() false; save omits the Start/End keys; load with missing keys yields null pos.
- Empty destinations on save: still writes "Destinations":{Count:0} (no DestN entries).
- Duplicate enable: internal HashSet → no duplicates; enable twice is idempotent.
- Disable non-member: removeIf / remove on set is safe no-op.
- Tag missing core fields (Id/Name/Enabled): load will fail at runtime (get* throws) — not guarded (production never produces such tags).
- getDestinations() on a platform with dests: map size == set size, all values are Boolean.TRUE (the map form loses "disabled" info but current model doesn't store disabled entries).
- Legacy 3-arg? ctor with name/enabled/pos actually takes 4 (name,enabled,start,end); the two-pos ctor exists.
- after clearEnabledDestinations(): hasNo... becomes true.
- equals(null) / wrong class: false (standard).
- hashCode consistent with equals (uses Objects.hash on all fields).
- No public way to change id after creation (immutable identity).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/platform/PlatformTest.java`
- Covered: all ctors (incl. tag roundtrip), isComplete for null/partial/full paths, hasNoEnabledDest + destination add/remove/is/clear/get/set/getDestinations (map transform + defensive), NBT save/load for positions + destinations (0 and N), equals/hash including dest sets, getName/getId after load.
- Not covered: integration with PlatformManager (max 10, manager-level add/remove, client list, legacy create, notify callbacks — those are in manager), actual world spawning or packet flows (require MC), visualization.
- 25 tests; all edges from this note + defensive mutation cases + full roundtrips with hand-verified state. Full suite green.

## Open questions
- Core tag fields (Id/Name/Enabled) have no contains() guard in load ctor — if a corrupted or hand-crafted tag is loaded the get* calls will throw. Production always roundtrips correctly, but a defensive "Unknown" default on missing name (like Contract does) would be more robust. Pinned as current behavior.
- The getDestinations() Map<UUID,Boolean> is a compatibility shim; all current callers treat presence as enabled. If the model ever needs to persist "disabled but known" destinations, this method and the internal storage would need change (currently only enabled are stored).
- No clone/copy constructor; callers that need an independent copy must go through NBT (save+fromNBT) or manual field copy. Not a bug for current usage.
- Platform equals/hash includes mutable dest set — safe because HashSet equals is by contents, but if a caller mutates a getEnabledDestinations() copy and expects original equality it would be surprised (but defensive copy prevents that).

## Related
- [[Town/Town Overview]]
- [[Town/Platforms/Platform Data Model]] (this note)
- PlatformManager (in town/data) owns the list, enforces MAX=10, handles creation mode and NBT for the collection
- Used by: TownInterfaceEntity, TownInterfaceBlock (spawn gates), PlatformVisualizationRenderer, various Set*Packet handlers for path/dest/enabled mutations
- TouristSpawningHelper.selectTouristDestination consumes hasNoEnabledDestinations + getDestinations
