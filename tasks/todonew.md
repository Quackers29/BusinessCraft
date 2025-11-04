# BusinessCraft Common Module Decoupling Plan

## Executive Summary
**Objective:** Transform the heavily Forge-coupled common module into a platform-agnostic codebase while maintaining full Forge functionality throughout the process.

### Key Metrics
- **Total files requiring changes:** 100+ files
- **Estimated timeline:** 6-8 weeks with incremental approach
- **Risk level:** High (but manageable with testing safeguards)
- **Success criteria:** Platform modules build with abstracted common code, full Forge functionality preserved
- **Current architecture:** Multi-module (common + forge + fabric) - will transition to single-JAR-per-platform after decoupling

### Current Status
- ✅ **Forge client:** Fully functional (`:forge:runClient`)
- ❌ **Fabric client:** Infrastructure exists but non-functional
- ✅ **Common module:** ~90% platform-agnostic (~210 Java files contain business logic)
- ✅ **Forge module:** Minimal platform-specific code (~21 Java files)
- ✅ **Architecture:** Multi-module setup maintained for stability
- ✅ **Phase 1-5 Complete:** Network packets, screen opening, debug network, client UI, events, and item handlers decoupled
- ⚠️ **Phase 6 Remaining:** Client rendering system, block entity capabilities, build.gradle

### Scope Breakdown
| Category | Files | Priority | Status |
|----------|-------|----------|--------|
| Network packets | 39 | 🔴 Critical | ✅ Complete |
| Debug network system | 1 | 🔴 Critical | ✅ Complete |
| Screen/menu opening | 3 | 🟡 High | ✅ Complete |
| Client UI abstractions | 51 | 🟠 Medium | ✅ Complete |
| Event system | 6 | 🟢 Low | ✅ Complete |
| Item handlers | 1 | 🟢 Low | ✅ Complete |
| Client rendering | 4 | 🟡 High | ⚠️ Remaining |
| Block entity capabilities | 1 | 🟡 Medium | ⚠️ Remaining |
| Build configuration | 1 | 🔴 Critical | ⚠️ Remaining |
| **TOTAL** | **107** | | **~90% Complete** |

## Implementation Phases

### Phase 1: Network Packet Handler Decoupling ✅ **COMPLETED**
**Status:** ✅ **ALL 39 PACKETS COMPLETED AND TESTED**

**Summary:**
- ✅ **39 packet files** updated to use `PlatformAccess` instead of `NetworkEvent.Context`
- ✅ **Base packet file** (`BaseBlockEntityPacket.java`) updated - affects 14 packets that extend it
- ✅ **All Forge-specific imports removed** from packet handle methods
- ✅ **Packet registrations updated** in `ForgeModMessages.java`
- ✅ **Build tested and verified** - all packets compile and work correctly

**Key Files:**
- All files in `common/src/main/java/com/quackers29/businesscraft/network/packets/`
- `forge/src/main/java/com/quackers29/businesscraft/forge/network/ForgeModMessages.java`

### Phase 1.5: Debug Network System Refactor ✅ **COMPLETED**
**Status:** ✅ **COMPLETED**

**Summary:**
- ✅ **Debug packets extracted** to separate files in `network/packets/debug/`
- ✅ **Both packets updated** to use `PlatformAccess` instead of `NetworkEvent.Context`
- ✅ **TownDebugNetwork simplified** - removed separate SimpleChannel, now uses `PlatformAccess.getNetwork().sendToServer()`
- ✅ **Debug packets registered** in main `ForgeModMessages` registration
- ✅ **All Forge-specific network code removed** from debug system

**Key Files:**
- `common/src/main/java/com/quackers29/businesscraft/network/packets/debug/RequestTownDataPacket.java`
- `common/src/main/java/com/quackers29/businesscraft/network/packets/debug/TownDataResponsePacket.java`
- `common/src/main/java/com/quackers29/businesscraft/client/TownDebugNetwork.java`

### Phase 2: Screen/Menu Opening ✅ **COMPLETED**
**Status:** ✅ **ALL 3 FILES COMPLETED**

**Summary:**
- ✅ **NetworkHelper interface** extended with `openScreen(Object, Object, Object)` overload for BlockPos support
- ✅ **Platform helpers updated** to support BlockPos parameter
- ✅ **All 3 files** updated to use `PlatformAccess.getNetwork().openScreen()` instead of direct `NetworkHooks.openScreen()`
- ✅ **All Forge imports removed** from screen opening code

**Key Files:**
- `common/src/main/java/com/quackers29/businesscraft/block/TownInterfaceBlock.java`
- `common/src/main/java/com/quackers29/businesscraft/network/packets/ui/OpenTownInterfacePacket.java`
- `common/src/main/java/com/quackers29/businesscraft/network/packets/ui/OpenPaymentBoardPacket.java`

### Phase 3: Client-Side UI Code ✅ **COMPLETED**
**Status:** ✅ **ALL 51+ FILES COMPLETED AND TESTED**

**Summary:**
- ✅ **ClientHelper interface created** - Platform-agnostic client-side operations abstraction
- ✅ **ForgeClientHelper implemented** - Full Forge implementation using `Minecraft.getInstance()`
- ✅ **FabricClientHelper implemented** - Placeholder implementation ready for Fabric-specific APIs
- ✅ **PlatformAccess extended** - Added `ClientHelper` static access point
- ✅ **51+ files updated** - All `Minecraft.getInstance()` calls replaced with `PlatformAccess.getClient()` abstractions
- ✅ **Build tested and verified** - All compilation errors resolved, Forge build successful

**Key Files:**
- `common/src/main/java/com/quackers29/businesscraft/api/ClientHelper.java` *(NEW)*
- `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeClientHelper.java` *(NEW)*
- All UI component files in `common/src/main/java/com/quackers29/businesscraft/ui/`

### Phase 4: Event System ✅ **COMPLETED**
**Status:** ✅ **ALL 6 FILES COMPLETED AND TESTED**

**Summary:**
- ✅ **EventCallbacks interface created** - Platform-agnostic callback interfaces for all event types
- ✅ **EventHelper extended** - Added registration methods for all callback types
- ✅ **ForgeEventCallbackHandler created** - Bridges Forge events to common module callbacks
- ✅ **6 event handler files updated** - All `@Mod.EventBusSubscriber` and `@SubscribeEvent` annotations removed
- ✅ **Event initialization added** - Server-side handlers initialized in `commonSetup()`, client-side in `clientSetup()`
- ✅ **All Forge imports removed** - Common module event handlers no longer depend on Forge types

**Key Files:**
- `common/src/main/java/com/quackers29/businesscraft/api/EventCallbacks.java` *(NEW)*
- `forge/src/main/java/com/quackers29/businesscraft/forge/event/ForgeEventCallbackHandler.java` *(NEW)*
- All event handler files in `common/src/main/java/com/quackers29/businesscraft/event/`

### Phase 5: Item Handler Abstractions ✅ **COMPLETED**
**Status:** ✅ **COMPLETED AND TESTED**

**Summary:**
- ✅ **SlotBasedStorageAccess interface created** - Platform-agnostic interface for storage operations
- ✅ **SlotBasedStorage updated** - Implements `SlotBasedStorageAccess`, removed `ItemStackHandler` import
- ✅ **ItemHandlerHelper extended** - Added `createStorageWrapper()` method for storage wrappers
- ✅ **ForgeItemHandlerHelper updated** - Moved `SlotBasedItemStackHandler` from common to Forge module
- ✅ **All Forge imports removed** - Common module no longer depends on `ItemStackHandler`

**Key Files:**
- `common/src/main/java/com/quackers29/businesscraft/api/SlotBasedStorageAccess.java` *(NEW)*
- `common/src/main/java/com/quackers29/businesscraft/town/data/SlotBasedStorage.java`

### Phase 6: Remaining Forge Dependencies ⚠️ **IN PROGRESS**
**Status:** ⚠️ **REMAINING WORK IDENTIFIED**

**Objective:** Remove final Forge dependencies from common module (client rendering, block entity capabilities, build configuration)

#### Issue 6.1: Client Rendering System (4 files)
**Files Affected:**
- `common/src/main/java/com/quackers29/businesscraft/client/TownDebugOverlay.java` - Implements `IGuiOverlay` (Forge-specific interface)
- `common/src/main/java/com/quackers29/businesscraft/client/render/world/WorldVisualizationRenderer.java` - Uses `RenderLevelStageEvent` (Forge-specific)
- `common/src/main/java/com/quackers29/businesscraft/client/render/world/PlatformVisualizationRenderer.java` - Uses `RenderLevelStageEvent.Stage`
- `common/src/main/java/com/quackers29/businesscraft/client/render/world/TownBoundaryVisualizationRenderer.java` - Uses `RenderLevelStageEvent`

**Required Changes:**
- [ ] Create `RenderHelper` interface for world rendering abstractions
- [ ] Abstract overlay rendering (replace `IGuiOverlay` with platform-agnostic interface)
- [ ] Abstract render stage/event system
- [ ] Update all rendering classes to use platform abstractions

#### Issue 6.2: Block Entity Capabilities (1 file)
**Files Affected:**
- `common/src/main/java/com/quackers29/businesscraft/block/entity/TownInterfaceEntity.java` - Uses `Capability` and `LazyOptional` in method signatures

**Required Changes:**
- [ ] Remove `Capability`/`LazyOptional` from method signatures
- [ ] Use `Object` types with PlatformAccess methods (already using PlatformAccess for operations)
- [ ] Update `getCapability()` signature to be platform-agnostic

#### Issue 6.3: Build Configuration (1 file)
**Files Affected:**
- `common/build.gradle` - Still has Forge Gradle plugin and Forge dependencies

**Required Changes:**
- [ ] Remove Forge Gradle plugin (`net.minecraftforge.gradle`)
- [ ] Remove Forge Minecraft dependency
- [ ] Use Minecraft-only dependencies (or platform-agnostic setup)
- [ ] Update comment that says "TEMPORARILY has Forge dependencies"

**Testing Strategy:**
```bash
# After each change
wsl ./gradlew :common:build    # Verify common builds without Forge
wsl ./gradlew :forge:build     # Verify Forge still builds correctly
wsl ./gradlew :forge:runClient # Test rendering and capabilities work
```

## Architecture Audit Results

### Code Distribution ✅ **GOOD**
- **Common module:** ~210 Java files (business logic, UI, calculations, processes)
- **Forge module:** 21 Java files (platform-specific implementations only)
- **Result:** Common module contains ~91% of code, which is correct

### Forge Module Assessment ✅ **GOOD**
The forge module is minimal and platform-specific:
- 11 platform helper implementations
- 4 initialization/registration files
- 3 event handlers
- 1 network registration
- 1 client setup
- 1 main mod class

**No business logic found** - forge module only contains platform-specific implementations.

### Remaining Issues ⚠️
1. **Client rendering** - 4 files still use Forge-specific rendering APIs
2. **Block entity capabilities** - 1 file uses Forge types in method signatures
3. **Build configuration** - Common module still depends on Forge Gradle plugin

## Platform Module Updates Required

### Forge Platform Updates: ✅ **COMPLETE**
- [x] Update `ForgeModMessages.java` to handle new packet handler signatures
- [x] Add `ClientHelper` implementations
- [x] Update event registration patterns
- [x] Ensure all PlatformAccess services are properly initialized
- [x] Add `ItemHandlerHelper` storage wrapper implementation

### Fabric Platform Updates: ⚠️ **PENDING**
- [ ] Implement proper packet handlers in Fabric counterparts
- [ ] Complete client helper implementations
- [ ] Fix PlatformAccess initialization (currently commented out)
- [ ] Implement Fabric-specific event handling
- [ ] Platform-specific initialization code
- [ ] Implement Phase 6 rendering abstractions for Fabric

## Testing & Validation Strategy

### Standard Testing Commands
```bash
# Build common module
wsl ./gradlew :common:build

# Build Forge module
wsl ./gradlew :forge:build

# Run Forge client
wsl ./gradlew :forge:runClient

# Verify functionality:
# - Mod loads without errors
# - Town Interface block places correctly
# - Right-click opens town interface screen
# - Packet communication works
# - Debug overlay works (F3+K)
# - Tourist spawning and platform system functional
# - All rendering works correctly
```

### Final Validation Tests
```bash
# Comprehensive end-to-end test
wsl ./gradlew :forge:runClient

# Test checklist:
# - [ ] World loads successfully
# - [ ] Town interface blocks place
# - [ ] Right-click opens town interface
# - [ ] All tabs work (Overview, Population, Resources, Settings)
# - [ ] Storage system functional (communal + personal)
# - [ ] Trade system works
# - [ ] Platform system operational
# - [ ] Tourist spawning works
# - [ ] Payment board accessible
# - [ ] Debug overlay toggles (F3+K)
# - [ ] All network packets functional
# - [ ] Save/load works correctly
# - [ ] No console errors or warnings
# - [ ] All rendering works correctly

# Test the complete decoupled system
wsl ./gradlew :common:build  # Platform-agnostic common
wsl ./gradlew :forge:build   # Forge with abstracted common
wsl ./gradlew :fabric:build  # Fabric with abstracted common (when implemented)
```

## Success Criteria & Completion Verification

### Forge Preservation (Primary Goal):
- [x] **All existing functionality works identically**
- [x] **No performance regression** (>5% acceptable)
- [x] **No new crashes or errors**
- [x] **Backward compatibility maintained** (save files, configs)
- [x] **All UI interactions work** (screens, modals, inputs)
- [x] **Network communication functional** (all packet types)
- [x] **Debug features work** (F3+K overlay, town data display)
- [ ] **All rendering works correctly** (Phase 6)

### Multi-Platform Readiness (Secondary Goal):
- [x] **Platform modules build independently** (Forge/Fabric)
- [ ] **Common code properly abstracted** (no direct platform calls) - ⚠️ Phase 6 remaining
- [x] **Clean abstraction separation** (platform code in platform modules)
- [x] **Fabric implementation path clear** (abstractions work for both)
- [ ] **Single JAR distribution** per platform (after Phase 6)

## Final Conclusion

### **Comprehensive Scope Summary:**
- **Total files requiring changes:** 107 files
- **Total estimated effort:** 6-8 weeks
- **Risk level:** High but manageable with incremental approach
- **Current completion:** ~90% (Phases 1-5 complete, Phase 6 remaining)

### **Critical Success Factors:**
1. **Incremental approach** - test after each change, never break working state
2. **Forge preservation** - primary goal, never compromise existing functionality
3. **Platform abstractions** - leverage existing PlatformAccess system properly
4. **Comprehensive testing** - verify all features work after each change

### **Current Architecture Status:**
- ✅ **Multi-module setup:** common + forge + fabric (working)
- ✅ **Forge functionality:** 100% preserved and tested
- ✅ **Decoupling plan:** Phases 1-5 completed successfully
- ✅ **Safety first:** No functionality broken during decoupling
- ✅ **Phase 1-5 Complete:** Network packets, screen opening, debug network, client UI, events, and item handlers decoupled
- ⚠️ **Phase 6 Remaining:** Client rendering, block entity capabilities, build.gradle

### **Next Steps:**
1. ✅ **Phase 1-5:** Complete decoupling while maintaining multi-module setup - **COMPLETED**
2. ⚠️ **Phase 6:** Remove remaining Forge dependencies (rendering, capabilities, build.gradle) - **IN PROGRESS**
3. **Post-Decoupling:** Transition to single-JAR-per-platform architecture
4. **Final:** Clean multi-platform codebase with simple distribution

### **Expected Outcome:**
This plan will transform your **working Forge mod** into a **properly abstracted multi-platform system** while **never breaking existing functionality**. After Phase 6 completion, all common module Forge dependencies will be removed, and the codebase will be ready for **Fabric support** and **simple single-JAR distribution**.

**✅ Phases 1-5 complete - ~90% platform-agnostic! Phase 6 remaining.** 🚀
