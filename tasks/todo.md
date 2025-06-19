# BusinessCraft Payment Board Implementation Plan

## CURRENT PRIORITY: Payment Board System

### 🎯 **PROJECT SCOPE**
Replace the existing communal storage UI with a comprehensive Payment Board system that acts as a town's economic hub for all reward claims.

### 📋 **IMPLEMENTATION PLAN**

#### **Phase 1: Core Payment Board Infrastructure**
- [ ] **1.1 Create RewardEntry Data Structure**
  - Define RewardEntry class with: UUID, timestamp, source type, item rewards, claim status
  - Add expiration system (configurable timeout for unclaimed rewards)
  - Include eligibility field (set to "ALL" for now, expandable for future player tracking)
  - Implement NBT serialization for persistence

- [ ] **1.2 Create TownPaymentBoard Component**
  - Replace communal storage Map with PaymentBoard system
  - Add methods: addReward(), getUnclaimedRewards(), claimReward(), expireOldRewards()
  - Integrate with existing Town.java save/load system
  - Maintain compatibility with existing reward delivery systems

- [ ] **1.3 Update Reward Delivery Systems**
  - Modify DistanceMilestoneHelper.deliverRewards() to create RewardEntry instead of direct storage
  - Update VisitorProcessingHelper to create tourist payment entries
  - Ensure all reward sources create proper RewardEntry objects
  - Add source tracking (MILESTONE, TOURIST_PAYMENT, etc.)

#### **Phase 2: Payment Board UI Implementation (Three-Section Layout)** ✅ COMPLETE

**Final Status:** All UI issues resolved with functional scrolling:
- ✅ Three-section layout working perfectly
- ✅ Static test data displaying (7 diverse reward items)  
- ✅ Inventory label positioning fixed (static positioning)
- ✅ Functional scrolling implemented (shows 3 rows, scrolls through 7 rewards)
- ✅ Text truncation working (12 char limit, no flipping)
- ✅ UI properly centered to screen width
- ✅ Interface simplified (single "Claim to Buffer" button)
- ✅ Framework ready for future settings integration
- ✅ Scrollbar positioned correctly without overlapping buttons
- ✅ HashMap order issue fixed (LinkedHashMap for stable text)
- ✅ Row positioning optimized to prevent border overlap
- [x] **2.1 Create PaymentBoardScreen with Three-Section Layout**
  - **Top Section**: Scrollable Payment Board (4-6 visible reward rows)
  - **Middle Section**: Payment Buffer (2x9 slots) replacing communal storage
  - **Bottom Section**: Standard player inventory + hotbar
  - Add "Back" button (top-right) returning to Resources tab

- [x] **2.2 Implement Payment Board Section using UIGridBuilder**
  - Use `UIGridBuilder` with vertical scrolling for reward list display
  - Configure 5 columns: Source Icon, Rewards, Time, "Claim", "→Buffer" 
  - Enable vertical scrolling with minimum 2 visible rows, scalable up to 4-6 rows
  - Add claim buttons: "Claim" (to inventory) and "→Buffer" (to 2x9 buffer)

- [x] **2.3 Implement Payment Buffer Section (2x9 Inventory)**
  - Repurpose existing 2x9 storage slot system as Payment Buffer
  - Use standard `ItemStackHandler` and slot rendering from existing storage
  - Maintain hopper compatibility underneath buffer slots
  - Allow manual drag-and-drop from buffer to player inventory

- [x] **2.4 Fix "Inventory" Label Positioning Issue** ✅
  - Fixed inventory label to use static positioning instead of following slots

- [x] **2.5 Implement Functional Scrolling System** ✅
  - Fixed UIGridBuilder scrolling implementation following StandardTabContent patterns
  - Grid creation preserves scroll state between data updates
  - Scrollbar positioned outside grid boundary to prevent button overlap

- [x] **2.6 Add Static Test Data** ✅
  - Added 7 diverse test rewards covering all reward sources
  - Included variety: emeralds, bread, gold, diamonds, tools, special items

- [x] **2.7 Optimize Layout and Spacing** ✅
  - Expanded screen width to 340px for better reward display
  - Centered UI sections for professional appearance
  - Optimized spacing and margins throughout

- [x] **2.8 Simplify Claim Interface** ✅
  - Single "Claim to Buffer" button workflow
  - 4-column layout: Source Icon, Rewards, Time, Claim
  - Framework ready for future settings integration

- [x] **2.9 Fix Text Display Issues** ✅
  - Fixed text flipping using LinkedHashMap for stable ordering
  - 12-character truncation prevents column overlap
  - Consistent reward display format

- [x] **2.10 Polish UI Positioning** ✅
  - Payment Board centered to screen width with even margins
  - Scrollbar positioned 20px outside grid to avoid overlap
  - Row positioning optimized to prevent border overlap

- [x] **2.11 Framework for Future Settings** ✅
  - Current implementation defaults to buffer for automation compatibility
  - Single "Claim" button designed to be compatible with future toggle
  - Framework ready: claimReward(UUID, boolean toBuffer) method exists

- [ ] **2.12 Enhance UIGridBuilder with Hover Tooltips** (Optional)
  - Add tooltip functionality for truncated text
  - Show full reward descriptions on hover

#### **Phase 3: UI Navigation and Controls**
- [ ] **3.1 Add Filtering and Sorting**
  - Filter by source type (All, Milestones, Tourist Payments, etc.)
  - Sort by timestamp (newest/oldest first)
  - Filter by claim status (unclaimed, claimed, expired)
  - Add search functionality for large reward lists

- [ ] **3.2 Implement Bulk Operations**
  - "Claim All" button with smart inventory management
  - "Claim All [Source Type]" for specific reward categories
  - Bulk expiration cleanup for old rewards
  - Select multiple rewards for batch claiming

- [ ] **3.3 Add Status Indicators**
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

### 🔧 **TECHNICAL SPECIFICATIONS**

#### **RewardEntry Data Structure**
```java
public class RewardEntry {
    private UUID id;
    private long timestamp;
    private long expirationTime;
    private RewardSource source; // MILESTONE, TOURIST_PAYMENT, TRADE, etc.
    private List<ItemStack> rewards;
    private ClaimStatus status; // UNCLAIMED, CLAIMED, EXPIRED
    private String eligibility; // "ALL" for now, expandable for player tracking
    private Map<String, Object> metadata; // Source-specific data
}
```

#### **UI Component Integration Plan**

**Option A: StandardTabContent Integration (Recommended)**
```java
// Replace Storage tab in town interface
StandardTabContent paymentTab = new StandardTabContent(width, height, 
    StandardTabContent.ContentType.CUSTOM_LIST, "Payment Board")
    .withCustomData(() -> buildRewardGridData());
```

**Option B: BCModalGridScreen Implementation**
```java
// Modal payment board screen
BCModalGridScreen<RewardEntry> paymentBoard = new BCModalGridScreen<>(
    Component.literal("Payment Board"), parentScreen, null)
    .withData(getUnclaimedRewards())
    .addColumn("Source", entry -> getSourceIcon(entry.getSource()))
    .addColumn("Rewards", entry -> formatRewardItems(entry.getRewards()))
    .addColumn("Time", entry -> formatTimeAgo(entry.getTimestamp()))
    .addColumn("Actions", entry -> "") // Claim/Hopper buttons
    .withRowClickHandler(this::handleRewardInteraction);
```

**UIGridBuilder Configuration**
```java
// Payment board grid with interactive buttons
UIGridBuilder grid = UIGridBuilder.create(x, y, width, height, 5) // 5 columns
    .enableVerticalScrolling(visibleRows, totalRewards)
    .setCustomRowHeight(24); // Larger rows for buttons

// For each reward entry:
grid.addItem(row, 0, getSourceIcon(entry)) // Source icon
    .addLabel(row, 1, formatRewards(entry)) // Reward description  
    .addLabel(row, 2, formatTimeAgo(entry)) // Timestamp
    .addButton(row, 3, "Claim", this::claimReward, SUCCESS_COLOR)
    .addButton(row, 4, "→🪣", this::claimToHopper, INFO_COLOR);
```

#### **Payment Board Integration Points**
- **Town.java**: Replace `Map<Item, Integer> communalStorage` with `TownPaymentBoard paymentBoard`
- **TownInterfaceScreen**: Replace storage tab with payment board tab
- **StandardTabContent**: Use CUSTOM_LIST type for reward display
- **DistanceMilestoneHelper**: Create RewardEntry instead of direct storage calls
- **VisitorProcessingHelper**: Generate tourist payment RewardEntry objects
- **Network Packets**: Extend existing storage packet patterns for payment board

#### **UI Layout Design (Three-Section Layout)**
```
┌─────────────────────────────────────────────────────────[Back]┐
│                    Payment Board                              │
├─────────────────────────────────────────────────────────────────┤
│ [All ▼] [Newest ▼] [Claim All] | Unclaimed: 12 | Total: 45    │
├───────┬─────────────────┬────────┬─────────┬─────────────────────┤
│Source │ Rewards         │ Time   │ Claim   │ To Buffer           │
├───────┼─────────────────┼────────┼─────────┼─────────────────────┤
│ 🏆    │ 2x Bread, 1x XP │ 5m ago │[Claim]  │ [→Buffer]          │
│ 🚂    │ 3x Emerald      │ 1h ago │[Claim]  │ [→Buffer]          │
│ 🏆    │ 1x Diamond      │ 2h ago │[Claim]  │ [→Buffer]          │
│       │ (scrollable, minimum 2 rows visible, scales to 4-6)  │
├─────────────────────────────────────────────────────────────────┤
│                   Payment Buffer (2x9)                         │
│ [slot][slot][slot][slot][slot][slot][slot][slot][slot]         │
│ [slot][slot][slot][slot][slot][slot][slot][slot][slot]         │
│                      ↑ hopper automation ↑                     │
├─────────────────────────────────────────────────────────────────┤
│                    Player Inventory                            │
│ [slot][slot][slot][slot][slot][slot][slot][slot][slot]         │
│ [slot][slot][slot][slot][slot][slot][slot][slot][slot]         │
│ [slot][slot][slot][slot][slot][slot][slot][slot][slot]         │
├─────────────────────────────────────────────────────────────────┤
│                      Player Hotbar                             │
│ [slot][slot][slot][slot][slot][slot][slot][slot][slot]         │
└─────────────────────────────────────────────────────────────────┘
```

**Claim Button Behavior:**
- **"Claim" Button**: Attempts to place items directly in player inventory
  - If inventory full → automatically places in Payment Buffer instead
  - Shows feedback message indicating where items went
- **"→Buffer" Button**: Always places items in Payment Buffer (2x9 slots)
  - Preferred for automation setups with hoppers
  - Guaranteed to work unless buffer is full

**Navigation:**
- **Back Button**: Returns to Resources tab of town interface
- **Screen Integration**: Replaces current storage screen completely

#### **Existing Component Reuse Benefits**
- **BCModalGridScreen**: Built-in scrolling, alternating rows, hover effects
- **UIGridBuilder**: Interactive buttons, item rendering, click handlers  
- **StandardTabContent**: Seamless tab integration with existing interface
- **TownInterfaceTheme**: Consistent colors and styling
- **BaseBlockEntityPacket**: Proven network synchronization patterns

### ⚠️ **IMPORTANT CONSIDERATIONS**

#### **Player Tracking Scope**
- **Current Implementation**: All rewards claimable by anyone ("ALL" eligibility)
- **Future Expansion**: Framework ready for player-specific rewards
- **Design Decision**: Eligibility system designed for easy future enhancement

#### **Backward Compatibility**
- Existing towns with communal storage will need migration
- Migration script to convert existing storage items to reward entries
- Preserve existing hopper automation functionality
- Maintain network packet compatibility during transition

#### **Performance Considerations**
- Reward list pagination for towns with many rewards
- Efficient NBT serialization for large reward datasets
- Client-side caching to reduce server requests
- Optimized rendering for real-time reward updates

### 🎯 **SUCCESS CRITERIA**
1. **Functional Replacement**: Payment board completely replaces communal storage
2. **Reward Processing**: All milestone and tourist rewards flow through payment board
3. **User Experience**: Intuitive claiming interface with proper feedback
4. **Automation Support**: Hopper integration maintains existing automation
5. **Performance**: System handles 100+ rewards without lag
6. **Expandability**: Architecture ready for future player tracking

### 📅 **ESTIMATED TIMELINE**
- **Phase 1-2**: Core infrastructure and basic UI (Foundation)
- **Phase 3-4**: Advanced UI features and backend integration (Core functionality)
- **Phase 5-6**: Enhanced features and polish (Production ready)

**Total estimated effort**: Comprehensive replacement of storage system with modern payment board architecture.

---

## Additional Tasks (Lower Priority)

### 3. Implement Minecraft Scoreboard System
- [ ] Create scoreboard objectives for town statistics
- [ ] Track tourists, population, visits, and other key metrics
- [ ] Set up automatic scoreboard updates when stats change
- [ ] Display scoreboard stats in-game

### 4. Create /bc Chat Commands
- [ ] Implement base /bc command structure
- [ ] Add subcommands for viewing town statistics
  - `/bc stats` - general town statistics
  - `/bc tourists` - tourist-related data
  - `/bc population` - population information
- [ ] Ensure proper permissions and error handling

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

## Review Section
(To be completed after implementation)

---
**Status**: Planning Phase - Payment Board System Design Complete
**Next Steps**: Begin Phase 1 implementation after plan approval