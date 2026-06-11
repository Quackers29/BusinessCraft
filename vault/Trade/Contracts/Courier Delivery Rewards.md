---
tags:
  - detail
  - trade
---
# Courier Delivery Rewards

**Breadcrumb**: Trade > Contracts > Courier Delivery Rewards
**TL;DR**: CourierContract carries a fixed `reward` (float, provided at creation) for a direct transport job; `processCourierDelivery` accumulates `deliveredAmount` on the contract, grants the resource to the recorded destination town on every increment, and when `deliveredAmount >= quantity` it calls `complete()` and (if courierId set and reward > 0) posts `(int) reward` emeralds as a `COURIER_DELIVERY` reward on the destination town's Payment Board for the courier. Delivery processing itself is gated by a destination-town match check in the caller (TownInterfaceEntity) — "wrong town" rejection is not inside the process method.

## What it does
Courier contracts provide a direct (non-auction) way for a town to pay a fixed emerald bounty to any player willing to transport a specific quantity of a resource from the issuer ("source") town to a named destination town. The reward is fixed when the contract is posted. The courier accepts near the source (distance-checked), receives a contract item as a pickup reward on the issuer's Payment Board, physically carries the item, and "completes" the job by using the item inside the correct destination town's Town Interface. On success the destination town gets the goods and the courier gets the pre-agreed emeralds (claimable on that town's Payment Board). This is the payout half of the courier economy for direct jobs (distinct from the courier phase of SellContract auctions).

## How it works (process view)
- A CourierContract is instantiated (long ctor or, in current code, only via NBT deserialization in ContractSavedData / ContractSyncPacket). It records issuer, source pos + radius (for acceptance distance checks), resource + quantity, destination town (UUID + name), and the `reward` float.
- Acceptance (player "bids" 0 on it via UI or packet): ContractBoard.addBid path for CourierContract does a proximity check (player must be within source town's radius + 10 of the recorded sourceTownPos). On accept: sets courierId and acceptedTime, computes a delivery timer using `distance * contractCourierDeliveryMinutesPerMeter * 60000`, extends expiry, and posts a contract item as a COURIER_PICKUP reward on the issuer/seller town's Payment Board.
- Courier carries the contract item to the destination. To complete: inside the destination town's Town Interface, the player uses the contract item. The block entity reads `destinationTownId` from the item tag; only if the current town's id matches does it call `ContractBoard.get(level).processCourierDelivery(contractId, 1L)` and shrink the item. Mismatch → no-op (the "Wrong Town" surface for delivery completion; the label "Wrong Town" also appears in the client ContractDetailScreen for the accept-courier button when the player is not near the source).
- In `processCourierDelivery` (CourierContract branch):
  - `cc.addDeliveredAmount(amount)` (here always 1 per use).
  - If destination town exists and resourceId, look up the base Item via ContractItemHelper; if not PAPER, `destination.addResource(item, amount)`.
  - If `cc.isDelivered()` (i.e. deliveredAmount >= quantity after the add): `cc.complete()`, mark dirty, then if courierId != null: create `new ItemStack(EMERALD, (int) cc.getReward())`, wrap in a list, call `destTown.getPaymentBoard().addReward(COURIER_DELIVERY, rewards, courierId.toString())`, and if created, attach contractId metadata to the entry. Logs at INFO level on success.
- No snail-mail path for CourierContract (snail is a SellContract concept using the all-zero SNAIL_MAIL_UUID sentinel).
- The reward value is never computed inside CourierContract or this process method — it is whatever float was supplied to the constructor (or deserialized). Compare to SellContract where `setCourierReward(calculateCourierCost(...))` happens at auction resolution.

**Worked example**: Issuer TownA posts a CourierContract for 64 "copper", destination = TownB (120 blocks away), reward = 25.0f. A player near A accepts the courier job → receives a contract item via COURIER_PICKUP reward on A's board; contract timer extended for delivery. Player travels to B. At B's Town Interface the player uses the item: townId == destTownId check passes → processCourierDelivery(id, 1) called repeatedly as the player uses the stack. After 30 uses: deliveredAmount=30, 30 copper added to B (no payout yet). After 34 more (total 64): deliveredAmount=64 >=64 → complete(), 25-emerald stack is created and posted as COURIER_DELIVERY reward on B's Payment Board, claimable only by the courier's UUID. B now holds the copper; courier can claim the 25 emeralds.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `CourierContract(UUID issuer, String issuerName, BlockPos sourcePos, int sourceRadius, long duration, String resourceId, long quantity, UUID destId, String destName, float reward)` | `common/src/main/java/com/quackers29/businesscraft/contract/CourierContract.java` (ctor lines 18-33) | Primary construction. Stores all job parameters including the fixed emerald `reward`. No clamping (unlike SellContract). Initializes delivered=0, courierId=null, acceptedTime=0. |
| `CourierContract(CompoundTag)` | same (35) | Deserialization only path used in current code (ContractSavedData.load, ContractSyncPacket). Delegates to super then loadAdditional. |
| `CourierContract.getResourceId / getQuantity / getDeliveredAmount / setDeliveredAmount / addDeliveredAmount / isDelivered / getAmount` | same (39-61,103-105) | Delivery accumulator. `isDelivered()` = `deliveredAmount >= quantity`. `getAmount()` = `(int) quantity` (potential truncation for huge values). `addDeliveredAmount` is `+=` only (no lower bound guard). |
| `CourierContract.getReward / get/setCourierId / isAccepted / get/setAcceptedTime / get/setDestination* / getSource*` | same (63-101) | Reward passthrough + courier assignment + destination/source metadata (used by UI, acceptance distance checks, and delivery target validation in caller). `isAccepted()` = `courierId != null`. |
| `CourierContract.saveAdditional / loadAdditional` | same (108-152) | Full round-trip of courier-specific fields (resource, quantity, deliveredAmount, dest UUID+name, reward, courierId, acceptedTime, source pos+radius). Optionals guarded by contains/hasUUID. No re-clamping on load. |
| `CourierContract.getType()` | same (155) | Returns "courier" — the discriminator used by ContractSavedData to choose the right ctor on load, and written in save. |
| `ContractBoard.processCourierDelivery(UUID contractId, long amount)` | `common/src/main/java/com/quackers29/businesscraft/contract/ContractBoard.java` (101-205) | The delivery processor. Two branches: SellContract (uses isDeliveryComplete + later isDelivered flag + processContractDelivery in tick) vs CourierContract (amount-driven isDelivered, immediate complete + reward payout on this call). For Courier: always grants resource to *cc.getDestinationTownId()*; payout only on the isDelivered transition inside this method; reward = `(int) cc.getReward()`. |
| `TownInterfaceEntity` (use of contract item) | `common/src/main/java/com/quackers29/businesscraft/block/entity/TownInterfaceEntity.java` (~1141-1152) | The "wrong-town rejection" gate for *delivery completion*: reads destTownId from the contract item's tag; only calls processCourierDelivery if the presenting town's id equals that dest id. Silent no-op on mismatch. |
| `ContractBoard.addBid` (CourierContract branch) + ContractDetailScreen | ContractBoard (~460), ContractDetailScreen (~100-217,330) | Acceptance gating: client disables button with "Wrong Town" label if player not near source; server re-checks distSqr <= (radius+10)^2 before setting courierId. Separate from the delivery-town check. |

## Rules & formulas (exact)
All taken directly from code (no reliance on comments/docs).

**Delivery threshold (CourierContract:59)**:
```java
public boolean isDelivered() {
    return deliveredAmount >= quantity;
}
```
- Long comparison; >= so exactly equal or over both true.
- `addDeliveredAmount(long amount)` does `this.deliveredAmount += amount;` (can go negative if negative amount passed; prod always passes positive 1).
- No clamp or upper bound on deliveredAmount.

**Payout amount (ContractBoard:185)**:
```java
int rewardAmount = (int) cc.getReward();
if (rewardAmount > 0) {
    ... new ItemStack(Items.EMERALD, rewardAmount) ...
    ... destTown.getPaymentBoard().addReward(RewardSource.COURIER_DELIVERY, rewards, cc.getCourierId().toString()) ...
}
```
- Simple narrowing cast from float reward to int (truncates toward zero). Negative reward → 0, no payout.
- Payout only happens inside the `if (cc.isDelivered())` block *after* the addDeliveredAmount and the resource grant for that increment.
- The emerald ItemStack is created fresh — no debit from any town balance or escrow in this path.
- Reward metadata (`contractId`) is attached post-creation if the addReward succeeded.

**Resource grant on every delivery tick (ContractBoard:167-172)**:
```java
if (destination != null && cc.getResourceId() != null) {
    Item resourceItem = ContractItemHelper.getBaseItemForResource(cc.getResourceId());
    if (resourceItem != Items.PAPER) {
        destination.addResource(resourceItem, amount);
    }
}
```
- Happens *before* the isDelivered check, on every processCourierDelivery call.
- Skips PAPER (sentinel/placeholder resource id).
- Destination is always looked up by the contract's recorded `getDestinationTownId()` — the caller is trusted to have already filtered.

**No clamps on CourierContract inputs** (ctor + loadAdditional):
- quantity, reward, etc. are stored exactly as provided (or loaded). Contrast SellContract which does `Math.max(1, Math.min(10_000_000, ...))` both in ctor and loadAdditional.
- `getAmount()` just casts: `return (int) quantity;` — for quantity > Integer.MAX or <0 the result is implementation-defined truncation.

**Acceptance vs delivery are distinct checks**:
- Accept (job claim): distance to *source/issuer* using sourceTownPos + radius.
- Delivery complete (item use): exact UUID match of *presenting town* to contract's destinationTownId (in TownInterfaceEntity, before even calling process).

**Type discriminator**:
- On save: `contractTag.putString("type", c.getType())` ("courier").
- On load in ContractSavedData: if ("courier".equals(type)) new CourierContract(contractTag).

**Timing**: CourierContract uses the base Contract `extendExpiry` (sets `expiryTime = now + delta`) and `isExpired` (strict >). Same "from call time" semantics as Sell.

## Edge cases & behaviors
- `quantity = 0` or negative in ctor/load: `isDelivered()` becomes true as soon as any positive add happens (or immediately if delivered starts >=0). getAmount() may be 0 or negative cast.
- `deliveredAmount` set > quantity: isDelivered stays true; further adds keep it true. Prod path uses this (over-deliver in one call is allowed).
- `reward = 0.9f` or `0.0f`: `(int)reward = 0` → no payout created (the if (rewardAmount > 0) guard).
- `reward = -5.0f`: cast to -5, guard prevents payout.
- `courierId == null` at delivery complete: the `if (destTown != null && cc.getCourierId() != null)` guard skips the entire reward creation block (contract still completes).
- Null destination town at delivery time: resource grant skipped; if also no courier or reward<=0 the payout block is skipped. Contract still completes.
- PAPER resourceId: resource grant skipped (no crash), delivery tracking and payout still occur.
- Wrong destination town presenting the item: TownInterfaceEntity check `townId.equals(destTownId)` fails → processCourierDelivery is never called for that contract from that interface. Silent for the player (item not shrunk).
- Same contract delivered from multiple towns? Only the recorded dest can trigger the call; others cannot.
- Float reward precision: large rewards lose ulp accuracy on cast to int (same float issues noted in SellContract notes). Realistic small rewards are exact.
- NBT roundtrip of source pos with Y != 64 or non-zero: fully preserved (BlockPos x/y/z written as ints).
- Optional destinationTownName: written only if non-null; on load, absent → field left as whatever ctor default was (in practice the NBT ctor path leaves it from the tag or prior init).
- CourierContract never sets or checks the Sell-specific `isDelivered` boolean flag or SNAIL_MAIL_UUID.
- Duplicate set in some paths not applicable here (the double setCourierAcceptedTime is in Sell/courier-accept path).
- Contract remains in the saved list after complete() (same as Sell); no auto-removal in the delivery path.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/contract/CourierContractTest.java`
- Covered: primary ctor field wiring, isDelivered / addDeliveredAmount / setDeliveredAmount threshold behavior (false at zero, flips on >=, stays true on over), isAccepted via courierId, getAmount int cast, getReward passthrough, full save/load NBT roundtrips (all courier fields, source pos, dest info with/without name, reward float, delivered over, courier assignment + acceptedTime), getType, no-clamping behavior (contrast Sell), load of partial/legacy tags.
- Intentionally not covered (pure-logic limit per protocol): the long-ctor call sites (none exist in current common source — only deserializers), any Town/Level/ContractBoard/TownInterfaceEntity orchestration, PaymentBoard.addReward side effects, ItemStack creation, resource add, the destination-town match guard (needs block entity + item tag context), acceptance distance checks (needs Town + player position), timer extension math (config + real dist), UI "Wrong Town" button state, network packets, ContractSavedData list + type dispatch roundtrip (covered indirectly via ContractBoardTest/Sell patterns), real expiry with wall clock.
- Note: processCourierDelivery reward payout + resource routing + wrong-town caller guard are documented exactly in this note but exercised only via MC-dependent paths (similar to closeAuctions / handleCourierAcceptance in T-003). They are therefore classified as NEEDS-MC for unit testing.

## Open questions
- **Reward is input, not computed here**: Unlike SellContract's courierReward (populated by calculateCourierCost at auction close), a CourierContract's reward is whatever the (currently unreachable in prod) long ctor receives or what was serialized. Is there (or should there be) a distance-based default when posting courier jobs? Or is the reward always a manually/externally set fixed bounty? The absence of any creation call site using the rich ctor inside common suggests the "post a courier contract" user flow may be incomplete or handled outside (e.g. commands, other mods, or future UI).
- **No clamping on CourierContract**: quantity can be 0/negative/huge, reward any float. This is either intentional (flexible direct contracts) or an oversight vs the defensive clamps on SellContract. Current behavior pinned by tests; if clamps are later added they must be added to both ctor and loadAdditional to match Sell pattern.
- **Payout creates emeralds from nothing on success**: The visible code in processCourierDelivery grants the emerald stack without a corresponding debit from issuer escrow or global pool. (Possibly the economics are closed at job *posting* time, or it's intended as a pure bounty.) Documented as-is; no test can assert the "where does the money come from" without the full creation + escrow paths.
- **Wrong-town checks are split**: Delivery completion wrong-town is a hard gate in TownInterfaceEntity (must match dest exactly, else silent). Acceptance wrong-town is a soft UI + soft distance check in ContractBoard/ContractDetailScreen. Inconsistent UX surface; worth unifying or clarifying in docs/UI text.
- **CourierContract never uses the separate isDelivered boolean** that SellContract has (the one set by processContractDelivery in the tick after complete). For courier contracts, complete() + payout happens synchronously inside processCourierDelivery on the amount threshold. Intentional asymmetry or legacy divergence?
- **Long deliveredAmount**: addDeliveredAmount accepts long; isDelivered is long >=. But getAmount and the ItemStack count in payout use int casts. For contracts >> 2^31 this would misbehave on both grant and payout. Unrealistic but worth noting for completeness.
- `calculateCourierCost` is used for Sell courier rewards and bid escrow but *not* for CourierContract reward (which is independent). If a future "create courier contract" helper wanted a default, it would call the same static, but the happy path remains untestable in pure JUnit (Town construction EIIE per T-003).

## Related
- [[Trade/Trade Overview]]
- [[Trade/Contracts/Sell Contract Lifecycle]] (T-004 — the other contract type that also has a courier/delivery phase and uses calculateCourierCost for its reward)
- [[Trade/Contracts/Auction Resolution]] (T-003 — where Sell courier rewards are set via the shared cost formula)
- [[Town/Payment Board/Reward Claims]] (T-012 — both COURIER_PICKUP (accept) and COURIER_DELIVERY (payout) rewards land here)
- [[Economy/Tourist Payments/Distance Payment Calculation]] (T-001 — another source of Payment Board emerald rewards for contrast)
