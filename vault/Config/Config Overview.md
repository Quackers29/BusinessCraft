---
tags:
  - overview
  - config
---
# Config Overview

**TL;DR**: All mod behavior is tuned from one settings file (`businesscraft.toml`) that is created automatically on first run, reloads live while the game is running, and feeds every major system — economy rates, town rules, tourist limits, contract timings, and feature toggles.

## Processes in this area
- **[[Config/Configuration Loading|Configuration Loading]]** (T-014) — How the settings file is read and written: every option has a built-in default so a missing or broken file never crashes the game, list-style settings (milestone rewards, town names) fall back safely when malformed, and edits to the file are picked up live without a restart.
- **[[Config/Data Parsing|Data Parsing]]** (T-021) — The compact text formats used in the mod's editable CSV files (upgrades, production recipes, biome starting kits) are turned into the structured effects, conditions, and resource costs the game uses. Parsing is forgiving: one bad entry is skipped with a warning instead of breaking the whole row.

## How it connects
Settings are read directly by the systems they tune — tourist lifespans and notifications, town naming and placement rules, payment rates, milestone rewards, contract timings, production thresholds, and the display timezone. Changes apply immediately after a reload. (For developers: tests that touch these values save and restore them around each test — see the detail notes for the pattern.)
