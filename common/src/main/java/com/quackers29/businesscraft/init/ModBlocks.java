package com.quackers29.businesscraft.init;

// UNIFIED ARCHITECTURE: Use TownInterfaceBlock from common module
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.RegistryHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.function.Supplier;

/**
 * Platform-agnostic block registration using the RegistryHelper abstraction.
 * This system works across different mod loaders (Forge, Fabric, etc.).
 */
public class ModBlocks {
    // Platform abstraction helper
    private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();
    
    // Registered blocks using platform abstraction
    public static Supplier<Block> TOWN_INTERFACE;
    public static Supplier<Item> TOWN_INTERFACE_ITEM;
    
    /**
     * Initialize all block registrations.
     * This should be called during mod initialization.
     */
    public static void initialize() {
        // Register blocks using platform abstraction
        TOWN_INTERFACE = REGISTRY.registerBlock("town_interface",
                () -> new TownInterfaceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(3.0f, 3.0f)
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops()
                )
        );
        
        // Register block items using platform abstraction
        TOWN_INTERFACE_ITEM = REGISTRY.registerItem("town_interface", 
                () -> new BlockItem(TOWN_INTERFACE.get(), new Item.Properties())
        );
    }
} 