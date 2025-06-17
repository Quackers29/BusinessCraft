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

#### **Phase 2: Payment Board UI Implementation**
- [ ] **2.1 Create PaymentBoardScreen**
  - Replace existing StorageScreen with new PaymentBoardScreen
  - Design scrollable list interface for reward entries
  - Add claim buttons for each reward entry
  - Include reward source icons and timestamps

- [ ] **2.2 Implement Reward Display Components**
  - Create RewardEntryComponent for individual reward display
  - Add source type indicators (icons for milestones, tourist payments, etc.)
  - Show expiration timers for time-sensitive rewards
  - Display item rewards with quantities and icons

- [ ] **2.3 Add Claim Processing Logic**
  - Implement claim button functionality
  - Add inventory space checking before claiming
  - Support both direct-to-inventory and hopper-output options
  - Handle partial claims for large reward stacks

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

#### **Phase 4: Backend Integration**
- [ ] **4.1 Update Network Packets**
  - Create PaymentBoardPacket for reward data synchronization
  - Replace storage-related packets with payment board equivalents
  - Add claim request/response packet system
  - Ensure efficient data transfer for large reward lists

- [ ] **4.2 Remove Personal Storage System**
  - Remove personal storage UI components completely
  - Remove personal storage data structures from Town.java
  - Clean up related network packets and menu handlers
  - Remove personal storage references from documentation

- [ ] **4.3 Replace StorageMenu with PaymentBoardMenu**
  - Create new PaymentBoardMenu container class
  - Remove 2x9 slot limitations
  - Implement dynamic container sizing based on reward count
  - Add claim processing through container interaction

#### **Phase 5: Enhanced Features**
- [ ] **5.1 Add Hopper Integration**
  - Create 2x9 output buffer for hopper automation
  - Add "Claim to Hopper" option for each reward
  - Implement auto-claim settings for hopper output
  - Maintain compatibility with existing automation setups

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
    private RewardSource source;
    private List<ItemStack> rewards;
    private ClaimStatus status;
    private String eligibility; // "ALL" for now, expandable later
    private Map<String, Object> metadata; // Source-specific data
}
```

#### **Payment Board Integration Points**
- **Town.java**: Replace communalStorage with PaymentBoard
- **DistanceMilestoneHelper**: Create RewardEntry instead of direct storage
- **VisitorProcessingHelper**: Generate tourist payment entries
- **StorageScreen**: Replace with PaymentBoardScreen
- **Network Packets**: New payment board packet system

#### **UI Layout Design**
```
┌─────────────────────────────────────────────┐
│              Payment Board                  │
├─────────────────────────────────────────────┤
│ [Filter: All ▼] [Sort: Newest ▼] [Claim All]│
├─────────────────────────────────────────────┤
│ 🏆 Milestone (5m ago)        [Claim] [→🪣] │
│ └─ 2x Bread, 1x XP Bottle                  │
├─────────────────────────────────────────────┤
│ 🚂 Tourist Payment (1h ago)  [Claim] [→🪣] │
│ └─ 3x Emerald                               │
├─────────────────────────────────────────────┤
│ 📊 Summary: 12 unclaimed, 45 total rewards │
└─────────────────────────────────────────────┘
```

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