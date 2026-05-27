# BusinessCraft v2 Roadmap — Town-to-Town Trading (Single-Server Economy)

**Status**: Initial High-Level Plan (Subject to Change)  
**Target**: Post v1.0  
**Focus**: Deepening the single-server economy — starting with advanced contracts, reputation, and companies carried from v1 planning, plus town specialization and player production.

## v2 Opening Priorities (Advanced Features Carried from v1 Planning)

To keep v1 focused, several more ambitious features were moved out of the initial release and are now the primary early focus of v2:

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
- The advanced contract, reputation, automation, and company systems that were deferred from v1 now form the opening priorities of v2.
- Builds directly on Tourist Contracts + the new configurable prestige system ("First to City") from v1.
- Deepens the existing town upgrade and production systems (already present in v1) with path-dependency mechanics.
- Player Companies and Reputation become first-class, deeply integrated features rather than afterthoughts.

## High-Level Implementation Considerations
- Designing good reputation curves and company mechanics will be critical — they need to feel rewarding without creating toxic meta or griefing vectors.
- Path dependency in specialization should feel natural and data-tunable.
- Everything must integrate cleanly with the Tourist Contracts and prestige systems delivered in v1.
- This release stays focused on single-server depth. Cross-server federation is a v3 concern.

**Note**: This is an initial planning document. Exact feature scope, balance, and implementation order will be refined closer to development based on v1.0 feedback and technical realities.