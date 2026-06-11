---
tags:
  - detail
  - trade
---
# Price Calculation

**Breadcrumb**: Trade > Global Market > Price Calculation
**TL;DR**: Server-wide singleton maintaining a floating-point price per resourceId (default 1.0); `recordTrade` nudges price 10% toward the transacted unit price while accumulating integer volume; `recordFailedAuction` drops price 5% (supply pressure); every read/write clamps to a hard floor of 0.0001.

## What it does
The Global Market provides a single source of truth for dynamic item prices across all towns and the auction system. When players or towns trade resources, the market learns: successful trades pull the price toward the actual transaction price; auctions that receive no bids apply mild downward pressure to signal oversupply. These prices feed the trading UI, contract valuations, and any future production/restock logic. Prices and cumulative volumes persist across restarts via the overworld's MarketSavedData.

## How it works (process view)
- Any caller obtains the shared instance via `GlobalMarket.get()`.
- `getPrice(resourceId)` returns the last known price for that id, or 1.0 if never seen, but never below the floor (0.0001).
- `recordTrade(resourceId, quantity, unitPrice)` is called after a completed player-to-town trade or a successful contract close. It adds the (truncated-to-long) quantity to a running volume total and blends the stored price: 90% old + 10% the just-traded unit price, then re-floors.
- `recordFailedAuction(resourceId)` is called by ContractBoard when a SellContract expires with zero bids: the price is multiplied by 0.95 and re-floored (a 5% drop per failed auction).
- `setPrice` is used only during legacy ContractSavedData migration load; it also floors.
- On world load, MarketSavedData calls `load()` which clears the maps first (cross-world hygiene) then re-populates, enforcing the floor on every restored price.
- `reset()` is called on new world creation and for test isolation; it clears both prices and volumes.
- Prices are stored under opaque string resourceIds (e.g. "wood", "iron", or a raw minecraft:foo for unregistered items). The market itself is agnostic to what the ids mean.

**Worked example (recordTrade)**: Resource "iron" starts unknown → getPrice("iron") = 1.0. A trade of 64 iron at a unit price of 2.5 occurs.
- volume: 0 + (long)64 = 64
- newPrice = (1.0 * 0.9) + (2.5 * 0.1) = 0.9 + 0.25 = 1.15
- stored price becomes 1.15 (above floor).
A second trade of 10 iron at 3.0:
- volume: 64 + 10 = 74
- newPrice = (1.15 * 0.9) + (3.0 * 0.1) = 1.035 + 0.3 = 1.335
- stored = 1.335

**Worked example (failed auction)**: "coal" is at 0.8. An auction for coal receives no bids.
- newPrice = max(0.8 * 0.95, 0.0001) = 0.76
- stored = 0.76

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `GlobalMarket.get()` | `common/src/main/java/com/quackers29/businesscraft/economy/GlobalMarket.java` | Returns the singleton instance (eagerly created). |
| `getPrice(String resourceId)` | same | Returns `max(prices.getOrDefault(id, 1.0f), MIN_PRICE)`. Never mutates state. |
| `setPrice(String, float)` | same | Stores `max(price, MIN_PRICE)` and marks dirty. Used by legacy migration. |
| `recordTrade(String, float quantity, float unitPrice)` | same | `volume += (long) quantity`; price ← `0.9*current + 0.1*unitPrice`; floor; dirty. |
| `recordFailedAuction(String)` | same | price ← `max(current * (1 - 0.05f), MIN_PRICE)`; dirty. Called on zero-bid auction expiry. |
| `reset()` | same | Clears both maps; logs; dirty. Called on new world and by MarketSavedData.create(). |
| `load(CompoundTag)` / `save(CompoundTag)` | same | Full state round-trip. Load clears first, then inserts with price floor enforcement (volume stored as-is). |
| `getPrices()` | same | Returns an unmodifiable snapshot of the internal prices map (only keys that have been written; defaults are not present). |
| `MarketSavedData` | `common/src/main/java/com/quackers29/businesscraft/economy/MarketSavedData.java` | Wraps GlobalMarket for SavedData persistence on the primary level; wires the dirty callback. |
| `ContractBoard.updateMarketPrice` / `getMarketPrice` / `getAllMarketPrices` | `common/src/main/java/com/quackers29/businesscraft/contract/ContractBoard.java` | Thin delegation to GlobalMarket for contracts; getAllMarketPrices also merges ProductionRegistry estimates. |

## Rules & formulas (exact)

**Constants** (GlobalMarket.java:17,20):
```java
private static final float MIN_PRICE = 0.0001f;
private static final float FAILED_AUCTION_DROP_RATE = 0.05f;
```

**getPrice** (line 47):
```java
float price = prices.getOrDefault(resourceId, 1.0f);
return Math.max(price, MIN_PRICE);
```
- Unknown id synthesizes 1.0 before the floor (so always ≥ 1.0 for unknowns).

**setPrice** (line 52):
```java
prices.put(resourceId, Math.max(price, MIN_PRICE));
```

**recordTrade** (lines 61-76):
```java
long currentVol = totalVolume.getOrDefault(resourceId, 0L);
totalVolume.put(resourceId, currentVol + (long) quantity);

float currentPrice = getPrice(resourceId);
float newPrice = (currentPrice * 0.9f) + (unitPrice * 0.1f);
newPrice = Math.max(newPrice, MIN_PRICE);
prices.put(resourceId, newPrice);
```
- Quantity is cast to long (fractional part truncated toward zero for volume).
- The blend always uses the *current* (floored) price as the starting point.
- 10% learning rate is hard-coded.

**recordFailedAuction** (lines 78-88):
```java
float currentPrice = getPrice(resourceId);
float newPrice = Math.max(currentPrice * (1 - FAILED_AUCTION_DROP_RATE), MIN_PRICE);
prices.put(resourceId, newPrice);
```
- Exactly 5% multiplicative drop per call; floor reapplied.

**load** (lines 90-111):
- `prices.clear(); totalVolume.clear();` first (prevents cross-world leakage).
- For each price key: `prices.put(key, Math.max(loadedPrice, MIN_PRICE))`.
- Volume loaded verbatim (no floor semantics on volume).

**save** (lines 113-121):
- Writes exactly the contents of the two maps into nested CompoundTags "prices" and "volume". Empty maps produce empty compound tags.

**getPrices** (line 57):
- Returns `Collections.unmodifiableMap(prices)` — a live unmodifiable view of the internal map. Callers must not assume defaults are present.

## Edge cases & behaviors
- Unknown resourceId → getPrice returns 1.0 (the synthesized default, above floor).
- Explicitly setting or trading a price < 0.0001 → clamped to 0.0001 on write and on subsequent reads.
- recordTrade with quantity < 1 (e.g. 0.5) → volume increases by 0 (long truncation); price still blends.
- recordTrade with quantity = 0 → volume unchanged; price still performs the 90/10 blend toward the supplied unitPrice.
- Multiple recordFailedAuction calls compound: 5% each time (0.80 → 0.76 → 0.722).
- After reset(), getPrice for any id returns the default 1.0 again.
- getPrices() after touching only "wood" contains exactly {"wood": <its price>}; "iron" (never touched) is absent even though getPrice("iron") == 1.0.
- load() of a tag containing a price of 0.00005 will store 0.0001 (floor enforced on load).
- The singleton is process-wide; the only isolation mechanism is explicit reset() or a fresh JVM. Tests must reset.
- Dirty callback is invoked on every mutating operation (set, record*, reset, load). In production this eventually reaches SavedData.setDirty().

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/economy/GlobalMarketTest.java`
- Covered: default price, set/get with and without floor, recordTrade volume accumulation (long cast) + 90/10 blend (first and subsequent), recordFailedAuction 5% drop + floor, reset clears both maps, load/save roundtrips (including floor-on-load), getPrices only contains written keys.
- Not covered (intentionally, per rules): ResourceRegistry / ResourceType expansion (requires PlatformAccess + registry bootstrap → NEEDS-MC), TradingViewModelBuilder / MarketViewModelBuilder price merging logic (they call into GlobalMarket but also touch registries and town state), full ContractBoard orchestration that calls record* as side effects (those paths are covered at a higher level or documented as MC-only in their own notes), ClientGlobalMarket (separate client cache, deprecated paths).
- No config values are read by GlobalMarket; no @BeforeEach/@AfterEach config save/restore needed.

## Open questions
- Volume is stored as long but incremented by `(long) quantity` where quantity is a float passed from trade packets / contract resolution. If a caller ever passes a huge float, truncation to long may lose magnitude; no saturation or logging occurs.
- The 90/10 blend and 5% drop rates are hard-coded magic numbers with no config exposure. Changing them would be a balancing decision.
- getPrices() returns a view that can be observed by UI code; because the underlying map mutates, callers who retain the reference see live updates. Current callers appear to snapshot or rebuild view models shortly after.
- recordTrade accepts a caller-supplied unitPrice rather than computing one internally. The caller (TradeResourcePacket, ContractBoard) is responsible for "what price was this trade actually at?" — the market just learns it. This keeps the market pure but means any bug in the caller's unitPrice calc pollutes the global curve.
- No public "initializePriceIfAbsent" or "seedFromBaseValue" — the first trade or explicit set wins. Production code seeds via the first player trade or via ContractSavedData migration which called setPrice for every stored market price.

## Related
- [[Trade/Trade Overview]]
- [[Trade/Contracts/Auction Resolution]] (T-003 — calls recordFailedAuction on zero-bid expiry; calls updateMarketPrice on successful close)
- [[Trade/Contracts/Sell Contract Lifecycle]] (T-004)
- [[Trade/Contracts/Courier Delivery Rewards]] (T-005)
- [[Economy/Economy Overview]] (mentions the global market in passing)
