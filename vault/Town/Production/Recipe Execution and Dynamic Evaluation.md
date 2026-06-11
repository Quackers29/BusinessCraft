---
tags:
  - detail
  - town
---
# Recipe Execution and Dynamic Evaluation

**Breadcrumb**: Town > Production > Recipe Execution and Dynamic Evaluation
**TL;DR**: Towns automatically run unlocked production "recipes" (population growth, work unit generation, farming, tourist spawning, etc.) on a per-tick progress timer; input and output quantities are computed at runtime from dynamic expressions (e.g. `2*pop`, `5*happiness`, `wu`, or upgrade modifier names); effective cycle time = base minutes / speed modifier from the matching upgrade node (near-zero modifier stops the recipe); production/consumption "per day" rates for any resource are summed across matching recipes as (resolved quantity / cycle); recipes gate on complex conditions (stock vs min/excess thresholds or % of cap, happiness, surplus = prod>cons, explicit values) and stall if an output would exceed its storage/tourist/wu cap; the special population_maintenance recipe continues during food shortage and applies a starvation penalty (pop -1 + consume remaining food) on cycle complete.

## What it does
The production system gives towns a living economy that runs without player micromanagement. Unlocked upgrades enable recipes that slowly generate population (so the town grows), work units (for other systems), tourists (to send out), and other stats or resources. Because quantities and gates can reference the town's current population, happiness, or other live values, a larger or better-fed town naturally ramps up production and meets its own thresholds more easily. The computed rates feed the Resources and Production tabs in the UI, the town AI's research priorities, and contract/upgrade decisions. All timing and scaling respects the production speed modifiers granted by upgrades.

## How it works (process view)
- On every tick the TownProductionComponent iterates recipes from ProductionRegistry. A recipe is considered "unlocked" only when the town's upgrade modifier for that recipe's id is > 0.
- For each unlocked recipe it first resolves every input and output amount by evaluating its expression against current town state (see formulas below). Special virtual ids "pop", "tourist", "wu" bypass item registry; real resource ids are later mapped to Items for stock checks/add/remove.
- Conditions are checked (pop/happiness/surplus/resource/mod targets, operators, and magic values "min"/"excess"/"N%"/"N%pop_cap"/"pop_cap"). "surplus" re-uses the rate calculators. If any condition fails, progress for that recipe is reset to 0 and it is skipped.
- Effective cycle time is baseCycleMinutes / speedModifier (the upgrade modifier value for the recipe id itself acts as a production speed multiplier). If the modifier is <= 0.0001 the cycle is treated as infinite (recipe is stopped).
- A small progress accumulator for the recipe is incremented by 1/1200 per tick (modeling "one real minute" at 20 TPS). When progress >= effective cycle time the recipe "completes": inputs are consumed (special handling for wu/pop), outputs are produced (pop inc/dec, pending tourist spawns, wu, real items via registry, or flat stat modifiers), progress resets, and the town is marked dirty. Notifications may fire for starvation.
- Before completing, output space is checked using a "total" (hand + escrow + in-transit for items; current+pending for tourist; current for wu) against the appropriate cap (trading storageCap, tourist_cap, wu_cap). If adding the output would exceed, the recipe stalls (progress is kept, nothing happens this completion attempt).
- Starvation special case: only the recipe literally named "population_maintenance" is allowed to advance progress when it lacks its food inputs. On completion it decrements population by 1 (if >0), broadcasts a warning, and consumes every last unit of the food stock that was short.
- Three public query methods are used heavily by view models, menus, and AI:
  - getProductionRate(resourceId) and getConsumptionRate(resourceId) sum (resolved quantity / cycleTime) over every unlocked recipe that lists a matching output or input. These are "potential" rates assuming the recipe can run; conditions and current stock are ignored.
  - getActiveRecipes() returns a snapshot map of recipeId -> (currentProgress / effectiveCycle) in [0,1] for progress bars.
- Config values minStockPercent (default 60) and excessStockPercent (default 80) are read live from the static ConfigLoader fields when conditions compute "min" or "excess" thresholds and when the happiness base is calculated from food stock vs min stock level. Hot reload affects in-flight production immediately.
- Worked example (hand numbers): recipe "farming_basic" has baseCycleTimeMinutes=5.0, one output ResourceAmount("food", "4"). Town has upgrade modifier "farming_basic"=2.0 (double speed). Effective cycle = 5.0 / 2.0 = 2.5. Rate for food = 4 / 2.5 = 1.6 per cycle-unit. If another recipe also yields food at 0.4, total getProductionRate("food") = 2.0. At pop=20 a recipe with output expr "1*pop" would resolve to 20 and (if its cycle is 10) contribute 2.0 to a pop rate.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownProductionComponent` (ctor + tick/processRecipe + public getters) | `common/src/main/java/com/quackers29/businesscraft/town/components/TownProductionComponent.java` | The runtime engine. Owns per-recipe progress, wires to a Town for all state, evaluates dynamics, enforces conditions/stalls, performs consume/produce on completion. |
| `resolveDynamicAmount(rawId, expression)` (private) | same | Rewrites legacy "pop*" / "*pop" forms then delegates to evaluateExpression to produce a ResolvedResource(id, computedAmount). |
| `evaluateExpression(expr)` (private) | same | Splits on `*`, multiplies: "pop" -> town population, "happiness" -> town happiness, "wu" -> workUnits, numeric literal, or any other token -> town.getUpgrades().getModifier(token) (0 if unknown). Empty/null -> 0. |
| `getEffectiveCycleTime(recipe)` (private) | same | `base / mod`; if (mod <= 0.0001f) returns Float.MAX_VALUE (stopped). |
| `getProductionRate(id)` / `getConsumptionRate(id)` (public) | same | Iterate registry.getAll(), for matching outputs/inputs use resolve(..., "0") only to get id then resolve full expr for amt; add (amt / cycle) when cycle > 0 and recipe unlocked. |
| `getActiveRecipes()` (public) | same | For each tracked progress entry, compute current / getEffectiveCycleTime(recipe) (or 0). |
| `checkConditions(recipe)` (private) | same | Per Condition: target value lookup (pop/happiness/resource count / upgrade mod / surplus via rates), threshold derivation from literal / "min" (cap*minStock/100) / "excess" / "N%" / "N%pop_cap" / "pop_cap", comparison with == using 0.001 epsilon. Surplus re-invokes the rate methods. |
| `ProductionRecipe` | `common/src/main/java/com/quackers29/businesscraft/production/ProductionRecipe.java` | Immutable holder of id, displayName, baseCycleTimeMinutes, inputs/outputs (ResourceAmount), conditions (Condition). |
| `ProductionRegistry` (getAll / get) | `common/src/main/java/com/quackers29/businesscraft/production/ProductionRegistry.java` | Static registry of recipes loaded from productions.csv (or defaults). |
| `Town.getProduction()` + delegates | `common/src/main/java/com/quackers29/businesscraft/town/Town.java` | Exposes the component and the two rate methods to the rest of the system (ITownState, viewmodels, menus, AI). |
| `ConfigLoader.minStockPercent` / `excessStockPercent` (static) | `common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java` | Live values (60/80) used for threshold and happiness math. |

## Rules & formulas (exact)
All formulas are taken verbatim from the implementation (as of this iteration). Units are "cycle units" (the division produces a rate whose display callers later scale to per-hour etc.).

- **Dynamic amount resolution**:
  - `resolveDynamicAmount` rewrites: if id starts with "pop*" then id = suffix, expr = expr + "*pop"; symmetrically for suffix "*pop".
  - Then `amount = evaluateExpression(expr)`.
- **evaluateExpression**:
  ```java
  float result = 1.0f;
  for (String part : expr.split("\\*")) {
      part = part.trim();
      if (part.equalsIgnoreCase("pop")) result *= town.getPopulation();
      else if (part.equalsIgnoreCase("happiness")) result *= town.getHappiness();
      else if (part.equalsIgnoreCase("wu")) result *= town.getWorkUnits();
      else {
          try { result *= Float.parseFloat(part); }
          catch (NumberFormatException e) {
              if (part.equalsIgnoreCase("wu")) ... else {
                  float mod = town.getUpgrades().getModifier(part); // 0 for unknown
                  result *= mod;
              }
          }
      }
  }
  ```
  - Empty or null expr -> 0.0f.
  - Order is left-to-right multiply from 1.0; a single bare number works.
- **Effective cycle time**:
  ```java
  float mod = town.getUpgrades().getModifier(recipe.getId());
  if (mod <= 0.0001f) return Float.MAX_VALUE;
  return recipe.getBaseCycleTimeMinutes() / mod;
  ```
- **Rate summation (production example)**:
  ```java
  for (ProductionRecipe r : ProductionRegistry.getAll()) {
      if (town.getUpgrades().getModifier(r.getId()) <= 0) continue;
      for (ResourceAmount out : r.getOutputs()) {
          ResolvedResource rid = resolveDynamicAmount(out.resourceId, "0"); // id only
          if (rid.id().equals(resourceId)) {
              float cyc = getEffectiveCycleTime(r);
              if (cyc > 0) {
                  float amt = resolveDynamicAmount(out.resourceId, out.amountExpression).amount();
                  totalPerDay += (amt / cyc);
              }
          }
      }
  }
  ```
  Identical structure for consumption over inputs.
- **Progress increment**: every tick (in processRecipe path) `current += 1.0f / 1200.0f; if (current >= effective) { ... complete ... current=0; }`
- **Output stall check** (pre-completion):
  - For tourist: current = touristCount + pending; cap = tourist_cap mod
  - For wu: current = workUnits; cap = wu_cap
  - Else: current = totalResourceCount + inTransit; cap = trading.getStorageCap(resId)
  - If current + amount > cap -> stall (keep progress, return).
- **Happiness update** (every 20 ticks, only for food resource):
  ```java
  float minStockLevel = cap * (ConfigLoader.minStockPercent / 100.0f);
  if (minStockLevel < 0.01f) minStockLevel = 0.01f; // safety
  float ratio = current / minStockLevel;
  float base = ratio * 50.0f;
  if (base > 50.0f) base = 50.0f;
  town.setHappiness(base);
  ```
- **Starvation (only when recipeId == "population_maintenance" && !hasInputs)**: still advance progress with the normal tickIncrement. On >= effective: pop = max(0, pop-1), broadcast red warning, consume all remaining units of every input resource that was required (even partial), reset progress.
- **Condition threshold derivation** (inside checkConditions):
  - "min" -> cap * (minStockPercent/100)
  - "excess" -> cap * (excessStockPercent/100)
  - ends with "%" (pure) -> cap * (pct/100)
  - contains "pop_cap" -> (pct/100)*popCap or just popCap
  - else Float.parseFloat or skip
  - Comparison uses `Math.abs(a - b) < 0.001f` for "=".
- Config fields are read at evaluation time (hot-reload visible). Registry and upgrade modifiers are live.

## Edge cases & behaviors
- expr null/empty/only whitespace -> 0.0 (multiplier stays 1 then yields 0 after trim loop).
- Unknown identifier in expr (not pop/happiness/wu and no upgrade mod) -> treated as 0 multiplier (result becomes 0 for that term).
- Speed mod <= 0.0001f or <=0 from unlock -> cycle = Float.MAX_VALUE; rates that see cyc > 0 skip the contribution; progress never reaches completion.
- cap == 0 or very small in happiness/min calc -> safety clamp minStockLevel >= 0.01f so no div-by-zero; base happiness still computed.
- Output "pop" or "tourist" or "wu" skip normal item registry / cap checks in some paths (explicit ifs).
- In rate methods the resolve for id matching uses dummy expression "0" (the id part is what matters; amt is re-resolved with the real expr only for the matching output).
- Surplus condition invokes getProductionRate/getConsumptionRate (potential rates) — it does not look at current stock.
- "surplus" target in conditions short-circuits the normal threshold math.
- Starvation path only triggers for the exact string id "population_maintenance"; other recipes simply stall/reset progress when inputs missing.
- In-transit + hand + escrow used for item output space checks (via getTotalResourceCount + getInTransitResourceCount).
- Duplicate assignment `current = ...; current = ...;` exists in the tourist stall block (harmless, second wins).
- getActiveRecipes returns 0 for recipes that have never run (no entry in recipeProgress) or when effective is "infinite".
- Negative amounts are not generated by the expression system in normal use (multipliers are non-negative); clamps live in Town.add* methods.
- Registry iteration order is HashMap (unspecified); rates simply sum so order does not affect numeric result.
- Legacy pop* rewrite happens before any evaluation; both prefix and suffix forms supported.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/components/TownProductionComponentTest.java`
- Covered via direct calls + reflection on privates: evaluateExpression (all token types + combos + rewrite + bad input), resolveDynamicAmount, getEffectiveCycleTime (normal + stopped + fractional), getProductionRate/getConsumptionRate (simple, pop-scaled, speed-modded, locked, additive, unknown), getActiveRecipes (progress %), condition evaluation (numeric, min/excess/%, pop_cap, surplus, ops, epsilon =), stall on output cap, starvation special path for population_maintenance, config percent live read (save/restore), zero/edge values.
- Not covered (by design for this iteration): full tick() side effects over many cycles, real CSV-loaded recipes + file I/O, network/UI delivery of the resulting rates, interaction with live ContractBoard or full TownManager, exact floating point accumulation drift over thousands of ticks.
- McBootstrap used (required for Town construction safety + Item registry when resource paths or "food" lookups execute). Reflection used for private expression/condition helpers (pure logic, matches protocol precedent). ProductionRegistry static RECIPES map manipulated via reflection for deterministic recipe injection (pattern from ProductionRegistryTest).

## Open questions
- The pop* / *pop rewrite is a compatibility shim — is the productions.csv still using the old syntax, or can it be removed after a migration?
- Speed modifier of exactly 0.0001f is the cutoff for "stopped"; this is an implementation detail (not documented in config comments). A clearer named constant or <=0 would be more obvious.
- getProductionRate / getConsumptionRate compute *potential* throughput ignoring conditions and current stock. Callers (viewmodels, AI) treat them as "current rate" — this can be misleading when a recipe is gated or starved. Consider separate "actual" vs "theoretical" or a javadoc warning.
- Starvation consumes the entire remaining stack of required food rather than the exact deficit for that cycle. Intentional punishment or accidental?
- Duplicate `current = town.getTouristCount() + ...;` line in the tourist stall block (line ~294-295) is a harmless copy-paste artifact; a future cleanup could remove it.
- evaluateExpression and the condition threshold logic are complex private methods. Extracting them to small pure static helpers (or a dedicated Expression/Condition evaluator) would make them directly testable without reflection and would help ProductionRegistryTest-style determinism.
- Happiness is hard-capped at a base of 50 from the food ratio (even when stock >> min level). Is the remaining 50 intended to come from other modifiers (upgrades, events)?

## Related
- [[Production/Production Overview]]
- [[Production/Recipes/Estimated Effort Calculation]] (T-025 — static registry + effort math)
- [[Production/Upgrades/Effect Value Calculation]] (T-016 — the node.calculateEffectValue and multipliers that feed recipe speed + caps)
- [[Town/Town Overview]]
- [[Config/Configuration Loading]] (T-014 — the minStockPercent / excessStockPercent values)
- [[Town/Resources/Resource Storage Operations]] (T-007 — the addResource / total counts used by production)
