# BusinessCraft v1.0 Roadmap — Version 1

**Date**: May 25, 2026  
**Scope**: Path to a stable, delightful, and understandable first public release.  
**Guiding Principle**: Polish the existing excellent foundation and meaningfully expand the contract system into tourist transport (following the established town-initiated → player acceptance model, with recurring contracts as a key player feature) to make the core tourism fantasy click. Large multiplayer systems (companies, advanced automation) are intentionally deferred. The architecture is already production-grade.

## Vision for v1.0
A new player should be able to found their first town, immediately grasp that "tourists traveling real distance through the world generates real revenue," receive frequent satisfying feedback, and have obvious next steps — including being able to take on tourist transport contracts. The deeper systems (contracts, production, upgrades, global market) should feel meaningful once the core tourism fantasy clicks.

## Phase 0 — Stability & Technical Hygiene (Recently Advanced)
- The three new tourist AI goals (gossip, window gazing, and target gazing) require focused playtesting on Create trains, long minecart routes, high tourist counts, and across chunk boundaries.

## Phase 1 — Player Onboarding & First 30–60 Minutes (Highest Priority)
- Introduce a lightweight "Founder’s Handbook" or in-game journal item granted on first Town Interface placement that gently teaches the core loop, platform construction, and early progression.
- Dramatically improve the immediate feedback and guidance given when a player places their very first Town Interface block, including clearer explanations of population requirements and what to build next.
- Add a simple "What should I do next?" suggestions area on the main Town Interface screen that updates based on the town’s current state during the first several hours.

## Phase 2 — Core Tourism Loop Feedback & Feel
- Increase the frequency, clarity, and visual/audio impact of positive feedback when tourists successfully arrive and generate revenue, using particles, better notifications, and transient effects.
- Surface meaningful journey statistics (average distance traveled by tourists, total revenue from tourism, repeat visitor counts) directly in the Town Overview tab.
- Make the first two or three milestone rewards significantly easier to unlock so players receive early dopamine hits and understand the distance-based reward system quickly.
- Re-tune the base payment rate (metersPerEmerald) and milestone thresholds through actual play sessions rather than theoretical balancing.

## Phase 3 — Visual, Audio & Immersion Polish
- Add subtle particle effects and brief animations when tourists gossip with each other or celebrate high-speed travel, making the new AI behaviors more visible and delightful.
- Leverage the already-built modular world visualization system (LineRenderer3D, BoundaryRenderer3D, VisualizationManager, PathRenderer3D) to optionally display live tourist flow lines and town influence zones.
- Expand visual variety for tourists by building on the existing skin tier system, possibly adding small hat or accessory differences that reflect experience level.
- Introduce tasteful ambient audio cues for platform arrivals and successful tourist departures using only vanilla sound events.

## Phase 4 — Documentation & Accessibility
- Publish a clear, well-organized Modrinth and CurseForge wiki covering founding a town, building and connecting platforms, how distance-based revenue actually works, the contract system, and common troubleshooting.
- Add high-quality tooltips, hover descriptions, and explanatory text to every major UI element, block, and tab.
- Create a short "Getting Started" video or annotated screenshot series showing the most common successful first-hour path.

## Phase 5 — Performance, Multiplayer & Edge Cases
- Add configuration options to cap maximum active tourists and simulation distance to protect performance in large worlds or with many active towns.
- Thoroughly test tourist entities riding vehicles (minecarts, Create contraptions, boats) across chunk boundaries, dimension changes, server restarts, and long journeys.
- Verify that payment board claims, milestone rewards, personal storage, and contract mechanics all behave correctly and fairly in multiplayer environments.
- Profile and optimize the per-tick cost of TouristEntity when 30–100+ tourists are active on a single server.

## Phase 6 — Tourist Contracts & Supporting Polish (v1.0 Target)
- Implement the foundation of **Tourist Transport Contracts**, following the same town-initiated model as existing courier contracts: towns post tourist transport needs, and players accept the work of actually moving the tourists (distinct from resource courier contracts).
- Allow towns to specify departure platforms when creating tourist transport contracts.
- Add the ability for players to accept tourist contracts as **recurring contracts**, with the player choosing the frequency and timing of the repeats.
- Improve tourist boarding behavior so tourists walk toward their assigned seat or platform for a short time (~1 second) before teleporting.
- Add client-side name tag coloring for tourists based on destination town, with a config toggle or keybind to disable the feature.

## v1 End Goals: Prestige System & "First to City" (Configurable End-Game Loop)
To give v1 a clear sense of long-term progression and an initial "win condition," a configurable prestige / end-goal system will be included:

- **"First to City" Town Goal** (Primary): A high, configurable tourism milestone for a town (e.g., total tourists transported, cumulative distance traveled by its tourists, or a combination). Default: enabled, set to a challenging but achievable number for a dedicated town.
- **Player Contribution Tracking & Display**: The existing ranking/leaderboard system will be upgraded to show **player contribution per town** (e.g., how much contracted transportation, distance, or revenue a player has generated for each town).
- **Player Contracted Transportation Goal** (Secondary): A separate configurable milestone for individual players based on total contracted tourist transportation they have personally completed (across one or more towns).
- Both goals are fully configurable (thresholds, enabled/disabled, scoring formulas) so servers can tune the difficulty or turn the end-goal loop off entirely.
- This provides v1 with a satisfying long-term loop: players help grow towns through tourism and contracts, see their personal impact on leaderboards, and work toward personal + town prestige milestones. These systems are intended as a solid starting point that can be significantly expanded and improved in v2.

## Additional Recommendations from Architecture Review
- The extremely powerful BCScreenBuilder and component-based UI system should be used to create a high-information "Town Management Dashboard" that surfaces the most important live data at a glance.
- Because the 3D world visualization framework is already modular, chunk-aware, and distance-culled, a modest investment here will produce outsized "wow" moments for players looking at their growing transportation network.
- The tourist AI systems are the newest and most visible gameplay to players; they deserve disproportionate attention and iteration in the v1.0 window even if that means deferring other features.
- Every new feature added from this point forward must live in the common module unless a platform-specific hook is genuinely unavoidable.

## v1.0 Release Readiness Checklist
- [ ] All items in Phases 0–6 plus the v1 prestige/end-goal system are either completed or explicitly marked as deferred past v1.0 (advanced contract expansions, reputation, automation, and company systems are the primary focus of v2 — see "Future Development" section).
- [ ] Foundational Tourist Transport Contracts (town-initiated, player-accepted, with recurring frequency option for players), boarding behavior polish, name tag coloring, and the configurable "First to City" prestige system (town tourism milestones + player contracted transportation goals with contribution tracking on leaderboards) are implemented and playable.
- [ ] Full playtest pass completed on both Forge and Fabric, including singleplayer, multiplayer, and Create mod integration.
- [ ] All DebugConfig flags default to false in release builds.
- [ ] Performance remains acceptable with at least five active towns and 50+ simultaneous tourists.
- [ ] Basic player documentation is published and linked from Modrinth.
- [ ] Changelog is written and version is set appropriately (1.0.0 or a public beta).

---

**Status**: Version 1 (Revised) — Tourist Contracts + configurable "First to City" prestige/end-goal system in v1.0.  
**Next**: Can be turned into concrete, checkable tasks in `tasks/todo.md` once the current world generation work reaches a good checkpoint.

This roadmap brings meaningful Tourist Contracts + a configurable prestige/end-goal system ("First to City" town tourism milestones + player contracted transportation goals with contribution tracking on leaderboards) into v1.0, so players have both a strong core loop and a clear long-term sense of progress and achievement. Heavier features (VIP contracts, reputation, advanced automation, and company systems) are intentionally deferred to v2. The focus remains on polish, understandability, and giving v1 a satisfying initial end-game loop.

---

## Future Development (v2 and Beyond)

The majority of the more ambitious contract, reputation, automation, and multiplayer company features that were explored during v1 planning have been deliberately moved to become the primary early focus of the v2 roadmap.

True long-term, cross-server federation work (global marketplace, inter-server tourist and worker migration, shared global leaderboards, etc.) remains targeted for v3.

This keeps v1 focused on delivering a solid, playable foundation with Tourist Contracts and an initial configurable prestige/end-goal system, while clearly signaling where the deeper systems will land.