package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.ui.screens.BaseTownScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "ButtonActionCoordinator initialized");
    }
    
    /**
     * Handles the edit town details action.
     */
    public void handleEditDetails() {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling edit details action");
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
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling view visitors action");
            // Save current tab before opening modal
            if (screen instanceof com.quackers29.businesscraft.ui.screens.BaseTownScreen) {
                ((com.quackers29.businesscraft.ui.screens.BaseTownScreen<?>) screen).saveActiveTab();
            }
            
            modalCoordinator.showVisitorModal(closedModal -> {
                // Trigger data refresh when modal closes
                refreshScreenData();
            });
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
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling trade resources action");
            modalCoordinator.showTradeModal("resources", result -> {
                // Additional refresh if needed
                refreshScreenData();
            });
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
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling manage storage action");
            modalCoordinator.showStorageModal("resources", result -> {
                // Additional refresh if needed
                refreshScreenData();
            });
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
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling assign jobs action (placeholder)");
        screen.sendChatMessage("Job assignment feature coming soon!");
    }
    
    /**
     * Handles the view visitor history action.
     */
    public void handleViewVisitorHistory() {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling view visitor history action");
            String currentTab = getCurrentActiveTab();
            // Pass the current tab to the modal so it knows where to return
            modalCoordinator.showVisitorHistoryModal(currentTab, result -> {
                // Additional refresh if needed
                refreshScreenData();
            });
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
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling save settings action");
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
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling reset defaults action");
            // TODO: Implement actual reset logic
            screen.sendChatMessage("Settings reset to defaults!");
        } catch (Exception e) {
            LOGGER.error("Failed to handle reset defaults action", e);
            screen.sendChatMessage("Failed to reset settings");
        }
    }
    
    /**
     * Handles the map view action by opening the town map modal.
     */
    public void handleManagePlatforms() {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling map view action");
            // Save current tab before opening map modal
            if (screen instanceof com.quackers29.businesscraft.ui.screens.BaseTownScreen) {
                ((com.quackers29.businesscraft.ui.screens.BaseTownScreen<?>) screen).saveActiveTab();
            }
            
            openTownMapModal();
        } catch (Exception e) {
            LOGGER.error("Failed to handle map view action", e);
            screen.sendChatMessage("Unable to open town map");
        }
    }
    
    /**
     * Handles generic actions with a message.
     * 
     * @param action The action identifier
     */
    public void handleGenericAction(String action) {
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Handling generic action: {}", action);
        screen.sendChatMessage("Action: " + action);
    }
    
    /**
     * Opens the town map modal with proper error handling.
     */
    private void openTownMapModal() {
        // Validate menu state
        if (screen.getMenu() == null) {
            throw new IllegalStateException("Menu is not available");
        }
        
        // Get current town position
        BlockPos currentTownPos = screen.getMenu().getBlockPos();
        if (currentTownPos == null) {
            throw new IllegalStateException("Block position not available");
        }
        
        // Create and show the town map modal
        com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal = 
            new com.quackers29.businesscraft.ui.modal.specialized.TownMapModal(
                Minecraft.getInstance().screen,
                currentTownPos,
                closedModal -> {
                    // Callback when modal closes - refresh data if needed
                    refreshScreenData();
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town map modal closed");
                }
            );
        
        // Load town data from client cache if available
        com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache cache = 
            com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache.getInstance();
        mapModal.setTownData(cache.getAllTowns());
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town map modal opened from current position: {}", currentTownPos);
        
        Minecraft.getInstance().setScreen(mapModal);
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
     * Gets the currently active tab ID.
     * 
     * @return The active tab ID, or "overview" as fallback
     */
    private String getCurrentActiveTab() {
        try {
            if (screen.getTabPanel() != null) {
                String activeTab = screen.getTabPanel().getActiveTabId();
                return activeTab != null ? activeTab : "overview";
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get current active tab", e);
        }
        return "overview";
    }
    
    /**
     * Returns to the specified tab, with fallback to current tab.
     * 
     * @param targetTab The tab to return to
     * @param fallbackTab The fallback tab if target is not available
     */
    private void returnToTab(String targetTab, String fallbackTab) {
        try {
            if (screen.getTabPanel() != null) {
                // Try target tab first, then fallback, then overview
                String tabToActivate = targetTab;
                if (!isTabAvailable(targetTab)) {
                    tabToActivate = fallbackTab;
                    if (!isTabAvailable(fallbackTab)) {
                        tabToActivate = "overview";
                    }
                }
                
                screen.getTabPanel().setActiveTab(tabToActivate);
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Returned to tab: {}", tabToActivate);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to return to tab: {}", targetTab, e);
        }
    }
    
    /**
     * Checks if a tab is available in the tab panel.
     * 
     * @param tabId The tab ID to check
     * @return True if the tab exists
     */
    private boolean isTabAvailable(String tabId) {
        try {
            return screen.getTabPanel() != null && screen.getTabPanel().getTabPanel(tabId) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Refreshes screen data after modal operations.
     */
    private void refreshScreenData() {
        try {
            // Refresh cache data
            if (screen.getCacheManager() != null) {
                // screen.getCacheManager().refreshCachedValues();  // Removed - less aggressive refresh
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Modal closed - relying on normal data sync mechanisms");
            }
            
            // Force refresh specific tabs that might have changed (keep this for resources)
            if (screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) {
                ((com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) screen).forceRefreshResourcesTab();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to refresh screen data", e);
        }
    }
    
    /**
     * Performs cleanup when the coordinator is no longer needed.
     */
    public void cleanup() {
        try {
            if (popupManager != null) {
                popupManager.cleanup();
            }
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "ButtonActionCoordinator cleanup completed");
        } catch (Exception e) {
            LOGGER.warn("Error during ButtonActionCoordinator cleanup", e);
        }
    }
}
