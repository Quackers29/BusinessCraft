# BusinessCraft - Living Document

**Last Updated**: May 20, 2026  
**Project Status**: Polishing for First Release  
**Core Vision**: Create a compelling **transport economy** in Minecraft. Towns generate supply and demand. Tourists provide the visible, living justification for building transportation infrastructure (especially Create trains).

## The Big Idea (Why This Mod Exists)

Most transport in Minecraft feels pointless — Create trains run empty around servers with no passengers or purpose.

**BusinessCraft's core fantasy**: Build towns that produce goods and need resources. These towns attract **living tourists**. By building platforms and transport networks, you enable tourists to travel efficiently. The further they travel, the more revenue they generate. Successful transport makes towns grow, produce more contracts, and attract more tourists.

**First release priority**: Focus on **entity (tourist) transport** because it creates the strongest emotional impact. Seeing tourists actually riding your Create trains (instead of them running empty) will be the main "aha" moment that makes the mod compelling.

## Core Player Loop

1. **Build Towns** — Place Town Interface, name it, grow population through successful tourism
2. **Build Transport** — Create platforms and routes (integrates with Create trains, rails, etc.)
3. **Attract & Move Tourists** — They travel your networks, generating revenue based on *actual distance traveled*
4. **Grow Economy** — Revenue funds upgrades, contracts, production. Towns produce what they sell and buy what they need
5. **Repeat** — Better transport → more tourists → more revenue → bigger towns

**Success metric for v1.0**: Player understands they are building a living transport economy within the first 10-15 minutes.

## Current Focus Areas (First Release)

- **Tourist Polish** (Active): Visuals, skins (config/skins folder), behaviors, and clear payoff when they use transport
- **Transportation Feedback**: Make it visually satisfying when tourists ride trains/platforms
- **Onboarding**: Clear messaging about the transport economy fantasy
- **Balance**: Early milestones should feel rewarding quickly

**Documentation & external communication** will be done last, just before release.

## Key Systems (Verified from Codebase)

### Town System (`com.quackers29.businesscraft.town`)
- `Town.java` (major class with component architecture)
- Components: Economy, Production, Trading, Contracts, Upgrades
- Multi-tier storage, population growth from tourism, milestone tracking

### Tourist System (`com.quackers29.businesscraft.entity`)
- `TouristEntity` with custom AI goals (gossip, gaze, celebration on speed)
- Smart ride extension, expiry system, origin tracking
- **Active work**: Tiered skins via dynamic config/skins loading (see Kanban task below)

### Platform & Transport (`com.quackers29.businesscraft.platform`)
- `Platform.java` with multi-destination and visualization
- Designed as integration point for Create and other transport mods

### UI & Supporting Systems
- Production-grade UI framework (`BCScreenBuilder`, state binding, modals)
- Strong network layer (22 packets), debug system (`DebugConfig` with 25+ flags), data helpers

## Architecture Principles
- Common-first (all logic in `common/`)
- Component and Provider patterns for clean separation
- Living documentation (this file is the single source of truth)
- Observability-first (excellent debug overlay via F3+K)

## Active Kanban Work
- `t_1ab40ead` (researcher): Completed — researched tiered tourist skins
- `t_961c5537` (coder): Completed — implemented dynamic skin loading system (`TouristSkinManager`, renderer integration, config/skins support)
- `t_0af9f211` (artist): **Rejected** — produced only hat textures. Changes reverted and code cleaned up.
- `t_d3f21c03` (artist): **Active** — new much stricter task to create proper full-body 64x64 tourist entity textures (no placeholders, specific clothing per tier, correct UV mapping).

**New Profile**: `artist` — created for visual work (Gareth's weakest area). Now being given extremely specific prompts.

**Next Priority**: Get high-quality full tourist skins that work properly with the existing renderer and hat layer. This is critical for first release visual polish.

---

**Archive**: Old documents moved to `docs/archive/`

**Obsidian**: Open this project folder in Obsidian for richer personal notes and linking.

*This is the single living document. It will be updated as we polish the transport economy systems. No other living docs will be maintained in this folder.*
