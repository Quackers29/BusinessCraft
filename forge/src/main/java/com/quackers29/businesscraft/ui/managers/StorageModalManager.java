package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.ui.modal.factories.BCModalInventoryFactory;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.ui.OpenPaymentBoardPacket;
import java.util.function.Consumer;

/**
 * Manages storage modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 * Refactored to use BaseModalManager for common functionality.
 */
public class StorageModalManager extends BaseModalManager {
    
    /**
     * Creates and shows a payment board screen using proper Minecraft container system.
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
        
        // Send packet to server to open Payment Board using proper container system
        // This ensures proper server-client synchronization via NetworkHooks
        ModMessages.sendToServer(new OpenPaymentBoardPacket(blockPos));
    }
} 