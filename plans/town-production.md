# BusinessCraft Town Production & Upgrades Plan

## Introduction
This plan adds **production and upgrades** to towns, generating/consuming resources to create **organic trade needs**. Integrates with `trading-overview.md`:

- **Base Town**: Grows population (consumes food) until cap.
- **Upgrades**: Buildings/jobs like \"wood_farm\", \"wheat_farm\" – net produce/consume resource units.
- **Spawn**: Random 1-3 basic upgrades (weighted by CSV).
- **Progression**: Pop enables advanced upgrades (e.g., mines at pop>50).
- **Configurable**: `town_upgrades.csv` + TOML.
- **Multi-Loader**: Common in `production/` pkg.
- **Economy Link**: Excess production → trade deficits.

**Goals**: Dynamic towns, admin-easy, perf-light.

## Core Components

### 1. Town NBT Extension (ProductionData)
```
ProductionData: {
  population: 5.0,           // current pop (float)
  max_population: 100.0,     // cap (config/pop upgrades future)
  upgrades: ["wood_farm", "food_farm"],  // active List<String>
  upgrade_levels: {"wood_farm": 1},      // future levels
  last_prod_time: 0L         // game ticks
}
```
- Defaults: pop=5.0, upgrades=[].
- Integrate: Town#read/writeAdditionalData.

### 2. Upgrades CSV (`config/town_upgrades.csv`)
Defines behaviors (Excel):

```
upgrade_id,input_resources,output_resources,pop_required,base_rate,spawn_weight
wood_farm,"",wood=0.5,0,0.5,10
wheat_farm,"",food=0.4,0,0.4,12
population_growth,"food=-0.01","",0,0.1,100
coal_mine,"wood=-0.2,iron=-0.1",coal=1.0,50,0.8,5
iron_mine,"food=-0.5,wood=-1.0",iron=0.4,75,0.4,3
bread_bakery,"food=-2.0",food=2.5,20,0.2,8
```

- **Parser**: "res=amt" → Map<String, Float>.
- **Negative**: Consume (inputs).
- **Positive**: Produce (outputs).
- **spawn_weight**: For random selection.
- Fallback: Hardcode basics.

### 3. Spawn Logic (Town Constructor/Init)
```java
List<String> candidates = loadSpawnCandidates();  // CSV or config
int numUpgrades = randBetween(config.min_spawn, config.max_spawn);  // 1-3
List<String> selected = weightedRandomSelect(candidates, numUpgrades);
town.upgrades.addAll(selected);
```
- Ensures basics (high-weight pop_growth, farms).

### 4. Production Tick (in Town#tick())
Perf: Every `config.interval` ticks (default 20):

#### A. Dynamic Happiness
- **Formula**: `Base Happiness = (Food / FoodStorageCap) * 50`.
- **Total**: `Base + Upgrades`.
- **Effect**: Controls unlock access and population growth eligibility.

#### B. Production Engine
Iterates active recipes defined in `productions.csv`:
```csv
id,name,time,conditions,inputs,outputs
population_growth,Growth,0.03,"surplus:food;happiness:>60;pop:<pop_cap",,"population:1"
```
- **Conditions**: 
  - `surplus:<id>`: Passes if global production rate > consumption rate for item.
  - `happiness:>X`, `pop:<limit>`.
- **Execution**:
  - If conditions met and inputs available → Progress increases.
  - On completion → Inputs consumed, Outputs produced.

#### C. Population Maintenance
- **Recipe**: `population_maintenance` (always active).
- **Input**: `pop*food:1`.
- **Logic**: Consumes food scaling with population.
- **Starvation**: If inputs missing, partial food is consumed, but happiness falls naturally due to the Dynamic Happiness formula (low food = low happiness).

### 5. Trading Integration
- **Imbalances**: Farms build wood/food excess → trade for coal/iron.
- **Stock Learning**: Producers auto-raise learnedMax.
- **Upgrade Signals**: Low coal → prioritize coal_mine spawn (future).

## Data Flow
```
Town Spawn
  ↓ Random Upgrades (CSV weights)
Initial: pop=5, farms running
  ↓
Town#tick (20t)
  ├── Run Upgrades (pop-scaled prod/consume)
  ├── Pop Growth (food check)
  └── Stock Changes → Trading eligible
  ↓
MarketSimulator trades excesses/deficits
```

## Configuration (`businesscraft-common.toml`)
```
[production]
  enabled = true
  tick_interval = 20
  pop_growth_rate = 0.01
  pop_food_threshold = 10.0
  pop_food_cost_per_unit = 0.1
  max_population_base = 100.0
  min_spawn_upgrades = 1
  max_spawn_upgrades = 3
```

## Multi-Loader
- Common: `production.Upgrade`, CSVLoader, tick methods.
- Forge: Town Capability.
- Fabric: Town Component.

## Performance
- Per-town: O(10 upgrades) every 20 ticks.
- Batched for loaded towns.

## Example Run (1000 ticks ~50s)
```
Spawn: pop=5, ["wood_farm", "food_farm", "population_growth"]
T=200: wood=50, food=30, pop=8
T=1000: wood=250 (excess), food=150, pop=20 → trades wood for iron
```

## Next
- Advanced progression (player-built upgrades).
- UI: `/town info <town>`.
- Happiness (food→growth bonus).

Risks: Starvation (fallback restock), overgrowth (trading sinks).

Living economy starter! 🚀
