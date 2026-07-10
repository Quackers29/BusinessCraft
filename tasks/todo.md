# Task: 0.9 Public Beta Release Preparation

**Context**: Full release-readiness review completed June 2026. Verdict: architecture and core tourism loop are strong (~75–80% mature), but packaging, scope, and polish gaps block a public release. **Versioning decision (user, June 2026)**: the polished core tourism loop ships as a **0.9.x public beta** (`tasks/ROADMAP_v0.9.md`); **v1.0** (`tasks/ROADMAP_v1.md`) adds tourist transport contracts, prestige/"First to City", and Town Interface worldgen — built after beta feedback arrives. Implementation plan for worldgen: `tasks/v1_worldgen.md`.

**Approach**: Work through phases in order. Phases A and B are cheap and unblock everything else. Phases D–F are the bulk of remaining player-facing work.

---

## 0.9 SHIP PLAN — adversarial audit, July 2026 (PROPOSED — awaiting approval)

Independent code verification of all prior findings (T-012, toggles, anti-cheat, Fabric/server risk, hygiene). Everything below was confirmed in code, not taken from earlier audits. This section is the ordered execution plan; items in Phases B–F below remain valid unless corrected here.

### New findings (not in any prior plan)

1. **BLOCKER — production-off kills tourism.** The only sustained source of `pendingTouristSpawns` is the `tourism` recipe in `productions.csv` (`food:10;money:10 → tourist:1`), run by `TownProductionComponent.tick()`, which is gated by `productionEnabled` (`Town.java:158`). Other sources: one-time biome-kit grant (~2 tourists), trading (also planned off). Flipping the Phase F tourism-only defaults as written ships a mod where each town spawns ~2 tourists ever, then goes dead. Fix needed BEFORE the default flip: a free tourist-regen path gated by `touristSystemEnabled` (e.g. timer in `Town.tick` granting pending spawns up to cap, or run the tourism recipe cost-free when production is off). ~30–60 lines + test.
2. **BLOCKER — Forge release jar ships without assets/data.** `forge/build.gradle:69–72` excludes `assets/**`, `data/**`, `pack.mcmeta` from the jar; `forge/src/main/resources` holds only `mods.toml`. Dev runs work via classpath; the published jar has no textures/recipes/loot tables. Remove excludes, resolve the duplicate-resources reason with `duplicatesStrategy`, verify by unzipping the built jar. Fabric already fixed; Forge was missed.

### Corrections to claims elsewhere in this file (verified July 2026)

- **"Free tourist spawns"** (Phase D/F wording): wrong today — generation costs food+money via production, and see New finding #1.
- **`minDistanceBetweenTowns=100`** (Phase D min-floor item): dead config — loaded but never read outside ConfigLoader. Real placement minimum ≈ 55 blocks via boundary radii (`TownBoundaryService`). Remove from TOML or wire it; the min-payout decision should use the ~55m reality.
- **Repo hygiene** (Phase B): overstated — run logs, `usercache.json`, `build/tmp` are already untracked. Only 8 files remain: `vault/.obsidian/*` (5) + `fabric/.gradle/loom-cache/*` (3). One small `git rm --cached`.
- **Tourist skin "uncommitted WIP"** (Phase D): textures (`tourist_basic.png` etc.) + `scripts/generate_tourist_basic.py` are committed. Remaining work is a subjective quality pass only — owner judgment, not a code blocker.
- **Distance anti-cheat** (Phase D): confirmed, and worse — expiry pauses while moving (`TouristEntity.java:288–309`), so a loop-track tourist accumulates unbounded path distance and never expires. Note: path-based payment was a deliberate earlier decision ("CORE CHANGE" comment, `VisitorProcessingHelper.java:182`); don't revert to straight-line. Clamp instead: `distance = min(traveled, 2 × straightLine(origin→dest))` at arrival. ~10–20 lines + tests.
- **Toggle audit** (Phase B): confirmed small. Exact fix set: guards in `TradeResourcePacket`, `BidContractPacket`, `AcceptContractPacket`; biome-kit filtering in `TownManager.registerTown` (allowlist `pop`/`happiness`/`tourist`/`tourist_cap`/`border`; skip `startingNodes` when research off, production stock when production off); one-line guard in `Town.calculateWants()`. Production/research have no action packets — tick gating already covers them. ~50–90 lines + tests.
- **T-012** (Phase F): both bugs confirmed. Root cause: `SlotBasedStorage.addItem` returns true on partial placement; `addToBufferStorage` assumes all-or-nothing (`TownPaymentBoard.java:134–145`). Bug 1 = partial leak + UNCLAIMED entry → re-claim dupes. Bug 2 = partial fit marked CLAIMED → remainder silently destroyed. Sibling `addToBuffer` (`:174–187`, used by inventory-overflow + communal storage) has the same flaw — fix together. Also add failure-path buffer sync + `markDirty` in `PaymentBoardClaimPacket`. ~30–50 lines; re-enable both @Disabled tests.

### Ordered execution plan

**Step 1 — Economy-integrity fixes (code, test-verified)**
- [x] T-012 payment board — DONE 2026-07-06: added `SlotBasedStorage.addItemFully` (all-or-nothing, staged on a copy); `addToBufferStorage` + `addToBuffer` now transactional; claim packet overflow drops items at player's feet instead of destroying them when buffer also full; 2 @Disabled tests re-enabled asserting atomic behavior + regression tests; vault notes + ledger updated (T-012 → DONE); full `:common:test` suite green. (Failure-path sync no longer needed — failed claims are now side-effect-free.)
- [x] Distance clamp — DONE 2026-07-09: `clampTraveledDistance` (public static, `min(traveled, 2 × straightLine)`) applied in `processIndividualVisitor` before the distance enters the batch; caps loop-track farming and (since it's upstream) milestone farming too. Min-payout decision: kept the 1-emerald floor, added `economy.minPayoutDistance` (default 100, inclusive threshold) — batches averaging below it pay 0 and their saved distance is cleared. 10 new tests (clamp + cutoff) in `VisitorProcessingHelperTest`, full `:common:test` green; vault note + ledger (T-001) updated.
- [ ] Tourism decoupling (New finding #1): free tourist-regen path gated by `touristSystemEnabled`; verify loop runs end-to-end with production/trading/contracts/research all false

**Step 2 — Toggle honesty (code, small)**
- [ ] Guards in the 3 packet handlers; biome-kit allowlist in `registerTown`; `calculateWants` guard; tests

**Step 3 — Packaging & hygiene (one pass)**
- [ ] Forge jar: remove `assets/**`/`data/**`/`pack.mcmeta` excludes (New finding #2), fix duplicates properly, verify jar contents; also remove duplicate `OpenPaymentBoardPacket` registration in `ForgeModMessages`
- [ ] Delete dead code: 10 Fabric files (4 stub packets, `fabric/block/TownInterfaceBlock`, 5 `fabric/api/` interfaces — zero references confirmed) + 3 demo screens in `ui/screens/demo/`
- [ ] Metadata: `mod_version` → `0.9.0-beta`; add `issueTrackerURL`/`displayURL` to `mods.toml` (keys currently absent) + fill empty `fabric.mod.json` contact fields; mod icon; `CHANGELOG.md`; improve `mods.toml` description
- [ ] `System.out.println` sweep; `git rm --cached` the 8 remaining files; save-format data-version int in town NBT; remove or wire dead `minDistanceBetweenTowns` in TOML

**Step 4 — Default flip + verification (last, as planned)**
- [ ] Set tourism-only TOML defaults (craftable on; production/research/trading/contracts off) + economy values (`metersPerEmerald=1000`, milestone table 1000m/5000m — current 10m default fires on every arrival)
- [ ] Dedicated-server boot smoke test, Forge + Fabric (Fabric is structurally fragile — client APIs in server-loaded `FabricModMessages`/`FabricEventCallbackHandler`; refactor ONLY if the test fails)
- [ ] Full SP pass both loaders + manual Create pass (note: Create Fabric detection is a silent string sniff — `TouristEntity.java:564`; verify or document Forge-only). Docs must state tourists can't walk (speed 0) — routes need carts/trains
- [ ] Listing/README/wiki, publish

**Deferred (unchanged)**: Fabric class-split refactor (unless smoke test fails), tourist skin quality pass (owner), localization, multiplayer playtest, `UIGridBuilder` refactor.

**Status**: PROPOSED — do not start until approved.

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
  Sub-plan:
  - [ ] Audit biome kit application in TownManager.registerTown (startingNodes for upgrades, startingValues for resources)
  - [ ] Find all packet handlers that can trigger production/trading/contracts/research (e.g. BidContractPacket, AcceptContractPacket, TradeResourcePacket, research start packets, etc.)
  - [ ] Decide per path: add server-side if (!ConfigLoader.xxxEnabled) return; or similar guard
  - [ ] Check UI tab content builders — do they need guards or is "visible but inert" acceptable?
  - [ ] Test toggles manually or with notes (since defaults stay full for now)
  - [ ] Update any comments / todo cross refs
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
- [x] **Distance loop anti-cheat** — DONE 2026-07-09 via the Ship Plan Step 1 distance clamp (payable distance capped at 2× straight-line origin→destination; path-based payment kept per the earlier CORE CHANGE decision).
- [x] **Min-payment floor check (sibling of anti-cheat)** — DONE 2026-07-09: added `economy.minPayoutDistance` (default 100 blocks, inclusive); batches averaging below it pay 0. The 1-emerald floor stays for real routes (good onboarding feedback with `metersPerEmerald=1000`).
- [ ] **Tourist skin & renderer (WIP → 0.9 scope)** — finish the uncommitted custom tourist texture work (`TouristRenderer`, `TouristHatLayer`, `tourist_basic.png`, `scripts/generate_tourist_basic.py`) + an entity skin check pass — owner not fully happy with the current tourist skin
- [x] ~~Tourist-arrival feedback polish~~ — **v1**; no extra chat messages. Maybe particles + villager sound on payment/train exit. *Verified (scope)*
- [x] ~~Journey statistics on Overview~~ (avg distance, total revenue, repeat visitors) — **v1 polish**; 0.9 keeps existing tourism count only. *Verified (scope)*
- [ ] Play-test economy defaults + distance anti-cheat; server owners expected to tune config

## Phase E — Localization → moved to **v1.0**
0.9 ships with hardcoded English. The sweep details (string counts, priority order) now live in `tasks/ROADMAP_v1.md` § Localization. *Verified (scope)*

## Phase F — Testing & Release
- [x] Unit test coverage for economy-critical logic — delivered via the **Test + Docs Loop**: seed list exhausted June 2026, 39 ledger items (35 DONE, 1 BUG-FOUND, 3 NEEDS-MC), ~600+ tests — far beyond the T-001–T-005 minimum
- [x] **Fix T-012 payment board bugs** — DONE 2026-07-06 (see Ship Plan Step 1): transactional buffer adds via `SlotBasedStorage.addItemFully`; both `@Disabled` tests re-enabled asserting the fixed atomic behavior; ledger → DONE.
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
