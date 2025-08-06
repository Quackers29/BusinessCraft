package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.MenuHelper;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.extensions.IForgeMenuType;

import java.util.function.Supplier;

/**
 * Forge implementation of the MenuHelper interface using IForgeMenuType.
 * This class provides cross-platform menu registration with complex data transfer support
 * essential for BusinessCraft's sophisticated UI framework.
 */
public class ForgeMenuHelper implements MenuHelper {
    
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
}