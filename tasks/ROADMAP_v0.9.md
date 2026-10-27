# BusinessCraft 0.9 Roadmap — Public Beta ("The Polished Core Loop")

**Date**: June 2026 (updated **October 2026**)  
**Scope**: First public release as a **0.9.x beta** on Modrinth/CurseForge.  
**Guiding principle**: Ship the existing core tourism loop polished, understandable, and honest — no fake-success UI, no half-features. Let real player feedback shape the road to 1.0.

Concrete checklist: `tasks/todo.md`.

## Vision for 0.9
A new player should be able to found their first town, immediately grasp that "tourists traveling real distance through the world generates real revenue," and get satisfying feedback from real routes. The beta ships as a **tourism-only economy by default**: towns spawn/consume tourists and earn rewards — no production, trading, contracts, or research unless a server owner enables them. UI tabs stay visible; disabled systems do nothing. Craftable Town Centre + Platform Interface blocks.

**Town setup (shipped)**: one **Town Centre** defines the town boundary; up to **10 Platform Interface** blocks inside the boundary are the platforms (settings on each block). Tourists **do not walk** — carts/trains only.

**Explicitly NOT in 0.9** (→ `ROADMAP_v1.md`): Tourist Transport Contracts, prestige / "First to City", Town Interface worldgen.

## Progress

### Done
- Release hygiene (Phase B / Steps 2–3): MIT license; toggle honesty (packets + kit filter + wants); debug flags off; dead code/demo screens gone; Forge jar ships assets/data; Fabric loot in jar; placeholder icon; `0.9.0-beta` + CHANGELOG; GitHub URLs in mod metadata; `TownSavedData.dataVersion`; repo ignore/untrack for obsidian + loom-cache (commit when asked)
- Tourism Option C: free `tourism` recipe owned by `tourists.enabled` (works with production off)
- Economy integrity: T-012 payment board; distance clamp (2× straight-line); `minPayoutDistance=100`
- Border cleanup: radius from border modifier / `defaultBorderRadius=50` (not population); optional CSV growth `pop:>80%pop_cap`
- Tourism-only **defaults** flipped: `metersPerEmerald=100`; milestones 1000m→apple, 5000m→bread; subsystems locked as documented in `todo.md`
- Town Centre + Platform Interface design implemented (not an open design item)
- Tourist catalog: **16 looks**, names, ranks, tourist screen (`docs/TOURIST_MODELS.md`)
- Unit tests + vault: Test + Docs Loop parked (39 ledger rows)
- Scope cuts: no in-game handbook / next-UI; arrival polish + journey stats → v1; multiplayer playtest → v1; localization → v1
- **Dedicated-server boot smoke** (Forge + Fabric): **2026-10-26** — `tools/server-boot-smoke.sh`, both loaders reached Done
- **Automated regression (2026-10-26)**: `./gradlew test`; Forge showcase all-views / platform / tourists / tourist-screen / two-towns PASS

### Still open (Step 4 — see todo.md)
- Manual Option C in-game trace
- Full SP both loaders, manual Create pass, performance smoke
- Owner in-game OK on tourist looks (not a blocker for automated checks)
- Listings + README/wiki polish + beta save caveat + publish

## Execution summary (maps to todo.md)

### 1. Release hygiene — DONE (code)
Tourism-only defaults shipped. Toggle audit done — packet handlers + kit filtering respect disabled systems; UI tabs remain visible-but-inert.

### 2. Honest UI (Phase C) — optional / largely done
Quick fake-success audit completed for 0.9 (see `todo.md`).

### 3. Onboarding & first session (Phase D)
- Default economy: **1 emerald / 100 blocks** (`metersPerEmerald=100`); example milestones (1000m→apple, 5000m→bread); `minPopForTourists=5`
- ~~Distance anti-cheat~~ DONE · ~~Min-payout floor~~ DONE
- **Tourist looks**: in game; owner visual/playtest sign-off still open

### 4. Testing & release (Phase F / Step 4)
- ~~Unit coverage~~ DONE · ~~T-012~~ DONE
- ~~Dedicated-server boot smoke (both loaders)~~ DONE (2026-10-26)
- Manual Create + minecart stress (owner); Create Fabric verify or document Forge-only
- Full SP both loaders; performance smoke
- Modrinth + CurseForge + README/wiki; GitHub Issues as feedback; beta save-compat caveat; publish

**CI**: manual only — `.github/workflows/build.yml` runs on `workflow_dispatch`.

## Release readiness checklist (0.9.0-beta)
- [ ] All todo.md remaining items completed or explicitly deferred
- [x] DebugConfig flags default to false
- [x] License **MIT** (root, mods.toml, fabric.mod.json)
- [x] Packet/kit toggle honesty + tourism-only defaults
- [x] Fake-success UI audit (0.9 scope)
- [ ] Full playtest Forge **and** Fabric (SP); Create manual pass
- [x] Dedicated-server boot smoke both loaders (2026-10-26)
- [x] Save-format `dataVersion` in town NBT
- [ ] Beta save-compat caveat in README/listing
- [ ] Repo cleaned commit landed; GitHub links in metadata ([x] metadata done; commit pending)
- [ ] Performance acceptable (thresholds TBD in playtesting)
- [x] Changelog + version `0.9.0-beta`
- [ ] Listings published

---

**Status**: Active — Step 4 verification and release packaging remain.  
**Next after ship**: `ROADMAP_v1.md` (contracts + prestige + worldgen).
