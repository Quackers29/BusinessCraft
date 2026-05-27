# BusinessCraft - First Release Evaluation & Improvement Ideas

**Note**: For a higher-level, phased, and prioritized view of the path to v1.0, see the dedicated **tasks/ROADMAP_v1.md** file. This document contains the raw detailed evaluation notes.

**Date**: Evaluation performed after Tourist Behavior Enhancement + Actual Distance Payment work.
**Scope**: Full codebase review for v1.0 / first public release readiness.
**Approach**: Explored from scratch — architecture, core loop, systems, polish, multiplayer, performance, UX.

---

## Overall Assessment

**Strengths**
- Extremely strong architecture for a Minecraft mod (component-based Town, sophisticated UI framework, packet boilerplate, viewmodel caching, world visualization, debug system).
- Rich feature set already implemented: Tourism with real distance tracking + milestones, Contracts, Production/Upgrades, Global Market, Payment Board, multiple storage tiers.
- Excellent separation of concerns and production-grade patterns (provider, component, state binding, rate limiting).
- Good debuggability (`DebugConfig` with 25+ flags, hot-reload config).
- Multi-platform ready (common + forge/fabric modules).

**Maturity Level**: 75-80% toward a polished first release. The core tourism loop is functional and the distance metric upgrade brings it closer to a complete experience. Main gaps are in **player onboarding**, **balance/progression**, **visual & audio feedback**, and **documentation**.

---

## High-Priority Release Improvements

### 1. Player Onboarding & First Experience (Critical)
- No clear "how to start" flow. Players need to discover Town Interface block placement, population requirements, and platform building.
- **Suggestions**:
  - Add a simple in-game guide book or quest-like system on first town creation.
  - Improve initial notification when placing the first Town Interface block.
  - Add a config option for "tutorial mode" that gives starting resources or reduced requirements.

### 2. Core Tourism Loop Balance & Feedback
- Tourist spawning, travel, and reward loop exists but feels opaque.
- Payment rate (`metersPerEmerald = 50`) and milestone thresholds need tuning/playtesting.
- **Suggestions**:
  - Add more frequent positive feedback when tourists arrive (particles, better notifications).
  - Show "tourist journey" stats in the Town Overview UI (average distance, total revenue).
  - Consider making some early milestones very easy to give players quick dopamine.

### 3. UI Polish & Consistency
- The UI framework is excellent, but secondary screens (storage, trade, contracts) have less polish than the main town interface.
- **Suggestions**:
  - Add loading spinners or "empty state" messages in lists.
  - Improve iconography and tooltips across all tabs.
  - Add keyboard shortcuts for common actions (e.g. close all modals with Esc consistently).
  - Consider a "Quick Actions" bar on the main town screen.

### 4. Documentation & Wiki
- Currently almost zero player-facing documentation.
- **Suggestions**:
  - Create a basic Modrinth / CurseForge wiki page covering:
    - How to found your first town
    - Platform building & path creation
    - How tourists generate revenue
    - Contract system basics
  - Add in-game tooltips and hover descriptions where possible.

### 5. Performance & Long-Term Play
- Particle effects for platforms and visualizations are rate-limited (good), but many towns + many tourists could still cause issues.
- **Suggestions**:
  - Add a "simulation distance" or max active tourists per chunk config.
  - Consider lazy-loading of town data for very large worlds.
  - Profile tourist tick overhead when 50+ tourists are active.

### 6. Multiplayer & Edge Cases
- Strong server-client sync in many areas, but need verification on:
  - Tourist entity sync when riding vehicles across chunk boundaries.
  - Payment board claims and milestone rewards in multiplayer.
  - World unload / dimension travel edge cases for active tourists.

### 7. Visual & Audio Feedback
- Tourist behaviors (gossip, gazing, celebration) are implemented but subtle.
- **Suggestions**:
  - Add subtle particle effects when tourists celebrate or gossip.
  - Improve hat layer or add small visual variety to tourists.
  - Consider ambient town sounds or platform arrival chimes (vanilla only).

### 8. Minor Technical / Quality Items
- Gossip sound chance is currently `0.0f` in `TouristGossipGoal` — should be raised (0.10–0.15).
- Some debug logs still use `TOURIST_ENTITY` even in production paths.
- Consider adding a simple `/businesscraft help` or version command.
- Add mod version to debug overlay.

---

## Medium / Nice-to-Have Improvements

- Job assignment system (UI exists, logic is placeholder)
- More contract types and deeper contract economy
- Research / tech tree system (mentioned in global toggles)
- Territory visualization improvements
- Better Create mod train integration feedback
- Localization support (currently English only)
- Statistics / leaderboard expansion

---

## Release Checklist (Proposed)

- [ ] All high-priority items above addressed or explicitly deferred
- [ ] Full playtest on Forge + Fabric (singleplayer + multiplayer)
- [ ] Config values balanced with community feedback
- [ ] Basic documentation published
- [ ] Performance tested with 5+ active towns
- [ ] All debug flags default to `false`
- [ ] Changelog and version bump prepared

---

## Recommendation

The mod is in a strong position for a first release. The recent work on **actual tourist distance traveled** + milestone system gives it a unique and satisfying core loop.

I recommend focusing the next 1–2 weeks on **onboarding + feedback polish** rather than adding big new systems. Once players can easily understand and enjoy the tourism economy, the existing deep systems (contracts, production, upgrades) will shine.

---

**Next Step**: Would you like me to turn the highest-priority items into concrete, checkable tasks in `tasks/todo.md`, or focus on a specific area first (e.g. onboarding flow or balance tuning)?

---

## Raw Player Ideas: Tourist Contracts & Reputation System (May 2026)

**Raw notes from user** (lightly formatted for readability):

Tourist contracts  
- General contract, works and tourist?  
- Works only want to go to nearby towns ie shortest town wins  
- Select platform for contract start  
- Auto new contract every x hours  
- Vip contracts  
- Vip seats on a train  

Create train name filter per contract, can be fuzzy  
Or create train route checker even better  

Colour passenger names based on destination  
With key toggle or config to disable name tag  

More automatic contracting based on available routes auto adds routes to network with editing add or remove  

Wage is good incentive but rep inc better  
More rep  
More vips better contracts  
Leaderboard inc  
Towns inc  
City creates from effort  
First to city?  

Company links to players  
Multiple players in a company working for it  
Players have a cap on contracts  
More rep more contracts  
Company better  
Add train names to your company or player  
Contracts only ride your trains  

Tourist walk towards seat prior to teleport, only for 1 sec ish  

Towns limited to platform number, can be upgrade maybe with tourism plus money  

Vip train or plane way more lucrative, speed is key there

---

### Synthesized & Expanded Version (for roadmap use)

- Distinguish between general/work contracts (which prefer short distances) and dedicated Tourist Transport Contracts.
- Allow contract issuers to choose a specific departure platform.
- Generate new tourist contracts automatically on a timer, intelligently based on existing routes.
- Introduce VIP contracts that require premium/VIP seating and pay substantially more.
- Add strong Create integration: fuzzy train name filtering or (preferably) full route validation per contract.
- Color tourist name tags by destination town, with client config/key toggle to disable.
- Build automatic network discovery that suggests or auto-creates contracts from a player's existing platforms and Create routes (with manual editing).
- Make **Reputation** the primary long-term incentive over wages — higher reputation unlocks better contracts, more VIPs, and higher capacity.
- Add leaderboards for contract performance, reputation, and "first to city" prestige milestones earned through sustained effort.
- Introduce Player Companies where multiple players collaborate, share capacity/reputation, register trains as company assets, and restrict some contracts to company trains only.
- Give players and companies contract capacity limits that scale with reputation and company standing.
- Improve tourist boarding so they walk toward their seat for ~1 second before teleporting.
- Add upgradeable platform count limits per town (costing tourism success + money).
- Make high-speed VIP trains and premium transport dramatically more profitable, rewarding investment in speed and quality.

**Notes**: These ideas represent a major expansion of the existing contract system (currently Sell + Courier only) into passenger/tourism transport. They have very strong synergy with the core "actual distance traveled" tourism loop and Create integration. This direction is likely too large for v1.0 and should be treated as a major post-release pillar. See the dedicated section in `tasks/ROADMAP_v1.md` for the structured version of these ideas.