package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.api.PlatformAccess;
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
     */
    public void showChangeTownNamePopup() {
        try {
            if (screen.getMenu() == null) {
                throw new IllegalStateException("Screen menu is not available");
            }
            
            String currentTownName = getCurrentTownName();
            BlockPos blockPos = screen.getMenu().getBlockPos();
            
            if (blockPos == null) {
                throw new IllegalStateException("Block position is not available");
            }
            
            // Create the popup
            activePopup = BCComponentFactory.createStringInputPopup(
                "Change Town Name", 
                currentTownName, // Initial value
                result -> {
                    // Handle the result
                    if (result.isConfirmed() && !result.getStringValue().isEmpty()) {
                        String newName = result.getStringValue().trim();
                        
                        // Send packet to update town name on the server
                        PlatformAccess.getNetworkMessages().sendToServer(
                            new SetTownNamePacket(blockPos, newName)
                        );
                        
                        // Provide immediate client-side feedback
                        TownNamePopupManager.sendChatMessage("Changing town name to: " + newName);
                    }
                    
                    // Close the popup by clearing it from the screen
                    clearScreenActivePopup(screen);
                    activePopup = null;
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town name popup closed");
                }
            );
            
            // Set the popup as active for rendering
            if (activePopup != null) {
                setScreenActivePopup(screen, activePopup);
                activePopup.focusInput(); // Focus the input field for immediate typing
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town name popup set as active with input focused");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to show change town name popup", e);
            screen.sendChatMessage("Unable to open town name editor");
        }
    }
    
    /**
     * Gets the current town name from the screen's cache.
     */
    private String getCurrentTownName() {
        try {
            if (screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) {
                return ((com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) screen).getCachedTownName();
            }
            
            // Fallback: try to get from menu
            if (screen.getMenu() instanceof TownInterfaceMenu) {
                // TODO: Add method to get town name from menu if available
                return "Unknown Town";
            }
            
            return "Unknown Town";
        } catch (Exception e) {
            LOGGER.warn("Failed to get current town name", e);
            return "Unknown Town";
        }
    }
    
    /**
     * Gets the currently active popup, if any.
     * 
     * @return The active popup or null
     */
    public BCPopupScreen getActivePopup() {
        return activePopup;
    }
    
    /**
     * Closes the active popup if one exists.
     */
    public void closeActivePopup() {
        if (activePopup != null) {
            activePopup = null;
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Active popup closed");
        }
    }
    
    /**
     * Performs cleanup when the manager is no longer needed.
     */
    public void cleanup() {
        closeActivePopup();
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownNamePopupManager cleanup completed");
    }
    
    /**
     * Creates and shows a town name change popup.
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
        
        // Create a popup for changing the town name
        BCPopupScreen popup = BCComponentFactory.createStringInputPopup(
            "Change Town Name", 
            currentTownName, // Initial value
            result -> {
                // Handle the result
                if (result.isConfirmed() && !result.getStringValue().isEmpty()) {
                    String newName = result.getStringValue().trim();
                    
                    // Send packet to update town name on the server
                    PlatformAccess.getNetworkMessages().sendToServer(
                        new SetTownNamePacket(blockPos, newName)
                    );
                    
                    // Provide immediate client-side feedback
                    sendChatMessage("Changing town name to: " + newName);
                }
            }
        );
        
        // Position the popup at screen center
        positionPopupAtCenter(popup);
        
        // Set close handler
        popup.setClosePopupHandler(button -> {
            if (onPopupClosed != null) {
                onPopupClosed.accept(null); // Clear the popup reference
            }
        });
        
        return popup;
    }
    
    /**
     * Positions a popup at the center of the screen.
     * 
     * @param popup The popup to position
     */
    private static void positionPopupAtCenter(BCPopupScreen popup) {
        // Get screen dimensions
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // Calculate exact center position
        int popupWidth = 300; // Same as in createStringInputPopup
        int popupHeight = 150; // Same as in createStringInputPopup
        int centerX = screenWidth / 2 - popupWidth / 2;
        int centerY = screenHeight / 2 - popupHeight / 2;
        
        // Directly position the popup at the center of the screen
        popup.position(centerX, centerY);
    }
    
    /**
     * Helper method to send a chat message to the player.
     * 
     * @param message The message to send
     */
    private static void sendChatMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }
    
    /**
     * Sets the active popup on the screen using reflection to access the protected field.
     * 
     * @param screen The screen to set the popup on
     * @param popup The popup to set as active
     */
    private void setScreenActivePopup(BaseTownScreen<?> screen, BCPopupScreen popup) {
        try {
            // Use reflection to access the protected activePopup field
            java.lang.reflect.Field activePopupField = BaseTownScreen.class.getDeclaredField("activePopup");
            activePopupField.setAccessible(true);
            activePopupField.set(screen, popup);
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Successfully set activePopup on screen via reflection");
        } catch (Exception e) {
            LOGGER.error("Failed to set activePopup on screen", e);
            throw new RuntimeException("Cannot set popup on screen", e);
        }
    }
    
    /**
     * Clears the active popup from the screen using reflection to access the protected field.
     * 
     * @param screen The screen to clear the popup from
     */
    private void clearScreenActivePopup(BaseTownScreen<?> screen) {
        try {
            // Use reflection to access the protected activePopup field
            java.lang.reflect.Field activePopupField = BaseTownScreen.class.getDeclaredField("activePopup");
            activePopupField.setAccessible(true);
            activePopupField.set(screen, null);
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Successfully cleared activePopup on screen via reflection");
        } catch (Exception e) {
            LOGGER.error("Failed to clear activePopup on screen", e);
            throw new RuntimeException("Cannot clear popup on screen", e);
        }
    }
} 
