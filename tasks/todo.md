# BusinessCraft - Current Tasks and Implementation Plan

## CURRENT PRIORITY: Architecture Improvements and Future Features

### 🎯 **IMMEDIATE TASKS**

#### Platform UI Redesign
- [x] Read CLAUDE.md and tasks/todo.md
- [x] Investigate current Platform UI implementation and compare to Resources tab and popups (Trade/Payment Board)

  **Investigation Summary:**
  - **Platform UI (PlatformManagementScreen & DestinationsScreen):** Older implementation extending vanilla Screen with manual rendering (GuiGraphics.fill/drawString), vanilla Buttons, hardcoded colors/positions, custom scrolling via scrollOffset. Does not use BC UI framework (components, layouts, builders, themes).
  - **Resources Tab:** Uses BC UI framework - extends BaseTownTab, employs BCFlowLayout, BCLabel, StandardTabContent (likely with UIGridBuilder for item grid), auto-refresh with change detection.
  - **Payment Board Popup:** Extends AbstractContainerScreen, integrates UIGridBuilder for rewards, custom section rendering but more aligned with BC styles.
  - **Trade Popup:** Similar to Payment Board, AbstractContainerScreen with custom rendering.
  - **Key Differences:** Platform UI lacks component-based design, layouts, theming, built-in scrolling; inconsistent style with main UI which uses BCScreenBuilder, BCComponents, StateBinding, etc.

- [ ] Proposed Refactor Plan (Awaiting User Approval):
  - [ ] Step 1: Create new PlatformManagementScreenV2 using BCScreenBuilder for structure, BCPanel with BCGridLayout/BCFlowLayout for layout.
  - [ ] Step 2: Replace platform list with BCScrollableListComponent, each entry as custom BCComponent with BCButtons for actions (toggle, destinations, set path, delete).
  - [ ] Step 3: Apply BCTheme for consistent colors/styling, remove hardcoded values.
  - [ ] Step 4: Implement scrolling and hovering using BC component features.
  - [ ] Step 5: Refactor DestinationsScreen similarly - use BCScrollableListComponent for town list, BCButtons for toggles.
  - [ ] Step 6: Update navigation (back buttons) with BCButton and ScreenNavigationHelper.
  - [ ] Step 7: Preserve packet integrations and data handling.
  - [ ] Step 8: Update openings (e.g., from Settings tab) to use new screens; deprecate old ones.
  - [ ] Step 9: Test for consistency with Resources tab style (e.g., auto-refresh if applicable).
  - [ ] Step 10: Clean up and remove old code if approved.

- [ ] Implement approved plan
- [ ] Test redesigned UI for functionality and style consistency
- [ ] Update documentation if needed

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