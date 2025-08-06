package com.quackers29.businesscraft.platform;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerType;

import java.util.function.Supplier;

/**
 * Platform-agnostic registry helper interface using Yarn mappings.
 * Provides cross-platform registration operations for the Enhanced MultiLoader approach.
 */
public interface RegistryHelper {
    
    /**
     * Register a block
     * @param name The registry name
     * @param blockSupplier Supplier for the block
     * @return Supplier for the registered block
     */
    <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> blockSupplier);
    
    /**
     * Register an item
     * @param name The registry name
     * @param itemSupplier Supplier for the item
     * @return Supplier for the registered item
     */
    <T extends Item> Supplier<T> registerItem(String name, Supplier<T> itemSupplier);
    
    /**
     * Register a block entity type
     * @param name The registry name
     * @param blockEntitySupplier Supplier for the block entity type
     * @return Supplier for the registered block entity type
     */
    <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String name, Supplier<BlockEntityType<T>> blockEntitySupplier);
    
    /**
     * Register an entity type
     * @param name The registry name
     * @param entitySupplier Supplier for the entity type
     * @return Supplier for the registered entity type
     */
    <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> entitySupplier);
    
    /**
     * Register a screen handler type (menu type)
     * @param name The registry name
     * @param menuSupplier Supplier for the screen handler type
     * @return Supplier for the registered screen handler type
     */
    <T extends ScreenHandlerType<?>> Supplier<T> registerMenu(String name, Supplier<T> menuSupplier);
}