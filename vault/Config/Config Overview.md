---
tags:
  - overview
  - config
---
# Config Overview

**TL;DR**: All mod behavior is driven by ~30 public static fields in ConfigLoader, populated from businesscraft.toml (NightConfig) with Java-declared defaults; the file is auto-generated on first run, supports hot-reload via a JDK WatchService, and feeds every major system (economy rates, town rules, tourist thresholds, contract timings, toggles).

## Processes in this area
- **[[Config/Configuration Loading|Configuration Loading]]** (T-014) — ConfigLoader.loadConfig/saveConfig + getOrElse parsing, the special milestoneRewards and townNames fallback rules, private getDefaultTownNames, ConfigurationService hot-reload registration + Result-based validation, and the "null platform → keep Java defaults" behavior that makes the statics testable in pure JUnit.

## How it connects
Config values are read directly (no getters) by TouristEntity (expiry, notify, enabled), TownInterface* (naming, craftable flag), TownBoundaryService / TownValidationService (min distance, starting pop), VisitorProcessingHelper (metersPerEmerald via T-001), DistanceMilestoneHelper (T-002), ContractBoard timings, production/trading loops, BCTimeUtils (timezone), and the global phase-11 toggles. Changes after load (hot reload or test mutation) are immediately visible. The Test + Docs Loop and many existing tests rely on the save/restore @BeforeEach/@AfterEach pattern for these statics plus stubbing PlatformAccess.platform when forcing a re-load from a temp toml.
