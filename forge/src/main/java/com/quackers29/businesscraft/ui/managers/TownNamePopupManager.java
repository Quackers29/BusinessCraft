package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.town.SetTownNamePacket;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.modal.core.BCPopupScreen;
import com.quackers29.businesscraft.ui.screens.BaseTownScreen;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Manages town name popup creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 * Now supports both static and instance-based usage.
 * 
 * TODO: Restore full functionality when BCPopupScreen is migrated to common
 */
public class TownNamePopupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownNamePopupManager.class);
    
    private final BaseTownScreen<?> screen;
    private BCPopupScreen activePopup;
    
    /**
     * Creates a new TownNamePopupManager for instance-based usage.
     * 
     * @param screen The parent screen
     */
    public TownNamePopupManager(BaseTownScreen<?> screen) {
        this.screen = screen;
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownNamePopupManager instance created");
    }
    
    /**
     * Shows the change town name popup using the screen's context.
     * TODO: Restore when BCPopupScreen is migrated to common
     */
    public void showChangeTownNamePopup() {
        LOGGER.warn("showChangeTownNamePopup temporarily disabled - BCPopupScreen not yet migrated");
    }
    
    /**
     * Gets the current town name from the screen's cache.
     */
    private String getCurrentTownName() {
        try {
            if (screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) {
                return ((com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) screen).getCachedTownName();
            }
            
            // Fallback to generic screen name
            return "Unknown Town";
        } catch (Exception e) {
            LOGGER.warn("Failed to get current town name", e);
            return "Unknown Town";
        }
    }
    
    /**
     * Gets the active popup instance.
     */
    public BCPopupScreen getActivePopup() {
        return activePopup;
    }
    
    /**
     * Closes the currently active popup if one exists.
     */
    public void closeActivePopup() {
        if (activePopup != null) {
            activePopup = null;
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Active popup closed");
        }
    }
    
    /**
     * Cleanup method to be called when the screen is closed.
     */
    public void cleanup() {
        closeActivePopup();
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownNamePopupManager cleanup completed");
    }
    
    /**
     * Creates and shows a town name change popup.
     * TODO: Restore when BCPopupScreen is migrated to common
     * 
     * @param currentTownName The current town name to display as default
     * @param blockPos The position of the town block
     * @param onPopupClosed Callback when popup is closed (receives the popup instance)
     * @return The created popup screen
     */
    public static BCPopupScreen showChangeTownNamePopup(
            String currentTownName, 
            BlockPos blockPos, 
            Consumer<BCPopupScreen> onPopupClosed) {
        // TODO: Restore when BCPopupScreen is migrated to common
        LOGGER.warn("showChangeTownNamePopup temporarily disabled - BCPopupScreen not yet migrated");
        return null;
    }
}