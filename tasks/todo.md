# BusinessCraft Debug Logging Control System Implementation

## Problem Analysis

The codebase currently has excessive debug logging throughout all systems, making development logs noisy and hard to follow. We need a configurable debug logging system that allows granular control over logging output per file/component.

## Requirements

1. **Per-file debug toggles** - Each file should have its own debug logging control
2. **Default to disabled** - Files without explicit toggle configuration should not log debug messages
3. **Global override** - Ability to force enable all debug logging for development
4. **Startup transparency** - Report which files have debug logging enabled at mod initialization
5. **Zero performance impact** - Use compile-time optimizable patterns
6. **Developer-friendly** - Simple boolean constants that are easy to understand and modify

## Implementation Plan

### Milestone 1: Core Debug Configuration System
**Goal**: Create the central debug configuration class and basic infrastructure

**Tasks**:
- [ ] **Task 1.1**: Create `DebugConfig.java` in `com.yourdomain.businesscraft.debug` package
  - Static boolean constants for each major component/file
  - Global override flag (`FORCE_ALL_DEBUG`)  
  - Helper method `isEnabled(boolean fileToggle)` 
  - Startup reporting method `logActiveDebuggers()`

- [ ] **Task 1.2**: Identify all files with current debug logging
  - Scan codebase for `LOGGER.debug()` calls
  - Categorize by logical component/system
  - Create initial toggle list for major systems

- [ ] **Task 1.3**: Test the basic infrastructure
  - Add debug config to mod initialization
  - Verify startup reporting works correctly
  - Test global override functionality

**Expected Files**:
- `src/main/java/com/yourdomain/businesscraft/debug/DebugConfig.java`

### Milestone 2: Replace Debug Logging in Core Systems  
**Goal**: Convert debug logging in the most problematic/noisy systems first

**Tasks**:
- [ ] **Task 2.1**: Replace debug logging in TownBlockEntity
  - Convert all `LOGGER.debug()` calls to conditional logging using DebugConfig
  - Add `TOWN_BLOCK_ENTITY` toggle
  - Test functionality remains unchanged

- [ ] **Task 2.2**: Replace debug logging in TownInterfaceMenu
  - Convert menu-related debug logs
  - Add `TOWN_INTERFACE_MENU` toggle
  - Verify UI functionality works correctly

- [ ] **Task 2.3**: Replace debug logging in Network Packet Handlers
  - Convert all packet handler debug logs  
  - Add `NETWORK_PACKETS` toggle
  - Test network communication still works

**Expected Outcome**: Significantly reduced log noise from the noisiest systems

### Milestone 3: Complete System Coverage
**Goal**: Convert remaining debug logging throughout the entire codebase

**Tasks**:
- [ ] **Task 3.1**: Replace debug logging in UI Management Systems
  - SearchRadiusManager, TownScreenDependencies, Modal managers
  - Add `UI_MANAGERS` toggle
  - Test UI interactions work correctly

- [ ] **Task 3.2**: Replace debug logging in Town & Data Systems  
  - TownService, TownManager, NBTDataHelper, sync helpers
  - Add `TOWN_DATA_SYSTEMS` toggle
  - Verify data persistence and synchronization

- [ ] **Task 3.3**: Replace debug logging in remaining components
  - Platform system, Entity system, Client handlers
  - Add appropriate toggles for each system
  - Comprehensive testing of all functionality

**Expected Outcome**: Complete debug logging control across entire codebase

### Milestone 4: Enhancement and Polish
**Goal**: Add advanced features and ensure production readiness  

**Tasks**:
- [ ] **Task 4.1**: Add logging level configuration
  - Support for different log levels (INFO, WARN, ERROR) in debug mode
  - Configurable output format for debug messages
  - Performance measurement for logging overhead

- [ ] **Task 4.2**: Create development convenience features
  - Quick toggle methods for common debugging scenarios  
  - Integration with existing debug overlay (F3+K)
  - Documentation for debugging workflows

- [ ] **Task 4.3**: Testing and documentation
  - Comprehensive testing with all toggles enabled/disabled
  - Update CLAUDE.md with new debugging guidelines
  - Performance verification (ensure no overhead when disabled)

**Expected Outcome**: Production-ready debug logging system with advanced features

## Files to Examine/Modify

### Primary Files (Milestone 1)
- `src/main/java/com/yourdomain/businesscraft/debug/DebugConfig.java` - **NEW** Core configuration
- `src/main/java/com/yourdomain/businesscraft/BusinessCraft.java` - Mod initialization integration

### High-Priority Files (Milestone 2)  
- `src/main/java/com/yourdomain/businesscraft/block/entity/TownBlockEntity.java` - Heavy debug logging
- `src/main/java/com/yourdomain/businesscraft/menu/TownInterfaceMenu.java` - UI debug logs  
- `src/main/java/com/yourdomain/businesscraft/network/packets/**/*.java` - Network debug logs

### Complete Coverage (Milestone 3)
- `src/main/java/com/yourdomain/businesscraft/ui/managers/*.java` - UI management systems
- `src/main/java/com/yourdomain/businesscraft/town/**/*.java` - Town and data systems  
- `src/main/java/com/yourdomain/businesscraft/town/data/*.java` - Data persistence helpers
- All remaining files with `LOGGER.debug()` calls

## Implementation Pattern

### Before (Current):
```java
LOGGER.debug("SearchRadiusManager initialized with radius: {}", currentRadius);
```

### After (New Pattern):
```java
DebugConfig.debug(LOGGER, DebugConfig.SEARCH_RADIUS_MANAGER, "SearchRadiusManager initialized with radius: {}", currentRadius);
```

## Expected Outcome

After implementation:
1. **Clean development logs** - Only relevant debug information appears
2. **Granular control** - Developers can enable/disable logging per system
3. **Zero performance impact** - Conditional checks are optimized away by compiler
4. **Startup transparency** - Clear visibility of which systems have debug logging enabled
5. **Developer-friendly** - Easy to toggle logging for specific areas during debugging

## Success Criteria

- [ ] All debug logging is controlled by DebugConfig toggles
- [ ] Default state produces clean, readable logs
- [ ] Global override enables all debug logging for development
- [ ] Startup reports show active debug logging status  
- [ ] No functional regressions in any game systems
- [ ] Performance impact is negligible when debug logging is disabled

---

## Review Section
*To be completed after implementation*
