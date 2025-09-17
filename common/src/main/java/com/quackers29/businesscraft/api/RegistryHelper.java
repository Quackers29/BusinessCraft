package com.quackers29.businesscraft.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;

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
}
