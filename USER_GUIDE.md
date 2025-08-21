## BusinessCraft User Guide (Gameplay Overview)

This guide explains how to use the core features of BusinessCraft from a player perspective. It focuses on concrete, confirmed behaviors present in the codebase.

### Getting Started
- Place a Town Interface Block and right‑click it to open the town management UI.
- On valid placement, a town is created automatically:
  - Name: chosen from the configured list; falls back to "DefaultTown" if the list is empty.
  - Validation: if the location violates placement rules (e.g., boundary distance), the block is removed and, if not in Creative, it’s returned to your inventory with an on‑screen reason.
- A default platform is created automatically:
  - Orientation: aligned with your facing direction at placement.
  - Path: START 3 blocks away and END 5 blocks away from the town block on the same Y level.
  - Limit: Up to 10 platforms per town.

### Creating and Managing a Town
- The Town Interface shows server‑synced values for population, current/max tourists, and search radius; open screens refresh automatically when values change.
- Town name updates are reflected live; the server is the source of truth.
- Removing the Town Interface Block deletes the associated town and its data.

### Platforms (Travel Lines)
- Open Platform Management from the Town Interface.
- For each platform you’ll see:
  - Status: green when Enabled + Path complete; gray otherwise.
  - Toggle (ON/OFF): click to enable/disable immediately.
  - Destinations: open the destinations UI to constrain where this platform can send/receive.
  - Actions: Set Path (if incomplete) or Reset (if complete).
- Add Platform: adds a new platform. Delete Last: removes only the most recently added platform; there is always at least one platform.
- Set Path (in‑world pathing mode):
  - On‑screen instructions: right‑click to set START, then right‑click to set END. Press ESC to cancel.
  - When a path is complete and the platform is enabled, it becomes active for spawning/visualization.
- After leaving the Town Interface, active platform paths display particle indicators for a short time to help you locate them.

### Tourists and Rewards
- Tourist spawning cadence: checked about every 10 seconds.
- Spawn conditions (all must be true):
  - Tourist spawning is enabled for the town.
  - The town has capacity to add more tourists.
  - At least one platform is enabled and has a complete START→END path.
- When tourists arrive, rewards (emeralds and milestone items) are generated and listed in the Payment Board.
  - Capacity: configurable max tourists per town (default 100).
  - Population thresholds: minimum population to spawn tourists (default 5).
  - Population growth: configuration supports tourist-driven population increase.

### Payment Board (Claiming Rewards)
- Layout: 4 columns — Source, Rewards, Time (HH:mm:ss), Claim/Status. The list auto‑scrolls for many entries.
- Tourist rewards render multiple item icons; tooltips show origin town and distance traveled when available.
- Other rewards show an item icon and concise text summary.
- Status values: Unclaimed, Claimed, or Expired.
- Actions: Click Claim to send the reward items directly to the town’s Buffer Storage.

### Buffer Storage (Payment Board Output)
- Capacity and layout: 18 slots (2 rows × 9 columns) displayed in the Payment Board screen.
- Player interactions are withdrawal‑only:
  - Left‑click: remove the whole stack from a buffer slot.
  - Right‑click: remove half the stack from a buffer slot.
  - Shift‑click: quick‑move items from the buffer into your inventory.
- Adding items to the buffer from the UI is blocked.
- Automation: hoppers can extract from the buffer when connected from below.

### Contributing Resources
- Insert items into the Town Interface Block’s input slot; any item type is accepted.
- Processing rate: one item per game tick (approx. 20 items per second) on the server.
- Added items are converted into town resources; UI values update and sync to viewers immediately.
 - Configuration examples influencing gameplay:
   - Default starting population: 5
   - Max tourists per town: 100
   - Min population for tourists: 5
   - Vehicle search radius (affects path area visuals and detection): 3
   - Town names list for auto-naming

### Additional Screens
- Trade Screen: One input slot and one output slot; click the Trade button to process the exchange and receive the result in the output slot. A Back button returns you to the Town Interface.

### Visualization and Debug Aids
- Platform paths: After leaving the UI, particles render along enabled, complete paths at intervals so you can follow the route.
- Search radius: Red flame particles outline a rectangle encompassing the START→END line, expanded by the configured search radius.
- Debug overlay: Press F3+K to toggle the town debug overlay.

### Commands
- /cleartowns (operator only): Removes all towns in the current world.

### Multiplayer and Persistence
- Multiple players can interact with the same town; open screens refresh when server values change.
- Town data, platforms, resources, and rewards persist across saves. Removing the town block removes the town.

### Search Radius Controls (in UI)
- Increase/decrease: left‑click increases, right‑click decreases.
- Fine/coarse adjustment: hold Shift for larger steps.
- Limits: minimum 1, maximum 100. Changes take effect immediately and sync to the server.

### Tips
- Ensure each platform has both a start and an end set; incomplete paths won’t be used for spawning or visualization.
- If placement fails, try moving farther from other towns’ boundaries; the game will show a reason when blocked.


