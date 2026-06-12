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
License alignment, **0.9 tourism-only defaults** (craftable on; production/research/trading/contracts off; tourist spawning must not cost resources; towns do not accumulate/trade resources unless owner enables those systems), audit all code paths respect the toggles, debug flags off, dead code/demo screens removed, mod icon, changelog, version renumbered to 0.9.0-beta. *Verified*

### 2. Honest UI (todo.md Phase C)
Every button either works or doesn't exist: job assignment UI hidden (v2 logic), settings save/reset implemented or removed, visitor modal wired to real data, placeholder toggles removed.

### 3. Onboarding & first 30–60 minutes (todo.md Phase D — highest retention value)
- Clear immediate feedback + guidance on first Town Interface placement (population requirements, what to build next).
- "What should I do next?" suggestions area on the main town screen, driven by town state.
- Lightweight Founder's Handbook / journal item granted on first placement.
- First 2–3 milestone rewards significantly easier to unlock (early dopamine).

### 4. Core loop feedback & feel (todo.md Phase D)
- Clearer/louder tourist-arrival feedback (particles, notifications, vanilla sound events).
- Journey statistics (avg distance, total tourism revenue, repeat visitors) in the Town Overview tab.
- Play-test re-tune of `metersPerEmerald` and milestone thresholds.

### 5. Localization sweep (todo.md Phase E)
All hardcoded UI strings funneled through translatable keys + `en_us.json`. English only for 0.9; structure enables community translations.

### 6. Testing & release (todo.md Phase F)
- Unit test coverage: DONE — Test + Docs Loop delivered 39 covered targets and a full documentation vault (`vault/`).
- Fix the two payment board claim bugs found by the loop (T-012) before beta.
- Multiplayer playtest pass (payment claims, milestones, personal storage) **plus tourism-only config profile**: large subsystems off, core loop still functions; then spot-check with production/trading enabled.
- Tourist vehicle stress test (minecarts + Create contraptions, chunk boundaries, server restarts, long journeys).
- Performance check: 5 active towns, 50+ simultaneous tourists.
- Full pass on BOTH loaders (Forge + Fabric).
- Modrinth/CurseForge listing + getting-started docs (the vault's plain-language overview layer is the starting material).

## Release readiness checklist (0.9.0-beta)
- [ ] All todo.md Phase A–F items completed or explicitly deferred with a tracking entry
- [ ] All DebugConfig flags default to false
- [ ] License consistent across root LICENSE, mods.toml, fabric.mod.json
- [ ] Tourism-only defaults verified in code: UI visible, subsystems dormant; tourist loop works with production/trading/contracts/research off (*Verified — scope*)
- [ ] No fake-success UI remains
- [ ] Full playtest pass on Forge and Fabric (singleplayer + multiplayer + Create integration)
- [ ] Performance acceptable with 5 towns / 50+ tourists
- [ ] Changelog written, version set to 0.9.0-beta, listing published

---

**Status**: Active — this is the current release target.
**Next**: After 0.9 ships and feedback arrives, development moves to `ROADMAP_v1.md` (Tourist Transport Contracts + prestige/"First to City" + Town Interface worldgen — the features that earn the 1.0).
