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
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> blockSupplier) {
        T block = Registry.register(Registries.BLOCK, new Identifier(MOD_ID, name), blockSupplier.get());
        return () -> block;
    }
    
    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> itemSupplier) {
        T item = Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), itemSupplier.get());
        return () -> item;
    }
    
    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String name, Supplier<BlockEntityType<T>> blockEntitySupplier) {
        BlockEntityType<T> type = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, name), blockEntitySupplier.get());
        return () -> type;
    }
    
    @Override
    public <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> entitySupplier) {
        T entityType = Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, name), entitySupplier.get());
        return () -> entityType;
    }
    
    @Override
    public <T extends ScreenHandlerType<?>> Supplier<T> registerMenu(String name, Supplier<T> menuSupplier) {
        T menuType = Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, name), menuSupplier.get());
        return () -> menuType;
    }
}