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

### 🏗️ **Current Architecture: Multi-Platform Ready**
- **Target Platforms**: Minecraft Forge 1.20.1 + Fabric 1.20.1 (full feature parity)
- **Architecture**: Unified Codebase with platform-specific modules
- **Status**: Common module complete, platform modules ready for implementation

### 📁 **Module Structure**
- **`common/`**: All business logic, UI framework, and platform-agnostic code
- **`forge/`**: Forge-specific platform implementations (currently empty)
- **`fabric/`**: Fabric-specific platform implementations (currently empty)

## Development Commands

### Build and Run (WSL Recommended)
**⚠️ IMPORTANT**: Use WSL commands for all Gradle operations to avoid UNC path issues.

- **Build all modules**: `wsl ./gradlew build` (**RECOMMENDED**)
- **Build specific module**: `wsl ./gradlew :common:build` or `wsl ./gradlew :forge:build`
- **Build Fabric module**: `wsl ./gradlew :fabric:build` (**REQUIRED** for proper compilation)
- **Run client (common)**: `wsl ./gradlew :common:runClient`
- **Run server (common)**: `wsl ./gradlew :common:runServer`
- **Run Fabric client**: `wsl ./gradlew :fabric:runClient`
- **Clean build**: `wsl ./gradlew clean build`
- **Notify User**: `powershell.exe -Command "[console]::beep(800,200); Start-Sleep -Milliseconds 200; [console]::beep(800,200); Start-Sleep -Milliseconds 200; [console]::beep(800,200)"`

### Development Environment Requirements
**All Gradle commands must use WSL** due to UNC path compatibility issues:
- ✅ **WSL Terminal**: Proper Unix-like environment for all Gradle operations
- ❌ **Windows PowerShell**: Causes UNC path issues with Gradle builds
- ❌ **Command Prompt**: May cause path resolution problems

### Testing
- **Run all tests**: `wsl ./gradlew test`
- **Run specific test**: `wsl ./gradlew test --tests "ClassName.methodName"`

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
- **Debug Control File**: `common/src/main/java/com/yourdomain/businesscraft/debug/DebugConfig.java`
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

**Provider Pattern**: Consistent data access through `ITownDataProvider`
**Component-Based UI**: Reusable components with composition over inheritance
**Separation of Concerns**: Clear layer separation between UI, data, and logic
**Event-Driven Design**: Clean event handling throughout UI system
**Rate Limiting**: Smart performance optimizations in rendering and updates

## File Organization

### Multi-Module Structure
- **`common/`**: All shared business logic and platform-agnostic code
  - `src/main/java/com/yourdomain/businesscraft/`: Complete implementation (200+ files)
  - `src/main/resources/`: Shared assets and data files
- **`forge/`**: Forge-specific platform implementations (currently minimal)
- **`fabric/`**: Fabric-specific platform implementations (currently minimal)

### Common Module Package Structure
- `block/`: Block implementations and block entities
- `entity/`: Tourist entity and rendering
- `town/`: Core town logic, components, and data management
- `platform/`: Platform abstraction interfaces (not implementations)
- `ui/`: Complete UI framework (11 subdirectories)
- `network/packets/`: Organized packet system (5 subdirectories)
- `init/`: Platform-agnostic registration interfaces
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
- Use `./gradlew :common:runClient` for development testing
- F3+K toggles town debug overlay
- Debug commands available through `/cleartowns`
- Comprehensive logging throughout all systems
- Test both platforms: `./gradlew :common:runClient` (Forge) and `./gradlew :fabric:runClient` (Fabric)

## Multi-Platform Development Guidelines

### Architecture Approach
- **Common Module**: Contains all business logic, UI framework, and platform-agnostic code
- **Platform Modules**: Only contain loader-specific implementations (Forge/Fabric)
- **Zero Platform Dependencies**: Common module must compile without Forge or Fabric
- **Feature Parity**: All functionality must work identically on both platforms

### Development Workflow
1. **Implement in Common**: Add new features to the common module first
2. **Test on Forge**: Use `./gradlew :common:runClient` to verify functionality
3. **Extract Platform Code**: Move loader-specific code to appropriate platform module
4. **Test Both Platforms**: Ensure identical behavior on Forge and Fabric
5. **Maintain Interfaces**: Use platform abstraction interfaces for loader differences

### Platform Abstraction Rules
- **Business Logic**: Always in common module
- **UI Components**: Always in common module (already platform-agnostic)
- **Network Packets**: Definitions in common, serialization through platform services
- **Registration**: Platform-specific implementations of common interfaces
- **Events**: Platform-agnostic event interfaces with platform-specific handlers

## Configuration
- `ConfigLoader` handles TOML configuration files
- Configurable: minimum distances, tourist capacities, population ratios
- Integration hooks for Create mod trains and economy mods