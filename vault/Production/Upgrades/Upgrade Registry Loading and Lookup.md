---
tags:
  - detail
  - production
---
# Upgrade Registry Loading and Lookup

**Breadcrumb**: Production > Upgrades > Upgrade Registry Loading and Lookup
**TL;DR**: Loads all upgrade node definitions (research minutes, resource costs, effects, prereqs, repeatability) from `config/businesscraft/upgrades.csv` at startup or reload; creates a minimal default file with two starter nodes if missing; exposes lookup by ID and enumeration of all nodes to drive research, upgrade application, and UI.

## What it does
The upgrade system is defined entirely by data files so that modpack authors and server admins can add or tweak progression without code changes. This registry is the single loader and in-memory catalog: on first access it ensures the CSV exists (writing two foundational nodes if needed), parses every valid row into an UpgradeNode object (with parsed effects and costs), and makes them available by ID for the town's research AI, upgrade component, and status displays. Without this, no upgrades can be researched or applied.

## How it works (process view)
- ConfigLoader (or explicit call) invokes `UpgradeRegistry.load()`.
- The platform tells the registry where the config directory lives; it looks for `businesscraft/upgrades.csv`.
- If the file is absent, a default is written containing the header plus "basic_settlement" (the root node that gives initial pop/tourist/storage/happiness) and "farming_basic" (a simple prereq-gated food producer).
- Each subsequent line is split on commas (9+ fields expected). The 5th field (0-based index 4) is a semicolon-separated prereq list; minutes/costs/effects are parsed by the shared DataParser helpers.
- Successfully parsed nodes are stored under their ID. Lines that are too short, have unparsable numbers, or throw in the effect/cost parser are logged and skipped.
- Callers then use `get("basic_settlement")` to fetch a single definition or `getAll()` to iterate for UI lists and AI scoring.
- **Worked example (default load)**: after a clean load, `get("basic_settlement")` returns a node with 0 research minutes, empty required items, and six effects (pop_cap:10, tourist_cap:2, storage_cap_all:200, happiness:50, plus two population behaviors). `get("farming_basic")` has 1 minute research, a wood:10 cost, and the "basic_farming" effect string. `getAll()` returns both plus any additional rows from a custom CSV.
- The REQ_FILE constant ("upgrade_requirements.csv") is declared but never consulted by the loader.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `UpgradeRegistry.load()` | `common/src/main/java/com/quackers29/businesscraft/production/UpgradeRegistry.java` (lines 24-32) | Public entry point; clears prior nodes, resolves the CSV path via Platform, delegates to loadNodes |
| `UpgradeRegistry.loadNodes(File)` | same (34-96) | Core: ensure file or write default, read lines, skip header, split/parse/construct UpgradeNode + setRequirements, put in NODES map; tolerant of bad rows |
| `UpgradeRegistry.createDefaultUpgrades(File)` | same (98-109) | Writes the two-node starter CSV (basic_settlement + farming_basic) when no file exists |
| `UpgradeRegistry.get(String)` / `getAll()` | same (111-118) | Simple map accessors; getAll exposes the live value collection |
| `UpgradeNode` + `DataParser.parseEffects` / `parseResources` | production/UpgradeNode.java + data/parsers/DataParser.java | The objects that receive the parsed minutes/costs/effects; prereqs and repeat info are stored directly on the node |
| `PlatformAccess.platform.getConfigDirectory()` | api/PlatformAccess.java + platform impls | Supplies the root under which `businesscraft/upgrades.csv` lives |

## Rules & formulas (exact)
Load always begins by clearing the static NODES map, guaranteeing a fresh set after each load() call (supports hot-reload scenarios).

```java
NODES.clear();
Path configDir = PlatformAccess.platform.getConfigDirectory();
File upgradesFile = configDir.resolve("businesscraft").resolve(UPGRADES_FILE).toFile();
loadNodes(upgradesFile);
```

In loadNodes:
- `if (!file.exists()) createDefaultUpgrades(file);`
- Reader loop: firstLine skipped (the CSV header), then for remaining:
  - `String[] parts = line.split(","); if (parts.length >= 9)`
  - id = parts[0].trim(), category=1, name=2, repeat=3, prereqsRaw=4, desc=5, minutesRaw=6, costsRaw=7, effectsRaw=8
  - prereqs: split prereqsRaw on ";" , trim, skip empties
  - effects = DataParser.parseEffects(effectsRaw)
  - minutes: try { Float.parseFloat(minutesRaw) } catch { 0 + warn }
  - costs = DataParser.parseResources(costsRaw)
  - node = new UpgradeNode(id, category, name, repeat, prereqs, desc, effects)
  - node.setRequirements(minutes, costs)
  - NODES.put(id, node)
- Any exception inside the per-line try: LOGGER.error("Error parsing upgrade line: {}", line, e) — line is dropped.
- createDefaultUpgrades always emits exactly the header + two well-formed rows (basic_settlement with minutes "0" and empty costs, farming_basic with "1,wood:10,basic_farming").

get and getAll are direct:
- `return NODES.get(id);`
- `return NODES.values();` (live view, not a defensive copy)

REQ_FILE constant is initialized but load/loadNodes never reference it.

## Edge cases & behaviors
- No CSV present → default file is created on disk and two nodes become available.
- CSV exists but empty or only header → NODES remains empty after load (no defaults written when file present).
- Line with <9 comma parts (after split) → silently skipped.
- Non-numeric research_minutes → warn logged, minutes treated as 0 for that node.
- Malformed effects or required_items string → DataParser returns empty list (or partial); node is still registered with what parsed.
- Duplicate IDs in CSV → last one wins (standard Map.put).
- Prereq field empty or ";;" → empty prereq list (node has no prerequisites).
- Whitespace around fields → trimmed for id/name/etc; split parts keep internal spaces only if not trimmed.
- load() called repeatedly → previous nodes are discarded each time; side-effect is that any in-memory UpgradeNode instances held by callers become stale (they are not refreshed).
- get(nonexistent) → null (callers must null-check, e.g. TownUpgradeComponent does).
- getAll() on empty registry → empty collection.
- The unused REQ_FILE constant means any separate requirements.csv is ignored by current implementation.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/production/UpgradeRegistryTest.java`
- Covered: default creation when file missing (with parent dir present), header+two-node content written, basic parse of minutes/costs/effects/prereqs, get returns correct node or null, getAll size/membership/iteration, bad/short lines skipped, bad minutes default to 0, custom CSV via temp dir, snapshot/restore isolation of the private NODES map, prereq ; split and empty effects/costs handling (7 tests, all hand-verified against the default CSV written by createDefaultUpgrades).
- Not covered: actual UpgradeNode effect application or research flow (those are in TownUpgradeComponent / AI — integration level); behavior when PlatformAccess.platform is uninitialized (tests always install a double first); file permission / IO errors (would surface as logged failures, caught inside createDefault).

## Open questions
- REQ_FILE ("upgrade_requirements.csv") is declared but completely unused — dead code or planned split of prereqs/costs into a second file that was never wired up?
- No defensive copy on getAll(); a caller that mutates the returned Collection would corrupt the registry (current callers only iterate).
- Default nodes are minimal; the real depth of the upgrade tree lives in player-supplied or modpack CSVs — the test only validates the loader mechanics, not any particular content.
- Error handling per line is "log + drop"; a future version might want to collect problems for admin feedback instead of silent omission.
- load() depends on a global PlatformAccess singleton; tests must carefully save/restore it (and the NODES map) to avoid cross-test pollution.

## Related
- [[Production/Production Overview]]
- [[Production/Upgrades/Effect Value Calculation]] (T-016 — the scaling math applied to nodes returned by this registry)
- [[Production/Recipes/Estimated Effort Calculation]] (T-025 — sibling data-driven registry using the same config-dir + CSV + DataParser pattern)
- [[Town/Production/Recipe Execution and Dynamic Evaluation]] (T-029 — production side of the same data pipeline)
- [[Config/Configuration Loading]] (T-014 — the caller that usually triggers UpgradeRegistry.load)
