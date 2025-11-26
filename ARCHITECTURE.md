# BusinessCraft Multi-Platform Architecture

## Overview

BusinessCraft is a multi-platform Minecraft mod supporting **Forge** and **Fabric**. The project uses a **three-module architecture** to maximize code reuse and minimize platform-specific complexity.

## Module Structure

```
BusinessCraft/
├── common/          # Platform-agnostic business logic
├── forge/           # Forge-specific implementations
└── fabric/          # Fabric-specific implementations
```

### Common Module

**Purpose**: Contains 95%+ of the mod's code in a platform-agnostic way.

**What belongs here:**
- **Business Logic**: Town management, resource systems, trading
- **Data Structures**: `Town`, `Platform`, `TouristEntity`, etc.
- **UI Components**: All screens, widgets, and rendering code
- **Network Packets**: Packet definitions and handling logic
- **Helper Interfaces**: `RegistryHelper`, `NetworkHelper`, `MenuHelper`, etc.
- **Utilities**: Math helpers, data cache, scoreboard management

**Key Pattern**: Use `PlatformAccess` service locator to call platform-specific functionality:
```java
// Instead of direct Forge/Fabric APIs
PlatformAccess.getRegistry().getItemKey(item);
PlatformAccess.getNetworkMessages().sendToServer(packet);
```

### Forge Module

**Purpose**: Thin wrapper providing Forge-specific implementations.

**What belongs here:**
- Entry point: `BusinessCraftForge.java`
- Platform implementations: `ForgeRegistryHelper`, `ForgeNetworkHelper`, etc.
- Event handling: `ForgeModEvents`, `ForgeClientEvents`
- Registration: `ForgeModBlocks`, `ForgeModBlockEntities`, `ForgeModMessages`

**Structure:**
```
forge/
└── src/main/java/.../forge/
    ├── BusinessCraftForge.java      # Entry point
    ├── init/                        # Registration
    ├── platform/                    # Helper implementations
    ├── event/                       # Event handlers
    └── network/                     # Network setup
```

### Fabric Module

**Purpose**: Thin wrapper providing Fabric-specific implementations.

**What belongs here:**
- Entry points: `BusinessCraftFabric.java`, `FabricClientSetup.java`
- Platform implementations: `FabricRegistryHelper`, `FabricNetworkHelper`, etc.
- Event handling: `FabricModEvents`
- Registration: `FabricModBlocks`, `FabricModBlockEntities`, `FabricModMessages`
- Platform-specific workarounds: `FabricTownInterfaceEntity` (for packet sync)

**Structure:**
```
fabric/
└── src/main/java/.../fabric/
    ├── BusinessCraftFabric.java     # Entry point
    ├── FabricClientSetup.java       # Client setup
    ├── init/                        # Registration
    ├── platform/                    # Helper implementations
    ├── event/                       # Event handlers
    ├── network/                     # Network setup
    └── block/entity/                # Platform-specific extensions
```

## Service Locator Pattern

### PlatformAccess

The `PlatformAccess` class is the **single point of access** from common code to platform-specific functionality.

**Location**: `common/src/.../api/PlatformAccess.java`

**Usage:**
```java
// Get platform name
String platform = PlatformAccess.getPlatform().getPlatformName();

// Register item
PlatformAccess.getRegistry().getItemKey(Items.BREAD);

// Send network packet
PlatformAccess.getNetworkMessages().sendToServer(new TradeResourcePacket(...));

// Client-side helpers (may be null on server)
if (PlatformAccess.getClient() != null) {
    Object screen = PlatformAccess.getClient().getCurrentScreen();
}
```

### Helper Interfaces

All platform abstractions are defined as interfaces in `common/src/.../api/`:

| Interface | Purpose |
|-----------|---------|
| `PlatformHelper` | Basic platform info (mod ID, platform name) |
| `RegistryHelper` | Block, item, entity registration |
| `NetworkHelper` | Packet handling and context access |
| `MenuHelper` | Screen factory registration |
| `EntityHelper` | Entity spawning and management |
| `BlockEntityHelper` | Block entity creation |
| `MenuTypeHelper` | Menu type creation |
| `ItemHandlerHelper` | Inventory capability abstraction |
| `NetworkMessages` | Packet sending (to server/client/players) |
| `ClientHelper` | Client-side operations (Minecraft instance, player, level) |
| `RenderHelper` | Rendering utilities |

**Implementation**: Each platform module provides concrete implementations in `platform/` directory.

## Rules and Best Practices

### ✅ DO

1. **Put business logic in `common`**
   ```java
   // common/src/.../town/Town.java
   public class Town {
       public void addResource(Item item, int count) {
           // Business logic here
       }
   }
   ```

2. **Use `PlatformAccess` for platform-specific needs**
   ```java
   // In common code
   ResourceLocation key = (ResourceLocation) PlatformAccess.getRegistry().getItemKey(item);
   ```

3. **Define interfaces in `common/api`**
   ```java
   // common/src/.../api/CustomHelper.java
   public interface CustomHelper {
       void doSomething();
   }
   ```

4. **Implement in platform modules**
   ```java
   // forge/src/.../platform/ForgeCustomHelper.java
   public class ForgeCustomHelper implements CustomHelper {
       @Override
       public void doSomething() {
           // Forge-specific implementation
       }
   }
   ```

5. **Initialize in platform entry point**
   ```java
   // forge/src/.../BusinessCraftForge.java
   PlatformAccess.customHelper = new ForgeCustomHelper();
   ```

### ❌ DON'T

1. **Duplicate data classes**
   ```java
   // ❌ BAD: Creating Platform.java in both common and fabric
   // ✅ GOOD: Only common/src/.../platform/Platform.java exists
   ```

2. **Duplicate interfaces**
   ```java
   // ❌ BAD: MenuHelper.java exists in both common/api and fabric/api
   // ✅ GOOD: Only common/src/.../api/MenuHelper.java exists
   ```

3. **Import platform packages in common**
   ```java
   // ❌ BAD: In common code
   import net.minecraftforge.common.ForgeHooks;
   
   // ✅ GOOD: Use PlatformAccess instead
   PlatformAccess.getEvents().someEventMethod();
   ```

4. **Put business logic in platform modules**
   ```java
   // ❌ BAD: In ForgeModEvents.java
   public static void onTownCreate(TownCreateEvent event) {
       // Complex town creation logic
   }
   
   // ✅ GOOD: In common/src/.../event/TownEventHandler.java
   public static void onTownCreate(Town town) {
       // Logic here
   }
   // Then call from ForgeModEvents
   ```

5. **Create unused stub files**
   ```java
   // ❌ BAD: Creating TouristUtils.java with placeholder methods
   public static boolean isValidTourist(Object entity) {
       return false; // Placeholder
   }
   
   // ✅ GOOD: Implement it properly or don't create it
   ```

## Platform-Specific Extensions

Sometimes platform modules need to extend common classes for platform-specific behavior.

**Example**: `FabricTownInterfaceEntity`

```java
// fabric/src/.../block/entity/FabricTownInterfaceEntity.java
public class FabricTownInterfaceEntity extends TownInterfaceEntity {
    // Override only when Fabric needs different behavior
    @Override
    public void setChanged() {
        super.setChanged();
        // Fabric-specific packet sending (if needed)
    }
}
```

**When to use:**
- Platform has fundamentally different behavior (rare)
- Workaround for platform-specific quirks
- Always extend common class, never duplicate

## Testing Checklist

When adding new features:

- [ ] Code added to `common` module (unless platform-specific)
- [ ] Used `PlatformAccess` for platform operations
- [ ] No platform-specific imports in common code
- [ ] Built successfully: `./gradlew :common:build :forge:build :fabric:build`
- [ ] Tested on **Forge**: Features work as expected
- [ ] Tested on **Fabric**: Features work as expected
- [ ] Data compatibility: Worlds created on one platform load on the other

## Common Pitfalls

### Issue: "Class not found" on one platform

**Cause**: Accidentally imported Forge/Fabric class in common code

**Fix**: Use `PlatformAccess` abstraction instead

### Issue: Feature works on Forge but not Fabric (or vice versa)

**Cause**: Platform-specific implementation missing or incorrect

**Fix**: Check helper implementations in both `forge/platform/` and `fabric/platform/`

### Issue: Duplicate class errors

**Cause**: Same class exists in both common and platform module

**Fix**: Delete platform duplicate, use common version

## Adding a New Platform (e.g., Quilt, NeoForge)

1. Create new module: `quilt/`
2. Implement all helpers from `common/api/`
3. Create entry point that initializes `PlatformAccess`
4. Register blocks, items, entities using Quilt APIs
5. Test against common codebase

The clean separation makes this straightforward!
