# Contributing to BusinessCraft

Thank you for your interest in contributing to BusinessCraft! This guide will help you understand the project structure and how to add new features correctly.

## Quick Start

1. **Read** [ARCHITECTURE.md](ARCHITECTURE.md) to understand the multi-platform design
2. **Fork** the repository
3. **Create** a feature branch: `git checkout -b feature/your-feature-name`
4. **Make** your changes following the guidelines below
5. **Test** on both Forge and Fabric
6. **Submit** a pull request

## Where to Put Your Code

### Rule of Thumb

**95% of code belongs in `common/`** — only create platform-specific code when absolutely necessary.

### Decision Tree

```
Does this code use Forge/Fabric-specific APIs?
│
├─ NO → Put in common/
│   └─ Use PlatformAccess for any platform operations
│
└─ YES → Is it implementing a helper interface?
    │
    ├─ YES → Put in forge/platform/ or fabric/platform/
    │
    └─ NO → Can it be abstracted?
        │
        ├─ YES → Define interface in common/api/, implement in platform/
        │
        └─ NO → Put in platform module with clear documentation
```

## Adding a New Feature

### Example: Adding a "Reputation" System

1. **Create data structure** in `common/`
   ```java
   // common/src/.../town/Reputation.java
   public class Reputation {
       private int level;
       private UUID playerId;
       
       public void increaseReputation(int amount) {
           this.level += amount;
       }
   }
   ```

2. **Add to Town class**
   ```java
   // common/src/.../town/Town.java
   public class Town {
       private Map<UUID, Reputation> reputations = new HashMap<>();
       
       public void addReputation(UUID playerId, int amount) {
           reputations.computeIfAbsent(playerId, Reputation::new)
                     .increaseReputation(amount);
       }
   }
   ```

3. **Create network packet** (if needed for sync)
   ```java
   // common/src/.../network/packets/ReputationSyncPacket.java
   public class ReputationSyncPacket {
       private UUID playerId;
       private int level;
       
       public void handle(Object context) {
           PlatformAccess.getNetwork().enqueueWork(context, () -> {
               // Handle packet
           });
       }
   }
   ```

4. **Register packet** in platform modules
   ```java
   // forge/src/.../ForgeModMessages.java
   CHANNEL.registerMessage(id++, ReputationSyncPacket.class,
       ReputationSyncPacket::encode,
       ReputationSyncPacket::decode,
       ReputationSyncPacket::handle);
   
   // fabric/src/.../FabricModMessages.java
   registerServerPacket("reputation_sync_packet", ReputationSyncPacket.class);
   ```

5. **Create UI screen** (in `common/`)
   ```java
   // common/src/.../ui/screens/ReputationScreen.java
   public class ReputationScreen extends BaseScreen {
       // UI implementation using platform-agnostic rendering
   }
   ```

6. **Test on both platforms**

## Using PlatformAccess

Never import Forge/Fabric classes in `common/`. Use `PlatformAccess` instead.

### Registry Operations
```java
// ❌ BAD
import net.minecraftforge.registries.ForgeRegistries;
ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);

// ✅ GOOD
ResourceLocation key = (ResourceLocation) PlatformAccess.getRegistry().getItemKey(item);
```

### Network Operations
```java
// ❌ BAD
import net.minecraftforge.network.NetworkDirection;
CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);

// ✅ GOOD
PlatformAccess.getNetworkMessages().sendToPlayer(packet, player);
```

### Client Operations
```java
// ❌ BAD
import net.minecraft.client.Minecraft;
Minecraft mc = Minecraft.getInstance();

// ✅ GOOD
ClientHelper client = PlatformAccess.getClient();
if (client != null) {  // Always check - null on server
    Object mcObj = client.getMinecraft();
}
```

## Adding a New Helper Interface

If `PlatformAccess` doesn't provide what you need:

1. **Define interface** in `common/src/.../api/`
   ```java
   // common/src/.../api/MyCustomHelper.java
   public interface MyCustomHelper {
       /**
        * Do something platform-specific
        */
       void doSomething(Object param);
   }
   ```

2. **Add to PlatformAccess**
   ```java
   // common/src/.../api/PlatformAccess.java
   public class PlatformAccess {
       public static MyCustomHelper customHelper;
       
       public static MyCustomHelper getCustomHelper() {
           if (customHelper == null) {
               throw new IllegalStateException("CustomHelper not initialized");
           }
           return customHelper;
       }
   }
   ```

3. **Implement for Forge**
   ```java
   // forge/src/.../platform/ForgeCustomHelper.java
   public class ForgeCustomHelper implements MyCustomHelper {
       @Override
       public void doSomething(Object param) {
           // Forge-specific implementation
       }
   }
   ```

4. **Implement for Fabric**
   ```java
   // fabric/src/.../platform/FabricCustomHelper.java
   public class FabricCustomHelper implements MyCustomHelper {
       @Override
       public void doSomething(Object param) {
           // Fabric-specific implementation
       }
   }
   ```

5. **Initialize in platform entry points**
   ```java
   // forge/src/.../BusinessCraftForge.java
   PlatformAccess.customHelper = new ForgeCustomHelper();
   
   // fabric/src/.../BusinessCraftFabric.java
   PlatformAccess.customHelper = new FabricCustomHelper();
   ```

## Code Style

- **Formatting**: Use 4 spaces for indentation
- **Naming**: `camelCase` for variables/methods, `PascalCase` for classes
- **Comments**: JavaDoc for public APIs, inline comments for complex logic
- **Logging**: Use `DebugConfig.debug()` for development logging

## Testing Checklist

Before submitting a PR, verify:

- [ ] **Build**: `./gradlew :common:build :forge:build :fabric:build` succeeds
- [ ] **No platform imports in common**: `grep -r "import.*forge\." common/src` returns nothing
- [ ] **Forge works**: Run `./gradlew :forge:runClient` and test your feature
- [ ] **Fabric works**: Run `./gradlew :fabric:runClient` and test your feature
- [ ] **Data compatibility**: Create world on Forge, load on Fabric (and vice versa)
- [ ] **No duplicates**: Your changes don't duplicate existing common code in platform modules

## Common Mistakes

### 1. Duplicating Data Classes

```java
// ❌ WRONG: Creating Platform.java in fabric/
// common/src/.../platform/Platform.java (exists)
// fabric/src/.../platform/Platform.java (DUPLICATE!)

// ✅ CORRECT: Use common version everywhere
import com.quackers29.businesscraft.platform.Platform;
```

### 2. Platform-Specific Logic in Common

```java
// ❌ WRONG
public void registerBlock() {
    if (FMLLoader.getDist() == Dist.CLIENT) {  // Forge-specific!
        // ...
    }
}

// ✅ CORRECT
public void registerBlock() {
    if (PlatformAccess.getPlatform().isClientSide()) {
        // ...
    }
}
```

### 3. Not Testing Both Platforms

Always test on **both** Forge and Fabric, even for "simple" changes!

## Debug Logging

Use the `DebugConfig` system:

```java
// In your code
DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, 
    "Town {} added resource {}", townName, itemName);

// Enable/disable in DebugConfig.java
public static final boolean TOWN_BLOCK_ENTITY = true;  // Show logs
public static final boolean TOWN_BLOCK_ENTITY = false; // Hide logs
```

## Getting Help

- **Architecture questions**: Read [ARCHITECTURE.md](ARCHITECTURE.md)
- **Build issues**: Check GitHub Issues
- **Feature discussion**: Open a discussion on GitHub

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see [LICENSE](LICENSE)).
