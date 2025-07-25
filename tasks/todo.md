# BusinessCraft - Current Tasks and Future Tasks

## 🎯 **IMMEDIATE TASKS** (Do not remove header)

### Map View Boundary Circle Visualization Implementation

**Objective**: When clicking on a town in map view, display the boundary circle around that town on the map (in addition to the existing platform visualization).

**Current State Analysis**:
- **3D World**: ✅ Boundary circles already work perfectly when clicking towns
- **Map View**: ❌ Shows platform paths but missing boundary circles  
- **Architecture**: ✅ All boundary infrastructure exists and is production-ready

**Implementation Plan**:

#### 1. **Examine Current Map View Implementation** 
- [ ] Read `TownMapModal.java` to understand town selection and platform rendering
- [ ] Identify where `drawSelectedTownPlatforms()` renders platform paths
- [ ] Understand coordinate transformation between world coords and map coords
- [ ] Review how town data is accessed during map interaction

#### 2. **Integrate Boundary Circle Rendering**
- [ ] Add boundary circle drawing after platform path rendering  
- [ ] Use same coordinate transformation system as platform paths
- [ ] Apply consistent green color scheme matching 3D boundary visualization
- [ ] Calculate boundary radius using `TownBoundaryService.calculateBoundaryRadius()`
- [ ] Ensure boundary circle centers on town position

#### 3. **Implement Coordinate System Integration**
- [ ] Convert world boundary radius to map-space radius 
- [ ] Handle map zoom levels for proper circle scaling
- [ ] Ensure boundary circles scale correctly with map zoom
- [ ] Test boundary positioning accuracy across different map scales

#### 4. **Visual Consistency and Polish**
- [ ] Match boundary line style with 3D visualization (green, appropriate thickness)
- [ ] Handle boundary circle clipping at map edges gracefully
- [ ] Ensure boundary circles don't interfere with platform path visibility
- [ ] Test with multiple selected towns to ensure no visual conflicts

#### 5. **Testing and Validation**
- [ ] Test boundary circles appear correctly when towns are selected
- [ ] Verify boundary radius accuracy matches town population
- [ ] Test map zoom behavior with boundary circles
- [ ] Test with edge cases (towns near map boundaries, overlapping boundaries)
- [ ] Ensure performance is maintained with multiple boundary circles

**Technical Details**:
- **Target File**: `TownMapModal.java` (around line 728 in `drawSelectedTownPlatforms()`)
- **Data Source**: `TownBoundaryService.calculateBoundaryRadius(town.getPopulation())`  
- **Coordinate System**: Use existing map coordinate transformation logic
- **Visual Style**: Green circles matching 3D world visualization
- **Integration Point**: After platform path rendering, before ending selected town rendering

**Expected Outcome**: 
Complete visual consistency between 3D world and 2D map interfaces - when a town is selected, both platform paths AND boundary circles are displayed on the map, matching the behavior in the 3D world.

**✅ IMPLEMENTATION COMPLETED**

**What was implemented**:
- ✅ Added `drawSelectedTownBoundary()` method to `TownMapModal.java`
- ✅ Integrated boundary circle rendering after platform path rendering (line 823)
- ✅ Used same coordinate transformation system (`worldToScreenX/Z()`) as platform paths
- ✅ Applied bright green color (0xFF00FF00) matching 3D boundary visualization
- ✅ Calculated boundary radius using town population (1:1 ratio, same as TownBoundaryService)
- ✅ Ensured boundary circles center on town position
- ✅ Used 64-segment circles for smooth circular rendering
- ✅ Implemented proper clipping to map bounds using existing `drawClippedLine()` method
- ✅ Boundary circles scale correctly with map zoom levels
- ✅ Build successful - no compilation errors

**Technical Details**:
- **File Modified**: `TownMapModal.java` (added 43 lines)
- **Integration Point**: Line 823 in `drawSelectedTownPlatforms()` method  
- **Coordinate System**: Leverages existing `worldToScreenX()` and `worldToScreenZ()` transformations
- **Radius Calculation**: `selectedTown.population * zoomLevel * 0.1` (matches platform coordinate scaling)
- **Visual Consistency**: Same green color as 3D world boundaries
- **Performance**: Uses existing line clipping infrastructure for efficient rendering

**✅ UPDATED TO USE SERVER-CALCULATED BOUNDARY RADIUS**

**Server-Side Changes**:
- ✅ Extended `TownPlatformDataResponsePacket.TownInfo` to include `boundaryRadius` field
- ✅ Updated packet serialization to include boundary radius in network transmission  
- ✅ Modified `RequestTownPlatformDataPacket` handler to calculate boundary using `TownBoundaryService`
- ✅ Server now calculates boundary radius: `boundaryService.calculateBoundaryRadius(town)`

**Client-Side Changes**:
- ✅ Added `selectedTownInfo` field to store server-provided town data
- ✅ Modified `drawSelectedTownBoundary()` to use `selectedTownInfo.boundaryRadius` instead of client calculation
- ✅ Updated town selection logic to clear server data when switching towns
- ✅ Updated `refreshTownData()` to store server-provided boundary radius
- ✅ Added proper cleanup of server town info on deselection

**✅ ARCHITECTURE CLEANED UP - PROPER SEPARATION OF CONCERNS**

**Clean Architecture**:
- ✅ **Town Map Data**: Contains all town-level data including boundary radius (loaded once)
- ✅ **Platform Data**: Contains only platform-specific data (loaded per-town on demand)
- ✅ **Boundary visualization**: Uses boundary data from town map, not platform data

**✅ FINAL ARCHITECTURE - LIVE BOUNDARY CALCULATION**

**Perfect Architecture**:
- ✅ **Town Map Data**: Basic town info only (name, position, population, tourists)
- ✅ **Platform Data**: Platform info + LIVE boundary calculation when town clicked  
- ✅ **Boundary visualization**: Uses LIVE server-calculated boundary when town is selected

**Technical Flow**:
1. **Map Opens** → `RequestTownMapDataPacket` → Basic town data loaded (no boundaries cached)
2. **User clicks town** → `RequestTownPlatformDataPacket` → Server calculates LIVE boundary + platform data
3. **Client renders** → Boundary circle appears with live server data + platforms rendered
4. **Always fresh**: Boundary radius reflects current population, calculated on-demand

**Server-Side Live Calculation**:
- **TownMapDataHandler**: Basic town data only (fast, lightweight)
- **PlatformDataHandler**: Live boundary calculation using `TownBoundaryService.calculateBoundaryRadius(town)`
- **Perfect separation**: Map data = cache, Platform data = live + interactive

**✅ FINAL CLEANUP: UNIFIED BOUNDARY CALCULATION**

**Cleanup Completed**:
- ✅ **Single Source of Truth**: Both 3D and map view systems now use `town.getBoundaryRadius()`
- ✅ **Eliminated Redundancy**: Removed duplicate boundary calculation logic
- ✅ **Consistency**: Both systems use the same calculation method for guaranteed consistency

**Before Cleanup**:
- **3D Boundaries**: Used `town.getBoundaryRadius()` directly
- **Map Boundaries**: Used `TownBoundaryService.calculateBoundaryRadius(town)` (redundant)
- **Result**: Same calculation, different code paths

**After Cleanup**:
- **Both Systems**: Use `town.getBoundaryRadius()` as single source of truth
- **Unified Logic**: One calculation method, consistent results
- **Cleaner Code**: No duplicate boundary calculation logic

**Technical Changes**:
- **BoundarySyncRequestPacket**: Already used `town.getBoundaryRadius()` ✅
- **RequestTownPlatformDataPacket**: Changed from service to `town.getBoundaryRadius()` ✅
- **Result**: Both systems now unified on same calculation method

**Ready for Testing**: 
The implementation is complete with unified boundary calculation. Both 3D and map view boundaries use the same server-side calculation method. Run `./gradlew runClient` to test the boundary circles in map view.

### Town Boundary Messages & Map Visualization Implementation ✅ **COMPLETED**

**Status**: ✅ **FULLY IMPLEMENTED** 
- Map boundary visualization was already working perfectly
- Player boundary tracking system successfully implemented
- Both config properties added: `playerTracking` (master toggle) and `townBoundaryMessages`
- Ready for testing in `/home/az/project/BusinessCraft/run/config/businesscraft.properties`


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