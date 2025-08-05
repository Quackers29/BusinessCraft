# BusinessCraft - Current Tasks and Future Tasks

## 🎯 **IMMEDIATE TASKS** (Do not remove header)

### **Forge + Fabric Compatibility Migration**
Priority: CRITICAL - Transform mod from Forge-only to multi-platform architecture

#### **Phase 1: Platform Abstraction Foundation (COMPLETED ✅)**
- [x] **1.1 Establish Working Forge Baseline**
  - [x] Verify current Forge build compiles and runs
  - [x] Confirm all systems work (registration, events, networking)
  - [x] Establish reliable development environment

- [x] **1.2 Create Platform Services Interface Layer**
  - [x] Create `com.yourdomain.businesscraft.platform` package
  - [x] Define `PlatformHelper` interface for loader-specific operations
  - [x] Create `RegistryHelper` interface for cross-platform registration
  - [x] Create `NetworkHelper` interface for networking abstraction
  - [x] Create `EventHelper` interface for event system abstraction

- [x] **1.3 Implement Forge Platform Abstraction**
  - [x] Create `ForgePlatformHelper` with mod loading and environment detection
  - [x] Create `ForgeRegistryHelper` with DeferredRegister integration
  - [x] Create `ForgeNetworkHelper` with SimpleChannel networking
  - [x] Create `ForgeEventHelper` with MinecraftForge event bus integration
  - [x] Create `PlatformServices` provider for accessing platform implementations

- [x] **1.4 Integrate Platform Abstraction into Registration Systems**
  - [x] Abstract `ModBlocks` registration (dual Forge/Platform approach)
  - [x] Abstract `ModBlockEntities` registration with proper supplier handling
  - [x] Abstract `ModEntityTypes` registration including attribute registration
  - [x] Abstract `ModMenuTypes` registration with IForgeMenuType compatibility
  - [x] Update `BusinessCraft` main class to initialize platform registrations
  - [x] Test compilation and runtime compatibility with existing Forge build

#### **Phase 2: Core Mod Architecture**
- [ ] **2.1 Create Platform Abstraction Layer**
  - [ ] Move `BusinessCraft.java` to common module
  - [ ] Create platform-specific mod entry points
  - [ ] Abstract mod initialization logic
  - [ ] Create platform service interfaces

- [ ] **2.2 Registration System Overhaul**
  - [ ] Convert `ModBlocks.java` to Architectury Registry API
  - [ ] Convert `ModBlockEntities.java` to platform-agnostic registration
  - [ ] Convert `ModEntityTypes.java` to cross-platform registration
  - [ ] Convert `ModMenuTypes.java` to platform-agnostic registration
  - [ ] Test registration works on both platforms

#### **Phase 3: Event System Migration**
- [ ] **3.1 Abstract Event Handling**
  - [ ] Convert `ModEvents.java` to platform-agnostic events
  - [ ] Migrate client events (`ClientSetup.java`, `ClientModEvents.java`)
  - [ ] Abstract platform-specific event handlers
  - [ ] Create event registration interfaces
  - [ ] Test event functionality on both platforms

#### **Phase 4: Network System Migration**
- [ ] **4.1 Convert Network Architecture**
  - [ ] Replace `ModMessages.java` with Architectury Networking
  - [ ] Convert 25 packet types to platform-agnostic format
  - [ ] Update packet serialization/deserialization
  - [ ] Abstract client/server packet handling
  - [ ] Test networking on both platforms

#### **Phase 5: Client-Side Rendering**
- [ ] **5.1 Migrate Rendering Systems**
  - [ ] Convert entity renderers to platform-agnostic
  - [ ] Migrate client-side initialization
  - [ ] Abstract rendering registration
  - [ ] Update visualization system for cross-platform
  - [ ] Test rendering on both platforms

#### **Phase 6: Configuration & Final Integration**
- [ ] **6.1 Platform-Specific Metadata**
  - [ ] Create `fabric.mod.json` for Fabric
  - [ ] Keep `mods.toml` for Forge
  - [ ] Update configuration loading for both platforms
  - [ ] Create platform-specific resource packs if needed

- [ ] **6.2 Testing & Verification**
  - [ ] Test full functionality on Forge
  - [ ] Test full functionality on Fabric  
  - [ ] Verify feature parity between platforms
  - [ ] Test mod loading and initialization
  - [ ] Performance testing on both platforms

#### **Phase 7: Documentation & Cleanup**
- [ ] **7.1 Update Documentation**
  - [ ] Update CLAUDE.md with new architecture
  - [ ] Create platform-specific build instructions
  - [ ] Document platform differences
  - [ ] Update development guidelines

### 🎯 **MIGRATION STRATEGY**
- **Approach**: Incremental migration maintaining working Forge build until Fabric is ready
- **Testing**: Each phase must compile and run on Forge before proceeding
- **Architecture**: Common module contains business logic, platform modules handle loader-specific code
- **Compatibility**: Maintain full feature parity between Forge and Fabric versions

## 🎯 **COMPLETED TASKS** ✅

## 🎯 **FUTURE TASKS**

#### **Phase 3: UI Navigation and Controls**
- [ ] **3.2 Add Filtering and Sorting**
  - Filter by source type (All, Milestones, Tourist Payments, etc.)
  - Sort by timestamp (newest/oldest first)
  - Filter by claim status (unclaimed, claimed, expired)
  - Add search functionality for large reward lists

- [ ] **3.3 Implement Bulk Operations**
  - "Claim All" button with smart inventory management
  - "Claim All [Source Type]" for specific reward categories
  - Bulk expiration cleanup for old rewards
  - Select multiple rewards for batch claiming

- [ ] **3.4 Add Status Indicators**
  - Show total unclaimed rewards count
  - Display reward expiration warnings
  - Add visual indicators for new rewards since last visit
  - Include town economic summary (total rewards earned, claimed, etc.)

#### **Phase 4: Backend Integration (Leveraging Existing Architecture)**
- [ ] **4.1 Update Network Packets (Using Existing Patterns)**
  - Extend existing storage packet system for payment board data
  - Use `BaseBlockEntityPacket` pattern for reward synchronization
  - Add claim request/response packets following existing packet structure
  - Leverage existing `ModMessages` registration system

- [ ] **4.2 Remove Personal Storage System (Clean Removal)**
  - Remove personal storage from `StandardTabContent` configurations
  - Remove personal storage methods from `Town.java`
  - Clean up personal storage packets in network/packets/storage/
  - Remove personal storage references from UI components

- [ ] **4.3 Create PaymentBoardMenu Container**
  - Create new menu class extending `AbstractContainerMenu` for three-section layout
  - **Top Section**: Payment board data (no slots, pure UI)
  - **Middle Section**: 2x9 Payment Buffer slots (using `ItemStackHandler`)
  - **Bottom Section**: Standard player inventory slots (36 + 9 hotbar)
  - Handle slot interactions: buffer ↔ player inventory, hopper automation

#### **Phase 5: Enhanced Features**
- [ ] **5.1 Enhance Hopper Integration**
  - Ensure Payment Buffer (2x9) works seamlessly with hoppers underneath
  - Add auto-claim settings: automatically claim rewards to buffer
  - Implement smart claiming: prefer "Claim" to inventory, fallback to buffer
  - Add configuration for auto-claim behavior per reward type

- [ ] **5.2 Implement Notification System**
  - Send notifications when new rewards are available
  - Add sound effects for successful claims
  - Include particle effects for milestone reward notifications
  - Create notification preferences (on/off per source type)

- [ ] **5.3 Add Configuration Options**
  - Configurable reward expiration times
  - Toggle for auto-claim functionality
  - Hopper output settings (enabled/disabled)
  - Maximum stored rewards per town

#### **Phase 6: Testing and Polish**
- [ ] **6.1 Comprehensive Testing**
  - Test milestone reward delivery to payment board
  - Test tourist payment processing
  - Verify claim functionality with full/empty inventories
  - Test hopper integration and automation

- [ ] **6.2 Performance Optimization**
  - Optimize reward list rendering for large numbers of entries
  - Implement pagination for performance with 100+ rewards
  - Add caching for frequently accessed reward data
  - Optimize network packet sizes for reward synchronization

- [ ] **6.3 Final Integration**
  - Update all references from storage to payment board
  - Clean up unused storage-related code
  - Update debug logging for payment board operations
  - Verify compatibility with existing town systems

## Medium Priority Tasks

### 5. Remove /cleartowns Command
- [ ] Locate and remove /cleartowns command registration
- [ ] Remove associated command class/methods
- [ ] Update command documentation if needed

### 6. Remove Town Block
- [ ] Remove TownBlock class and related files
- [ ] Update block registration to exclude TownBlock
- [ ] Clean up any references to TownBlock in codebase
- [ ] Ensure Town Interface Block handles all functionality

### 7. Create Crafting Recipe System
- [ ] Design emerald circle pattern recipe for Town Interface Block
- [ ] Implement recipe registration system
- [ ] Add configuration option for recipe toggle (default: off)
- [ ] Test recipe in survival mode

### 8. Configure Recipe Toggleability
- [ ] Add config option for crafting recipe enable/disable
- [ ] Ensure recipe only loads when config is enabled
- [ ] Test configuration changes take effect

## Tasks Handled by User
- Adjust tourist clothing skin
- Design custom graphic for Town Interface Block
- Review of outputs