package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.RegistryHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Forge implementation of the RegistryHelper interface using DeferredRegister.
 */
public class ForgeRegistryHelper implements RegistryHelper {
    
    private final String modId;
    private final DeferredRegister<Block> blocks;
    private final DeferredRegister<Item> items;
    private final DeferredRegister<BlockEntityType<?>> blockEntities;
    private final DeferredRegister<EntityType<?>> entities;
    private final DeferredRegister<MenuType<?>> menus;
    
    public ForgeRegistryHelper(String modId) {
        this.modId = modId;
        this.blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, modId);
        this.items = DeferredRegister.create(ForgeRegistries.ITEMS, modId);
        this.blockEntities = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, modId);
        this.entities = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, modId);
        this.menus = DeferredRegister.create(ForgeRegistries.MENU_TYPES, modId);
    }
    
    @Override
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> blockSupplier) {
        RegistryObject<T> registryObject = blocks.register(name, blockSupplier);
        return registryObject; // RegistryObject implements Supplier<T>
    }
    
    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> itemSupplier) {
        RegistryObject<T> registryObject = items.register(name, itemSupplier);
        return registryObject; // RegistryObject implements Supplier<T>
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String name, Supplier<BlockEntityType<T>> blockEntitySupplier) {
        RegistryObject<BlockEntityType<?>> registryObject = blockEntities.register(name, 
            () -> (BlockEntityType<?>) blockEntitySupplier.get());
        return () -> (BlockEntityType<T>) registryObject.get();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> entitySupplier) {
        RegistryObject<EntityType<?>> registryObject = entities.register(name, 
            () -> (EntityType<?>) entitySupplier.get());
        return () -> (T) registryObject.get();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends MenuType<?>> Supplier<T> registerMenu(String name, Supplier<T> menuSupplier) {
        RegistryObject<MenuType<?>> registryObject = menus.register(name, 
            () -> (MenuType<?>) menuSupplier.get());
        return () -> (T) registryObject.get();
    }
    
    @Override
    public void finalizeRegistrations() {
        // In Forge, registrations are handled automatically by the DeferredRegister
        // This method exists for compatibility with other platforms that may need explicit finalization
    }
    
    /**
     * Gets the DeferredRegister instances for manual registration if needed.
     * This is Forge-specific functionality.
     */
    public DeferredRegister<Block> getBlocks() { return blocks; }
    public DeferredRegister<Item> getItems() { return items; }
    public DeferredRegister<BlockEntityType<?>> getBlockEntities() { return blockEntities; }
    public DeferredRegister<EntityType<?>> getEntities() { return entities; }
    public DeferredRegister<MenuType<?>> getMenus() { return menus; }
}