package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.ui.modal.factories.BCModalInventoryFactory;
import com.yourdomain.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/**
 * Manages storage modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class StorageModalManager {
    
    /**
     * Creates and shows a storage modal screen.
     * 
     * @param parentScreen The parent screen to return to
     * @param blockPos The position of the town block
     * @param townMenu The town interface menu for accessing storage data
     * @param onScreenClosed Optional callback when screen is closed
     */
    public static void showStorageModal(
            Screen parentScreen, 
            BlockPos blockPos, 
            TownInterfaceMenu townMenu,
            Consumer<BCModalInventoryScreen<StorageMenu>> onScreenClosed) {
        
        // Create a modal storage screen using the factory
        BCModalInventoryScreen<StorageMenu> storageScreen = BCModalInventoryFactory.createStorageScreen(
            Component.literal("Town Storage"),
            parentScreen,
            blockPos,
            onScreenClosed
        );
        
        // Initialize the storage inventory with the town's communal storage items
        initializeStorageInventory(storageScreen, townMenu);
        
        // Show the storage screen as a modal overlay
        Minecraft.getInstance().setScreen(storageScreen);
    }
    
    /**
     * Initializes the storage inventory with communal storage items.
     * 
     * @param storageScreen The storage screen to initialize
     * @param townMenu The town menu containing storage data
     */
    private static void initializeStorageInventory(
            BCModalInventoryScreen<StorageMenu> storageScreen, 
            TownInterfaceMenu townMenu) {
        
        if (storageScreen.getMenu() != null && townMenu != null) {
            StorageMenu storageMenu = storageScreen.getMenu();
            
            // Start with communal storage by default
            storageMenu.updateStorageItems(townMenu.getAllCommunalStorageItems());
            
            // If at some point we want to start with personal mode, we would do:
            // boolean isPersonalMode = true;
            // storageMenu.toggleStorageMode();
            // storageMenu.updatePersonalStorageItems(townMenu.getPersonalStorageItems(Minecraft.getInstance().player.getUUID()));
        }
    }
} 