# BusinessCraft v1.0 Roadmap — Contracts, Prestige & World Discovery

**Date**: June 2026
**Scope**: The road from the 0.9.x public beta (see `ROADMAP_v0.9.md`) to a true 1.0.
**Guiding principle**: 0.9 proves the core tourism loop is polished and understandable. 1.0 earns its number by adding the three features that complete the fantasy: **player-driven tourist transportation work**, **a long-term goal to chase**, and **towns you can discover in the world** — not only craft. Beta feedback decides ordering and tuning within this scope.

## Vision for v1.0
A player who has mastered the core loop should have a reason to keep playing: accept tourist transport contracts from towns, run a transportation business with recurring routes, see their personal contribution on leaderboards, and race toward the "First to City" prestige milestone — alone or against other players on a server. New players joining a world for the first time should also be able to stumble on generated Town Interfaces in villages or across the map, giving the mod a natural discovery path beyond crafting.

## Pillar 1 — Tourist Transport Contracts
New contract type on the **existing contract board** — same town-to-town pattern as today's resource contracts (see `vault/Trade/` — sell auctions + courier accept/complete), but for **tourists** not items. Towns negotiate/post on the board; once settled, the associated **player transport contract** is what the player accepts and fulfils. *Verified*

- Mirrors current flow: town-level contract resolution first → player courier/transport leg (contract item, destination match, payment board payout) adapted for moving tourists.
- Towns can specify departure platforms when creating tourist transport contracts.
- **Recurring contracts required for v1.0** — player chooses frequency/timing of repeats (transport-company loop). *Verified*
- Supporting polish (v1.0):
  - Tourists walk toward seat/platform ~1s before teleport (not instant). *Verified*
  - Client-side name tag coloring by destination town; config toggle or keybind to disable. *Verified — v1, low priority*

## Pillar 2 — Prestige & "First to City" (configurable end-game loop)
Town **tiers** in config (same spirit as `upgrades.csv` / `milestones.rewards` in TOML): a ladder of named tiers per town. **City = the max tier** in the ladder (label configurable). Each tier advance requires **both** a tourist **count** and a cumulative tourist **distance** threshold — same dual-metric pattern as milestone rewards. *Verified*

- **Default shipped config**: tier **B** defined with count + distance goals; full ladder (e.g. A → B → City) tunable by server owners. Enabled/disabled per tier or for the whole prestige loop.
- **"First to City"**: town reaches the **max configured tier** (City by default naming).
- **Player contribution tracking & display**: leaderboard upgraded to show player contribution per town (contracted transportation, distance, revenue).
- **Player Contracted Transportation Goal (secondary, v1.0)**: separate configurable player milestone for total contracted tourist transport completed (across towns). *Verified*
- v2 reputation builds on this tracking.

## Pillar 3 — Town Interface World Generation
Configurable world generation so Town Interfaces appear naturally in the world — improving discovery and world integration without breaking the transport economy fantasy. Exploration and design are complete; full implementation plan in `tasks/v1_worldgen.md`.

- **Villages mode** — jigsaw building in vanilla villages; town-square (platform + centred Town Interface); pre-founded on spawn; default when worldgen on. *Verified*
- **Random mode** — optional standalone spawns; off by default; rarity via playtesting. *Verified*
- **Config UX**: master switch **on** by default (v1.0); villages on; random off. Tunable rarity for random mode when enabled. *Verified*
- **Boundary respect**: generated placements must go through the same `TownBoundaryService` / `TownManager` validation as player-placed towns — likely a custom `StructurePlacement` for dynamic distance checks in random mode.
- **Platform implementation**: datapack structure assets in common, platform-specific registry injection on Forge and Fabric (exploration notes in the worldgen plan).
- **Generated town bootstrap**: spawned towns use the same registration, boundary validation, biome kit, default platform, and notification paths as player-founded towns (no bypass shortcuts). Name from `townNames` pool; on exhaustion merge 2→3→… pool names (merged names tracked as used); pop = `defaultStartingPopulation`. *Verified*
- **0.9 interim**: the beta still enables crafting by default so survival works before worldgen ships; worldgen is the v1.0 upgrade that makes discovery feel native.
- **Existing worlds**: new chunks only — no retrofit of villages already generated. *Verified*

## Config defaults (v1.0)
No change from 0.9 tourism-only subsystem defaults yet — production/trading/contracts/research stay **off**; revisit after beta feedback when heading to v1 ship. *Verified*

## Platform expansion
Add **NeoForge** and **Quilt** loader support (alongside existing Forge + Fabric from 0.9). Same common module; new platform modules + parity testing. *Verified*

## Localization
Full `Component.translatable` + `en_us.json` sweep (~180+ strings); English only; structure for community translations later. *Verified*

## Player documentation
No in-game manual. Wiki (`vault/` → GitHub wiki) + README/listing fleshed out for v1 features (contracts, tiers, worldgen). *Verified*

## Supporting work (as beta feedback dictates)
- **Town Overview polish**: avg tourist distance, total tourism revenue, repeat visitors (0.9 keeps existing tourism count only). *Verified*
- **Arrival feedback** (no extra chat): maybe particles + villager sound on tourist payment or train exit. *Verified — v1*
- Visual/audio delight on contract flows: boarding animations/particles, arrival celebration for contract completions, leaderboard screen polish.
- Live tourist flow lines / contract routes via existing visualization framework — needs more design thought; *v1 low priority, ship if time*.
- Re-balance the economy around contract income (contract rewards vs. passive tourism fares).
- Each new logic unit gets a Test + Docs Loop iteration (vault note + unit tests) after implementation — the loop protocol is in `tasks/test_doc_loop.md`.

## v1.0 Release readiness checklist
- [ ] Tourist Transport Contracts implemented and playable (new type on contract board; town-initiated, player-accepted; **recurring required**; departure platform selection)
- [ ] Boarding behavior polish shipped; name tag coloring (*low priority*, ship if time)
- [ ] "First to City" prestige system + player contribution leaderboards implemented and configurable
- [ ] Town Interface worldgen implemented: all mode combinations (off / villages / random / both), boundary rules never violated, tested on Forge + Fabric + at least one major worldgen mod
- [ ] New systems covered by Test + Docs Loop iterations (vault + unit tests)
- [ ] Full playtest on Forge, Fabric, NeoForge, and Quilt; **multiplayer required** (payment board, contracts, storage); Create where applicable
- [ ] 0.9 beta feedback triaged — blockers fixed, balance re-tuned
- [ ] Documentation/listing updated for the new features

---

**Status**: Queued — begins after the 0.9.x public beta ships (see `ROADMAP_v0.9.md`).
**Next after v1.0**: `ROADMAP_v2.md` — reputation, VIP contracts, automation, player companies (single-server depth), building directly on v1's contracts + prestige tracking + world-discovered towns.
