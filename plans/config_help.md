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
*   `storage_cap_[resource_id]`: Adds to the storage capacity of a **specific** resource.
    *   *Example:* `storage_cap_minecraft:wheat:500`

### Tourism
These are special calculated stats that can be used as requirements or costs.
*   `tourism_count`: The number of valid "tourist" blocks (e.g., Hotels) in the town.
*   `tourism_distance`: The cumulative distance score of tourism blocks from the town center.

### Production & Recipes
*   **Recipe Unlocks:** Use the `recipe_id` as an effect key (without value) to unlock that recipe for the town.
    *   *Example:* `population_maintenance`
*   **Speed Multipliers:** use `recipe_id*multiplier` to change the speed of a recipe.
    *   *Example:* `basic_farming*0.5` (Reduces cycle time by half / doubles speed? *Note: Implementation adds to a modifier, so higher is faster.*) 

### Town Stats
*   `happiness`: Modifies town happiness.
*   `pop`: Refers to current population count.
*   `pop_cap`: Refers to/modifies population capacity.

## Examples

**Upgrade Effect:**
```csv
storage_cap_all:200;basic_farming*1.5;happiness:10
```
*Adds 200 storage to all items, increases Basic Farming speed by 150% (additive), and adds 10 happiness.*

**Upgrade Requirement (Prerequisite):**
```csv
pop:>=20;tourism_count:>2
```
*Requires population at least 20 and at least 3 tourist arrivals.*
