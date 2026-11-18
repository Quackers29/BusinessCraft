# Production and Upgrades: overhaul review

Read-only review of `origin/main` (0.9.0-alpha line). Nothing was run. Everything below comes from reading the code and the bundled configs.
It builds on the earlier incremental review (A1-F1 fix list). That list stands. This doc asks a different question: **is the whole approach right, and what would a bigger redesign look like?**

Paths are under `common/src/main/java/com/quackers29/businesscraft/` unless noted.

---

## 1. Short answer

- **The CSV files are not silly.** The data is tiny (9 recipes, 12 upgrade nodes, 6 items, ~10 biome kits), hand-editable, diffable and hot-reloadable. Swapping the file format would not fix anything that is actually wrong.
- **What is wrong is what we put in the cells.** Each cell is a small hidden language (`wu:20;wood:25;wood:>min;money:1`, `farming*0.5`, `5:1.1:1.2`, `pop:>80%pop_cap`). It is read by several hand-written parsers (two CSV splitters plus three cell parsers), checked by nobody, and it fails silently.
- **"Data-driven" is already half a myth.** The Java code hardcodes `pop`, `wu`, `tourist`, `happiness`, `border`, `tourism`, `food`, `population_maintenance`, `basic_settlement`, `research`, and anything ending in `_cap`. A pack maker can re-balance, but cannot add a new kind of thing.
- **The biggest design gap is not the config. It is that the player cannot choose research.** The server code is almost ready for it.

**Recommended direction:** keep CSV. Make it honest (one typed loader, validation, bundled files are the real defaults). Add player-chosen research behind a toggle. Do not move to datapacks or in-game config UI now. Details in section 6.

---

## 2. What the code does today (checked)

| Piece | Where | Size | Notes |
|---|---|---|---|
| Recipe loading | `production/ProductionRegistry.java` | 318 lines | Custom quote-aware split, then sorts the `inputs` cell into inputs, conditions and "global modifiers" (`min`, `excess`, `10%`) |
| Upgrade loading | `production/UpgradeRegistry.java` | 118 lines | Plain `split(",")`, a different parser from recipes |
| Cell parsers | `data/parsers/DataParser.java` | 155 lines | `parseEffects`, `parseConditions`, `parseResources`. Bad values are dropped with a WARN, or silently |
| Recipe run | `town/components/TownProductionComponent.java` | 671 lines | Expression evaluator (25 lines, product of terms only), condition checker (about 100 lines of string checks), starvation, output stall |
| Upgrades | `TownUpgradeComponent.java`, `UpgradeNode.java` | 421 + 159 | Levels, cost/benefit scaling, flat modifier map |
| AI research | `town/ai/TownResearchAI.java` | 405 lines | Scores nodes by guessing from effect names (`target.contains("_cap")`) |
| Display | `ProductionTab`, 2 view-models, 2 builders, 2 packets | about 1,600 lines | Server-built, read-only |

About 4,000 lines serve 21 rows of recipe and upgrade data. That ratio is the real smell: the machinery is far larger than the content it runs.

### Three facts that decide the question

1. **One flat namespace.** A recipe id, an upgrade modifier name, an effect target and a node id are all the same kind of string. `basic_settlement` lists `pop_cap:100;tourism;population_maintenance;farming_basic` in one `effects` cell. The first is a stat, the next two are recipe unlocks, the last is a chained node. The code finds out which at run time by looking the name up in several places. That is why nothing can be validated and why the AI has to guess from name suffixes.
2. **The admin manual and the code disagree.** `docs/archive/plans/manual_admin_config.md` describes a 6-column `productions.csv` with a separate conditions column and a different upgrades layout. The code reads 5 columns and hides conditions in the inputs cell. The format has drifted without anyone noticing, which is a sign it is too loose.
3. **The shipped defaults are not the real defaults.** The maintained CSVs in `assets/businesscraft/config/` are never copied out. Each registry writes its own older hardcoded rows instead (earlier review A1). This is the single worst problem with the config approach today, and it is a delivery bug, not a format problem.

---

## 3. Options compared

Effort: S = a day or less, M = a few days, L = a week or more. Risk is for the game and for existing server configs.

### Option A: Keep CSV, fix it (the earlier review)

Install bundled CSVs, validate on load, share one splitter, fix the toml comments.
- **Pros:** cheap, low risk, keeps every existing config working.
- **Cons:** the hidden mini-language stays. Validation has to re-discover the meaning of each string, so it stays heuristic.
- **Effort / risk:** S-M / low.

### Option B: Hardcode defaults in Java, tune with toml

Recipes and nodes become Java objects. Toml exposes speed multipliers, thresholds, caps.
- **Pros:** compile-time checking, no parsing bugs, no stale-fallback bug, easy to test, one source of truth. Fits the 0.9 plan, where production and research are **off by default** and tourism is the headline.
- **Cons:** every balance change needs a rebuild. Pack makers lose the main reason this system exists. Toml is poor at lists of rows with several fields, so each recipe would need a nested section. Biome kits and items would need the same treatment or the mod would have two config styles.
- **Where it makes sense:** the *rules* (starvation, happiness curve, the `tourism` recipe, minimum research speed). These are already hardcoded and just need to be honest about it and expose a few toml keys.
- **Effort / risk:** M / medium. It removes capability to save a little complexity.
- **Verdict:** wrong for the content, right for the rules.

### Option C: JSON via datapacks (or `config/` JSON)

Recipes and nodes as JSON files with a Codec and a reload listener.
- **Pros:** typed fields and nesting (`"inputs": [{"item":"wood","amount":25}]`), real error messages from Codecs, works with `/reload`, can ship inside modpacks and be overridden by other packs, item tags are native.
- **Cons:**
  - It does not remove the real problems. Conditions and expressions still need a language, either strings inside JSON or a new JSON AST.
  - The mod is on Forge and Fabric. Reload hooks differ per loader (`AddReloadListenerEvent` vs the Fabric resource manager helper), so it needs new platform glue.
  - Datapacks are per world. Today's config is per server install and hot-reloads. This is a real behaviour change for admins.
  - `items.csv` and `biomes.csv` should move too, or the mod ends up with two config systems. Everything that calls the four registries (30 files) would be touched.
  - Breaks existing configs, so it needs a converter or a clean break.
- **Effort / risk:** L / medium-high.
- **Verdict:** a good end state if the mod gains a modding ecosystem. Not worth it for 21 rows today. Option D below gets most of the validation benefit.

### Option D: Keep CSV files, replace the cell language with typed, explicit syntax and one loader

This is the middle road. The files stay as spreadsheets. The *cells* get stricter, and one loader turns them into typed Java records and reports every problem.

Concrete changes:
- **Separate `conditions` column** in `productions.csv` (the manual already says this). No more sorting the inputs cell by guessing.
- **Effects get a verb:** `stat:pop_cap:+100`, `unlock:farming`, `speed:farming:+50%`, `grant:farming_basic`. Today `farming*0.5` and `farming` and `pop_cap:100` differ only by punctuation, and `farming_basic` is distinguished only by being in a different registry.
- **One condition form:** `left op right`, where right is a number, `N%` (of the left side's cap), `min` or `excess`. The legacy `95%pop_cap` branch is the same as `95%` for `pop` today (the cap already resolves to `pop_cap`), so it can go.
- **Repeat column** gets named parts (`max=5;cost=1.1;benefit=1.2`) so an empty or reordered field can't shift meaning. The scaling maths question (earlier review B3) gets a decision at the same time.
- **One loader + one `ValidationReport`:** every unknown id, bad number, missing prereq, short row, and unknown biome node is one WARN with file and row. `/businesscraft validate` prints it. A unit test loads the bundled files and asserts zero problems.
- **Bundled files are the defaults** (copy from the jar), and the four registries share the loader.
- **Pros:** fixes the real defects (silent failure, drift, two parsers, stale defaults). Keeps spreadsheet editing and hot reload. Makes the AI scorer and the tooltips able to read effect kinds instead of guessing from names. Alpha-stage means few outside configs to break, so a clean format change is cheapest now. (Confirm with Gareth that no outside packs exist.)
- **Cons:** it is a breaking format change for anyone with edited CSVs. It is a real refactor of the parsers, two registries, `TownUpgradeComponent.recalculateModifiers`, the AI scorer and both builders. Tests for those need rewriting.
- **Effort / risk:** M-L / medium. Risk is concentrated in the effects change; the loader itself is easy to test.
- **Verdict:** the best value overhaul of the data side.

### Option E: In-game UI-driven configuration

An editor screen for recipes, nodes and thresholds, with packets and saved state.
- **Pros:** friendly to non-technical admins.
- **Cons:** needs permission design, packets, a serialised store (NBT or JSON), a UI for rows with lists and expressions, undo, and sync to clients. It still needs the same rule language underneath, so it adds a layer rather than removing one. For a tourism-first alpha with research and production off by default, this is the largest and least justified option.
- **Effort / risk:** L+ / high.
- **Verdict:** no. A `/businesscraft reload` and `/businesscraft validate` command gives most of the admin comfort for a few hours of work. (There are already commands in `command/`.)

### Option F: Player-chosen research (design change, independent of the file format)

Today research is **AI-only**: `TownResearchAI` picks the node. The UI says "Idle - Select Research" but there is nothing to select.

What is already in place:
- `TownUpgradeComponent.startResearch(String nodeId)` is public. It already checks max level, prerequisites and affordability, deducts cost and notifies the town.
- The Upgrades view-model already carries per-node cost, affordability and prerequisite status.
- Nodes show in a list with rows.

What is missing:
- One client-to-server packet (`StartResearchPacket(townId, nodeId)`), with the usual range and permission check used by the other town packets.
- A button on each Upgrades row.
- A `research.mode` toml key: `ai` (today), `player`, or `both` (AI only fills idle time).
- Make the "wants" system follow the player's choice: `Town.calculateWants()` calls `TownResearchAI.getBestUpgradeTarget` today. With player choice it would want the resources for the queued node, which is much more readable for a player.

- **Pros:** the biggest gameplay gain per line of code. Turns the tab from a dashboard into decisions. Makes the AI optional (405 lines that guess from name strings). Removes the "Select Research" lie. The old manual already described this design.
- **Cons:** new packet and permission surface. Needs a "who may spend town funds" decision (owner only? any member?). Balance changes, because a human will pick better than the AI and research will move faster. Needs a multiplayer test of simultaneous clicks.
- **Effort / risk:** M / low-medium.
- **Verdict:** yes, and it is the one thing that justifies keeping the Upgrades system at all.

### Option G: Simplify the expression and condition language

What is there is not complex. `evaluateExpression` is a product of terms: numbers, `pop`, `happiness`, `wu`, or any upgrade modifier. It has no parentheses, no `+`, no precedence. That is already the right size.

The mess is in conditions: `min`, `excess`, `N%`, `N%pop_cap`, `surplus:food`, plus implied conditions made from inputs (`wood:25;wood:>min`), plus operators found by `contains(">")`. About 100 lines of string checks.

- **Replace with a real parser or an expression library?** No. It would add a bigger language than the one that exists. Nothing in the 9 recipes needs it.
- **Do instead:** keep multiplication only, and make unknown tokens an error at load (they quietly become 0 today, so `1*popp` makes an input free). Put conditions in their own column with one form (see Option D). Make `surplus:` an explicit function name.
- **Effort / risk:** S-M / low, mostly inside Option D.

---

## 4. Verdict on the CSV question

- **Is CSV overkill?** No. For a table of 9 and 12 rows it is the lightest option that a non-programmer can edit. It is also what the other config files here use (`items.csv`, `biomes.csv`), so the mod stays consistent.
- **Is it silly to cram a language into cells?** Yes, a bit. That part should be tightened, not the file format.
- **Would hardcoding be better?** Only for the rules, not the data. It would remove the one thing that lets server owners shape the economy.
- **Would datapacks/JSON be better?** Cleaner end state, but a large move for little gain right now, and it changes admin behaviour (per world, `/reload`). Revisit if a modding ecosystem appears or if the mod moves to a Codec-based data layer anyway.
- **Is the bigger issue the format at all?** No. Ranked by damage:
  1. Bundled defaults never installed (delivery).
  2. Silent failures (no validation).
  3. One untyped namespace (stat vs unlock vs node).
  4. No player agency in research (design).
  5. Format choice (distant last).

---

## 5. Scope sanity check

The 0.9 roadmap (`tasks/ROADMAP_v0.9.md`) ships tourism-only by default. Production and research are off. So this system is a power-user feature for now. Spend in proportion:
- Do what is needed to make it **correct and safe to turn on** (Option A first, Option D soon after).
- Add player choice only if Gareth wants research to be a real feature for 1.0 (Option F).
- Do not start the datapack or UI-config work until the tourism loop is solid.

---

## 6. Recommended direction

Do in this order. Each step is a separate PR.

1. **Earlier review P1s** (A1 install bundled CSVs, B1 `pop_cap`, C1 wrong Production numbers). These do not depend on the overhaul and stop the system being broken. *S-M.*
2. **Single typed loader and validation (Option D, part 1).** One splitter, one `ValidationReport`, `/businesscraft validate` and `/businesscraft reload`, a test that loads the bundled files with zero warnings. Keep the current cell syntax in this step so no config breaks. *M.*
3. **Typed effect verbs and separate conditions column (Option D, part 2).** One clean format break, with the loader accepting the old syntax for one release and logging a deprecation WARN. Simplify `checkConditions` and the AI scorer in the same PR. *M.*
4. **Player-chosen research (Option F),** behind `research.mode`. Default `ai` so nothing changes for existing servers. *M.*
5. **Rules into toml (Option B, rules only):** starvation penalty, happiness curve, global speed multipliers, and named constants for `tourism`, `food`, `population_maintenance`. *S.*

**Do not do now:** datapacks/JSON (C), in-game config editor (E), a new expression language (G).

### Questions for Gareth
1. Are there outside servers or packs with edited CSVs? (Decides whether step 3 needs a compatibility window.)
2. Are the bundled cycle times (0.01 to 0.5 minutes) the intended balance, or test speeds?
3. Who may start research in a town: owner only, or any member?
4. Should research stay AI-driven at all once players can choose? (`both` mode keeps it as autopilot.)

### Main risks
- Step 3 touches effects, which feed upgrade levels, the AI scorer, tooltips and save data. Save data stores node ids, levels and flat modifiers keyed by stat name (`save()` in `TownUpgradeComponent`), not effect text, so worlds should still load. Renaming a stat would orphan saved flat modifiers, so add a save/load test.
- Player research changes pace. Rebalance cost and time after it lands.
- Hot reload must be made thread-safe (earlier review A6) before the `reload` command is added, or the command will make the existing race easier to hit.
