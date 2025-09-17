package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.RegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;

/**
 * Fabric implementation of RegistryHelper
 */
public class FabricRegistryHelper implements RegistryHelper {

    private static final String MOD_ID = "businesscraft";

    @Override
    public void registerBlock(String name, Block block) {
        // Register the block
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, name), block);
        // Also register the block item
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, name),
            new BlockItem(block, new Item.Properties()));
    }

    @Override
    public void registerBlockItem(String name, Block block) {
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, name),
            new BlockItem(block, new Item.Properties()));
    }

    @Override
    public void registerEntityType(String name, EntityType<?> entityType) {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(MOD_ID, name), entityType);
    }

    @Override
    public void registerBlockEntityType(String name, BlockEntityType<?> blockEntityType) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(MOD_ID, name), blockEntityType);
    }

    @Override
    public void registerMenuType(String name, MenuType<?> menuType) {
        Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MOD_ID, name), menuType);
    }

    @Override
    public Item getItem(ResourceLocation location) {
        return BuiltInRegistries.ITEM.get(location);
    }

    @Override
    public ResourceLocation getItemKey(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
