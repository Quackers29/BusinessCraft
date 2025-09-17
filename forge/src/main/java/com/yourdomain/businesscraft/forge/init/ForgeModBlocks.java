package com.yourdomain.businesscraft.forge.init;

import com.yourdomain.businesscraft.block.TownInterfaceBlock;
import com.yourdomain.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific block registrations
 */
public class ForgeModBlocks {
    public static RegistryObject<Block> TOWN_INTERFACE_BLOCK = ForgeRegistryHelper.BLOCKS.register("town_interface",
            () -> new TownInterfaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static RegistryObject<Item> TOWN_INTERFACE_BLOCK_ITEM = ForgeRegistryHelper.ITEMS.register("town_interface",
            () -> new BlockItem(TOWN_INTERFACE_BLOCK.get(), new Item.Properties()));

    public static void register() {
        // Block registration is handled by RegistryObject above
    }

    public static void registerBlockItems() {
        // Block items are now registered using RegistryObject above
        System.out.println("DEBUG: Town Interface BlockItem registered via RegistryObject");
    }
}
