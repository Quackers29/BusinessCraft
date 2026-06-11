---
tags:
  - overview
  - production
---
# Production Overview

**TL;DR**: Towns produce and consume resources via configurable recipes (farming, crafting, population maintenance); upgrade nodes apply tiered effects (scaled by level) to town stats such as population caps, tourist capacity, storage, happiness, and tax rates.

## Processes in this area
- **[[Production/Upgrades/Effect Value Calculation|Effect Value Calculation]]** (T-016) — central scaling formula for upgrade effects at a given level: linear (base × level) when benefit multiplier is 1.0 (default), or exponential/compound (base × mult^(level-1)) otherwise. Used by both server logic (TownUpgradeComponent) and UI viewmodels for consistent display and application of upgrade benefits. Level ≤ 0 yields zero effect.
- **[[Production/Recipes/Estimated Effort Calculation|Estimated Effort Calculation]]** (T-025) — production recipes are walked recursively to compute an "effort in minutes per unit output" for every producible resource (cycle time plus input efforts, divided by yield, cheapest path wins); the baseline emerald price is that effort times 20. These values provide starting prices for contracts and market views and are overridden by actual GlobalMarket trading activity.
- **[[Production/Upgrades/Upgrade Registry Loading and Lookup|Upgrade Registry Loading and Lookup]]** (T-030) — loads upgrade node definitions (research minutes, resource costs, effects, prereqs, repeatability rules) from config/businesscraft/upgrades.csv (writing a minimal two-node default if the file is missing) and exposes them by ID and as a collection so the research AI, upgrade component, and status screens all see the same data.

## How it connects
Production recipes (loaded from productions.csv via ProductionRegistry) define inputs, outputs, cycle times, and conditions (happiness thresholds, pop limits, etc.). Running recipes affect town resources and population. Upgrade nodes (from upgrades.csv + requirements) provide permanent or repeatable stat changes; the effect value at the town's current level for that node is computed by the scaling formula so that higher tiers give more (or diminishing) benefit. The same scaled values feed both gameplay systems and the upgrade status UI.
