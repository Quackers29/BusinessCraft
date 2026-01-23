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
- **✅ COMPLETED**: Configuration File Modernization (Phase 9) - Migrated from .properties to TOML using NightConfig
- **✅ COMPLETED**: GPI Supply Pressure (Phase 10) - Failed auctions now reduce GPI by 5%
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

### **8.3 Implementation Steps** ✅ **ALL COMPLETE**

- [x] **Step 1: Core Storage Migration** ✅
  - `TownResources.java`: Changed `Map<Item, Integer>` → `Map<Item, Long>`
  - Updated all methods: `addResource`, `getResourceCount`, `consumeResource`
  - Updated NBT: `putLong()`/`getLong()`

- [x] **Step 2: Town.java Migration** ✅
  - Changed `workUnits`, `totalTouristsArrived` fields: `int` → `long`
  - Changed `Map<Item, Integer>` fields → `Map<Item, Long>` for escrow, wanted, visitors, personalStorage
  - Updated all delegation methods and NBT save/load

- [x] **Step 3: Contract System Migration** ✅
  - `TownContractComponent.java`: All resource count variables now `long`
  - Contract board operations use long arithmetic

- [x] **Step 4: Network Packet Migration** ✅
  - `BufferStorageResponsePacket.java`: `writeLong`/`readLong`
  - `CommunalStorageResponsePacket.java`: `writeLong`/`readLong`
  - `PersonalStorageResponsePacket.java`: `writeLong`/`readLong`

- [x] **Step 5: View Model Updates** ✅
  - `TownResourceViewModelBuilder.java`: Handles long values
  - `TradingViewModelBuilder.java`: Uses `Map<Item, Long>`
  - `ITownDataProvider.java`: Interface updated to long

- [x] **Step 6: Build Verification** ✅
  - Build compiles successfully on all platforms
  - No remaining `Map<Item, Integer>` in core storage (only UI display conversion)

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

## 🔧 **PHASE 9: CONFIGURATION FILE MODERNIZATION**

**Problem:** Current `businesscraft.properties` uses Java Properties format which is non-standard for Minecraft mods.
**Goal:** Migrate to TOML format with proper sectioning and comments using NightConfig.
**Note:** CSV data files (items.csv, productions.csv, etc.) stay as-is for Excel compatibility.

### **9.1 Current State**

| File | Format | Status |
|------|--------|--------|
| `businesscraft.properties` | Java Properties | Replace with TOML |
| `ConfigLoader.java` | Manual parsing | Update to use NightConfig |
| `ConfigurationService.java` | File watcher | Keep for hot-reload |
| `*.csv` files | CSV | Keep as-is |

### **9.2 Dependencies**

| Library | Status | Notes |
|---------|--------|-------|
| `NightConfig` | **Bundled with Forge** | Same library Forge uses internally |
| `toml4j` | Remove from build.gradle | Replaced by NightConfig |

**Forge:** NightConfig is already bundled - no dependency needed.
**Fabric:** Add `com.electronwill.night-config:toml:3.8.1` to fabric/build.gradle.

Source: [NightConfig GitHub](https://github.com/TheElectronWill/night-config)

### **9.3 Proposed TOML Structure**

```toml
# BusinessCraft Configuration

[general]
    # Minimum distance between towns in blocks
    minDistanceBetweenTowns = 100
    # Default starting population for new towns
    defaultStartingPopulation = 5
    # List of random town names
    townNames = ["Riverside", "Hillcrest", "Meadowbrook", "Oakville"]

[vehicles]
    # Enable Create mod train integration
    enableCreateTrains = true
    # Enable minecart detection for tourists
    enableMinecarts = true
    # Search radius for vehicle detection (blocks)
    vehicleSearchRadius = 3
    # Minecart stop velocity threshold
    minecartStopThreshold = 0.001

[tourists]
    # Minimum population required to spawn tourists
    minPopForTourists = 5
    # Maximum tourists per town
    maxTouristsPerTown = 1000
    # Population required per tourist slot
    populationPerTourist = 5
    # Maximum population-based tourists
    maxPopBasedTourists = 20
    # Tourist expiry time in minutes (0 = never expire)
    touristExpiryMinutes = 120.0
    # Enable tourist expiry system
    enableTouristExpiry = true
    # Notify origin town when tourist departs
    notifyOnTouristDeparture = true

[economy]
    # Meters of travel per emerald earned
    metersPerEmerald = 50
    # Currency item for trading
    currencyItem = "minecraft:emerald"

[milestones]
    # Enable distance milestone rewards
    enabled = true

    # Milestone rewards - distance in meters, items as "modid:item:count"
    [[milestones.rewards]]
        distance = 10
        items = ["minecraft:bread:1", "minecraft:experience_bottle:2"]

[contracts]
    # Auction duration in minutes
    auctionDurationMinutes = 1.0
    # Courier acceptance window in minutes
    courierAcceptanceMinutes = 2.0
    # Courier delivery time per meter (minutes)
    courierDeliveryMinutesPerMeter = 0.05
    # Snail mail delivery time per meter (minutes)
    snailMailDeliveryMinutesPerMeter = 0.1

[production]
    # Enable automatic production system
    enabled = true
    # Ticks between production cycles
    tickInterval = 100
    # Daily tick interval for consumption
    dailyTickInterval = 24000
    # Minimum stock percentage before buying
    minStockPercent = 60
    # Excess stock percentage for selling
    excessStockPercent = 80

[trading]
    # Enable trading system
    enabled = true
    # Ticks between trading cycles
    tickInterval = 60
    # Restock rate multiplier
    restockRate = 0.5
    # Default max stock for items
    defaultMaxStock = 1000.0

[display]
    # Timezone for time display (UTC, SYSTEM, or timezone ID like America/New_York)
    timezone = "UTC"

[player]
    # Enable player tracking system
    playerTracking = true
    # Show town boundary entry/exit messages
    townBoundaryMessages = true
```

### **9.4 Implementation Plan** ✅ **COMPLETE**

- [x] **Step 1: Update build.gradle**
  - Removed toml4j from common/build.gradle
  - Added NightConfig to fabric/build.gradle (Forge has it bundled)

- [x] **Step 2: Update ConfigLoader.java**
  - Replaced `Properties` with NightConfig `CommentedFileConfig`
  - Uses `config.getOrElse("section.key", default)` for reading
  - Uses `config.set("section.key", value)` for writing
  - Kept static field pattern for easy access

- [x] **Step 3: Create default businesscraft.toml**
  - Created at `assets/businesscraft/config/businesscraft.toml`
  - Includes comments explaining each option
  - Auto-copied to config dir on first run

- [x] **Step 4: Delete old .properties handling**
  - Removed old `businesscraft.properties` files
  - Clean break, no migration

- [x] **Step 5: Keep hot-reload**
  - ConfigurationService still watches files
  - Updated to watch `businesscraft.toml`

### **9.5 Files to Modify**

| File | Action |
|------|--------|
| `common/build.gradle` | Remove toml4j dependency |
| `fabric/build.gradle` | Add NightConfig dependency |
| `ConfigLoader.java` | Replace Properties → NightConfig |
| `businesscraft.toml` | NEW - Default config with comments |
| `businesscraft.properties` | DELETE |

### **9.6 NightConfig Usage Example**

```java
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

// Loading with comments preserved
CommentedFileConfig config = CommentedFileConfig.builder(configFile)
    .defaultResource("/assets/businesscraft/config/businesscraft.toml")
    .autosave()
    .build();
config.load();

// Reading values
enableCreateTrains = config.getOrElse("vehicles.enableCreateTrains", true);
maxTouristsPerTown = config.<Integer>getOrElse("tourists.maxTouristsPerTown", 1000);
townNames = config.getOrElse("general.townNames", List.of("Riverside", "Hillcrest"));

// Writing values
config.set("vehicles.enableCreateTrains", enableCreateTrains);
config.setComment("vehicles.enableCreateTrains", "Enable Create mod train integration");

// Save happens automatically with autosave(), or call:
config.save();
config.close();
```

### **9.7 Summary**

| Aspect | Before | After |
|--------|--------|-------|
| **Format** | `.properties` (flat) | `.toml` (sectioned) |
| **Library** | Java Properties | NightConfig (bundled with Forge) |
| **New Dependencies** | - | Fabric only: night-config:toml |
| **Sections** | None | Yes |
| **Comments** | Lost on save | Preserved |
| **Hot Reload** | ✅ | ✅ |
| **Default Resource** | Manual copy | Built-in support |
| **Autosave** | Manual | Built-in support |

---

## 📉 **PHASE 10: GPI SUPPLY PRESSURE MECHANISM**

**Problem:** With trades-only price discovery, oversupplied items (like wheat) stay stuck at ~1.0 GPI. No downward pressure when supply exceeds demand.

**Previous Approach:** Failed auctions dropped GPI by 20% → too aggressive, caused death spiral.

**New Approach:** Failed auctions drop price by small amount (2-5%). Simple.

### **10.1 Simple Solution**

```
On failed auction (no bids):
  dropRate = 0.05 (5%)
  minPrice = 0.0001 (floor: 10,000 items = 1 emerald)

  newPrice = max(currentPrice * (1 - dropRate), minPrice)
```

**Why this works:**
- 5% is much gentler than 20%
- Floor at 0.0001 allows very cheap bulk items (sticks, dirt, etc.)
- When prices get low enough, someone WILL buy → price stabilizes
- Successful trades still pull price toward trade price (existing 10% learning rate)

### **10.2 Implementation Plan** ✅ **COMPLETE**

- [x] **Step 1: Add `recordFailedAuction()` to GlobalMarket**
  - Added method with 5% drop rate constant
  - Logs price changes for debugging

- [x] **Step 2: Update MIN_PRICE in GlobalMarket**
  - Changed from 0.001f to 0.0001f (10,000 items per emerald floor)

- [x] **Step 3: Call from ContractBoard.closeAuctions()**
  - Added call in the "no bids" branch
  - Replaced old "trades-only" comment

- [ ] **Step 4: Add config option (optional - future)**
  ```toml
  [economy]
  # Price drop rate per failed auction (0.05 = 5%)
  failedAuctionDropRate = 0.05
  ```

### **10.3 Files to Modify**

| File | Changes |
|------|---------|
| `GlobalMarket.java` | Add `recordFailedAuction()`, lower `MIN_PRICE` to 0.0001f |
| `ContractBoard.java` | Call `recordFailedAuction()` in no-bids branch |

### **10.4 Expected Behavior**

| GPI | Items per Emerald | Example |
|-----|-------------------|---------|
| 1.0 | 1 | Diamonds, rare items |
| 0.1 | 10 | Common resources |
| 0.01 | 100 | Abundant items |
| 0.001 | 1,000 | Very common |
| 0.0001 | 10,000 | Sticks, dirt (floor) |

After ~50 failed auctions with no trades: 1.0 → 0.08 (still above floor)
After ~90 failed auctions: hits floor at 0.0001

---

## 🎛️ **PHASE 11: GLOBAL SYSTEM ENABLE/DISABLE CONFIG**

**Goal:** Add global config toggles for all major systems (tourists, production, research, contracts) so server admins can completely enable/disable functionality.

### **11.1 Current State Analysis**

| System | Global Config | Per-Town Toggle | Code Location |
|--------|---------------|-----------------|---------------|
| **Tourists** | ❌ MISSING | ✅ `isTouristSpawningEnabled()` | `TownInterfaceEntity.java:1034,1073` |
| **Production** | ✅ `productionEnabled` | ❌ None | `Town.java:158` |
| **Trading** | ✅ `tradingEnabled` | ❌ None | `Town.java:155` |
| **Contracts** | ❌ MISSING | ❌ None | `Town.java:161` (always ticks) |
| **Research/Upgrades** | ❌ MISSING | ❌ None | `Town.java:162` (always ticks) |

**How Current System Works:**
- **Per-Town Tourist Toggle**: `Town.touristSpawningEnabled` - checked in `TownInterfaceEntity` before spawning
- **Production/Trading**: Global config checked in `Town.tick()` - if disabled, component doesn't tick
- **Contracts/Research**: Always tick regardless of config - NO disable option

### **11.2 Proposed Config Structure**

```toml
[tourists]
    # Master switch - completely disables tourist spawning globally
    enabled = true
    # ... existing tourist settings ...

[contracts]
    # Enable contract system (auction, bidding, delivery)
    enabled = true
    # ... existing contract settings ...

[research]
    # Enable research/upgrade system
    enabled = true
```

### **11.3 Implementation Plan**

#### **Step 1: Add Config Fields to ConfigLoader.java**
- [ ] Add `touristSystemEnabled` field (default: true)
- [ ] Add `contractsEnabled` field (default: true)
- [ ] Add `researchEnabled` field (default: true)
- [ ] Load from TOML in `loadConfig()`
- [ ] Save to TOML in `saveConfig()`

#### **Step 2: Update businesscraft.toml**
- [ ] Add `enabled = true` to `[tourists]` section
- [ ] Add `enabled = true` to `[contracts]` section
- [ ] Add `[research]` section with `enabled = true`

#### **Step 3: Update Town.tick()**
- [ ] Add `ConfigLoader.contractsEnabled` check before `contracts.tick()`
- [ ] Add `ConfigLoader.researchEnabled` check before `upgrades.tick()`

#### **Step 4: Update TownInterfaceEntity Tourist Spawning**
- [ ] Add `ConfigLoader.touristSystemEnabled` check at lines 1034 and 1073
- [ ] Global switch takes precedence over per-town toggle

#### **Step 5: Update UI (Optional - show disabled state)**
- [ ] Consider showing "Disabled by Server" in UI when global toggle is off
- [ ] Grey out controls when system is globally disabled

### **11.4 Files to Modify**

| File | Changes |
|------|---------|
| `ConfigLoader.java` | Add 3 new boolean fields + load/save logic |
| `businesscraft.toml` | Add `enabled` to tourists/contracts, add `[research]` section |
| `Town.java:161-162` | Add config checks for contracts/research |
| `TownInterfaceEntity.java:1034,1073` | Add `touristSystemEnabled` check |

### **11.5 Behavior Matrix**

| Global Config | Per-Town Toggle | Result |
|---------------|-----------------|--------|
| `enabled=true` | `enabled=true` | ✅ System active |
| `enabled=true` | `enabled=false` | ❌ System inactive (per-town) |
| `enabled=false` | `enabled=true` | ❌ System inactive (global override) |
| `enabled=false` | `enabled=false` | ❌ System inactive |

### **11.6 Estimated Effort**

- **Step 1-3**: ~30 mins - Config changes, straightforward
- **Step 4**: ~15 mins - Add checks to tourist spawning
- **Step 5**: ~1-2 hours (optional) - UI changes to show disabled state
- **Total**: ~45 mins minimum, ~2.5 hours with UI polish

---
