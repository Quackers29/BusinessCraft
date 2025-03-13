package com.yourdomain.businesscraft.screen.util;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

/**
 * Utility class to handle common screen navigation patterns
 */
public class ScreenNavigationHelper {
    
    /**
     * Returns to the main town interface screen from any other screen
     * 
     * @param minecraft The Minecraft client instance
     * @param player The player whose inventory will be used in the new screen
     * @param townBlockPos The position of the town block entity
     */
    public static void returnToTownInterface(Minecraft minecraft, Player player, BlockPos townBlockPos) {
        // Create the menu with the town block position to ensure town data is correctly retrieved
        TownInterfaceMenu containerMenu = new TownInterfaceMenu(
            0, player.getInventory(), 
            townBlockPos);
        
        // Create and set the new screen
        minecraft.setScreen(new TownInterfaceScreen(
            containerMenu,
            player.getInventory(),
            Component.literal("Town Interface")));
    }
} 