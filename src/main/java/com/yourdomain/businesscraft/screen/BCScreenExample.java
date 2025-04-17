package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.state.TownInterfaceState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Example of how to create the new town interface screen
 */
public class BCScreenExample {
    
    /**
     * Example of how to create a town interface screen
     */
    public static TownInterfaceScreen createTownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        // Create state manager for the screen
        TownInterfaceState state = new TownInterfaceState();
        
        // Create the screen with the state
        TownInterfaceScreen screen = new TownInterfaceScreen(menu, inventory, title);
        
        // In actual usage, you would add components in the TownInterfaceScreen init method
        // This is just a demonstration showing how to create the basic screen
        
        return screen;
    }
} 