---
tags:
  - overview
  - trade
---
# Trade Overview

**TL;DR**: Towns can auction surplus production via timed sell contracts (other towns bid emeralds + commit to courier delivery); winning bidder takes ownership after auction close, with courier or "snail mail" delivery; direct courier contracts also exist for fixed-reward deliveries.

## Processes in this area
- **[[Trade/Contracts/Auction Resolution|Auction Resolution]]** — expired SellContracts with bids resolve to the highest bidder (see getHighestBidder + close logic); emeralds (bid) transfer to seller, goods released from escrow, courier reward computed as ceil(euclidean distance / 10), market records the effective transaction price; no-bid auctions refund goods to seller + apply supply pressure. Courier cost also computed at bid time for escrow + refunds.
- **[[Trade/Contracts/Sell Contract Lifecycle|Sell Contract Lifecycle]]** — creation clamping [1,10M qty; 0.01,1M price], state machine (expired / completed / isDelivered / isDeliveryComplete / courier assigned / snail mail via zero UUID), NBT round-trips (all fields + bids), getCurrentBid always = ask (not live highest). (T-004)
- **[[Trade/Contracts/Courier Delivery Rewards|Courier Delivery Rewards]]** (T-005) — fixed reward (ctor input) paid as emeralds on full delivery via processCourierDelivery; destination-town match guard ("wrong town") lives in TownInterfaceEntity caller; CourierContract delivery accumulator + NBT; no snail path. Process orchestration needs MC (Town/PaymentBoard).
- **[[Trade/Contracts/Contract List and Detail ViewModels|Contract List and Detail ViewModels]]** (T-020) — when players browse the contract board, the server filters sell contracts into auction/active/history tabs, sorts by urgency or recency, pages the results, pre-formats every price, timer, status, and button flag, and builds rich per-contract details (including sorted bids and tooltips) so the client UI is a pure display.
- **[[Trade/Global Market/Price Calculation|Global Market Price Calculation]]** (T-006) — server-wide singleton price discovery: trades blend price 10% toward transacted unit price (volume accumulates as long); failed auctions drop price 5% (supply pressure); all paths clamp to floor 0.0001; full NBT roundtrip with cross-world hygiene on load.

## How it connects
Sell contracts are proposed by a town's production surplus (via TownContractComponent auto-offer logic) or players. Bids come from other towns (player-initiated via UI or town AI bidding in TownContractComponent). On close (ContractBoard.tick → closeAuctions), the winner is chosen using the pure bid map logic in the base Contract class. The courier cost formula is shared for pre-bid escrow calc and post-resolution reward. Successful closes feed Global Market price discovery and supply/demand signals (recordTrade / recordFailedAuction). Rewards and contract items land on the destination/seller town's Payment Board.

Contract timing (auction duration, courier acceptance window, delivery time per meter) lives in ConfigLoader under `[contracts]` and hot-reloads.
