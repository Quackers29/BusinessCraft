package com.quackers29.businesscraft.ui.modal.factories;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalGridScreen;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import java.util.function.Consumer;

/**
 * Factory class for creating common types of modal inventory screens.
 * This provides a convenient way to create trade and storage screens
 * that appear as modal overlays rather than separate screens.
 */
public class BCModalInventoryFactory {
    
    /**
     * Create a modal trade screen with input and output slots
     * 
     * @param title The screen title
     * @param parentScreen The parent screen to return to
     * @param townBlockPos The position of the town block
     * @param onCloseCallback Optional callback for screen close
     * @return Configured BCModalInventoryScreen for trading
     */
    public static BCModalInventoryScreen<TradeMenu> createTradeScreen(
            Component title, 
            Screen parentScreen,
            BlockPos townBlockPos,
            Consumer<BCModalInventoryScreen<TradeMenu>> onCloseCallback) {
        
        // Get the player
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            throw new IllegalStateException("ClientHelper not available when creating trade screen");
        }
        
        Object playerObj = clientHelper.getClientPlayer();
        if (!(playerObj instanceof Player player)) {
            throw new IllegalStateException("Player cannot be null when creating trade screen");
        }
        
        // Create a trade menu
        int containerId = player.containerMenu.containerId + 1;
        TradeMenu tradeMenu = new TradeMenu(containerId, player.getInventory(), townBlockPos);
        
        // IMPORTANT: Set the player's active container menu to the new trade menu
        // This is crucial for proper inventory interaction
        player.containerMenu = tradeMenu;
        
        // Create the modal inventory screen
        return BCModalInventoryScreen.createTradeScreen(
                title,
                parentScreen,
                tradeMenu,
                player.getInventory(),
                onCloseCallback)
                .withBackButtonText("Back");
    }
    
    /**
     * Create a modal storage screen with a 2x9 grid of slots
     * 
     * @param title The screen title
     * @param parentScreen The parent screen to return to
     * @param townBlockPos The position of the town block
     * @param onCloseCallback Optional callback for screen close
     * @return Configured BCModalInventoryScreen for storage
     */
    public static BCModalInventoryScreen<StorageMenu> createStorageScreen(
            Component title, 
            Screen parentScreen,
            BlockPos townBlockPos,
            Consumer<BCModalInventoryScreen<StorageMenu>> onCloseCallback) {
        
        // Get the player
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            throw new IllegalStateException("ClientHelper not available when creating storage screen");
        }
        
        Object playerObj = clientHelper.getClientPlayer();
        if (!(playerObj instanceof Player player)) {
            throw new IllegalStateException("Player cannot be null when creating storage screen");
        }
        
        // Create a storage menu
        int containerId = player.containerMenu.containerId + 1;
        StorageMenu storageMenu = new StorageMenu(containerId, player.getInventory(), townBlockPos);
        
        // IMPORTANT: Set the player's active container menu to the new storage menu
        // This is crucial for proper inventory interaction
        player.containerMenu = storageMenu;
        
        // Create the modal inventory screen
        return BCModalInventoryScreen.createStorageScreen(
                title,
                parentScreen,
                storageMenu,
                player.getInventory(),
                onCloseCallback)
                .withBackButtonText("Back to Town");
    }
} 
