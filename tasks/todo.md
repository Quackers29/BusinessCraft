# Task: 0.9 Public Beta Release Preparation

**Context**: Full release-readiness review completed June 2026. Verdict: architecture and core tourism loop are strong (~75–80% mature), but packaging, scope, and polish gaps block a public release. **Versioning decision (user, June 2026)**: the polished core tourism loop ships as a **0.9.x public beta** (`tasks/ROADMAP_v0.9.md`); **v1.0** (`tasks/ROADMAP_v1.md`) adds tourist transport contracts, prestige/"First to City", and Town Interface worldgen — built after beta feedback arrives. Implementation plan for worldgen: `tasks/v1_worldgen.md`.

**Approach**: Work through phases in order. Phases A and B are cheap and unblock everything else. Phases D–F are the bulk of remaining player-facing work.

---

## Phase A — Roadmap & Documentation Reorganisation ✅ COMPLETE (June 2026)
- [x] Move worldgen task out of `todo.md` to `tasks/v1_worldgen.md` (v1.0 Pillar 3 implementation plan)
- [x] Split the old ROADMAP_v1: lean beta scope → new `tasks/ROADMAP_v0.9.md` (polish + onboarding + feedback + docs + multiplayer testing); old file's content preserved in git history
- [x] Create new `tasks/ROADMAP_v1.md`: Tourist Transport Contracts + prestige/"First to City" as the features that earn the 1.0 (per user decision — these were too good to cut)
- [x] Align `tasks/ROADMAP_v2.md` with the new sequence (0.9 beta → v1.0 contracts/prestige → v2 reputation/companies)
- [x] Fix `CLAUDE.md` drift: package corrected to `com.quackers29.businesscraft`, platform modules described accurately (forge 23 / fabric 34 classes), packet count ~57 in 6 subpackages + root view-model sync, debug keybind corrected to F4, stale line counts removed, test suite + vault referenced
- [x] Fix `README.md` overstatements: packet section rewritten with real packets (~57, was "22" with invented names), Population tab honestly marked as placeholder data, stale line counts/class name fixed, "70%+" claim softened, Fabric added to install requirements

## Phase B — Release Hygiene (the "one-day pass")
- [ ] **License (blocker)**: pick one license and align all three places — root `LICENSE` (currently GPL v3), `forge/.../META-INF/mods.toml` (currently "All rights reserved"), `fabric/.../fabric.mod.json` (currently "MIT")
- [ ] **Survival access (blocker for 0.9 beta)**: flip `craftableTownInterface` default to `true` (recipe already exists at `data/businesscraft/recipes/town_interface.json`) — interim path until v1.0 worldgen ships
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
- [ ] Job assignment: hide the UI ("Job assignment feature coming soon!" in `ButtonActionCoordinator.handleAssignJobs()`) — logic is post-1.0 scope
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
- [x] Unit test coverage for economy-critical logic — delivered via the **Test + Docs Loop**: seed list exhausted June 2026, 39 ledger items (35 DONE, 1 BUG-FOUND, 3 NEEDS-MC), ~600+ tests — far beyond the T-001–T-005 minimum
- [ ] **Fix T-012 payment board bugs (pre-beta blocker)**: two `@Disabled` tests in `TownPaymentBoardTest` pin real bugs in `toBuffer` claims — (1) partial-claim item leak while entry stays UNCLAIMED, (2) excess item loss on success. Fix production code, re-enable both tests, set ledger row to DONE
- [ ] Multiplayer playtest pass: payment board claims, milestone rewards, personal storage, contract flow with 2+ players
- [ ] Tourist vehicle stress test: minecarts + Create contraptions across chunk boundaries, server restarts, long journeys
- [ ] Performance check: 5 active towns, 50+ simultaneous tourists
- [ ] Full pass on BOTH loaders (`wsl ./gradlew :forge:runClient` equivalent + `:fabric:runClient`)
- [ ] Write Modrinth/CurseForge listing + basic getting-started docs (the vault's plain-language overview layer is ~80% of the material)
- [ ] Publish public beta (0.9.x); beta feedback shapes the v1.0 work in `tasks/ROADMAP_v1.md` (tourist contracts + prestige)

---

## Parallel Track — Test + Docs Loop (runs alongside Phases A–F)

**Concept**: An AI-executable loop where each iteration picks one untested unit of pure game logic (e.g. bid price calculation), documents it in an Obsidian vault (`vault/` — open as its own vault in Obsidian), and covers it with unit tests. Iterations are small and self-verifying, so cheaper/simpler models can run them. Protocol: `tasks/test_doc_loop.md`. Tracking: `vault/_meta/Coverage Ledger.md` (14 seed targets) + `vault/_meta/Loop Log.md`.

### One-time setup (capable agent, before first loop iteration)
- [x] Write loop protocol (`tasks/test_doc_loop.md`)
- [x] Create vault skeleton (`vault/Home.md`, `_meta/` ledger + log + note template)
- [x] Seed Coverage Ledger with 14 priority targets (economy math first)
- [x] Add JUnit 5 to `common/build.gradle` (junit-jupiter 5.10.2, junit-platform-launcher, `useJUnitPlatform()`)
- [x] Add smoke test (`common/src/test/java/.../SmokeTest.java`) and verify `wsl ./gradlew :common:test` runs green — confirmed: 2 tests pass, no ForgeGradle quirks; smoke test also verifies tests can reference production classes and guards `FORCE_ALL_DEBUG == false`
- [x] Pilot: ran iteration T-001 (distance payment calculation) end-to-end — vault note + 10 passing tests; protocol refined (reflection allowed for private pure methods, config field save/restore pattern, BlockPos confirmed safe)
- [x] Hand off to loop execution (cheap-model subagents or recurring loop runs, one ledger item per iteration)

### Status: beta-coverage goal met — loop PARKED (June 2026)
- [x] Work through Coverage Ledger — all 39 seed rows resolved (35 DONE, T-012 BUG-FOUND, T-034/T-037/T-038 NEEDS-MC)
- [x] Senior review #1 (after T-015): built McBootstrap fixture, re-opened 6 NEEDS-MC rows, corrected dates
- [x] Senior review #2 (T-016–T-039): docs/tests quality held up; fixed plain-language drift in overview notes (T-024/T-033/T-034 entries had code jargon), reconciled Home.md area taxonomy; T-012 bugs promoted to Phase F task
- [ ] Re-invoke the loop only when new logic lands (e.g. Phase D features should each get an iteration after implementation) — review cadence lesson: every ~5 iterations was specified but slipped to 24; enforce it next time
- [ ] NEEDS-MC rows (T-034 autonomous contracts, T-037 tourist spawning, T-038 entity ticking) need a GameTest harness — tracked in `tasks/toImprove.md`, not v1

## Not in the 0.9 beta (v1.0 scope)
- Tourist Transport Contracts + Prestige/"First to City" + Town Interface worldgen → **v1.0** (`tasks/ROADMAP_v1.md`; worldgen plan in `tasks/v1_worldgen.md`)
- `UIGridBuilder.java` refactor (2,647 lines) → post-release maintainability work (tracked in `tasks/toImprove.md`)

**Status**: Plan approved by user (June 2026). Release sequence: **0.9.x beta** (this file + `ROADMAP_v0.9.md`) → **v1.0** (contracts + prestige + worldgen, `ROADMAP_v1.md`) → **v2** (`ROADMAP_v2.md`). Test + Docs Loop COMPLETE for the beta (seeds exhausted, parked). Phase A COMPLETE. Next up: Phase B one-day pass, then fix T-012 payment board bugs.
