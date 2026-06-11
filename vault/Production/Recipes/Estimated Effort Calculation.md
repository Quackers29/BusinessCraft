---
tags:
  - detail
  - production
---
# Estimated Effort Calculation

**Breadcrumb**: Production > Recipes > Estimated Effort Calculation
**TL;DR**: After loading productions.csv, ProductionRegistry walks each producible resource's recipes to compute a "base effort minutes per output unit" (min over recipes of (cycleTime + sum(inputQty × inputEffort)) / outputQty); the estimated emerald value is effort × 20 (default 1.0 if resource has no production path or on cycle/zero).

## What it does
Production recipes define how raw and intermediate resources are turned into outputs over time (e.g. wheat farming, wood to planks, population growth). The effort calculator reverse-engineers a consistent "cost in minutes" for every output that can be produced. This gives the game a baseline "fair price" for every resource based purely on production time and dependency costs — used to seed contract bidding and market displays before live player trades update the GlobalMarket prices on top.

## How it works (process view)
- At load time (called from ConfigLoader after reading the CSV), calculateEstimatedValues builds a reverse index of "which recipes produce each resource".
- For every resource that appears as an output somewhere, it runs a recursive effort calculation with memoization and cycle detection.
- For a recipe: effort = (its cycle time in minutes + the effort cost of all its inputs) / how many of the target resource it yields.
- Input costs are looked up recursively (so "planks" cost includes the effort of the "wood" that went into them).
- If a resource has several recipes, the cheapest (lowest effort) one wins.
- The public estimated value is simply effort × 20 emeralds. Resources that cannot be produced by any recipe (e.g. raw "wood" if only consumed, or "happiness") fall back to 1.0.
- **Worked example (simple)**: basic_farming recipe: cycle 1 min, no inputs, outputs food:4 → effort = (1 + 0) / 4 = 0.25 → estimated value 0.25 × 20 = 5 emeralds per food.
- **Worked example (chained)**: wood_to_planks: cycle 0.5, inputs wood:4, outputs planks:16. If wood effort=0 (base input), total = 0.5 + 4×0 = 0.5, /16 = 0.03125 → est 0.625 per plank.
- The 20× multiplier and the effort numbers themselves are not configurable; they are baked into the estimator. Live GlobalMarket trades (90/10 blend + 5% drops) override these seeds for actual contract pricing.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ProductionRegistry.calculateEstimatedValues()` | `common/src/main/java/com/quackers29/businesscraft/production/ProductionRegistry.java` (lines 163-192) | Orchestrates: builds producers reverse map from RECIPES, calls recursiveGetEffort per output resource, stores effort and 20×price into static ESTIMATED_VALUES / EFFORT_VALUES |
| `ProductionRegistry.recursiveGetEffort(...)` | same file (194-266) | Core recursive formula with memo + visiting set for cycles. Returns min unit effort or MAX/0/1 fallbacks. |
| `ProductionRegistry.resolveQuantity(ResourceAmount)` | same file (268-285) | Handles dynamic quantities (amount=0 + expr like "1*pop"): crude split on first non [0-9.] segment and parse, else 1.0 |
| `ProductionRegistry.getEstimatedValue(String)`, `getEffort(String)`, `getAllEstimatedValues()` | same (307-317) | Public accessors (default 1.0f when missing); getAll returns unmodifiable snapshot of the computed prices |
| `ProductionRecipe` + `DataParser.ResourceAmount` | production/ProductionRecipe.java + data/parsers/DataParser.java | Data carriers consumed by the calculator (inputs/outputs lists after condition stripping in load) |

## Rules & formulas (exact)
The estimator runs only on resources that are outputs of at least one recipe (producers.keySet()).

```java
// inside calculateEstimatedValues, per output resource
float effort = recursiveGetEffort(resourceId, producers, effortCache, visiting);
float estimatedPrice = 20.0f * effort;
ESTIMATED_VALUES.put(resourceId, estimatedPrice);
EFFORT_VALUES.put(resourceId, effort);
```

Core recursion (simplified from recursiveGetEffort):

```java
if (cache.containsKey(r)) return cache.get(r);
if (visiting.contains(r)) return Float.MAX_VALUE; // cycle
List<ProductionRecipe> recipes = producers.get(r);
if (recipes == null || recipes.isEmpty()) return 0.0f; // base / non-producible
visiting.add(r);
float minEffort = Float.MAX_VALUE;
for (each recipe) {
  float cycleTime = recipe.getBaseCycleTimeMinutes();
  float inputsCost = 0;
  boolean invalid = false;
  for (input : recipe.getInputs()) {
    float q = resolveQuantity(input);
    if (q > 0) {
      float ie = recursiveGetEffort(input.resourceId ...);
      if (ie == Float.MAX_VALUE) { invalid=true; break; }
      inputsCost += q * ie;
    }
  }
  if (invalid) continue;
  float total = cycleTime + inputsCost;
  float outQty = 1.0f;
  for (out : recipe.getOutputs()) if (matches) { outQty = resolveQuantity(out); break; }
  if (outQty <= 0) outQty = 1.0f;
  float unit = total / outQty;
  if (unit < minEffort) minEffort = unit;
}
visiting.remove(r);
if (minEffort == Float.MAX_VALUE) minEffort = 1.0f;
cache.put(r, minEffort);
return minEffort;
```

- `resolveQuantity`: if ra.amount <= 0.0001f and expr present: `split("[^0-9.]")[0]` parse or fallback 1.0f. Otherwise the static amount.
- Multiple recipes for one output: the minimum unitEffort across valid recipes is kept (cheapest path wins).
- The 20.0f multiplier is hard-coded (no config, no comment explaining the constant in the executed path).
- ESTIMATED_VALUES only contains entries for actually-produced outputs after a load(); everything else yields the 1.0f default from the getters.
- Callers (e.g. ContractBoard.getAllMarketPrices) merge the map (production estimates first) then overlay live GlobalMarket prices (later entries win).

## Edge cases & behaviors
- Resource never appears as a recipe *output* (pure input like "wood" in the default wood_to_planks, or stats like "pop", "happiness") → no entry → getEstimatedValue/getEffort return 1.0f.
- Recipe graph contains a cycle → the cycling path is marked invalid (MAX); if *all* paths for a resource cycle or fail, final minEffort stays MAX → clamped to 1.0f fallback.
- Output quantity resolves to ≤ 0 (dynamic 0 or explicit) → treated as 1.0 to avoid div-by-zero.
- Input quantity dynamic → resolveQuantity heuristic: if amount<=0.0001 and expr, `split("[^0-9.]")[0]` then parse or 1.0. Works for leading-digit forms ("2*foo" → 2, "4.5*pop"→4.5). Pure non-leading forms (e.g. "*pop" as the amountExpression) cause ArrayIndexOutOfBoundsException on [0] (the split returns a zero-length array when the entire string consists of delimiters). In normal load flow the amountExpression side after ":" is usually a plain number or parsable token from the CSV, so this AIOOB is unreachable in practice via productions.csv but is pinned by test.
- No recipes at all (empty RECIPES) → maps stay empty → all lookups return the 1.0 default.
- Recipe with 0 cycle time and 0 inputs, yield 1 → effort 0 → est price 0.
- Very deep trees or high multipliers can produce large effort numbers (float, no cap); NaN/Inf in inputs would propagate.
- The estimator ignores all Condition objects (they were stripped during load parsing into the recipe's separate conditions list).
- After any load(), previous computed values are cleared (ESTIMATED_VALUES.clear() etc).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/production/ProductionRegistryTest.java`
- Covered: default fallback 1.0 (for never-produced), base 0-effort resources, simple cycleTime / yield, chained inputs (effort accumulation), multi-recipe min selection, outputQty <=0 guard, resolveQuantity numeric expr heuristic (+ assertThrows on bad leading-non-digit expr), cycle detection + fallback interaction (MAX skip, entry-point can compute cycle+depFallback1 instead of pure 1), hand-computed expectations with arithmetic comments for each scenario; static map snapshot/restore for isolation between tests; no McBootstrap (pure data + reflection on privates). 13 tests.
- Not covered: actual CSV load + file I/O + platform config dir (exercised only via integration in full mod startup); the complex input/condition/global-modifier parsing that precedes recipe construction (see DataParserTest); runtime use of the values inside TownProductionComponent tick or ContractBoard auction math (those require Town/Level or full registries); effect of conditions on production (they gate running, not the estimator).
- Note: calculateEstimatedValues and recursiveGetEffort are private; tests drive them via reflection after injecting crafted ProductionRecipe lists into the private RECIPES map. Matches the loop rule for pure private logic.

## Open questions
- The 20× "emeralds per effort-minute" constant is magic and undocumented in the executed code. Is it intended to be exposed in config or balanced against real play economy?
- resolveQuantity's expr heuristic (`split("[^0-9.]")[0]`) is extremely crude and only supports leading integer/float before any * or letters. Forms without leading digit (amountExpression "*pop") cause ArrayIndexOutOfBoundsException (split returns length-0, [0] fails). Normal CSV paths put the * in the resourceId side of k:v and numeric values after ":", so AIOOB is unreachable today via load(); pinned by test (assertThrows) + Open questions.
- Base resources that are only inputs never receive an effort entry (return 1.0). In practice "wood" would be 1.0 unless a "woodcutting" recipe that outputs wood is added. Intentional seed price, or oversight?
- Cycle detection: when a back-edge hits `visiting.contains` it returns MAX for that lookup; the recipe using it is marked invalid and skipped. If *all* recipes for a resource are invalid, minEffort stays MAX → clamped to 1.0f inside that resource's recursion frame. However, a resource that *consumes* a cycled resource may receive the fallback-1 from the dep and compute cycleTime + 1*dep as its own effort (see pinned test `calculate_cycleInRecipeGraph_fallsBackToOne`). The entry point of a pure cycle pair can therefore end up with effort = cycle + 1 rather than 1.0. Visiting set is mutated/recovered per frame so separate top-level queries are independent.
- The estimator is recomputed from scratch on every ProductionRegistry.load() (which happens on mod init / config reload). No incremental update.
- (From T-016 Open questions) This was explicitly called out as the "much more complex recursive effort/price estimator" left untested at the time of the upgrade effect work.

## Related
- [[Production/Production Overview]]
- [[Production/Upgrades/Effect Value Calculation]] (T-016 — different scaling formula, same registry area)
- [[Trade/Trade Overview]] (prices from here seed ContractBoard.getAllMarketPrices before GlobalMarket overlay)
- [[Trade/Global Market/Price Calculation]] (T-006 — the live 90/10 + drop system that sits on top of these seeds)
- [[Config/Data Parsing]] (T-021 — the ResourceAmount/Condition parsing that feeds the recipes consumed here)
