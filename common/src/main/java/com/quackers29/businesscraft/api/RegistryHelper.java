package com.quackers29.businesscraft.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Platform-agnostic interface for registration operations.
 * Implementations will handle platform-specific registration.
 */
public interface RegistryHelper {
    /**
     * Register a block with the given name
     */
    void registerBlock(String name, Block block);

    /**
     * Register a block item for the given block
     */
    void registerBlockItem(String name, Block block);

    /**
     * Register an entity type with the given name
     */
    void registerEntityType(String name, EntityType<?> entityType);

    /**
     * Register a block entity type with the given name
     */
    void registerBlockEntityType(String name, BlockEntityType<?> blockEntityType);

    /**
     * Register a menu type with the given name
     */
    void registerMenuType(String name, MenuType<?> menuType);

    /**
     * Get an item by its ResourceLocation
     */
    Item getItem(ResourceLocation location);

    /**
     * Get the ResourceLocation for an item
     */
    ResourceLocation getItemKey(Item item);
}
