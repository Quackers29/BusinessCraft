# BusinessCraft - Forge + Fabric Compatibility Analysis

## Files to Change for Forge + Fabric Compatibility

### 🔴 Critical Changes Required (Must be done first)

#### Project Configuration
- [ ] `/build.gradle` - **MAJOR CHANGE** - Add Fabric/Architectury support, dual mod loader configuration
  - **Current**: Uses `net.minecraftforge.gradle` plugin, Forge dependencies
  - **Required**: Switch to Architectury/MultiLoader setup with separate modules (common, forge, fabric)
  - **Replace**: `minecraft 'net.minecraftforge:forge:1.20.1-47.1.0'` with platform-agnostic mappings
  - **Add**: Architectury transformer, platform-specific source sets
  
- [ ] `/src/main/resources/META-INF/mods.toml` - **REPLACE** - Create fabric.mod.json for Fabric
  - **Current**: Forge-specific `modLoader="javafml"`, `loaderVersion="[47,)"`
  - **Required**: Create separate `fabric.mod.json` with Fabric-specific format
  - **Migration**: Move mod metadata to platform-agnostic format
  
- [ ] **NEW FILE** - Create architectury-common configuration
  - **Required**: `architectury.common.json` for shared mod definition
  - **Required**: Platform-specific modules structure

#### Core Mod Class
- [ ] `/src/main/java/com/yourdomain/businesscraft/BusinessCraft.java` - **MAJOR CHANGE** - Platform abstraction layer
  - **Current**: `@Mod(BusinessCraft.MOD_ID)` annotation, `FMLJavaModLoadingContext`, `MinecraftForge.EVENT_BUS`
  - **Required**: Platform abstraction, move Forge-specific code to platform module
  - **Replace**: `IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus()` with platform-agnostic initialization
  - **Replace**: `MinecraftForge.EVENT_BUS.register(this)` with platform-specific event registration
  - **Split**: Move `@SubscribeEvent` methods to platform-specific classes

#### Registration System (Complete Overhaul)
- [ ] `/src/main/java/com/yourdomain/businesscraft/init/ModBlocks.java` - **MAJOR CHANGE** - Platform-agnostic registration
  - **Current**: `DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID)`
  - **Current**: `RegistryObject<Block> TOWN_BLOCK = registerBlock("town_block", TownBlock::new)`
  - **Required**: Replace with Architectury Registry API
  - **Replace**: `DeferredRegister` with `RegistrySupplier` from Architectury
  - **Replace**: `ForgeRegistries.BLOCKS` with `Registry.BLOCK`
  
- [ ] `/src/main/java/com/yourdomain/businesscraft/init/ModBlockEntities.java` - **MAJOR CHANGE** - Platform-agnostic registration  
  - **Current**: `DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES`
  - **Required**: Architectury Registry API for block entities
  - **Replace**: Forge-specific `BlockEntityType.Builder` with platform-agnostic version
  
- [ ] `/src/main/java/com/yourdomain/businesscraft/init/ModEntityTypes.java` - **MAJOR CHANGE** - Platform-agnostic registration
  - **Current**: `DeferredRegister<EntityType<?>> ENTITY_TYPES`
  - **Required**: Architectury Registry API for entities
  - **Replace**: Forge entity type registration with cross-platform equivalent
  
- [ ] `/src/main/java/com/yourdomain/businesscraft/init/ModMenuTypes.java` - **MAJOR CHANGE** - Platform-agnostic registration
  - **Current**: `DeferredRegister<MenuType<?>> MENU_TYPES`
  - **Required**: Architectury Registry API for menu types
  - **Replace**: Forge menu type registration with cross-platform equivalent

### 🟠 High Priority Changes

#### Event System (Complete Rewrite)
- [ ] `/src/main/java/com/yourdomain/businesscraft/event/ModEvents.java` - **MAJOR CHANGE** - Platform-specific event handling
  - **Current**: `@Mod.EventBusSubscriber`, `@SubscribeEvent`, `PlayerInteractEvent.RightClickBlock`
  - **Required**: Platform abstraction layer for events
  - **Replace**: Forge `@SubscribeEvent` with Architectury Event API
  - **Replace**: `PlayerInteractEvent.RightClickBlock` with cross-platform equivalent
  
- [ ] `/src/main/java/com/yourdomain/businesscraft/event/ClientModEvents.java` - **MAJOR CHANGE** - Client event abstraction
- [ ] `/src/main/java/com/yourdomain/businesscraft/event/ClientRenderEvents.java` - **MAJOR CHANGE** - Render event abstraction
- [ ] `/src/main/java/com/yourdomain/businesscraft/event/PlatformPathHandler.java` - **MODERATE CHANGE** - Event registration
- [ ] `/src/main/java/com/yourdomain/businesscraft/event/PlayerBoundaryTracker.java` - **MODERATE CHANGE** - Event registration

#### Network System (25 files - Complete Overhaul)
- [ ] `/src/main/java/com/yourdomain/businesscraft/network/ModMessages.java` - **MAJOR CHANGE** - Platform-agnostic networking
  - **Current**: `SimpleChannel INSTANCE`, `NetworkRegistry.ChannelBuilder`, `NetworkDirection.PLAY_TO_SERVER`
  - **Current**: `net.messageBuilder(SetTownNamePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)`
  - **Required**: Replace with Architectury Networking API
  - **Replace**: `SimpleChannel` with `NetworkManager` from Architectury
  - **Replace**: Forge packet registration with cross-platform equivalent
  - **Restructure**: Separate client/server packet handling

- [ ] **Platform Packets (9 files)** - **MODERATE CHANGE** - Replace Forge SimpleChannel with platform abstraction
  - **Current**: All packets use `NetworkEvent.Context`, `FriendlyByteBuf`, `Supplier<NetworkEvent.Context>`
  - **Required**: Update to Architectury networking patterns
  
- [ ] **Storage Packets (12 files)** - **MODERATE CHANGE** - Replace Forge SimpleChannel with platform abstraction  
- [ ] **Town Packets (2 files)** - **MODERATE CHANGE** - Replace Forge SimpleChannel with platform abstraction
  - **Example**: `SetTownNamePacket` extends `BaseBlockEntityPacket`, uses `NetworkEvent.Context`
  - **Required**: Update packet base class and context handling
  
- [ ] **UI Packets (14 files)** - **MODERATE CHANGE** - Replace Forge SimpleChannel with platform abstraction
- [ ] **Misc Packets (2 files)** - **MODERATE CHANGE** - Replace Forge SimpleChannel with platform abstraction

#### Client-Side Rendering
- [ ] `/src/main/java/com/yourdomain/businesscraft/client/ClientSetup.java` - **MAJOR CHANGE** - Platform-specific client initialization
  - **Current**: `@Mod.EventBusSubscriber(value = Dist.CLIENT)`, `FMLClientSetupEvent`, `EntityRenderersEvent.RegisterRenderers`
  - **Required**: Platform abstraction for client setup
  - **Replace**: `@Mod.EventBusSubscriber` with Architectury client setup
  - **Replace**: `EntityRenderersEvent.RegisterRenderers` with cross-platform renderer registration
  
- [ ] `/src/main/java/com/yourdomain/businesscraft/client/renderer/TouristRenderer.java` - **MINOR CHANGE** - Update imports
- [ ] `/src/main/java/com/yourdomain/businesscraft/client/renderer/layer/TouristHatLayer.java` - **MINOR CHANGE** - Update imports

### 🟡 Medium Priority Changes

#### Block System
- [ ] `/src/main/java/com/yourdomain/businesscraft/block/TownBlock.java` - **MINOR CHANGE** - Update imports only
- [ ] `/src/main/java/com/yourdomain/businesscraft/block/TownInterfaceBlock.java` - **MINOR CHANGE** - Update imports only
- [ ] `/src/main/java/com/yourdomain/businesscraft/block/entity/TownBlockEntity.java` - **MINOR CHANGE** - Update imports only

#### Entity System  
- [ ] `/src/main/java/com/yourdomain/businesscraft/entity/TouristEntity.java` - **MINOR CHANGE** - Update imports only

#### Menu System
- [ ] `/src/main/java/com/yourdomain/businesscraft/menu/PaymentBoardMenu.java` - **MINOR CHANGE** - Update imports
- [ ] `/src/main/java/com/yourdomain/businesscraft/menu/StorageMenu.java` - **MINOR CHANGE** - Update imports
- [ ] `/src/main/java/com/yourdomain/businesscraft/menu/TownBlockMenu.java` - **MINOR CHANGE** - Update imports
- [ ] `/src/main/java/com/yourdomain/businesscraft/menu/TownInterfaceMenu.java` - **MINOR CHANGE** - Update imports
- [ ] `/src/main/java/com/yourdomain/businesscraft/menu/TradeMenu.java` - **MINOR CHANGE** - Update imports

#### Configuration System
- [ ] `/src/main/java/com/yourdomain/businesscraft/config/ConfigLoader.java` - **MODERATE CHANGE** - Platform-specific config loading
- [ ] `/src/main/java/com/yourdomain/businesscraft/config/ConfigurationService.java` - **MODERATE CHANGE** - Platform-specific config paths

### 🟢 Low Priority Changes (Mostly Import Updates)

#### UI Framework (80+ files)
- [ ] **UI Components (25+ files)** - **MINOR CHANGE** - Import updates only
- [ ] **UI Builders (7 files)** - **MINOR CHANGE** - Import updates only  
- [ ] **UI Managers (16 files)** - **MINOR CHANGE** - Import updates only
- [ ] **UI Screens (10 files)** - **MINOR CHANGE** - Import updates only
- [ ] **UI State Management (8 files)** - **MINOR CHANGE** - Import updates only
- [ ] **UI Modal System (12 files)** - **MINOR CHANGE** - Import updates only
- [ ] **UI Layout System (4 files)** - **MINOR CHANGE** - Import updates only

#### Town Management System
- [ ] `/src/main/java/com/yourdomain/businesscraft/town/Town.java` - **MINOR CHANGE** - Import updates only
- [ ] `/src/main/java/com/yourdomain/businesscraft/town/TownManager.java` - **MINOR CHANGE** - Import updates only
- [ ] **Town Components (3 files)** - **MINOR CHANGE** - Import updates only
- [ ] **Town Data Management (15 files)** - **MINOR CHANGE** - Import updates only
- [ ] **Town Services (3 files)** - **MINOR CHANGE** - Import updates only

#### Platform System
- [ ] `/src/main/java/com/yourdomain/businesscraft/platform/Platform.java` - **MINOR CHANGE** - Import updates only

#### World Visualization System
- [ ] **Visualization Renderers (7 files)** - **MINOR CHANGE** - Import updates only

#### Client Handlers
- [ ] `/src/main/java/com/yourdomain/businesscraft/client/PlatformPathKeyHandler.java` - **MINOR CHANGE** - Import updates
- [ ] `/src/main/java/com/yourdomain/businesscraft/client/TownDebugKeyHandler.java` - **MINOR CHANGE** - Import updates
- [ ] `/src/main/java/com/yourdomain/businesscraft/client/TownDebugNetwork.java` - **MINOR CHANGE** - Import updates
- [ ] `/src/main/java/com/yourdomain/businesscraft/client/TownDebugOverlay.java` - **MINOR CHANGE** - Import updates

#### Utilities & Services
- [ ] **Error Handling System (5 files)** - **MINOR CHANGE** - Import updates only
- [ ] **Utilities (3 files)** - **MINOR CHANGE** - Import updates only
- [ ] **Data Management (3 files)** - **MINOR CHANGE** - Import updates only
- [ ] **Services (2 files)** - **MINOR CHANGE** - Import updates only

#### Commands
- [ ] `/src/main/java/com/yourdomain/businesscraft/command/ClearTownsCommand.java` - **MODERATE CHANGE** - Platform-specific command registration

### 🔵 Resource Files (No Changes Required)
- [ ] **Block Models & States (6 files)** - **NO CHANGE** - JSON files are platform-agnostic
- [ ] **Textures (6 files)** - **NO CHANGE** - PNG files are platform-agnostic
- [ ] **Data Files (6 files)** - **NO CHANGE** - JSON files are platform-agnostic (except fabric.mod.json needed)

---

## Summary of Changes Required

### Change Categories:
- **🔴 MAJOR CHANGE**: Complete rewrite/restructure required (25 files)
- **🟠 MODERATE CHANGE**: Significant modifications needed (15 files)  
- **🟡 MINOR CHANGE**: Import updates and small tweaks (150+ files)
- **🔵 NO CHANGE**: Platform-agnostic files (18 files)

### Key Architectural Changes Needed:

1. **Build System**: Switch to Architectury/MultiLoader setup
2. **Registration**: Platform abstraction layer for all registrations
3. **Events**: Complete event system abstraction
4. **Networking**: Replace Forge SimpleChannel with platform-agnostic system
5. **Client Setup**: Platform-specific client initialization
6. **Configuration**: Platform-specific config loading

### Estimated Work Distribution:
- **Critical Path**: 25 files requiring major changes
- **Import Updates**: 150+ files requiring minor import changes
- **New Files**: ~10 new abstraction layer files needed
- **Configuration**: New build scripts and mod metadata files

The majority of the business logic (Town management, UI framework, data structures) will remain unchanged, with only import statements and registration calls needing updates.