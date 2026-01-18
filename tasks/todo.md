# BusinessCraft - Server-Client Sync Architecture Refactoring

## 🎯 **CURRENT PRIORITY: SERVER-AUTHORITATIVE ARCHITECTURE**

**🔧 OBJECTIVE:** Implement the "View-Model" pattern to eliminate client-side business logic duplication and create a true server-authoritative architecture where the client is a "dumb terminal" that only renders pre-calculated data.

**📋 PROBLEM SUMMARY:** Current architecture violates the server-authoritative principle by duplicating business logic on both client and server sides, leading to potential desyncs and security vulnerabilities.

## 🎉 **MAJOR SUCCESS: PHASE 1 COMPLETE! ALL CRITICAL VIOLATIONS FIXED!**

**✅ RESOURCE STATISTICS VIEW-MODEL ARCHITECTURE IMPLEMENTED (Phase 1.1)**
- **Server-Authoritative Pattern**: ✅ FULLY IMPLEMENTED
- **Client "Dumb Terminal"**: ✅ ACHIEVED
- **Zero Client Business Logic**: ✅ CONFIRMED
- **Build Status**: ✅ SUCCESS (all platforms compile)

**✅ PRODUCTION FORMULA VIEW-MODEL ARCHITECTURE IMPLEMENTED (Phase 1.2)**
- **View-Models Created**: ✅ ProductionStatusViewModel & Builder (430+ lines)
- **Critical Issue Fixed**: ✅ Removed all ProductionRegistry.get() from UI!
- **Sync System**: ✅ Server sends pre-calculated production display data
- **Build Status**: ✅ SUCCESS (all platforms compile cleanly)

**✅ MARKET PRICE VIEW-MODEL ARCHITECTURE IMPLEMENTED (Phase 1.3)**
- **View-Models Created**: ✅ MarketViewModel & Builder (310+ lines)
- **Critical Issue Fixed**: ✅ Eliminated 38 lines of client-side price calculations!
- **Sync System**: ✅ Server sends pre-calculated prices for ALL items
- **Build Status**: ✅ SUCCESS (all platforms compile cleanly)

**📊 COMPLIANCE IMPROVEMENT:**
- **BEFORE**: ~30% server-authoritative compliance
- **CURRENT**: ~85% server-authoritative compliance (PHASE 1 COMPLETE! 🎉)
- **TARGET**: 100% server-authoritative compliance
- **NEXT WORK**: Phase 2.1 - Upgrade registry view-model & eliminate remaining client config access

---

## 🔧 **PHASE 1: CRITICAL SERVER-CLIENT SYNC VIOLATIONS** - ✅ **COMPLETE (3/3)**

### **1.1 Resource Statistics Calculation** ✅ **COMPLETED** - Eliminated client-side resource calculations, created server-calculated view-models (175+ lines)

### **1.2 Production Formula Evaluation** ✅ **COMPLETED** - Eliminated client-side ProductionRegistry access, created server-calculated production view-models (270+ lines)

### **1.3 Market Price Resolution** ✅ **COMPLETED** - Eliminated client-side price calculations, created global market view-models with all item prices


---

## 🟡 **PHASE 2: MEDIUM PRIORITY VIOLATIONS**

### **2.1 Upgrade Registry View-Model** ✅ **COMPLETED** - Eliminated client-side UpgradeRegistry access, created upgrade tree view-models (315+ lines)

### **2.3 Menu System Fallbacks** ✅ **COMPLETED** - Eliminated client-side menu calculations, created TownInterfaceViewModel for UI state

### **2.4 Trading Component Logic** ✅ **COMPLETED** - Implemented currency system and trading view-models with server-calculated prices

### **2.5 Global Market Unification** ✅ **COMPLETED** - Unified auction and trade pricing systems into single persistent GlobalMarket with dynamic item tracking

---

## 🟢 **PHASE 3: CONFIGURATION & CLEANUP**

### **3.1 CSV Configuration Distribution** ✅ **COMPLETED** - Documented registry loading strategy and created architecture validation checklist

### **3.2 Client Sync Helper Simplification** ✅ **COMPLETED** - Removed deprecated calculation methods, eliminated legacy ResourceSyncPacket (135+ lines removed)

### **3.3 View-Model Architecture Enhancements** 🔧 **MEDIUM** (Recommended)
- [ ] **Create ViewModelCache Helper Class**:
  - Consolidate all view-model caching in TownInterfaceEntity
  - Reduce field clutter (6-8 individual fields → 1 cache object)
  - Type-safe getter/setter methods: `cache.get(TownResourceViewModel.class)`
  - **Priority**: 🟢 **RECOMMENDED** - Implement during Phase 1.3 or 2.1
  - **Effort**: 1-2 hours, ~50 lines of code
  - **Benefit**: Cleaner TownInterfaceEntity, easier to manage view-models
- [ ] **Create BaseViewModelSyncPacket Pattern** (Optional):
  - Abstract base class for common packet serialization logic
  - Reduces boilerplate from ~90 lines to ~30 lines per packet
  - Refactor existing packets: ResourceViewModelSyncPacket, ProductionViewModelSyncPacket
  - **Priority**: 🟡 **NICE-TO-HAVE** - Only if packet code feels repetitive
  - **Effort**: 2-3 hours to create base class and refactor
  - **Benefit**: Less boilerplate, consistent packet structure
- [ ] **Centralized ViewModelSyncManager** (Future Optimization):
  - Single manager class to coordinate all view-model syncing
  - Methods: `syncAll()`, `syncSelective()`, `markDirty()`
  - Enables future dirty-flag optimization (only sync changed data)
  - **Priority**: 🔵 **PHASE 3+** - Only if performance becomes an issue
  - **Effort**: 3-4 hours
  - **Benefit**: Network optimization, centralized sync timing control

**📋 Architectural Review Notes:**
- **Current Pattern Verdict**: ✅ **ARCHITECTURALLY SOUND** - Continue with modular view-models
- **Scalability**: Current approach scales well to 6-8 view-models without issues
- **Maintainability**: Separation of concerns is excellent, code is clear and testable
- **Don't Over-Engineer**: YAGNI principle - only implement enhancements when pain is felt
- **Incremental Refactoring**: Can always add abstractions later if boilerplate becomes genuinely painful

---

## 🕐 **PHASE 4: TIME UTILITY MODULE**

**Goal:** Centralize time formatting, eliminate client `System.currentTimeMillis()` for display.

### **4.1 Create `BCTimeUtils.java`** 🔧 **MEDIUM PRIORITY**
- [ ] **Create `BCTimeUtils.java`** (~60 lines):
  - `formatTimeRemaining(long expiryEpoch, long serverNow)` → "5m 30s" or "Expired"
  - `formatDateTime(long epoch)` → "01/14 15:30" in client timezone
  - `formatDuration(long millis)` → "2h 15m 30s"
  - `isExpired(long expiryEpoch, long serverNow)` → boolean

### **4.2 Timezone Configuration**
- [ ] **Add timezone setting to `businesscraft.properties`**:
  - `clientTimezone=AUTO` (default: use system timezone)
  - Options: "AUTO", "UTC", "America/New_York", "Europe/London", etc.
  - `BCTimeUtils.setTimezone(String)` to configure on client startup

### **4.3 Audit Notes**
**Client `System.currentTimeMillis()` usage that is OK (animation/debouncing):**
- `BCScrollableComponent.java:164,183` - Scroll drag timing ✅ OK
- `UIGridBuilder.java:109,887` - Debouncing ✅ OK
- `ProductionTab.java:52,68` - Update throttling ✅ OK
- `BCComponent.java:141,170,558` - Animation timing ✅ OK
- `UIDirectRenderer.java:94` - Cursor blink ✅ OK
- `ModalEventHandler.java:50` - Click debounce ✅ OK
- `BCModalInventoryScreen.java:353,432,749` - Animation ✅ OK

**Usage that needs fixing (business logic time display):**
- `ContractBoardScreen.java:187-189` - Time remaining display
- `ContractDetailScreen.java:218-220` - Time remaining display
- These will be fixed by Phase 5's generic query system

---

## 🔧 **PHASE 5: CONTRACT BOARD VIEW-MODEL COMPLIANCE**

### **📋 Context: Why Contract Board Needs Work**

The Contract Board is the **only system** that doesn't follow the view-model pattern properly:

| Issue | Location | Problem |
|-------|----------|---------|
| Client-side filtering | `ContractBoardScreen.java:128-155` | `filterContractsByTab()` streams/filters |
| Client time calculation | `ContractBoardScreen.java:187-189` | `System.currentTimeMillis()` for display |
| Client time calculation | `ContractDetailScreen.java:218-220` | `System.currentTimeMillis()` for display |
| Variable bid lists | `Contract.java:15` | `Map<UUID, Float> bids` - unbounded |
| Server-wide data | `ContractSyncPacket` | Sends ALL contracts, not player-scoped |

**Why Contract Board is Different:**
- Other view-models sync **town-scoped** data (your resources, your upgrades)
- Contract Board syncs **server-wide** data (all contracts from all towns)
- Bid maps have **no cap** and grow with auction activity

---

### **5.1 Core Philosophy: Server Stores All, Client Gets Paginated**

**Key Principle:** The server is the permanent record of ALL contracts ever created. The client only receives what it needs to display the current view.

| Aspect | Server | Client |
|--------|--------|--------|
| **Storage** | ALL contracts forever (no limits) | Only current page/view |
| **History** | Complete audit trail | Paginated on-demand fetch |
| **Active contracts** | Full list | Synced when relevant |
| **Expired contracts** | Kept for history | Only fetched when viewing history tab |

---

### **5.2 Contract Summary View-Model (List View)** ✅ **COMPLETE**

**Goal:** Replace full Contract sync with lightweight summaries for list display.

- [x] **Create `ContractSummaryViewModel.java`** ✅
  - Location: `contract/viewmodel/ContractSummaryViewModel.java`
  - Fields: `contractId`, `resourceId`, `quantity`, `issuerTownName`, `timeRemainingDisplay`, 
    `highestBidDisplay`, `statusDisplay`, `priceDisplay`, `canBid`, `canAcceptCourier`, `isExpired`, `isDelivered`

- [x] **Create `ContractSummaryViewModelBuilder.java`** ✅
  - Location: `contract/viewmodel/ContractSummaryViewModelBuilder.java`
  - Server-side filtering into tabs: AUCTION, ACTIVE, HISTORY
  - Server-side sorting (expiring first for auction/active, latest first for history)
  - Pagination support with `ContractListResult` record
  - Time formatting via `formatTimeRemaining()` (eliminates client `System.currentTimeMillis()`)

- [x] **Create `ContractListSyncPacket.java`** ✅
  - Location: `network/packets/ui/ContractListSyncPacket.java`
  - Contains: `List<ContractSummaryViewModel>`, pagination metadata, `serverCurrentTime`, `marketPrices`
  - Registered in `PacketRegistry.java`

- [x] **Create `RequestContractListPacket.java`** ✅
  - Location: `network/packets/ui/RequestContractListPacket.java`
  - Contains: `tab`, `page`, `pageSize`
  - Server handler builds and sends `ContractListSyncPacket`
  - Registered in `PacketRegistry.java`

- [x] **Update `ContractBoardScreen.java`** ✅
  - Removed `filterContractsByTab()` method - server handles filtering
  - Removed `System.currentTimeMillis()` call - uses view-model `timeRemainingDisplay`
  - Updated `populateGrid()` to use `ContractSummaryViewModel`
  - Added "Load More" button for history tab pagination
  - Requests contract list from server on init and tab change

- [x] **Update `TownDataCacheManager.java`** ✅
  - Added `ContractListCache` class for caching contract lists per tab
  - Added `updateContractList()`, `getContractListCache()`, `getCachedContracts()` methods

---

### **5.3 Contract Detail View-Model (On-Demand)** ✅ **COMPLETE**

**Goal:** Fetch full contract details only when user clicks "View".

- [x] **Create `ContractDetailViewModel.java`** ✅
  - Location: `contract/viewmodel/ContractDetailViewModel.java`
  - All summary fields plus: `bids` list, `courierName`, `deliveryProgressDisplay`,
    `createdDateDisplay`, `expiresDateDisplay`, `winningBidderName`, `isAuctionClosed`
  - Inner record `BidDisplayInfo { bidderName, amountDisplay, isHighest }`

- [x] **Create `ContractDetailViewModelBuilder.java`** ✅
  - Location: `contract/viewmodel/ContractDetailViewModelBuilder.java`
  - Builds full detail with bid list (sorted by amount)
  - All time calculations server-side

- [x] **Create `RequestContractDetailPacket.java`** ✅
  - Location: `network/packets/ui/RequestContractDetailPacket.java`
  - Contains: `UUID contractId`
  - Server builds detail and responds with `ContractDetailSyncPacket`
  - Registered in `PacketRegistry.java`

- [x] **Create `ContractDetailSyncPacket.java`** ✅
  - Location: `network/packets/ui/ContractDetailSyncPacket.java`
  - Contains: `ContractDetailViewModel` + `serverCurrentTime`
  - Registered in `PacketRegistry.java`

- [x] **Update `ContractDetailScreen.java`** ✅
  - Requests detail on screen open via `RequestContractDetailPacket`
  - No client `System.currentTimeMillis()` for business logic
  - Shows "Loading details..." while waiting for response
  - Uses `renderViewModelDetails()` for server-provided display strings
  - Bid list from view-model (server pre-sorted)

- [x] **Update `TownDataCacheManager.java`** ✅
  - Added `ContractDetailCache` class
  - Added `updateContractDetail()`, `getCachedContractDetail()`, `clearContractDetailCache()`

---

### **5.4 Implementation Order**

**Step 1: Remove Hard Limits** ✅ **DONE**
1. ~~**Remove 100 contract limit** in `ContractBoard.java:97-108` (the cleanup loop)~~ ✅
2. ~~Server now stores ALL contracts permanently as historical record~~ ✅
3. ~~Archive flag not needed~~ - existing `SellContract` flags sufficient:
   - `isAuctionClosed()` → auction ended
   - `isDelivered()` → delivery complete
   - Tab filtering: Auction=`!closed`, Active=`closed && !delivered`, History=`delivered`

**Step 2: Contract List with Pagination** ✅ **DONE**
1. ~~Create `ContractSummaryViewModel` + Builder~~ ✅
2. ~~Create `RequestContractListPacket` (client requests specific tab + page)~~ ✅
3. ~~Create `ContractListSyncPacket` (with pagination metadata)~~ ✅
4. ~~Server responds to list requests via `RequestContractListPacket` handler~~ ✅
5. ~~Update `ContractBoardScreen` to consume view-model~~ ✅
6. ~~Delete `filterContractsByTab()` method~~ ✅
7. ~~Add "Load More" / page controls for History tab~~ ✅

**Step 3: Contract Detail (Complete the Pattern)** ✅ **DONE**
1. ~~Create `ContractDetailViewModel` + Builder~~ ✅
2. ~~Create `RequestContractDetailPacket` + `ContractDetailSyncPacket`~~ ✅
3. ~~Update `ContractDetailScreen` to request on open~~ ✅
4. ~~Add loading state UI~~ ✅

**Step 4: Cleanup** (Optional - can keep legacy for backwards compatibility)
1. Old `ContractSyncPacket` still works for legacy code paths
2. New view-model packets handle modern contract board UI
3. All client time calculations eliminated from view-model path

---

### **5.5 Data Size & Network Optimization**

| Metric | Current | After Phase 5 |
|--------|---------|---------------|
| **Server storage** | Hard limit: 100 contracts | **NO LIMIT** - all history preserved |
| **Client sync (list)** | All 100+ contracts + bids | **Paginated** summaries (~20 per page) |
| **Client sync (detail)** | All upfront | **On-demand** per contract |
| List packet size | ~50KB worst case | ~2KB per page (20 summaries) |
| Detail packet size | N/A | ~1-2KB per contract |
| Client filtering | `filterContractsByTab()` | **ELIMINATED** |
| Client time calc | `System.currentTimeMillis()` | **ELIMINATED** |

**Pagination defaults:**
- Auction tab: All active auctions (typically small, no pagination needed)
- Active tab: All in-progress contracts (typically small, no pagination needed)
- History tab: **Paginated** - 20 contracts per page, "Load More" to fetch older

---

### **5.6 Contract Board Compliance Summary**

| Item | Before | After |
|------|--------|-------|
| **Server storage** | Hard limit (100 contracts) | **No limit** - permanent record |
| **History access** | All synced upfront | **Paginated** on-demand |
| Contract Filtering | Client-side (`ContractBoardScreen:128`) | Server-side (pre-filtered lists) |
| Time Calculations | Client `System.currentTimeMillis()` | Server epoch + `BCTimeUtils` |
| Bid Display | Full bid map synced | Summary in list, detail on-demand |
| Button States | Client checks | Server-provided booleans |
| Tooltip Strings | Client generates | Server pre-calculated |
| Data Strategy | All data upfront | Paginated list + on-demand detail |
| Compliance | ~40% | Target: 100% |

---

### **5.7 Packet Architecture Note**

**Why we're keeping specific packets (not going generic):**

Per community analysis, 99% of Minecraft mods use specific packet classes per message type:
- **Type safety**: Each packet has compile-time guarantees
- **Community standard**: Create, AE2, Mekanism all use this pattern
- **Minecraft's design**: Vanilla has ~100+ specific packet classes
- **Easier debugging**: Clear handler per packet type

This phase adds **4 new specific packets** (following the established pattern):
- `RequestContractListPacket` - client requests tab + page
- `ContractListSyncPacket` - server responds with paginated summaries
- `RequestContractDetailPacket` - client requests full contract
- `ContractDetailSyncPacket` - server responds with full contract

Net change: **+3 packets** (replaces 1, adds 4) but significantly better architecture with proper pagination.

---

## 💰 **PHASE 6: ECONOMY STABILIZATION** ✅ **COMPLETE**

### **6.1 Problem & Solution Summary**

**Problem:** Failed auctions (no bids) dropped GPI by 20%, causing death spiral to zero. Auctions fail for many reasons beyond price (capacity limits, no demand, etc.).

**Solution Implemented: Trades-Only Price Discovery + Need-Based Bidding**

| Change | Location | Description |
|--------|----------|-------------|
| Remove failed auction price drop | `ContractBoard.java:405-419` | Failed auctions no longer affect GPI |
| Add need-based bid modifier | `TownContractComponent.java` | Buyers bid 0.80x-1.20x GPI based on need level |

**Result:** Symmetric pricing enables natural equilibrium:
- **Sellers**: List at GPI × 0.55 to 1.10 (based on excess)
- **Buyers**: Bid at GPI × 0.80 to 1.20 (based on need) + ±5% randomness
- **No trades**: GPI unchanged (no signal from silence)
- **High supply/low demand**: Both list and bid low → price drops naturally
- **Low supply/high demand**: Both list and bid high → price rises naturally

**Safety nets:** MIN_PRICE floor (0.001), 1 emerald minimum bid

---

### **6.2 Contract History Display** ✅ **COMPLETE**

**Goal**: Replace pagination with recent-first display + optional "Show All" button.

- [x] **Server**: Send last 50 contracts by default, add `showAll` parameter ✅
- [x] **Client**: Replace "Load More" with "Show All History" button ✅
- [x] **State**: Add `showAllHistory` tracking, reset on tab switch ✅
- [x] **Build**: Verified all changes compile ✅

**Files Modified:**
- `RequestContractListPacket.java`: Added `showAll` boolean parameter
- `ContractBoardScreen.java`: Added `showAllHistory` state, "Show All" button, reset on tab switch

---

## 📊 **PHASE 7: VERIFICATION & TESTING**

### **7.1 Architecture Compliance Testing** 🧪 **CRITICAL**
- [ ] **Create view-model validation tests**:
  - Verify client contains zero business logic
  - Test that all UI displays pre-calculated values
  - Ensure no CSV files are loaded on client
- [ ] **Sync integrity testing**:
  - Test server-client data consistency
  - Verify no calculation discrepancies
  - Load testing with multiple clients
- [ ] **Security validation**:
  - Confirm client cannot manipulate business logic
  - Test that all financial calculations are server-authoritative
  - Verify proper validation of client requests

### **7.2 Performance Analysis** 🧪 **MEDIUM**
- [ ] **Network bandwidth optimization**:
  - Measure packet size reduction from view-model approach
  - Verify on-demand fetch reduces data transfers
  - Compare before/after sync performance
- [ ] **Client performance testing**:
  - Verify removal of calculation load from client
  - Test UI responsiveness with pure display rendering
  - Memory usage analysis of simplified client

---

## 🎯 **SUCCESS CRITERIA: THE "DUMB TERMINAL" CLIENT**

### ✅ **View-Model Pattern Compliance**
- **Client Calculations**: ZERO business logic calculations on client
- **Configuration Access**: Client never reads CSV/config files
- **Display Only**: Client renders pre-calculated strings and states
- **Server Authority**: All business logic calculations happen server-side only

### ✅ **Network Architecture Goals**
- **Single Source of Truth**: Server maintains all authoritative state
- **View-Model Packets**: All sync packets contain display-ready data
- **No Duplication**: Zero logic duplication between client and server
- **Security**: Client cannot manipulate or calculate business values

### ✅ **UI Framework Goals**
- **Pure Rendering**: UI components only display received data
- **No Fallbacks**: No client-side calculation fallbacks in UI
- **String-Based Display**: All numbers/states as formatted display strings
- **Server-Driven State**: All button states and UI logic server-controlled

---

## 📋 **IMPLEMENTATION NOTES**

### **Architecture Principles**
- **Golden Rule**: "The Server calculates the View; The Client renders the View"
- **No Client Logic**: Client is truly a "dumb terminal" for game state
- **View-Model Pattern**: All network packets contain display-ready view-models
- **Server Authority**: Single source of truth for all business calculations

### **Current Compliance Status** (Updated 2026-01-14)
- **✅ COMPLETED**: Resource statistics view-model (Phase 1.1)
- **✅ COMPLETED**: Production formula view-model (Phase 1.2)
- **✅ COMPLETED**: Market price view-model (Phase 1.3)
- **✅ COMPLETED**: Upgrade registry view-model (Phase 2.1) 🎉
- **✅ COMPLETED**: Menu fallbacks & trading logic (Phase 2.3-2.4)
- **✅ COMPLETED**: Global Market Unification (Phase 2.5) 🔧
- **✅ COMPLETED**: CSV configuration documentation (Phase 3.1) 📝
- **✅ COMPLETED**: Client sync helper simplification (Phase 3.2) 🧹
- **✅ COMPLETED**: Tourism indicator view-model fix (TownInterfaceViewModel) 🎉
- **⚠️ REMAINING**: Optional enhancement phase (3.3)
- **🕐 PENDING**: Time handling & Contract Board view-model (Phase 4)
- **🕐 PENDING**: Contract Board View-Model Compliance (Phase 5) - In Progress
- **✅ COMPLETED**: Economy Stabilization - Trades-Only + Need-Based Bidding (Phase 6.9) 💰
- **🧪 PENDING**: Verification & Testing (Phase 7)
- **📊 Overall**: ~95% compliant with target architecture (Contract Board is ~40%)

### **Development Guidelines**
- **Before Changes**: Verify current Forge functionality works correctly
- **During Changes**: Test each view-model conversion individually
- **After Changes**: Ensure both Forge and Fabric platforms maintain functionality
- **Testing**: Verify client can never influence server calculations

### **Architectural Review Results** (2026-01-12)
**✅ VERDICT: Current Modular View-Model Pattern is ARCHITECTURALLY SOUND**

**Strengths of Current Approach:**
- ✅ Excellent separation of concerns (each domain is independent)
- ✅ Incremental & non-breaking (can add new view-models without touching existing)
- ✅ Highly maintainable (clear responsibilities, easy to debug)
- ✅ Type-safe (compile-time checking, no generic type erasure)
- ✅ Good team velocity (pattern is working, don't disrupt progress)

**Alternatives Considered & Rejected:**
- ❌ Unified TownUIViewModel: Too inflexible, doesn't match incremental needs
- ❌ Over-abstraction with registries: Adds complexity without clear benefit
- 🟡 Event-driven dirty flags: Premature optimization, not needed yet

**Recommended Enhancements (See Phase 3.3):**
- 🟢 ViewModelCache Helper: Consolidate cache management (1-2 hours effort)
- 🟡 BaseViewModelSyncPacket: Reduce packet boilerplate (optional)
- 🔵 Centralized Sync Manager: Future optimization only if needed

**Decision:** Continue with current pattern through all remaining phases. Current approach scales perfectly to 6-8 view-models. Don't over-engineer - YAGNI principle applies.

---
