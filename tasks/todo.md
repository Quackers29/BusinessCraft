# BusinessCraft - Current Tasks and Future Tasks

## 🎯 **IMMEDIATE TASKS** (Do not remove header)

### **TownBlock Cleanup (Pre-Forge/Fabric Migration)**
Priority: HIGH - Clean up legacy artifacts before platform migration

- [ ] **Phase 1: Rename Block Entity System**
  - [ ] Rename `TownBlockEntity.java` → `TownInterfaceEntity.java` 
  - [ ] Update all imports across codebase
  - [ ] Update registration in `ModBlockEntities.java`
  - [ ] Update all method names: `getTownBlockEntity()` → `getTownInterfaceEntity()`
  - [ ] Update all variable names: `townBlock` → `townInterface`

- [ ] **Phase 2: Remove TownBlock Registration**  
  - [ ] Remove `TOWN_BLOCK` from `ModBlocks.java`
  - [ ] Remove TownBlock from block entity registration
  - [ ] Remove `TownBlock.java` class entirely
  - [ ] Update debug logging references

- [ ] **Phase 3: Clean Menu System**
  - [ ] Remove `TownBlockMenu.java` class
  - [ ] Remove `TOWN_BLOCK` menu registration from `ModMenuTypes.java`
  - [ ] Update all menu references to use `TownInterfaceMenu`
  - [ ] Clean up any menu-related imports

- [ ] **Phase 4: Asset Cleanup**
  - [ ] Remove all `town_block.*` asset files (blockstates, models, textures)
  - [ ] Remove `town_block.json` recipe and loot table
  - [ ] Update language file to remove town_block entries
  - [ ] Verify no broken texture/model references

- [ ] **Phase 5: Code Reference Cleanup**
  - [ ] Search and replace remaining "TownBlock" references in comments/strings
  - [ ] Update debug logging class names and messages
  - [ ] Clean up any remaining imports
  - [ ] Update documentation and comments

- [ ] **Phase 6: Testing & Verification**
  - [ ] Test town interface block placement and functionality
  - [ ] Verify UI opens correctly with new menu system  
  - [ ] Test platform management functionality
  - [ ] Check for any compilation errors or broken references
  - [ ] Test in-game to ensure no crashes or missing textures

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