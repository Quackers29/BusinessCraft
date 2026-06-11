---
tags:
  - detail
  - trade
---
# Sell Contract Lifecycle

**Breadcrumb**: Trade > Contracts > Sell Contract Lifecycle
**TL;DR**: SellContract models a timed auction sell listing (resource + clamped qty + ask price) posted by a town; it supports bidding (inherited), transitions through auction close (winner + accepted bid set), courier assignment (player UUID or SNAIL_MAIL), incremental delivery tracking (deliveredAmount), a separate isDelivered flag, and completion/expiry; input validation clamps qty [1,10M] and price [0.01,1M] both at creation and on every NBT load; all state fully roundtrips for persistence and client sync.

## What it does
Sell contracts are the mechanism by which towns liquidate surplus production into emeralds via inter-town auctions. A producing town escrows goods and posts a SellContract for a duration; bidding towns escrow emeralds (bid + courier fee); on expiry the highest bidder wins the goods (released from seller escrow) and pays the bid to the seller, a courier reward is computed, and a delivery phase begins. The contract tracks the entire post-auction lifecycle until the goods are delivered (or snail mail auto-completes) and the contract is marked complete. Clamping protects against invalid data from config, UI, or corrupted saves. The serialized form is used by ContractSavedData (world save) and ContractSyncPacket (client UI).

## How it works (process view)
- **Creation**: TownContractComponent (or manual) computes a price (market + modifiers + random), constructs `new SellContract(issuerId, name, durationMs, resourceId, qty, pricePerUnit)`. Qty and price are immediately clamped. Goods are moved from available to escrow on the issuer town. Contract is added to the board.
- **Open auction**: Bids arrive via ContractBoard.addBid (or AI). Base Contract.addBid only allows a bidder to raise (or match) their own prior bid using max(). getHighestBid/getHighestBidder reflect the live max.
- **Auction resolution (on expiry, no winningTownId yet)**: see [[Trade/Contracts/Auction Resolution]]. If bids exist: setWinningTown + setAcceptedBid, compute tx price for market, transfer emeralds, release goods escrow, setCourierReward(ceil(dist/10)), extendExpiry(courier acceptance window from Config). If no bids: refund goods, recordFailedAuction, remove contract.
- **Courier acceptance window**: While closed but !courierAssigned and not expired, players can "bid" (accept) the courier job. On accept: setCourierId(player), setCourierAcceptedTime (twice in current code), compute delivery duration = dist * courierDeliveryMinPerM * 60s, extendExpiry(that). A contract item appears as a pickup reward on the seller's Payment Board.
- **Snail-mail fallback**: If the acceptance window expires with no courier assigned: setCourierId(SNAIL_MAIL_UUID), set time, compute snail duration = dist * snailMinPerM * 60s, extend. Later when that expires: complete().
- **Delivery**: Courier (player or snail) calls processCourierDelivery(amount) repeatedly. SellContract.addDeliveredAmount accumulates; when deliveredAmount >= quantity, the path calls complete() + expireNow(). (For snail, the complete happens on its expiry instead.)
- **Final grant tick**: ContractBoard.tick sees isCompleted() && !isDelivered() → processContractDelivery which sets isDelivered(true) and grants any remaining quantity to the buyer town's resources (bypassing some escrow?).
- After complete + expire, the contract remains in the saved list until manually cleaned or on reload filters?

**Worked example**: TownA posts 128 "copper" at 1.25 emeralds/unit (ask total 160) with 3 min auction. TownB (120 blocks away) bids 175 (rounded). On close: winning=B, acceptedBid=175, courierReward=ceil(120/10)=12, expiry extended +2 min (accept). No player accepts in time → snail assigned, expiry extended by snail calc (~120*0.1*60s = 12 min). "Snail" delivers 128: addDeliveredAmount(128) → isDeliveryComplete true → complete() + expireNow(). Later tick: completed && !delivered → setDelivered(true) + add 128 copper to TownB.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `SellContract(UUID, String, long, String, long, float)` ctor | `common/src/main/java/com/quackers29/businesscraft/contract/SellContract.java` (lines 27-44) | Primary constructor. Calls super for base timing/ids, clamps quantity [1,10M] and pricePerUnit [0.01,1M], initializes buyer/winning/courier/delivery fields to null/0/false. |
| `SellContract(CompoundTag)` | same | Deserialization path; calls super(tag) then loadAdditional which re-clamps qty/price from tag. |
| `SellContract.getResourceId / getQuantity / getPricePerUnit / getCurrentBid` | same | Core offer data. getCurrentBid() returns original ask (`pricePerUnit * quantity`) — never the winning/accepted bid. |
| `SellContract.isAuctionClosed()` | same (96) | `winningTownId != null`. Set by resolution path. |
| `SellContract.setWinningTownId / setWinningTown(UUID, String) / getWinningTownId / getWinningTownName / getAcceptedBid / setAcceptedBid` | same | Post-resolution state. setWinningTownId clears the name cache; setWinningTown populates both. |
| `SellContract.isDelivered() / setDelivered / getDeliveredAmount / setDeliveredAmount / addDeliveredAmount / isDeliveryComplete()` | same (100-149) | Delivery progress. isDeliveryComplete = `deliveredAmount >= quantity`. Separate boolean isDelivered flag exists for final grant gating. |
| `SellContract.isCourierAssigned() / isSnailMail() / get/setCourierId / get/setCourierReward / get/setCourierAcceptedTime` | same | Courier phase state. SNAIL_MAIL_UUID is public static final all-zero UUID. isSnailMail = exact UUID match. |
| `SellContract.getAmount()` | same (152) | `(int) quantity` — safe because of 10M clamp. |
| `Contract(ctor, CompoundTag) / isExpired / isCompleted / complete / extendExpiry / expireNow / getCreation/ExpiryTime / getBids / addBid / getHighestBid / getHighestBidder / getBidderName / save / load` | `common/src/main/java/com/quackers29/businesscraft/contract/Contract.java` (lines 17-162) | Base auction + timing + persistence. Bids stored as Map<UUID,Float> with parallel bidderNames cache. addBid does `max(prior, amount)`. extendExpiry and expireNow manipulate expiryTime directly. isExpired uses live `System.currentTimeMillis() > expiryTime`. save/load handle base fields + bids list + delegate to abstract *Additional. |
| `SellContract.saveAdditional / loadAdditional` | SellContract (161-216) | Persists all sell-specific fields (resource, qty, price, buyer, winning, accepted, isDelivered, courier*, deliveredAmt). Load re-clamps qty/price and uses hasUUID/contains guards for optionals. |
| `Contract.getType()` (abstract) / Sell impl | both | Returns "sell" — used by ContractSavedData to choose the right subclass on load. |
| `Contract.getFullDateTimeDisplay()` | Contract (156) | SimpleDateFormat "MM/dd HH:mm" on the stored expiryTime (display only). |

## Rules & formulas (exact)
**Input validation / clamping** (SellContract:31,189):
```java
this.quantity = Math.max(1L, Math.min(10_000_000L, quantity));
this.pricePerUnit = Math.max(0.01f, Math.min(1_000_000f, pricePerUnit));
// identical logic reapplied in loadAdditional on loadedQty/loadedPrice
```
Clamps are inclusive. Applied at construction (when TownContractComponent or players create offers) *and* on every deserialization (defensive against bad persisted data or future changes). No exceptions thrown; silent sanitization.

**State predicates (pure getters, no side effects)**:
- `isAuctionClosed()` → `winningTownId != null`
- `isCourierAssigned()` → `courierId != null`
- `isSnailMail()` → `SNAIL_MAIL_UUID.equals(courierId)` (constant `00000000-0000-0000-0000-000000000000`)
- `isDeliveryComplete()` → `deliveredAmount >= quantity`
- `isDelivered()` → the separate boolean flag
- `isCompleted()` (base) → the completed flag
- `isExpired()` (base) → `System.currentTimeMillis() > expiryTime`

**Timing manipulation** (all use wall clock at call site):
- Ctor: `expiryTime = creationTime + duration` (duration usually `Config.contractAuctionDurationMinutes * 60000L`)
- `extendExpiry(long additionalMillis)`: `expiryTime = System.currentTimeMillis() + additionalMillis` (replaces, does not add to prior remaining)
- `expireNow()`: `expiryTime = System.currentTimeMillis() - 1`
- `complete()`: `isCompleted = true` (no effect on time)

**Bid rules** (base, used pre-close):
- `addBid(UUID bidder, String name, float amount)`: `bids.put(bidder, Math.max(bids.getOrDefault(bidder, 0f), amount))`; name cached only if non-null.
- `getHighestBid()`: `bids.isEmpty() ? 0f : max(values)`
- `getHighestBidder()`: stream max by value, first on tie (HashMap order)
- `getCurrentBid()` on Sell: *always* the original ask `pricePerUnit * quantity`, independent of live bids.

**Serialization round-trip invariants**:
- Base save writes id, issuerId, issuerName (if non-null), creation/expiry, isCompleted, then bids as ListTag of {bidder, amount, optional name}.
- Load clears bids/names, repopulates, defaults issuerName to "Unknown Town" if absent.
- Sell saveAdditional writes resource, qty, price, conditional buyer/winning/winningName/accepted/isDelivered + all courier* + deliveredAmount.
- LoadAdditional reads with hasUUID/contains guards, then *re-clamps* the qty and price values.
- The outer ContractSavedData adds a "type" = getType() ("sell") wrapper; deserialisers branch on it before calling the Sell(CompoundTag) ctor.
- All fields that have setters (including deliveredAmount which can be driven > qty) are written and read back.

**Other**:
- `getAmount()`: `(int) quantity` (post-clamp safe).
- `getBidderName(UUID)`: falls back to "Unknown Town".
- BuyerTownId: serializes (conditional hasUUID), has public setter, but is not populated by the current auction resolution path (winningTownId is the destination).

## Edge cases & behaviors
- Clamped inputs: qty=0 or negative or 10M+1 → 1 or 10M; price=0 or 0.005 or 2M → 0.01 or 1M. Same after load of bad tag.
- Fresh contract: all *Assigned/Closed/Delivered/Completed flags false; deliveredAmount=0; highestBid=0; isExpired false (for positive duration); getCurrentBid = ask total.
- Zero/empty bids: highest=0, highestBidder=null; resolution takes the no-bid branch.
- Tie highest bids: getHighestBidder returns whichever HashMap iteration yields first (non-deterministic; pinned in T-003 tests).
- Delivery cross: addDeliveredAmount(partial) keeps isDeliveryComplete false; one more add that makes >= true flips it (and prod code then completes).
- deliveredAmount set beyond qty: isDeliveryComplete stays true; prod paths don't prevent it.
- Negative delivered via direct set (setter allows, add only +=): isDeliveryComplete may be false if <0 effectively; not exercised in normal play.
- extendExpiry on already-expired contract: still sets a full future window from *call time*.
- isExpired boundary: exactly equal to expiryTime → false (uses >); one ms past → true.
- expireNow + isExpired: immediately true.
- complete() is idempotent.
- Roundtrip of snail: courierId = SNAIL_MAIL_UUID, isSnailMail true after load.
- Roundtrip with bids + names + winning + courier + delivered + times + completed: all preserved (tested via base + sell fields).
- getFullDateTimeDisplay: always formats the *stored* expiryTime at call time (can be in past or future); uses system default locale for SimpleDateFormat.
- Large ask totals: float price*long qty in getCurrentBid may lose ulp precision for values >> 1e7 (float mantissa); realistic small prices are fine.
- UUID zero for snail is a sentinel; normal player UUIDs won't collide.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/contract/SellContractTest.java`
- Covered: ctor clamping (qty/price bounds), load clamping (via hand-crafted minimal + full tags), state predicates before/after transitions (isAuctionClosed via setWinning*, isCourierAssigned/isSnailMail, isDeliveryComplete via addDelivered, isDelivered flag, isCompleted via complete), expiry control (isExpired after expireNow/extend, using small durations), delivery accumulation and threshold crossing, getCurrentBid (ask vs highest), getAmount, full save→load roundtrips exercising every sell + base field (including optional names, no-bid, with-bids, courier state, delivered > qty, snail), sequences simulating auction-close → courier-assign → partial delivery → complete.
- Intentionally not covered (pure-logic limit): any path requiring Town/Level/ContractBoard (closeAuctions, addBid side effects, tick/process*, handleCourierAcceptance, escrow moves, market updates, payment board rewards), viewmodel construction, TownContractComponent auto-posting + escrow, ContractSavedData list + type dispatch, network packets, real time-based expiry without helpers, config values (explicit durations passed).
- Overlap note: clamps, getHighest*, isAuctionClosed, getCurrentBid, basic bid roundtrip already exercised in ContractBoardTest (T-003). This test adds the delivery/courier/completion state machine coverage + exhaustive Sell-specific NBT fields.

## Open questions
- **Dual delivery flags**: `isDelivered` (boolean, set only in the tick's processContractDelivery after isCompleted) vs `isDeliveryComplete` (amount >= qty, flipped in the courier delivery callback). In the happy courier path the amount path completes the contract but the isDelivered flag is set later by the tick. The flag appears to gate a final resource grant. Is this separation intentional (e.g. "delivery acknowledged" vs "amount fulfilled"), or could they be unified? Current behavior is pinned by tests.
- **extendExpiry always from now**: `expiryTime = currentTimeMillis() + delta` (not `oldExpiry + delta`). This gives a fresh window from the moment the phase decision is made (good for acceptance/delivery timers) but means late calls still award full duration and "revive" an expired contract's timer. Matches all call sites; documented.
- **buyerTownId underused**: Full field + setter + NBT persistence, yet auction flow and resolution use winningTownId for the buyer/destination. Possibly planned for a "direct sell" (non-auction) contract type or symmetry with CourierContract. No prod code currently writes it for SellContracts. Preserved for compatibility.
- **getCurrentBid naming vs semantics**: Returns the *original list ask* even after bids and close. Callers that want the transaction value use getAcceptedBid or getHighestBid. The name is slightly misleading post-resolution.
- **Float precision on large lots**: `pricePerUnit * quantity` (float) for 10M qty @ 1M price/unit yields 1e13; float has ~7 significant decimal digits so not all integer values are exactly representable. getCurrentBid and getCurrentBid usage in UI may show/round differently than exact math for huge contracts. Unlikely in practice (10M stacks are extreme); roundtrip tests use realistic small numbers.
- **deliveredAmount no lower bound**: setter accepts any long (including negative); isDeliveryComplete would be false. addDeliveredAmount only does += (prod always positive). Test pins current permissive behavior.
- **No "type" inside Sell save**: the "sell"/"courier" discriminator lives only in the ContractSavedData wrapper. Direct construction via Sell(CompoundTag) (used by sync packet) works because the caller already knows the type.
- **getFullDateTimeDisplay uses expiryTime as-is**: even if the contract was extended or expired, it formats whatever is stored. Client clocks may differ from server creation time.

## Related
- [[Trade/Trade Overview]]
- [[Trade/Contracts/Auction Resolution]] (T-003 — resolution that populates winning/courierReward and triggers the post-auction extensions)
- [[Trade/Contracts/Courier Delivery Rewards]] (T-005 — parallel CourierContract + reward issuance on delivery complete)
- [[Economy/Global Market/Price Calculation]] (T-006 — receives transactionPrice = acceptedBid / qty on successful close)
- [[Town/Payment Board/Reward Claims]] (T-012 — courier pickup items and delivery rewards are issued here)
