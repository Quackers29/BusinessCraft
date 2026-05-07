# Town Leaderboard Feature Implementation

## Overview
Add leaderboard view showing all towns with sortable stats. Replaces "Edit Details" button in Overview tab with "Leaderboard", moves "Edit Details" to Settings tab replacing "Reset Defaults".

## Architecture Analysis

### Available Components
- **BCModalGridScreen<T>**: Reusable modal with scrolling grid, row click handlers, auto-scaling
- **StandardTabContent**: Tab content system with button grids
- **UIGridBuilder**: Grid building with sorting support
- **TownManager.getAllTowns()**: Returns Map<UUID, Town> for all towns server-side

### Town Data Available
- **Town properties**: name, position (BlockPos), population, resources (emeralds for money)
- **Distance calculation**: Use BlockPos.distSqr() or BlockPos.distManhattan()
- **Leader metrics**: Population (economy.getPopulation()), Money (economy.getResourceCount(Items.EMERALD))

### Network Requirements
- Need client-side data sync packet for all towns data
- Pattern: Similar to existing ContractListSyncPacket, BoundarySyncResponsePacket
- Data transfer: TownLeaderboardData record with: UUID, name, position, population, money

## Implementation Tasks

### Phase 1: Data Layer & Network
- [ ] **1.1** Create `TownLeaderboardData` record class
  - Fields: UUID townId, String name, BlockPos position, int population, long money
  - Location: `common/src/main/java/com/quackers29/businesscraft/town/data/TownLeaderboardData.java`

- [ ] **1.2** Create server packet: `LeaderboardDataRequestPacket`
  - Client → Server: Request all towns data
  - Location: `common/src/main/java/com/quackers29/businesscraft/network/packets/ui/LeaderboardDataRequestPacket.java`

- [ ] **1.3** Create response packet: `LeaderboardDataResponsePacket`
  - Server → Client: List<TownLeaderboardData>
  - Server handler: Query TownManager.getAllTowns(), transform to TownLeaderboardData list
  - Location: `common/src/main/java/com/quackers29/businesscraft/network/packets/ui/LeaderboardDataResponsePacket.java`

- [ ] **1.4** Register packets in ModMessages
  - Add packet registration for both request/response packets

### Phase 2: Leaderboard Screen
- [ ] **2.1** Create `TownLeaderboardScreen` extending BCModalGridScreen<TownLeaderboardData>
  - Location: `common/src/main/java/com/quackers29/businesscraft/ui/screens/town/TownLeaderboardScreen.java`
  - Constructor: Takes current town position for distance calc
  - Columns:
    - "Town Name" - data.name()
    - "Distance" - calculate from current town position (format: "123m" or "1.2km")
    - "Score" - population OR money based on sort mode
  - Panel size: 80% width, 70% height for better visibility
  - Row height: 18px for readability
  - Colors: Use TownInterfaceTheme constants

- [ ] **2.2** Implement sorting functionality
  - Dropdown/toggle button for sort mode: "Distance" / "Population" / "Money"
  - Sort logic: Collections.sort() on data list
  - Default sort: Distance (nearest first)

- [ ] **2.3** Implement row click handler
  - Opens detail modal: `TownDetailScreen` (new)
  - Shows: name, distance, biome, population, money, coordinates
  - "Visit" button placeholder (future navigation feature)
  - "Back to Leaderboard" button

- [ ] **2.4** Screen scaling and responsiveness
  - Use BCModalGridScreen's withPanelSize() for dynamic sizing
  - Calculate panel dimensions from minecraft.getWindow().getGuiScaledWidth/Height()
  - Test on different GUI scales

### Phase 3: Town Detail Screen
- [ ] **3.1** Create `TownDetailScreen` as nested modal
  - Extends BCModalGridScreen or custom modal
  - Location: `common/src/main/java/com/quackers29/businesscraft/ui/screens/town/TownDetailScreen.java`
  - Display fields (label-value grid):
    - Town Name
    - Distance from your town
    - Coordinates (X, Y, Z)
    - Biome
    - Population
    - Money (emeralds)
  - Buttons: "Back to Leaderboard"

### Phase 4: Button Reorganization
- [ ] **4.1** Modify BottomButtonManager.configureOverviewButtons()
  - Change "Edit Details" → "Leaderboard"
  - Handler: onViewLeaderboard() → Opens TownLeaderboardScreen
  - Keep "Map View" button unchanged
  - Location: Line 168-174 in BottomButtonManager.java

- [ ] **4.2** Add onViewLeaderboard() to ButtonActionHandler interface
  - Location: BottomButtonManager.java line 16

- [ ] **4.3** Modify BottomButtonManager.configureSettingsButtons()
  - Change "Reset Defaults" → "Edit Details"
  - Handler: onEditDetails() (existing)
  - Keep "Save Settings" button unchanged
  - Location: Line 216-222 in BottomButtonManager.java

- [ ] **4.4** Implement onViewLeaderboard() in TownInterfaceScreen
  - Send LeaderboardDataRequestPacket to server
  - Open TownLeaderboardScreen when response received
  - Location: TownInterfaceScreen.java

- [ ] **4.5** Update ButtonActionCoordinator if needed
  - Location: `common/src/main/java/com/quackers29/businesscraft/ui/managers/ButtonActionCoordinator.java`
  - Add handleViewLeaderboard() method

### Phase 5: Testing & Polish
- [ ] **5.1** Test with no towns (empty state)
  - Leaderboard should show "No towns to display"

- [ ] **5.2** Test with single town (current town only)
  - Distance should be 0m
  
- [ ] **5.3** Test with multiple towns (3-10 towns)
  - Verify sorting by distance, population, money
  - Verify distance calculations accurate
  - Test row click opens correct detail screen

- [ ] **5.4** Test screen scaling
  - GUI Scale 1, 2, 3, 4
  - Verify panel doesn't overflow screen
  - Verify text readable at all scales

- [ ] **5.5** Test navigation flow
  - Overview → Leaderboard → Detail → Leaderboard → Back
  - Settings → Edit Details modal (verify moved correctly)

- [ ] **5.6** Polish and UX
  - Add tooltips to column headers explaining sort
  - Add visual indicator for current sort mode
  - Smooth scrolling experience
  - Consider adding search/filter functionality (future enhancement)

## File Structure Summary
```
common/src/main/java/com/quackers29/businesscraft/
├── town/data/
│   └── TownLeaderboardData.java (NEW)
├── network/packets/ui/
│   ├── LeaderboardDataRequestPacket.java (NEW)
│   └── LeaderboardDataResponsePacket.java (NEW)
├── ui/screens/town/
│   ├── TownLeaderboardScreen.java (NEW)
│   └── TownDetailScreen.java (NEW)
└── ui/managers/
    ├── BottomButtonManager.java (MODIFY - lines 16, 168-174, 216-222)
    └── ButtonActionCoordinator.java (MODIFY - add handleViewLeaderboard)
```

## Technical Considerations

### Distance Calculation
- Use `BlockPos.distSqr()` for performance, then `Math.sqrt()` for display
- Format: `<1000m = "123m"`, `>=1000m = "1.2km"` (one decimal)

### Data Synchronization
- Client caches leaderboard data on screen open
- Future: Auto-refresh on town changes (low priority)
- Current: Refresh on screen reopen

### Performance
- Max towns: ~100 (typical server)
- Sorting: O(n log n) - negligible for <100 items
- Rendering: BCModalGridScreen virtualization handles scrolling

### Future Enhancements (Not Current Scope)
- Search/filter towns by name
- Multiple sort columns with indicators
- "Visit" navigation system (teleport/waypoint)
- Real-time leaderboard updates
- Player-specific stats (visits, trades with town)

## Dependencies
- Existing: BCModalGridScreen, TownManager, Network packet system
- No new external libraries needed

## Testing Strategy
1. Unit test distance calculations
2. Manual test UI navigation flow
3. Test with edge cases (0 towns, 1 town, many towns)
4. Test screen scaling and responsiveness
5. Verify button placement changes correct
