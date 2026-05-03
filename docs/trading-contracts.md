# BusinessCraft Trading Contracts CURRENT IMPLEMENTATION

## Introduction
Replaces direct trading with **timed contract auctions** for realism/delays. Integrates `trading-overview.md` (resources/units/market) & `town-production.md` (excess/deficits):

- **Sell Contracts**: Town posts excess → **town-only bids** during timer → highest wins. ✅ **PARTIALLY IMPLEMENTED**
- **Courier Contracts**: Post-sale delivery → **town/player bids** (players can compete for haul rewards). ❌ **NOT IMPLEMENTED**
- **Timers**: 5-10min bids. ❌ **NOT IMPLEMENTED**
- **Global Board**: Active contracts (NBT-persisted). ✅ **IMPLEMENTED**
- **Player UI**: Contracts Board block + screen with tabs. ✅ **IMPLEMENTED**
- **Full Implementation**: Towns auto-post/bid sells; players bid couriers + full visibility. ❌ **~50% COMPLETE**

**Goals**: Immersive delays, player jobs (couriers), configurable. ✅ **UI COMPLETE**

**IMPLEMENTATION STATUS**: ~50% Complete - Contract system and UI work, but no courier delivery, no timers, no fulfillment.

## Core Components

### 1. Contract Classes (Common)
```java
abstract class Contract {  // IMPLEMENTED
  UUID id = UUID.randomUUID();
  long startTime, endTime;  // bid closes - NOT USED FOR TIMERS
  String poster;  // town UUID or player - TOWN ONLY
  Map<UUID, Float> bids = new HashMap<>();  // bidder -> bid_price
  boolean fulfilled = false;
  float reward;  // emeralds post-delivery - NOT USED
}

class SellContract extends Contract {  // IMPLEMENTED
  String resource;  // "wood"
  float quantity;   // units - USING INT
  String sellerTown;  // UUID
  float minPrice;     // reserve - NOT USED
}

class CourierContract extends Contract {  // IMPLEMENTED BUT UNUSED
  SellContract linkedSell;
  BlockPos from, to;  // town centers
  String resource;
  float quantity;
  String sellerTown, buyerTown;
}
```
- **Board**: `ContractBoard` singleton: Map<UUID, Contract>, tick cleanup expired. ✅ **IMPLEMENTED**

### 2. Global Contract Board NBT
- Save: `world/businesscraft_contracts.dat` (list of CompoundNBT). ✅ **IMPLEMENTED**
- Limit: Config max_active (50 global, 3/town). ✅ **IMPLEMENTED**

### 3. Sell Contract Posting (Production Trigger)
**CURRENT IMPLEMENTATION (TownContractComponent):**
```java
// Called every 100 ticks in TownContractComponent
if (resourceCount > EXCESS_THRESHOLD) {
  // Check if already at max contracts (3 per town)
  if (activeCount >= MAX_ACTIVE_CONTRACTS) return;

  int sellQuantity = (resourceCount - EXCESS_THRESHOLD) / 2;
  float pricePerUnit = 1.0f;  // HARDCODED

  SellContract contract = new SellContract(
      town.getId(),
      town.getName(),
      60000L,  // 60 second hardcoded duration
      resourceId,
      sellQuantity,
      pricePerUnit);

  board.addContract(contract);
  // ESCROW: Immediately deduct resources from seller
  town.addResource(item, -sellQuantity);
}
```

### 4. Town Bidding (Auto, Needs-Based) **– Sell Contracts Only**
Town tick scans board **(towns only – players view via UI)**: ✅ **IMPLEMENTED**
```java
for (SellContract sc : board.activeSells()) {
  if (sc.sellerTown != this.id && needs(sc.resource)) {
    float bid = sc.getPricePerUnit() * sc.getQuantity() * 1.1f;  // HARDCODED
    // ESCROW: Check if town has enough emeralds
    if (emeraldCount >= bid) {
      board.addBid(sc.getId(), town.getId(), bid);
    }
  }
}
```
- **Auction Close**: NO TIMER-BASED AUCTION CLOSE ❌ **NOT IMPLEMENTED**
- **Winner Notification**: NO WINNER DETERMINATION ❌ **NOT IMPLEMENTED**

### 5. Courier Flow & Fulfillment
Board auto-posts on sell close: ❌ **NOT IMPLEMENTED**
```java
// THIS CODE DOES NOT EXIST
CourierContract cc = new CourierContract();
cc.linkedSell = sc;
// ... populate fields
board.post(cc);
```
- **Town/Player Bids**: Price for service (emeralds). ❌ **NOT IMPLEMENTED**
- **Courier Entity**: NO SLOW COURIER ENTITY ❌ **NOT IMPLEMENTED**
- **Fulfillment**: NO DELIVERY MECHANICS ❌ **NOT IMPLEMENTED**

### 6. Player Integration (**View Sells + Bid Couriers**)
**Contracts Board UI (Core Feature)** ✅ **FULLY IMPLEMENTED**

- **Access**:
  - Placeable "Contracts Board" block (sign-like, recipe: paper+emerald). ❌ **NOT IMPLEMENTED - NO BLOCK**
  - Command: `/contracts` (requires permission). ❌ **NOT IMPLEMENTED**
  - Packet: `OpenContractsBoardPacket`. ❌ **NOT IMPLEMENTED - USES TownInterfaceMenu**

- **UI Layout** (276x195px): ✅ **IMPLEMENTED AS ContractBoardScreen**
  ```
  [Tabs: Available/Active/History] [Search: wood] [Filter: Sell/Courier] [Sort: Time/Reward/Dist] - PARTIAL
  ---------------- List ----------------
  ID:abc | Sell Wood x50 (TownA) | Min:0.55e | **Town Bids: TownB=0.62 (lead)** | Ends:4m32s | [View]
  ID:def | Courier Iron to TownB | Reward:12e | Ends:9m | Dist:250b | [Bid] - COURIER CONTRACTS DON'T EXIST
  ... (paginated, real-time updates)
  ---------------- Footer -------------
  [Refresh] [Map View] [Close]
  ```
  - **Icons**: Resource item. ✅ **IMPLEMENTED**
  - **Real-time**: Live bid updates (server sync). ✅ **IMPLEMENTED**
  - **Search/Filter**: NO SEARCH/FILTER ❌ **NOT IMPLEMENTED**

- **Interactions**:
  - **Sells**: View-only (real-time town bids, history). ✅ **IMPLEMENTED**
  - **Couriers**: Bid (number input → `BidContractPacket`), Accept (if eligible), Fulfill. ❌ **BidContractPacket exists but no backend**
  - **History**: View past contracts (fulfilled/timed-out). ❌ **NO HISTORY TRACKING**

**Commands** (backup): ❌ **NOT IMPLEMENTED**
- `/contracts list [available/active/history] [filter]`
- `/bid <courier_id> <price>`
- `/deliver <contract_id>`

## Data Flow Diagram
**CURRENT IMPLEMENTATION:**
```
Town Prod Tick --> Excess Resource
  ↓ Manual check in TownContractComponent (every 100 ticks)
Town Creates Sell Contract (escrows resources) - NO TIMER
  ↓ Towns scan and bid (no timer expiry)
NO AUCTION CLOSE - contracts persist forever
  ↓ NO Courier Contract Creation
NO Player Bidding
  ↓ NO Courier Entity Delivery
NO Resource Transfer
```

## Configuration (`businesscraft-common.toml`)
**CURRENT CONFIG:**
```
[contracts] - DOES NOT EXIST
  enabled = true - HARDCODED TRUE
  sell_bid_ticks = 60000  // HARDCODED 60 seconds
  courier_bid_ticks = 12000  // NOT USED
  sell_fraction = 0.5  // HARDCODED
  sell_markup = 1.1  // NOT USED
  bid_aggressiveness = 1.2  // HARDCODED 1.1
  max_active_town = 3  // HARDCODED
  max_global = 50  // NOT ENFORCED
```

## Multi-Loader
- **Common**: Contracts, Board, town hooks. ✅ **IMPLEMENTED**
- **Forge**: TickEvent, EntityType for courier. ❌ **NO COURIER ENTITY**
- **Fabric**: ServerTickEvents, EntityRenderer. ❌ **NO COURIER ENTITY**
- **NBT**: Architectury. ✅ **IMPLEMENTED**

## Performance
- Board tick: O(active) small. ✅ **GOOD**
- Entity: 1/courier, despawn post. ❌ **NO ENTITIES**

## Example Timeline (~30min real)
**ACTUAL CURRENT BEHAVIOR:**
```
T=0: TownA wood excess → Sell 50 wood @1.0e (no timer)
T=Variable: TownB bids 1.1x if needs wood and has emeralds
T=FOREVER: Contract stays active, no auction close, no courier, no delivery
```

## Future Enhancements (Actually Missing Features)
- Timer-based auction close
- Courier contract creation and bidding
- Courier entity delivery system
- Contract fulfillment and resource transfer
- Player bidding on courier contracts
- Contract history and completion tracking
- Search/filter in UI
- Contract board block placement

## Risks/Mitigations
- **No Contract Resolution**: Contracts accumulate forever ✅ **MITIGATED - board cleans expired**
- **No Delivery**: No actual trading happens ❌ **MAJOR ISSUE**
- **UI Without Backend**: Bid button does nothing for couriers ❌ **INCOMPLETE FEATURE**

Auction system foundation exists, but delivery mechanics missing! 🚧
