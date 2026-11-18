---
tags:
  - detail
  - production
---
# Upgrade Registry Loading and Lookup

**Breadcrumb**: Production > Upgrades > Upgrade Registry Loading and Lookup
**TL;DR**: Loads all upgrade node definitions (research minutes, resource costs, effects, prereqs, repeatability) from `config/businesscraft/upgrades.csv` at startup or reload; installs the bundled `upgrades.csv` from the jar if the file is missing; exposes lookup by ID and enumeration of all nodes to drive research, upgrade application, and UI.

## What it does
The upgrade system is defined entirely by data files so that modpack authors and server admins can add or tweak progression without code changes. This registry is the single loader and in-memory catalog: on first access it ensures the CSV exists (copying the bundled file if needed), parses every valid row into an UpgradeNode object (with parsed effects and costs), and makes them available by ID for the town's research AI, upgrade component, and status displays. Without this, no upgrades can be researched or applied.

## How it works (process view)
- ConfigLoader (or explicit call) invokes `UpgradeRegistry.load()`.
- The platform tells the registry where the config directory lives; it looks for `businesscraft/upgrades.csv`.
- If the file is absent, the bundled `upgrades.csv` (12 nodes, the design shipped in the repo) is copied into place. An existing file is never overwritten. See [[Config/CSV Config Files]].
- Rows are read with the shared CSV rules: header skipped, blank and `#` lines skipped, commas inside double quotes do not split, empty trailing columns are kept (so a node with no effects is still loaded). The 5th column is a semicolon-separated prereq list; minutes, costs and effects are parsed by the shared DataParser helpers.
- Successfully parsed nodes are stored under their ID. Rows that are too short, have no id, or throw in the parser are reported through `ConfigWarnings` and skipped; unreadable minutes are reported and treated as 0; duplicate ids are reported and the later row wins.
- After all registries load, `ConfigValidator` warns about unknown prerequisites, effect targets and cost ids.
- Callers then use `get("basic_settlement")` to fetch a single definition or `getAll()` to iterate for UI lists and AI scoring.
- **Worked example (fresh install)**: after a clean load, `get("basic_settlement")` returns a node with 0 research minutes, empty required items, and effects pop_cap:100, tourist_cap:5, storage_cap_all:2000, happiness:50, the flags tourism, population_maintenance, population_growth and taxes, and the chained node `farming_basic`. `get("farming_basic")` is a prereq-gated food producer costing food:100. Toggle honesty keeps unlocking `basic_settlement` when research is off so tourism still works - see [[Config/Subsystem Toggle Honesty]].

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `UpgradeRegistry.load()` | `common/src/main/java/com/quackers29/businesscraft/production/UpgradeRegistry.java` | Clears prior nodes, installs the bundled CSV when missing, parses every row into a node |
| `UpgradeRegistry.get(String)` / `getAll()` | same | Simple map accessors; getAll exposes the live value collection |
| `CsvConfig.ensureInstalled` / `readRows` | `config/CsvConfig.java` | Shared install and row splitting for all four CSVs |
| `ConfigWarnings` / `ConfigValidator` | `config/` | Row-level and cross-reference problems, logged once each |
| `UpgradeNode` + `DataParser.parseEffects` / `parseResources` | production/UpgradeNode.java + data/parsers/DataParser.java | The objects that receive the parsed minutes/costs/effects; prereqs and repeat info are stored directly on the node |
| `PlatformAccess.platform.getConfigDirectory()` | api/PlatformAccess.java + platform impls | Supplies the root under which `businesscraft/upgrades.csv` lives |

## Rules & formulas (exact)
Load always begins by clearing the static NODES map, guaranteeing a fresh set after each load() call (supports hot-reload scenarios).

```java
NODES.clear();
File file = CsvConfig.ensureInstalled(PlatformAccess.platform.getConfigDirectory(), "upgrades.csv");
for (CsvConfig.Row row : CsvConfig.readRows(file)) { ... }
```

Per row (`columns = row.columns()`, already trimmed and unquoted):
- fewer than 9 columns: warning, row skipped
- id = 0, category = 1, name = 2, repeat = 3, prereqs = 4, desc = 5, minutes = 6, costs = 7, effects = 8
- empty id: warning, row skipped; duplicate id: warning, later row replaces
- prereqs: split on ";", trim, skip empties
- effects = DataParser.parseEffects(effectsRaw); costs = DataParser.parseResources(costsRaw) (a cost not in `resource:amount` form is reported)
- minutes: Float.parseFloat, on failure warning and 0
- `new UpgradeNode(...)`, `setRequirements(minutes, costs)`, `NODES.put(id, node)`

get and getAll are direct:
- `return NODES.get(id);`
- `return NODES.values();` (live view, not a defensive copy)

## Edge cases & behaviors
- No CSV present: the bundled file is copied and its nodes become available.
- CSV exists but is empty or only a header: NODES stays empty (nothing is written over it).
- A row with an empty effects column is kept with an empty effect list.
- A quoted description may contain commas.
- Non-numeric research_minutes: warning, minutes treated as 0 for that node.
- Malformed effects: DataParser skips the bad entry and logs; the node is still registered with what parsed.
- Duplicate IDs: last one wins, with a warning.
- Prereq field empty or ";;": empty prereq list.
- load() called repeatedly: previous nodes are discarded each time; UpgradeNode instances held by callers become stale.
- get(nonexistent) returns null (callers must null-check, e.g. TownUpgradeComponent does).
- `TownUpgradeComponent.unlockNode` still accepts an id that is not in the registry; the validator is what reports bad ids in the CSVs.

## Test coverage
- `production/UpgradeRegistryTest`: bundled install on a fresh config folder, existing file never overwritten, bundled prereqs and effects, custom rows (good, short, bad minutes, empty id), reload replaces nodes, empty effects column kept, quoted description with a comma, comment lines, repeat column parsing.
- `config/BundledConfigTest`: the bundled files load with zero validation warnings.
- `town/components/TownUpgradeModifiersTest`: modifiers produced from the bundled nodes, research flow and NBT (see [[Production/Upgrades/Upgrade Cost and Research Time Scaling]]).
- Not covered: file permission / IO errors (logged).

## Open questions
- No defensive copy on getAll(); a caller that mutates the returned Collection would corrupt the registry (current callers only iterate).
- Registry reload runs on the config watcher thread while the server thread reads the maps (review item A6, not part of this change).

## Related
- [[Config/CSV Config Files]]
- [[Production/Production Overview]]
- [[Production/Upgrades/Effect Value Calculation]] (T-016 — the scaling math applied to nodes returned by this registry)
- [[Production/Recipes/Estimated Effort Calculation]] (T-025 — sibling data-driven registry using the same config-dir + CSV + DataParser pattern)
- [[Town/Production/Recipe Execution and Dynamic Evaluation]] (T-029 — production side of the same data pipeline)
- [[Config/Configuration Loading]] (T-014 — the caller that usually triggers UpgradeRegistry.load)
- [[Config/Subsystem Toggle Honesty]] (T-041 — biome kits keep unlocking `basic_settlement` when research is off)
- [[Tourists/Tourists Overview]] (Option C depends on the `tourism` effect from that node)
