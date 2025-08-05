package com.yourdomain.businesscraft.platform;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Platform abstraction interface for cross-platform registration.
 * This interface provides a common API for registering game objects across mod loaders.
 */
public interface RegistryHelper {
    
    /**
     * Registers a block with the given name and supplier.
     * @param name The registry name
     * @param blockSupplier The block supplier
     * @return A supplier that provides the registered block
     */
    <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> blockSupplier);
    
    /**
     * Registers an item with the given name and supplier.
     * @param name The registry name
     * @param itemSupplier The item supplier
     * @return A supplier that provides the registered item
     */
    <T extends Item> Supplier<T> registerItem(String name, Supplier<T> itemSupplier);
    
    /**
     * Registers a block entity type with the given name and supplier.
     * @param name The registry name
     * @param blockEntitySupplier The block entity type supplier
     * @return A supplier that provides the registered block entity type
     */
    <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String name, Supplier<BlockEntityType<T>> blockEntitySupplier);
    
    /**
     * Registers an entity type with the given name and supplier.
     * @param name The registry name
     * @param entitySupplier The entity type supplier
     * @return A supplier that provides the registered entity type
     */
    <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> entitySupplier);
    
    /**
     * Registers a menu type with the given name and supplier.
     * @param name The registry name
     * @param menuSupplier The menu type supplier
     * @return A supplier that provides the registered menu type
     */
    <T extends MenuType<?>> Supplier<T> registerMenu(String name, Supplier<T> menuSupplier);
    
    /**
     * Called during mod initialization to register all deferred objects.
     * This should be called at the appropriate time for each platform.
     */
    void finalizeRegistrations();
}