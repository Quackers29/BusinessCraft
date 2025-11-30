# BusinessCraft Town Trading Overview (Simplified: Canonical + Fuzzy Matching)

## Introduction
This document defines the trading system for abstract resources (wood, iron, coal, food, emeralds) across Forge and Fabric. Towns trade **resource units** in an autonomous economy:

- **No Player Involvement (Phase 1)**: Town-to-town trades only. Players join later via "companies".
- **Global Price Index**: Learned market prices per resource (starts 1.0 emerald/unit).
- **Tradeable Resources from CSV**: Admins define via simple `config/tradeable_items.csv` (one canonical MC item per resource).
- **Fuzzy Auto-Expansion**: Registry tags/properties auto-include equivalents (e.g., all logs for "wood", saturation-based for "food").
- **Town Autonomy**: Stocks in resource units; learns optimal levels (default max=1000 units).
- **Multi-Loader**: Architectury common; platform-specific ticks/NBT.
- **Goals**: Realistic simulation, zero-maintenance setup, processes-ready (future production CSV).

## Core Components

### 1. Tradeable Resources CSV (`config/tradeable_items.csv`)
Ultra-simple 2-column format (Excel: copy-paste item IDs):

```
resource_id,canonical_item_id
wood,minecraft:oak_log
iron,minecraft:iron_ingot
coal,minecraft:coal
food,minecraft:bread
emerald,minecraft:emerald
```

- **Loaded Once**: Server/world start. Invalid IDs skipped (logs warn).
- **Extensible**: Add `stone,minecraft:stone` → auto-tags variants.
- **Fallback**: Hardcoded defaults if missing.
- **Log Output**: "wood: canonical=oak_log (1.0) + 6 equivalents (spruce_log=1.0, etc.)"

**Excel Workflow**:
1. Add row: `tools,minecraft:iron_pickaxe`.
2. Save → `/reload` → New resource with auto-matches (tags).

### 2. Fuzzy Expansion & ResourceType (Load-Time)
Canonical = 1.0 unit. System auto-populates equivalents:

```java
public class ResourceType {
  String id;  // "wood"
  Identifier canonical;  // "minecraft:oak_log"
  Map<Identifier, Float> equivalents = new HashMap<>();  // item_id -> units per stack

  void expand() {
    Item canon = Registries.ITEM.get(canonical);
    equivalents.put(canonical, 1.0f);

    // Priority: Tags (vanilla/mod-safe)
    TagKey<Item>[] tags = getTagsForResource(id);  // e.g., wood -> minecraft:logs
    for (TagKey<Item> tag : tags) {
      for (Holder<Item> h : Registries.ITEM.getTagOrEmpty(tag)) {
        equivalents.put(Registries.ITEM.getKey(h.value()), 1.0f);
      }
    }

    // Food: Scan edible + normalize saturation
    if ("food".equals(id)) {
      Item bread = Registries.ITEM.get(Identifier.of("minecraft:bread"));
      float breadSat = bread.getFoodProperties(new ItemStack(bread)).getSaturationModifier();  // ~6.0
      for (Item item : Registries.ITEM) {
        FoodProperties fp = item.getFoodProperties(new ItemStack(item));
        if (fp != null) {
          float rate = fp.getSaturationModifier() / breadSat;
          equivalents.put(Registries.ITEM.getKey(item), rate);
        }
      }
    }

    // Heuristics fallback (name regex, e.g., ".*(ingot|nugget).*(iron|fe)")
    applyHeuristics();
  }
}
```

**Auto-Matches Examples**:
| Resource | Canonical | Equivalents (via tags/properties) |
|----------|-----------|----------------------------------|
| wood    | oak_log  | All `minecraft:logs` (spruce=1.0, etc.) |
| iron    | iron_ingot | `c:iron_ingots` or name~iron (nugget=0.1) |
| coal    | coal     | `minecraft:coals` (charcoal=0.9) |
| food    | bread    | All edible: steak~1.5x, apple~0.33x (sat-based) |
| emerald | emerald  | Exact 1.0 |

### 3. Global Price Index
Server singleton:
```java
class GlobalMarket {
  Map<String, Float> prices = new HashMap<>();  // "wood" -> 0.5
  Map<String, Long> totalVolume = new HashMap<>();
}
```
- Init: 1.0 per resource_id.
- Update: Weighted avg post-trade.
- Save: `world/businesscraft_market.dat`.

### 4. Town Stocks & Learning (NBT)
```
TradingStocks: [
  {id:"food", current:125.5, learnedMin:100.0, learnedMax:300.0},
  ...
]
```
- **Units**: Floats (fractions OK).
- **Restock**: +config_rate if <min (e.g., +0.5 food/tick).
- **Learning**: Rolling avg/stddev → min/max (clamp 64-4096).

### 5. Town-to-Town Trading Mechanics
**Simulator#tick()** (every 60 ticks):
1. **Discovery**: Nearby towns (200 blocks).
2. **Imbalance**: Excess (>max*0.8), Deficit (<min*1.2).
3. **Proposal**: Excess X for Y's excess/deficit.
   - Rate: qtyX / qtyY = priceY / priceX (e.g., 64 wood = 16 iron).
4. **Haggle**: ±10%, accept if beneficial.
5. **Execute**: Transfer units, learn prices, particles.

**Limits**: 1-5 trades/town/cycle, cooldowns.

## Data Flow Diagram
```
CSV Load --> Resource Registry [expand: tags + food scan]
                   |
World Load --> GlobalMarket (1.0) + Town Stocks (500/1000 units)
                   |
Town#tick() --> Restock (if low)
                   |
Simulator#tick()
  ├── Scan partners
  ├── Propose (price ratio)
  ├── Execute units ± --> Learn prices
                   |
Save: market.dat + Town NBT
```

## Configuration (`businesscraft-common.toml`)
```
[trading]
  enabled = true
  tick_interval = 60
  partner_radius = 200
  restock_rate = 0.5
  haggle_percent = 10.0
  bread_reference_saturation = 6.0
  default_max_stock = 1000

[trading.resources]
  use_tags = true
  use_heuristics = true
```

## Multi-Loader
- **Common**: `trading/` (ResourceType, GlobalMarket, Simulator, CSVLoader).
- **Forge**: Capabilities for NBT/ticks.
- **Fabric**: Components/ServerTickEvents.
- **CSV**: Commons-CSV lib.

## Performance
- Expand: O(registry) once (~1ms).
- Simulator: O(n log n) batched, loaded chunks only.

## Next Phases
- Production CSV (processes: wood→coal).
- Player trades (convert MC stacks → units).
- UI/Commands: `/market prices`, town ledger.

## Risks/Mitigations
- Stagnation: Random trades.
- Tag Misses: Heuristics + config overrides.
- Mod Conflicts: Tags preferred.

Elegant, admin-proof economy! 🚀
