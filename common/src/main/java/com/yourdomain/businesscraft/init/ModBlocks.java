package com.yourdomain.businesscraft.init;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.block.TownInterfaceBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.function.Supplier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BusinessCraft.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BusinessCraft.MOD_ID);
    
    // Town Interface Block - our primary town management block
    public static final RegistryObject<Block> TOWN_INTERFACE = registerBlock("town_interface",
            () -> new TownInterfaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            )
    );
    
    // Helper method to register blocks and block items
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);
        registerBlockItem(name, registeredBlock);
        return registeredBlock;
    }
    
    // Helper method to register block items
    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
} 