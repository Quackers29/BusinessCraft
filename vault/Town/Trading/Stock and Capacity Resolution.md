---
tags:
  - detail
  - town
---
# Stock and Capacity Resolution

**Breadcrumb**: Town > Trading > Stock and Capacity Resolution
**TL;DR**: TownTradingComponent maps special virtual resource ids ("pop", "tourist", "happiness", "tourism", "tourism_dist") to live town stats and returns real item counts for everything else; storage caps come from upgrade modifiers (pop_cap/tourist_cap + storage_cap_all/specific) or Float.MAX for historical stats, with alias resolution for item ids; adjustStock excludes read-only stats, routes real items to the town's resource ledger, and clamps virtual internal stocks (with defaults min=100, max from config, learned from usage).

## What it does
Many systems (production recipes, research AI priorities, contract bidding, upgrade affordability) need a uniform way to ask "how much X does this town have right now?" and "what is the max it can hold?". Not everything is a physical item in a chest: population, active tourists, happiness, and cumulative tourism are virtual "stocks" derived from other town fields. The trading component supplies these derived values plus capacity limits that scale with upgrades, and a small internal market for virtual commodities that slowly restock when below a learned floor.

## How it works (process view)
- Code calls `town.getStock("pop")` (or directly via the trading component) and receives the current population as a float.
- `town.getStock("tourist")` returns active tourists plus any pending spawns from production.
- `town.getStock("happiness")`, `town.getStock("tourism")`, and `town.getStock("tourism_dist")` surface the other virtual stats used in recipe conditions and AI scoring.
- For ordinary resources like "iron" or "food", it looks up the ResourceType, resolves to a real Minecraft Item via the registry, and delegates to the town's on-hand resource count.
- `getStorageCap("pop")` returns the pop_cap modifier from upgrades (or 0 if none). Same for tourist_cap. Tourism history stats always report unlimited capacity. Real resources combine a global storage_cap_all modifier with a per-resource storage_cap_<id> (with fallback alias resolution if the id isn't a direct resource key).
- `adjustStock(id, amount)` is the write path: positive adds, negative spends. It refuses to touch pop/tourism*/happiness (they are outputs, not stores), routes +pending for "tourist", delegates real items to the resource ledger (which does the safe add/clamp), and for other virtual ids maintains a small internal map of TradingStock with a default floor of 100 and a configurable max; additions are capped to the current storage cap for that id and the value is never allowed below 0.
- A background `tick()` slowly pushes any internal virtual stock that has fallen below its learnedMin back up by ConfigLoader.tradingRestockRate each cycle (subject to enabled flag on the town level).

**Worked example**: A town has population 42 and an upgrade that sets pop_cap to 150 via flat modifier. `getStock("pop")` returns 42.0f. `getStorageCap("pop")` returns 150.0f. A production condition "pop > 40" sees 42 and passes. For an internal commodity "luxury" with no real item mapping, first adjustStock("luxury", 25) creates a TradingStock(current=25, learnedMin=100, learnedMax=500) (using the config default max). Later getStock("luxury") returns 25.0. Calling adjustStock("luxury", 100) would compute 125, see the cap is 500, store 125. If instead the cap was 80 (from an upgrade), it would clamp the addition to 80.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownTradingComponent.getStock(String)` | `common/src/main/java/com/quackers29/businesscraft/town/components/TownTradingComponent.java` (lines 54-90) | Primary read path implementing ITownState; special-cases five virtual ids, falls back through ResourceRegistry + real item count or internal stocks map |
| `TownTradingComponent.getStorageCap(String)` | same (139-187) | Capacity for the stock system; specials for pop/tourist, unlimited for tourism*, otherwise sum of upgrade storage_cap_* modifiers with alias fallback |
| `TownTradingComponent.adjustStock(String, float)` | same (92-137) | Write path with exclusions, delegation to real resources or pending spawns, internal stock creation + clamping to 0 and to cap on additions |
| `TownTradingComponent.Tick()` | same (42-52) | Restock hook for internal virtual stocks that are below learnedMin |
| `Town.getStock` / `Town.getStorageCap` | `common/src/main/java/com/quackers29/businesscraft/town/Town.java` (1196-1203) | Convenience delegates that also satisfy ITownState for the AI |
| `TradingStock` (inner) | same file | Simple holder: current + learnedMin + learnedMax for virtual non-real resources |

## Rules & formulas (exact)
From the source (exact behavior, not comments):

**getStock special cases (in order):**
- "pop" → (float) town.getPopulation()
- "happiness" → town.getHappiness()
- "tourist" → town.getTouristCount() + town.getPendingTouristSpawns()
- "tourism" → (float) town.getTotalTouristsArrived()
- "tourism_dist" → (float) town.getTotalTouristDistance()
- Otherwise: ResourceRegistry.get(resourceId); if type && type.getMcItemId() != null → resolve Item via PlatformAccess registry → town.getResourceCount(item) (as float)
- Fallback: stocks.getOrDefault(resourceId, 0.0f)  (note: returns the .current of a TradingStock or 0)

**getStorageCap special cases (in order):**
- town == null → 999999f
- "pop" → town.getUpgrades().getModifier("pop_cap")
- "tourist" → town.getUpgrades().getModifier("tourist_cap")
- "tourism" or "tourism_dist" → Float.MAX_VALUE
- Otherwise:
  - baseGlobal = 0f
  - globalMod = upgrades.getModifier("storage_cap_all")
  - capKey resolution: if ResourceRegistry.get(resourceId) == null, attempt to parse resourceId as ResourceLocation, lookup Item, then ResourceRegistry.getFor(item) and use the mapped id if found
  - specificMod = upgrades.getModifier("storage_cap_" + capKey)
  - return baseGlobal + globalMod + specificMod

**adjustStock rules:**
- "tourism", "tourism_dist", "pop" → return immediately (no-op; read-only historical/derived)
- "tourist" → if (amount > 0) town.addPendingTouristSpawns((int)amount); return
- If the id resolves via ResourceRegistry to a type with mcItemId → town.addResource(item, (int)amount); return
- Else (internal virtual stock):
  - If not present in stocks map: create TradingStock(0, 100, ConfigLoader.tradingDefaultMaxStock)
  - newAmount = stock.current + amount
  - if (amount > 0) { cap = getStorageCap(resourceId); if (newAmount > cap) newAmount = cap; }
  - if (newAmount < 0) newAmount = 0;
  - stock.current = newAmount

**Tick restock (only for internal stocks):**
- for each stock: if (stock.current < stock.learnedMin) stock.current += ConfigLoader.tradingRestockRate
- (no explicit clamp to learnedMax or storage cap here; growth is unbounded in the simple linear step)

Config values that feed in (mutable statics on ConfigLoader):
- tradingRestockRate (used only in tick)
- tradingDefaultMaxStock (used only when first creating an internal TradingStock)
- (minStockPercent / excessStockPercent are consumed by callers of getStorageCap, not inside this class)

## Edge cases & behaviors
- Unknown resourceId in getStock → 0.0 (no exception, no creation)
- getStorageCap for a never-seen real resource with no matching upgrade keys → 0.0 (baseGlobal only)
- adjustStock on a read-only id (pop/tourism*) silently drops the delta
- Negative adjust on a real resource delegates (the real ledger will clamp to 0)
- Adding to an internal stock when current cap is 0 or very small → clamps to that cap (or 0)
- First adjust on internal creates with learnedMin=100 hard-coded (not from any "learned" observation yet; the name "learned" appears to be aspirational for future persistence of observed min/max)
- tick restock only runs when ConfigLoader.tradingEnabled (checked by Town before calling component.tick)
- tourism_* and pop never get internal stocks or restock; they are always derived on read
- cap resolution for item-based ids can succeed via the fallback RL parse + getFor even if the string id itself is not a registered resource key (e.g. a raw "minecraft:iron_ingot" passed as resourceId)
- If upgrades component is absent on the town, all non-special caps collapse to 0

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/components/TownTradingComponentTest.java`
- 20 tests. Covers: every special getStock virtual id (pop/happiness/tourist/tourism/tourism_dist) with injected state, getStorageCap specials + upgrade modifier sums (pop_cap, tourist_cap, storage_cap_all + specific), internal stock creation + addition clamping to current getStorageCap + zero-floor on negative, read-only exclusions for pop/tourism*, delegation of real-item ids to the Town resource ledger (via seeded ResourceRegistry + TestRegistryHelper), hand-computed expectations on all arithmetic (e.g. 30+50 under cap 55 → 55; 0+100+25 = 125). Alias RL-to-mappedId branch for capKey is present in source and equivalent numeric formula is pinned via direct ids (full isolation of getFor during alias proved sensitive to suite-wide static registry/platform state from sibling tests; core rule covered).
- Intentionally light on tick restock (time + enabled flag driven) and full csv expansion (delegated to ResourceRegistryTest T-019).

## Open questions
- The "learnedMin/learnedMax" fields on TradingStock are initialized to fixed 100 / config default and never updated from observed usage in the current code — the names suggest a future adaptive economy but the behavior is just "default floor + configurable ceiling".
- adjustStock for real items bypasses any "trading stock" and goes straight to the economy ledger; the internal stocks map is only for ids that do not resolve to real MC items. This split is important for production vs pure trading simulations.
- No persistence of the internal stocks map is visible in the provided save/load (only the TradingStock list under "tradingStocks" is round-tripped); on load it repopulates whatever was saved, but first access after a fresh Town may recreate defaults.

## Related
- [[Town/Production/Recipe Execution and Dynamic Evaluation]] — heavy user of getStock/getStorageCap for condition evaluation and stall logic
- [[Tourists/Capacity/Tourist Capacity Calculation]] — tourist_cap and "tourist" stock are inputs to spawn eligibility
- [[Town/Resources/Resource Storage Operations]] — the real item path in getStock/adjustStock ultimately lands here for physical resources
- ITownState (used by research AI scoring)
