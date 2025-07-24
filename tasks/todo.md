# BusinessCraft - Current Tasks and Future Tasks

## 🎯 **IMMEDIATE TASKS** (Do not remove header)

### Population-Based Town Boundaries Implementation

#### **Phase 1: Data Model and Boundary System**
- [ ] **1.1 Add Town Boundary Methods to Town.java**
  - Add `getBoundaryRadius()` method that returns current population (1:1 ratio)
  - Add `getPosition()` accessor for town center coordinates
  - Add validation method `wouldOverlapWith(Town other)` for boundary overlap checks
  
- [ ] **1.2 Create TownBoundaryService.java**
  - Create new service class in `town/service/` package
  - Implement `calculateBoundaryRadius(Town town)` - returns town population as radius
  - Implement `checkTownPlacement(BlockPos newPos, Collection<Town> existingTowns)` - validates placement
  - Implement `getMinimumDistanceRequired(Town town1, Town town2)` - calculates required distance
  - Include logic: minDistance = town1.boundary + town2.boundary

#### **Phase 2: Update Town Placement Logic**
- [ ] **2.1 Modify TownManager.canPlaceTownAt()**
  - Replace static `minDistanceBetweenTowns` config usage with dynamic boundary calculation
  - For new towns: use `defaultStartingPopulation` (5) as initial boundary radius
  - For existing towns: use current population as boundary radius
  - Calculate required distance as: existingTown.boundary + newTown.boundary
  
- [ ] **2.2 Update Block Placement Error Messages**
  - Modify error messages in `TownBlock.java` and `TownInterfaceBlock.java`
  - Show dynamic distance requirements instead of static config value
  - Format: "Town requires X blocks distance (your boundary: Y + nearby town boundary: Z)"

#### **Phase 3: Boundary Growth Handling**
- [ ] **3.1 Add Population Change Event System**
  - Create `TownPopulationChangeEvent` class in events package
  - Trigger event when town population increases/decreases
  - Include old population, new population, and boundary radius changes
  
- [ ] **3.2 Implement Boundary Overlap Detection**
  - Add method to detect when growing boundaries would overlap existing towns
  - Create validation system to prevent population increases that would cause overlaps
  - Log warnings when towns approach boundary conflicts
  
- [ ] **3.3 Add Boundary Visualization**
  - **World Visualization**: Create `TownBoundaryVisualizationRenderer` extending `WorldVisualizationRenderer`
  - **Circle Rendering**: Implement circular boundary rendering in `BoundaryRenderer3D.java` (currently has placeholder)
  - **Integration**: Add `TYPE_TOWN_BOUNDARY = "town_boundary"` to `VisualizationManager`
  - **Trigger**: Show boundary circles when platform visualization is active (same trigger as platforms)
  - **Colors**: Green=safe spacing, yellow=close to overlap, red=would overlap
  - **Map Display**: Add boundary radius display to `TownMapModal.java` map view

#### **Phase 4: Configuration and Backward Compatibility**
- [ ] **4.1 Update Configuration System**
  - Keep `minDistanceBetweenTowns` config as fallback/minimum distance override
  - Add new config: `enablePopulationBasedBoundaries=true`
  - Add config: `minTownBoundaryRadius=5` (minimum boundary regardless of population)
  - Add config: `maxTownBoundaryRadius=50` (maximum boundary cap)
  
- [ ] **4.2 Migration Support**
  - Ensure existing towns work correctly with new boundary system
  - Validate existing town positions don't violate new boundary rules
  - Add debug logging for boundary calculations during world load

#### **Phase 5: Implementation Details**

**World Visualization Architecture:**
- Extend existing `VisualizationManager` system (supports TYPE_PLATFORM, TYPE_ROUTE, etc.)
- `BoundaryRenderer3D.java` already has circular boundary placeholder methods ready to implement
- Use same trigger system as platforms: when town interface is accessed, show both platform lines AND boundary circles
- Integration point: `PlatformVisualizationRenderer.showPlatformVisualization()` → also trigger boundary visualization

**Map Display Integration:**
- `TownMapModal.java` already displays town positions and data
- Add boundary radius as visual circles around town markers
- Show radius value in town info panel
- Color-code based on proximity to other towns

**Circle Rendering Implementation:**
```java
// In BoundaryRenderer3D.java - implement the placeholder method
public static void renderCircularBoundary(PoseStack poseStack, BlockPos center, int radius, Color color) {
    List<Vec3> points = createCircularBoundaryPoints(center, radius, 32); // 32 segments for smooth circle
    renderPolygonBoundary(poseStack, points, color, config);
}
```

#### **Phase 6: Testing and Validation**
- [ ] **6.1 Unit Testing**
  - Test boundary calculation with various population levels
  - Test town placement validation with multiple existing towns
  - Test edge cases: towns at world border, very high populations
  
- [ ] **6.2 Integration Testing**
  - Test population growth triggering boundary expansion
  - Test preventing town placement when boundaries would overlap
  - Verify backward compatibility with existing worlds
  - Test visualization triggers with existing platform system

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