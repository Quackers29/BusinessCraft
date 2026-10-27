# BusinessCraft - Town Management Mod

**A Minecraft Forge / Fabric 1.20.1 mod (0.9.0-beta) featuring town management and a tourism transport economy.**

## 🏙️ Core Systems Overview

### **Town Management System** (`Town.java`)
**✅ Full Town Management Implementation**

- **Multi-Tiered Storage Architecture**:
  - Town Resources: Bread, building materials, and consumables
  - Payment Board System: Structured reward claiming with configurable distance milestones
  - Personal Storage: Per-player UUID-based inventory (18 slots each)
  
- **Advanced Population Mechanics**:
  - Dynamic population growth from tourist visits
  - Tourist spawning gated by minimum population (`minPopForTourists`, default 5)
  - Up to **10 platforms per town**, each tied to a placed **Platform Interface** block inside the town boundary (not allocated from population)
  
- **Comprehensive Visit Tracking**:
  - Up to 50 visit history records with timestamps
  - Origin town tracking and visitor analytics
  - Persistent data across server restarts with NBT migration support

- **Service Layer Integration**:
  - Delegates complex business logic to `TownService`
  - Clean separation between data model and business operations

### **Town Centre & Platform Interface blocks**
**✅ Town Centre (`town_interface`, display name Town Centre) + craftable Platform Interface blocks**

- **Town Centre** defines the town, its name (from `general.townNames`), and circular boundary (border modifier or `general.defaultBorderRadius`, default 50). Placement must be at least **sum of both towns’ radii** from another Town Centre (100 blocks for two fresh towns). The boundary does **not** grow with population. In survival it **drops only when mined with a diamond-tier pickaxe** (vanilla `needs_diamond_tool` + `mineable/pickaxe` tags). **Platform Interface** blocks use a normal iron-tier pickaxe (any pickaxe).
- **Platform Interface** blocks are crafted and placed **inside** the boundary; each block is one platform. Breaking the block removes that platform. Settings (path, search radius, spread, enable, destinations) live on the Platform Interface screen; the Town Centre only shows a read-only platform list.
- **Town Centre block entity** (`TownInterfaceEntity.java`) — modular server-side town logic:
  - **14 Specialized Helper Classes**: `ClientSyncHelper`, `PlatformManager`, `VisitorProcessingHelper`, `TouristSpawningHelper`, `NBTDataHelper`, `ContainerDataHelper`, `TownBufferManager`
  - **Real-Time World Integration**: particle visualization, buffer processing, platform-based spawning (up to 10/town), client caching

### **Tourist Entity System** (`TouristEntity.java`)
**✅ Advanced Villager-Based Implementation**

- **Intelligent Lifecycle Management**:
  - Configurable expiry system (prevents infinite accumulation)
  - Movement-based expiry (pauses when moving/riding vehicles)
  - Smart ride extension (resets timer on minecart/Create train boarding)

- **Professional Tourist Behavior**:
  - **Tourists do not walk** in 0.9 (movement speed is effectively zero); they wait on the platform until a minecart or Create train carries them
  - **16 layered looks** (`data/businesscraft/tourist_looks`), personal names (name tag shows first name), distance ranks (Wanderer → Renowned; see `tourists.levelDistances` in config)
  - Origin/destination tracking with UUID persistence
  - Unemployed villager base with layered look rendering
  - Multiple breeding prevention safeguards
  - Death/departure notifications to origin towns

- **Advanced Movement Detection**:
  - Spawn position tracking distinguishes moved vs stationary
  - Integration with Create mod vehicle systems
  - Proper cleanup and accounting on entity removal

### **Platform Transportation System** (`Platform.java` + Platform Interface block)
**✅ Multi-Destination Platform Architecture**

- **Comprehensive Platform Management** (per Platform Interface block):
  - Multi-destination allow-list per platform (empty selection = any town)
  - Complete NBT serialization for persistence (platforms without a saved interface position are dropped on load)
  - Path set on the Platform Interface (two right-clicks); spawn requires **enabled** + complete path
  - Search radius (default 10, cap `platforms.maxSearchRadius` default 100), spread (≤ search radius; 0 = on the path line), enable toggle
  - Sign lamp lit only when linked and enabled; disabling stops spawns

- **Destination System**:
  - UUID-based town identification
  - "Any Town" universal destination support
  - Distance-based tourist allocation
  - Real-time platform visualization with particles

---

## 🎨 Production-Grade UI Framework

### **Component-Based Architecture** (11 directories, 40+ components)
**✅ Advanced UI System with Clean APIs**

#### **Foundation Components** (`ui/components/basic/`)
- `BCButton`: Advanced button with state management
- `BCLabel`: Dynamic text rendering with data binding  
- `BCPanel`: Container component with layout support
- `BCComponent`: Base class with event handling

#### **Advanced Containers** (`ui/components/containers/`)
- `BCTabPanel`: Professional tabbed interface system
- `SlotComponent`: Inventory slot integration
- `StandardTabContent`: Template-based tab content with 4 content types

#### **Display Components** (`ui/components/display/`)
- `ResourceListComponent`: Virtualized resource display with scrolling
- `VisitHistoryComponent`: Historical data viewer with pagination
- `BCScrollableComponent`: High-performance list virtualization

#### **Input Systems** (`ui/components/input/`)
- `BCEditBox`: Advanced text input with validation
- `BCToggleButton`: State-aware toggle controls
- `TownNameEditor`: Real-time town name editing

---

## 🏛️ Town Centre UI System

### **Main Town Centre Screen** (`TownInterfaceScreen.java`)
**✅ Complete 4-Tab Management Interface**

The **Town Centre** block provides a tabbed interface for town overview, resources, and settings. **Platform path, radius, spread, enable, and destinations are configured on each Platform Interface block**, not here (this screen only lists platform state). Built with clean architecture using dependency injection and specialized managers.

#### **Core Architecture**
- **15+ UI Managers**: `TownScreenDependencies`, `BottomButtonManager`, `TownTabController`, etc.
- **Coordination System**: `TownScreenDependencies` manages all functionality
- **Clean Event Handling**: Separation between UI events and game logic
- **Smart Caching**: `TownDataCacheManager` with auto-refresh

### **Tab System Overview**

#### **🔍 Overview Tab** (`OverviewTab.java`)
**✅ Town Summary Dashboard**

- **Primary Information Display**:
  - Town Name with real-time editing capabilities
  - Current Population count with dynamic updates
  - Active Tourist count with status indicators
  
- **Data Presentation**:
  - `StandardTabContent` with `LABEL_VALUE_GRID` layout
  - `LinkedHashMap` preserves display order
  - Auto-refresh on town data changes
  - Animated presentation with smooth transitions

#### **👥 Population Tab** (`PopulationTab.java`)
**🚧 Citizen Display (placeholder data)**

- **Citizen Display**:
  - **Custom List Layout**: Citizen information display (Name, Job, Level)
  - **Scrollable Interface**: Handles large populations efficiently
  - **Note**: currently shows placeholder citizen profiles — the job assignment system behind it is planned for a future release (see roadmaps)

- **Technical Features**:
  - `StandardTabContent` with `CUSTOM_LIST` content type
  - Dynamic data supplier with real-time updates

#### **📦 Resources Tab** (`ResourcesTab.java`)
**✅ Advanced Resource Management**

- **Comprehensive Resource Display**:
  - **Item List Visualization**: All town resources with quantities
  - **Auto-Refresh System**: Checks for changes every 20 ticks (1 second)
  - **Change Detection**: Monitors resource additions, removals, and quantity changes
  - **Force Refresh API**: External systems can trigger immediate updates

- **Technical Implementation**:
  - `StandardTabContent` with `ITEM_LIST` content type
  - `Map<Item, Integer>` data structure for resource tracking
  - Change detection with `hasResourcesChanged()` algorithm
  - Cache synchronization with `TownDataCacheManager`
  - Comprehensive debug logging with `DebugConfig.UI_RESOURCES_TAB`

- **Performance Features**:
  - Efficient diff algorithm for resource changes
  - Minimal UI updates (only when data actually changes)
  - Resource state tracking with `Map.copyOf()` for safety

#### **⚙️ Settings Tab** (`SettingsTab.java`)
**✅ Town-Level Settings (not platforms)**

- Town name editing and ranking display (no add/delete platform controls on the Town Centre)
- Tabs for production, research, trading, and contracts remain visible but are **locked off by default** in 0.9 (tourism + milestones on)

- **Technical Architecture**:
  - `StandardTabContent` with `BUTTON_GRID` layout
  - `Consumer<Void>` button action handlers
  - `LinkedHashMap` maintains button order

### **Specialized Screen System**

#### **💰 Payment Board Screen** (`PaymentBoardScreen.java`)
**✅ Reward Management Interface**

- **Professional Layout System**:
  - **Header Section**: Back button, title, auto-claim toggle
  - **Payment Board Section**: 3-row reward grid (300px width)
  - **Inventory Section**: Player inventory integration
  - **Optimized Margins**: Reduced padding prevents UI overlaps

- **Reward System Integration**:
  - `RewardEntry` and `ClaimStatus` data structures
  - `RewardSource` tracking for different reward types
  - Distance-based milestone rewards
  - Player-specific claim history

#### **🏪 Trade Screen** (`TradeScreen.java`)
**✅ Commerce Interface**
- Advanced trading system with inventory integration
- Real-time trade validation and processing

#### **📦 Storage Screen** (`StorageScreen.java`)
**✅ Inventory Management**
- Multi-tiered storage access (resources, communal, personal)
- 18-slot buffer storage system integration

### **Advanced UI Manager System**

#### **Modal Management** (`ui/managers/`)
**✅ 15+ UI Managers**

- **`VisitorModalManager`**: Visitor list display with 2-column layout
- **`TradeModalManager`**: Commerce modal interfaces
- **`StorageModalManager`**: Storage access modals
- **`UnifiedModalManager`**: Centralized modal state management
- **`ModalCoordinator`**: Inter-modal communication

#### **State Management Managers**
- **`TownDataCacheManager`**: Client-side caching with auto-refresh
- **`TownTabController`**: Tab switching and state preservation
- **`ButtonActionCoordinator`**: Centralized button action handling
- **`SearchRadiusManager`**: Radius control coordination

#### **Event System Managers**
- **`TownScreenEventHandler`**: UI event processing and delegation
- **`TownScreenRenderManager`**: Rendering pipeline management
- **`BottomButtonManager`**: Bottom button grid management

### **Advanced UI Features**

#### **Screen Builder System** (`BCScreenBuilder`)
```java
// Fluent API for rapid screen creation
BCScreenBuilder.create()
    .withTabs()
    .addTab("Overview", overviewContent)
    .addTab("Population", populationContent)
    .withTheme(TownInterfaceTheme.DEFAULT)
    .build();
```

#### **Layout Management System**
- `BCFlexLayout`: Flexible box layout with alignment
- `BCGridLayout`: Grid-based positioning system  
- `BCFlowLayout`: Dynamic flow-based arrangements

#### **Modal Dialog System**
- `BCModalGridScreen`: Data table display with sorting
- `BCModalInventoryScreen`: Inventory management modals
- `UnifiedModalManager`: Centralized modal state management

#### **State Management** (`ui/state/`)
- `StateBindingManager`: Sophisticated data binding system
- Real-time updates with automatic UI refresh
- Component state synchronization across screens

### **StandardTabContent System**
**✅ 4 Unified Content Types**

The `StandardTabContent` component provides consistent layouts across all tabs:

1. **`LABEL_VALUE_GRID`**: Key-value pair display (Overview Tab)
2. **`CUSTOM_LIST`**: Custom data list display (Population Tab)  
3. **`ITEM_LIST`**: Minecraft item display with quantities (Resources Tab)
4. **`BUTTON_GRID`**: Interactive button grid (Settings Tab)

Each content type optimizes rendering and interaction patterns for specific data presentation needs while maintaining UI consistency.

---

## 🌐 Network Communication System

### **~57-Packet System** (6 organized packages + view-model sync)
**✅ Complete Client-Server Communication**

#### **Platform Management** (`network/packets/platform/` - 9 packets)
Path setting and reset, destination selection, enable/disable, path creation mode, search radius and spread, refresh — e.g. `SetPlatformPathPacket`, `SetPlatformDestinationPacket`, `SetPlatformEnabledPacket`, `SetPlatformAreaPacket`

#### **Storage & Payment Board** (`network/packets/storage/` - 12 packets)
Communal/personal/buffer storage request+response pairs, payment board sync and claims, resource trading — e.g. `PaymentBoardClaimPacket`, `PersonalStoragePacket`, `BufferStorageResponsePacket`, `TradeResourcePacket`

#### **Town Management** (`network/packets/town/` - 2 packets)
`SetTownNamePacket`, `ToggleTouristSpawningPacket`

#### **UI & Contracts** (`network/packets/ui/` - ~24 packets)
Screen navigation, contract board (list/detail/bid/accept), town map data, leaderboard, boundary visualization, platform visualization — e.g. `OpenTownInterfacePacket`, `BidContractPacket`, `RequestTownMapDataPacket`, `LeaderboardDataResponsePacket`

#### **View-Model Sync** (`network/packets/` root - 6 packets)
Server-built UI view models pushed to the client: town interface, resources, production, trading, upgrades, market (`BaseViewModelSyncPacket` base class)

#### **Base Infrastructure & Debug** (`misc/`, `debug/`)
- `BaseBlockEntityPacket`: Foundation for block entity communication
- `PaymentResultPacket`, town debug data request/response

---

## 💾 Data Management & Persistence

### **Advanced Data Layer** (`town/data/` - 14 helper classes)
**✅ Well-Organized Data Management**

#### **Core Data Components**
- **`TownPaymentBoard`**: Structured reward system with claim tracking
  - 50+ configurable milestone rewards
  - Player-specific claim history
  - Distance-based reward scaling

- **`SlotBasedStorage`**: High-performance inventory system
  - 18-slot buffer storage per town
  - Type-safe item stack management
  - Automatic synchronization with clients

#### **Advanced Helper System**
- **`VisitorProcessingHelper`**: Complex visitor detection algorithms
- **`PlatformManager`**: Complete platform lifecycle management
- **`ClientSyncHelper`**: Advanced client-server synchronization
- **`ContainerDataHelper`**: Modular ContainerData field system
- **`TownDataCacheManager`**: Client-side caching with automatic refresh

#### **Persistence Features**
- **NBT Data Migration**: Handles legacy data format upgrades
- **Atomic Operations**: Transaction-safe data updates
- **Error Recovery**: Comprehensive error handling with `Result<T, E>` types
- **Resource Cleanup**: Proper lifecycle management on server stop/restart

---

## ⚙️ Configuration & Performance

### **Hot-Reload Configuration System**
**✅ Smart Configuration Management**

- **`ConfigurationService`**: File watching with automatic reload
- **27+ Configurable Parameters**:
  - Tourist expiry times and behavior
  - Population growth ratios and limits
  - Distance calculations and spawn rates
  - Vehicle integration settings
  - Performance tuning parameters

### **Performance Optimizations**

#### **Rate Limiting & Caching**
- Particle effect optimization with rate limiting
- Client-side caching significantly reduces server requests
- UI virtualization handles large data sets efficiently
- Smart update batching prevents excessive network traffic

#### **Memory Management**
- Proper resource cleanup on server lifecycle events
- Garbage collection optimization for entity management
- Efficient data structures for high-performance operations

---

## 🐛 Debug & Development System

### **Debug System** (`debug/DebugConfig.java`)
**✅ 25+ Debug Flags for Easy Troubleshooting**

#### **Component-Specific Debug Controls**
- `TOWN_BLOCK_ENTITY`: Block entity operations
- `NETWORK_PACKETS`: Packet transmission logging  
- `UI_MANAGERS`: UI event and state logging
- `VISITOR_PROCESSING`: Tourist processing pipeline
- `STORAGE_OPERATIONS`: Storage system debugging
- `PLATFORM_SYSTEM`: Platform management logging
- `TOURIST_ENTITY`: Entity behavior debugging

#### **Global Debug Features**
- `FORCE_ALL_DEBUG`: Enable comprehensive logging
- Debug overlay: F4 toggles town debug visualization
- Clean production logs: Debug-controlled logging system
- Performance metrics: Built-in performance monitoring

### **Development Tools**
- **Admin Commands**: `/cleartowns` for development testing
- **Hot Configuration**: Changes apply without server restart
- **Comprehensive Logging**: Throughout all systems for debugging
- **Error Reporting**: Structured error handling with context

---

## 🚀 Getting Started

### **Installation Requirements**
- **Minecraft**: 1.20.1
- **Loader**: Forge or Fabric (both fully supported)
- **Mod version**: 0.9.0-beta
- **Memory**: Recommended 4GB+ for optimal performance
- **Storage**: Persistent data saved with world files (no pre-alpha save back-compat promise)

### **How a town is set up**

1. **Craft and place a Town Centre** — recipe: 8 obsidian around 1 diamond (`OOO` / `O#O` / `OOO`). Place it at least **100 blocks** from another Town Centre if both use the default radius (50). The town name is chosen from config; there is no town owner. The Town Centre lists platforms; it does not add or remove them.
2. **Craft Platform Interface blocks** — recipe: rails and iron around redstone (`RIR` / `I#I` / `RIR`). Place them **inside** the town boundary (max 10 per town, at least **5 blocks** from another interface of the same town). Placement outside the boundary fails.
3. **On each Platform Interface** — set the spawn path (two right-clicks), search radius and spread, enable the platform, and pick destination towns (or leave none for “any town”). The sign lamp is on only when the platform is linked and enabled.
4. **Move tourists** — they **do not walk**; use minecarts or Create trains. Fare is linear: default **100 blocks per emerald** (`economy.metersPerEmerald`). Claim distance milestones from the Payment Board.
5. **Breaking blocks** — breaking a Platform Interface removes that platform; breaking the Town Centre deactivates loaded interfaces and dismisses tourists on those platforms.

### **Quick Start Guide**

1. **Establish your first town**
   ```
   → Craft and place a Town Centre (see recipe above)
   → Right-click for overview, resources, and town settings
   ```

2. **Set up transport**
   ```
   → Place Platform Interface blocks inside the boundary
   → On each interface: set path, radius/spread, enable, destinations
   → Build rails or Create routes; tourists need carts/trains
   ```

3. **Grow tourism**
   ```
   → New towns start at population 5, the default minimum for tourists
   → Watch visit history and claim Payment Board milestones
   ```

4. **Server tuning (optional)**
   ```
   → Edit config TOML: tourist expiry, milestones, locked subsystems
   → Production, research, trading, and contracts stay off unless enabled
   ```

---

## Showcase testbed (dev harness)

For automated two-town scenarios and feature checks in a throwaway client world, see **[tools/SHOWCASE-TESTBED.md](tools/SHOWCASE-TESTBED.md)**.

Quick start:

```bash
bash tools/showcase.sh testbed forge
bash tools/showcase-map-sweep.sh forge   # map modal across resolutions / GUI scales
```

Outputs land under **`showcase-output/`** (see [tools/SHOWCASE-TESTBED.md](tools/SHOWCASE-TESTBED.md)).

Related: `bash tools/showcase.sh two-towns forge` (boundary tests + all-views on town B).

---

## 🔗 Integration & Compatibility

### **Mod Integration Support**
- **Create Mod**: Full integration with train systems
  - Automatic tourist ride extension on train boarding
  - Smart vehicle detection and timer management
  - Railway-based tourist transportation

- **Economy Mods**: Designed for economy system integration
  - Extensible API through `ITownDataProvider`
  - Clean interfaces for external mod integration
  - Event-driven architecture supports mod interactions

### **API & Extension Points**
- **ITownDataProvider**: Standardized data access interface
- **Component-based UI**: Reusable components for mod extensions
- **Event system**: Clean integration points for external mods
- **Configuration API**: Hot-reload configuration support

---
