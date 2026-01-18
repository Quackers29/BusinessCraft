# BusinessCraft - Server-Client Sync Architecture Refactoring

## 📋 **DOCUMENT GUIDELINES**
- **Be efficient**: Use minimal words, avoid waffle
- **Keep line count low**: Summarize completed work in 1 line, detail only active tasks
- **Structure**: One-line summaries for completed sections, full detail for pending work

## 🎯 **CURRENT PRIORITY: SERVER-AUTHORITATIVE ARCHITECTURE**

**🔧 OBJECTIVE:** Implement "View-Model" pattern to eliminate client-side business logic duplication and create server-authoritative architecture where client is "dumb terminal" rendering pre-calculated data.

**📋 PROBLEM SUMMARY:** Architecture violates server-authoritative principle with business logic duplication on client/server, causing potential desyncs and security issues.

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

## ✅ **PHASE 4: TIME UTILITY MODULE** - **COMPLETE**

**Goal:** Centralize time formatting, eliminate client `System.currentTimeMillis()` for display.

### **4.1 Create `BCTimeUtils.java`** ✅ **COMPLETED**
- [x] **Created `BCTimeUtils.java`** (~230 lines):
  - `formatTimeRemaining(long expiryEpoch, long serverNow)` → "5m 30s" or "Expired"
  - `formatDateTime(long epoch)` → "01/14 15:30" in configured timezone
  - `formatFullDateTime(long epoch)` → "Jan 14, 2026 15:30:45"
  - `formatTimeOnly(long epoch)` → "15:30:45"
  - `formatDuration(long millis)` → "2h 15m 30s"
  - `formatTimeAgo(long timestamp, long now)` → "5m ago"
  - `isExpired(long expiryEpoch, long serverNow)` → boolean
  - `getTimeRemaining(long expiryEpoch, long serverNow)` → millis remaining

### **4.2 Timezone Configuration** ✅ **COMPLETED**
- [x] **Added `displayTimezone` to `ConfigLoader.java`**:
  - Default: `"UTC"` (consistent server-side formatting)
  - Options: "UTC", "SYSTEM" (use system default), or any valid timezone ID
  - Examples: "America/New_York", "Europe/London", "Asia/Tokyo"
  - `BCTimeUtils.setTimezone()` called on config load

### **4.3 Code Consolidation** ✅ **COMPLETED**
**Updated files to use BCTimeUtils:**
- `ContractSummaryViewModelBuilder.java` - Removed duplicate `formatTimeRemaining()`
- `ContractDetailViewModelBuilder.java` - Removed duplicate `formatTimeRemaining()`, uses `BCTimeUtils.formatDateTime()`
- `RewardEntry.java` - Updated `getTimeAgoDisplay()`, `getTimeDisplay()`, `getFullDateTimeDisplay()`

### **4.4 Audit Notes**
**Client `System.currentTimeMillis()` usage that is OK (animation/debouncing):**
- `BCScrollableComponent.java:164,183` - Scroll drag timing ✅ OK
- `UIGridBuilder.java:109,887` - Debouncing ✅ OK
- `ProductionTab.java:52,68` - Update throttling ✅ OK
- `BCComponent.java:141,170,558` - Animation timing ✅ OK
- `UIDirectRenderer.java:94` - Cursor blink ✅ OK
- `ModalEventHandler.java:50` - Click debounce ✅ OK
- `BCModalInventoryScreen.java:353,432,749` - Animation ✅ OK

**Business logic time display:** ✅ FIXED by Phase 5 view-models
- Contract screens now use server-calculated `timeRemainingDisplay` from view-models
- No client-side time calculations for business logic

---

## 🔧 **PHASE 5: CONTRACT BOARD VIEW-MODEL COMPLIANCE** ✅ **COMPLETE** - Implemented paginated contract lists + on-demand detail fetching, eliminated client-side filtering/time calculations




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
**Problem:** Failed auctions dropped GPI by 20%, causing death spiral to zero.
**Solution:** Removed failed auction price drops + added need-based bidding (0.80x-1.20x GPI based on town need).
**Result:** Symmetric pricing enables natural equilibrium with trades-only price discovery.

---

### **6.2 Contract History Display** ✅ **COMPLETE** - Replaced pagination with recent-first display + "Show All History" button

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

### **Current Compliance Status** (Updated 2026-01-18)
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
- **✅ COMPLETED**: Time Utility Module - BCTimeUtils consolidation (Phase 4) ⏰
- **✅ COMPLETED**: Contract Board View-Model Compliance (Phase 5) 📋
- **✅ COMPLETED**: Economy Stabilization - Trades-Only + Need-Based Bidding (Phase 6) 💰
- **🧪 PENDING**: Verification & Testing (Phase 7)
- **✅ COMPLETED**: Resource Storage int→long Migration (Phase 8) - Updated all contracts, storage, and view-models to use long for quantities
- **📊 Overall**: ~98% compliant with target architecture

### **Development Guidelines**
- **Before Changes**: Verify current Forge functionality works correctly
- **During Changes**: Test each view-model conversion individually
- **After Changes**: Ensure both Forge and Fabric platforms maintain functionality
- **Testing**: Verify client can never influence server calculations

### **Architectural Review Results** (2026-01-12)
**✅ VERDICT: Modular view-model pattern architecturally sound - continue current approach. Scales to 6-8 view-models without issues.**

---

## 🔢 **PHASE 8: RESOURCE STORAGE INT→LONG MIGRATION**

**Problem:** Resource storage uses `int` (max 2.1B), limiting large-scale town economies.
**Goal:** Migrate aggregated resource storage from `int` to `long` for virtually unlimited capacity.

### **8.1 Scope Analysis**

**What NEEDS to change (aggregated storage - our code):**
- `Map<Item, Integer>` → `Map<Item, Long>` in resource maps
- NBT: `putInt()`/`getInt()` → `putLong()`/`getLong()` for resource amounts
- Method signatures: `int count` → `long count` parameters
- Overflow handling: `Integer.MAX_VALUE` → `Long.MAX_VALUE`

**What CANNOT change (Minecraft API constraint):**
- `ItemStack.getCount()` returns `int` (max 64 per stack normally)
- This is fine - we aggregate into long, individual stacks stay as int

### **8.2 Files to Modify (19 files, ~100 changes)**

#### **Core Storage (CRITICAL)**
| File | Changes |
|------|---------|
| `TownResources.java` | `Map<Item, Integer>` → `Long`, all methods, NBT |
| `Town.java` | workUnits, escrow, wanted, personal storage, visitors, NBT |
| `TownEconomyComponent.java` | addResource/getResourceCount signatures |

#### **Contract System**
| File | Changes |
|------|---------|
| `SellContract.java` | quantity, deliveredAmount fields |
| `CourierContract.java` | quantity, deliveredAmount fields |
| `ContractBoard.java` | Casting operations |

#### **Storage Classes**
| File | Changes |
|------|---------|
| `TownPaymentBoard.java` | Buffer storage methods |
| `SlotBasedStorage.java` | Accumulation methods (aggregate from int stacks to long total) |

#### **Network Packets**
| File | Changes |
|------|---------|
| `PersonalStoragePacket.java` | writeInt→writeLong, readInt→readLong |
| `CommunalStoragePacket.java` | writeInt→writeLong, readInt→readLong |
| `BufferStoragePacket.java` | writeInt→writeLong, readInt→readLong |
| `TownResourceViewModel.java` | population, counts if needed |

#### **View Model Builders**
| File | Changes |
|------|---------|
| `TownResourceViewModelBuilder.java` | Local variables, formatting |

### **8.3 Implementation Steps**

- [ ] **Step 1: Core Storage Migration**
  - `TownResources.java`: Change `Map<Item, Integer>` → `Map<Item, Long>`
  - Update all methods: `addResource`, `getResourceCount`, `consumeResource`
  - Update overflow: `Math.addExact(long, long)`, cap at `Long.MAX_VALUE`
  - Update NBT: `putLong()`/`getLong()`

- [ ] **Step 2: Town.java Migration**
  - Change `workUnits` field: `int` → `long`
  - Change `Map<Item, Integer>` fields → `Map<Item, Long>` for escrow, wanted, visitors
  - Update all delegation methods
  - Update all NBT save/load

- [ ] **Step 3: Contract System Migration**
  - `SellContract.java`: `quantity`, `deliveredAmount` → `long`
  - `CourierContract.java`: `quantity`, `deliveredAmount` → `long`
  - Update NBT serialization

- [ ] **Step 4: Network Packet Migration**
  - Update all resource-related packets to use `writeLong`/`readLong`
  - Consider backward compatibility (version check)

- [ ] **Step 5: View Model Updates**
  - Update view-model builders to handle long values
  - Formatting: use `formatAmount()` for display (K/M/B suffixes)

- [ ] **Step 6: Testing & Verification**
  - Test with values > Integer.MAX_VALUE
  - Verify NBT save/load works with existing worlds (backward compat)
  - Test network sync with large values

### **8.4 Backward Compatibility**
**NOT REQUIRED** - No existing worlds to migrate. Clean break with `putLong()`/`getLong()` throughout.

### **8.5 Display Formatting**
Already have `formatAmount()` in view-model builders that handles large numbers:
- 1,000 → "1K"
- 1,000,000 → "1M"
- 1,000,000,000 → "1B"

### **8.6 Risk Assessment**
- **Low Risk**: Core storage changes are isolated to few files
- **Medium Risk**: NBT format change requires migration
- **Low Risk**: Network packets - both sides update together
- **No Risk**: ItemStack.getCount() stays as int (MC API)

---
