---
tags:
  - overview
  - tourists
---
# Tourists Overview

**TL;DR**: Tourists are AI villagers that spawn at towns (once population >= minPopForTourists), travel along platforms/paths (or foot/minecarts/trains), and on arrival at a destination trigger emerald payments to that town (distance-based) plus optional milestone item rewards; towns track concurrent tourist counts against capacity and use population-proportional allocation for choosing which destination gets the next spawn from a given origin.

## Processes in this area
- **[[Tourists/Capacity/Tourist Allocation|Tourist Allocation]]** (T-009) — `TouristAllocationTracker` maintains per-origin current vs. population-weighted target allocations; `selectFairDestination` biases toward the most under-allocated destination (with 10% random-among-unders). In current code `recordTouristSpawn` is never called, so the bias is inert and selection falls back to map iteration order.
- Tourist spawning flow (platforms → TouristSpawningHelper → fair select → TouristEntity creation with expiry and origin/dest tags).
- Capacity rules: spawning eligibility gated by `pop >= ConfigLoader.minPopForTourists` (default 5) and `!town.canAddMoreTourists()` (which also caps at `ConfigLoader.maxTouristsPerTown` and the upgrade-driven `tourist_cap` modifier); `addTourist`/`removeTourist` now go through `TownService`.
- Tourist lifecycle on the entity: movement distance tracking (real path for payments), ride extension (minecart/train resets expiry), expiry removal, arrival processing that triggers payment + removal record in the allocation tracker.
- AI behaviors, gossip, gazing, and Create-train integration live in TouristEntity / TouristRenderer (not pure-logic testable here).

## How it connects
Tourist allocation and capacity sit between the Town population/economy systems and the actual spawning + payment pipelines. Fair destination choice (when multiple platforms targets) uses live `Town.getPopulation()` values at spawn time. On successful spawn the origin's touristCount goes up (via TownService); on arrival at dest the origin count goes down and a removal is recorded for fairness, plus the destination earns the distance payment (see Economy area) and the visit is recorded. The tracker itself is intentionally side-effecting static state so that repeated spawns from the same origin gradually balance load according to pop proportions.
