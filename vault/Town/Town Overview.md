---
tags:
  - overview
  - town
---
# Town Overview

**TL;DR**: A Town is the mod's central entity — it owns a name, position, population, resource storage, a payment board, platforms, visit history, and boundary rules. Tourism grows population, and population unlocks capacity and upgrades.

## Processes in this area
- **[[Town/Resources/Resource Storage Operations|Resource Storage Operations]]** (T-007) — Every town keeps a ledger of resources (wood, food, emeralds, anything). Adding and spending is safe by design: counts can never overflow or go negative, and everything survives save/reload.
- **[[Town/Visits/Visit Buffer|Visit Buffer]]** (T-010) — Tourists arriving around the same time from the same origin town are grouped into one batch (about a 1-second window), so the town gets one combined payment and one notification instead of spam.
- **[[Town/Leaderboard/Ranking Calculation|Ranking Calculation]]** (T-011) — The leaderboard screen ranks towns by distance from you, population, visits, or total tourism, and formats distances nicely (meters under 1 km, kilometers above).
- **[[Town/Payment Board/Reward Claims|Reward Claims]]** (T-012) — All town income (tourist fares, milestone bonuses, trade profits, courier payouts) lands on a payment board as claimable entries. Some are open to everyone, others reserved for a specific player (e.g. the courier who did the work). Entries expire after 7 days and the board keeps at most 100.
- **[[Town/Boundaries/Town Distance Validation|Town Distance Validation]]** (T-008) — Towns can't be founded too close to each other. Each town has a boundary radius that grows with population, and new placements/expansions are checked against every existing town's boundary. Town names are also validated here (length, characters, appropriateness).
- **[[Town/Storage/Slot-Based Storage|Slot-Based Storage]]** (T-013) — A chest-like grid storage used by the payment board's claim buffer (18 slots). Items stack into matching slots first, then empty ones, exactly like you'd expect from a chest.
- **[[Town/Platforms/Platform Data Model|Platform Data Model]]** (T-015) — Each town can have up to 10 platforms: a start/end path where tourists arrive and depart. A platform can target specific destination towns or be open to "any town", and can be toggled on/off.
- **[[Town/Platforms/Platform Management|Platform Management]]** (T-023) — The owner of a town's platform list: it enforces the hard cap of 10, records path and destination changes from the UI, keeps a separate snapshot for the client screen and world renderer, writes compact NBT, and turns old single-path saves into a "Main Platform" entry on first load.
- **[[Tourists/Tourists Overview|Tourist Capacity & Allocation]]** (T-009) — How many tourists a town can send depends on its population, and destinations are chosen fairly so bigger towns receive proportionally more visitors.

## How it connects
A town is born when a player places a Town Interface block. From there, everything loops through the town's resources and population: tourists generate fares (→ payment board), fares and trade fill the resource ledger, resources feed production and upgrades, and population growth from tourism raises the tourist capacity — closing the loop. Boundaries keep neighboring towns honest, and the visit history records who came from where.
