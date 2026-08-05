# Task: 0.9 Public Beta Release Preparation

**Context**: Public beta ships the polished tourism core as **0.9.x** (`tasks/ROADMAP_v0.9.md`). **v1.0** adds tourist transport contracts, prestige/"First to City", and Town Interface worldgen (`tasks/ROADMAP_v1.md`, `tasks/v1_worldgen.md`).

**Status**: Steps 1–3 + border cleanup **done**. Finish pre-Step-4 decisions, then Step 4 last.

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

**Earlier phases**
- [x] Phase A roadmap/docs reorg
- [x] Phase B hygiene (license MIT, debug off, Fabric loot jar, etc. — merged into Steps 2–3)
- [x] Phase D anti-cheat + min-payout (via Step 1)
- [x] Phase F unit coverage + T-012 (Test + Docs Loop parked; 39 ledger rows)
- [x] Scope cuts verified: no in-game handbook / next-suggestions UI; arrival polish + journey stats → v1; multiplayer playtest → v1; localization → v1

---

## Before Step 4 (get fully ready)

- [x] **Commit** pending work (done by owner)
- [x] **Oasis pop**: bumped oasis kit `pop:4` → `pop:5` (matches `minPopForTourists=5`; only kit that was below). Kept minPop at 5.
- [ ] **Tourist skins (strategy A, rebuilt from original PNGs)** — deferred (owner working on it); in-game OK still open
- [ ] **Phase C** (optional): fake-success UI audit — in progress (job assignment OK; Save→Ranking done)

### Tourist skin rework — rebuilt from original UV masks; await in-game OK

**Approach (2nd pass)**
- Deleted `scripts/generate_tourist_*.py` (no committed generators)
- Used restored `tourist_basic.png` + `tourist_hat_red.png` as UV/mask source of truth
- Hat overlays: same opaque mask as original red hat (480 px); clean solid crown/band colors
- Bodies: keep original head + camera; darken baked hat shell under overlay; recolor clothing for tiers
  - basic ≈ original safari (hat shell darkened)
  - experienced = deeper olive / browns
  - luxury = navy / charcoal (+ gold lens)

**Code**
- [x] No profession layer; hat layer renders `hat` + `hat_rim`
- [ ] **Owner**: Forge client — check tiers + colored hats sit on head

---

## Step 4 — Default flip + verification (LAST)

- [ ] Tourism-only defaults: `craftableTownInterface=true`; tourists/milestones on; production/research/trading/contracts **off**; `metersPerEmerald=1000`; milestones 1000m→apple, 5000m→bread; `minPayoutDistance=100`; confirm free tourism CSV
- [ ] Manual Option C in-game trace (tourism with production off; oasis pop=5 OK)
- [ ] Dedicated-server boot smoke — Forge + Fabric (refactor Fabric server paths **only if** smoke fails)
- [ ] Full SP pass both loaders + manual Create pass (Create Fabric = class-name sniff; verify or document Forge-only). Docs: tourists can't walk (speed 0) — need carts/trains
- [ ] Performance smoke: multiple towns / many tourists (thresholds TBD in playtest)
- [ ] Listing (Modrinth + CurseForge) + README/wiki; beta save-compat caveat; publish

**Deferred (unchanged)**: Fabric class-split (unless smoke fails), localization, multiplayer playtest, `UIGridBuilder` refactor, GameTest for NEEDS-MC ledger rows (`toImprove.md`)

---

## Optional / owner

### Phase C — Fake-success UI (optional)
- [x] Job assignment / population viewer — not in live UI (`TownTabController` only registers overview/resources/production/settings; no Population tab; Assign Jobs only on dead `population` bottom-button path — leave for future)
- [x] Settings Save button → replaced with Ranking (same as Overview); Edit Name kept
- [ ] Remaining audit: visitor modal example data, `autoCollect`/`taxes`, contract player-name — fix/hide only if still present

---

## Not in 0.9 (v1.0+)
- Tourist Transport Contracts, prestige/"First to City", Town Interface worldgen → `ROADMAP_v1.md` / `v1_worldgen.md`
- Localization sweep → `ROADMAP_v1.md`
- `UIGridBuilder` refactor → `toImprove.md`

**Test + Docs Loop**: parked (seeds exhausted). Re-invoke when new logic lands.
