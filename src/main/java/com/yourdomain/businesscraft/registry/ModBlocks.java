package com.yourdomain.businesscraft.registry;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.block.CompanyBlock;
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

    public static final RegistryObject<Block> COMPANY_BLOCK = BLOCKS.register("company_block",
            CompanyBlock::new);

    public static final RegistryObject<Item> COMPANY_BLOCK_ITEM = ITEMS.register("company_block",
            () -> new BlockItem(COMPANY_BLOCK.get(), new Item.Properties()));
}