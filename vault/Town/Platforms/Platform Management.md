---
tags:
  - detail
  - town
---
# Platform Management

**Breadcrumb**: Town > Platforms > Platform Management
**TL;DR**: Each town owns a PlatformManager that holds up to 10 Platform entries; it enforces the cap on add, notifies on mutations (for dirty/save), keeps a parallel client snapshot for UI/render sync, writes the list to NBT only when non-empty, can bootstrap a legacy "Main Platform" from old single-path data, and provides a filtered view of only enabled+complete platforms for spawning and visualization.

## What it does
A town can define up to 10 distinct arrival/departure platforms (paths with optional destination filters). The manager is the collection owner: it creates, removes, mutates, serializes, and limits them. The TownInterfaceEntity (the block that players interact with) delegates all platform UI and logic through this manager. When platforms change, the manager fires a callback so the block entity marks itself dirty for saving. On the client, a separate list is kept up to date via compact NBT snapshots so the management screen and world overlays stay in sync without sending full town data.

## How it works (process view)
- When a player opens the Platform Management tab and clicks "Add Platform", the manager checks `canAddMorePlatforms()` (size < 10). If allowed it creates a fresh Platform (random ID, name "Platform N", enabled=true, no path yet), appends it, notifies the callback, and returns true.
- Player can then enter creation mode for a platform and set start/end BlockPos (via right-clicks on the world). The manager records these on the specific platform and notifies.
- Enabling/disabling a platform or adding/removing target destinations are direct mutations that also notify.
- On every world save the block entity asks the manager to `saveToNBT(tag)`. If the list is empty, nothing is written (keeps saves small and lets legacy detection work). When loading on server, `loadFromNBT` clears the server list and repopulates from the "platforms" ListTag using Platform.fromNBT.
- For clients (or after a sync packet), `updateClientPlatforms(tag)` clears the client-only list and rebuilds it from the same tag — this is the fast path used by response packets and the block entity's client load.
- If an old save has legacy "PathStart"/"PathEnd" at the block-entity level and the manager list is still empty, `createLegacyPlatform(start, end)` will (once) synthesize a single enabled "Main Platform" with that path so old worlds keep working.
- Spawning code and the 3D renderer ask for `getEnabledPlatforms()` — a filtered list containing only platforms where `isEnabled() && isComplete()`. Incomplete or disabled platforms are ignored for tourists and visuals.
- **Worked example**: Fresh town, manager starts empty. Player adds first platform → addPlatform returns true, list size=1, name="Platform 1". Player sets path (100,64,200)→(150,64,200) and enables two destination UUIDs. Now getPlatformCount()=1, canAddMorePlatforms()=true (9 slots left), getEnabledPlatforms() returns that one (enabled+complete). saveToNBT writes a "platforms" ListTag with one entry. On reload, loadFromNBT restores it exactly (defensive copies on all getters). Client receives a sync tag and calls updateClientPlatforms; its list now mirrors for the UI. At 10 platforms, 11th add returns false and size stays 10.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `PlatformManager` | `common/src/main/java/com/quackers29/businesscraft/town/data/PlatformManager.java` | Collection + cap + notification + dual (server/client) lists + NBT orchestration + legacy migration |
| `addPlatform()` | same | Creates default-named enabled Platform; returns false at MAX_PLATFORMS (10) without adding |
| `removePlatform(UUID)` | same | removeIf by id; returns true if removed; notifies |
| `getPlatform(UUID)` | same | Stream findFirst or null (no defensive copy of the Platform itself) |
| `setPlatformPathStart/End(UUID, BlockPos)` / `togglePlatformEnabled(UUID)` | same | Delegate to the Platform, then notifyChanged() |
| `getPlatforms(boolean isClientSide)` | same | Returns `new ArrayList<>(...)` — defensive copy of the server list or the client snapshot list |
| `getEnabledPlatforms()` | same | Stream filter: `p.isEnabled() && p.isComplete()`, then `.toList()` |
| `saveToNBT(CompoundTag)` | same | If platforms non-empty, writes ListTag "platforms" containing each `platform.toNBT()` |
| `loadFromNBT(CompoundTag)` | same | `platforms.clear()`, then for each compound in the list: `platforms.add(Platform.fromNBT(...))` — no notify |
| `updateClientPlatforms(CompoundTag)` | same | `clientPlatforms.clear()` + repopulate from tag's "platforms" list using fromNBT; used for client sync (debug log) |
| `createLegacyPlatform(BlockPos, BlockPos)` | same | One-time migration: only if server list empty AND both positions non-null → add a named "Main Platform" (enabled, path set) + notify + info log |
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
- **NBT read (loadFromNBT)**: always `platforms.clear()` first (even if tag has no "platforms" key), then conditionally adds. Does not touch clientPlatforms list or fire callback.
- **Client snapshot (updateClientPlatforms)**: always clears client list first, then if tag has "platforms" ListTag, adds fromNBT entries. Used on client load and by BufferSlotStorageResponsePacket-style syncs. Includes debug logging of count.
- **Legacy migration (createLegacyPlatform)**: guard `if (platforms.isEmpty() && pathStart != null && pathEnd != null)`. Creates `new Platform("Main Platform", true, pathStart, pathEnd)`, adds it, notifies, and does an INFO log. Only happens once per manager (subsequent calls with empty check fail the isEmpty).
- **Enabled + complete filter**: `getEnabledPlatforms()` uses `p.isEnabled() && p.isComplete()` (from Platform: both start and end non-null). This is the exact predicate used by TouristSpawningHelper and rendering.
- **Creation mode**: purely transient UI state; never serialized. `setPlatformCreationMode(mode, id)` sets the two fields; when mode=false the id is forced to null.
- **Change notification**: every server-mutating public method that alters the authoritative `platforms` list (add, remove, the three set/toggle paths, createLegacy) calls `notifyChanged()` which invokes the callback if set. loadFromNBT, updateClientPlatforms, and clear do not notify.
- **getPlatform / remove / setX on unknown id**: get returns null; remove returns false; setX methods return false. Safe no-ops.

## Edge cases & behaviors
- Adding the 10th succeeds (size becomes 10, name uses 10); 11th add returns false, list size remains 10, no Platform allocated.
- remove of non-existent id: returns false, no notify.
- getEnabledPlatforms on a list with a mix of (enabled+complete), (enabled but incomplete), (disabled+complete) returns only the first group.
- save on empty list writes nothing under "platforms".
- loadFromNBT with missing or empty "platforms" tag leaves the list empty after clear.
- updateClientPlatforms with no "platforms" key leaves client list empty after clear (and logs "No platforms in NBT tag").
- createLegacyPlatform called when list already has entries: no-op (the isEmpty guard).
- createLegacyPlatform with one or both positions null: no-op.
- After clear(): size=0, client size=0, creation mode=false, edited id=null.
- getPlatforms(true) and getPlatforms(false) always return distinct new ArrayList instances even if contents match.
- Platform objects inside the lists are the same instances until replaced by load; their internal mutation (e.g. direct setStartPos on a gotten Platform) would bypass notify — but all production call sites go through the manager's delegating methods.
- Concurrent modification not an issue (single-threaded server tick + save/load on main).
- MAX_PLATFORMS is not configurable; it's a constant.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/PlatformManagerTest.java`
- Covered: 26 tests — add under cap + at cap (with naming), remove (found/missing), get (present/absent), path/toggle delegation+notify via callback, getPlatforms defensive copy (server + client), getEnabledPlatforms filter predicate, saveToNBT (empty omits key vs populated), loadFromNBT (clear+repopulate, missing key), updateClientPlatforms (clear+rebuild from tag, missing key), createLegacy (happy path + already-populated guard + null-pos guard), counts/canAdd/getMax, creation-mode transient state, clear, roundtrips with dests/paths/enabled.
- Not covered: live TownInterfaceEntity integration (callback to setChanged, full BE save/load), packet-driven client sync, spawning consumption of getEnabledPlatforms (requires Level), visualization.
- All tests use only BlockPos + CompoundTag + UUID (no McBootstrap required). Full suite green.

## Open questions
- Direct mutation of a Platform obtained via `getPlatform(id)` (calling its setStartPos etc.) would bypass the manager's notifyChanged. All current callers use the manager wrapper methods, but the exposure of the live object is a foot-gun. A future hardening could return an immutable view or force all writes through the manager.
- createLegacyPlatform logs at INFO level (not debug-controlled) and only on the first successful migration. In worlds that have been loaded many times this is harmless; the guard prevents repeats.
- Client list is never defensively copied in the same way on read paths inside update (it is replaced wholesale). Since update is only called from desync paths this is fine, but getPlatforms(true) still wraps it in new ArrayList for callers.
- The "Platform N" naming uses the size at add time; if platforms are removed later the numbers are not renumbered. This matches current behavior (pinned by test if written); whether players expect dense numbering is a UX question.
- No persistence of creation-mode state across logins/saves (by design — it's a transient UI session).

## Related
- [[Town/Platforms/Platform Data Model]] (T-015 — the individual Platform objects managed here)
- [[Town/Town Overview]]
- Used by: TownInterfaceEntity (primary owner + UI delegation + spawning), NBTDataHelper (save/load wrapper + legacy path data), VisitorProcessingHelper (receives manager for spawn decisions)
- Callers of getEnabledPlatforms: tourist spawning loops and vehicle mounting logic
- The 10-cap is mentioned in the platform UI and in the data model note
