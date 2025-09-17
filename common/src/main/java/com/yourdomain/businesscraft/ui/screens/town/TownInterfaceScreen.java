package com.yourdomain.businesscraft.ui.screens.town;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.ui.managers.*;
import com.yourdomain.businesscraft.ui.screens.BaseTownScreen;
import com.yourdomain.businesscraft.ui.tabs.ResourcesTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.network.packets.ui.PlayerExitUIPacket;
import com.yourdomain.businesscraft.network.packets.platform.SetSearchRadiusPacket;
import net.minecraft.core.BlockPos;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import com.yourdomain.businesscraft.ui.screens.platform.PlatformManagementScreenV2;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * The Town Interface Screen showcases the BusinessCraft UI system capabilities.
 * This screen demonstrates various UI components and layouts using the enhanced BCTabPanel.
 * Refactored to use dependency injection and extracted managers for better separation of concerns.
 */
public class TownInterfaceScreen extends BaseTownScreen<TownInterfaceMenu> 
        implements BottomButtonManager.ButtonActionHandler {

    // Manager instances specific to this screen
    private BottomButtonManager buttonManager;
    
    // Dependency container for coordinated functionality
    private TownScreenDependencies dependencies;
    
    // Cache the current search radius for UI updates
    private int currentSearchRadius;

    public TownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Initialize cached values from the menu
        this.currentSearchRadius = menu.getSearchRadius();
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "TownInterfaceScreen created with radius: {}", currentSearchRadius);
    }
    
    @Override
    protected void initializeSpecificManagers() {
        try {
            // Create dependency container with all coordinated functionality
            this.dependencies = TownScreenDependencies.create(menu, this, cacheManager);
            
            // Initialize managers specific to TownInterfaceScreen
            this.buttonManager = new BottomButtonManager(this);
            this.eventHandler = new TownScreenEventHandler(this, this, this.buttonManager);
            this.renderManager = new TownScreenRenderManager(this, this, this);
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "TownInterfaceScreen managers initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize TownInterfaceScreen managers", e);
            throw new RuntimeException("Screen initialization failed", e);
        }
    }

    @Override
    protected void performAdditionalInit() {
        // Initialize the button manager with screen position
        buttonManager.updateScreenPosition(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        buttonManager.createBottomButtonsGrid();
    }

    @Override
    protected void performAdditionalCleanup() {
        try {
            // Clean up dependencies first
            if (dependencies != null) {
                dependencies.cleanup();
                dependencies = null;
                DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Dependencies cleaned up successfully");
            }
            
            // Send a packet to register the player exit UI to show platform indicators
            BlockPos blockPos = this.menu.getBlockPos();
            if (blockPos != null) {
                // Send a packet to the server to register player exit UI
                PlatformAccess.getNetworkMessages().sendToServer(new PlayerExitUIPacket(blockPos));
                DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Player exit UI packet sent");
            }
        } catch (Exception e) {
            LOGGER.warn("Error during additional cleanup", e);
        }
    }
    
    // ===== Interface Implementations =====
    
    // ButtonActionHandler implementation - delegates to ButtonActionCoordinator
    @Override
    public void onEditDetails() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleEditDetails();
        } else {
            LOGGER.warn("Dependencies not available for onEditDetails");
        }
    }
    
    @Override
    public void onViewVisitors() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleViewVisitors();
        } else {
            LOGGER.warn("Dependencies not available for onViewVisitors");
        }
    }
    
    @Override
    public void onTradeResources() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleTradeResources();
        } else {
            LOGGER.warn("Dependencies not available for onTradeResources");
        }
    }
    
    @Override
    public void onManageStorage() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleManageStorage();
        } else {
            LOGGER.warn("Dependencies not available for onManageStorage");
        }
    }
    
    @Override
    public void onAssignJobs() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleAssignJobs();
        } else {
            sendChatMessage("Job assignment feature coming soon!");
        }
    }
    
    @Override
    public void onViewVisitorHistory() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleViewVisitorHistory();
        } else {
            LOGGER.warn("Dependencies not available for onViewVisitorHistory");
        }
    }
    
    @Override
    public void onSaveSettings() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleSaveSettings();
        } else {
            sendChatMessage("Settings saved successfully!");
        }
    }
    
    @Override
    public void onResetDefaults() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleResetDefaults();
        } else {
            sendChatMessage("Settings reset to defaults!");
        }
    }
            
    @Override
    public void onManagePlatforms() {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleManagePlatforms();
        } else {
            openPlatformManagementScreen(); // Fallback to legacy method
        }
    }
            
    @Override
    public void onGenericAction(String action) {
        if (dependencies != null) {
            dependencies.getButtonCoordinator().handleGenericAction(action);
        } else {
            sendChatMessage("Action: " + action);
        }
    }
    
    // ===== Specific Interface Implementations =====
    
    @Override
    public void setCurrentSearchRadius(int radius) {
        this.currentSearchRadius = radius;
                }
                
    @Override
    public int getSearchRadiusFromMenu() {
        return menu.getSearchRadius();
                }
    
    @Override
    public BottomButtonManager getButtonManager() {
        return buttonManager;
    }
    
    /**
     * Opens the platform management screen
     * This method is used by tab implementations.
     */
    public void openPlatformManagementScreen() {
        // Get real platform data from the menu rather than using sample data
        List<Platform> platforms = menu.getPlatforms();
        
        // Create the platform management screen V2 (using BC UI framework)
        PlatformManagementScreenV2 platformScreen = new PlatformManagementScreenV2(
            menu.getBlockPos(), platforms);
        
        // Close this screen and open the platform screen
        // This ensures when the user navigates back from the platform screen,
        // they return to the town interface
        this.minecraft.setScreen(platformScreen);
    }
    
    /**
     * Handles changes to the search radius using the extracted SearchRadiusManager.
     * This method is used by tab implementations.
     */
    public void handleRadiusChange(int mouseButton) {
        if (dependencies == null) {
            LOGGER.warn("Dependencies not initialized, cannot handle radius change");
            // Chat message removed
            return;
        }
        
        try {
            SearchRadiusManager radiusManager = dependencies.getRadiusManager();
            boolean isShift = hasShiftDown();
            
            SearchRadiusManager.RadiusChangeResult result = radiusManager.handleRadiusChange(mouseButton, isShift);
            
            // Update our cached value
            currentSearchRadius = result.getNewRadius();
            
            // User feedback removed - no chat message for radius changes
            
            // Play a click sound for feedback
            playButtonClickSound();
            
            DebugConfig.debug(LOGGER, DebugConfig.SEARCH_RADIUS_MANAGER, "Search radius changed to: {}", result.getNewRadius());
            
        } catch (Exception e) {
            LOGGER.error("Error handling radius change", e);
            // Error chat message removed
        }
    }
    
    /**
     * Gets the current search radius for UI display
     * @return The current search radius
     */
    public int getCurrentSearchRadius() {
        return currentSearchRadius;
    }
    
    // Helper methods to get data from the cache manager
    public String getCachedTownName() {
        return cacheManager.getCachedTownName();
    }
    
    public int getCachedPopulation() {
        return cacheManager.getCachedPopulation();
    }
    
    // Changed from private to public to allow access from tab implementations
    public Map<Item, Integer> getCachedResources() {
        return cacheManager.getCachedResources();
    }
    
    public String getTouristString() {
        return cacheManager.getTouristString();
            }
            
    // ===== Legacy Modal Methods - Now Handled by Coordinators =====
    // These methods are kept for backwards compatibility but delegate to coordinators
    
    /**
     * Close the active modal if one exists
     */
    @Override
    protected void closeActiveModal() {
        this.activeModal = null;
    }
    
    /**
     * Force refresh the resources tab when resource changes are detected
     * This can be called externally when resource updates occur
     */
    public void forceRefreshResourcesTab() {
        if (tabController != null) {
            Object resourcesTab = tabController.getTab("resources");
            if (resourcesTab instanceof ResourcesTab) {
                ((ResourcesTab) resourcesTab).forceRefresh();
            }
        }
    }
    
    /**
     * Gets the cache manager for external access
     * @return The cache manager instance
     */
    public TownDataCacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Invalidates the cached data and forces a refresh of all tabs.
     * Called when town data changes (e.g., after renaming) to ensure UI shows updated information.
     */
    public void invalidateCache() {
        if (cacheManager != null) {
            cacheManager.invalidateCache();
            cacheManager.refreshCachedValues();
            
            // Force refresh all tabs to show updated data
            if (tabController != null) {
                tabController.forceRefreshAllTabs();
            }
        }
    }
} 