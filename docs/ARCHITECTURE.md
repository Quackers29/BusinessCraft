# BusinessCraft - Living Document

**Last Updated**: October 26, 2026  
**Project Status**: 0.9.0-beta — tourism transport core  
**Core Vision**: Create a compelling **transport economy** in Minecraft. Towns attract tourists; **moving them real distance** (minecarts, Create trains) generates revenue and growth. Production, trading, and contracts exist in code but ship **locked off** in 0.9.

## The Big Idea (Why This Mod Exists)

Most transport in Minecraft feels pointless — Create trains run empty around servers with no passengers or purpose.

**BusinessCraft's long-term fantasy**: Towns produce goods, negotiate contracts, and attract living tourists. Transport networks matter because tourists (and eventually goods) need to move. **What ships in 0.9** is the tourism slice of that fantasy: found a town with a **Town Centre**, place **Platform Interface** blocks, route tourists, earn fares and milestones. Deeper supply/demand and town-to-town trading are roadmap work (`tasks/ROADMAP_v2.md`, `tasks/ROADMAP_v3.md`).

**First release priority**: **Entity (tourist) transport** — seeing tourists ride your trains instead of empty loops is the main “aha” moment. Tourists **do not walk** in 0.9; they wait until carried.

## Core Player Loop (0.9)

1. **Found a town** — Place a **Town Centre** block (boundary + name). No owner.
2. **Place platforms** — Craft **Platform Interface** blocks inside the boundary; set path, radius, spread, enable, destinations on each.
3. **Move tourists** — Spawn when enabled + path complete and population ≥ `minPopForTourists` (default 5). Carry them with minecarts or Create; fare scales with distance (`metersPerEmerald=100` default).
4. **Collect payoff** — Payment Board milestones, population growth from visits, visit history.
5. **Expand** — More interfaces (up to 10), better routes, optional unlocked subsystems if a server enables them.

**Success metric for v1.0**: Player understands they are building a living transport economy within the first 10-15 minutes. (0.9 focuses on getting tourism transport understandable and honest.)

## Current Focus Areas (0.9 beta)

- **Tourism transport loop**: Town Centre + Platform Interface setup, honest UI, distance payouts
- **Tourist presentation**: 16 layered looks, names, ranks — see **`docs/TOURIST_MODELS.md`** (not a `config/skins` folder workflow)
- **Verification**: Automated tests + showcase harness; owner playtest and listings still open (`tasks/todo.md`)
- **Locked by default**: production, research, trading, contracts (tabs visible, inert)

**Documentation**: README + roadmaps updated for 0.9; wiki/vault for deeper design notes.

## Key Systems (Verified from Codebase)

### Town System (`com.quackers29.businesscraft.town`)
- `Town.java` (major class with component architecture)
- Components: Economy, Production, Trading, Contracts, Upgrades (several gated off in default config)
- Multi-tier storage, population growth from tourism, milestone tracking
- Boundary from modifier / `defaultBorderRadius` (not population growth)

### Tourist System (`com.quackers29.businesscraft.entity`)
- `TouristEntity` — ride extension, expiry, origin/destination tracking
- **Looks**: datapack `tourist_looks` + layered textures (`docs/TOURIST_MODELS.md`)
- **Movement**: effectively stationary on foot in 0.9 (player transport required)

### Platform & Transport
- `Platform.java` data + **Platform Interface** block entity (settings and path on the block)
- Town Centre block entity holds town state; read-only platform list in UI
- Integration point for Create and minecarts

### UI & Supporting Systems
- Production-grade UI framework (`BCScreenBuilder`, state binding, modals)
- Network layer (platform/town/storage/UI packets), debug system (`DebugConfig`), data helpers

## Architecture Principles
- Common-first (all logic in `common/`)
- Component and Provider patterns for clean separation
- Living documentation (this file is the single source of truth for architecture intent)
- Observability-first (excellent debug overlay via F3+K)

## Roadmap pointers
- **0.9**: `tasks/ROADMAP_v0.9.md`, checklist `tasks/todo.md`
- **v1**: contracts, prestige, worldgen — `tasks/ROADMAP_v1.md`, `tasks/v1_worldgen.md` (includes future tourist walk-to-seat polish)
- **v2 / v3**: trading depth, federation — `tasks/ROADMAP_v2.md`, `tasks/ROADMAP_v3.md`

---

**Archive**: Old documents moved to `docs/archive/`

**Obsidian**: Personal design vault lives in **`vault/`** (link from repo root in your Obsidian vault setup).

*This is the single living document. It will be updated as systems ship. No other living docs will be maintained in this folder.*
