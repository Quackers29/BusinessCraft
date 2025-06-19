# BusinessCraft - Fabric Compatibility Analysis

## Overview
This document outlines the key changes required to make the BusinessCraft mod compatible with Fabric, in addition to its current Forge implementation. The mod is currently built specifically for Forge 1.20.1 and requires significant architectural changes to support both platforms.

## Core Architecture Changes

### 1. Platform Abstraction Layer
**Current Issue**: Direct Forge dependencies throughout the codebase
**Solution**: Create platform abstraction interfaces

#### Files to Create:
- `src/common/java/com/yourdomain/businesscraft/platform/Platform.java` - Abstract platform interface
- `src/forge/java/com/yourdomain/businesscraft/platform/ForgePlatform.java` - Forge implementation
- `src/fabric/java/com/yourdomain/businesscraft/platform/FabricPlatform.java` - Fabric implementation

#### Key Methods to Abstract:
```java
// Registration methods
void registerBlocks();
void registerBlockEntities();
void registerMenuTypes();
void registerEntityTypes();

// Network handling
void sendToServer(Object packet);
void sendToPlayer(Object packet, ServerPlayer player);

// Event handling
void registerClientEvents();
void registerServerEvents();
```

### 2. Build System Changes

#### Current: `build.gradle` (Forge-only)
```gradle
plugins {
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}
minecraft 'net.minecraftforge:forge:1.20.1-47.1.0'
```

#### Required: Multi-platform build system
- **Root Project**: `build.gradle` for common configuration
- **Common Module**: `common/build.gradle` for shared code
- **Forge Module**: `forge/build.gradle` for Forge-specific implementation
- **Fabric Module**: `fabric/build.gradle` for Fabric-specific implementation

### 3. Mod Loading and Initialization

#### Current Issue: Forge-specific mod class
**File**: `src/main/java/com/yourdomain/businesscraft/BusinessCraft.java`
**Problem**: Uses `@Mod` annotation and Forge event system

#### Solution: Platform-specific entry points
- **Common**: Abstract mod initializer
- **Forge**: Forge mod class extending common initializer
- **Fabric**: Fabric mod initializer implementing common interface

### 4. Registration System Overhaul

#### Current Files Requiring Changes:
- `src/main/java/com/yourdomain/businesscraft/init/ModBlocks.java`
- `src/main/java/com/yourdomain/businesscraft/init/ModBlockEntities.java`
- `src/main/java/com/yourdomain/businesscraft/init/ModMenuTypes.java`
- `src/main/java/com/yourdomain/businesscraft/init/ModEntityTypes.java`

#### Current Issue: DeferredRegister usage
```java
public static final DeferredRegister<Block> BLOCKS = 
    DeferredRegister.create(ForgeRegistries.BLOCKS, BusinessCraft.MOD_ID);
```

#### Solution: Platform-agnostic registration
```java
// Common interface
public interface RegistryHelper<T> {
    Supplier<T> register(String name, Supplier<T> supplier);
    void initialize();
}

// Forge implementation uses DeferredRegister
// Fabric implementation uses Registry.register
```

### 5. Networking System Replacement

#### Current Issue: Forge SimpleChannel
**Files affected**:
- `src/main/java/com/yourdomain/businesscraft/network/ModMessages.java`
- All packet classes in `src/main/java/com/yourdomain/businesscraft/network/packets/`

#### Current Pattern:
```java
SimpleChannel net = NetworkRegistry.ChannelBuilder
    .named(new ResourceLocation(BusinessCraft.MOD_ID, "messages"))
    .networkProtocolVersion(() -> "1.0")
    .simpleChannel();
```

#### Solution: Platform abstraction
- **Common**: Packet interface and registration helper
- **Forge**: SimpleChannel wrapper
- **Fabric**: ServerPlayNetworking/ClientPlayNetworking wrapper

### 6. Event System Transformation

#### Current Issue: Forge event bus system
**Files affected**:
- `src/main/java/com/yourdomain/businesscraft/event/ModEvents.java`
- `src/main/java/com/yourdomain/businesscraft/event/ClientModEvents.java`
- `src/main/java/com/yourdomain/businesscraft/event/PlatformPathHandler.java`

#### Current Pattern:
```java
@Mod.EventBusSubscriber
public class ModEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Event handling
    }
}
```

#### Solution: Platform event abstraction
- **Common**: Event callback interfaces
- **Forge**: Forge event wrapper
- **Fabric**: Fabric event callbacks

### 7. Capabilities System Replacement

#### Current Issue: Forge Capabilities
**File**: `src/main/java/com/yourdomain/businesscraft/block/entity/TownBlockEntity.java`
**Lines**: 42-46

```java
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
```

#### Solution: Platform storage abstraction
- **Common**: Storage interface
- **Forge**: Capability wrapper
- **Fabric**: Component/API implementation

### 8. Client-Side Registration

#### Current Issue: Forge client setup
**File**: `src/main/java/com/yourdomain/businesscraft/event/ClientModEvents.java`

```java
@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(ModMenuTypes.TOWN_INTERFACE.get(), TownInterfaceScreen::new);
    }
}
```

#### Solution: Platform client initialization
- **Common**: Client registration interface
- **Forge**: FML client setup wrapper
- **Fabric**: Client mod initializer

### 9. Resource Location Handling

#### Current Issue: Mixed usage of ResourceLocation
**Files**: Various throughout codebase using `new ResourceLocation()`

#### Solution: Consistent resource location utility
```java
public class ModResourceLocation {
    public static ResourceLocation of(String path) {
        return new ResourceLocation(BusinessCraft.MOD_ID, path);
    }
}
```

### 10. Menu and Screen System

#### Current Issue: Forge menu types
**File**: `src/main/java/com/yourdomain/businesscraft/init/ModMenuTypes.java`

```java
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
```

#### Solution: Platform menu abstraction
- **Common**: Menu factory interface
- **Forge**: IForgeMenuType wrapper
- **Fabric**: ScreenHandlerType wrapper

## Implementation Strategy

### Phase 1: Core Abstraction
1. Create platform abstraction interfaces
2. Restructure project into common/forge/fabric modules
3. Move shared code to common module

### Phase 2: Registration System
1. Abstract all DeferredRegister usage
2. Implement platform-specific registration helpers
3. Update all init classes

### Phase 3: Networking
1. Create packet abstraction layer
2. Implement platform-specific networking
3. Update all packet classes

### Phase 4: Events and Capabilities
1. Abstract event system
2. Replace capabilities with platform storage
3. Update block entities and other components

### Phase 5: Client Integration
1. Abstract client registration
2. Update screen and rendering code
3. Test UI functionality on both platforms

## Estimated Effort

### High Priority (Core functionality):
- **Registration System**: 40+ files to modify
- **Networking**: 30+ packet classes to abstract
- **Event System**: 15+ event handlers to convert

### Medium Priority (Features):
- **Block Entities**: 5+ files requiring capability replacement
- **Menu System**: 10+ menu and screen files
- **Client Rendering**: 20+ UI component files

### Low Priority (Polish):
- **Debug Systems**: Debug overlay and logging
- **Utility Classes**: Various helper and utility classes

## Dependencies for Fabric

### Required Fabric API Modules:
- `fabric-api-base`
- `fabric-networking-api-v1`
- `fabric-registry-sync-v0`
- `fabric-resource-loader-v0`
- `fabric-screen-api-v1`
- `fabric-block-api-v1`
- `fabric-item-api-v1`
- `fabric-entity-events-v1`

### Build Configuration:
```gradle
// fabric/build.gradle
dependencies {
    minecraft "com.mojang:minecraft:1.20.1"
    mappings "net.fabricmc:yarn:1.20.1+build.10:v2"
    modImplementation "net.fabricmc:fabric-loader:0.14.21"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.83.0+1.20.1"
}
```

## Conclusion

Converting BusinessCraft to support Fabric requires a complete architectural overhaul to abstract platform-specific functionality. The estimated effort is significant (100+ files requiring changes), but the result would be a truly multi-platform mod that can run on both Forge and Fabric mod loaders. The key is creating a robust abstraction layer that hides platform differences while maintaining full functionality on both platforms. 