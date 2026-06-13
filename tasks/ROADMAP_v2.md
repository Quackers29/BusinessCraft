# BusinessCraft v2 Roadmap — Town-to-Town Trading (Single-Server Economy)

**Status**: Initial High-Level Plan (Subject to Change)  
**Target**: Post v1.0 (release sequence: 0.9.x public beta → v1.0 with Tourist Transport Contracts + prestige — see `ROADMAP_v0.9.md` and `ROADMAP_v1.md`)  
**Focus**: Deepening the single-server economy — starting with advanced contracts, reputation, and companies, plus town specialization and player production.

## v2 Opening Priorities (Advanced Features Deferred from v1 Planning)

To keep v1 focused on the base Tourist Transport Contracts and the initial prestige system, the more ambitious expansions are the primary early focus of v2:

- **Tourist Contract Expansions**: VIP Tourist Contracts, a proper Reputation system, and fuzzy Create train name / route targeting.
- **Deeper Automation & Route Intelligence**: Automatic contract generation, network discovery, and smart contract suggestions based on actual player routes.
- **Companies & Advanced Multiplayer Systems**: Full Player Companies (with shared reputation, asset registration, capacity scaling, and restricted contracts).
- **Additional Polish**: Upgradeable platform limits per town.

These form the foundation that the rest of v2 will build upon.

## Vision for v2
After players have experienced the core tourism fantasy and initial prestige systems in v1.0, v2 significantly expands the single-server economy. Building on the advanced contract, reputation, automation, and company foundations carried forward from v1 planning, v2 focuses on making town identity persistent and meaningful, introducing deeper reputation mechanics, and enabling players to own and operate production through company structures. The economy matures from "tourists and basic contracts bring value" to a rich, player-influenced town-to-town trading system.

## Core Pillars

### 1. Town Specialization (Path Dependency)
The foundation already exists via biome-based starting kits and the upgrade system. v2 adds strong incentives to continue down a chosen path rather than everyone rushing the same meta production tree (see earlier detailed description in previous versions of this doc).

### 2. Reputation System
A full reputation system (originally planned as a v1 expansion but moved to v2) that tracks town and player reliability. High reputation unlocks better contracts, higher rewards, and better positioning. This will be one of the first major systems implemented in v2, building directly on the basic prestige tracking added in v1.

### 3. Player Companies & Advanced Multiplayer Production
The full Player Company system (also carried forward from v1 planning) including:
- Multiplayer companies with shared reputation and pooled capacity.
- Registering trains and production assets as company property.
- Advanced contract rules and restrictions.
- Player-owned production sites that compete or cooperate with NPC towns.

This pillar absorbs the "Companies & Advanced Multiplayer Systems" work moved from v1 planning and becomes a central feature of v2.

## Relationship to v1.0
- The advanced contract, reputation, automation, and company systems that were deferred from v1 planning form the opening priorities of v2.
- Builds directly on the Tourist Transport Contracts + configurable prestige system ("First to City") delivered in v1.0 (`ROADMAP_v1.md`).
- Deepens the existing town upgrade and production systems (already present in v1) with path-dependency mechanics.
- Player Companies and Reputation become first-class, deeply integrated features rather than afterthoughts.

## High-Level Implementation Considerations
- Designing good reputation curves and company mechanics will be critical — they need to feel rewarding without creating toxic meta or griefing vectors.
- Path dependency in specialization should feel natural and data-tunable.
- Everything must integrate cleanly with the Tourist Contracts and prestige systems delivered in v1.
- This release stays focused on single-server depth. Cross-server federation is a v3 concern.

## Technical foundations (carried from architecture work)
- **Resource storage int→long migration** *(complete)* — Aggregated town storage (`TownResources`, escrow, contracts, payment board, network packets, view-models) uses `long` instead of `int` so large-scale economies are not capped at ~2.1B per item. Individual `ItemStack` counts stay `int` (Minecraft API); UI formats big totals as K/M/B. **Why v2 cares**: town-to-town trading and company-scale stockpiles need headroom. **Save note**: worlds with pre-migration int NBT need a load-time migration — coordinate with 0.9 beta save-version work in `todo.md` Phase B.
- **Optional data-layer refactor** *(v2, only if needed)* — If v2 trading/company scale exposes sync or persistence pain, consider a centralized `ViewModelSyncManager` (`syncAll()`, `syncSelective()`, `markDirty()`) atop the existing `ViewModelCache` + dirty-flag path. YAGNI until profiling says otherwise.

**Note**: This is an initial planning document. Exact feature scope, balance, and implementation order will be refined closer to development based on v1.0 feedback and technical realities.