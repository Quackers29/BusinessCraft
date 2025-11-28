package com.quackers29.businesscraft.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Platform-agnostic interface for registration operations.
 * Implementations will handle platform-specific registration.
 */
public interface RegistryHelper {
    /**
     * Register a block with the given name
     */
    <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block);

    /**
     * Register a block item for the given block
     */
    <T extends Item> Supplier<T> registerBlockItem(String name, Supplier<? extends Block> block);

    /**
     * Register an entity type with the given name
     */
    <T extends EntityType<?>> Supplier<T> registerEntityType(String name, Supplier<T> entityType);

    /**
     * Register a block entity type with the given name
     */
    <T extends BlockEntityType<?>> Supplier<T> registerBlockEntityType(String name, Supplier<T> blockEntityType);

    /**
     * Register a menu type with the given name
     */
    /**
     * Register a menu type with the given name
     */
    <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType);

    /**
     * Factory for creating menus with extra data
     */
    @FunctionalInterface
    interface MenuFactory<T extends net.minecraft.world.inventory.AbstractContainerMenu> {
        T create(int windowId, net.minecraft.world.entity.player.Inventory inv,
                net.minecraft.network.FriendlyByteBuf data);
    }

    /**
     * Register an extended menu type (one that requires extra data from the server)
     */
    <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<MenuType<T>> registerExtendedMenuType(
            String name, MenuFactory<T> factory);

    /**
     * Get an item by its ResourceLocation
     */
    Item getItem(ResourceLocation location);

    /**
     * Get the ResourceLocation for an item
     */
    ResourceLocation getItemKey(Item item);
}
