package com.quackers29.businesscraft.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public interface RegistryHelper {
    <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block);

    <T extends Item> Supplier<T> registerBlockItem(String name, Supplier<? extends Block> block);

    <T extends EntityType<?>> Supplier<T> registerEntityType(String name, Supplier<T> entityType);

    <T extends BlockEntityType<?>> Supplier<T> registerBlockEntityType(String name, Supplier<T> blockEntityType);

    <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType);

    @FunctionalInterface
    interface MenuFactory<T extends net.minecraft.world.inventory.AbstractContainerMenu> {
        T create(int windowId, net.minecraft.world.entity.player.Inventory inv,
                net.minecraft.network.FriendlyByteBuf data);
    }

    <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<MenuType<T>> registerExtendedMenuType(
            String name, MenuFactory<T> factory);

    Item getItem(ResourceLocation location);

    ResourceLocation getItemKey(Item item);

    Iterable<Item> getItems();
}
