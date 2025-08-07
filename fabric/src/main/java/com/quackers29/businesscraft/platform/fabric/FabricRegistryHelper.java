package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.RegistryHelper;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

/**
 * Fabric implementation of RegistryHelper using Yarn mappings.
 * Implements cross-platform registration using Fabric Registry API.
 */
public class FabricRegistryHelper implements RegistryHelper {
    
    private static final String MOD_ID = "businesscraft";
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> registerBlock(String name, Supplier<T> blockSupplier) {
        Block block = Registry.register(Registries.BLOCK, new Identifier(MOD_ID, name), (Block) blockSupplier.get());
        return () -> (T) block;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> registerItem(String name, Supplier<T> itemSupplier) {
        Item item = Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), (Item) itemSupplier.get());
        return () -> (T) item;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> registerBlockEntity(String name, Supplier<T> blockEntitySupplier) {
        BlockEntityType<?> type = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, name), (BlockEntityType<?>) blockEntitySupplier.get());
        return () -> (T) type;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> registerEntity(String name, Supplier<T> entitySupplier) {
        EntityType<?> entityType = Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, name), (EntityType<?>) entitySupplier.get());
        return () -> (T) entityType;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> registerMenu(String name, Supplier<T> menuSupplier) {
        ScreenHandlerType<?> menuType = Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, name), (ScreenHandlerType<?>) menuSupplier.get());
        return () -> (T) menuType;
    }
    
    @Override
    public void finalizeRegistrations() {
        // Fabric handles registration immediately, so no finalization needed
        // This method exists for compatibility with other platforms
    }
    
    // ========================================
    // ENHANCED METHODS FOR TOWN MANAGEMENT
    // ========================================
    
    @Override
    public Object getItem(String resourceLocation) {
        // TODO: Implement in Phase 10.2 - get item by resource location
        throw new UnsupportedOperationException("Town management methods will be implemented in Phase 10.2");
    }
    
    @Override
    public String getItemId(Object item) {
        // TODO: Implement in Phase 10.2 - get resource location from item
        throw new UnsupportedOperationException("Town management methods will be implemented in Phase 10.2");
    }
    
    @Override
    public Object serializeItem(Object item) {
        // TODO: Implement in Phase 10.2 - serialize item to NBT-compatible format
        throw new UnsupportedOperationException("Town management methods will be implemented in Phase 10.2");
    }
    
    @Override
    public Object deserializeItem(Object data) {
        // TODO: Implement in Phase 10.2 - deserialize item from NBT-compatible format
        throw new UnsupportedOperationException("Town management methods will be implemented in Phase 10.2");
    }
    
    @Override
    public boolean itemExists(String resourceLocation) {
        // TODO: Implement in Phase 10.2 - check if item exists in registry
        throw new UnsupportedOperationException("Town management methods will be implemented in Phase 10.2");
    }
    
    @Override
    public String[] getAllItemIds() {
        // TODO: Implement in Phase 10.2 - get all item resource locations
        throw new UnsupportedOperationException("Town management methods will be implemented in Phase 10.2");
    }
}