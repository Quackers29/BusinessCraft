# Production & Upgrades Implementation Plan

This plan outlines the technical steps to transform the current hardcoded/linear production system into the data-driven, modifier-based architecture described in `production and upgrades.md`.

## Goal
Replace the current `TownProductionComponent` + `Upgrade` (legacy) system with three distinct systems:
1.  **Resources & Stats**: Enhanced storage with limits, happiness, and population logic.
2.  **Production Engine**: Recipe-based processing (`productions.csv`) with `cycle_time` and conditions.
3.  **Upgrade System**: Research tree (`upgrades.csv`) providing modifiers (effects) to the other systems.

## User Review Required
> [!IMPORTANT]
> **Data Migration**: This change completely replaces the configuration system. Existing worlds may lose progress or look broken until `biomes.csv` and `upgrades.csv` are properly tuned.
> **Breaking Changes**: `tradeable_items.csv` will be replaced by `items.csv`. `ResourceRegistry` will change significantly.

## Divergences & Clarifications
1.  **Production Stalling vs Wasting**: The original plan says "Excess is wasted". This implementation plan defaults to **Stalling** (not running) if there is no output space, to prevent draining inputs for no gain. This can be changed to "Run and Waste" if desired.
2.  **Research Logic**: The plan implies "research" requires time and items. We are splitting this into a `TownUpgradeComponent` that explicitly tracking "Research Progress" separate from production.
3.  **Caps Default**: We assume storage starts at 0 and is granted by the `starting_nodes` (e.g. `basic_settlement`) just like the plan, but we'll need a fallback small buffer (e.g. 10) to allow initial bootstrap if config is broken.


## Proposed Changes

### 1. Data Layer (Registries)
All data will be loaded from `config/businesscraft/`.

#### [NEW] `com.quackers29.businesscraft.data.parsers`
Create generic parsers for the "packed string" formats used in the plan (e.g. `item:5;pop:10%`).
- `EffectParser`: Parses strings like `storage_cap_food:20%`, `basic_farming-time:-0.5`. returns `Effect` objects (target, operation, value).
- `ConditionParser`: Parses `happiness:>60`, `pop:<pop_cap`.

#### [MODIFY] `com.quackers29.businesscraft.economy.ResourceRegistry` reads `items.csv`
- **Schema**: `item_id`, `display_name`, `mc_item_id`
- **Logic**: Map `item_id` (internal) to `mc_item_id` (Minecraft Item/Block).

#### [NEW] `com.quackers29.businesscraft.production.ProductionRegistry` reads `productions.csv`
- **Class `ProductionRecipe`**: `id`, `displayName`, `baseCycleTime`, `inputs` (List<ResourceAmount>), `outputs` (List<ResourceAmount>), `conditions` (List<Condition>).
- **Note**: `inputs` can be standard items or special keys like `pop*food`.

#### [NEW] `com.quackers29.businesscraft.production.UpgradeRegistry` reads `upgrades.csv` & `upgrade_requirements.csv`
- **Class `UpgradeNode`**: `id`, `prereqs` (List<String>), `effects` (List<Effect>), `researchTime`, `costs` (List<ResourceAmount>).

#### [NEW] `com.quackers29.businesscraft.world.BiomeRegistry` reads `biomes.csv`
- **Class `BiomeKit`**: `biomeId`, `startingNodes` (List<String>), `startingValues` (Map<String, Float>).

---

### 2. Town Core Updates

#### [MODIFY] `com.quackers29.businesscraft.town.Town`
Add core tracked fields:
- `happiness` (0-100)
- `populationCap` (derived from modifiers)
- `touristRate` (base + modifiers)
- Methods to get "Effective Value" for any stat, consulting the `TownUpgradeComponent`.

#### [MODIFY] `com.quackers29.businesscraft.town.components.TownResources`
- Implement **Storage Caps**.
- `Map<String, Integer> storageCaps`: default 0.
- `getStorageCap(resourceId)`: returns global_cap + specific_cap (from Upgrades).
- `addResource(...)`: Reject if full. Return amount added.

### 3. Upgrade & Research System (The "Brain")

#### [NEW] `com.quackers29.businesscraft.town.components.TownUpgradeComponent`
Replaces legacy `activeUpgrades` list.
- **State**:
  - `unlockedNodes`: Set<String>
  - `activeModifiers`: Map<String, Float> (Pre-calculated sum of effects from unlocked nodes)
  - `currentResearch`: String (nodeId) + `progress` (days)
- **Logic**:
  - `tick()`: Advances research if costs were paid.
  - `getModifier(target)`: Returns sum of modifiers for a target (e.g. `wood_to_planks-time`).
  - `recalculateModifiers()`: Called when a new node is unlocked. Iterates all nodes -> aggregates effects into `activeModifiers`.

### 4. Production Engine (The "Heart")

#### [MODIFY] `com.quackers29.businesscraft.town.components.TownProductionComponent`
Complete rewrite.
- **State**:
  - `recipeProgress`: Map<String (prod_id), Float (days_accumulated)>
- **Logic `tick()`**:
  - Iterate all *unlocked* recipes.
  - **Dynamic Happiness**: Every 20 ticks, update `baseHappiness = (food / food_cap) * 50`.
  - **Check Conditions**: 
    - `happiness`, `pop_cap` matches.
    - `surplus:<resource>`: Checks if `ProductionRate(resource) > ConsumptionRate(resource)`.
  - **Check Inputs/Outputs**:
    - Calculate **Base Cycle Time** + `prod_id-time` modifier.
    - Calculate **Input Costs** + `prod_id-input` modifier.
    - Calculate **Output Amounts** + `prod_id-output` modifier.
  - Check `TownResources` has inputs and space for outputs.
  - If all good: `progress += daily_tick_interval_fraction`.
  - If `progress >= cycle_time`:
    - Consume inputs (handle `pop*` scaling).
    - Produce outputs.
    - Reset progress.

---

### 5. Initialization

#### [MODIFY] `TownManager.registerTown` / `TownInterfaceEntity`
- Detect Biome.
- Fetch `BiomeKit`.
- Apply `startingvalues` (resources, pop).
- Unlock `startingNodes` immediately via `TownUpgradeComponent`.

## Verification Plan

### Automated Tests
- Unit tests for `EffectParser` to ensure `pop_cap:15%` and `storage_cap_food:200` parse correctly.
- Unit test for `UpgradeRegistry` linking requirements to nodes.

### Manual Verification
1.  **Config Load**: Launch game, minimal config files generated?
2.  **Town Creation**: Place Town Hub in Plains. Check "Plains" kit applied (e.g. wheat seeds, basic pop).
3.  **Production**: Watch "Basic Farming". 
    - Verify it cycles only when cycle time hits.
    - Verify it stops if storage full.
4.  **Upgrades**:
    - Check UI for "Research" tab (might need basic UI implementation or debug command).
    - Trigger an upgrade (e.g. `farming_improved`) via command or UI.
    - Verify `farming_basic` production speeds up or output increases.

## Implementation Adjustments & Fixes (Post-Debugging)
1.  **Town Ticking**: 
    - Critical: `Town.tick()` **must** explicitly call `upgrades.tick()` alongside other components. Unlike `TownProductionComponent` which might be optional, upgrades drive the AI and stats.
    - Added `upgrades.tick()` to the main loop to ensure research progress occurs.

2.  **Resource Storage Bridging**: 
    - Problem: `TownTradingComponent` maintained a "Virtual Stock" (`stocks` map) which was disconnected from the actual `TownEconomyComponent` resource storage (`TownResources`).
    - Fix: `TownTradingComponent.getStock(id)` and `adjustStock(id)` were modified to check `ResourceRegistry`. If the ID maps to a real Minecraft item (e.g., `wood` -> `oak_log`), it directly accesses `TownEconomyComponent`'s inventory. This allows the Research AI to "see" and consume physical items collected by the town.

3.  **UI Feedback**:
    - Tooltips added to Production and Upgrade tabs to visualize:
        - Research Progress.
        - Production Cycle Times (Base vs Actual).
        - Active Effects.

4.  **AI Standardization**:
    - Scoring logic unified to a 0-100 scale.
    - Added "Patience" logic to prevent impulse buying of cheap upgrades.
    - Differentiated "Deficit" logic (for consumables) from "Accumulation" logic (for Population/Tourists).
