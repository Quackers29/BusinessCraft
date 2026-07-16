# BusinessCraft 0.9 Roadmap — Public Beta ("The Polished Core Loop")

**Date**: June 2026 (updated July 2026)  
**Scope**: First public release as a **0.9.x beta** on Modrinth/CurseForge.  
**Guiding principle**: Ship the existing core tourism loop polished, understandable, and honest — no fake-success UI, no half-features. Let real player feedback shape the road to 1.0.

Concrete checklist: `tasks/todo.md`.

## Vision for 0.9
A new player should be able to found their first town, immediately grasp that "tourists traveling real distance through the world generates real revenue," and get satisfying feedback from real routes. The beta ships as a **tourism-only economy by default**: towns spawn/consume tourists and earn rewards — no production, trading, contracts, or research unless a server owner enables them. UI tabs stay visible; disabled systems do nothing. Craftable Town Interface on.

**Explicitly NOT in 0.9** (→ `ROADMAP_v1.md`): Tourist Transport Contracts, prestige / "First to City", Town Interface worldgen.

## Progress (July 2026)

### Done
- Release hygiene (Phase B / Steps 2–3): MIT license; toggle honesty (packets + kit filter + wants); debug flags off; dead code/demo screens gone; Forge jar ships assets/data; Fabric loot in jar; placeholder icon; `0.9.0-beta` + CHANGELOG; GitHub URLs in mod metadata; `TownSavedData.dataVersion`; repo ignore/untrack for obsidian + loom-cache (commit when asked)
- Tourism Option C: free `tourism` recipe owned by `tourists.enabled` (works with production off)
- Economy integrity: T-012 payment board; distance clamp (2× straight-line); `minPayoutDistance=100`
- Border cleanup: radius from border modifier / `defaultBorderRadius=50` (not population); optional CSV growth `pop:>80%pop_cap`
- Unit tests + vault: Test + Docs Loop parked (39 ledger rows)
- Scope cuts: no in-game handbook / next-UI; arrival polish + journey stats → v1; multiplayer playtest → v1; localization → v1

### Still open (Step 4 last — see todo.md)
- Pre-Step-4 decisions: oasis `pop:4` vs `minPopForTourists=5`; tourist skin finish vs defer; optional Phase C UI audit; commit pending changes
- Tourism-only default flip + economy TOML (`metersPerEmerald=1000`, example milestones)
- Dedicated-server smoke (Forge + Fabric), full SP both loaders, manual Create pass, performance smoke
- Listings + README/wiki + beta save caveat + publish

## Execution summary (maps to todo.md)

### 1. Release hygiene — DONE (code)
Tourism-only **defaults** still flip at Step 4 (kept late so full-mod testing stays easy). Toggle audit done — packet handlers + kit filtering respect disabled systems; UI tabs remain visible-but-inert.

### 2. Honest UI (Phase C) — optional
Not a beta blocker. Quick fake-success audit if time.

### 3. Onboarding & first session (Phase D)
- Default economy values ship at Step 4: **1 emerald / 1000 blocks**; example milestones (1000m→apple, 5000m→bread)
- ~~Distance anti-cheat~~ DONE · ~~Min-payout floor~~ DONE
- **Tourist skin**: owner quality call — finish before Step 4 or defer out of 0.9

### 4. Testing & release (Phase F / Step 4)
- ~~Unit coverage~~ DONE · ~~T-012~~ DONE
- Dedicated-server boot smoke (both loaders)
- Manual Create + minecart stress (owner); Create Fabric verify or document Forge-only
- Full SP both loaders; performance smoke
- Modrinth + CurseForge + README/wiki; GitHub Issues as feedback; beta save-compat caveat; publish

## Release readiness checklist (0.9.0-beta)
- [ ] All todo.md remaining items completed or explicitly deferred
- [x] DebugConfig flags default to false
- [x] License **MIT** (root, mods.toml, fabric.mod.json)
- [x] Packet/kit toggle honesty (tourism-only **defaults** still Step 4)
- [ ] *(optional)* Fake-success UI audit
- [ ] Full playtest Forge **and** Fabric (SP); Create manual pass
- [ ] Dedicated-server boot smoke both loaders
- [x] Save-format `dataVersion` in town NBT
- [ ] Beta save-compat caveat in README/listing
- [ ] Repo cleaned commit landed; GitHub links in metadata ([x] metadata done; commit pending)
- [ ] Performance acceptable (thresholds TBD in playtesting)
- [x] Changelog + version `0.9.0-beta`
- [ ] Listings published

---

**Status**: Active — Step 4 is the last execution block after pre-Step-4 decisions.  
**Next after ship**: `ROADMAP_v1.md` (contracts + prestige + worldgen).
