package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.ui.screens.BaseTownScreen;
import com.yourdomain.businesscraft.ui.screens.platform.PlatformManagementScreen;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Coordinates button actions for town interface screens.
 * Extracted from TownInterfaceScreen to separate UI action logic from screen management.
 */
public class ButtonActionCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ButtonActionCoordinator.class);
    
    private final BaseTownScreen<?> screen;
    private final ModalCoordinator modalCoordinator;
    private final TownNamePopupManager popupManager;
    
    /**
     * Creates a new ButtonActionCoordinator.
     * 
     * @param screen The parent screen
     * @param modalCoordinator The modal coordinator for modal actions
     */
    public ButtonActionCoordinator(BaseTownScreen<?> screen, ModalCoordinator modalCoordinator) {
        this.screen = screen;
        this.modalCoordinator = modalCoordinator;
        this.popupManager = new TownNamePopupManager(screen);
        
        LOGGER.debug("ButtonActionCoordinator initialized");
    }
    
    /**
     * Handles the edit town details action.
     */
    public void handleEditDetails() {
        try {
            LOGGER.debug("Handling edit details action");
            popupManager.showChangeTownNamePopup();
        } catch (Exception e) {
            LOGGER.error("Failed to handle edit details action", e);
            screen.sendChatMessage("Unable to open town name editor");
        }
    }
    
    /**
     * Handles the view visitors action.
     */
    public void handleViewVisitors() {
        try {
            LOGGER.debug("Handling view visitors action");
            modalCoordinator.showVisitorModal(null);
        } catch (Exception e) {
            LOGGER.error("Failed to handle view visitors action", e);
            screen.sendChatMessage("Unable to open visitor list");
        }
    }
    
    /**
     * Handles the trade resources action.
     */
    public void handleTradeResources() {
        try {
            LOGGER.debug("Handling trade resources action");
            modalCoordinator.showTradeModal(null);
        } catch (Exception e) {
            LOGGER.error("Failed to handle trade resources action", e);
            screen.sendChatMessage("Unable to open trade interface");
        }
    }
    
    /**
     * Handles the manage storage action.
     */
    public void handleManageStorage() {
        try {
            LOGGER.debug("Handling manage storage action");
            modalCoordinator.showStorageModal(null);
        } catch (Exception e) {
            LOGGER.error("Failed to handle manage storage action", e);
            screen.sendChatMessage("Unable to open storage interface");
        }
    }
    
    /**
     * Handles the assign jobs action.
     * Currently shows a placeholder message as the feature is not yet implemented.
     */
    public void handleAssignJobs() {
        LOGGER.debug("Handling assign jobs action (placeholder)");
        screen.sendChatMessage("Job assignment feature coming soon!");
    }
    
    /**
     * Handles the view visitor history action.
     */
    public void handleViewVisitorHistory() {
        try {
            LOGGER.debug("Handling view visitor history action");
            modalCoordinator.showVisitorHistoryModal(null);
        } catch (Exception e) {
            LOGGER.error("Failed to handle view visitor history action", e);
            screen.sendChatMessage("Unable to open visitor history");
        }
    }
    
    /**
     * Handles the save settings action.
     */
    public void handleSaveSettings() {
        try {
            LOGGER.debug("Handling save settings action");
            // TODO: Implement actual settings save logic
            screen.sendChatMessage("Settings saved successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to handle save settings action", e);
            screen.sendChatMessage("Failed to save settings");
        }
    }
    
    /**
     * Handles the reset defaults action.
     */
    public void handleResetDefaults() {
        try {
            LOGGER.debug("Handling reset defaults action");
            // TODO: Implement actual reset logic
            screen.sendChatMessage("Settings reset to defaults!");
        } catch (Exception e) {
            LOGGER.error("Failed to handle reset defaults action", e);
            screen.sendChatMessage("Failed to reset settings");
        }
    }
    
    /**
     * Handles the manage platforms action by opening the platform management screen.
     */
    public void handleManagePlatforms() {
        try {
            LOGGER.debug("Handling manage platforms action");
            openPlatformManagementScreen();
        } catch (Exception e) {
            LOGGER.error("Failed to handle manage platforms action", e);
            screen.sendChatMessage("Unable to open platform management");
        }
    }
    
    /**
     * Handles generic actions with a message.
     * 
     * @param action The action identifier
     */
    public void handleGenericAction(String action) {
        LOGGER.debug("Handling generic action: {}", action);
        screen.sendChatMessage("Action: " + action);
    }
    
    /**
     * Opens the platform management screen with proper error handling.
     */
    private void openPlatformManagementScreen() {
        // Validate menu state
        if (screen.getMenu() == null) {
            throw new IllegalStateException("Menu is not available");
        }
        
        // Get platform data with validation
        List<Platform> platforms = screen.getMenu().getPlatforms();
        if (platforms == null) {
            throw new IllegalStateException("Platform data not available");
        }
        
        // Get block position with validation
        if (screen.getMenu().getBlockPos() == null) {
            throw new IllegalStateException("Block position not available");
        }
        
        // Create and open the platform management screen
        PlatformManagementScreen platformScreen = new PlatformManagementScreen(
            screen.getMenu().getBlockPos(), 
            platforms
        );
        
        Minecraft.getInstance().setScreen(platformScreen);
        LOGGER.debug("Platform management screen opened successfully");
    }
    
    /**
     * Validates that all required dependencies are available.
     * 
     * @return true if all dependencies are valid
     */
    public boolean validateDependencies() {
        if (screen == null) {
            LOGGER.error("Screen is null");
            return false;
        }
        
        if (modalCoordinator == null) {
            LOGGER.error("ModalCoordinator is null");
            return false;
        }
        
        if (!modalCoordinator.validateDependencies()) {
            LOGGER.error("ModalCoordinator dependencies are invalid");
            return false;
        }
        
        return true;
    }
    
    /**
     * Performs cleanup when the coordinator is no longer needed.
     */
    public void cleanup() {
        try {
            if (popupManager != null) {
                popupManager.cleanup();
            }
            LOGGER.debug("ButtonActionCoordinator cleanup completed");
        } catch (Exception e) {
            LOGGER.warn("Error during ButtonActionCoordinator cleanup", e);
        }
    }
}