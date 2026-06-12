# BusinessCraft - Town Management Mod

**A Minecraft Forge / Fabric 1.20.1 mod featuring town management and tourism economy systems.**

## 🏙️ Core Systems Overview

### **Town Management System** (`Town.java`)
**✅ Full Town Management Implementation**

- **Multi-Tiered Storage Architecture**:
  - Town Resources: Bread, building materials, and consumables
  - Payment Board System: Structured reward claiming with configurable distance milestones
  - Personal Storage: Per-player UUID-based inventory (18 slots each)
  
- **Advanced Population Mechanics**:
  - Dynamic population growth from tourist visits
  - Tourist capacity management (population-driven, configurable, upgradeable)
  - Population-based platform allocation and spawning rates
  
- **Comprehensive Visit Tracking**:
  - Up to 50 visit history records with timestamps
  - Origin town tracking and visitor analytics
  - Persistent data across server restarts with NBT migration support

- **Service Layer Integration**:
  - Delegates complex business logic to `TownService`
  - Clean separation between data model and business operations

### **Block Entity System** (`TownInterfaceEntity.java`)
**✅ Advanced Modular Architecture**

- **14 Specialized Helper Classes**:
  - `ClientSyncHelper`: Advanced server-client synchronization
  - `PlatformManager`: Complete platform lifecycle management
  - `VisitorProcessingHelper`: Complex visitor detection algorithms
  - `TouristSpawningHelper`: Population-based spawning logic
  - `NBTDataHelper`: Robust save/load with data migration
  - `ContainerDataHelper`: Modular ContainerData field system
  - `TownBufferManager`: 18-slot buffer inventory management

- **Real-Time World Integration**:
  - Particle effect visualization with rate limiting
  - Smart resource processing (1 item/tick with sync)
  - Platform-based tourist spawning (up to 10 platforms/town)
  - Sophisticated client-side caching with auto-refresh

### **Tourist Entity System** (`TouristEntity.java`)
**✅ Advanced Villager-Based Implementation**

- **Intelligent Lifecycle Management**:
  - Configurable expiry system (prevents infinite accumulation)
  - Movement-based expiry (pauses when moving/riding vehicles)
  - Smart ride extension (resets timer on minecart/Create train boarding)

- **Professional Tourist Behavior**:
  - Origin/destination tracking with UUID persistence
  - Unemployed villager base with random appearance generation
  - Multiple breeding prevention safeguards
  - Death/departure notifications to origin towns

- **Advanced Movement Detection**:
  - Spawn position tracking distinguishes moved vs stationary
  - Integration with Create mod vehicle systems
  - Proper cleanup and accounting on entity removal

### **Platform Transportation System** (`Platform.java`)
**✅ Multi-Destination Platform Architecture**

- **Comprehensive Platform Management**:
  - Multi-destination support per platform
  - Complete NBT serialization for persistence
  - Path validation with start/end position tracking
  - Individual enable/disable controls

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

## 🏛️ Town Interface Block UI System

### **Main Town Interface Screen** (`TownInterfaceScreen.java`)
**✅ Complete 4-Tab Management Interface**

The Town Interface Block provides a well-designed tabbed interface for complete town management. Built with clean architecture using dependency injection and specialized managers.

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
**✅ Configuration Management Interface**

- **Platform Management Integration**:
  - **"Set Platforms" Button**: Opens `PlatformManagementScreenV2`
  - Direct navigation to platform configuration
  - Sound feedback on button interactions
  
- **Search Radius Controls**:
  - **Dynamic Radius Display**: Shows current search radius value
  - **Left-Click**: Increase radius
  - **Right-Click**: Decrease radius (via custom click handler)
  - **Real-time Updates**: Radius changes reflected immediately

- **Technical Architecture**:
  - `StandardTabContent` with `BUTTON_GRID` layout
  - `Consumer<Void>` button action handlers
  - Custom click handler for right-click functionality
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
Platform creation/deletion, path setting and reset, destination selection, enable/disable, path creation mode, search radius, refresh — e.g. `AddPlatformPacket`, `SetPlatformPathPacket`, `SetPlatformDestinationPacket`, `SetPlatformEnabledPacket`

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
- **Memory**: Recommended 4GB+ for optimal performance
- **Storage**: Persistent data saved with world files

### **Quick Start Guide**

1. **Establish Your First Town**
   ```
   → Place a Town Block in your desired location
   → Right-click to access the town interface
   → Configure your town name using the real-time editor
   ```

2. **Set Up Transportation Infrastructure**
   ```
   → Build platforms for tourist spawning and travel
   → Use the platform path creation system to connect destinations
   → Configure individual platform settings (enable/disable, destinations)
   ```

3. **Monitor Town Growth**
   ```
   → Access the Population tab to track growth metrics
   → Review visit history to understand tourist patterns
   → Claim milestone rewards from the Payment Board system
   ```

4. **Advanced Configuration**
   ```
   → Adjust tourist expiry times and population ratios
   → Configure minimum distances between towns
   → Set up Create mod integration for train transportation
   ```

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
