# Task: 0.9 Public Beta Release Preparation

**Context**: Full release-readiness review completed June 2026. Verdict: architecture and core tourism loop are strong (~75–80% mature), but packaging, scope, and polish gaps block a public release. **Versioning decision (user, June 2026)**: the polished core tourism loop ships as a **0.9.x public beta** (`tasks/ROADMAP_v0.9.md`); **v1.0** (`tasks/ROADMAP_v1.md`) adds tourist transport contracts, prestige/"First to City", and Town Interface worldgen — built after beta feedback arrives. Implementation plan for worldgen: `tasks/v1_worldgen.md`.

**Approach**: Work through phases in order. Phases A and B are cheap and unblock everything else. Phases D–F are the bulk of remaining player-facing work.

---

## 0.9 SHIP PLAN — adversarial audit, July 2026 (PROPOSED — awaiting approval)

Independent code verification of all prior findings (T-012, toggles, anti-cheat, Fabric/server risk, hygiene). Everything below was confirmed in code, not taken from earlier audits. This section is the ordered execution plan; items in Phases B–F below remain valid unless corrected here.

### New findings (not in any prior plan)

1. **~~BLOCKER — production-off kills tourism~~** — FIXED Option C (2026-07-16): `tourists.enabled` ticks the `tourism` recipe from `productions.csv`; `production.enabled` skips it. Free CSV row + `tourism` on `basic_settlement`. See Step 1.
2. **BLOCKER — Forge release jar ships without assets/data.** `forge/build.gradle:69–72` excludes `assets/**`, `data/**`, `pack.mcmeta` from the jar; `forge/src/main/resources` holds only `mods.toml`. Dev runs work via classpath; the published jar has no textures/recipes/loot tables. Remove excludes, resolve the duplicate-resources reason with `duplicatesStrategy`, verify by unzipping the built jar. Fabric already fixed; Forge was missed.

### Corrections to claims elsewhere in this file (verified July 2026)

- **"Free tourist spawns"** (Phase D/F wording): 0.9 ships free tourism via Option C CSV; full-mod can restore costs in config `productions.csv`.
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
- [x] **Tourism Option C** — DONE 2026-07-16: `tickTourismRecipe()` gated by `tourists.enabled`; production skips `tourism`; free CSV + `tourism` on `basic_settlement`; tourist_cap stall fix; 9 tests + T-040. (Research still works with production off if enabled — AI + deposit slot; no start packet.) Manual in-game trace deferred to Step 4.

**Step 2 — Toggle honesty (code, small)** — DONE 2026-07-16 (see `.claude/tasks/step2-toggle-honesty_onboarding.md`)
- [x] Guard `TradeResourcePacket.handle` with `ConfigLoader.tradingEnabled` (early return)
- [x] Guard `BidContractPacket` + `AcceptContractPacket` with `ConfigLoader.contractsEnabled`
- [x] Guard `Town.calculateWants()` with `researchEnabled` (clear wants + return)
- [x] Filter via extracted `TownManager.applyBiomeKit`: research off → unlock **`basic_settlement` only**; always apply pop/happiness/tourist/*_cap/border; skip ResourceRegistry grants when production off
- [x] Guard contract-item branch in `TownInterfaceEntity.processResourcesInSlot` with `contractsEnabled`
- [x] Tests: `ToggleHonestyTest` (4) — kit filter + calculateWants; full `:common:test` green

**Step 3 — Packaging & hygiene (one pass)** — IN PROGRESS; plan verified 2026-07-16 (see `.claude/tasks/step3-packaging-hygiene_onboarding.md`)
Decisions: (1) **remove** `minDistanceBetweenTowns`; (2) **generate placeholder icon** (owner replaces later).

**3a — Forge jar (release blocker)** — DONE 2026-07-16
- [x] Remove `exclude 'assets/**'/'data/**'/'pack.mcmeta'` from `forge/build.gradle`; add `duplicatesStrategy = DuplicatesStrategy.EXCLUDE`
- [x] Delete duplicate `OpenPaymentBoardPacket` registration in `ForgeModMessages`
- [x] Verified `:forge:build` jar contains `pack.mcmeta`, 29 `assets/` entries, 12 `data/` entries (incl. loot tables + recipes)

**3b — Dead code (13 files)** — DONE 2026-07-16
- [x] Deleted 4 Fabric stub packets + `fabric/block/TownInterfaceBlock` + 5 unused `fabric/api/` interfaces
- [x] Deleted 3 demo screens under `common/.../ui/screens/demo/`; updated `README-MODALGRID.md` pointer
- [x] `:fabric:compileJava` / `:common:compileJava` green

**3c — Log hygiene** — DONE 2026-07-16
- [x] Converted/removed remaining `System.out`/`System.err` in Forge init, ForgeRegistryHelper, Fabric init, FabricModBlockEntities, TownMapModal, BCComponent → slf4j LOGGER

**3d — Metadata & listing polish** — DONE 2026-07-16
- [x] `gradle.properties` `mod_version` → `0.9.0-beta`
- [x] `mods.toml`: `issueTrackerURL`/`displayURL` → GitHub; `logoFile`; description aligned with Fabric + beta caveat
- [x] `fabric.mod.json`: `contact.homepage`/`sources`/`issues` filled
- [x] Placeholder `assets/businesscraft/icon.png` (256×256; replace later)
- [x] Root `CHANGELOG.md` for 0.9.0-beta

**3e — Repo hygiene** — DONE 2026-07-16 (staged; commit when you ask)
- [x] Expanded `.gitignore`: `vault/.obsidian/` + `fabric/.gradle/`
- [x] `git rm --cached` the 8 tracked files (still on disk; no longer in index)

**3f — Save-format version** — DONE 2026-07-16
- [x] `TownSavedData`: `dataVersion` root key, `CURRENT_DATA_VERSION = 1`, missing key → 1, save always stamps
- [x] Stepwise `applyMigrations` + `migrateStep` hook (no production migrators yet); newer-than-supported → warn + best-effort
- [x] `TownSavedDataVersionTest` (6); also fixed `NAME` to stable `"businesscraft_towns"` (was PlatformAccess at class-init)
- Later: same pattern on Contract/Market SavedData when they need a break; bump CURRENT + add `case 1:` in `migrateStep`

**3g — Dead config**
- [ ] `minDistanceBetweenTowns`: **remove** (user decision locked)

**Step 4 — Default flip + verification (last, as planned)**
- [ ] Set tourism-only defaults: craftable on; tourists on; production/research/trading/contracts off; economy values (`metersPerEmerald=1000`, milestone 1000m/5000m; `minPayoutDistance=100`); confirm free tourism CSV
  - ⚠️ `minPopForTourists=5` + no pop growth → oasis `pop:4` never spawns; bump kits or lower default. Manual Option C trace here.
- [ ] Dedicated-server boot smoke test, Forge + Fabric (Fabric is structurally fragile — client APIs in server-loaded `FabricModMessages`/`FabricEventCallbackHandler`; refactor ONLY if the test fails)
- [ ] Full SP pass both loaders + manual Create pass (note: Create Fabric detection is a silent string sniff — `TouristEntity.java:564`; verify or document Forge-only). Docs must state tourists can't walk (speed 0) — routes need carts/trains
- [ ] Listing/README/wiki, publish

**Deferred (unchanged)**: Fabric class-split refactor (unless smoke test fails), tourist skin quality pass (owner), localization, multiplayer playtest, `UIGridBuilder` refactor.

**Status**: Steps 1–2 done; Step 3 through 3f done — next 3g then Step 4.

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
- [x] **Toggle-respect packet audit** — DONE 2026-07-16 via Ship Plan Step 2: packet guards (Trade/Bid/Accept), `applyBiomeKit` filtering, `calculateWants` + deposit-slot contract guards; UI tabs remain visible-but-inert (intentional). Tests in `ToggleHonestyTest`. Default flip still at end of Phase F.
- [x] Set `DebugConfig.TOURIST_ENTITY = false` (only flag still on; contradicts release checklist) — Done 2026-06-18. Only remaining production debug flag turned off.
- [x] Remove/convert `System.out.println` calls — DONE 2026-07-16 via Step 3c (stubs deleted in 3b; remaining → LOGGER)
- [x] Delete orphaned Fabric dead code — DONE 2026-07-16 via Step 3b
- [x] Remove demo screens — DONE 2026-07-16 via Step 3b
- [x] Fix Fabric build excluding loot tables from its jar (`fabric/build.gradle` line ~58) — also fixed root cause compile error (Forge-only `Block.getExplosionResistance(BlockState,...)` override). Removed exclude, loot tables now included for Fabric. Build now succeeds on both loaders. (2026-06-18)
- [x] Add mod icon — DONE 2026-07-16 via Step 3d (placeholder; owner replaces later)
- [x] Renumber `mod_version` to `0.9.0-beta` — DONE 2026-07-16 via Step 3d
- [x] Create `CHANGELOG.md` — DONE 2026-07-16 via Step 3d
- [x] Improve `mods.toml` description — DONE 2026-07-16 via Step 3d
- [x] **GitHub feedback links** — DONE 2026-07-16 via Step 3d
- [x] **Save-format versioning** — DONE 2026-07-16 via Step 3f (`TownSavedData.dataVersion`); README/listing beta caveat still Step 4
- [x] **Repo hygiene before going public** — DONE 2026-07-16 via Step 3e (`vault/.obsidian/` + `fabric/.gradle/` ignored + untracked; commit still pending)

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
- [ ] **Set 0.9 tourism-only defaults** (final step, right before release/publishing) — Update the default config so the public beta ships as tourism-only: `craftableTownInterface=true`; `tourists.enabled=true`; `milestones.enabled=true`; `production.enabled=false`; `research.enabled=false`; `trading.enabled=false`; `contracts.enabled=false`. Tourism generation still runs via Option C (tourist-owned `tourism` recipe). Also ship free tourism row in `productions.csv` as agreed. UI tabs remain visible but disabled systems do nothing. Move this late so full-mod testing is easy during development. Also do a final audit that no resources are granted or consumed for tourists, and that biome kits / starting values don't leak production/research content.
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
