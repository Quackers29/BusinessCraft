---
tags:
  - overview
  - economy
---
# Economy Overview

**TL;DR**: Towns earn emeralds from arriving tourists (distance-based fares + milestone bonuses), delivered to a claimable Payment Board; a global market and currency config round out the economy.

## Processes in this area
- **[[Economy/Tourist Payments/Distance Payment Calculation|Distance Payment Calculation]]** — tourists pay `distance ÷ metersPerEmerald × count` emeralds per arrival batch (default rate: 1 emerald per 50 blocks), minimum 1 emerald per batch, truncated down. Uses the *real* path the tourist traveled, not straight-line distance.
- **[[Economy/Milestones/Distance Milestone Resolution|Distance Milestone Resolution]]** — highest configured distance threshold <= average travel distance awards scaled item rewards (default 10m: 1 bread + 2 exp bottles per tourist) delivered to Payment Board.
- **Global Market Price Calculation** *(T-006, pending)* — server-wide market pricing and restock behavior.

## How it connects
Tourist arrivals are batched per origin town (~1s window), then a single bundled reward (fare emeralds + any milestone items) is posted to the destination town's Payment Board for players to claim. The payment rate (`metersPerEmerald`) and milestone thresholds live in `businesscraft.toml` under `[economy]` and `[milestones]`.
