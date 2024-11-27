package com.yourdomain.businesscraft.registry;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.block.TownBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
                        BusinessCraft.MOD_ID);
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
                        BusinessCraft.MOD_ID);

        public static final RegistryObject<Block> TOWN_BLOCK = BLOCKS.register("town_block",
                        TownBlock::new);

        public static final RegistryObject<Item> TOWN_BLOCK_ITEM = ITEMS.register("town_block",
                        () -> new BlockItem(TOWN_BLOCK.get(), new Item.Properties()));
}