# BusinessCraft Town Trading Overview CURRENT IMPLEMENTATION (Simplified: Canonical + Fuzzy Matching)

## Introduction
This document defines the trading system for abstract resources (wood, iron, coal, food, emeralds) across Forge and Fabric. Towns trade **resource units** in an autonomous economy:

- **No Player Involvement (Phase 1)**: Town-to-town trades only. ✅ **IMPLEMENTED (via contracts)**
- **Global Price Index**: Learned market prices per resource (starts 1.0 emerald/unit). ✅ **IMPLEMENTED**
- **Tradeable Resources from CSV**: Admins define via simple `config/tradeable_items.csv` (one canonical MC item per resource). ✅ **IMPLEMENTED**
- **Fuzzy Auto-Expansion**: Registry tags/properties auto-include equivalents (e.g., all logs for "wood", saturation-based for "food"). ❌ **NOT IMPLEMENTED**
- **Town Autonomy**: Stocks in resource units; learns optimal levels (default max=1000 units). ✅ **PARTIALLY IMPLEMENTED**
- **Multi-Loader**: Architectury common; platform-specific ticks/NBT. ✅ **IMPLEMENTED**
- **Goals**: Realistic simulation, zero-maintenance setup, processes-ready (future production CSV). ✅ **FOUNDATION COMPLETE**

**IMPLEMENTATION STATUS**: ~60% Complete - Market and resources work, but no actual town-to-town trading simulator.

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

- **Loaded Once**: Server/world start. ✅ **IMPLEMENTED**
- **Extensible**: Add `stone,minecraft:stone` → auto-tags variants. ❌ **NO FUZZY EXPANSION**
- **Fallback**: Hardcoded defaults if missing. ✅ **IMPLEMENTED**
- **Log Output**: "wood: canonical=oak_log (1.0) + 6 equivalents (spruce_log=1.0, etc.)" ❌ **NOT IMPLEMENTED**

**Excel Workflow**: ✅ **WORKS FOR BASIC RESOURCES**

### 2. Fuzzy Expansion & ResourceType (Load-Time)
Canonical = 1.0 unit. System auto-populates equivalents: ❌ **NOT IMPLEMENTED**

**CURRENT IMPLEMENTATION**: Only exact canonical items are tradeable.

**Auto-Matches Examples** (PLANNED BUT NOT IMPLEMENTED):
| Resource | Canonical | Equivalents (via tags/properties) |
|----------|-----------|----------------------------------|
| wood    | oak_log  | All `minecraft:logs` (spruce=1.0, etc.) |
| iron    | iron_ingot | `c:iron_ingots` or name~iron (nugget=0.1) |
| coal    | coal     | `minecraft:coals` (charcoal=0.9) |
| food    | bread    | All edible: steak~1.5x, apple~0.33x (sat-based) |
| emerald | emerald  | Exact 1.0 |

### 3. Global Price Index
Server singleton: ✅ **IMPLEMENTED**
```java
class GlobalMarket {
  Map<String, Float> prices = new HashMap<>();  // "wood" -> 0.5
  Map<String, Long> totalVolume = new HashMap<>();
}
```
- Init: 1.0 per resource_id. ✅ **IMPLEMENTED**
- Update: Weighted avg post-trade. ✅ **IMPLEMENTED**
- Save: `world/businesscraft_market.dat`. ❌ **NOT IMPLEMENTED - NO PERSISTENCE**

### 4. Town Stocks & Learning (NBT)
```
TradingStocks: [
  {id:"food", current:125.5, learnedMin:100.0, learnedMax:300.0},
  ...
]
```
- **Units**: Floats (fractions OK). ✅ **IMPLEMENTED**
- **Restock**: +config_rate if <min (e.g., +0.5 food/tick). ✅ **IMPLEMENTED**
- **Learning**: Rolling avg/stddev → min/max (clamp 64-4096). ❌ **NOT IMPLEMENTED - NO LEARNING LOGIC**

### 5. Town-to-Town Trading Mechanics
**Simulator#tick()** (every 60 ticks): ❌ **NOT IMPLEMENTED**

**CURRENT TRADING**: Via contract system, not direct town-to-town trades.

**Planned but not implemented:**
1. **Discovery**: Nearby towns (200 blocks).
2. **Imbalance**: Excess (>max*0.8), Deficit (<min*1.2).
3. **Proposal**: Excess X for Y's excess/deficit.
4. **Haggle**: ±10%, accept if beneficial.
5. **Execute**: Transfer units, learn prices, particles.

**Limits**: 1-5 trades/town/cycle, cooldowns. ❌ **NOT IMPLEMENTED**

## Data Flow Diagram
**CURRENT IMPLEMENTATION:**
```
CSV Load --> Resource Registry [NO fuzzy expansion]
                   |
World Load --> GlobalMarket (1.0) + Town Stocks (restock only)
                   |
Town#tick() --> Restock (if low) - NO LEARNING
                   |
Contract System --> Town-to-Town via Auctions - WORKS
  ↓
GlobalMarket records prices - WORKS BUT NO PERSISTENCE
```

**PLANNED BUT NOT IMPLEMENTED:**
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
**CURRENT CONFIG:**
```
[trading]
  enabled = true
  tick_interval = 60         - NOT USED (no simulator)
  partner_radius = 200       - NOT USED
  restock_rate = 0.5         - IMPLEMENTED
  haggle_percent = 10.0      - NOT USED
  bread_reference_saturation = 6.0 - NOT USED
  default_max_stock = 1000   - IMPLEMENTED

[trading.resources]
  use_tags = true            - NOT IMPLEMENTED
  use_heuristics = true      - NOT IMPLEMENTED
```

## Multi-Loader
- **Common**: `trading/` (ResourceType, GlobalMarket, Simulator, CSVLoader). ✅ **PARTIALLY IMPLEMENTED**
- **Forge**: Capabilities for NBT/ticks. ✅ **IMPLEMENTED**
- **Fabric**: Components/ServerTickEvents. ✅ **IMPLEMENTED**
- **CSV**: Commons-CSV lib. ✅ **IMPLEMENTED**

## Performance
- Expand: O(registry) once (~1ms). ❌ **NO EXPANSION**
- Simulator: O(n log n) batched, loaded chunks only. ❌ **NO SIMULATOR**

## Next Phases (Actually Missing)
- Fuzzy resource expansion using tags
- Town-to-town direct trading simulator
- Stock learning system
- Market data persistence
- Haggle mechanics
- Player trades (convert MC stacks → units)
- UI/Commands: `/market prices`, town ledger

## Risks/Mitigations
- **Stagnation**: Random trades. ❌ **MITIGATED BY CONTRACT SYSTEM**
- **Tag Misses**: Heuristics + config overrides. ❌ **NO HEURISTICS**
- **Mod Conflicts**: Tags preferred. ❌ **EXACT MATCHES ONLY**

**CURRENT STATUS**: Foundation solid, but trading is contract-based, not direct town-to-town. Market learns prices from contract auctions. 🚧
