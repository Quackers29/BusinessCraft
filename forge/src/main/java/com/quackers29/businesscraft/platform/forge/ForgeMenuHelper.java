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
    
    @Override
    public boolean openTownInterfaceMenu(Object player, int[] blockPos, String displayName) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }
        
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(blockPos[0], blockPos[1], blockPos[2]);
        
        try {
            // Use NetworkHooks to open the town interface menu (Forge-specific)
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.literal(displayName);
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId, 
                        net.minecraft.world.entity.player.Inventory inventory, 
                        net.minecraft.world.entity.player.Player player) {
                    // Create the TownInterfaceMenu using the town's position
                    return new com.quackers29.businesscraft.menu.TownInterfaceMenu(windowId, inventory, pos);
                }
            }, pos);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}