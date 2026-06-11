---
tags:
  - overview
  - trade
---
# Trade Overview

**TL;DR**: Towns can auction surplus production via timed sell contracts (other towns bid emeralds + commit to courier delivery); winning bidder takes ownership after auction close, with courier or "snail mail" delivery; direct courier contracts also exist for fixed-reward deliveries.

## Processes in this area
- **[[Trade/Contracts/Auction Resolution|Auction Resolution]]** — expired SellContracts with bids resolve to the highest bidder (see getHighestBidder + close logic); emeralds (bid) transfer to seller, goods released from escrow, courier reward computed as ceil(euclidean distance / 10), market records the effective transaction price; no-bid auctions refund goods to seller + apply supply pressure. Courier cost also computed at bid time for escrow + refunds.
- **[[Trade/Contracts/Sell Contract Lifecycle|Sell Contract Lifecycle]]** *(T-004, pending)* — creation clamping, state machine (expired / completed / delivered / courier assigned), NBT round-trips, bid state.
- **[[Trade/Contracts/Courier Delivery Rewards|Courier Delivery Rewards]]** *(T-005, pending)* — reward calc on delivery complete, wrong-town rejection, snail-mail vs player courier paths.

## How it connects
Sell contracts are proposed by a town's production surplus (via TownContractComponent auto-offer logic) or players. Bids come from other towns (player-initiated via UI or town AI bidding in TownContractComponent). On close (ContractBoard.tick → closeAuctions), the winner is chosen using the pure bid map logic in the base Contract class. The courier cost formula is shared for pre-bid escrow calc and post-resolution reward. Successful closes feed Global Market price discovery and supply/demand signals (recordTrade / recordFailedAuction). Rewards and contract items land on the destination/seller town's Payment Board.

Contract timing (auction duration, courier acceptance window, delivery time per meter) lives in ConfigLoader under `[contracts]` and hot-reloads.
