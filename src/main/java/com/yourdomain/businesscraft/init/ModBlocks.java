package com.yourdomain.businesscraft.init;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.block.TownInterfaceBlock;
import com.yourdomain.businesscraft.platform.PlatformServices;
import com.yourdomain.businesscraft.platform.RegistryHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.function.Supplier;

// Legacy Forge imports - kept for backwards compatibility during transition
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    // Legacy Forge registration system - kept for backwards compatibility
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BusinessCraft.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BusinessCraft.MOD_ID);
    
    // Platform abstraction helper
    private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();
    
    // Registered blocks using platform abstraction
    public static Supplier<Block> TOWN_INTERFACE_PLATFORM;
    
    // Legacy Forge registered blocks - kept for backwards compatibility  
    public static final RegistryObject<Block> TOWN_INTERFACE = registerBlock("town_interface",
            () -> new TownInterfaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            )
    );
    
    /**
     * Initialize platform-agnostic block registration.
     * This will eventually replace the legacy Forge system.
     */
    public static void initializePlatformRegistration() {
        // Register blocks using platform abstraction
        TOWN_INTERFACE_PLATFORM = REGISTRY.registerBlock("town_interface_platform",
                () -> new TownInterfaceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(3.0f, 3.0f)
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops()
                )
        );
        
        // Register block items using platform abstraction
        REGISTRY.registerItem("town_interface_platform", 
                () -> new BlockItem(TOWN_INTERFACE_PLATFORM.get(), new Item.Properties())
        );
    }
    
    // Legacy helper method to register blocks and block items
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);
        registerBlockItem(name, registeredBlock);
        return registeredBlock;
    }
    
    // Legacy helper method to register block items
    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
} 