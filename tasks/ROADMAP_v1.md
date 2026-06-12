# BusinessCraft v1.0 Roadmap — Contracts, Prestige & World Discovery

**Date**: June 2026
**Scope**: The road from the 0.9.x public beta (see `ROADMAP_v0.9.md`) to a true 1.0.
**Guiding principle**: 0.9 proves the core tourism loop is polished and understandable. 1.0 earns its number by adding the three features that complete the fantasy: **player-driven tourist transportation work**, **a long-term goal to chase**, and **towns you can discover in the world** — not only craft. Beta feedback decides ordering and tuning within this scope.

## Vision for v1.0
A player who has mastered the core loop should have a reason to keep playing: accept tourist transport contracts from towns, run a transportation business with recurring routes, see their personal contribution on leaderboards, and race toward the "First to City" prestige milestone — alone or against other players on a server. New players joining a world for the first time should also be able to stumble on generated Town Interfaces in villages or across the map, giving the mod a natural discovery path beyond crafting.

## Pillar 1 — Tourist Transport Contracts
Following the same town-initiated → player-accepted model as the existing courier contracts:

- Towns post tourist transport needs on the contract board; players accept the work of actually moving the tourists (distinct from resource courier contracts).
- Towns can specify departure platforms when creating tourist transport contracts.
- Players can accept tourist contracts as **recurring contracts**, choosing the frequency and timing of repeats — the foundation of running a "transport company" loop.
- Supporting polish that makes the work feel good:
  - Tourists walk toward their assigned seat or platform for a short time (~1 second) before teleporting.
  - Client-side name tag coloring for tourists based on destination town, with a config toggle or keybind to disable.

## Pillar 2 — Prestige & "First to City" (configurable end-game loop)
A configurable prestige / end-goal system that gives v1 a clear sense of long-term progression and an initial "win condition":

- **"First to City" Town Goal (primary)**: a high, configurable tourism milestone for a town (total tourists transported, cumulative distance traveled by its tourists, or a combination). Default: enabled, set to a challenging but achievable number for a dedicated town.
- **Player contribution tracking & display**: the existing leaderboard system upgraded to show **player contribution per town** (contracted transportation, distance, or revenue generated per player).
- **Player Contracted Transportation Goal (secondary)**: a separate configurable milestone for individual players based on total contracted tourist transportation personally completed (across one or more towns).
- Both goals fully configurable (thresholds, enabled/disabled, scoring formulas) so servers can tune difficulty or turn the end-goal loop off entirely.
- Intended as a solid starting point — v2's reputation system builds directly on this tracking.

## Pillar 3 — Town Interface World Generation
Configurable world generation so Town Interfaces appear naturally in the world — improving discovery and world integration without breaking the transport economy fantasy. Exploration and design are complete; full implementation plan in `tasks/v1_worldgen.md`.

- **Villages mode** — jigsaw building in vanilla villages; town-square (platform + centred Town Interface); pre-founded on spawn; default when worldgen on. *Verified*
- **Random mode** — optional standalone spawns; off by default; rarity via playtesting. *Verified*
- **Config UX**: master switch **on** by default (v1.0); villages on; random off. Tunable rarity for random mode when enabled. *Verified*
- **Boundary respect**: generated placements must go through the same `TownBoundaryService` / `TownManager` validation as player-placed towns — likely a custom `StructurePlacement` for dynamic distance checks in random mode.
- **Platform implementation**: datapack structure assets in common, platform-specific registry injection on Forge and Fabric (exploration notes in the worldgen plan).
- **Generated town bootstrap**: spawned towns use the same registration, boundary validation, biome kit, default platform, and notification paths as player-founded towns (no bypass shortcuts). Name from `townNames` pool; on exhaustion merge 2→3→… pool names (merged names tracked as used); pop = `defaultStartingPopulation`. *Verified*
- **0.9 interim**: the beta still enables crafting by default so survival works before worldgen ships; worldgen is the v1.0 upgrade that makes discovery feel native.

## Supporting work (as beta feedback dictates)
- Visual/audio delight pass on the new flows: boarding animations/particles, arrival celebration for contract completions, leaderboard screen polish.
- Leverage the existing world visualization framework (LineRenderer3D, PathRenderer3D, VisualizationManager) to optionally display live tourist flow lines and contract routes — the "wow" moment for a growing transport network.
- Re-balance the economy around contract income (contract rewards vs. passive tourism fares).
- Each new logic unit gets a Test + Docs Loop iteration (vault note + unit tests) after implementation — the loop protocol is in `tasks/test_doc_loop.md`.

## v1.0 Release readiness checklist
- [ ] Tourist Transport Contracts implemented and playable (town-initiated, player-accepted, recurring option, departure platform selection)
- [ ] Boarding behavior polish + destination name tag coloring shipped
- [ ] "First to City" prestige system + player contribution leaderboards implemented and configurable
- [ ] Town Interface worldgen implemented: all mode combinations (off / villages / random / both), boundary rules never violated, tested on Forge + Fabric + at least one major worldgen mod
- [ ] New systems covered by Test + Docs Loop iterations (vault + unit tests)
- [ ] Full playtest pass on both loaders, singleplayer + multiplayer + Create integration
- [ ] 0.9 beta feedback triaged — blockers fixed, balance re-tuned
- [ ] Documentation/listing updated for the new features

---

**Status**: Queued — begins after the 0.9.x public beta ships (see `ROADMAP_v0.9.md`).
**Next after v1.0**: `ROADMAP_v2.md` — reputation, VIP contracts, automation, player companies (single-server depth), building directly on v1's contracts + prestige tracking + world-discovered towns.
