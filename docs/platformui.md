# Platform Management System Documentation

## Overview

The Platform Management System in BusinessCraft is a feature that allows players to create and manage tourist platforms within their towns. Platforms serve as tourist arrival points and navigation endpoints, enabling town owners to control how tourists enter and move through their towns.

## Key Components

### 1. Platform Class

The `Platform` class (`src/main/java/com/yourdomain/businesscraft/platform/Platform.java`) represents a tourist platform with the following properties:

- `id` - Unique identifier (UUID)
- `name` - Display name
- `enabled` - Whether the platform is active
- `startPos` and `endPos` - Path start and end positions
- `enabledDestinations` - List of town destinations this platform connects to

The class provides methods for:
- Saving/loading platform data to/from NBT
- Getting and setting platform properties
- Managing enabled destinations

### 2. PlatformManagementScreen

The `PlatformManagementScreen` (`src/main/java/com/yourdomain/businesscraft/screen/PlatformManagementScreen.java`) provides the UI for managing platforms. It features:

- List view of all platforms with scrolling
- Status indicators for enabled/disabled state
- Buttons for toggling, editing paths, and managing destinations
- Support for mouse wheel scrolling and button navigation

### 3. Network Packets

The system includes several network packets for client-server communication:

- `AddPlatformPacket` - Creates a new platform on the server
- `DeletePlatformPacket` - Removes a platform from the server
- `SetPlatformEnabledPacket` - Toggles a platform's enabled state
- `SetPlatformPathPacket` - Sets a platform's path start and end positions
- `OpenDestinationsUIPacket` - Opens the destinations UI for a platform

## Implementation Notes

1. **Platform Lifecycle**
   - Platforms are created with a random UUID
   - Platforms are stored in the TownBlockEntity
   - When a town is loaded, platforms are loaded from NBT data

2. **Network Communication**
   - All platform operations are performed on the server
   - Changes are synchronized to clients via update packets
   - The client UI sends requests to the server for changes

3. **Data Persistence**
   - Platforms are saved to NBT data in the TownBlockEntity
   - The `save()` and `Platform.fromNBT()` methods handle serialization
   - Legacy platform data is converted during loading

## UI Design

The platform management UI follows the BusinessCraft design language:

- Semi-transparent backgrounds with highlights
- Clear status indicators (green for enabled, red for disabled)
- Tooltips for buttons and controls
- Support for keyboard navigation and mouse wheel scrolling

## Integration with Town System

The Platform Management System integrates with the existing town system:

- Accessible from the Settings tab in the Town Interface
- Platforms affect tourist behavior and pathing
- Platform destinations connect towns in the tourism network

## Development Lessons

1. For scrollable components, use lenient bounds checking (x-5, x+width+5) to catch scroll events slightly outside the grid component
2. When implementing destinations, use a clear system for managing enabled/disabled state
3. When using constructor references in packet registration, ensure the constructor matches the expected signature
4. Use static decode methods for packet deserialization for clarity 