---
tags:
  - detail
  - town
---
# Platform Data Model

**Breadcrumb**: Town > Platforms > Platform Data Model
**TL;DR**: A Platform is a named, enableable record tied to a Platform Interface block position, with path endpoints, search radius, lateral spread, and destination allow-list (empty = any town); `isEnabled && isComplete` gates spawning and the interface lamp.

## What it does
Each platform row belongs to one **Platform Interface** block (`interfacePos`). Tourists spawn along the segment from `startPos` to `endPos` using `PlatformSpawn` (point on the line plus lateral **spread** within the platform's **search radius**). Players edit path, enabled flag, search radius (default 10, max `platforms.maxSearchRadius`), spread (default 0 = on the path line; cannot exceed that platform's radius), and destinations on **PlatformInterfaceScreen**. Up to 10 platforms per town (`PlatformManager`). Disabled platforms are skipped for spawning and show an unlit lamp. Data persists in the town NBT; load drops platforms with no interface position.

## How it works (process view)
- Platform is created when an interface is placed (`addInterfacePlatform` sets `interfacePos` and default name `"Platform N"`).
- Player configures start/end path, optional destination UUIDs (empty set = any town), search radius, and spread on the interface block UI.
- `TouristSpawningHelper` uses enabled + complete platforms only; spawn position from `PlatformSpawn.point` along the segment.
- Town Centre platform tab displays the list read-only; it does not create platforms.
- **Worked example**: After placement: id=random, name="Platform 1", enabled=true, interfacePos=(10,64,5), path unset → `isComplete()` false, not in `getEnabledPlatforms()`. After path (10,64,5)→(40,64,5), radius 10, spread 2, dests {D1}: complete and spawnable. Disable → omitted from spawning, lamp off.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `Platform()` / `Platform(UUID)` | `common/src/main/java/com/quackers29/businesscraft/platform/Platform.java` | New platform (random id) or relink with fixed id |
| `getInterfacePos()` / `setInterfacePos()` | same | Link to Platform Interface block; required for load into manager list |
| `getSearchRadius()` / `setSearchRadius()` | same | Default `PlatformSpawn.DEFAULT_RADIUS` (10); clamped to config max when edited |
| `getSpread()` / `setSpread()` | same | Default 0; capped to platform search radius |
| `Platform(String, boolean, BlockPos, BlockPos)` | same | Legacy ctor for tests; not player migration |
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
  - Always: "Id", "Name", "Enabled"
  - Optional: Start/End XYZ, Interface XYZ, SearchRadius, Spread
  - "Destinations" compound { "Count": N, "Dest0": UUID, ... }
- **Load into manager**: `PlatformManager.loadFromNBT` / `updateClientPlatforms` only retain entries with non-null `interfacePos`.
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
- Not covered: full PlatformManager + interface placement integration (see manager/interface notes), live spawning (NEEDS-MC).
- 25 tests; all edges from this note + defensive mutation cases + full roundtrips with hand-verified state. Full suite green.

## Open questions
- Core tag fields (Id/Name/Enabled) have no contains() guard in load ctor — if a corrupted or hand-crafted tag is loaded the get* calls will throw. Production always roundtrips correctly, but a defensive "Unknown" default on missing name (like Contract does) would be more robust. Pinned as current behavior.
- The getDestinations() Map<UUID,Boolean> is a compatibility shim; all current callers treat presence as enabled. If the model ever needs to persist "disabled but known" destinations, this method and the internal storage would need change (currently only enabled are stored).
- No clone/copy constructor; callers that need an independent copy must go through NBT (save+fromNBT) or manual field copy. Not a bug for current usage.
- Platform equals/hash includes mutable dest set — safe because HashSet equals is by contents, but if a caller mutates a getEnabledDestinations() copy and expects original equality it would be surprised (but defensive copy prevents that).

## Related
- [[Town/Town Overview]]
- [[Town/Platforms/Platform Interface]]
- [[Town/Platforms/Platform Management]]
- PlatformManager owns the list; PlatformInterfaceEntity edits per-block fields via packets
- TouristSpawningHelper: `getEnabledPlatforms`, `PlatformSpawn.point`, destination selection
