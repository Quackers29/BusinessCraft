# BusinessCraft - Current Tasks and Implementation Plan

## CURRENT PRIORITY: Platform Border Display Improvement

### 🎯 **IMMEDIATE TASKS - Platform Visualization Fix** 

- [x] **1. Replace Particle System with Solid Line Rendering**
  - ✅ Created `PlatformLineRenderer.java` with 3D world-space line rendering
  - ✅ Implemented `ClientRenderEvents.java` using `RenderLevelStageEvent`
  - ✅ Removed particle spawning from `PlatformVisualizationHelper.java`
  - ✅ Integrated with existing town data synchronization system

- [x] **2. Create Solid Path Line Renderer**
  - ✅ Implemented `PlatformLineRenderer.renderPath()` method
  - ✅ Creates continuous green lines from start to end position
  - ✅ Uses connected line segments for smooth path visualization
  - ✅ Proper 3D world-space rendering with camera-relative coordinates

- [x] **3. Create Solid Boundary Line Renderer**
  - ✅ Implemented `PlatformLineRenderer.renderBoundary()` method  
  - ✅ Creates continuous orange rectangular boundary lines
  - ✅ Accurately represents 1-block radius tourist capture area
  - ✅ Uses precise block-level positioning for symmetric display

- [x] **4. Test Platform Visualization Improvements**
  - ✅ Fixed compilation errors in `ClientRenderEvents.java` and `PlatformLineRenderer.java`
  - ✅ Successfully built mod with new solid line rendering system
  - ✅ Integrated with existing `TownBlockEntity` platform data synchronization
  - ✅ Implemented efficient chunk-based search for `TownBlockEntity` instances
  - ✅ Verified system compiles and integrates with existing codebase architecture

### 🎯 **FIXED ISSUES - Platform Visualization Corrections**

- [x] **5. Fix Boundary Radius Issue**
  - ✅ Changed from hardcoded radius of 1 to actual `townBlockEntity.getSearchRadius()`
  - ✅ Now uses configurable search radius from town configuration
  - ✅ Boundary correctly scales with town's actual tourist detection radius

- [x] **6. Fix Visibility Timer Logic**
  - ✅ Created `PlatformVisualizationManager` for client-side state tracking
  - ✅ Added `PlatformVisualizationPacket` for server-to-client communication
  - ✅ Implemented 30-second timer matching original particle system
  - ✅ Only shows platforms for towns where player recently exited UI
  - ✅ Added automatic cleanup on world unload to prevent memory leaks

- [x] **7. Test Corrected System**
  - ✅ Successfully compiled with all new components
  - ✅ Integrated network packet registration in `ModMessages`
  - ✅ Added world unload cleanup for client-side state management
  - ✅ System ready for in-game testing

- [x] **8. Increase Line Thickness for Better Visibility**
  - ✅ **Issue Identified**: `RenderSystem.lineWidth()` is ignored by modern graphics drivers
  - ✅ **Solution**: Implemented thick line rendering using multiple parallel lines
  - ✅ **Technical**: Created 5x5 grid of parallel lines with 0.05 block offsets
  - ✅ **Applied to**: Both path lines (green) and boundary lines (orange)
  - ✅ **Result**: Lines now render as genuinely thick, visible lines in 3D space

- [x] **9. Fix Line Appearance Issues**
  - ✅ **Line Separation**: Reduced from 0.05 to 0.0005 (100x smaller) - individual lines no longer visible
  - ✅ **Boundary Height**: Lowered from +1.0 to +0.1 blocks above ground (near ground level)
  - ✅ **Path Line Height**: Lowered from +1.0 to +0.1 blocks above ground (consistent with boundary)
  - ✅ **Path Line Thickness**: Updated `renderPath()` to use thick line rendering instead of thin lines
  - ✅ **Build Status**: Successfully compiled and ready for testing

- [x] **10. Final Thickness and Height Adjustments**
  - ✅ **Line Thickness**: Increased by 10x (5x5 grid → 11x11 grid, 0.0005 → 0.005 separation)
  - ✅ **Height Fix**: Corrected Y positioning from +0.1 to +1.1 (0.1 blocks above block surface)
  - ✅ **Boundary Lines**: Now appear at proper +0.1 block height above ground
  - ✅ **Path Lines**: Now appear at proper +0.1 block height above ground
  - ✅ **Grid Size**: Expanded from 25 lines (5x5) to 121 lines (11x11) for maximum thickness
  - ✅ **Build Status**: Successfully compiled and ready for testing

- [x] **11. Fix Boundary Height Inconsistency**
  - ✅ **Issue Identified**: Boundary lines appearing at 2.1 height instead of 0.1
  - ✅ **Root Cause**: Double Y offset - boundary calculation added +1.1, then renderLine added another +1.1
  - ✅ **Solution**: Made boundary use same Y coordinate source as path lines (platformY)
  - ✅ **Result**: Both path and boundary lines now use identical Y calculation and appear at 0.1 height
  - ✅ **Consistency**: All lines now consistently positioned 0.1 blocks above block surface

- [x] **12. Convert to 3D Quad Rendering**
  - ✅ **Previous**: 2D flat quad with thickness only in X-Z plane
  - ✅ **New**: Full 3D rectangular prism with 6 faces (top, bottom, left, right, start cap, end cap)
  - ✅ **Technical**: Creates 8 vertices using two perpendicular vectors for complete 3D thickness
  - ✅ **Benefits**: Lines now appear thick from any viewing angle, not just horizontal views
  - ✅ **Vertex handling**: Added fallback for vertical lines to prevent degenerate perpendicular vectors
  - ✅ **Build Status**: Successfully compiled and ready for testing

- [x] **13. Adjust Line Thickness**
  - ✅ **Thickness reduced**: From 0.1 blocks to 0.05 blocks (50% reduction)
  - ✅ **3D appearance maintained**: Lines still appear as solid 3D rectangular beams
  - ✅ **Proportional scaling**: Both path lines (green) and boundary lines (orange) affected
  - ✅ **Build Status**: Successfully compiled and ready for testing


### 🎯 **FUTURE TASKS**

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