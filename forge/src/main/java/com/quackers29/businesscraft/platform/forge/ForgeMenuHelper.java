package com.quackers29.businesscraft.platform.forge;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.extensions.IForgeMenuType;

import java.util.function.Supplier;

/**
 * Forge implementation of the MenuHelper interface using IForgeMenuType.
 * This class provides cross-platform menu registration with complex data transfer support
 * essential for BusinessCraft's sophisticated UI framework.
 */
public class ForgeMenuHelper implements com.quackers29.businesscraft.platform.ForgeMenuHelper {
    
    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> createDataDrivenMenuType(MenuFactory<T> factory) {
        return () -> IForgeMenuType.create((containerId, playerInventory, data) -> 
            factory.create(containerId, playerInventory, data));
    }
    
    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> createSimpleMenuType(SimpleMenuFactory<T> factory) {
        return () -> new MenuType<T>((containerId, playerInventory) -> 
            factory.create(containerId, playerInventory), null);
    }
    
    @Override
    public void refreshActiveMenu(Object player, String refreshType) {
        // TODO: Implement menu refresh logic for Forge
        // This method will be used to update active menus with new data
    }
}