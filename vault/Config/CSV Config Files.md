---
tags:
  - detail
  - config
  - production
---
# CSV Config Files

**Breadcrumb**: Config > CSV Config Files
**TL;DR**: The four editable game-data files (`items.csv`, `productions.csv`, `upgrades.csv`, `biomes.csv`) are copied from the mod jar into `config/businesscraft/` the first time they are missing, read with one shared row format, and checked after every load for bad references. Problems are logged as warnings and never change how the game behaves.

## What it does
Server owners and pack makers tune the economy by editing these files. A clean install gets exactly the files shipped in the repo (`assets/businesscraft/config/`), so the design in the repo is the design a new server runs. Existing files are never overwritten. A typo no longer fails silently: each bad reference is reported once in the log, with the file and the line or entry it came from.

## How it works (process view)
- On load each registry asks for its file under `config/businesscraft/`. If the file is missing, the bundled copy is installed first. If the jar copy is somehow missing too, an error is logged and the registry loads empty.
- Every file uses the same row rules: first line is a header, blank lines and lines starting with `#` are skipped, columns are split on commas that are not inside double quotes, empty trailing columns are kept, each column is trimmed and one pair of enclosing quotes is removed. A comma inside a quoted description therefore does not shift later columns, and a node with an empty effects column is kept.
- Rows with too few columns, a missing id, a duplicate id, a cycle time of 0 or less, an unreadable number, or an input or output that is not in `resource:amount` form are reported. The row is still kept where it was kept before (for example a duplicate id replaces the earlier row).
- After all four registries load, `ConfigValidator` checks the references between them and adds one warning per problem:
  - input, output and cost ids that are not in `items.csv` (outputs may also be `pop`, `wu`, `tourist` or a known stat; costs may also be requirement-only stats such as `tourism`)
  - unknown tokens in an amount expression such as `1*popp` (these evaluate as 0 at runtime)
  - condition targets and values that cannot be evaluated (`crowding` is a known target: current density divided by the biome kit's starting density)
  - upgrade effect targets that are not a recipe, a node, or a known stat (`pop_cap`, `tourist_cap`, `wu_cap`, `storage_cap_all`, `storage_cap_<item>`, `border`, `happiness`, `research`)
  - unknown prerequisites and unknown biome starting nodes or starting values
  - a biome variant with weight 0 or less
- `ConfigWarnings` keeps the list (cleared at the start of each reload) so tests can assert a config is clean.

**Worked example**: a pack maker writes `food:1*popp` in the outputs of a recipe `taxes`. On the next load the log shows `Config problem: productions.csv [taxes]: output food amount '1*popp' has unknown token 'popp' (evaluates as 0)`. Nothing else changes; the recipe still loads and still produces 0.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `CsvConfig.ensureInstalled(configDir, fileName)` | `common/src/main/java/com/quackers29/businesscraft/config/CsvConfig.java` | Copies `/assets/businesscraft/config/<fileName>` to `<configDir>/businesscraft/` when absent; never overwrites |
| `CsvConfig.splitLine` / `readRows` | same | Shared row rules; `readRows` returns `Row(lineNumber, columns)` |
| `ConfigWarnings` | `.../config/ConfigWarnings.java` | Thread-safe list of problems; `warn(file, line, msg)` and `warn(file, entry, msg)` log at WARN |
| `ConfigValidator.validate()` | `.../config/ConfigValidator.java` | Cross-reference pass over the loaded registries |
| `ConfigLoader.loadRegistries()` | `.../config/ConfigLoader.java` | Clears warnings, loads items, productions, upgrades, biomes, then validates |
| `BiomeRegistry.getAllKits()` | `.../world/BiomeRegistry.java` | Lets the validator see every kit once |

## Column layouts
- `items.csv`: `item_id,display_name,mc_item_id[,base_price]`
- `productions.csv`: `prod_id,display_name,base_cycle_time_minutes,inputs,outputs` (the inputs column mixes resources, conditions and the global tokens `min`, `excess`, `N%`)
- `upgrades.csv`: `node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects`. The `repeat` field is `maxLevel:costMultiplier:benefitMultiplier`. `maxLevel` is a number (Border Expansion is `5:2:2`) or `infinite` (Pop Cap Upgrade). Empty means once.
- `biomes.csv`: `biome_ids,variant_id,variant_name,description,weight,starting_nodes,starting_values` (starting nodes may carry `:level`)

## Edge cases & behaviors
- A fresh install no longer gets the old hardcoded fallbacks (4 recipes, 2 nodes, `basic_taxes`, `basic_farming`). It gets the bundled files, including the `tourism`, `taxes` and `work_unit_production` recipes.
- Upgrading from an older build keeps whatever files already exist.
- The validator is read only. It does not drop rows or change runtime behaviour.
- Warning text for registry-level problems uses `file line N`; validator problems use `file [id]` because the row number is not kept after parsing.
- `items.csv` `base_price` is optional; the bundled file has none, so every resource has base value 1.0 (nothing reads it today).

## Test coverage
- `config/CsvConfigTest`: splitter rules, comment and blank lines, install and no-overwrite.
- `config/ConfigValidatorTest`: one test per warning type, plus a clean config and "validation changes nothing".
- `config/BundledConfigTest`: loads the four bundled files from a fresh config folder and asserts zero warnings (it fails if the bundled biomes name a node that does not exist), plus the toml section layout.
- `production/ProductionRegistryLoadTest`, `production/UpgradeRegistryTest`: parsing and install behaviour.

## Related
- [[Config/Configuration Loading]]
- [[Config/Data Parsing]]
- [[Production/Upgrades/Upgrade Registry Loading and Lookup]]
- [[Production/Production Overview]]
