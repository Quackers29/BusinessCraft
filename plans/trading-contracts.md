# BusinessCraft Trading Contracts Plan

## Introduction
Replaces direct trading with **timed contract auctions** for realism/delays. Integrates `trading-overview.md` (resources/units/market) & `town-production.md` (excess/deficits):

- **Sell Contracts**: Town posts excess → **town-only bids** during timer → highest wins. Players view real-time bids/history.
- **Courier Contracts**: Post-sale delivery → **town/player bids** (players can compete for haul rewards).
- **Timers**: 5-10min bids.
- **Global Board**: Active contracts (NBT-persisted). Player UI for search/browse/view (real-time/history).
- **Full Implementation**: Towns auto-post/bid sells; players bid couriers + full visibility.
- **Multi-Loader**: Common `contracts/` logic.
- **Drives Economy**: Production excess → contracts → deliveries.

**Goals**: Immersive delays, player jobs (couriers), configurable.

## Core Components

### 1. Contract Classes (Common)
```java
abstract class Contract {
  UUID id = UUID.randomUUID();
  long startTime, endTime;  // bid closes
  String poster;  // town UUID or player
  Map<UUID, Float> bids = new HashMap<>();  // bidder -> bid_price
  boolean fulfilled = false;
  float reward;  // emeralds post-delivery
}

class SellContract extends Contract {
  String resource;  // "wood"
  float quantity;   // units
  String sellerTown;  // UUID
  float minPrice;     // reserve
}

class CourierContract extends Contract {
  SellContract linkedSell;
  BlockPos from, to;  // town centers
  String resource;
  float quantity;
  String sellerTown, buyerTown;
}
```
- **Board**: `ContractBoard` singleton: Map<UUID, Contract>, tick cleanup expired.

### 2. Global Contract Board NBT
- Save: `world/businesscraft_contracts.dat` (list of CompoundNBT).
- Limit: Config max_active (50 global, 3/town).

### 3. Sell Contract Posting (Production Trigger)
In `Town#tick()` (excess detect):
```java
if (excess(resource) > threshold && cooldownOk()) {
  SellContract sc = new SellContract();
  sc.resource = resource;
  sc.quantity = excess * config.sell_fraction;  // 0.5
  sc.minPrice = GlobalMarket.prices.get(resource) * config.sell_markup;  // 1.1x
  sc.endTime = level.getGameTime() + config.sell_bid_ticks;
  sc.sellerTown = this.id;
  ContractBoard.post(sc);
  adjustStock(resource, -sc.quantity);  // reserve stock
}
```

### 4. Town Bidding (Auto, Needs-Based) **– Sell Contracts Only**
Town tick scans board **(towns only – players view via UI)**:
```java
for (SellContract sc : board.activeSells()) {
  if (sc.sellerTown != this.id && needs(sc.resource)) {
    float bid = GlobalMarket.prices.get(sc.resource) * (1.0f + needFactor * config.aggressiveness);
    if (bid >= sc.minPrice) {
      board.addBid(sc.id, this.id, bid);
    }
  }
}
```
- **Auction Close** (board tick): Highest **town** bid wins → notify, post CourierContract.
  - Seller += bid emeralds equiv (abstract).
  - Buyer pays reserve.

### 5. Courier Flow & Fulfillment
Board auto-posts on sell close:
```java
CourierContract cc = new CourierContract();
cc.linkedSell = sc;
cc.from = seller.center();
cc.to = buyer.center();
cc.endTime = now + config.courier_bid_ticks;
board.post(cc);
```
- **Town/Player Bids**: Price for service (emeralds).
- **Close/No Bids**: 
  - Spawn `CourierEntity` (minecart-like): Path from→to, slow speed.
  - On arrive: Transfer units (buyer += qty, seller confirmed).
  - Learn market: price = avg(sell bid).
  - Visual: Particles, sounds.

### 6. Player Integration (**View Sells + Bid Couriers**)
**Contracts Board UI (Core Feature)**

- **Access**: 
  - Placeable "Contracts Board" block (sign-like, recipe: paper+emerald). Right-click opens `ContractsBoardScreen`.
  - Command: `/contracts` (requires permission).
  - Packet: `OpenContractsBoardPacket`.

- **UI Layout** (276x195px):
  ```
  [Tabs: Available/Active/History] [Search: wood] [Filter: Sell/Courier] [Sort: Time/Reward/Dist]
  ---------------- List ----------------
  ID:abc | Sell Wood x50 (TownA) | Min:0.55e | **Town Bids: TownB=0.62 (lead)** | Ends:4m32s | [View]
  ID:def | Courier Iron to TownB | Reward:12e | Ends:9m | Dist:250b | [Bid]
  ... (paginated, real-time updates)
  ---------------- Footer -------------
  [Refresh] [Map View] [Close]
  ```
  - **Icons**: Resource item.
  - **Map Preview**: Town positions.
  - **Search/Filter**: Fuzzy/client-side.
  - **Details Modal**: Full info, bid history log.
  - **Real-time**: Live bid updates (server sync), history tab (past completed).

- **Interactions**:
  - **Sells**: View-only (real-time town bids, history).
  - **Couriers**: Bid (number input → `BidContractPacket`), Accept (if eligible), Fulfill (`/fulfill <id>` or chest → rewards).
  - History: View past contracts (fulfilled/timed-out).

**Commands** (backup):
- `/contracts list [available/active/history] [filter]`
- `/bid <courier_id> <price>`
- `/deliver <contract_id>`

## Data Flow Diagram
```
Town Prod Tick --> Excess Resource
  ↓ Post Sell Contract (bid 5min timer)
Town Scans/Bids (auto-need)
  ↓ Highest Bid Wins (timer end)
Post Courier Contract (10min bid)
  ↓ Player Bid? Or Timeout
Slow Courier Entity Paths + Transfer Units
  ↓ Stocks Update + Market Learn + Rewards
Complete: Particles/Logs
```

## Configuration (`businesscraft-common.toml`)
```
[contracts]
  enabled = true
  sell_bid_ticks = 6000  // 5min
  courier_bid_ticks = 12000  // 10min
  sell_fraction = 0.5
  sell_markup = 1.1
  bid_aggressiveness = 1.2
  max_active_town = 3
  max_global = 50
  slow_courier_speed_blocks_per_sec = 0.5
  courier_fee = 0.05  // skim
  # NEW: UI
  ui_max_display = 50
  ui_refresh_ticks = 100  // 5s
  ui_search_fuzzy_threshold = 0.7
```

## Multi-Loader
- **Common**: Contracts, Board, town hooks.
- **Forge**: TickEvent, EntityType for courier.
- **Fabric**: ServerTickEvents, EntityRenderer.
- **NBT**: Architectury.

## Performance
- Board tick: O(active) small.
- Entity: 1/courier, despawn post.

## Example Timeline (~30min real)
```
T=0: TownA wood excess → Sell 50 wood @min0.55 (5min bid)
T=5min: TownB bids 0.62 → wins
T=5min+: Courier bid 10min, no player
T=15min+: Slow cart A→B (5min travel), transfer, wood price→0.59
```

## Future Enhancements
- Multi-resource contracts.
- Auto-spawn contract buildings near town halls.
- Player companies (group bids on couriers).

## Risks/Mitigations
- No Bids: Auto courier.
- Spam: Limits.
- Desync: NBT sync.

Auction-based economy! 🚀
