# BusinessCraft - Minecraft 1.12 Downgrade Analysis

## Overview
This document outlines the comprehensive changes required to port BusinessCraft from Minecraft 1.20.1 to Minecraft 1.12. This involves significant API changes, rendering system updates, and structural modifications due to the 8-year gap between these versions.

## Major Version Differences

### Java Version Compatibility
- **1.20.1**: Requires Java 17+
- **1.12**: Requires Java 8
- **Impact**: All code must be compatible with Java 8 language features only

### Forge Version Changes
- **Current**: Forge 47.1.0 for MC 1.20.1
- **Target**: Forge 14.23.5.2859 for MC 1.12.2
- **Impact**: Completely different modding APIs and registration systems

## Critical System Changes

### 1. Registration System Overhaul

#### Current 1.20.1 System (`ModBlocks.java`):
```java
public static final DeferredRegister<Block> BLOCKS = 
    DeferredRegister.create(ForgeRegistries.BLOCKS, BusinessCraft.MOD_ID);

public static final RegistryObject<Block> TOWN_BLOCK = registerBlock("town_block", TownBlock::new);
```

#### 1.12 Equivalent:
```java
@GameRegistry.ObjectHolder(BusinessCraft.MOD_ID)
public static class ModBlocks {
    public static final Block TOWN_BLOCK = null; // Populated by @ObjectHolder
}

// In main mod class
@EventHandler
public void preInit(FMLPreInitializationEvent event) {
    GameRegistry.register(new TownBlock().setRegistryName("town_block"));
    GameRegistry.register(new ItemBlock(ModBlocks.TOWN_BLOCK).setRegistryName("town_block"));
}
```

#### Files Requiring Registration Changes:
- `src/main/java/com/yourdomain/businesscraft/init/ModBlocks.java`
- `src/main/java/com/yourdomain/businesscraft/init/ModBlockEntities.java` (becomes TileEntities)
- `src/main/java/com/yourdomain/businesscraft/init/ModMenuTypes.java` (containers work differently)
- `src/main/java/com/yourdomain/businesscraft/init/ModEntityTypes.java`

### 2. Block Entity → Tile Entity Conversion

#### Current Issue: BlockEntity API doesn't exist in 1.12
**File**: `src/main/java/com/yourdomain/businesscraft/block/entity/TownBlockEntity.java`

#### Major Changes Required:
```java
// 1.20.1 (Current)
public class TownBlockEntity extends BlockEntity {
    public TownBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWN_BLOCK_ENTITY.get(), pos, state);
    }
}

// 1.12 (Required)
public class TileEntityTownBlock extends TileEntity {
    public TileEntityTownBlock() {
        // No position/state in constructor
    }
}
```

#### Registration Changes:
```java
// 1.20.1
ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

// 1.12
GameRegistry.registerTileEntity(TileEntityTownBlock.class, "town_block");
```

### 3. NBT System Changes

#### Current Issue: CompoundTag doesn't exist in 1.12
**Files affected**: All files using NBT operations

#### API Changes:
```java
// 1.20.1 (Current)
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public void save(CompoundTag tag) {
    tag.putString("name", name);
    tag.putInt("value", value);
}

// 1.12 (Required)
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public NBTTagCompound writeToNBT(NBTTagCompound compound) {
    compound.setString("name", name);
    compound.setInteger("value", value);
    return compound;
}
```

### 4. Rendering System Complete Rewrite

#### Current Issue: GuiGraphics doesn't exist in 1.12
**Files requiring major rendering changes**:
- All files in `src/main/java/com/yourdomain/businesscraft/ui/`
- All GUI and screen components

#### Current 1.20.1 Pattern:
```java
public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    guiGraphics.drawString(font, text, x, y, color);
    guiGraphics.fill(x, y, x + width, y + height, color);
    guiGraphics.renderItem(itemStack, x, y);
}
```

#### 1.12 Equivalent:
```java
public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    fontRenderer.drawString(text, x, y, color);
    drawRect(x, y, x + width, y + height, color);
    renderItemStack(itemStack, x, y);
}

// OpenGL state management required
GlStateManager.pushMatrix();
GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
// Rendering code
GlStateManager.popMatrix();
```

### 5. Menu/Container System Redesign

#### Current Issue: AbstractContainerMenu doesn't exist in 1.12
**Files affected**:
- `src/main/java/com/yourdomain/businesscraft/menu/StorageMenu.java`
- `src/main/java/com/yourdomain/businesscraft/menu/TradeMenu.java`
- `src/main/java/com/yourdomain/businesscraft/menu/TownInterfaceMenu.java`
- All screen classes

#### API Changes:
```java
// 1.20.1 (Current)
public class StorageMenu extends AbstractContainerMenu {
    public StorageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(ModMenuTypes.STORAGE_MENU.get(), containerId);
    }
}

// 1.12 (Required)
public class ContainerStorage extends Container {
    public ContainerStorage(InventoryPlayer playerInv, TileEntityTownBlock tileEntity) {
        // Manual slot setup
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
}
```

### 6. Networking System Conversion

#### Current Issue: FriendlyByteBuf and modern networking don't exist
**Files affected**:
- `src/main/java/com/yourdomain/businesscraft/network/ModMessages.java`
- All packet classes in `network/packets/`

#### Current 1.20.1 System:
```java
public static void register() {
    SimpleChannel net = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(BusinessCraft.MOD_ID, "messages"))
        .networkProtocolVersion(() -> "1.0")
        .simpleChannel();
}
```

#### 1.12 Equivalent:
```java
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class NetworkHandler {
    @SidedProxy(clientSide = "client.ClientProxy", serverSide = "common.CommonProxy")
    public static CommonProxy proxy;
    
    public static SimpleNetworkWrapper INSTANCE;
    
    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(BusinessCraft.MOD_ID);
        INSTANCE.registerMessage(PacketHandler.class, PacketMessage.class, 0, Side.SERVER);
    }
}
```

### 7. Resource System Changes

#### Current Issue: Modern resource location and pack systems
**Files affected**: 
- `src/main/resources/assets/businesscraft/`
- All model and texture references

#### Changes Required:
- Model format differences (1.12 uses older model specification)
- Texture handling changes
- Recipe system completely different (JSON → code-based)

### 8. Entity System Migration

#### Current Issue: Entity API completely different
**File**: `src/main/java/com/yourdomain/businesscraft/entity/TouristEntity.java`

#### Current 1.20.1:
```java
public class TouristEntity extends PathfinderMob {
    public TouristEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }
}
```

#### 1.12 Equivalent:
```java
public class EntityTourist extends EntityCreature {
    public EntityTourist(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
    }
    
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }
}
```

### 9. Item and Block State Changes

#### Current Issue: Modern block state system
**Files affected**:
- `src/main/java/com/yourdomain/businesscraft/block/TownBlock.java`
- `src/main/java/com/yourdomain/businesscraft/block/TownInterfaceBlock.java`

#### API Changes:
```java
// 1.20.1 (Current)
public class TownBlock extends BaseEntityBlock {
    public InteractionResult use(BlockState state, Level level, BlockPos pos, 
                               Player player, InteractionHand hand, BlockHitResult hit) {
        // Implementation
    }
}

// 1.12 (Required)
public class BlockTown extends Block implements ITileEntityProvider {
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                  EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                  float hitX, float hitY, float hitZ) {
        // Implementation
    }
    
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTownBlock();
    }
}
```

## Complete File Migration Requirements

### High Priority (Core Functionality):
1. **Build System**: Complete `build.gradle` rewrite for 1.12 Forge
2. **Main Mod Class**: `BusinessCraft.java` - Convert to FML lifecycle events
3. **Registration Classes**: All `init/` package files - Convert to GameRegistry
4. **Block Entities**: Convert all to TileEntity system
5. **NBT Handling**: Update all save/load methods

### Medium Priority (Features):
1. **Networking**: Rewrite all packet handling
2. **Menu System**: Convert containers and screens
3. **Rendering**: Complete UI system rewrite
4. **Entity System**: Update entity registration and behavior

### Low Priority (Polish):
1. **Resource Pack**: Update models and textures for 1.12 format
2. **Recipes**: Convert from JSON to code-based registration
3. **Advancements**: Remove (didn't exist in 1.12)

## Dependency Changes

### Build Configuration Changes:
```gradle
// Current (1.20.1)
plugins {
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}
minecraft {
    mappings channel: 'parchment', version: '2023.06.26-1.20.1'
}
dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.1.0'
}

// Required (1.12)
plugins {
    id 'net.minecraftforge.gradle' version '2.3'
}
minecraft {
    version = "1.12.2"
    runDir = "run"
    mappings = "stable_39"
}
dependencies {
    compile 'net.minecraftforge:forge:1.12.2-14.23.5.2859'
}
```

### Mod Metadata Changes:
```toml
# Current (mods.toml)
modLoader="javafml"
loaderVersion="[47,)"

# Required (mcmod.info)
[{
    "modid": "businesscraft",
    "name": "BusinessCraft",
    "description": "A Minecraft mod adding business and economy features to the game.",
    "version": "1.0.0",
    "mcversion": "1.12.2",
    "acceptableRemoteVersions": "[1.0.0]"
}]
```

## Critical Compatibility Issues

### 1. Java 17 Features Used
The mod currently uses Java 17 features that don't exist in Java 8:
- Pattern matching
- Text blocks
- Records (if any)
- Switch expressions

### 2. Modern Minecraft Features
Features that need complete removal or alternatives:
- **ComponentData**: Doesn't exist in 1.12
- **DataComponents**: Modern item data system
- **ResourceKeys**: Modern resource location system
- **Codecs**: Data serialization system

### 3. Rendering Pipeline
The entire rendering system changed between 1.12 and 1.20:
- **GuiGraphics**: Doesn't exist, use direct OpenGL calls
- **Matrix transformations**: Different API
- **Font rendering**: Completely different system

## Estimated Effort

### Development Time:
- **Core System Migration**: 80-120 hours
- **UI System Rewrite**: 60-80 hours  
- **Testing and Debugging**: 40-60 hours
- **Total Estimated**: 180-260 hours

### Risk Assessment:
- **High Risk**: Complete UI rewrite required
- **Medium Risk**: Networking and container systems
- **Low Risk**: Basic block and entity functionality

## Conclusion

Downgrading BusinessCraft from 1.20.1 to 1.12 is a massive undertaking requiring:
1. Complete rewrite of the UI system (largest effort)
2. Migration of all registration systems
3. Conversion of block entities to tile entities
4. Update of all NBT operations
5. Rewrite of networking system
6. Java 8 compatibility changes

This is essentially a complete mod rewrite rather than a simple port, due to the fundamental changes in Minecraft's modding APIs over the 8-year span. The estimated effort of 180-260 hours makes this a major project requiring careful planning and testing.

Alternative recommendation: Consider targeting a more recent intermediate version (1.16 or 1.18) to reduce the migration complexity while still achieving broader compatibility. 