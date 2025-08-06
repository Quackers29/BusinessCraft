# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Instructions

"AI models are geniuses who start from scratch on every task." - Noam Brown

Your job is to "onboard" yourself to the current task.

Do this by:

- Using ultrathink
- Exploring the codebase
- Asking me questions if needed

The goal is to get you fully prepared to start working on the task.

Take as long as you need to get yourself ready. Overdoing it is better than underdoing it.

Record everything in a .claude/tasks/[TASK_ID]/onboarding.md file. This file will be used to onboard you to the task in a new session if needed, so make sure it's comprehensive.

## Standard Workflow

0. Always read ALL of CLAUDE.md and tasks/todo.md every time the user has prompted you to continue on so this stays fresh.
1. First, read the codebase for relevant files, and write a plan to tasks/todo.md.
2. Read tasks/toImprove.md, this is a log of future improvements to be made but not the current task, update as needed
3. The plan should have a list of todo items that you can check off as you complete them.
4. Before you begin working, check in with me and I will verify the plan. Use the 'Notify User' command to notify me prior to any input to get my attention (including when you complete).
5. Then, begin working on the todo items, keep on marking them off as you go in the file so I can track progress.
6. Finally, if you have noticed you have move onto your next task, move the previously completed task to the bottom of the done.md file, using similar style to whats already in the end of that file.

## Project Overview

BusinessCraft is a sophisticated Minecraft mod featuring a complete town management and tourism economy system. The codebase is production-ready with advanced architectural patterns and enterprise-grade implementation quality.

### Multi-Platform Architecture

**Target Platforms**: Minecraft Forge 1.20.1 + Fabric 1.20.1 (full feature parity)
**Architecture Pattern**: Enhanced MultiLoader Template with zero external dependencies

**CRITICAL**: BusinessCraft uses the **Enhanced MultiLoader Approach** for cross-platform compatibility. This architectural decision was made after comprehensive analysis and must be maintained.

#### Why MultiLoader Template (Not Architectury or Other APIs)
- **Zero External Dependencies**: No third-party API risks (Architectury, FFAPI, etc.)
- **Maximum Performance**: Direct platform API access with no abstraction overhead
- **Industry Proven**: Used by JEI, Jade, Create and other major successful mods
- **100% Feature Parity**: All BusinessCraft features have direct Fabric equivalents
- **Long-term Stability**: No risk of abandoned dependencies or API changes

#### Current Multi-Platform Status
- **Platform Abstraction**: 65% complete - excellent enterprise-grade foundation
- **Common Module**: 19 files (8%) - all business logic platform-agnostic
- **Forge Module**: 208 files (92%) - platform-specific implementations
- **Fabric Module**: Planned - will match Forge functionality exactly

## Development Commands

### Build and Run
- **Build the mod**: `./gradlew build`
- **Run client**: `./gradlew runClient`
- **Run server**: `./gradlew runServer`
- **Clean build**: `./gradlew clean build`
- **Notify User**: `powershell.exe -Command "[console]::beep(800,200); Start-Sleep -Milliseconds 200; [console]::beep(800,200); Start-Sleep -Milliseconds 200; [console]::beep(800,200)"`

### Testing
- **Run all tests**: `./gradlew test`
- **Run specific test**: `./gradlew test --tests "ClassName.methodName"`

## Task Management Files

### **tasks/todo.md** - Current Active Tasks
- Contains current priority tasks and implementation plans
- Focus on immediate and near-term work items
- Medium priority maintenance tasks and bug fixes
- Keep this file clean and focused on active work

### **tasks/done.md** - Completed Task Archive
- Contains all completed tasks with full implementation details
- Comprehensive achievement summaries and technical specifications
- Reference for understanding what has been implemented
- **Do not modify** - this is historical record

### **tasks/toImprove.md** - Future Enhancements and Technical Specs
- Future architectural improvements and optimization ideas
- Detailed technical specifications and design patterns
- Lower priority tasks and enhancement suggestions
- Excessive implementation details that would clutter todo.md
- **Add new improvement ideas here** when they come up

### Debug Configuration
- **Debug Control File**: `src/main/java/com/yourdomain/businesscraft/debug/DebugConfig.java`
- **Toggle debug logging**: Edit boolean flags in DebugConfig.java (requires rebuild)
- **Current debug status**: All systems disabled for clean production logs
- **Clean Logs**: Excessive INFO logs converted to debug-controlled logging:
  - Payment Board UI operations (`UI_MANAGERS`)
  - Storage operations (`STORAGE_OPERATIONS`) 
  - Tourist arrival processing (`VISITOR_PROCESSING`)
  - Milestone reward processing (`VISITOR_PROCESSING`)
- **Global override**: Set `FORCE_ALL_DEBUG = true` to enable all debugging
- **Component-specific flags**: 25+ individual flags for targeted debugging
  - `TOWN_BLOCK_ENTITY`, `NETWORK_PACKETS`, `UI_MANAGERS`, etc.
  - `VISITOR_PROCESSING`, `STORAGE_OPERATIONS`, `TRADE_OPERATIONS`
  - `PLATFORM_SYSTEM`, `TOURIST_ENTITY`, `CLIENT_HANDLERS`
- **Usage**: `DebugConfig.debug(LOGGER, DebugConfig.COMPONENT_FLAG, "Message");`

## Current Implementation Status

### ✅ Fully Implemented Core Systems

**Block System** (`com.yourdomain.businesscraft.block`)
- `TownInterfaceBlock`: Complete block implementation with custom GUI
- `TownInterfaceEntity`: Sophisticated 977-line block entity with:
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

**World Visualization System** (`com.yourdomain.businesscraft.client.render.world`)
- Modular 3D line rendering framework for world overlays: `LineRenderer3D`, `BoundaryRenderer3D`, `PathRenderer3D`
- `WorldVisualizationRenderer` base class with distance culling and chunk management
- `VisualizationManager` handles multiple visualization types (platform, route, debug, territory, quest)
- Ready for territory boundaries, transportation routes, quest paths, and debug overlays

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

**MultiLoader Template Pattern**: Cross-platform compatibility through service abstraction
**Provider Pattern**: Consistent data access through `ITownDataProvider`
**Service-Oriented Architecture**: Platform services (`PlatformServices`, `RegistryHelper`, `NetworkHelper`, `EventHelper`)
**Component-Based UI**: Reusable components with composition over inheritance
**Separation of Concerns**: Clear layer separation between UI, data, and logic
**Event-Driven Design**: Clean event handling throughout UI system
**Maximum Common Code**: Business logic abstracted to common module
**Rate Limiting**: Smart performance optimizations in rendering and updates

## File Organization

### Multi-Module Structure
- **`common/`**: Platform-agnostic business logic and shared code
  - All core game logic, data models, UI framework
  - Service interfaces: `PlatformServices`, helper interfaces
  - Zero platform dependencies - pure business logic
- **`forge/`**: Forge-specific platform implementations
  - Platform services: `ForgePlatformHelper`, `ForgeRegistryHelper`, etc.
  - Forge-specific initialization and registration
- **`fabric/`**: Fabric-specific platform implementations (planned)
  - Platform services: `FabricPlatformHelper`, `FabricRegistryHelper`, etc.
  - Fabric-specific initialization and registration

### Common Module Package Structure (Platform-Agnostic)
- `business/`: Core business logic and game rules
- `service/`: Service interfaces for platform abstraction
- `town/`: Core town logic, components, and data management
- `ui/`: Complete UI framework (11 subdirectories) - zero platform deps
- `network/packets/`: Packet definitions (5 subdirectories)
- `config/`: Configuration data structures
- `util/`: Utility classes with no platform dependencies

### Platform Module Structure (Forge/Fabric-Specific)
- `platform/`: Platform service implementations
- `init/`: Platform-specific registration classes
- `client/`: Client-side setup and key handlers
- `event/`: Platform-specific event handling
- Main class: `BusinessCraft.java` / `BusinessCraftFabric.java`

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

## Multi-Platform Development Guidelines

### MANDATORY: Enhanced MultiLoader Approach
**All development must follow the MultiLoader Template pattern. Do not use:**
- Architectury API (dependency risk, performance overhead)
- Forgified Fabric API (experimental, unnecessary complexity)
- Platform-specific development (abandons abstraction work)

### Platform Abstraction Rules
1. **Maximize Common Code**: All business logic goes in common module
2. **Service-Oriented Design**: Use `PlatformServices` for all platform operations
3. **Zero Platform Dependencies in Common**: Common module must compile without Forge or Fabric
4. **Complete Feature Parity**: Fabric implementation must match Forge functionality exactly
5. **Performance Priority**: Direct platform APIs, no unnecessary abstraction layers

### Code Organization Rules
- **Business Logic**: Always in common module (town management, economy, calculations)
- **UI Components**: Always in common module (already 100% platform-agnostic)
- **Network Packets**: Definitions in common, serialization through platform services
- **Platform Services**: Only in platform-specific modules
- **Registration**: Platform-specific implementations of common interfaces

## Development Guidelines

### Working with the UI Framework
- Use `BCScreenBuilder` for all new screens - provides fluent API
- Leverage layout managers (`BCFlowLayout`, `BCGridLayout`, `BCFlexLayout`)
- Follow `BCTheme` system for consistent styling
- Use `BCModalGridScreen` for data display tables
- Implement state binding through `StateBindingManager`

### Multi-Platform Development Best Practices
- **Platform Services**: Always use `PlatformServices.getXXXHelper()` for platform operations
- **Common Module First**: Implement business logic in common, then create platform implementations
- **Service Interfaces**: Define interfaces in common, implement in platform modules
- **Testing Strategy**: Test both platforms for feature parity

### Data Management Best Practices
- Access town data via `TownManager.get(ServerLevel)`
- Use helper classes in `town.data` package for complex operations
- Implement proper cleanup in server lifecycle events
- Cache frequently accessed data through `TownDataCacheManager`
- **Platform Abstraction**: Use service interfaces for inventory, networking, events

### Network Development
- **Platform Abstraction**: Use `NetworkHelper` interface for all networking
- Extend `BaseBlockEntityPacket` for block entity-related packets
- Organize new packets in appropriate subdirectories
- Register all packets through `PlatformServices.getNetworkHelper()`
- **Common Definitions**: Packet classes in common, serialization through platform services
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