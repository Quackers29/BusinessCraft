# BusinessCraft Fabric Implementation Plan

## Executive Summary
**Objective:** Implement complete Fabric support for BusinessCraft, replicating all Forge functionality with 100% feature parity - no loss, no extras.

### Key Metrics
- **Forge implementations:** 11 platform helpers + 4 init classes + 3 event handlers + 1 network registration
- **Current Fabric status:** Infrastructure exists but incomplete implementations
- **Target:** Full feature parity with Forge version
- **Success criteria:** All BusinessCraft features work identically on Fabric

### Current Status
- ✅ **Common module:** Fully platform-agnostic (~95% complete)
- ✅ **Forge module:** Fully functional (~100% complete)
- ⚠️ **Fabric module:** Partial infrastructure (~30% complete)
  - Platform helpers exist but are mostly placeholders
  - PlatformAccess initialization commented out
  - Event handlers incomplete
  - Network registration incomplete
  - Client helpers incomplete
  - Render helpers missing

## Implementation Phases

### Phase 1: Platform Helpers - Core Infrastructure ✅ **PLANNED**
**Status:** ⚠️ **PLACEHOLDERS EXIST, NEED FULL IMPLEMENTATION**

**Objective:** Complete all platform helper implementations to match Forge functionality

#### 1.1: PlatformHelper ✅ **COMPLETE**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricPlatformHelper.java`
- **Status:** ✅ Already implemented
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgePlatformHelper.java`
- **Actions Required:** None - already complete

#### 1.2: RegistryHelper ⚠️ **NEEDS VERIFICATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricRegistryHelper.java`
- **Status:** ⚠️ Implementation exists, needs verification against Forge version
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeRegistryHelper.java`
- **Actions Required:**
  - [ ] Verify all registry methods match Forge implementation
  - [ ] Ensure `getItem()`, `getBlock()`, `getEntityType()`, `getBlockEntityType()`, `getMenuType()` work correctly
  - [ ] Test registry lookups return correct types

#### 1.3: NetworkHelper ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricNetworkHelper.java`
- **Status:** ⚠️ Partial implementation exists
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeNetworkHelper.java`
- **Fabric API:** Uses Fabric's `ServerPlayNetworking` and `ClientPlayNetworking`
- **Actions Required:**
  - [ ] Implement `SimpleChannel` equivalent using Fabric networking
  - [ ] Implement `registerMessage()` method
  - [ ] Implement `sendToPlayer()`, `sendToAllPlayers()`, `sendToServer()`, `sendToAllTrackingChunk()`
  - [ ] Implement context handling: `enqueueWork()`, `getSender()`, `setPacketHandled()`
  - [ ] Implement `openScreen()` methods for menu opening
  - [ ] Map Fabric's `PacketContext` to platform-agnostic Object handling

#### 1.4: EventHelper ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricEventHelper.java`
- **Status:** ⚠️ Placeholder methods exist
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeEventHelper.java`
- **Fabric API:** Uses Fabric's event system (`ServerTickEvents`, `PlayerEvent`, `WorldRenderEvents`, etc.)
- **Actions Required:**
  - [ ] Implement `registerModEvent()` and `registerPlatformEvent()` (if applicable)
  - [ ] Implement server lifecycle callbacks using `ServerLifecycleEvents`
  - [ ] Implement level unload callbacks using `WorldUnloadEvents`
  - [ ] Implement all event callback registrations:
    - [ ] `registerPlayerTickCallback()` → `ServerTickEvents.PLAYER_TICK`
    - [ ] `registerPlayerLoginCallback()` → `PlayerEvent.PLAYER_JOIN`
    - [ ] `registerPlayerLogoutCallback()` → `PlayerEvent.PLAYER_DISCONNECT`
    - [ ] `registerRightClickBlockCallback()` → `PlayerBlockBreakEvents.BEFORE`
    - [ ] `registerClientTickCallback()` → `ClientTickEvents.END_CLIENT_TICK`
    - [ ] `registerKeyInputCallback()` → `ClientTickEvents.END_CLIENT_TICK` + key bindings
    - [ ] `registerMouseScrollCallback()` → `MouseEvents.SCROLL`
    - [ ] `registerRenderLevelCallback()` → `WorldRenderEvents.*`
    - [ ] `registerLevelUnloadCallback()` → `WorldUnloadEvents.UNLOAD`
  - [ ] Implement `setActiveTownBlock()` and `clearActiveTownBlock()` state management

#### 1.5: MenuHelper ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricMenuHelper.java`
- **Status:** ⚠️ Placeholder exists
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeMenuHelper.java`
- **Actions Required:**
  - [ ] Implement `registerScreenFactory()` method
  - [ ] Store screen factories for client-side registration
  - [ ] Map to Fabric's screen registration system

#### 1.6: EntityHelper ✅ **COMPLETE**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricEntityHelper.java`
- **Status:** ✅ Empty implementation - no methods needed (matches Forge)
- **Actions Required:** None

#### 1.7: BlockEntityHelper ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricBlockEntityHelper.java`
- **Status:** ⚠️ Empty implementation
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeBlockEntityHelper.java`
- **Actions Required:**
  - [ ] Implement `getTownInterfaceEntityType()` method
  - [ ] Return `FabricModBlockEntities.TOWN_INTERFACE_ENTITY`

#### 1.8: MenuTypeHelper ⚠️ **NEEDS VERIFICATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricMenuTypeHelper.java`
- **Status:** ⚠️ Implementation exists, needs verification
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeMenuTypeHelper.java`
- **Actions Required:**
  - [ ] Verify all menu type getters return correct types
  - [ ] Ensure `getTownInterfaceMenuType()`, `getTradeMenuType()`, `getStorageMenuType()`, `getPaymentBoardMenuType()` work correctly

#### 1.9: ItemHandlerHelper ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricItemHandlerHelper.java`
- **Status:** ⚠️ Partial implementation exists
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeItemHandlerHelper.java`
- **Fabric API:** Uses Fabric's `SimpleInventory` or similar storage abstraction
- **Actions Required:**
  - [ ] Implement `createItemStackHandler()` - create Fabric equivalent of `ItemStackHandler`
  - [ ] Implement `createLazyOptional()` - Fabric doesn't use `LazyOptional`, may need wrapper
  - [ ] Implement `isItemHandlerCapability()` - Fabric uses different capability system
  - [ ] Implement `getEmptyLazyOptional()` - return empty wrapper
  - [ ] Implement `castLazyOptional()` - Fabric capability casting
  - [ ] Implement `createCustomItemStackHandler()` - with change callback support
  - [ ] Implement `invalidateLazyOptional()` - cleanup wrapper
  - [ ] Implement `serializeItemHandler()` and `deserializeItemHandler()` - NBT serialization
  - [ ] Implement `createStorageWrapper()` - wrapper for `SlotBasedStorage`

#### 1.10: ClientHelper ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricClientHelper.java`
- **Status:** ⚠️ Partial implementation exists
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeClientHelper.java`
- **Fabric API:** Uses `MinecraftClient.getInstance()` (Fabric's client access)
- **Actions Required:**
  - [ ] Implement `getMinecraft()` - return `MinecraftClient.getInstance()`
  - [ ] Implement `getClientLevel()` - return client world
  - [ ] Implement `getCurrentScreen()` - return current screen
  - [ ] Implement `getFont()` - return text renderer
  - [ ] Implement `executeOnClientThread()` - use `MinecraftClient.execute()`
  - [ ] Implement `isOnClientThread()` - check thread
  - [ ] Implement `getSoundManager()` - return sound manager
  - [ ] Implement `getClientPlayer()` - return client player

#### 1.11: RenderHelper ❌ **MISSING**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricRenderHelper.java` *(NEW)*
- **Status:** ❌ **NOT IMPLEMENTED**
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeRenderHelper.java`
- **Fabric API:** Uses `WorldRenderEvents` and `InGameHud` for rendering
- **Actions Required:**
  - [ ] Create `FabricRenderHelper` class implementing `RenderHelper` interface
  - [ ] Implement `registerOverlay()` - use Fabric's `InGameHud` overlay registration
  - [ ] Implement `unregisterOverlay()` - remove overlay from registry
  - [ ] Implement `registerWorldRenderCallback()` - use `WorldRenderEvents` callbacks
  - [ ] Implement `getPoseStack()`, `getCamera()`, `getPartialTick()` - extract from Fabric render events
  - [ ] Implement `getRenderStage()` - map Fabric render stages to platform-agnostic constants
  - [ ] Implement `isRenderStage()` - check stage matching
  - [ ] Create `FabricWorldRenderHandler` similar to Forge's static handler
  - [ ] Create `FabricOverlayRenderer` for GUI overlay rendering

#### 1.12: NetworkMessages ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/platform/FabricNetworkMessages.java`
- **Status:** ⚠️ Partial implementation exists
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/ForgeNetworkMessages.java`
- **Actions Required:**
  - [ ] Implement `sendToServer()` method
  - [ ] Implement `sendToPlayer()` method
  - [ ] Implement `sendToAllPlayers()` method
  - [ ] Implement `sendToAllTrackingChunk()` method

### Phase 2: Initialization & Registration ✅ **PLANNED**
**Status:** ⚠️ **INFRASTRUCTURE EXISTS, NEEDS COMPLETION**

#### 2.1: Main Mod Class ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/BusinessCraftFabric.java`
- **Status:** ⚠️ Basic structure exists, PlatformAccess commented out
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/BusinessCraftForge.java`
- **Actions Required:**
  - [ ] Uncomment PlatformAccess initialization
  - [ ] Initialize all platform helpers
  - [ ] Register blocks, entities, block entities, menu types
  - [ ] Register network messages
  - [ ] Register event handlers
  - [ ] Implement server lifecycle event handlers:
    - [ ] `ServerLifecycleEvents.SERVER_STARTING` → initialize TownManager
    - [ ] `ServerLifecycleEvents.SERVER_STOPPING` → save town data, clear instances
    - [ ] `ServerLifecycleEvents.SERVER_STARTED` → log loaded towns
  - [ ] Implement level unload handler → clear tracked vehicles
  - [ ] Register command dispatcher → `ClearTownsCommand`

#### 2.2: Block Registration ⚠️ **NEEDS VERIFICATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/init/FabricModBlocks.java`
- **Status:** ⚠️ Implementation exists, needs verification
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/init/ForgeModBlocks.java`
- **Actions Required:**
  - [ ] Verify `TOWN_INTERFACE_BLOCK` registration
  - [ ] Verify `TOWN_INTERFACE_BLOCK_ITEM` registration
  - [ ] Ensure creative tab registration works

#### 2.3: Entity Registration ⚠️ **NEEDS VERIFICATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/init/FabricModEntityTypes.java`
- **Status:** ⚠️ Implementation exists, needs verification
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/init/ForgeModEntityTypes.java`
- **Actions Required:**
  - [ ] Verify `TOURIST_ENTITY` registration
  - [ ] Ensure entity spawn egg registration works

#### 2.4: Block Entity Registration ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/init/FabricModBlockEntities.java`
- **Status:** ⚠️ Implementation exists, needs Fabric-specific block entity
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/init/ForgeModBlockEntities.java`
- **Actions Required:**
  - [ ] Create `FabricTownInterfaceEntity` extending `TownInterfaceEntity` (similar to `ForgeTownInterfaceEntity`)
  - [ ] Override `getCapability()` with Fabric's capability system (if applicable)
  - [ ] Register `FabricTownInterfaceEntity` instead of `TownInterfaceEntity`

#### 2.5: Menu Type Registration ⚠️ **NEEDS VERIFICATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/init/FabricModMenuTypes.java`
- **Status:** ⚠️ Implementation exists but commented out
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/init/ForgeModMenuTypes.java`
- **Actions Required:**
  - [ ] Uncomment menu type registration
  - [ ] Verify all menu types register correctly:
    - [ ] `TOWN_INTERFACE_MENU`
    - [ ] `TRADE_MENU`
    - [ ] `STORAGE_MENU`
    - [ ] `PAYMENT_BOARD_MENU`

### Phase 3: Network System ✅ **PLANNED**
**Status:** ⚠️ **INFRASTRUCTURE EXISTS, NEEDS COMPLETION**

#### 3.1: Network Message Registration ❌ **NEEDS CREATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/network/FabricModMessages.java` *(NEW)*
- **Status:** ❌ **NOT IMPLEMENTED**
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/network/ForgeModMessages.java`
- **Fabric API:** Uses `ServerPlayNetworking.registerReceiver()` and `ClientPlayNetworking.registerReceiver()`
- **Actions Required:**
  - [ ] Create `FabricModMessages` class
  - [ ] Register all 39+ packets using Fabric's networking API:
    - [ ] Town packets: `ToggleTouristSpawningPacket`, `SetTownNamePacket`
    - [ ] Platform packets: `SetSearchRadiusPacket`, `AddPlatformPacket`, `DeletePlatformPacket`, `SetPlatformEnabledPacket`, `SetPlatformPathPacket`, `ResetPlatformPathPacket`, `SetPlatformPathCreationModePacket`, `RefreshPlatformsPacket`, `SetPlatformDestinationPacket`
    - [ ] UI packets: `SetPathCreationModePacket`, `OpenDestinationsUIPacket`, `RefreshDestinationsPacket`, `PlayerExitUIPacket`, `PlatformVisualizationPacket`, `BoundarySyncRequestPacket`, `BoundarySyncResponsePacket`, `OpenTownInterfacePacket`, `OpenPaymentBoardPacket`, `RequestTownMapDataPacket`, `TownMapDataResponsePacket`, `RequestTownPlatformDataPacket`, `TownPlatformDataResponsePacket`
    - [ ] Storage packets: `TradeResourcePacket`, `CommunalStoragePacket`, `CommunalStorageResponsePacket`, `PaymentBoardResponsePacket`, `PaymentBoardRequestPacket`, `PaymentBoardClaimPacket`, `BufferStoragePacket`, `BufferStorageResponsePacket`, `BufferSlotStorageResponsePacket`, `PersonalStoragePacket`, `PersonalStorageRequestPacket`, `PersonalStorageResponsePacket`
    - [ ] Misc packets: `PaymentResultPacket`
    - [ ] Debug packets: `RequestTownDataPacket`, `TownDataResponsePacket`
  - [ ] Implement packet handler wrappers that convert Fabric's `PacketContext` to platform-agnostic Object
  - [ ] Ensure all packets call `handle(Object context)` correctly
  - [ ] Map Fabric's `PacketContext` to platform-agnostic context handling

### Phase 4: Event System ✅ **PLANNED**
**Status:** ⚠️ **INFRASTRUCTURE EXISTS, NEEDS COMPLETION**

#### 4.1: Event Callback Handler ❌ **NEEDS CREATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/event/FabricEventCallbackHandler.java` *(NEW)*
- **Status:** ❌ **NOT IMPLEMENTED**
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/event/ForgeEventCallbackHandler.java`
- **Fabric API:** Uses Fabric's event system (`ServerTickEvents`, `PlayerEvent`, `WorldRenderEvents`, etc.)
- **Actions Required:**
  - [ ] Create `FabricEventCallbackHandler` class
  - [ ] Implement callback storage (similar to Forge's static handlers)
  - [ ] Implement server-side tick handler → `ServerTickEvents.PLAYER_TICK`
  - [ ] Implement player login handler → `PlayerEvent.PLAYER_JOIN`
  - [ ] Implement player logout handler → `PlayerEvent.PLAYER_DISCONNECT`
  - [ ] Implement right-click block handler → `PlayerBlockBreakEvents.BEFORE` or similar
  - [ ] Implement client tick handler → `ClientTickEvents.END_CLIENT_TICK`
  - [ ] Implement key input handler → `ClientTickEvents.END_CLIENT_TICK` + key bindings
  - [ ] Implement mouse scroll handler → `MouseEvents.SCROLL`
  - [ ] Implement render level handler → `WorldRenderEvents.*` callbacks
  - [ ] Implement level unload handler → `WorldUnloadEvents.UNLOAD`
  - [ ] Ensure all callbacks are properly registered and invoked

#### 4.2: Mod Events ⚠️ **NEEDS VERIFICATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/event/FabricModEvents.java`
- **Status:** ⚠️ Implementation exists, needs verification
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/event/ForgeModEvents.java`
- **Actions Required:**
  - [ ] Verify event handlers match Forge functionality
  - [ ] Ensure `setActiveTownBlock()` and `clearActiveTownBlock()` work correctly

#### 4.3: Client Mod Events ❌ **NEEDS CREATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/event/FabricClientModEvents.java` *(NEW)*
- **Status:** ❌ **NOT IMPLEMENTED**
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/event/ForgeClientModEvents.java`
- **Actions Required:**
  - [ ] Create `FabricClientModEvents` class
  - [ ] Register client-side event handlers
  - [ ] Implement screen registration callbacks

### Phase 5: Client Setup ✅ **PLANNED**
**Status:** ⚠️ **INFRASTRUCTURE EXISTS, NEEDS COMPLETION**

#### 5.1: Client Setup ⚠️ **NEEDS COMPLETION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/client/FabricClientSetup.java`
- **Status:** ⚠️ Partial implementation exists
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/client/ForgeClientSetup.java`
- **Actions Required:**
  - [ ] Initialize `PlatformAccess.client` and `PlatformAccess.render`
  - [ ] Register screen factories for all menus
  - [ ] Initialize client-side event handlers:
    - [ ] `ClientRenderEvents.initialize()`
    - [ ] `TownDebugOverlay.initialize()`
    - [ ] `TownDebugKeyHandler.initialize()`
    - [ ] `PlatformPathKeyHandler.initialize()`
  - [ ] Register key bindings (F3+K for debug overlay, etc.)
  - [ ] Ensure render helper is initialized before overlay registration

### Phase 6: Block Entity Capabilities ✅ **PLANNED**
**Status:** ⚠️ **NEEDS IMPLEMENTATION**

#### 6.1: Fabric Town Interface Entity ❌ **NEEDS CREATION**
- **File:** `fabric/src/main/java/com/quackers29/businesscraft/fabric/block/entity/FabricTownInterfaceEntity.java` *(NEW)*
- **Status:** ❌ **NOT IMPLEMENTED**
- **Forge Reference:** `forge/src/main/java/com/quackers29/businesscraft/forge/block/entity/ForgeTownInterfaceEntity.java`
- **Fabric API:** Fabric uses different capability system (may use `Storage` or direct method calls)
- **Actions Required:**
  - [ ] Create `FabricTownInterfaceEntity` extending `TownInterfaceEntity`
  - [ ] Implement Fabric-specific capability handling (if applicable)
  - [ ] Bridge to Fabric's item handler system
  - [ ] Ensure hopper extraction works correctly

### Phase 7: Testing & Validation ✅ **PLANNED**
**Status:** ⚠️ **NOT STARTED**

#### 7.1: Build Testing
- [ ] Verify Fabric module builds successfully
- [ ] Verify common module still builds correctly
- [ ] Ensure no circular dependencies

#### 7.2: Runtime Testing
- [ ] Mod loads without errors
- [ ] Town Interface block places correctly
- [ ] Right-click opens town interface screen
- [ ] All packet communication works
- [ ] Debug overlay works (F3+K)
- [ ] Tourist spawning and platform system functional
- [ ] All rendering works correctly (platform/boundary visualizations)
- [ ] Storage system works (communal + personal)
- [ ] Trade system works
- [ ] Payment board accessible
- [ ] Save/load works correctly
- [ ] No console errors or warnings

#### 7.3: Feature Parity Verification
- [ ] Compare feature list with Forge version
- [ ] Verify no features are missing
- [ ] Verify no extra features were added
- [ ] Test all UI interactions
- [ ] Test all network packets
- [ ] Test all event handlers

## Implementation Checklist

### Platform Helpers (11 total)
- [x] PlatformHelper ✅ Complete
- [ ] RegistryHelper ⚠️ Needs verification
- [ ] NetworkHelper ⚠️ Needs completion
- [ ] EventHelper ⚠️ Needs completion
- [ ] MenuHelper ⚠️ Needs completion
- [x] EntityHelper ✅ Complete (empty - matches Forge)
- [ ] BlockEntityHelper ⚠️ Needs completion
- [ ] MenuTypeHelper ⚠️ Needs verification
- [ ] ItemHandlerHelper ⚠️ Needs completion
- [ ] ClientHelper ⚠️ Needs completion
- [ ] RenderHelper ❌ Missing

### Initialization & Registration (5 files)
- [ ] BusinessCraftFabric ⚠️ Needs PlatformAccess initialization
- [ ] FabricModBlocks ⚠️ Needs verification
- [ ] FabricModEntityTypes ⚠️ Needs verification
- [ ] FabricModBlockEntities ⚠️ Needs FabricTownInterfaceEntity
- [ ] FabricModMenuTypes ⚠️ Needs uncommenting + verification

### Network System (1 file)
- [ ] FabricModMessages ❌ Missing - needs creation

### Event System (3 files)
- [ ] FabricEventCallbackHandler ❌ Missing - needs creation
- [ ] FabricModEvents ⚠️ Needs verification
- [ ] FabricClientModEvents ❌ Missing - needs creation

### Client Setup (1 file)
- [ ] FabricClientSetup ⚠️ Needs completion

### Block Entity (1 file)
- [ ] FabricTownInterfaceEntity ❌ Missing - needs creation

## Testing Strategy

### Build Commands
```bash
# Build common module
./gradlew :common:build

# Build Fabric module
./gradlew :fabric:build

# Run Fabric client
./gradlew :fabric:runClient
```

### Feature Testing Checklist
- [ ] Mod initialization
- [ ] Block placement
- [ ] Screen opening
- [ ] Network packets (all 39+)
- [ ] Debug overlay (F3+K)
- [ ] Tourist spawning
- [ ] Platform system
- [ ] Storage system
- [ ] Trade system
- [ ] Payment board
- [ ] Rendering (platform/boundary visualizations)
- [ ] Save/load
- [ ] Event handlers

## Success Criteria

### Primary Goal: Feature Parity
- ✅ All Forge features work identically on Fabric
- ✅ No features are missing
- ✅ No extra features were added
- ✅ Performance is comparable (>5% difference acceptable)

### Secondary Goal: Code Quality
- ✅ No code duplication between Forge and Fabric implementations
- ✅ Platform abstractions are properly used
- ✅ Common module remains platform-agnostic
- ✅ Clean separation of platform-specific code

## Next Steps

1. **Start with Phase 1:** Complete all platform helper implementations
2. **Verify PlatformAccess:** Uncomment and initialize in `BusinessCraftFabric`
3. **Complete Network System:** Create `FabricModMessages` and register all packets
4. **Complete Event System:** Create `FabricEventCallbackHandler` and register all events
5. **Complete Client Setup:** Finish `FabricClientSetup` initialization
6. **Create Block Entity:** Implement `FabricTownInterfaceEntity`
7. **Test Thoroughly:** Run comprehensive feature parity tests

## Notes

- **Fabric API Differences:** Fabric uses different networking, event, and capability systems than Forge
- **Capability System:** Fabric may not have a direct equivalent to Forge's capability system - may need wrapper or different approach
- **Event System:** Fabric's event system is callback-based rather than annotation-based
- **Networking:** Fabric uses `ServerPlayNetworking` and `ClientPlayNetworking` instead of `SimpleChannel`
- **Rendering:** Fabric uses `WorldRenderEvents` and `InGameHud` instead of `RenderLevelStageEvent` and `IGuiOverlay`
- **Menu Opening:** Fabric uses different screen opening mechanism than Forge's `NetworkHooks.openScreen()`

## References

- **Forge Implementations:** `forge/src/main/java/com/quackers29/businesscraft/forge/platform/`
- **Common Interfaces:** `common/src/main/java/com/quackers29/businesscraft/api/`
- **Fabric API Documentation:** https://fabricmc.net/wiki/
- **Forge API Documentation:** https://docs.minecraftforge.net/

