---
tags:
  - detail
  - town
---
# Client Data Synchronization

**Breadcrumb**: Town > Data Synchronization > Client Data Synchronization
**TL;DR**: ClientSyncHelper maintains client-side mirrors of a town's resources, storage tiers (communal/escrow/personal/wanted), and visit history by serializing to/from network tags using stable registry keys; it also resolves town UUIDs to human names for visit lists via server pre-resolution, local cache, or short-ID fallback so UIs stay responsive and never show raw IDs.

## What it does
The client never has direct access to live Town objects (those live only on the server). When the Town Interface menu or tabs need to show current resources, escrowed items, wanted resources for trading, or the visit history log, the server pushes compact snapshots in update tags. ClientSyncHelper is the single place that writes those snapshots on the server and reads them back on the client into simple in-memory maps and lists that the UI components query.

It also solves the "what is the name of the town that sent this tourist?" problem for visit history without extra network round-trips: the server resolves the name once when building the tag and includes it; the client caches it.

## How it works (process view)
- On the server (inside TownInterfaceEntity), sync*ForClient methods are called to populate a CompoundTag with sub-tags ("clientResources", "clientCommunalStorage", "clientEscrowedResources", "clientWantedResources", "visitHistory").
- Items are keyed by their registry ResourceLocation string (e.g. "minecraft:iron_ingot") so the mapping survives world saves, mod reloads, and client/server version skew as long as the item still exists.
- For visits, the server also calls resolveTownName (with log flag) to put a "townName" field next to the "townId" so the client can display a nice name immediately.
- On the client, load*FromTag methods parse the incoming tags using the local registry, populate the private maps/lists, and for visits also populate the townNameCache.
- UI code (ResourcesTab, VisitorHistoryManager, etc.) reads the caches via getClient* and getVisitHistory (which branches on level.isClientSide()).
- Name resolution for display: getTownNameFromId / resolveTownName prefers a real server lookup when you have a ServerLevel, otherwise falls back to the client name cache, otherwise emits a short "Town-1234abcd" string derived from the UUID so something is always shown.
- Static notify* methods are used by the payment buffer sync path to push live updates to any players who have the Payment Board open.

**Worked example**: A tourist batch from "Ironhill" (UUID 550e8400-e29b-41d4-a716-446655440000) arrives. Server builds visitHistory tag entry with timestamp, townId, townName:"Ironhill", count:3, pos. Client receives the tag, loadVisitHistoryFromTag stores the record in clientVisitHistory and puts the name in townNameCache under that UUID. Later the visit history UI calls townInterface.getTownNameFromId(id) which delegates to resolveTownName with a client level (or null); cache hit returns "Ironhill" instantly. If the cache entry had been missing, it would show "Town-550e8400" instead of a raw UUID or "Unknown".

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ClientSyncHelper` | `common/src/main/java/com/quackers29/businesscraft/town/data/ClientSyncHelper.java` | Central holder of client caches and the sync/load logic for town data over the wire |
| `syncResourcesForClient(CompoundTag, ITownDataProvider)` | same | Writes "clientResources" + "clientCommunalStorage" subtags using registry key strings as keys |
| `loadResourcesFromTag(CompoundTag)` / `load*Escrowed*` / `loadWanted*` | same | Parse those subtags back into the four client maps, skipping AIR and bad keys |
| `syncVisitHistoryForClient(...)` / `loadVisitHistoryFromTag(...)` | same | Serializes the visit list (with pre-resolved names) and rebuilds both the list and the name cache on load |
| `resolveTownName(UUID, boolean log, Level)` + overloads + `getTownNameFromId` | same | 3-way decision: null id → "Unknown"; server context → TownManager lookup or "Unknown Town"; client → cache hit or "Town-" + short(8) |
| `updateClientResourcesFromTown(Town)` | same | Bulk copy from a live Town into the client resource + communal maps (used on initial sync) |
| `updateClientPersonalStorage(UUID, Map)` / `getClientPersonalStorage` | same | Per-player personal storage mirror used by some trade UIs |
| `getClient*` (resources, communal, escrowed, wanted, visitHistory) | same | Return live maps or unmodifiable views for UI consumption |
| `getVisitHistory(Level, ITownDataProvider)` | same | Chooses the client cached list (when client-side) or falls back to the provider |
| `clearAll()` / `getCacheStats()` | same | Test/debug helpers |
| `notifyBuffer*Change` (static) | same | Fire packet broadcasts for live hopper/buffer UI updates (side-effect only) |

## Rules & formulas (exact)
**Name resolution (resolveTownName):**
```java
if (townId == null) return "Unknown";
if (level != null && !level.isClientSide()) {
    // server path
    if (ServerLevel) { Town t = TownManager.get(...).getTown(townId); if (t!=null) return t.getName(); }
    return "Unknown Town";
}
// client path
if (townNameCache contains townId) {
    String cached = cache.get(townId);
    if (cached != null && !cached.isEmpty()) return cached;
}
return "Town-" + townId.toString().substring(0, 8);
```
- Server path never falls back to the short "Town-xxx" form — it always says "Unknown Town" on miss.
- Client path only trusts non-empty cached names; empty string in cache still produces the short form.
- getTownNameFromId(UUID, Level) simply calls resolveTownName(townId, level!=null && !client, level).
- The two-arg resolveTownName(townId, level) calls the 3-arg with log=false.

**Resource / storage tag format:**
- Each map (resources, communal, escrowed, wanted) becomes a CompoundTag child whose keys are `item.getRegistryKey().toString()` (or via PlatformAccess.getRegistry().getItemKey) and values are the long counts.
- On load: `new ResourceLocation(key)` → `registry.getItem(loc)` → if (item != null && item != AIR) put(count).
- If key cannot be parsed as ResourceLocation or resolves to AIR/null, the entry is skipped (with a warn log for the main resources case).
- syncWantedResourcesForClient and syncEscrowed always emit the tag (even when empty) "to ensure client clears old data".
- syncResourcesForClient / communal do not emit when empty in the examples, but the load path handles absence by leaving the map untouched (or cleared only when the "clientResources" key is present).

**Visit history tag shape (per entry):**
- timestamp (long), count (int), townId (UUID, required in modern), townName (string, pre-resolved by server for client), optional "pos" subtag (x/y/z).
- Legacy entries without "townId" are warned and skipped on load.
- On load the townName (if present) is written into townNameCache immediately, and the record is added to clientVisitHistory.

**Cache update rules:**
- updateClientPersonalStorage(playerId, items): clears that player's submap then putAll (full replace).
- updateClientResourcesFromTown(town): clears clientResources + clientCommunalStorage then copies town's getAllResources() / getAllCommunalStorageItems().
- clearAll() wipes all five maps + the name cache.
- All getters that return maps for "personal/escrow/wanted/visit" return unmodifiable wrappers (resources/communal return the live mutable maps — historical).

**getVisitHistory branching:**
- if (level != null && level.isClientSide()) return unmod view of clientVisitHistory
- else if provider != null return provider.getVisitHistory()
- else empty list

## Edge cases & behaviors
- null townId anywhere in resolve → "Unknown" (no exception).
- Server lookup miss → "Unknown Town" (distinct from client miss "Town-8chars").
- Cache hit with empty string → treated as miss, produces short form.
- Resource sync key that resolves to AIR or fails registry lookup → dropped (no crash).
- loadVisitHistoryFromTag on a tag without "visitHistory" key → no-op (list not cleared).
- Wanted/escrow syncs deliberately send empty tags so the client side can clear stale "I used to want X" state.
- The four resource load methods contain near-identical key→item parsing loops (duplication).
- notify* methods are no-ops if level or townId null; they always broadcast to every player in level.players() (the client filters by open UI).
- getCacheStats and clearAll are pure on the maps (no registry involvement).
- resolveTownName with a non-ServerLevel that is still !isClientSide() still takes the server branch and returns "Unknown Town" (TownManager.get expects ServerLevel).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/ClientSyncHelperTest.java`
- Covered (pure/client paths): resolveTownName null/edge, client cache hit, client miss → short form, getTownNameFromId delegation, update/get/clear personal + resources-from-town, getCacheStats, clearAll, loadVisitHistory populates both list and name cache, getVisitHistory client vs provider branch, basic resource tag roundtrip (sync→load) for one item using registry double.
- Not covered (requires live server world / packets): full server resolveTownName path with real TownManager + populated towns, the static notify* packet broadcasts, actual TownInterfaceEntity wiring, visit history with pos, multi-item communal + wanted + escrow tag fidelity under real registry, error paths that only log.
- Uses McBootstrap + local TestRegistryHelper (for getItem / getItemKey roundtrips on a couple of real Items) + CompoundTag/BlockPos/UUID (allowed data types). No Level construction for the client-path tests (pass null level).

## Open questions
- Server-side resolveTownName path is effectively untestable in this harness without a real ServerLevel + TownManager population (would be NEEDS-MC if we wanted 100% of the method).
- Significant code duplication across the four *Resources load methods and the two syncResource* methods; a small internal (de)serializer for "map of Item<->long using registry keys" would remove it.
- The live mutable maps returned by getClientResources / getClientCommunalStorage mean callers can accidentally mutate the cache (current UI code only reads).
- "townName" is sent redundantly with every visit record even though the nameCache is also populated; this is intentional for resilience if cache is cleared between load and display.
- Short-ID fallback "Town-12345678" leaks 8 hex chars of the UUID; acceptable for UX but not private.

## Related
- [[Town/Data Synchronization/Container Data Registration]]
- [[Town/Town Overview]]
- [[Town/Visits/Visit Buffer]]
- [[Town/Payment Board/Reward Claims]]
- [[Economy/Tourist Payments/Distance Payment Calculation]] (visit history is how payments are attributed back to origins)
