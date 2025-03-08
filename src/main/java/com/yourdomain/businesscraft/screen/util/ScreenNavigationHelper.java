package com.yourdomain.businesscraft.screen.util;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class to handle common screen navigation patterns
 */
public class ScreenNavigationHelper {
    
    /**
     * Returns to the main town interface screen from any other screen
     * 
     * @param minecraft The Minecraft client instance
     * @param player The player whose inventory will be used in the new screen
     */
    public static void returnToTownInterface(Minecraft minecraft, Player player) {
        // Create the menu with just the player's current position
        TownInterfaceMenu containerMenu = new TownInterfaceMenu(
            0, player.getInventory(), 
            player.blockPosition());
        
        // Create and set the new screen
        minecraft.setScreen(new TownInterfaceScreen(
            containerMenu,
            player.getInventory(),
            Component.literal("Town Interface")));
    }
} 