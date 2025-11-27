package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.RegistryHelper;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Fabric implementation of RegistryHelper using strong types.
 */
public class FabricRegistryHelper implements RegistryHelper {

    private static final String MOD_ID = "businesscraft";

    @Override
    public void registerBlock(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, name), block);
    }

    @Override
    public void registerBlockItem(String name, Block block) {
        BlockItem blockItem = new BlockItem(block, new FabricItemSettings());
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, name), blockItem);
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
