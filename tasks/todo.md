# Task: 0.9 Public Beta Release Preparation

**Context**: Public beta ships the polished tourism core as **0.9.x** (`tasks/ROADMAP_v0.9.md`). **v1.0** adds tourist transport contracts, prestige/"First to City", and Town Interface worldgen (`tasks/ROADMAP_v1.md`, `tasks/v1_worldgen.md`).

**Status**: Steps 1–3 + border cleanup + **Town Centre / Platform Interface town setup** **done**. Step 4 verification mostly open (see below).

---

## Done (compressed)

**Step 1 — Economy integrity**
- [x] T-012 payment board transactional buffer (`addItemFully`)
- [x] Distance clamp `min(traveled, 2× straightLine)` + `economy.minPayoutDistance` (default 100)
- [x] Tourism Option C — `tourists.enabled` owns free `tourism` recipe; production skips it

**Step 2 — Toggle honesty**
- [x] Packet guards: Trade / Bid / Accept; deposit-slot contracts
- [x] `calculateWants` gated; `applyBiomeKit` filters research/production grants
- [x] `ToggleHonestyTest` (4)

**Step 3 — Packaging & hygiene**
- [x] 3a Forge jar ships assets/data (`duplicatesStrategy`)
- [x] 3b Dead Fabric stubs + demo screens removed
- [x] 3c System.out → LOGGER
- [x] 3d `0.9.0-beta`, GitHub URLs, CHANGELOG, placeholder icon
- [x] 3e `.gitignore` + untrack obsidian/loom-cache (**staged — commit when asked**)
- [x] 3f `TownSavedData.dataVersion` + migrations hook
- [x] 3g Removed dead `minDistanceBetweenTowns`

**Border cleanup**
- [x] Border from modifier / `defaultBorderRadius=50` (not population)
- [x] Placement ≥ `defaultBorder + existing border` (100 for two unmodified fresh towns)
- [x] Production option: `border_expansion_production` (`pop:>80%pop_cap` → `border:1`) unlocked by `border_expansion`

**Town setup (current design — closed)**
- [x] **Town Centre** block defines town + boundary; no owner; name from `townNames`
- [x] **Platform Interface** blocks (craftable, inside boundary, cap 10, 5-block spacing) — one block per platform; settings on interface; Town Centre read-only platform list
- [x] Sign lamp = linked + enabled; spawns need enabled + path; breaking centre deactivates interfaces / dismisses tourists

**Earlier phases**
- [x] Phase A roadmap/docs reorg
- [x] Phase B hygiene (license MIT, debug off, Fabric loot jar, etc. — merged into Steps 2–3)
- [x] Phase D anti-cheat + min-payout (via Step 1)
- [x] Phase F unit coverage + T-012 (Test + Docs Loop parked; 39 ledger rows)
- [x] Scope cuts verified: no in-game handbook / next-suggestions UI; arrival polish + journey stats → v1; multiplayer playtest → v1; localization → v1
- [x] Town data snapshot dump: `/bc debug dump` (op) + standalone viewer in `tools/bc-dump-viewer/` (`tasks/town-data-snapshot-dump.md`)

---

## Before Step 4 (get fully ready)

- [x] **Commit** pending work (done by owner)
- [x] **Oasis pop**: bumped oasis kit `pop:4` → `pop:5` (matches `minPopForTourists=5`; only kit that was below). Kept minPop at 5.
- [x] **Tourist looks in game**: 16 `tourist_looks` presets, personal names, distance ranks, tourist screen (`docs/TOURIST_MODELS.md`) — **owner in-game visual/playtest sign-off still open**
- [x] **Phase C** (optional): fake-success UI audit — done for 0.9 (see Optional section)

---

## Step 4 — Default flip + verification (LAST)

- [x] Tourism-only defaults: `craftableTownInterface=true`; tourists/milestones on; production/research/trading/contracts **off**; `metersPerEmerald=100` (shipped default); milestones 1000m→apple, 5000m→bread; `minPayoutDistance=100`; `minPopForTourists=5`; confirm free tourism CSV
- [ ] Manual Option C in-game trace (tourism with production off; oasis pop=5 OK)
- [x] Dedicated-server boot smoke — Forge + Fabric (**2026-10-26 pass**: `tools/server-boot-smoke.sh`, both reached Done)
- [ ] Full SP pass both loaders + manual Create pass (Create Fabric = class-name sniff; verify or document Forge-only). Docs: tourists can't walk (speed 0) — need carts/trains
- [ ] Performance smoke: multiple towns / many tourists (thresholds TBD in playtest)
- [ ] Listing (Modrinth + CurseForge) + README/wiki; beta save-compat caveat; publish

**2026-10-26 automated checks (this pass)**: `./gradlew test` pass; Forge `tools/showcase.sh` all-views (19), platform, tourists (16 looks), tourist-screen, two-towns — all PASS. Fabric client showcase and full survival playtest **not** run this pass.

**Deferred (unchanged)**: Fabric class-split (unless smoke fails), localization, multiplayer playtest, `UIGridBuilder` refactor, GameTest for NEEDS-MC ledger rows (`toImprove.md`)

---

## Optional / owner

### Phase C — Fake-success UI (optional)
- [x] Job assignment / population viewer — not in live UI (dead `population` path; leave for future)
- [x] Settings Save button → replaced with Ranking (same as Overview); Edit Name kept
- [x] Hardcoded visitor modal — deleted `VisitorModalManager` + call chain (real visitor history kept)
- [x] `autoCollect` placeholder removed (ViewModel/State/Menu); unused, always false; no gameplay readers. `taxes` placeholder left (same pattern, not requested)
- [x] Contract courier player-name resolution → deferred to v1 (`ROADMAP_v1.md`)

---

## Not in 0.9 (v1.0+)
- Tourist Transport Contracts, prestige/"First to City", Town Interface worldgen → `ROADMAP_v1.md` / `v1_worldgen.md`
- Localization sweep → `ROADMAP_v1.md`
- Contract courier player-name lookup (shows ID/"Assigned" today) → `ROADMAP_v1.md`
- `UIGridBuilder` refactor → `toImprove.md`

**Test + Docs Loop**: parked (seeds exhausted). Re-invoke when new logic lands.

---

## Future ideas (not now)

- **Create Aeronautics** — planes, balloons, etc. as another tourist transport mode (alongside minecarts / Create trains).
- **Elevation-aware rewards** — factor vertical travel into tourist payout or milestone bonuses.
- **Achievements** — e.g. “Flying High” for extreme altitude journeys.
- **Speed bonuses** — extra rewards for deliveries faster than Create trains’ max speed (anti-exploit rules TBD).

See also `vault/Tourists/Future Tourism Ideas.md`.
