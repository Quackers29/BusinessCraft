package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.RegistryHelper;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
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
 * Fabric implementation of RegistryHelper using direct Fabric API calls
 */
public class FabricRegistryHelper implements RegistryHelper {

    private static final String MOD_ID = "businesscraft";

    @Override
    public void registerBlock(String name, Object block) {
        if (block instanceof Block mcBlock) {
            // Register the block directly with Fabric's registry system
            Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, name), mcBlock);
            // Also register the block item
            registerBlockItem(name, mcBlock);
        }
    }

    @Override
    public void registerBlockItem(String name, Object block) {
        if (block instanceof Block mcBlock) {
            // Create and register block item directly
            BlockItem blockItem = new BlockItem(mcBlock, new Item.Properties());
            Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, name), blockItem);
        }
    }

    @Override
    public void registerEntityType(String name, Object entityType) {
        if (entityType instanceof EntityType<?> mcEntityType) {
            // Register entity type directly with Fabric's registry system
            Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(MOD_ID, name), mcEntityType);
        }
    }

    @Override
    public void registerBlockEntityType(String name, Object blockEntityType) {
        if (blockEntityType instanceof BlockEntityType<?> mcBlockEntityType) {
            // Register block entity type directly with Fabric's registry system
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(MOD_ID, name), mcBlockEntityType);
        }
    }

    @Override
    public void registerMenuType(String name, Object menuType) {
        if (menuType instanceof MenuType<?> mcMenuType) {
            // Register menu type directly with Fabric's registry system
            Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MOD_ID, name), mcMenuType);
        }
    }

    @Override
    public Object getItem(Object location) {
        if (location instanceof ResourceLocation mcLocation) {
            // Direct lookup using Fabric's registry system
            return BuiltInRegistries.ITEM.get(mcLocation);
        }
        return null;
    }

    @Override
    public Object getItemKey(Object item) {
        if (item instanceof Item mcItem) {
            // Direct lookup using Fabric's registry system
            return BuiltInRegistries.ITEM.getKey(mcItem);
        }
        return null;
    }
}
