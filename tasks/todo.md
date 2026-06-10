# Task: v1.0 Release Preparation

**Context**: Full release-readiness review completed June 2026. Verdict: architecture and core tourism loop are strong (~75–80% mature), but packaging, scope, and polish gaps block a public release. Decision: redefine v1 as "the polished core tourism loop" — tourist transport contracts, prestige/"First to City", and worldgen are deferred (worldgen plan preserved in `tasks/v1.1_worldgen.md`).

**Approach**: Work through phases in order. Phases A and B are cheap and unblock everything else. Phases D–F are the bulk of remaining player-facing work.

---

## Phase A — Roadmap & Documentation Reorganisation (do first, ~1 session)
- [x] Move worldgen task out of `todo.md` to `tasks/v1.1_worldgen.md` (done as part of this cleanup)
- [ ] Rewrite `tasks/ROADMAP_v1.md` around the leaner scope: cut tourist transport contracts, prestige/"First to City", and worldgen from v1.0; v1 = polish + onboarding + feedback + docs + multiplayer testing
- [ ] Move the cut features (tourist contracts, prestige) into `tasks/ROADMAP_v2.md` opening priorities (or a new v1.x section)
- [ ] Fix `CLAUDE.md` drift: package is `com.quackers29.businesscraft` (not `com.yourdomain`), platform modules are NOT empty (forge 23 files, fabric 34), packet count is 57 (not 22), debug keybind reference is stale
- [ ] Fix `README.md` overstatements: packet count, Population tab "sample data" described as a feature, any other claims that don't match code

## Phase B — Release Hygiene (the "one-day pass")
- [ ] **License (blocker)**: pick one license and align all three places — root `LICENSE` (currently GPL v3), `forge/.../META-INF/mods.toml` (currently "All rights reserved"), `fabric/.../fabric.mod.json` (currently "MIT")
- [ ] **Survival access (blocker)**: flip `craftableTownInterface` default to `true` (recipe already exists at `data/businesscraft/recipes/town_interface.json`) — without worldgen there is currently no survival path to the core block
- [ ] Set `DebugConfig.TOURIST_ENTITY = false` (only flag still on; contradicts release checklist)
- [ ] Remove/convert ~37 `System.out.println` calls (Forge/Fabric init classes + Fabric stub packets) to logger calls or delete
- [ ] Delete orphaned Fabric dead code: stub packets under `fabric/network/packets/` not wired into `FabricModMessages`, placeholder `fabric/block/TownInterfaceBlock.java`, unused placeholder `fabric/api/` interfaces
- [ ] Remove or move demo screens out of main sources (`ui/screens/demo/BCScreenTemplateDemo.java`, `BCModalGridExample.java`)
- [ ] Fix Fabric build excluding loot tables from its jar (`fabric/build.gradle` line ~58)
- [ ] Add mod icon at `assets/businesscraft/icon.png` (already referenced by `fabric.mod.json`); add to Forge metadata too
- [ ] Renumber `mod_version` in `gradle.properties` from `1.0.0` to `0.9.0-beta` (earn the 1.0)
- [ ] Create `CHANGELOG.md` and start tracking versions
- [ ] Improve `mods.toml` description to match the better `fabric.mod.json` one

## Phase C — Placeholder / Fake-Success UI Cleanup
Principle: fake success messages are worse than missing buttons. Either implement, hide, or remove.
- [ ] Job assignment: hide the UI ("Job assignment feature coming soon!" in `ButtonActionCoordinator.handleAssignJobs()`) — logic is v2 scope
- [ ] Settings save/reset in `ButtonActionCoordinator` (lines ~181, 195): implement for the settings that exist, or remove the buttons
- [ ] `VisitorModalManager` hardcoded example visitor data: wire to real visit history or hide the modal
- [ ] `TownInterfaceViewModelBuilder`: `autoCollect` / `taxes` hardcoded `false` placeholders — remove from UI or implement
- [ ] `ContractDetailViewModelBuilder` TODO: resolve player name from UUID

## Phase D — Onboarding & Core Loop Feedback (highest player-retention value)
(From old ROADMAP_v1 Phases 1–2 — these survive the scope cut.)
- [ ] First-placement experience: clear immediate feedback + guidance when the player places their first Town Interface (population requirements, what to build next)
- [ ] "What should I do next?" suggestions area on the main Town Interface screen, driven by town state
- [ ] Founder's Handbook / journal item granted on first placement (lightweight version is fine for v1)
- [ ] Make the first 2–3 milestone rewards significantly easier to unlock (early dopamine)
- [ ] Increase clarity/impact of tourist-arrival feedback (particles, notifications, sounds — vanilla sound events only)
- [ ] Surface journey statistics (avg distance, total tourism revenue, repeat visitors) in the Town Overview tab
- [ ] Play-test and re-tune `metersPerEmerald` + milestone thresholds

## Phase E — Localization Sweep
- [ ] Funnel hardcoded UI strings (~180+ `Component.literal` across ~47 files) through `Component.translatable` + `en_us.json` — mechanical work, good for low-energy sessions; do it before release while strings are still free to change
- [ ] Priority order: contract screens, payment/trade messages, town notifications (`TownNotificationUtils`), contract item lore (`ContractItemHelper`), then the rest
- [ ] English only for v1; structure makes community translations possible later

## Phase F — Testing & Release
- [ ] Thin unit-test slice for pure economy logic only: payment calculation, milestone thresholds, contract auction resolution (cheap to test, highest silent-bug risk in multiplayer; NOT a full test suite)
- [ ] Multiplayer playtest pass: payment board claims, milestone rewards, personal storage, contract flow with 2+ players
- [ ] Tourist vehicle stress test: minecarts + Create contraptions across chunk boundaries, server restarts, long journeys
- [ ] Performance check: 5 active towns, 50+ simultaneous tourists
- [ ] Full pass on BOTH loaders (`wsl ./gradlew :forge:runClient` equivalent + `:fabric:runClient`)
- [ ] Write Modrinth/CurseForge listing + basic getting-started docs (old Phase 4)
- [ ] Publish public beta (0.9.x); let real feedback decide what 1.0 needs

---

## Deferred (not v1.0)
- Town Interface worldgen → `tasks/v1.1_worldgen.md` (planning complete, implementation not started)
- Tourist Transport Contracts → v1.x/v2 (move into ROADMAP_v2 during Phase A)
- Prestige / "First to City" → v1.x/v2 (move into ROADMAP_v2 during Phase A)
- `UIGridBuilder.java` refactor (2,647 lines) → post-release maintainability work; add to `tasks/toImprove.md`

**Status**: Plan approved by user (June 2026). Phase A in progress — file reorganisation done, roadmap rewrites pending.
