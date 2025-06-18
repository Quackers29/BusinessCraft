package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.menu.PaymentBoardMenu;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.ui.modal.factories.BCModalInventoryFactory;
import com.yourdomain.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import com.yourdomain.businesscraft.ui.screens.town.PaymentBoardScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.function.Consumer;

/**
 * Manages storage modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 * Refactored to use BaseModalManager for common functionality.
 */
public class StorageModalManager extends BaseModalManager {
    
    /**
     * Creates and shows a payment board screen (replacing storage modal).
     * 
     * @param parentScreen The parent screen to return to
     * @param blockPos The position of the town block
     * @param townMenu The town interface menu for accessing storage data
     * @param targetTab The tab to activate when returning
     * @param onScreenClosed Optional callback when screen is closed
     */
    public static void showStorageModal(
            Screen parentScreen, 
            BlockPos blockPos, 
            TownInterfaceMenu townMenu,
            String targetTab,
            Consumer<BCModalInventoryScreen<StorageMenu>> onScreenClosed) {
        
        // Validate inputs
        validateParentScreen(parentScreen, "parentScreen");
        
        // Get the player
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Player cannot be null when creating payment board screen");
        }
        
        // Create a payment board menu
        int containerId = player.containerMenu.containerId + 1;
        PaymentBoardMenu paymentBoardMenu = new PaymentBoardMenu(containerId, player.getInventory(), blockPos);
        
        // Set the player's active container menu to the new payment board menu
        player.containerMenu = paymentBoardMenu;
        
        // Create the payment board screen
        PaymentBoardScreen paymentBoardScreen = new PaymentBoardScreen(
            paymentBoardMenu, 
            player.getInventory(), 
            Component.literal("Payment Board")
        );
        
        // Show the payment board screen
        Minecraft.getInstance().setScreen(paymentBoardScreen);
    }
} 