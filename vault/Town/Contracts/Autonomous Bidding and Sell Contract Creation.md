---
tags:
  - detail
  - town
  - trade
---
# Autonomous Bidding and Sell Contract Creation

**Breadcrumb**: Town > Contracts > Autonomous Bidding and Sell Contract Creation
**TL;DR**: Towns autonomously detect excess resources above a configurable threshold, escrow the surplus, and post SellContracts at market price adjusted by excess ratio (+5% to -40% modifier) plus ±5% randomness (minimum 0.1 per unit); the same component scans the global board and submits need-based bids (0.80–1.20 modifier from stock deficit or "wanted" status for upgrades) that beat the current highest by at least max(1, 10%), respecting a 3×/5× budget cap (including courier cost) and mutual-exclusion rules so a town never simultaneously sells and buys the same resource.

## What it does
This is the AI "brain" that lets NPC towns participate in the player-driven economy without player intervention. When a town's storage (virtual + real) exceeds an "excess" percentage of its upgraded capacity for a registered tradeable resource, the town automatically lists a sell contract for the surplus, moving the goods into escrow so they can't be double-spent. 

Conversely, when a town's stock falls below its "min stock" threshold (or the item is explicitly wanted for an in-progress upgrade), the town will place competitive bids on other towns' sell auctions — but only after a 1-second delay (pendingBids), only if it can afford the bid + courier fee in emeralds, and never for resources it is already trying to sell. Bids and listings respect a hard cap of 3 active contracts per town.

The system creates natural price discovery and resource flow between towns: high-production towns liquidate surplus at slight discounts when very full; needy towns (or towns researching) pay premiums to acquire what they lack.

## How it works (process view)
- **Excess detection (tryCreateContract + checkAndCreateContract, every ~5s)**: For every non-emerald registered resource, compute total (available + escrowed). If total > cap × (excessStockPercent/100), and we are not already actively selling that resource, and we are not the highest bidder on someone else's auction for it, compute a sell quantity and price and post a SellContract.
- **Sell quantity**: targetStock = minThreshold + (excessThreshold - minThreshold)/2 ; sellQuantity = min(10000, total - target, available) ; must be > 0.
- **Sell price**: excessRatio = clamp( (total - excessThreshold) / max(1, cap - excessThreshold) , 0..1 ); modifier = 0.05 + excessRatio × (-0.45) + (random ±0.05); pricePerUnit = max(0.1, marketPrice × (1 + modifier + randomness)).
- **Escrow on post**: immediately town.addResource(item, -qty); town.addEscrowResource(item, qty).
- **Bidding (scanForBids every ~5s, processPendingBids every tick)**: For each visible non-expired non-closed SellContract from another town, if we need the resource (stock < min threshold or wanted for upgrade) and are not already selling it, compute a needRatio (1.0 when desperate or wanted, 0 when at threshold), derive bidModifier 0.80–1.20 + random, compute base = market × qty, courier = ceil(dist/10), then either beat current highest by max(1, 10%) or use first-bid formula, floor at 1, cap total outlay at max(3×base, 1+courier) or 5× if wanted, check we hold enough emeralds for (bid + courier), then either schedule (pendingBids map with +1000ms) or place immediately via board.addBid.
- **Mutual exclusion**: isSellingResource and isBuyingResource walk the board to prevent a town from listing a sell while it is highest bidder on a buy for the same resourceId (and vice versa).
- **In-transit accounting**: getInTransitResourceCount sums (qty - deliveredAmount) over SellContracts we have won (winningTownId == us) that are not yet fully delivered and not expired. Used by trading UI/stock views.

**Worked example (sell side)**: cap=1000, excess%=80 → excessThreshold=800; min%=20 → minThreshold=200; target=500. Suppose total=950 (all available). sellQty = 950-500=450 (capped later if needed). excessRatio=(950-800)/ (1000-800) = 150/200 = 0.75. modifier = 0.05 + 0.75×(-0.45) ≈ -0.2875 + random(-0.05..+0.05). If market=2.0, pricePerUnit ≈ 2.0 × (1 - 0.2875 + 0.01) ≈ 1.445 (then listed at that ask).

**Worked example (bid side)**: needThreshold=200, currentCount=40 → needRatio=(200-40)/200=0.8 → bidModifier≈0.80 + 0.8*0.40 = 1.12 + random. basePrice for 64 iron at market 2.0 = 128. First bid: ceil(128 * 1.12) = 144. Courier cost (assume 120m) = 12. Budget check: 3×128=384, minBudget=1+12=13 → maxTotal=384; maxBid=384-12=372. If 144 +12 < emeralds held, schedule/place the bid.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `TownContractComponent(Town)` | `common/src/main/java/com/quackers29/businesscraft/town/components/TownContractComponent.java` | Per-town autonomous participant in the contract economy |
| `tick()` | same (~50) | Drives processPendingBids (every tick) + tryCreateContract/scanForBids (every CHECK_INTERVAL≈5s) |
| `isSellingResource(String, ContractBoard)` (private) | same (~26) | True if any non-expired !completed SellContract with issuer==this town for the resource |
| `isBuyingResource(String, ContractBoard)` (private) | same (~35) | True if any non-expired !completed SellContract with highestBidder==this town for the resource |
| `tryCreateContract()` / `checkAndCreateContract(...)` (private) | same (~346, ~403) | Excess detection, qty/price math, escrow move, board.addContract |
| `scanForBids(long)` / `processPendingBids(long)` (private) | same (~201, ~69) | Need detection, needRatio/bidModifier math, courier inclusion, budget caps, emerald escrow check, pending 1s delay then board.addBid |
| `getInTransitResourceCount(String)` | same (~529) | Sum of undelivered qty on contracts we won (for UI/stock views) |
| `ContractBoard.calculateCourierCost(Town,Town)` (static) | `common/src/main/java/com/quackers29/businesscraft/contract/ContractBoard.java:539` | ceil( straight-line dist / 10 ) used in every bid budget |
| `SellContract` ctor + state | `contract/SellContract.java` | The thing being created/bid on (see [[Trade/Contracts/Sell Contract Lifecycle]]) |
| `ConfigLoader.{excessStockPercent,minStockPercent,contractAuctionDurationMinutes,...}` | `config/ConfigLoader.java` | Tunables feeding the thresholds, durations, and courier rates |

## Rules & formulas (exact)
All values are live at decision time (ConfigLoader, ResourceRegistry, board market prices, current Town stock via trading component).

**Creation guard (tryCreateContract / checkAndCreateContract)**:
- Only registered resources (ResourceRegistry.getFor(item) != null), never EMERALD.
- activeCount = count of issuer's non-expired !completed SellContracts < MAX_ACTIVE_CONTRACTS (3).
- resourceCount = town.getTotalResourceCount(item)  (available + escrow).
- excessThreshold = cap * (ConfigLoader.excessStockPercent / 100f)
- if (resourceCount > excessThreshold)
  - also guard !isSellingResource && !isBuyingResource
  - availableCount = town.getResourceCount(item)
  - minThreshold = cap * (minStockPercent/100f)
  - targetStock = minThreshold + (excessThreshold - minThreshold)/2
  - sellQuantity = resourceCount - (long)targetStock ; min(10000, sellQuantity, availableCount)
  - if (sellQuantity <= 0) skip
  - marketPrice = board.getMarketPrice(resourceId)
  - scalingRange = max(1f, cap - excessThreshold)
  - excessRatio = clamp( (resourceCount - excessThreshold) / scalingRange , 0f, 1f )
  - modifier = 0.05f + (excessRatio * (-0.45f))
  - randomness = (float)(Math.random()*0.10f)-0.05f
  - pricePerUnit = max(0.1f, marketPrice * (1f + modifier + randomness))
  - create SellContract(issuerId, issuerName, ConfigLoader.contractAuctionDurationMinutes*60000L, resourceId, sellQuantity, pricePerUnit)
  - board.addContract(...)
  - town.addResource(item, -sellQuantity); town.addEscrowResource(item, sellQuantity)

**Bidding formulas (scanForBids + processPendingBids, identical math)**:
- needThreshold = cap * (minStockPercent/100f)
- needRatio = needThreshold>0 ? clamp( (needThreshold - currentCount)/needThreshold , 0f, 1f ) : 1f
- if (isWanted) needRatio = 1f
- bidModifier = 0.80f + (needRatio * 0.40f) + (random ±0.05f)
- currentHighest = sc.getHighestBid(); marketPrice = ...; basePrice = marketPrice * qty; courierCost = ContractBoard.calculateCourierCost(thisTown, sellerTown)
- if (currentHighest > 0) {
    minInc = max(1f, currentHighest * 0.10f);
    bid = (float) Math.ceil(currentHighest + minInc);
  } else {
    bid = (float) Math.ceil(basePrice * bidModifier);
  }
- bid = max(1f, bid)
- multiplier = isWanted ? 5.0f : MAX_BID_MULTIPLIER (3.0f)
- calculatedBudget = basePrice * multiplier
- minBudget = 1f + courierCost
- maxTotalBudget = max(calculatedBudget, minBudget)
- maxBid = maxTotalBudget - courierCost
- if (bid > maxBid) skip
- if (emeraldCount < (bid + courierCost)) skip
- then either pendingBids.put(id, now+1000) or board.addBid(id, ourId, bid, level)

**Other rules**:
- pendingBids are re-evaluated fresh on expiry of the delay (currentHighest, market, need may have changed).
- Auction-closed contracts are never bid on or created against.
- Self-contracts (issuer == us) are ignored for bidding.
- getInTransitResourceCount only counts SellContracts where winningTownId==us && !isExpired && !isDelivered (sums qty-deliveredAmount).

## Edge cases & behaviors
- Zero or negative excess after calc → sellQuantity <=0 → no contract.
- Tiny surplus just above threshold but after target calc yields <=0 → no contract (the /2 midpoint creates a dead band).
- count=0 or missing resourceType → skipped early.
- No enabled destinations or platform logic is separate (TouristSpawningHelper); this component is purely about the contract board.
- Randomness in both creation price and bid modifier means two identical situations can produce different listings/bids — non-deterministic and non-replayable.
- Courier cost is straight-line (BlockPos.distSqr sqrt /10 ceil) even though tourist payments use path distance.
- MAX_ACTIVE=3 is hard; once at limit no more sells or bids are attempted until some complete/expire.
- Escrow check uses real emerald count at bid time; if emeralds are spent between schedule and execution the bid may be skipped (no auto-refund of pending intent).
- The 10000 hard cap on sellQuantity prevents a single contract from dumping an entire large stock in one listing.
- Mutual exclusion prevents "churn" where a town sells what it is simultaneously trying to acquire.
- lastContractCheckTime is only persisted (save/load); pendingBids map is transient (lost on reload — intentional?).

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/town/components/TownContractComponentTest.java`
- Covered: construction via Town ctor (McBootstrap), save/load roundtrip of lastContractCheckTime, getInTransitResourceCount safe return (0) when no TownManager registered.
- Not covered / intentionally NEEDS-MC: the full excess detection, price math, bid calculation, pending scheduling, board interaction, escrow moves, and all guards — these are private methods deeply coupled to live ContractBoard, TownManager static registry, ServerLevel, ResourceRegistry + PlatformAccess registry, ConfigLoader percents, and real Town resource/trading state. No pure entry points exist that can be exercised without a running server world.
- Formulas and edge rules are exhaustively specified in this note (hand-computed arithmetic included) so future integration tests or refactors have a spec.

## Open questions
- **Randomness in core economy loops**: both sell price and bid amounts incorporate Math.random(). This makes autonomous economy behavior non-reproducible and hard to test or debug. Consider seeded RNG per town or removal for v1 determinism.
- **Global static TownManager lookup inside the component**: every tick/decision walks TownManager.getAllInstances() to discover "our" ServerLevel so it can call ContractBoard.get(level). This makes the class impossible to unit test in isolation and introduces ordering/registration hazards. A cleaner injection of (level, board) or a TownContext would be a large improvement.
- **pendingBids survive only until reload**: the 1s delayed-bid map is not serialized. After a server restart any scheduled bids are lost (they will be re-evaluated on next scan if still relevant). Is this desired or a source of "missed" bids?
- **5× budget only for "wanted" items**: the isWanted path (from Town.getWantedResources, fed by TownResearchAI) gives a much larger bidding headroom. The 3× vs 5× distinction and the "desperate=1.0 needRatio" special case for wanted items should be confirmed against design intent.
- **No re-entrancy or duplicate contract guards across ticks**: a slow board or concurrent tick could in theory allow two excess checks to both see the same surplus before the first contract is registered. The activeCount check happens inside the critical section but the resourceCount snapshot is not atomic with the addContract.
- **getInTransitResourceCount comment says "simplest is ... !isDelivered"** — the implementation matches that, but the broader comment block describes more states (auction closed + courier assigned). The current filter is intentionally broad (any not-fully-delivered win).

## Related
- [[Trade/Contracts/Sell Contract Lifecycle]] (T-004)
- [[Trade/Contracts/Auction Resolution]] (T-003)
- [[Trade/Contracts/Bid Selection and Clamping]] (T-028)
- [[Town/Trading/Stock and Capacity Resolution]] (T-031 — provides the getStock / getStorageCap / getWantedResources used here)
- [[Economy/Resources/Resource Type Expansion and Lookup]] (T-019 — only registered resources generate contracts)
- [[Config/Configuration Loading]] (T-014 — the *StockPercent and contract* timing knobs)
- [[Town/Production/Recipe Execution and Dynamic Evaluation]] (T-029 — production can create the excess that this component liquidates)
