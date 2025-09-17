package com.quackers29.businesscraft.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Platform-agnostic interface for menu/screen operations.
 * Implementations will handle platform-specific screen registration.
 */
public interface MenuHelper {
    /**
     * Register a screen factory for a menu type
     */
    <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerScreenFactory(
        MenuType<? extends M> menuType,
        MenuHelper.ScreenFactory<M, U> screenFactory
    );

    /**
     * Functional interface for screen factories
     */
    @FunctionalInterface
    interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title);
    }
}
