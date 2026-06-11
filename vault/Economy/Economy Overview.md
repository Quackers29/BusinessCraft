---
tags:
  - overview
  - economy
---
# Economy Overview

**TL;DR**: Towns earn emeralds from arriving tourists (distance-based fares + milestone bonuses), delivered to a claimable Payment Board; a global market and currency config round out the economy.

## Processes in this area
- **[[Economy/Tourist Payments/Distance Payment Calculation|Distance Payment Calculation]]** (T-001) — Each batch of arriving tourists pays the destination town emeralds based on how far they actually traveled along their route (default: 1 emerald per 50 blocks, always at least 1) — real path distance, not straight-line.
- **[[Economy/Milestones/Distance Milestone Resolution|Distance Milestone Resolution]]** (T-002) — Long journeys earn bonus items on top of the fare: the longest configured milestone distance the trip beats decides the reward (default: bread and experience bottles, scaled per tourist), delivered to the payment board.
- **[[Trade/Global Market/Price Calculation|Global Market Price Calculation]]** (T-006) — One shared market tracks a going price for every resource: each completed trade nudges the price toward what was actually paid, auctions that attract no bids push the price down slightly, and prices can never fall to zero. Prices survive server restarts.
- **[[Economy/Resources/Resource Type Expansion and Lookup|Resource Type Expansion and Lookup]]** (T-019) — A simple editable file maps game items to the mod's resource categories (wood, iron, food, ...), and the mod automatically fills in related variants (planks, nuggets, blocks, different foods) with sensible relative values, so anything a player trades or stores can be classified and priced.

## How it connects
Tourist arrivals are batched per origin town by the [[Town/Visits/Visit Buffer|Visit Buffer]] (arrivals within about a second are grouped), then a single bundled reward (fare emeralds + any milestone items) is posted to the destination town's [[Town/Payment Board/Reward Claims|Payment Board]] for players to claim. The payment rate and milestone thresholds are configurable in `businesscraft.toml`.
