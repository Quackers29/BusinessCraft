# Admin Config Manual

BusinessCraft is highly configurable through CSV files located in `config/businesscraft/`.

## 1. File Reference

| File | Purpose | Default Location |
| see | --- | --- |
| `items.csv` | Defines all resources the town can use, store, and trade. | `config/businesscraft/items.csv` |
| `productions.csv` | Defines recipes for the Production System. | `config/businesscraft/productions.csv` |
| `upgrades.csv` | Defines the Tech Tree / Upgrade system. | `config/businesscraft/upgrades.csv` |
| `biomes.csv` | Defines starting stats for towns based on biome. | `config/businesscraft/biomes.csv` |

## 2. Items Configuration (`items.csv`)
Defines the mapping between abstract "Resource Units" and physical Minecraft items.
*   **Format**: `item_id,display_name,mc_item_id`
*   **Example**: `wood,Wood Logs,minecraft:oak_log`
*   **Note**: The system uses "Fuzzy Matching". Defining `oak_log` generally works for all `minecraft:logs` via tags.

## 3. Production Configuration (`productions.csv`)
Defines recipes.
*   **Format**: `id,display_name,cycle_time,inputs,outputs,conditions`
*   **Inputs**: `item_id:amount` (semicolon separated).
*   **Outputs**: `item_id:amount`.
*   **Conditions**: e.g., `pop:>5` (See Syntax below).

## 4. Upgrades Configuration (`upgrades.csv`)
Defines the tech tree.
*   **Format**: `id,display_name,category,repeatable,research_time,effects,requirements,costs`
*   **Repeatable**: `1` (single), `10` (max 10), `infinite`.
*   **Effects**: `key:value` pairs (See Syntax below).

## 5. Syntax Guide

### Repeatable Upgrades
In `upgrades.csv` (Column 4), you can define complex scaling for upgrades.
*   **Format:** `[Max Repeats]:[Cost Scaler]:[Benefit Scaler]`
*   **Examples:**
    *   `10:1.2` -> Max 10 levels, Cost & Time multiplies by 1.2 (+20%) each level.
    *   `infinite:1.5:1.01` -> Infinite levels, Cost & Time x1.5 per level, Benefit x1.01 per level.
*   **Cost & Time Logic:**
    *   **Compound (Standard)**: `Base * Scaler^Level`.
    *   The `^` syntax is no longer required or supported. All multipliers are treated as Compound multipliers.
    *   Note: The Cost Scaler now scales **both** the Resource Cost and the Research Time.
*   **Benefit Logic:**
    *   **Linear (Default, No 3rd param)**: `Base * Level`. (Adds base value each level).
    *   **Exponential (If 3rd param != 1.0)**: `Base * Scaler^(Level-1)`. (Replaces previous value with new exponential value).
        *   Level 1: Base
        *   Level 2: Base * Scaler
        *   Level 3: Base * Scaler^2

### Effects
Effects modify town stats or unlock capabilities.
*   **Format:** `key:value` or `key:value%`
*   **Simple Unlock:** Use just the key (e.g., `unlock_smelting`) to treat it as a boolean unlock.
*   **Additive Modifier:** `key:value` (e.g., `storage_cap_food:200` adds 200).
*   **Multiplicative Modifier:** `key*value` (e.g., `basic_farming*0.5`) multiplies the base value.
    *   *Note:* Use `*` for speed multipliers where a lower value is better (0.5 = double speed).

### Conditions / Requirements
Used in `productions.csv` (Conditions) and `upgrades.csv` (Requirements).
*   **Format:** `key:operatorVALUE`
*   **Operators:** `>`, `<`, `>=`, `<=`, `=`
*   **Special Values (Calculated from Storage Capacity):**
    *   `min`: Checks against Min Stock % (Config `minStockPercent`, Default 60%).
    *   `excess`: Checks against Excess Stock % (Config `excessStockPercent`, Default 80%).
    *   `X%`: Checks against X% of Capacity.
*   **Examples:**
    *   `pop:>10` (Pop > 10).
    *   `wood:>500` (Wood > 500).
    *   `wood:>min` (Wood > 60% of Wood Cap).
    *   `iron:<10%` (Iron < 10% of Iron Cap).

### Global Modifiers (Production Only)
In `productions.csv` Inputs column, you can add "Global Modifiers" that apply to **all inputs** in that list.
*   **Syntax:** Add `;min`, `;excess`, or `;X%` to the input list.
*   **Behavior:** Adds a `> [Value]` condition for every input resource.
*   **Example:** `wood:25;min` -> Requires Input `wood:25` AND Condition `wood > min`.
*   **Example:** `wood:25;iron:5;10%` -> Requires Inputs Wood/Iron AND Conditions `wood > 10% cap` and `iron > 10% cap`.

### Dynamic Costs & Expressions
Used for upgrade costs or recipe inputs.
*   **Format:** `key:expression`
*   **Expression Support:**
    *   Fixed Value: `wood:10`
    *   Dynamic Calculation: `money:1*pop` (1 Emerald per Population).

## Special Keys Reference

### Storage
*   `storage_cap_all`: Adds to the storage capacity of **all** resources.
*   `storage_cap_[id]`: Adds to specific resource cap (e.g. `storage_cap_food`).

### Stats
*   `happiness`: Town happiness (0-100).
*   `pop` / `population`: Current Population count.
*   `pop_cap`: Population Capacity (Soft limit).
*   `tourist`: Current active Tourist count.
*   `tourist_cap`: Tourist Capacity.
*   `tourism`: Historical total of tourists (Only used in Conditions).
*   `border`: Town boundary radius in blocks (Default: 50).

### Production Modifiers
*   `[recipe_id]*[val]`: Modifies recipe cycle time (Speed).
    *   `farming*0.8` = 20% Faster.
*   `[recipe_id]-input`: Modifies input cost.
*   `[recipe_id]-output`: Modifies output amount.

### Research
*   `research`: Multiplier for research progress (Default: 1.0).
    *   `research:0.5` -> Adds +50% speed (Total 1.5x).
    *   Effect is additive: `1.0 + Unlocks`.

### Production Stats Output
You can make a recipe output a stat change instead of an item by using the stat key (e.g. `happiness`, `border`) in the `outputs` column.
*   **Additive Only**: These outputs add to the flat modifier of the stat per cycle.
*   **Example**: `border:1` -> Adds +1 Block to border radius every time the recipe completes.
*   **Dynamic**: `border:border*0.1` -> Adds 10% of current border to the flat modifier (Exponential growth).

> [!WARNING]
> **Naming Collisions**
> Do not name a Production Recipe ID the same as a Stat Key (e.g. `border`).
> If a Stat (like `border`) has a value > 0, the system may misinterpret it as "Recipe Unlocked" if they share the same ID.
> **Best Practice**: Use `border_expansion` for the recipe ID, and `border` for the output/stat key.

### Biome Configuration
Keys used in `biomes.csv` to set starting values.
*   `pop`: Starting population.
*   `happiness`: Starting happiness.
*   `[item_id]`: Starting resource amount (e.g., `wood:100`).
*   `[stat]_cap`: Permanent modifier to caps (e.g., `pop_cap:10`).

## 6. AI Wants Logic

Starting around version 3.0, the AI uses dynamic logic to prioritize (Want) certain upgrades.

### Research Speed
The AI calculates a "Want" (Priority 0-100) for Research Speed upgrades based on the **Average Time for Top 3 Next Level (Scaled)**.
*   **Formula**: `Average(Top 3 Longest Available Researches)`.
*   **Inclusions**: Scaled time for the *next level* of all available non-maxed upgrades.
*   **Exclusions**: Upgrades that provide Research Speed themselves (to avoid circular dependence).
*   **Behavior**:
    *   Focuses heavily on the most expensive researches available.
    *   If you have one 100m research and many 1m researches, the average will be weighted significantly by the 100m one (e.g. `(100+1+1)/3 = 34` vs `(100+50+1s)/52` previously).
    *   100 minutes average = ~100 Priority (Max).

### Border Expansion
The AI prioritizes Border Expansion based on **Population Density (Area) relative to Starting Density**.
*   **Formula**: `Priority = (CrowdingRatio - 1.0) * 100`.
*   **Crowding Ratio**: `CurrentAreaDensity / BaselineAreaDensity`.
    *   **Area Density**: `Pop / (BorderRadius^2)`.
*   **Behavior**:
    *   If density matches start (Ratio 1.0), Priority is 0 (Balanced).
    *   If population doubles while border stays same (Ratio 2.0), Priority is 100 (Urgent).
    *   The "Want" scales proportionally with Area Increase requirements.

### Population / Tourists
The AI prioritizes these "Accumulation" stats inversely to how full they are.
*   **Formula**: `100 * (1.0 - Fullness)`.
*   **Fullness**: `Current / Cap`.
*   **Behavior**: Empty capacity = High Priority. Full capacity = Low Priority.

### Storage Capacity
The AI prioritizes increasing storage capacity when current storage is nearing full.
*   **Formula**: `100 * (Current / Cap)^2`.
*   **Behavior**:
    *   If storage is full (100%), Priority is 100 (Urgent).
    *   If storage is half full (50%), Priority is 25 (Low).
    *   This ensures the AI doesn't waste resources upgrading storage it isn't using.

### Production Speed (Supply vs Demand)
For standard production recipes, the AI balances Production against Consumption.
*   **Formula**: `50 * (2.0 - Ratio)`.
*   **Ratio**: `ProductionRate / ConsumptionRate`.
*   **Behavior**:
    *   **Critical Deficit** (Ratio 0): If consuming but not producing, Priority is 100.
    *   **Balanced** (Ratio 1): If production matches consumption, Priority is 50.
    *   **Surplus** (Ratio >= 2): If producing 2x consumption, Priority is 0.
*   **Special Case**: If storage is full (>95%), Priority is penalized (-50) to avoid overproduction.
