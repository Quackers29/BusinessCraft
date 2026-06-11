---
tags:
  - detail
  - economy
---
# Resource Type Expansion and Lookup

**Breadcrumb**: Economy > Resources > Resource Type Expansion and Lookup
**TL;DR**: ResourceRegistry loads a csv (items.csv) of logical resources (wood, iron, food, ...) mapped to canonical MC items + optional base price; ResourceType.expand() then augments each with fuzzy variant Items (food by saturation ratio, wood logs, iron nuggets/blocks, coal) so that getFor(item) / getAllFor(item) / getUnitValue(item) can classify any in-game Item and return its per-unit multiplier (default 1.0 for canonical).

## What it does
The mod needs a stable way to talk about "resources" like "wood" or "food" even though players use dozens of variant items (oak_log vs birch_log, bread vs cooked_beef, iron_nugget vs iron_block). This system loads the mapping from a user-editable csv, then auto-discovers variants using simple Minecraft properties (edible + saturation) and name heuristics so storage, trading, contracts, market prices, and AI all agree on what counts as what and how much "one unit" of the abstract resource a given stack represents.

## How it works (process view)
- On startup (or explicit load) ResourceRegistry ensures `config/businesscraft/items.csv` exists (writes 5 sensible defaults if missing) and parses it into ResourceType objects (id, canonical MC ResourceLocation, basePrice default 1.0).
- It then calls expand() on every type.
- expand() seeds the equivalents map with the canonical at 1.0×, looks up the real Item, then walks the entire item registry:
  - For the special "food" resource: any edible item whose saturationModifier ratio to bread's (the default canonical) is between 0.1× and 10× gets added with that ratio (e.g. a very filling food might be 3.2).
  - For "wood"/"iron"/"coal": simple path suffix/contains rules add logs, nuggets (0.11), blocks (9.0), etc.
- Later, getFor(item) returns the first ResourceType whose equivalents contain the item's RL (or null).
- getAllFor(item) returns every match (supports "food" + specific if an item fits multiple).
- getUnitValue(item) returns the multiplier from that map (0.0 if unknown) — used for valuing partial stacks in trades/storage.
- **Worked example**: items.csv has `food,Food,minecraft:bread,1.5`. After expand, an ItemStack(Items.COOKED_BEEF, 4) whose saturation is 1.2× bread's will be recognized by getAllFor() as matching the "food" type with unit value 1.2; 4 items therefore contribute 4.8 "food units" to a town's stock calculations.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ResourceRegistry.load()` | `common/src/main/java/com/quackers29/businesscraft/economy/ResourceRegistry.java` | Ensures csv, parses lines into ResourceType, calls expand() on all, populates the static map |
| `ResourceRegistry.get(String)`, `getAll()`, `getFor(Item)`, `getAllFor(Item)` | same | Lookup by id or reverse-map any Item (getFor returns first, getAllFor collects multiples for ambiguity) |
| `ResourceType.expand()` | `common/src/main/java/com/quackers29/businesscraft/economy/ResourceType.java` | Populates the private equivalents map: canonical always 1.0 + discovered variants via food saturation or name heuristics |
| `ResourceType.getUnitValue(Item)` | same | Returns the float multiplier for that exact Item (or 0.0) from the equivalents map |
| `ResourceType.getBaseValue()` | same | Returns the csv basePrice (parsed, default 1.0); currently not used by the equiv system |
| `ConfigLoader` (indirect) | `config/ConfigLoader.java:231` | Calls ResourceRegistry.load() during its own load (after platform registries are up) |

## Rules & formulas (exact)
- CSV format (first line header): `item_id,display_name,mc_item_id,base_price` (base_price optional, defaults to 1.0f). Lines with <3 non-empty fields after trim are skipped. Malformed base_price logs warn and uses 1.0.
- `new ResourceType(id, loc, base)` stores baseValue; canonical is always inserted as `equivalents.put(canonical, 1.0f)` inside expand regardless of baseValue.
- Food rule (only when resource id equals "food"):
  ```java
  if (item.isEdible() && canonical.isEdible()) {
      float saturationRatio = foodProps.getSaturationModifier() / canonicalProps.getSaturationModifier();
      if (saturationRatio > 0.1f && saturationRatio < 10.0f) equivalents.put(itemId, saturationRatio);
  }
  ```
- Heuristics (only for the matching resource id, after the food block):
  - wood: path endsWith "_log" or "_wood" → 1.0f
  - iron: "iron_ingot" →1, "iron_nugget"→0.11f, "iron_block"→9.0f
  - coal: contains("coal") && !contains("block") && !contains("ore") →1.0f
- getFor: first type whose equivalents.keySet contains the item's RL (iteration order of the HashMap, i.e. insertion = csv order).
- getAllFor: collects every type that matches (order = csv order of the types).
- getUnitValue: `equivalents.getOrDefault(registry.getItemKey(item), 0.0f)`
- expand() is idempotent per instance only if called once (it always adds canonical again, but HashMap put overwrites same key with 1.0f); called exactly once per load.
- Registry is a static HashMap; load() does `RESOURCES.clear()` first.
- Unknown / AIR canonical → warn, skip adding variants, but the ResourceType is still registered (get() will return it with only canonical if it was added before the fail).

## Edge cases & behaviors
- Non-existent csv dir/file: parent dirs created, default 5-resource csv written (wood@0.5, iron@2, coal@1, food@1.5, money/emerald@5).
- Item not in any equivalents → getFor returns null, getAllFor returns empty list, getUnitValue returns 0.0.
- Canonical itself is always present at 1.0 even if registry lookup for it failed (the put happens before the null/AIR check).
- Food saturation exactly 0.1 or 10.0 is excluded (strict > / <).
- A food item whose canonical saturation is 0 (or missing props) will cause /0 → NaN or Inf; the ratio check will likely exclude it (current code does not guard).
- Same physical item can match multiple resources only if two ResourceTypes both claim it in their expand (rare, but getAllFor supports it — used by MarketViewModelBuilder to handle "food" vs concrete).
- Registry load order = csv order; getFor "first match" therefore prefers earlier csv entries.
- basePrice from csv is stored and readable via getBaseValue() but is never consulted by expand, getUnitValue, getFor etc. (appears vestigial or reserved for future pricing layer).
- expand() logs at INFO level the final equivalent count per resource.
- getItems() / getItem calls go through the installed RegistryHelper (normally the loader's); in unit tests replaced by TestRegistryHelper + BuiltInRegistries.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/economy/ResourceRegistryTest.java`
- Covered: default csv creation, parsing (ids, base prices, skips), direct ResourceType construction + expand with McBootstrap+TestRegistryHelper, food saturation ratio selection + bounds, wood/iron/coal heuristics (exact multipliers for nugget/block), canonical always 1.0, getFor/getAllFor single vs multi, getUnitValue fallback 0 and multiplier, unknown items, AIR handling, getAll order, getBaseValue exposure.
- Not covered: full runtime load side-effects on real config dir (exercised indirectly via ConfigLoaderTest), concurrent load, custom user csv content beyond defaults, integration with GlobalMarket / storage valuation.
- Uses @BeforeEach/@AfterEach to save+restore PlatformAccess.platform + .registry (and reflection to snapshot/restore the private RESOURCES map for isolation). McBootstrap in @BeforeAll for registry population. 15 tests.

## Open questions
- baseValue / basePrice is parsed and stored but never influences the unit-value or matching logic — is it dead, or intended for a future "intrinsic price" separate from GlobalMarket dynamic prices and the equiv multipliers?
- Food ratio uses the canonical's saturation as divisor with no zero guard; if a csv points "food" at a non-edible canonical the division can produce NaN and all food ratios become unusable.
- Heuristics are hardcoded and only trigger for exact resource ids "wood"/"iron"/"coal"/"food". Adding a new resource type with variants requires code change (or future data-driven rules).
- getFor returns "first" by csv iteration order (HashMap); getAllFor is the safe multi API. Callers mix both (some only need one, some use getAllFor then pick).
- Registry is global static; two calls to load() with different csv contents will stomp each other. No unload or namespaced registries.
- The default csv is written with "money,Emeralds,minecraft:emerald,5.0" but emerald is also the currencyItem in ConfigLoader — potential overlap with pure-money handling.

## Related
- [[Economy/Economy Overview]]
- [[Trade/Global Market/Price Calculation]] (T-006 — uses getAllFor for client price mapping)
- [[Town/Resources/Resource Storage Operations]] (T-007 — TownResources / trading use getFor to map items)
- [[Trade/Contracts/Sell Contract Lifecycle]] (T-004 — contract items resolve resource via registry)
