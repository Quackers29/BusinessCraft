---
tags:
  - detail
  - trade
---
# Contract List and Detail ViewModels

**Breadcrumb**: Trade > Contracts > Contract List and Detail ViewModels
**TL;DR**: Server builds paginated, tab-filtered lists and full details for contract UIs by transforming live SellContracts (and generic fallbacks) into display-ready DTOs with pre-formatted prices, timers, status strings, bid lists, and action eligibility flags; only SellContracts participate in the auction/active/history tabs.

## What it does
When a player opens the contract board UI, the client sends a request for a specific tab (auction, active, or history) and page. The server responds with a ready-to-render list of summaries plus (on detail request) a rich detail viewmodel. These builders perform all the filtering, sorting, paging, status derivation, and string formatting on the server so the client just displays the data. Non-sell contract types fall back to minimal generic entries. This keeps the UI reactive and consistent while the underlying contracts change state on ticks and deliveries.

## How it works (process view)
- The ContractBoard holds the live list of Contract objects (mostly SellContracts).
- On list request: parse the tab string to one of AUCTION / ACTIVE / HISTORY, apply effective page size (history has special 50 or "all" caps from caller), call the summary builder.
- Builder keeps only SellContracts that match the tab's state rules, sorts them (urgency for open tabs, recency for history), slices the page, then for each builds a summary VM with formatted highest bid or "No bids", price per unit, status label, time remaining (via BCTimeUtils), and boolean flags for whether "Bid" or "Accept Courier" buttons should be enabled.
- On detail request for a contract ID: builder picks the contract and builds either a rich sell-specific detail (with sorted bid list, delivery progress, courier name, tooltip, courier reward, accepted bid) or a minimal generic one.
- All monetary values use a "#,##0.##" decimal format + " emeralds" / " emeralds/unit" / " ◎" suffixes. Time values and expiry checks are computed against the serverTime snapshot passed from the packet handler.
- **Worked example**: Board has 3 SellContracts. Auction tab (page 0, size 20): the two !auctionClosed contracts are kept, sorted by soonest expiry first. For the 200 copper at 1.5/unit with a 175 emerald highest bid: summary shows resource "copper", qty 200, "1.5 emeralds/unit", "175 emeralds", status "Auction", timeRemaining "2m 15s", canBid=true (still open and not expired at snapshot), canAcceptCourier=false. Clicking one sends detail request; detail includes full bid list sorted highest-first, "Auction Open" status, tooltip with total value calc, deliveryProgress null, courierRewardDisplay "12 ◎", acceptedBidDisplay null.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ContractSummaryViewModelBuilder.build(List<Contract>, Tab, int page, int pageSize, ServerPlayer, long serverTime)` | `common/src/main/java/com/quackers29/businesscraft/contract/viewmodel/ContractSummaryViewModelBuilder.java` | Entry point for paginated tabbed lists; returns ContractListResult |
| `ContractSummaryViewModelBuilder.Tab` | same | AUCTION / ACTIVE / HISTORY enum |
| `ContractSummaryViewModelBuilder.ContractListResult` (record) | same | DTO wrapper: contracts list + page + pageSize + totalCount + hasMore |
| `ContractSummaryViewModelBuilder.filterByTab(...)` (private) | same | Keeps only SellContract instances whose state matches the tab |
| `ContractSummaryViewModelBuilder.sortContracts(...)` (private) | same | In-place sort by expiry asc (auction/active) or creation desc (history) |
| `ContractSummaryViewModelBuilder.buildSellContractSummary(...)` / `buildSummary(...)` | same | Per-contract mapping to summary VM + status/flag/price formatting |
| `ContractDetailViewModelBuilder.build(Contract, ServerPlayer, long serverTime)` | `common/src/main/java/com/quackers29/businesscraft/contract/viewmodel/ContractDetailViewModelBuilder.java` | Entry for single-contract rich detail; null for null input |
| `ContractDetailViewModelBuilder.buildSellContractDetail(...)` / `buildGenericDetail(...)` | same | Sell path populates 20+ fields including sorted bids; generic is minimal |
| `ContractDetailViewModelBuilder.buildBidList(...)` (private) | same | Copies bids, sorts descending by amount, marks isHighest with 0.001 tolerance |
| `calculateStatus(SellContract)` (private, duplicated) | both files | Returns "Delivered" / "In Transit (x/y)" / "Courier Assigned" / "Snail Mail" / "Awaiting Courier" / "Auction" (summary) or "Auction Open" (detail) |
| `calculateCanBid(SellContract, long)` / `calculateCanAcceptCourier(SellContract)` (private, duplicated) | both files | Predicate rules for button enablement |
| `ContractSummaryViewModel` / `ContractDetailViewModel` (+ BidDisplayInfo record) | same package | The pure DTOs with FriendlyByteBuf (de)serialization; builders produce them |

## Rules & formulas (exact)
- **Tab filtering (only SellContract; others dropped for these tabs)**:
  - AUCTION: `!sc.isAuctionClosed()`
  - ACTIVE: `sc.isAuctionClosed() && !sc.isDelivered()`
  - HISTORY: `sc.isDelivered()`
- **Sort rules**:
  - AUCTION or ACTIVE: `sort(Comparator.comparingLong(Contract::getExpiryTime))` — soonest-expiring first.
  - HISTORY: `sort(Comparator.comparingLong(Contract::getCreationTime).reversed())` — most recently created first.
- **Paging (applied after filter+sort)**:
  - `if (pageSize <= 0) pageSize = 20; if (page < 0) page = 0;`
  - `startIndex = page * pageSize; endIndex = min(startIndex + pageSize, totalCount); hasMore = endIndex < totalCount;`
  - page slice = (start < total) ? subList(start, end) : empty list.
- **Price / bid formatting**: `new DecimalFormat("#,##0.##")`; summary highest: `>0 ? fmt + " emeralds" : "No bids"`; price: `fmt + " emeralds/unit"`.
- **Status derivation** (see code for exact if/else ladder; note "Auction" vs "Auction Open" difference between builders):
  - Delivered → "Delivered"
  - Auction closed + courier assigned + snail → "Snail Mail"
  - Auction closed + courier + partial delivered >0 → "In Transit (delivered/total)"
  - Auction closed + courier + 0 delivered → "Courier Assigned"
  - Auction closed + no courier → "Awaiting Courier"
  - Else (open) → "Auction" (summary) or "Auction Open" (detail)
- **canBid**: `!sc.isAuctionClosed() && !(sc.getExpiryTime() < serverTime)`
- **canAcceptCourier**: `sc.isAuctionClosed() && !sc.isCourierAssigned() && !sc.isDelivered()`
- **Detail bid list**: copy entrySet, sort by value descending (Float.compare(e2, e1)), for each: name, fmt+" emeralds", isHighest = Math.abs(amount - highest) < 0.001f
- **Other fields**: courierRewardDisplay always `fmt(sc.getCourierReward()) + " ◎"` (even if 0); acceptedBidDisplay only if >0; deliveryProgress only if closed && !delivered; destinationTownName = winning name only if closed; courierName = "Snail Mail" or "Courier (ID: 8hex)".
- **Generic (non-sell) fallback**: type from contract.getType(), resource/price "unknown"/"N/A", qty=0, all flags false, isExpired via BCTimeUtils.isExpired, isDelivered=contract.isCompleted(), empty bids.
- **Time handling**: all remaining/created/expired strings and booleans use the caller-supplied serverTime snapshot (not live System.current). formatTimeRemaining and formatDateTime (MM/dd HH:mm) from BCTimeUtils; isExpired is strict `serverNow > expiryEpoch`.
- **History overrides happen in caller** (RequestContractListPacket): !showAll → effectiveSize=50; showAll → Integer.MAX_VALUE. Builder just obeys the pageSize it receives.
- **Player param**: accepted in signatures from packet layer but completely unused inside both builders.

## Edge cases & behaviors
- Empty input list or no matching SellContracts for tab → result.contracts empty, totalCount=0, hasMore=false.
- page >= needed pages → slice yields empty list, hasMore=false.
- pageSize=0 or negative → treated as 20.
- Very large pageSize → returns the entire (filtered/sorted) set in one page.
- SellContract with 0 bids: highestBidDisplay="No bids", canBid depends on open+not-expired.
- Partial delivery (delivered 40/128): status "In Transit (40/128)", deliveryProgressDisplay="40/128 delivered", canAcceptCourier=false.
- Snail mail path: courierName="Snail Mail", status="Snail Mail".
- Auction just expired at snapshot: isExpired=true (from BCTime), canBid=false, but may still appear in AUCTION tab (filter only looks at isAuctionClosed, not expiry).
- Tie bids: getHighestBidder in base pins first-in-iteration (see T-003), but detail list sorts stable desc; isHighest uses tolerance so both could? No — only the first after sort gets exact match to highest.
- Non-SellContract in board list: skipped for the three tabs (only appear as generic if a detail is requested directly).
- Float price math: uses the SellContract values directly (already clamped on ctor/load); formatting drops trailing .0 via the DecimalFormat pattern.
- Bid list defensive: `new ArrayList<>(bids)` in VM ctor; builder builds fresh list.
- Duplicate status/can* logic lives in both builders (copy-paste); a change in one can diverge from the other (e.g. "Auction" vs "Auction Open").

## Test coverage
- Test files: `common/src/test/java/com/quackers29/businesscraft/contract/ContractSummaryViewModelBuilderTest.java`, `common/src/test/java/com/quackers29/businesscraft/contract/ContractDetailViewModelBuilderTest.java`
- Covered: all tab filters + only-Sell behavior, both sort orders, paging math (clamps, hasMore, empty slices), status strings for every branch, canBid/canAcceptCourier rules with time boundaries, bid list sorting + isHighest tolerance, price formatting, generic fallback, time/expiry delegation with controlled serverTime, summary vs detail status string difference, empty and boundary page cases. Hand-computed expects for counts, slices, and string contents. 23 tests total (14 summary + 9 detail).
- Not covered: actual packet round-trip or client deserialization (network), timezone effects on date strings (tests force UTC), live board mutation under ContractBoard, UI button wiring.

## Open questions
- **Duplicated logic**: calculateStatus, calculateCanBid, and calculateCanAcceptCourier are duplicated (with one string difference). Easy source of future divergence. Open: should the builders share a common helper, or should status strings be single-sourced from the contract itself?
- Player parameter is accepted but ignored in both public build methods — dead parameter from the packet layer. Harmless but noisy.
- History "showAll" cap to Integer.MAX_VALUE is handled in the packet, not visible in builder API; the builder's pageSize=MAX path is exercised only via that.
- Float comparison for isHighest uses 0.001 tolerance; bids are floats from emerald math — potential for display oddities on very close bids, but matches how highest is chosen elsewhere.
- Non-sell contracts are carried in the board but effectively invisible in the three main tabs; only direct ID detail requests surface them as "unknown".

## Related
- [[Trade/Trade Overview]]
- [[Trade/Contracts/Sell Contract Lifecycle]]
- [[Trade/Contracts/Auction Resolution]]
- [[Trade/Contracts/Courier Delivery Rewards]]
