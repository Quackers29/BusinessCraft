# Configuration Help

This document describes the special syntax and keys available for configuring game data in CSV files (e.g., `production_recipes.csv`, `upgrades.csv`).

## General Syntax

### Effects
Effects are typically defined as a semicolon-separated list.
*   **Format:** `key:value` or `key:value%`
*   **Simple Unlock:** Use just the `key` to treat it as a boolean unlock (e.g., enabling a recipe).
*   **Multiplier:** `key*value` (e.g., `basic_farming*0.5`) is used to modify existing values like production speed.

### Conditions / Requirements
Conditions check against town stats.
*   **Format:** `key:operatorVALUE`
*   **Operators:** `>`, `<`, `>=`, `<=`, `=`
*   **Example:** `pop:>10` (Population must be greater than 10)

### Dynamic Resources / Costs
Costs can use simple math or references to stats.
*   **Format:** `key:expression`
*   **Example:** `money:1*pop` (Cost is 1 money per population count)

## Special Keys

### Storage
*   `storage_cap_all`: Adds to the storage capacity of **all** resources.
    *   *Example:* `storage_cap_all:200` (Adds 200 to every resource's cap)
*   `storage_cap_[resource_id]`: Adds to the storage capacity of a **specific** resource (or resource group).
    *   *Example:* `storage_cap_food:300` (Sets the cap for all items mapped to "food", like wheat, bread, etc.).
    *   *Note:* The game prioritizes the ID defined in `items.csv` (e.g. `food`). If an item is not in that file, use its direct ID (e.g. `minecraft:dirt`).

### Tourism
These are special calculated stats that can be used as **requirements**.
*   `tourism`: The total number of tourists that have *historically* arrived at the town.
    *   *Note: This is different from the `tourist` production output, which increases current active count.*
*   `tourism_dist`: The cumulative distance score of tourism blocks / travel distance.

### Production & Recipes
*   **Recipe Unlocks:** Use the `recipe_id` as an effect key (without value) to unlock that recipe for the town.
    *   *Example:* `population_maintenance` - Core recipe for population upkeep.
    *   *Example:* `population_growth` - Core recipe for population growth.
    *   *Example:* `taxes` - Recipes that generate income from population.
*   **Speed Multipliers:** use `recipe_id*multiplier` to change the speed of a recipe.
    *   *Example:* `basic_farming*0.5` (Reduces cycle time by half / doubles speed).
*   **Special Outputs:**
    *   `tourist`: Adds to the active tourist count (accumulates until `tourist_cap`).

### Town Stats & Modifiers
*   `happiness`: Modifies town happiness (0-100).
*   `pop`: Refers to current population count.
*   `pop_cap`: Modifies population capacity.
*   `tourist`: Refers to current active tourist count (Note: `tourism` usually refers to historical visitation).
*   `tourist_cap`: Modifies tourist capacity.

### Biome Configuration (Starting Values)
These keys are specific to the `starting_values` column in `biomes.csv`.
*   `pop` or `population`: Sets the initial population count.
*   `happiness`: Sets the initial happiness level.
*   `tourist`: Adds directly to the "Pending Tourist Spawn" buffer. These tourists will spawn shortly after town creation.
    *   *Note: Do not use `tourist_count`, use `tourist`.*
*   `*_cap` (e.g. `pop_cap`, `tourist_cap`): Applies a **permanent** flat modifier to the town's stats (e.g. `pop_cap:10` permanently adds +10 capacity).
*   **Resources**: Any key matching an item in `items.csv` (e.g. `wood`, `food`, `money`) will add that amount to the town's starting storage.

## Examples

**Upgrade Effect:**
```csv
storage_cap_all:200;basic_farming*1.5;happiness:10
```
*Adds 200 storage to all items, increases Basic Farming speed by 150% (additive), and adds 10 happiness.*

**Upgrade Requirement (Prerequisite):**
```csv
pop:>=20;tourism:>50
```
*Requires population at least 20 and at least 50 tourists to have arrived historically.*

### Repeatable Upgrades
The `upgrades.csv` file includes a dedicated column for repeatability configuration (index 3, after Category).

*   **Format:** `[max_repeats]:[cost_multiplier]`
*   **Default:** `1` (One-time upgrade, no multiplier).
*   **Fixed Repeats:** Enter a number, e.g., `10`. The upgrade can be researched up to this level.
*   **Infinite:** Use keyword `infinite` for no limit.
*   **Cost Multiplier:** Append `:value` to specify how much the cost increases per level. Cost is calculated as `BaseCost * Multiplier^(Level-1)`.
    *   *Example:* `infinite:1.2` (Infinite repeats, 20% cost increase per level).
    *   *Example:* `5:2.0` (Max 5 levels, cost doubles each level).
