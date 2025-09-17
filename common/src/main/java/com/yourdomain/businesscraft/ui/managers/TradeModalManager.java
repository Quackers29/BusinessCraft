package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.ui.modal.factories.BCModalInventoryFactory;
import com.yourdomain.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/**
 * Manages trade modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 * Refactored to use BaseModalManager for common functionality.
 */
public class TradeModalManager extends BaseModalManager {
    
    /**
     * Creates and shows a trade resources modal screen.
     * 
     * @param parentScreen The parent screen to return to
     * @param blockPos The position of the town block
     * @param targetTab The tab to activate when returning
     * @param onScreenClosed Optional callback when screen is closed
     */
    public static void showTradeResourcesModal(
            Screen parentScreen, 
            BlockPos blockPos,
            String targetTab,
            Consumer<BCModalInventoryScreen<TradeMenu>> onScreenClosed) {
        
        // Validate inputs
        validateParentScreen(parentScreen, "parentScreen");
        
        // Prepare parent screen for modal display
        prepareParentScreen(parentScreen);
        
        // Create a modal trade screen using the factory with standardized callback
        BCModalInventoryScreen<TradeMenu> tradeScreen = BCModalInventoryFactory.createTradeScreen(
            Component.literal("Trade Resources"),
            parentScreen,
            blockPos,
            createStandardCallback(parentScreen, onScreenClosed)
        );
        
        // Show the trade screen as a modal overlay
        displayModal(tradeScreen);
    }
} 