# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Standard Workflow

1. First, read the codebase for relevant files, and write a plan to tasks/todo.md.
2. Read tasks/toImprove.md, this is a log of future improvements to be made but not the current task, update as needed
3. The plan should have a list of todo items that you can check off as you complete them.
4. Before you begin working, check in with me and I will verify the plan. Use the 'Notify User' command to notify me prior to any input to get my attention (including when you complete).
5. Then, begin working on the todo items, keep on marking them off as you go in the file so I can track progress.
6. Finally, add a review section to the todo.md file with a summary of the changes you made and any other relevant information.

## Project Overview

BusinessCraft is a sophisticated Minecraft Forge 1.20.1 mod featuring a complete town management and tourism economy system. The codebase is production-ready with advanced architectural patterns and enterprise-grade implementation quality.

## Development Commands

### Build and Run
- **Build the mod**: `./gradlew build`
- **Run client**: `./gradlew runClient`
- **Run server**: `./gradlew runServer`
- **Clean build**: `./gradlew clean build`
- **Notify User**: `powershell.exe -Command "1..5 | ForEach-Object { [console]::beep(800,200); Start-Sleep -Milliseconds 100 }" && echo "🔔 ATTENTION: Task completed - please check results 🔔"`

### Testing
- **Run all tests**: `./gradlew test`
- **Run specific test**: `./gradlew test --tests "ClassName.methodName"`

## Current Implementation Status

### ✅ Fully Implemented Core Systems

**Block System** (`com.yourdomain.businesscraft.block`)
- `TownBlock` & `TownInterfaceBlock`: Complete block implementations with custom GUIs
- `TownBlockEntity`: Sophisticated 977-line block entity with:
  - Automatic town registration with distance validation
  - Multi-platform system (up to 10 platforms per town)
  - Real-time particle effects for platform visualization
  - Smart resource processing with client-server sync
  - Advanced debug overlay integration

**Entity System** (`com.yourdomain.businesscraft.entity`)
- `TouristEntity`: Complete villager-based tourist with 393 lines of advanced features:
  - Configurable expiry system to prevent infinite accumulation
  - Movement detection from spawn position
  - Smart ride extension (resets timer when boarding minecarts/trains)
  - Origin/destination tracking with full persistence
  - Professional randomization and breeding prevention
- `TouristRenderer` with custom hat layer rendering

**Town Management** (`com.yourdomain.businesscraft.town`)
- `Town`: Comprehensive 701-line implementation with:
  - Complete `ITownDataProvider` interface
  - Multi-tiered storage (resources, communal, personal per player)
  - Visit history tracking with persistence
  - Population growth from tourism
  - Dynamic tourist capacity based on population
- `TownManager`: Level-specific management with save/load system
- Component architecture: `TownEconomyComponent`, `TownResources`

**Platform System** (`com.yourdomain.businesscraft.platform`)
- `Platform`: Advanced 275-line implementation with:
  - Multi-destination support with UUID tracking
  - Complete NBT serialization
  - Path validation and completion checks
  - Individual platform enable/disable
  - Real-time visualization with particle effects

**UI Framework** (`com.yourdomain.businesscraft.ui`) - **Production-Grade**
- Complete component-based architecture with 11-directory structure
- `BCScreenBuilder`: Fluent API for screen creation with tabbed interfaces
- Component hierarchy:
  - Basic: `BCButton`, `BCLabel`, `BCPanel`, `BCComponent`
  - Containers: `BCTabPanel`, `SlotComponent`, `StandardTabContent`
  - Display: `ResourceListComponent`, `VisitHistoryComponent` (with scrolling)
  - Input: `BCEditBox`, `BCToggleButton`, `TownNameEditor`
- Layout system: `BCFlexLayout`, `BCGridLayout`, `BCFlowLayout`
- State management: Sophisticated binding system with real-time updates
- Modal system: `BCModalGridScreen`, `BCModalInventoryScreen`
- Screen implementations: Town Interface, Platform Management, Trade, Storage

**Network System** (`com.yourdomain.businesscraft.network`)
- **22 different packet types** organized in 5 logical subpackages:
  - `platform/`: Platform management (7 packets)
  - `storage/`: Storage systems (5 packets)
  - `town/`: Town management (2 packets)
  - `ui/`: UI navigation (4 packets)
  - `misc/`: Miscellaneous (4 packets)
- All packets fully implemented with proper serialization/deserialization
- `BaseBlockEntityPacket` base class for block entity packets

**Data Management** (`com.yourdomain.businesscraft.town.data`)
- Sophisticated helper system:
  - `ClientSyncHelper`: Server-client synchronization
  - `ContainerDataHelper`: Modular ContainerData system
  - `NBTDataHelper`: Complex save/load operations
  - `TownDataCacheManager`: Client-side caching
  - `VisitorProcessingHelper`: Complex visitor detection
  - `PlatformVisualizationHelper`: Particle effect management

### 🔧 Minor Implementation Gaps

- Job assignment system (UI present, logic placeholder)
- Advanced economy features (basic coin system defined)
- Some UI polish in secondary screens

### Key Architectural Patterns

**Provider Pattern**: Consistent data access through `ITownDataProvider`
**Component-Based UI**: Reusable components with composition over inheritance
**Separation of Concerns**: Clear layer separation between UI, data, and logic
**Event-Driven Design**: Clean event handling throughout UI system
**Rate Limiting**: Smart performance optimizations in rendering and updates

## File Organization

### Main Package Structure
- `block/`: Block implementations and block entities
- `entity/`: Tourist entity and rendering
- `town/`: Core town logic, components, and data management
- `platform/`: Platform system implementation
- `ui/`: Complete UI framework (11 subdirectories)
- `network/packets/`: Organized packet system (5 subdirectories)
- `init/`: Forge registration classes
- `config/`: Configuration loading
- `command/`: Admin commands
- `client/`: Client-side setup and key handlers

### UI Framework Organization
- `components/basic/`: Foundation UI components
- `components/containers/`: Complex container components
- `components/display/`: Data display with scrolling support
- `components/input/`: User input components
- `layout/`: Layout management system
- `modal/`: Modal dialog system
- `screens/`: Complete screen implementations
- `state/`: State management and binding
- `managers/`: UI event and data managers
- `templates/`: Screen templates and themes

## Development Guidelines

### Working with the UI Framework
- Use `BCScreenBuilder` for all new screens - provides fluent API
- Leverage layout managers (`BCFlowLayout`, `BCGridLayout`, `BCFlexLayout`)
- Follow `BCTheme` system for consistent styling
- Use `BCModalGridScreen` for data display tables
- Implement state binding through `StateBindingManager`

### Data Management Best Practices
- Access town data via `TownManager.get(ServerLevel)`
- Use helper classes in `town.data` package for complex operations
- Implement proper cleanup in server lifecycle events
- Cache frequently accessed data through `TownDataCacheManager`

### Network Development
- Extend `BaseBlockEntityPacket` for block entity-related packets
- Organize new packets in appropriate subdirectories
- Register all packets in `ModMessages`
- Follow existing serialization patterns

### Performance Considerations
- UI components use virtualization for large lists
- Particle effects are optimized with rate limiting
- Client-side caching reduces server requests
- Proper resource cleanup on server stop/level unload

### Testing and Debugging
- Use `./gradlew runClient` for development testing
- F3+K toggles town debug overlay
- Debug commands available through `/cleartowns`
- Comprehensive logging throughout all systems

## Configuration
- `ConfigLoader` handles TOML configuration files
- Configurable: minimum distances, tourist capacities, population ratios
- Integration hooks for Create mod trains and economy mods