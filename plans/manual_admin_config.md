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
    *   `10:1.2` -> Max 10 levels, Cost increases +20% (Linear) per level.
    *   `10:^1.2` -> Max 10 levels, Cost multiplies by 1.2 (Exponential) per level.
    *   `infinite:^1.5:1.01` -> Infinite levels, Cost x1.5 per level, Benefit x1.01 per level.
*   **Cost Logic:**
    *   **Linear (Default)**: `Base * (1 + (Scaler-1) * Level)`. (e.g. 1.2 = +20% each level).
    *   **Exponential (`^`)**: `Base * Scaler^Level`.
*   **Benefit Logic:**
    *   **Linear (Default, No 3rd param)**: `Base * Level`. (Adds base value each level).
    *   **Exponential (If 3rd param != 1.0)**: `Base * Scaler^Level`. (Compounds value).

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

### Production Modifiers
*   `[recipe_id]*[val]`: Modifies recipe cycle time (Speed).
    *   `farming*0.8` = 20% Faster.
*   `[recipe_id]-input`: Modifies input cost.
*   `[recipe_id]-output`: Modifies output amount.

### Biome Configuration
Keys used in `biomes.csv` to set starting values.
*   `pop`: Starting population.
*   `happiness`: Starting happiness.
*   `[item_id]`: Starting resource amount (e.g., `wood:100`).
*   `[stat]_cap`: Permanent modifier to caps (e.g., `pop_cap:10`).
