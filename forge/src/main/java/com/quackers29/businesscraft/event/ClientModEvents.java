package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.init.ModMenuTypes;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.screens.town.TradeScreen;
import com.quackers29.businesscraft.ui.screens.town.StorageScreen;
import com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen;
import com.quackers29.businesscraft.client.TownDebugOverlay;
import com.quackers29.businesscraft.client.TownDebugNetwork;
import com.quackers29.businesscraft.platform.PlatformServices;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Platform-agnostic client-side event handling.
 * Uses EventHelper abstraction for cross-platform compatibility.
 */
public class ClientModEvents {
    
    /**
     * Initialize client-side platform-agnostic event handlers.
     * Should be called during client setup.
     */
    public static void initialize() {
        PlatformServices.getEventHelper().registerClientSetupEvent(ClientModEvents::onClientSetup);
        PlatformServices.getEventHelper().registerGuiOverlayRegistrationEvent(ClientModEvents::onGuiOverlayRegistration);
    }
    
    /**
     * Platform-agnostic client setup handler.
     */
    private static void onClientSetup() {
        // Register all menu screens in one place for consistency
        // Register the TownInterfaceScreen for the TOWN_INTERFACE menu type
        MenuScreens.register(ModMenuTypes.TOWN_INTERFACE.get(), TownInterfaceScreen::new);
        
        // Register the TradeScreen for the TRADE_MENU menu type
        MenuScreens.register(ModMenuTypes.TRADE_MENU.get(), TradeScreen::new);
        
        // Register the StorageScreen for the STORAGE_MENU menu type
        MenuScreens.register(ModMenuTypes.STORAGE_MENU.get(), StorageScreen::new);
        
        // Register the PaymentBoardScreen for the PAYMENT_BOARD_MENU menu type
        MenuScreens.register(ModMenuTypes.PAYMENT_BOARD_MENU.get(), PaymentBoardScreen::new);
        
        // Note: TOWN_BLOCK menu type is used internally only and doesn't need a screen registration
        
        // Register network handlers for debug overlay
        TownDebugNetwork.register();
    }
    
    /**
     * Platform-agnostic GUI overlay registration handler.
     */
    private static void onGuiOverlayRegistration(String id, Object overlay) {
        // This will be called by platform-specific implementation to register our overlay
        // For now, we'll register our specific overlay directly
        // In the actual implementation, this might be handled differently per platform
    }
}