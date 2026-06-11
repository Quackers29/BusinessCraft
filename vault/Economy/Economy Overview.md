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
- **[[Trade/Global Market/Price Calculation|Global Market Price Calculation]]** (T-006) — server-wide singleton: trades nudge prices 10% toward the deal price while accumulating volume; no-bid auctions apply 5% supply-pressure drops; hard floor 0.0001 on every path; persisted via MarketSavedData on the primary level.
- **[[Economy/Resources/Resource Type Expansion and Lookup|Resource Type Expansion and Lookup]]** (T-019) — csv (items.csv) defines logical resources (wood, iron, food, ...) to canonical MC items; auto-expands with fuzzy variants (logs, nuggets at 0.11×, blocks at 9×, foods by saturation ratio) so any Item can be classified and valued per-unit for trading, storage, contracts and the market UI.

## How it connects
Tourist arrivals are batched per origin town by the [[Town/Visits/Visit Buffer|Visit Buffer]] (~1s global quiet window, count+real-distance accumulation), then a single bundled reward (fare emeralds + any milestone items) is posted to the destination town's Payment Board for players to claim. The payment rate (`metersPerEmerald`) and milestone thresholds live in `businesscraft.toml` under `[economy]` and `[milestones]`.
