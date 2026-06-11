---
tags:
  - detail
  - town
---
# Container Data Registration

**Breadcrumb**: Town > Data Synchronization > Container Data Registration
**TL;DR**: A named-field adapter for Minecraft's ContainerData sync protocol: code registers live getters (and optional setters) by friendly string names; the helper assigns stable indices, enforces read-only rules, tracks dirty state so values refresh on demand, and safely handles the int-only array protocol that vanilla menus use.

## What it does
The Town Interface block's menu needs to show and occasionally edit live values from the town (population, tourist counts, spawn toggle, search radius) without hard-coding slot numbers that break when fields are added. This helper lets developers register each piece of data by a readable name backed by a supplier (and setter when editable). Minecraft's menu sync system only understands a flat array of ints accessed by index; the helper hides that, provides name-based lookup for the rest of the code, and ensures client and server see consistent values with minimal refreshes.

## How it works (process view)
- During construction (usually in the block entity's field initializer), a Builder is used to register fields in order: `addReadOnlyField("population", this::getPop, "Current town population")`, `addField("spawn_enabled", getter, setter, "...")`, etc.
- Each registration appends to an ordered list (the index order) and a name map. Duplicate names are rejected at registration time with an exception.
- When the menu asks `get(index)`, the helper looks up the field at that position and returns its current int value (re-fetched from the live supplier if the field has one).
- For two-way fields like spawn toggle or radius, `set(index, value)` from the client is routed to the registered setter lambda, which can clamp or forward to town state.
- `markDirty("name")` or `markAllDirty()` forces the next read to treat the cached value as stale.
- Read-only fields have no setter; attempts to set them log a warning and do nothing.
- The same helper object is returned from `getContainerData()` on the block entity so the vanilla menu sync machinery can poll it.

**Worked example**: The TownInterfaceEntity registers six fields at startup:
1. population (read-only)
2. spawn_enabled (read/write 0/1)
3. can_spawn (read-only)
4. search_radius (read/write, setter clamps 1..100)
5. tourist_count (read-only)
6. max_tourists (read-only)

A client menu doing `containerData.get(3)` receives the current search radius. Changing it in the UI calls `set(3, 25)`, which runs the entity's setter (clamps + writes to town). Calling `getValue("search_radius")` from server code also works by name.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ContainerDataHelper` | `common/src/main/java/com/quackers29/businesscraft/town/data/ContainerDataHelper.java` | Core adapter implementing `net.minecraft.world.inventory.ContainerData`; owns the ordered list + name map of fields |
| `ContainerDataHelper.registerField(name, getter, setter, desc)` | same | Adds a read-write or read-only field; throws `IllegalArgumentException` on duplicate name; returns the assigned index |
| `ContainerDataHelper.registerReadOnlyField(...)` | same | Convenience that passes null setter and appends " (read-only)" to description |
| `ContainerDataHelper.getValue(name)` / `setValue(name, v)` | same | Name-based access used by entity code (e.g. `getBreadCount`, `getPopulation`); unknown name → 0 + warn |
| `ContainerDataHelper.markDirty(name)` / `markAllDirty()` | same | Forces re-evaluation from suppliers on next access |
| `ContainerDataHelper.Builder` + `builder(contextName)` | same | Fluent registration API used in the entity: `ContainerDataHelper.builder("TownBlock").addReadOnlyField(...)...build()` |
| `ContainerDataHelper.DataField` (inner) | same | Holds one named supplier/consumer + cached int + dirty flag; `getValue()` re-calls getter when dirty or when a getter exists |
| `get(int)` / `set(int, v)` / `getCount()` | same (ContainerData impl) | Vanilla menu protocol surface; bounds-checked with warnings on bad indices |

## Rules & formulas (exact)
Registration order determines the int indices exposed to Minecraft (0-based, stable for the lifetime of that helper instance).

- Duplicate name: `if (fieldsByName.containsKey(name)) throw new IllegalArgumentException("Field '" + name + "' is already registered");`
- Index lookup: `if (index >= 0 && index < fieldsByIndex.size())` else warn + return 0 (get) or no-op (set).
- DataField.getValue():
  ```java
  if (isDirty || getter != null) {
      int newValue = getter != null ? getter.getAsInt() : cachedValue;
      if (newValue != cachedValue) cachedValue = newValue;
      isDirty = false;
  }
  return cachedValue;
  ```
  Presence of a getter means "always consult live source" (the `|| getter != null` makes dirty mostly a no-op for normal fields).
- DataField.setValue(v): only if setter != null: `setter.accept(value); cachedValue = value; isDirty = false;` else warn.
- Name-based get/set on helper: delegate to the DataField or `LOGGER.warn(...)` + 0/ignore.
- getCount(): `return fieldsByIndex.size();`
- getFieldNames(): unmodifiable view of the LinkedHashMap keySet (insertion order).
- getDebugInfo(): multi-line dump of index, name, current value (via getValue), description, and [READ-ONLY] marker.

Context name is stored only for log messages; it does not affect behavior.

## Edge cases & behaviors
- Registering the same name twice throws immediately (fail-fast at construction).
- Getting/setting an unknown name by string: logs a warning each time, returns 0 or does nothing.
- Setting a read-only field: logs "Attempted to set value on read-only field 'xxx'" and ignores.
- Out-of-range index on get/set: logs "Attempted to get/set invalid index X in context 'Y' (max: Z)" and returns 0 / ignores.
- A field with only a setter (no getter) will serve the last set value from its cache until markDirty; in practice all registrations supply getters.
- markDirty on unknown name: silently does nothing (no warning in current code).
- getDebugInfo() and getFieldNames() reflect current registration; values inside debug info are live (they call getValue()).
- The LinkedHashMap + ArrayList combo guarantees that name lookup and index lookup are consistent and that registration order = sync order.
- No automatic "has changed" detection for the whole container — callers must call markDirty or markAllDirty when town state that feeds a supplier mutates (see syncTownData / markDirty("search_radius") in the entity).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/data/ContainerDataHelperTest.java`
- Covered: registration order and index assignment, duplicate-name rejection, name and index get/set roundtrips, read-only enforcement, unknown name handling, markDirty + markAllDirty forcing supplier re-reads, builder fluency, getCount, getFieldNames, getDebugInfo output, DataField caching behavior, bounds and error-path warnings (via behavior). 22 tests, all hand-exercised via live AtomicInteger suppliers and direct index/name calls.
- Not covered: actual menu opening / vanilla sync packet flow (would require full screen + player), concurrent modification (not a concern in single-threaded MC server tick), very large numbers of fields.

## Open questions
- The dirty flag is effectively redundant for any field that supplies a getter (the `getter != null` clause always re-evaluates). This may be legacy from a design that supported pure cached fields; harmless but slightly confusing.
- No way to unregister a field after build — the helper is effectively immutable after the builder finishes. Fine for the current use case (one helper per block entity).
- Warning logs on bad access are at WARN level and always fire; in a hot path this could be noisy, but registration mistakes are construction-time and runtime misuse is rare.
- The contextName is only for diagnostics; it would be useful to expose it publicly if more debug tooling is added.

## Related
- [[Town/Town Overview]]
- [[Town/Platforms/Platform Data Model]] (T-015) and [[Town/Platforms/Platform Management]] (T-023) — other data that flows through the same block entity
- [[Town/Storage/Slot-Based Storage]] (T-013) — sibling helper extracted from the same TownInterfaceEntity for buffer/hopper concerns
