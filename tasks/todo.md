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
- [x] **License (blocker)** — *Verified (scope)*: **MIT** everywhere. Root LICENSE replaced with MIT, Forge `mods.toml` updated from "All rights reserved" to "MIT". `fabric.mod.json` was already correct. Done 2026-06-17.
- [ ] **Toggle-respect packet audit** (keep in hygiene phase) — *Code check (June 2026)*: the subsystem toggles currently gate only tick paths (`Town.tick`, `TownInterfaceEntity` spawn loop); **no network packet handler checks any toggle**, and biome kits apply starting nodes/values unconditionally in `TownManager.registerTown`. Since UI tabs stay visible, player actions (trade, contract accept/bid, research start) may still mutate "disabled" systems server-side. Audit the actual exposure per packet, then pick the cheap fix per path: gate the server-side handler (authoritative) and/or hide the tab. Might be small — confirm exposure before sizing. (The actual default flip itself is now at end of Phase F.)
- [x] Set `DebugConfig.TOURIST_ENTITY = false` (only flag still on; contradicts release checklist) — Done 2026-06-18. Only remaining production debug flag turned off.
- [ ] Remove/convert 37 `System.out.println` calls (Forge/Fabric init classes, Fabric stub packets, and one in common `ui/modal/specialized/TownMapModal.java`) to logger calls or delete
- [ ] Delete orphaned Fabric dead code: stub packets under `fabric/network/packets/` not wired into `FabricModMessages`, placeholder `fabric/block/TownInterfaceBlock.java`, unused placeholder `fabric/api/` interfaces
- [ ] Remove or move demo screens out of main sources (`ui/screens/demo/`: `BCScreenTemplateDemo.java`, `BCModalGridExample.java`, `BCScreenExample.java`)
- [x] Fix Fabric build excluding loot tables from its jar (`fabric/build.gradle` line ~58) — also fixed root cause compile error (Forge-only `Block.getExplosionResistance(BlockState,...)` override). Removed exclude, loot tables now included for Fabric. Build now succeeds on both loaders. (2026-06-18)
- [ ] Add mod icon at `assets/businesscraft/icon.png` (already referenced by `fabric.mod.json`); add to Forge metadata too
- [ ] Renumber `mod_version` in `gradle.properties` from `1.0.0` to `0.9.0-beta` (earn the 1.0)
- [ ] Create `CHANGELOG.md` and start tracking versions
- [ ] Improve `mods.toml` description to match the better `fabric.mod.json` one
- [ ] **GitHub feedback links** — GitHub repo is the feedback channel (Issues/PRs): fill `issueTrackerURL`/`displayURL` in `mods.toml` and `contact.homepage`/`sources`/`issues` in `fabric.mod.json` (currently empty); listing copy points to GitHub Issues
- [ ] **Save-format versioning + beta caveat** — add a data-version int to the town saved-data NBT root (consider config TOML version key too) so v1.0 migrations are possible; state clearly in README + listing that 0.9 is a **beta**: worlds/config may not carry forward to v1.0
- [ ] **Repo hygiene before going public** — git still tracks `forge/run/logs/*.gz`, `fabric/run/logs/*`, `usercache.json`, `common/build/tmp/...`, `.obsidian/`, `fabric/.gradle/` (all show as modified in `git status`). Note: `.gitignore` edits don't untrack already-tracked files — that's why the fix "isn't working". One-time `git rm -r --cached <paths>` + commit, then the existing ignore rules hold; verify with `git status` after

## Phase C — Placeholder / Fake-Success UI Cleanup (optional for 0.9)
Principle: fake success messages are worse than missing buttons. Either implement, hide, or remove. *Verified (scope)*: **not a 0.9 blocker** — quick audit pass if time; many items may already be gone. Tourism-path fixes first if anything remains.
- [ ] *(optional)* Quick UI audit: job assignment placeholder, settings save/reset, visitor modal example data, `autoCollect`/`taxes` placeholders, contract detail player-name — fix/hide only if still present

## Phase D — Onboarding & Core Loop Feedback (highest player-retention value)
(From old ROADMAP_v1 Phases 1–2 — these survive the scope cut.)
- [x] ~~First-placement in-game guidance~~ — wiki/README only, not 0.9 code. *Verified (scope)*
- [x] ~~"What should I do next?" suggestions UI~~ — cut permanently (0.9 and v1+). Wiki + first-placement notifications only. *Verified (scope)*
- [x] ~~Founder's Handbook~~ — cut; no in-game manual. External docs only: README + listing → wiki (`vault/`). *Verified (scope)*
- [ ] **Economy defaults** — *Verified (scope)*: `metersPerEmerald=1000` (1 emerald per 1000 blocks); example milestones in default TOML only — 1000m → 1 apple, 5000m → 1 bread (payments are the main reward; milestones are bonus exemplars for server owners). Implementation in Phase D/B.
- [ ] **Distance loop anti-cheat** — *Verified (scope)*: fix back-and-forth track farming (`TouristEntity` samples every ~2s and adds all path length). Approach: sample less often and/or only credit movement ≥50m from last checkpoint and/or net progress toward destination — pick at implementation. 0.9 blocker.
- [ ] **Min-payment floor check (sibling of anti-cheat)** — *Code check (June 2026)*: payment is `max(1, distance/metersPerEmerald × count)` (`VisitorProcessingHelper`); there is **no minimum payout distance** in code — only `minDistanceBetweenTowns=100` (town placement), so adjacent towns ~100m apart with free tourist spawns farm 1 emerald per arrival batch. Decide alongside the anti-cheat fix: add a minimum payout distance, or drop the 1-emerald floor.
- [ ] **Tourist skin & renderer (WIP → 0.9 scope)** — finish the uncommitted custom tourist texture work (`TouristRenderer`, `TouristHatLayer`, `tourist_basic.png`, `scripts/generate_tourist_basic.py`) + an entity skin check pass — owner not fully happy with the current tourist skin
- [x] ~~Tourist-arrival feedback polish~~ — **v1**; no extra chat messages. Maybe particles + villager sound on payment/train exit. *Verified (scope)*
- [x] ~~Journey statistics on Overview~~ (avg distance, total revenue, repeat visitors) — **v1 polish**; 0.9 keeps existing tourism count only. *Verified (scope)*
- [ ] Play-test economy defaults + distance anti-cheat; server owners expected to tune config

## Phase E — Localization → moved to **v1.0**
0.9 ships with hardcoded English. The sweep details (string counts, priority order) now live in `tasks/ROADMAP_v1.md` § Localization. *Verified (scope)*

## Phase F — Testing & Release
- [x] Unit test coverage for economy-critical logic — delivered via the **Test + Docs Loop**: seed list exhausted June 2026, 39 ledger items (35 DONE, 1 BUG-FOUND, 3 NEEDS-MC), ~600+ tests — far beyond the T-001–T-005 minimum
- [ ] **Fix T-012 payment board bugs** — *Verified (scope)*: **0.9 blocker**. Two `@Disabled` tests in `TownPaymentBoardTest` (`toBuffer` partial leak + excess loss). Fix production code, re-enable tests, ledger → DONE.
- [x] ~~Multiplayer playtest~~ — **v1.0** (not 0.9; tourism-only defaults, SP + manual Create pass enough for beta). *Verified (scope)*
- [ ] Tourist vehicle stress test: minecarts + **Create (required compatibility)** — chunk boundaries, server restarts, long journeys; **manual playtest by owner only — no automated Create testing planned**. Note: Create detection is class-name sniffing (`create.content.trains`); Create *Fabric* is a separate port — verify it matches during the manual pass or document Create support as Forge-only for 0.9. *Verified (scope)*
- [ ] **Dedicated-server boot smoke test (both loaders)** — public beta will be run on servers day one even though multiplayer playtest is v1.0: boot a headless server on Forge + Fabric, found a town, let tourists spawn — catches client-class-in-server-path crashes cheaply
- [ ] Performance smoke test: multiple towns + many simultaneous tourists (no fixed target yet — tune during playtesting). *Verified (scope)*
- [ ] Full pass on **both** loaders (Forge + Fabric) — dual-platform 0.9 ship required. *Verified (scope)*
- [ ] **Set 0.9 tourism-only defaults** (final step, right before release/publishing) — Update the default config so the public beta ships as tourism-only: `craftableTownInterface=true`; `tourists.enabled=true`; `milestones.enabled=true`; `production.enabled=false`; `research.enabled=false`; `trading.enabled=false`; `contracts.enabled=false`. UI tabs remain visible but disabled systems do nothing. Move this late so full-mod testing is easy during development. Also do a final audit that no resources are granted or consumed for tourists, and that biome kits / starting values don't leak production/research content.
- [ ] Modrinth **and** CurseForge listing + GitHub README (concept, tourism-only defaults, wiki link); publish `vault/` overview layer as GitHub wiki — *Verified (scope): both platforms for 0.9 beta*
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
- Localization sweep → **v1.0** (`ROADMAP_v1.md` § Localization); v1.0 config defaults unchanged from 0.9 (also in `ROADMAP_v1.md`)
- `UIGridBuilder.java` refactor (2,647 lines) → post-release maintainability work (tracked in `tasks/toImprove.md`)

**Status**: Plan approved by user (June 2026). Release sequence: **0.9.x beta** (this file + `ROADMAP_v0.9.md`) → **v1.0** (contracts + prestige + worldgen, `ROADMAP_v1.md`) → **v2** (`ROADMAP_v2.md`). Test + Docs Loop COMPLETE for the beta (seeds exhausted, parked). Phase A COMPLETE. Tourism-only config defaults and final toggle enforcement moved to the end of Phase F (right before publishing) so full-mod testing remains easy during development.
