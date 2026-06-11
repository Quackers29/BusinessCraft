---
tags:
  - overview
  - production
---
# Production Overview

**TL;DR**: Towns produce and consume resources via configurable recipes (farming, crafting, population maintenance); upgrade nodes apply tiered effects (scaled by level) to town stats such as population caps, tourist capacity, storage, happiness, and tax rates.

## Processes in this area
- **[[Production/Upgrades/Effect Value Calculation|Effect Value Calculation]]** (T-016) — The central rule for how strong an upgrade's benefit is at each level: most upgrades give the same amount per level, while some are designed to compound and snowball as they level up. The same rule drives both gameplay and what the upgrade screen displays, so they always agree. Un-researched upgrades give nothing.
- **[[Production/Recipes/Estimated Effort Calculation|Estimated Effort Calculation]]** (T-025) — production recipes are walked recursively to compute an "effort in minutes per unit output" for every producible resource (cycle time plus input efforts, divided by yield, cheapest path wins); the baseline emerald price is that effort times 20. These values provide starting prices for contracts and market views and are overridden by actual GlobalMarket trading activity.
- **[[Production/Upgrades/Upgrade Registry Loading and Lookup|Upgrade Registry Loading and Lookup]]** (T-030) — loads upgrade node definitions (research minutes, resource costs, effects, prereqs, repeatability rules) from config/businesscraft/upgrades.csv (writing a minimal two-node default if the file is missing) and exposes them by ID and as a collection so the research AI, upgrade component, and status screens all see the same data.
- **[[Production/Upgrades/Upgrade Cost and Research Time Scaling|Upgrade Cost and Research Time Scaling]]** (T-032) — each next tier (or repeat) of an upgrade multiplies its resource costs and research minutes by the node's cost multiplier raised to the town's current unlock count for that node; resource amounts are rounded up to whole items. First unlock is always at the base listed cost and time.

## How it connects
Production recipes (loaded from productions.csv via ProductionRegistry) define inputs, outputs, cycle times, and conditions (happiness thresholds, pop limits, etc.). Running recipes affect town resources and population. Upgrade nodes (from upgrades.csv + requirements) provide permanent or repeatable stat changes; the effect value at the town's current level for that node is computed by the scaling formula so that higher tiers give more (or diminishing) benefit. The same scaled values feed both gameplay systems and the upgrade status UI.
