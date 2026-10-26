---
tags:
  - detail
  - town
---
# Platform Management

**Breadcrumb**: Town > Platforms > Platform Management
**TL;DR**: Each town's `PlatformManager` holds up to 10 `Platform` entries tied to Platform Interface block positions; players add via `addInterfacePlatform`, load uses `relink`, save/load drops rows without `interfacePos`, mutations notify for dirty/save, a client snapshot mirrors NBT for UI/render, and `getEnabledPlatforms()` feeds spawning.

## What it does
A town can have up to 10 platforms. Each platform is created when a **Platform Interface** block is placed inside the town boundary (`PlatformPlacement` + `addInterfacePlatform`). The manager owns the authoritative list: add via interface placement, remove when an interface breaks, path/destination/radius/spread/enabled edits from the interface UI (and packets), serialization with the Town Centre block entity, and a filtered enabled+complete view for tourists. The Town Centre's platform management screen is read-only; it does not call `addPlatform`. When data changes, a callback marks the Town Centre entity dirty. Clients rebuild from compact NBT via `updateClientPlatforms`.

## How it works (process view)
- **Add (player flow)** — Placing a Platform Interface inside the boundary calls `TownInterfaceEntity.addInterfacePlatform(pos)` → `PlatformManager.addInterfacePlatform`. Rejects at cap (10), duplicate interface position, or another interface within 5 blocks (3D). On success: new `Platform` with name `"Platform N"`, `enabled=true`, `interfacePos` set, optional path unset until configured on the block screen.
- **Relink (load)** — When an interface block entity loads with stored `platformId`, `relink(id, pos)` returns `LINKED`, `RELINKED` (recreates missing row), or an error (`AT_CAP`, `DUPLICATE`, `NOT_OURS`).
- **Mutate** — Path start/end, enabled toggle, destinations, search radius, and spread go through manager/platform setters (from interface UI); each notifies.
- **Remove** — Breaking the interface removes the platform (`removePlatform`). Breaking the Town Centre runs `releasePlatformsForRemovedTown` (inactive interfaces + dismiss tourists on those platforms).
- **Save/load** — `saveToNBT` writes a `platforms` list when non-empty. `loadFromNBT` and `updateClientPlatforms` **skip** any entry whose `interfacePos` is null (orphan data is discarded). There is **no** legacy "Main Platform" migration (`createLegacyPlatform` was removed; pre-alpha back-compat not supported).
- **Spawn filter** — `getEnabledPlatforms()` = `isEnabled() && isComplete()` (path endpoints set). Disabled platforms are omitted and their lamps stay unlit.
- **Worked example** — Empty town. Player places first interface at (10,64,5) → `addInterfacePlatform` returns UUID, size=1. They open the block UI and set path (10,64,5)→(30,64,5), search radius 10, spread 0, two destination towns. `getEnabledPlatforms()` returns one entry. Second interface at (12,64,5) is rejected (too close). After save/reload, interface calls `relink` → `LINKED`; platform without interface in NBT would not reload into the list.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `PlatformManager` | `common/src/main/java/com/quackers29/businesscraft/town/data/PlatformManager.java` | Collection + cap + notification + dual (server/client) lists + NBT orchestration |
| `addInterfacePlatform(BlockPos)` | same | Player flow: new Platform with `interfacePos`; null at cap/duplicate/too close |
| `relink(UUID, BlockPos)` | same | Load-time link; `RelinkResult` enum |
| `addPlatform()` | same | **Test/showcase helper only** — synthetic spaced positions; not player UI |
| `removePlatform(UUID)` | same | removeIf by id; returns true if removed; notifies |
| `getPlatform(UUID)` | same | Stream findFirst or null (no defensive copy of the Platform itself) |
| `setPlatformPathStart/End(UUID, BlockPos)` / `togglePlatformEnabled(UUID)` | same | Delegate to the Platform, then notifyChanged() |
| `getPlatforms(boolean isClientSide)` | same | Returns `new ArrayList<>(...)` — defensive copy of the server list or the client snapshot list |
| `getEnabledPlatforms()` | same | Stream filter: `p.isEnabled() && p.isComplete()`, then `.toList()` |
| `saveToNBT(CompoundTag)` | same | If platforms non-empty, writes ListTag "platforms" containing each `platform.toNBT()` |
| `loadFromNBT(CompoundTag)` | same | `platforms.clear()`, then for each compound in the list: `platforms.add(Platform.fromNBT(...))` — no notify |
| `updateClientPlatforms(CompoundTag)` | same | `clientPlatforms.clear()` + repopulate from tag's "platforms" list using fromNBT; used for client sync (debug log) |
| `canAddMorePlatforms()` / `getMaxPlatforms()` / `getPlatformCount()` | same | `size < 10`, 10, `size` respectively |
| `setChangeCallback(Runnable)` / `notifyChanged()` (private) | same | Callback is typically `TownInterfaceEntity::setChanged`; all mutating ops (add/remove/path/enable/legacy) fire it |
| `setPlatformCreationMode(...)` / `isInPlatformCreationMode()` / `getPlatformBeingEdited()` | same | UI state for the path-creation flow (which platform is being edited) — no persistence |
| `clear()` | same | Wipes both lists + resets creation mode fields (used for cleanup) |

## Rules & formulas (exact)
No arithmetic formulas; the rules are structural and operational.

- **Hard cap**: `MAX_PLATFORMS = 10` (constant). `addPlatform()` returns false immediately if `platforms.size() >= 10`; the new platform is never created.
- **Name on add**: `"Platform " + (platforms.size() + 1)` at the moment of insertion (1-based, reflects the count after this add in the name).
- **Defensive copies on read**: `getPlatforms(isClient)` and the internal `getEnabledPlatforms` path both allocate fresh collections. Callers can mutate the returned lists without affecting internal state. Individual `Platform` objects returned by `getPlatform` are the live ones (mutations via their setters are the intended path and go through the manager's setX methods which notify).
- **NBT write (saveToNBT)**: only emits the "platforms" key when `!platforms.isEmpty()`. Empty list → no key written (enables legacy detection on load and keeps tag small).
- **NBT read (loadFromNBT)**: always `platforms.clear()` first, then adds from tag only when `platform.getInterfacePos() != null`. Entries without interface position are dropped. Does not touch clientPlatforms or fire callback.
- **Client snapshot (updateClientPlatforms)**: clears client list, repopulates from tag with the same `interfacePos != null` filter as load.
- **Legacy migration**: removed — no `createLegacyPlatform`; old single-path Town Centre fields are not auto-imported.
- **Enabled + complete filter**: `getEnabledPlatforms()` uses `p.isEnabled() && p.isComplete()` (from Platform: both start and end non-null). This is the exact predicate used by TouristSpawningHelper and rendering.
- **Creation mode**: purely transient UI state; never serialized. `setPlatformCreationMode(mode, id)` sets the two fields; when mode=false the id is forced to null.
- **Change notification**: server mutations (addInterfacePlatform, relink when it adds, remove, path/toggle setters) call `notifyChanged()`. loadFromNBT, updateClientPlatforms, and clear do not notify.
- **getPlatform / remove / setX on unknown id**: get returns null; remove returns false; setX methods return false. Safe no-ops.

## Edge cases & behaviors
- Adding the 10th succeeds (size becomes 10, name uses 10); 11th add returns false, list size remains 10, no Platform allocated.
- remove of non-existent id: returns false, no notify.
- getEnabledPlatforms on a list with a mix of (enabled+complete), (enabled but incomplete), (disabled+complete) returns only the first group.
- save on empty list writes nothing under "platforms".
- loadFromNBT with missing or empty "platforms" tag leaves the list empty after clear.
- updateClientPlatforms with no "platforms" key leaves client list empty after clear (and logs "No platforms in NBT tag").
- loadFromNBT with platforms missing InterfacePos in NBT: those compounds load as Platform objects but are not added to the list.
- After clear(): size=0, client size=0, creation mode=false, edited id=null.
- getPlatforms(true) and getPlatforms(false) always return distinct new ArrayList instances even if contents match.
- Platform objects inside the lists are the same instances until replaced by load; their internal mutation (e.g. direct setStartPos on a gotten Platform) would bypass notify — but all production call sites go through the manager's delegating methods.
- Concurrent modification not an issue (single-threaded server tick + save/load on main).
- MAX_PLATFORMS is not configurable; it's a constant.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/PlatformManagerTest.java`
- Covered: cap, `addInterfacePlatform`, relink, remove, path/toggle delegation+notify, defensive copies, getEnabledPlatforms filter, save/load/updateClient NBT (including interfacePos filter), counts, creation-mode transient state, clear, roundtrips. See `PlatformManagerTest` and `PlatformInterfaceLinkTest`.
- Not covered: live TownInterfaceEntity integration (callback to setChanged, full BE save/load), packet-driven client sync, spawning consumption of getEnabledPlatforms (requires Level), visualization.
- All tests use only BlockPos + CompoundTag + UUID (no McBootstrap required). Full suite green.

## Open questions
- Direct mutation of a Platform obtained via `getPlatform(id)` (calling its setStartPos etc.) would bypass the manager's notifyChanged. All current callers use the manager wrapper methods, but the exposure of the live object is a foot-gun. A future hardening could return an immutable view or force all writes through the manager.
- `addPlatform()` still exists for unit tests/showcase; document and guard so it is not described as the player add flow (see [[Town/Platforms/Platform Interface]]).
- Client list is never defensively copied in the same way on read paths inside update (it is replaced wholesale). Since update is only called from desync paths this is fine, but getPlatforms(true) still wraps it in new ArrayList for callers.
- The "Platform N" naming uses the size at add time; if platforms are removed later the numbers are not renumbered. This matches current behavior (pinned by test if written); whether players expect dense numbering is a UX question.
- No persistence of creation-mode state across logins/saves (by design — it's a transient UI session).

## Related
- [[Town/Platforms/Platform Interface]] — placement, lamp, break, town removal
- [[Town/Platforms/Platform Data Model]] (T-015 — the individual Platform objects managed here)
- [[Town/Town Overview]]
- Used by: TownInterfaceEntity (list owner + `addInterfacePlatform` / `releasePlatformsForRemovedTown`), PlatformInterfaceEntity (per-block link), TouristSpawningHelper (`getEnabledPlatforms`)
- Callers of getEnabledPlatforms: tourist spawning loops and vehicle mounting logic
- The 10-cap is mentioned in the platform UI and in the data model note
