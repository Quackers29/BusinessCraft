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
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;

import java.util.function.Supplier;

/**
 * Fabric implementation of RegistryHelper using strong types.
 */
public class FabricRegistryHelper implements RegistryHelper {

    private static final String MOD_ID = "businesscraft";

    @Override
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        T blockInstance = block.get();
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, name), blockInstance);
        return () -> blockInstance;
    }

    @Override
    public <T extends Item> Supplier<T> registerBlockItem(String name, Supplier<? extends Block> block) {
        BlockItem blockItem = new BlockItem(block.get(), new FabricItemSettings());
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, name), blockItem);
        // Cast is safe because we just created it
        @SuppressWarnings("unchecked")
        T result = (T) blockItem;
        return () -> result;
    }

    @Override
    public <T extends EntityType<?>> Supplier<T> registerEntityType(String name, Supplier<T> entityType) {
        T entityTypeInstance = entityType.get();
        Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(MOD_ID, name), entityTypeInstance);
        return () -> entityTypeInstance;
    }

    @Override
    public <T extends BlockEntityType<?>> Supplier<T> registerBlockEntityType(String name,
            Supplier<T> blockEntityType) {
        T blockEntityTypeInstance = blockEntityType.get();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(MOD_ID, name),
                blockEntityTypeInstance);
        return () -> blockEntityTypeInstance;
    }

    @Override
    public <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
        T menuTypeInstance = menuType.get();
        Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MOD_ID, name), menuTypeInstance);
        return () -> menuTypeInstance;
    }

    @Override
    public <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<MenuType<T>> registerExtendedMenuType(
            String name, MenuFactory<T> factory) {
        // Use Fabric's ScreenHandlerRegistry for extended menu types
        // Note: ScreenHandlerRegistry is deprecated in newer versions but used here for
        // consistency with existing code
        @SuppressWarnings("unchecked")
        ExtendedScreenHandlerType<T> menuType = (ExtendedScreenHandlerType<T>) (Object) ScreenHandlerRegistry
                .registerExtended(
                        new ResourceLocation(MOD_ID, name),
                        (syncId, inventory, buf) -> factory.create(syncId, inventory, buf));

        return () -> (MenuType<T>) menuType;
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
