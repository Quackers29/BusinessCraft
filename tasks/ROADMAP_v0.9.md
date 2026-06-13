# BusinessCraft 0.9 Roadmap — Public Beta ("The Polished Core Loop")

**Date**: June 2026 (supersedes the May 25 draft of ROADMAP_v1, which has been split: the lean beta scope lives here, the deferred headline features moved to the new `ROADMAP_v1.md`)
**Scope**: First public release as a **0.9.x beta** on Modrinth/CurseForge.
**Guiding principle**: Ship the existing core tourism loop polished, understandable, and honest — no fake-success UI, no half-features. Let real player feedback shape the road to 1.0.

## Vision for 0.9
A new player should be able to found their first town, immediately grasp that "tourists traveling real distance through the world generates real revenue," receive frequent satisfying feedback, and always know what to do next. The beta ships as a **tourism-only economy by default**: towns spawn/consume tourists and earn rewards — no production resource loops, no town trading, no contracts, no research unless a server owner deliberately enables them after reviewing config. UI tabs stay visible; disabled systems simply do nothing. Craftable Town Interface on.

**Explicitly NOT in 0.9** (moved to `ROADMAP_v1.md`): Tourist Transport Contracts, the prestige / "First to City" end-goal system, and Town Interface worldgen.

## Execution plan
The concrete, checkable task list lives in `tasks/todo.md` (Phases A–F). Summary of what 0.9 includes:

### 1. Release hygiene (todo.md Phase B)
License alignment (MIT — root LICENSE is currently GPLv3, mods.toml "All rights reserved"), **0.9 tourism-only defaults** (craftable on; milestones on; production/research/trading/contracts off; tourist spawning must not cost resources; towns do not accumulate/trade resources unless owner enables those systems), audit all code paths respect the toggles, debug flags off, dead code/demo screens removed, mod icon, changelog, version renumbered to 0.9.0-beta. *Verified*

**Toggle audit (code check, June 2026)**: toggles currently gate only tick paths — no network packet handler checks any toggle, and biome kits apply unconditionally at town registration. With UI tabs visible, player actions may still drive "disabled" systems server-side. Confirm exposure per packet, then gate handlers and/or hide tabs (todo Phase B). Might be small — verify before sizing.

Also in Phase B: GitHub repo as the feedback channel (issue/source URLs in `mods.toml` + `fabric.mod.json`, listing points to GitHub Issues), save-format versioning (data-version int in town NBT) + explicit "beta — worlds may not carry to v1.0" caveat in README/listing, and repo hygiene (untrack run logs/usercache/build tmp via one-time `git rm --cached`; `.gitignore` alone can't untrack tracked files).

### 2. Honest UI (todo.md Phase C) — optional quick pass
Not a beta blocker. If time: audit for leftover fake-success/placeholder UI (job assignment, visitor modal, settings buttons, etc.); fix only what still exists. *Verified*

### 3. Onboarding & first 30–60 minutes (todo.md Phase D — highest retention value)
- ~~First-placement in-game guidance~~ — covered by wiki/README, not 0.9 code. *Verified*
- ~~"What should I do next?" UI~~ — cut permanently. *Verified*
- No in-game manual for 0.9 — external docs only (see §6). *Verified*
- Default economy: **1 emerald / 1000 blocks**; example milestones (1000m→apple, 5000m→bread) — payments are main income, milestones are small bonus exemplars. *Verified*
- **Distance anti-cheat**: stop loop-back track farming of the per-~2s distance counter (min delta / direction / slower sampling). *Verified*
- **Min-payment floor check** (sibling of anti-cheat): payment has a `max(1, …)` emerald floor and no minimum payout distance — adjacent towns + free tourists = 1 emerald per batch. Add a min payout distance or drop the floor (decide at implementation).
- **Tourist skin & renderer**: finish the in-progress custom tourist texture (renderer/hat layer/`tourist_basic.png`) + entity skin check pass — current skin not satisfying.

### 4. Core loop feedback & feel (todo.md Phase D)
- ~~Arrival feedback polish~~ — v1 (no extra chat; maybe particles + villager sound on payment/train exit). *Verified*
- ~~Extra journey stats on Overview~~ — v1 polish (0.9 has tourism count already). *Verified*
- Play-test economy defaults + distance anti-cheat; owners tune TOML.

### 5. ~~Localization sweep~~ — v1.0 (0.9 ships hardcoded English). *Verified*

### 6. Testing & release (todo.md Phase F)
- Unit test coverage: DONE — Test + Docs Loop delivered 39 covered targets and a full documentation vault (`vault/`).
- Fix payment board `toBuffer` bugs (T-012) — **0.9 blocker**. *Verified*
- ~~Multiplayer playtest~~ — v1.0; 0.9 = singleplayer + manual Create pass. *Verified*
- **Dedicated-server boot smoke test** (Forge + Fabric): headless server, found a town, spawn tourists — cheap insurance since beta users will run servers day one even without the full multiplayer playtest.
- Tourist vehicle stress test: minecarts + **Create required** — **manual playtest by owner only, no automated Create testing**. Create Fabric is a separate port (class-name detection) — verify in the manual pass or document Create as Forge-only for 0.9. *Verified*
- Performance smoke test: multiple towns + many tourists (no fixed numbers yet). *Verified*
- Full pass on **both loaders (Forge + Fabric)** — dual-platform ship required for 0.9+. *Verified*
- **Player docs (no in-game handbook)**: Modrinth **and** CurseForge listing + GitHub README — concept, tourism-only defaults, link to wiki, **GitHub Issues as the feedback/bug channel**, beta save-compat caveat. Wiki source = `vault/` plain-language layer (GitHub wiki). Both platforms at 0.9 beta (standard for multi-loader mods). *Verified*

## Release readiness checklist (0.9.0-beta)
- [ ] All todo.md Phase A–F items completed or explicitly deferred with a tracking entry
- [ ] All DebugConfig flags default to false
- [ ] License **MIT** aligned across root LICENSE, mods.toml, fabric.mod.json — *Verified*
- [ ] Tourism-only defaults verified in code: UI visible, subsystems dormant; tourist loop works with production/trading/contracts/research off (*Verified — scope*); **packet-handler toggle audit done** (player actions can't drive disabled systems)
- [ ] *(optional)* Quick fake-success UI audit — not required for beta ship
- [ ] Full playtest on Forge **and** Fabric (singleplayer); **Create required** (manual only). Multiplayer → v1.0. *Verified*
- [ ] Dedicated-server boot smoke test passed on both loaders
- [ ] Save-format data-version in town NBT; beta save-compat caveat in README/listing
- [ ] Repo cleaned for public (run logs / usercache / build tmp untracked); GitHub issue/source links in mod metadata
- [ ] Performance acceptable with multiple towns / many tourists (thresholds TBD in playtesting)
- [ ] Changelog written, version set to 0.9.0-beta, listing published

---

**Status**: Active — this is the current release target.
**Next**: After 0.9 ships and feedback arrives, development moves to `ROADMAP_v1.md` (Tourist Transport Contracts + prestige/"First to City" + Town Interface worldgen — the features that earn the 1.0).
