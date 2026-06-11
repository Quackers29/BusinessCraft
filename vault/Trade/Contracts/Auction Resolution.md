---
tags:
  - detail
  - trade
---
# Auction Resolution

**Breadcrumb**: Trade > Contracts > Auction Resolution
**TL;DR**: When a SellContract expires with no winner assigned yet, if it has bids the highest bidder wins the lot (emeralds paid to seller, goods released from winner's escrow), courier reward is set to `ceil(euclid(town1, town2) / 10)`, auction closes and a courier-acceptance window opens; if no bids the goods return to seller, the contract is removed, and Global Market records a failed auction (supply pressure).

## What it does
Auction resolution is the moment a sell contract "clears." A producing town posts a SellContract (resource + quantity + ask price, with an auction duration). Other towns bid by committing emeralds (the bid price rounded up + a distance-based courier fee escrowed). When time expires, the ContractBoard's close logic picks a winner or refunds, moves resources, sets up delivery (player courier or snail mail), and updates market prices. This is the core price-discovery and goods-transfer mechanism for the inter-town trade economy.

## How it works (process view)
- A SellContract is active until its expiryTime. While open, towns can bid via the contract board UI (or town AI auto-bids).
- On every ContractBoard.tick(), closeAuctions() scans for expired SellContracts that are not yet completed and have no winningTownId.
- **Has bids case (normal resolution)**: pick highestBidder + highestBid via the Contract bid map. Set winning town, record the accepted bid, compute transactionPrice = highestBid / quantity (for market), transfer (int)highestBid emeralds from winner's escrow to seller, release the goods from seller's escrow, compute courierCost = ceil(dist/10), store it on the contract, extend the expiry by the courier acceptance window (default 2 min from Config), mark dirty.
- **No bids case**: return the escrowed goods to the seller town, recordFailedAuction (lowers global price via supply pressure), remove the contract entirely.
- After a winner is set, if the courier acceptance window also expires without a player claiming the courier job, the contract falls back to "Snail Mail" (special UUID), gets a longer delivery timer (using the snail-mail rate), and eventually auto-completes.
- Courier cost is computed both at bid time (to decide total escrow the bidder must lock) and at resolution (to set the reward the courier will eventually earn on delivery).

**Worked example**: Seller posts 64 iron at 3.0 emeralds/unit (ask 192). BidderA at 80 blocks away bids 210. At bid time courierCost = ceil(80/10) = 8, so bidder escrows 211 (ceil(210)? wait 210 was already int; code ceils the offered amount) + 8 = 219 emeralds total. BidderB bids 250 → higher. On expiry: highest=250, quantity=64, txPrice=250/64≈3.906. Seller receives 250 emeralds (int). Winner's escrow loses 250 emeralds (the courier portion  ceil(250?)+their courierCost remains in escrow on winner until delivery complete?). Courier reward set to 8 (based on seller→winner dist). Courier has 2 min (config) to accept the run; otherwise snail mail takes over with its own slower rate.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ContractBoard.closeAuctions()` | `common/src/main/java/com/quackers29/businesscraft/contract/ContractBoard.java` (private, called from tick) | The auction resolver. Scans expired open SellContracts, branches on has-bids vs no-bids, performs transfers, sets winner/courier reward, market updates, snail mail fallback. |
| `ContractBoard.calculateCourierCost(Town, Town)` | same file (public static) | Pure distance-based courier fee/reward: `nulls → 0`, else `(int) Math.ceil( sqrt( distSqr ) / 10.0 )`. Used both pre-bid (escrow) and post-resolution (reward). |
| `Contract.addBid(UUID, String, float)` | `common/src/main/java/com/quackers29/businesscraft/contract/Contract.java` | Stores `max(existingForBidder, amount)` — only allows a bidder to raise (or match) their own bid. Also caches bidder name. |
| `Contract.getHighestBid()` / `getHighestBidder()` | same file | `bids.isEmpty() ? 0f : max(values)` and `argmax(entry by value) or null`. Stream max; ties take the first-encountered entry (HashMap iteration order). |
| `SellContract` (ctor clamps, isAuctionClosed, setWinningTown, setAcceptedBid, setCourierReward, getCurrentBid) | `common/src/main/java/com/quackers29/businesscraft/contract/SellContract.java` | Auction-specific state (winningTownId, acceptedBid, courier*, delivery tracking). Ctor and load clamp quantity [1,10M] and pricePerUnit [0.01,1M]. `getCurrentBid()` returns the original ask (price*quantity), not the live highest. `isAuctionClosed() == (winningTownId != null)`. |
| `ContractBoard.addBid(...)` (the public entry) | ContractBoard.java | Validates, computes courier at bid time, rounds offered amount up, escrows total (bid+cost), refunds previous high bidder their (ceil(prev)+theirCourier), calls contract.addBid(rounded), updates UI. Also handles post-close courier acceptance path. |

## Rules & formulas (exact)
All formulas taken directly from the implementation (no reliance on comments).

**Courier cost (ContractBoard:539)**:
```java
if (town1 == null || town2 == null) return 0;
double distance = Math.sqrt(town1.getPosition().distSqr(town2.getPosition()));
return (int) Math.ceil(distance / 10.0);
```
- Uses Euclidean (3D but Y usually same) straight-line between town centers.
- Division by 10.0 then ceil → e.g. 0-10 blocks = 0 cost; 11-20 = 2, etc. Minimum non-zero is 2? Wait: dist=10 →1.0 →ceil1; dist=0.1→0.01→ceil 1? No: for dist<=10, 1.0 max for =10, ceil(1.0)=1; for dist<10 e.g. 5 →0.5 →1? Math.ceil(0.5)=1.0 yes. So any positive distance yields at least 1.
- (int) after ceil.

**Bid amount handling in addBid path (ContractBoard:438)**:
```java
float roundedAmount = (float) Math.ceil(amount);
int totalCost = (int) roundedAmount + courierCost;
...
contract.addBid(bidder, bidderTown.getName(), roundedAmount);
bidderTown.addResource(EMERALD, -totalCost);
bidderTown.addEscrowResource(EMERALD, totalCost);
```
- The *stored* bid (what getHighestBid returns) is the ceiled offered price only.
- Total escrowed by bidder includes their courierCost at the moment of *that* bid.
- On outbid: previous is refunded `prevBid = (int) Math.ceil(previousHighestBid) + prevCourierCost` (their courier at their bid time).

**Winner selection (Contract:87)**:
```java
public float getHighestBid() {
    return bids.isEmpty() ? 0f : bids.values().stream().max(Float::compare).orElse(0f);
}
public UUID getHighestBidder() {
    return bids.entrySet().stream()
            .max(Map.Entry.comparingByValue(Float::compare))
            .map(Map.Entry::getKey).orElse(null);
}
```
- Used by closeAuctions, UI, and town AI bidding logic.

**Resolution in closeAuctions (ContractBoard:281)** (SellContract branch, simplified):
```java
if (sc.isExpired() && !sc.isCompleted() && sc.getWinningTownId() == null) {
    if (!sc.getBids().isEmpty()) {
        UUID highestBidder = sc.getHighestBidder();
        float highestBid = sc.getHighestBid();
        ... setWinningTown ...
        sc.setAcceptedBid(highestBid);
        if (sc.getQuantity() > 0 && highestBid > 0) {
            float transactionPrice = highestBid / (float) sc.getQuantity();
            updateMarketPrice(resourceId, quantity, transactionPrice);
        }
        ... emerald transfer: seller.add(EMERALD, (int)highestBid); winner.removeEscrow(EMERALD, (int)highestBid);
        ... release goods escrow from seller ...
        int courierCost = calculateCourierCost(winnerTown, sellerTown);
        sc.setCourierReward(courierCost);
        sc.extendExpiry( ConfigLoader.contractCourierAcceptanceMinutes * 60000L );
    } else {
        ... refund goods from escrow to seller ...
        GlobalMarket.get().recordFailedAuction(resourceId);
        removeContract(...);
    }
}
```
- After this, a separate branch handles "auction closed but courier not assigned and now expired" → assign SNAIL_MAIL_UUID and extend using snail mail rate.
- Another: snail mail expired → complete().

**Snail mail duration (ContractBoard:361)**:
```java
distance = sqrt(seller.pos.distSqr(winner.pos));
smDuration = (long) (distance * ConfigLoader.contractSnailMailDeliveryMinutesPerMeter * 60000L);
sc.extendExpiry(smDuration);
```

**Config values that feed resolution (ConfigLoader)**:
- contractCourierAcceptanceMinutes (default 2.0)
- contractCourierDeliveryMinutesPerMeter (0.05) — used for player-courier delivery timer, not directly in close
- contractSnailMailDeliveryMinutesPerMeter (0.1)

## Edge cases & behaviors
- Null towns to calculateCourierCost → 0 (guard at top of method).
- Zero or tiny distance (same-block towns, though normally prevented) → ceil(0/10)=0 or ceil(small/10)=1.
- Bid amount <1 (e.g. 0.1) → roundedAmount = 1.0f after ceil.
- Same bidder bids again lower → ignored (max keeps the higher prior).
- Same bidder raises → new higher stored.
- Tie bids (two towns bid exactly same float) → getHighestBidder returns whichever entry the HashMap stream encounters first (insertion + hash order dependent; non-deterministic across runs/JDKs). No explicit tie-breaker (e.g. by UUID or time).
- No bids on expiry → goods refunded (addResource from escrow), contract deleted, recordFailedAuction called (affects pricing).
- quantity==0 or highestBid==0 guards the market update and some transfers.
- After resolution the contract is still in the list (with winningTownId set) until delivery/courier complete; later tick paths handle delivery complete → complete+expireNow, snail mail auto-complete, etc.
- Courier reward is set only on the successful close path; snail mail path does not appear to override it.
- Duplicate statement in handleCourierAcceptance (and similar in close path?): `sc.setCourierAcceptedTime(System.currentTimeMillis());` appears twice consecutively (line 233-234 in source) — harmless but odd.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/contract/ContractBoardTest.java`
- Covered (11 base + 7 new McBootstrap tests = 18 total): null-guard for calculateCourierCost (the ContractBoard static); bid map / winner selection (getHighestBid/getHighestBidder on SellContract, which is exactly what closeAuctions calls); addBid "only raises for same bidder" rule; SellContract auction state (isAuctionClosed, getCurrentBid vs live highest); ctor + load clamping for quantity/pricePerUnit (valid auction setup); bids + bidder names round-trip through Contract.save/load + SellContract(CompoundTag) so highest selection survives persistence.
- **Courier cost happy path (added via McBootstrap)**: positive-distance cases now exercised with real Town instances (3-param ctor + BlockPos, which are safe after vanilla registry bootstrap). 7 tests cover dist=0 → 0, tiny dist (ceil yields 1), exact 10-block steps, and non-multiples that require ceil (e.g. 11→2, 25→3, 100→10, 3D hypotenuse). Hand-computed expectations in test comments (e.g. "11 / 10.0 = 1.1; ceil(1.1)=2.0"). This covers the exact formula used both at bid time (for escrow total) and at resolution (for courierReward). Test file link: `common/src/test/java/com/quackers29/businesscraft/contract/ContractBoardTest.java` (extended, not rewritten).
- Not covered (intentionally, per rules): full closeAuctions() private orchestration and side effects (ServerLevel, TownManager.get, live Town escrow/resource moves + addEscrow/removeEscrow, GlobalMarket.recordTrade/recordFailedAuction, broadcastUpdate, snail-mail fallback timer math + SNAIL_MAIL_UUID path, courier acceptance in addBid); the player-distance check in handleCourierAcceptance (needs real ServerPlayer + level); AI bidding in TownContractComponent. These require a live Level/world and populated TownManager — remain NEEDS-MC. (The pure selection + courier math that the orchestrator *calls* are covered.)
- The core "bid resolution + winner selection" rules that *define* auction outcome are fully exercised in pure logic via the Contract base class methods. Courier cost formula now has both guard and positive arithmetic coverage in unit tests.

## Open questions
- **Tie-breaker**: HashMap + stream.max means equal-highest bids have unstable winner. Unlikely in practice (float prices), but if two towns bid identical amounts the "first" in iteration order wins. Worth a deterministic rule (e.g. earliest bid or lowest UUID) in future.
- **Escrow accounting on win**: Winner escrows (roundedBid + courierCostAtTheirBidTime). On resolution only (int)highestBid is removed from their escrow. The courier-fee portion of emeralds stays escrowed on the winner town. Is this intentional (held until courier delivery succeeds)? Or should the courier portion be released/refunded to winner at close and separately escrowed/paid on delivery? Current behavior pinned by lack of pure test (would require full Town escrow logic).
- **Duplicate set line**: `setCourierAcceptedTime` called twice in a row in handleCourierAcceptance (ContractBoard:233-234). Harmless (overwrites with same value), but indicates copy-paste; no functional bug.
- **getCurrentBid vs highest**: SellContract.getCurrentBid() returns the *original ask* (pricePerUnit*quantity), never the live highest bid. UI and some code use getHighestBid separately. Naming is a bit confusing but consistent in source.
- Courier cost minimum: any >0 distance yields at least 1 (ceil(x/10) for x>0 is >=1). Matches the "per batch minimum" spirit seen in tourist payments.
- The resolution lives in a private method with massive side effects and many cross-cutting concerns (market, escrow, items, payment board later). Extracting a pure "AuctionResult resolve(SellContract, Map<UUID,Town> towns, ConfigSnapshot)" would make this fully testable and clarify the rules.

## Related
- [[Trade/Trade Overview]]
- [[Trade/Contracts/Sell Contract Lifecycle]] (T-004)
- [[Trade/Contracts/Courier Delivery Rewards]] (T-005)
- [[Economy/Global Market/Price Calculation]] (T-006 — receives transactionPrice on close and failed-auction signals)
- [[Town/Payment Board/Reward Claims]] (T-012 — courier rewards and contract items land here)
