---
tags:
  - detail
  - config
---
# Data Parsing

**Breadcrumb**: Config > Data Parsing > Effect / Condition / Resource String Formats
**TL;DR**: Semicolon-delimited packed strings from CSVs are parsed into lists of Effect (key:value or key*mult or bare key), Condition (key:opvalue with >= <= > < = and optional %), and ResourceAmount (key:amount-or-expr); population aliases normalized; bad segments are skipped with warnings; pure string-to-data transformation with no side effects.

## What it does
The mod's upgrade nodes, production recipes, biome starting resources, and some milestone/production conditions are defined in simple CSV files using compact text formats (e.g. "pop_cap:10;tourist_cap:2;storage_cap_all:200" or "happiness:>80%"). This parser is the single place that turns those human-editable strings into the structured objects the game logic consumes. It makes config-driven features (scaling benefits by level, recipe gating, resource costs) possible without hard-coding numbers everywhere.

## How it works (process view)
- On load (UpgradeRegistry, ProductionRegistry, BiomeRegistry), raw comma-split fields from CSV rows are fed as "packed" strings to the three parse methods.
- Effects describe numeric or flag-like benefits: "pop_cap:10" means +10 population cap (absolute), "happiness:50" same, bare words like "population_maintenance" or "basic_taxes" become value=1.0 flag effects, "foo*1.5" is a multiplier form.
- Conditions gate recipes or behaviors: "happiness:>50", "pop:>=100", "surplus:>0%" etc. The operator and value (with % flag) are captured for later evaluation by TownProductionComponent etc.
- Resource amounts appear in upgrade costs, recipe inputs/outputs, starting biome values: "wood:10;iron:5" or special "pop:10" (normalized). The amount can be a literal number or a dynamic expression token (parsed as 0 if non-numeric, leaving expression for later).
- A worked example (from default upgrades.csv):
  Input: "basic_settlement,housing,Basic Settlement,,,Unlocks basic survival,0,,pop_cap:10;tourist_cap:2;storage_cap_all:200;happiness:50;population_maintenance;population_growth;basic_taxes"
  After effectsRaw parse: seven Effects — pop_cap=10 (not %), tourist_cap=2, storage_cap_all=200, happiness=50, population_maintenance=1.0 (bare), population_growth=1.0, basic_taxes=1.0.
  Costs example: "1,wood:10" → one ResourceAmount("wood", "10").
- Another: "farming_basic,farming,Basic Farming,,basic_settlement,Starts food production,1,wood:10,basic_farming" → costs ResourceAmount wood=10, and effects parse would see "basic_farming" as bare flag.
- The parser never throws on bad data; it logs a warning per bad segment and continues, so a single malformed entry in a long list doesn't kill the whole row.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `DataParser.parseEffects(String packed)` | `common/src/main/java/com/quackers29/businesscraft/data/parsers/DataParser.java` (lines 11-62) | Splits on ";", handles bare key (value 1.0), key*float, and key:value (with optional trailing % for isPercentage). Returns List<Effect>. |
| `DataParser.parseConditions(String packed)` | same (lines 64-110) | Splits on ";", each "key:expr" where expr may start with >= <= > < or =. Detects operator, strips it from value, detects trailing % on the remaining valueStr. Always emits Condition even for bare "=" case. |
| `DataParser.parseResources(String packed)` | same (lines 134-154) | Splits on ";", "key:val" pairs only. Normalizes three population-style aliases (population→pop, population_cap→pop_cap, tourist_count→tourist). Uses ResourceAmount(String,String) ctor so expressions are preserved. |
| `DataParser.ResourceAmount` (inner) | same | Simple carrier: resourceId, numeric amount (best-effort parse), and the original amountExpression string (for dynamic cases that stay symbolic). |
| `Effect` / `Condition` | `common/src/main/java/com/quackers29/businesscraft/data/parsers/Effect.java`, `Condition.java` | Immutable value objects carrying the parsed fields; toString mirrors the canonical form. |

## Rules & formulas (exact)
All three methods are static, null/empty-safe (return empty list), and tolerant.

**parseEffects**:
- Split on ";", trim parts, skip empty.
- No ":" present:
  - If contains "*": split on first "\*", left=key, right→float (else warn, skip this part). Effect(key, val, false).
  - Else: Effect(part, 1.0f, false) — bare flag form.
- Contains ":": split exactly once (length !=2 → warn+skip). key= [0].trim, valStr=[1].trim.
  - isPct = valStr.endsWith("%"); if true strip the "%" char before parse.
  - float val = Float.parseFloat(stripped); on fail warn+skip.
  - Effect(key, val, isPct).
- Order of checks: * handling is inside the no-":" branch; ":" branch never sees * form in current code.

**parseConditions**:
- Split ";", trim, skip empty.
- Must have exactly one ":", else warn+skip.
- key = [0].trim
- expr = [1].trim
- operator detection (checked in this order): startsWith(">=") → ">=", "<=" → "<=", ">" → ">", "<" → "<", "=" → "=", else default operator="=" and valueStr remains expr.
- If a real operator was detected (!empty && != "="), valueStr = expr.substring(op.length()).trim()
- isPct = valueStr.endsWith("%")  (note: the % stays in the stored value)
- Condition(key, operator, valueStr, isPct) — value may still contain "%" suffix when isPct true.

**parseResources**:
- Split ";", for each split on ":" (len==2 else ignored silently? — no explicit warn here).
- key normalization (exact match on original before trim? but after trim in practice):
  - "population" → "pop"
  - "population_cap" → "pop_cap"
  - "tourist_count" → "tourist"
- ResourceAmount(key, right-hand raw string). The ResourceAmount(String,String) ctor stores the expr and attempts Float.parse; failure sets numeric amount=0f (expression remains for callers that understand dynamics).

**ResourceAmount(String,float)** ctor (used in a few direct constructions outside parser): sets amount + expression=String.valueOf(a).

## Edge cases & behaviors
- null or "" or only whitespace/semicolons → empty list (no exceptions).
- Malformed segments: parseEffects and parseConditions log a single-line WARN via slf4j and omit the bad item; parseResources silently drops pairs that don't have exactly one ":".
- Float parse failures (e.g. "foo:bar", "x:1.2.3", "y:10%abc") → warn (effects/conds) or amount=0 (resources via its ctor), item omitted or zeroed.
- Percent forms: "happiness:>75%" → Condition("happiness", ">", "75%", true). The "%" character remains inside the value field.
- Operator edge: "foo:=" → operator="=", valueStr="".
- "foo:>= " (space) → valueStr after trim may be empty.
- * form only triggers in no-colon branch; "key:val*2" would be treated as literal value string "val*2" (parse fails → skipped).
- Alias normalization only happens for the three exact keys in parseResources; other keys (wood, iron, pop etc.) pass through.
- Duplicate keys in one packed string are allowed (last wins in lists, but lists preserve order and multiples).
- ResourceAmount with non-numeric expr (e.g. "pop:level*2" or "surplus:excess") → amount=0f, amountExpression keeps the original token string. Callers (Town etc.) decide how to interpret 0 + expression.
- No trimming of key/value beyond the explicit .trim() calls; leading/trailing spaces around delimiters are removed.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/data/parsers/DataParserTest.java`
- Covered: happy paths for all three parsers with realistic upgrade/production strings; bare flags, * multipliers, :value and :pct; condition operators >= <= > < = ; resource alias rewriting; null/empty/whitespace inputs → empty; malformed (no :, bad float, extra :, non-numeric) → skipped or zeroed with no crash; round-trip flavor via Effect/Condition toString where relevant; ResourceAmount expression vs parsed amount.
- Not covered (intentionally, per rules): actual application of Effects/Conditions to Town state (those live in TownUpgradeComponent / TownProductionComponent and require full town + registry setup); file loading side of registries; BiomeRegistry usage; any runtime evaluation of the operator/value in conditions.
- 16 tests (all pure, no bootstrap); every documented rule and edge has a hand-computed assertion. Full suite green.

## Open questions
- The "*" form in parseEffects is implemented but appears unused in the current default CSVs and production code paths. Is it legacy, intended for future multiplier effects, or dead? (Current behavior is pinned by test if present.)
- ResourceAmount stores both a (possibly 0) numeric amount and the original expression string. Is the numeric 0 a sentinel that means "look at expression", or do some code paths treat amount=0 literally? (Observed in Town.java upgrade cost handling and production.)
- Condition stores the "%" inside the value when isPercentage. Callers must know to strip it again for numeric comparison — potential source of future parsing bugs if evaluation logic is added.
- parseResources has no WARN logging for bad pairs (unlike the other two). Intentional silent drop or oversight?
- No public pretty-printer or inverse (list → packed string) exists, making round-trip editing or debugging harder.

## Related
- [[Config/Config Overview]]
- [[Config/Configuration Loading]] (T-014 — the TOML layer that feeds some of the same systems)
- [[Production/Production Overview]] (recipes and upgrades are the primary consumers of parsed Effects + ResourceAmounts + Conditions)
- [[Production/Upgrades/Effect Value Calculation]] (T-016 — receives the Effect objects that came out of parseEffects)
- [[Economy/Milestones/Distance Milestone Resolution]] (T-002 — milestone rewards use a different but similar "item:count" parse path inside DistanceMilestoneHelper)
