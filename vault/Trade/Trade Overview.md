---
tags:
  - overview
  - trade
---
# Trade Overview

**TL;DR**: Towns can auction surplus production via timed sell contracts (other towns bid emeralds + commit to courier delivery); winning bidder takes ownership after auction close, with courier or "snail mail" delivery; direct courier contracts also exist for fixed-reward deliveries.

## Processes in this area
- **[[Trade/Contracts/Auction Resolution|Auction Resolution]]** (T-003) — When a timed auction ends, the highest bidder wins: the seller receives the emeralds, the goods leave escrow for delivery, the courier's fee scales with the distance between the two towns, and the market records the sale price. Auctions that attract no bids return the goods to the seller and nudge the market price down.
- **[[Trade/Contracts/Sell Contract Lifecycle|Sell Contract Lifecycle]]** (T-004) — Selling a load of resources moves through clear stages — listed, bid on, won, in delivery, completed — with sane limits on quantity and price, a fallback "snail mail" delivery when no courier takes the job, and every stage surviving save/reload.
- **[[Trade/Contracts/Bid Selection and Clamping|Bid Selection and Clamping]]** (T-028) — Each participating town can only raise (never lower) its own emerald offer on an open auction; when the timer expires the single highest of those standing offers wins and drives the handoff to courier or snail-mail delivery.
- **[[Trade/Contracts/Courier Delivery Rewards|Courier Delivery Rewards]]** (T-005) — Couriers who carry auctioned goods are paid a fixed emerald reward once the full load has arrived at the right town; partial drop-offs accumulate until the delivery is complete, and dropping at the wrong town is rejected.
- **[[Trade/Contracts/Contract List and Detail ViewModels|Contract List and Detail ViewModels]]** (T-020) — when players browse the contract board, the server filters sell contracts into auction/active/history tabs, sorts by urgency or recency, pages the results, pre-formats every price, timer, status, and button flag, and builds rich per-contract details (including sorted bids and tooltips) so the client UI is a pure display.
- Contract items turn abstract sell and courier jobs into carryable enchanted packages (with purple glint and readable pickup/delivery/cargo lore) that embed the contract UUID and town details so delivery handoff at a town interface can validate and complete the job. [[Trade/Contracts/Contract Item Creation and Inspection]] (T-022)
- **[[Trade/Global Market/Price Calculation|Global Market Price Calculation]]** (T-006) — One shared market tracks a going price for every resource: each completed trade nudges the price toward what was actually paid, failed (no-bid) auctions push the price down slightly, and prices can never fall to zero. Prices survive server restarts.

## How it connects
Sell contracts are created by towns automatically offering their surplus ([[Town/Contracts/Autonomous Bidding and Sell Contract Creation|autonomous trading]]) or by players. Bids come from other towns — players through the UI, or the towns' own bidding logic. When the auction timer ends the winner is picked, the courier fee (the same distance-based formula used when budgeting a bid) is paid out, and the result feeds the global market's price discovery. Rewards and contract items land on the relevant town's [[Town/Payment Board/Reward Claims|Payment Board]].

Contract timing (auction duration, courier acceptance window, delivery time per distance) is configurable in `businesscraft.toml` and hot-reloads.
