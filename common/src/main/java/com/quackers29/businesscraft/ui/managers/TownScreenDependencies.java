package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.ui.screens.BaseTownScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Dependency container for town screen components.
 * Implements dependency injection pattern to reduce coupling between components.
 */
public class TownScreenDependencies {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownScreenDependencies.class);
    
    private final ModalCoordinator modalCoordinator;
    private final SearchRadiusManager radiusManager;
    private final ButtonActionCoordinator buttonCoordinator;
    private final TownDataCacheManager cacheManager;
    
    /**
     * Private constructor - use factory method create() instead.
     */
    private TownScreenDependencies(ModalCoordinator modalCoordinator,
                                  SearchRadiusManager radiusManager,
                                  ButtonActionCoordinator buttonCoordinator,
                                  TownDataCacheManager cacheManager) {
        this.modalCoordinator = modalCoordinator;
        this.radiusManager = radiusManager;
        this.buttonCoordinator = buttonCoordinator;
        this.cacheManager = cacheManager;
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_SCREEN_DEPENDENCIES, "TownScreenDependencies container created");
    }
    
    /**
     * Creates a new dependency container with all required components.
     * 
     * @param menu The town interface menu
     * @param screen The parent screen
     * @param cacheManager The cache manager instance
     * @return A new TownScreenDependencies container
     * @throws IllegalArgumentException if any required parameter is null
     */
    public static TownScreenDependencies create(TownInterfaceMenu menu, 
                                              BaseTownScreen<?> screen,
                                              TownDataCacheManager cacheManager) {
        validateInputs(menu, screen, cacheManager);
        
        try {
            // Create modal coordinator
            ModalCoordinator modalCoordinator = new ModalCoordinator(screen, menu);
            
            // Create search radius manager
            SearchRadiusManager radiusManager = new SearchRadiusManager(
                menu, 
                menu.getBlockPos(), 
                cacheManager
            );
            
            // Create button action coordinator
            ButtonActionCoordinator buttonCoordinator = new ButtonActionCoordinator(
                screen, 
                modalCoordinator
            );
            
            // Validate all dependencies were created successfully
            validateDependencies(modalCoordinator, radiusManager, buttonCoordinator, cacheManager);
            
            TownScreenDependencies dependencies = new TownScreenDependencies(
                modalCoordinator, 
                radiusManager, 
                buttonCoordinator, 
                cacheManager
            );
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SCREEN_DEPENDENCIES, "TownScreenDependencies created successfully");
            return dependencies;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create TownScreenDependencies", e);
            throw new RuntimeException("Failed to create screen dependencies", e);
        }
    }
    
    /**
     * Validates input parameters for dependency creation.
     */
    private static void validateInputs(TownInterfaceMenu menu, 
                                     BaseTownScreen<?> screen, 
                                     TownDataCacheManager cacheManager) {
        if (menu == null) {
            throw new IllegalArgumentException("Menu cannot be null");
        }
        if (screen == null) {
            throw new IllegalArgumentException("Screen cannot be null");
        }
        if (cacheManager == null) {
            throw new IllegalArgumentException("Cache manager cannot be null");
        }
        if (menu.getBlockPos() == null) {
            throw new IllegalArgumentException("Menu block position cannot be null");
        }
    }
    
    /**
     * Validates that all dependencies were created successfully.
     */
    private static void validateDependencies(ModalCoordinator modalCoordinator,
                                           SearchRadiusManager radiusManager,
                                           ButtonActionCoordinator buttonCoordinator,
                                           TownDataCacheManager cacheManager) {
        if (modalCoordinator == null || !modalCoordinator.validateDependencies()) {
            throw new RuntimeException("ModalCoordinator validation failed");
        }
        if (radiusManager == null) {
            throw new RuntimeException("SearchRadiusManager creation failed");
        }
        if (buttonCoordinator == null || !buttonCoordinator.validateDependencies()) {
            throw new RuntimeException("ButtonActionCoordinator validation failed");
        }
        if (cacheManager == null) {
            throw new RuntimeException("TownDataCacheManager is null");
        }
    }
    
    /**
     * Gets the modal coordinator instance.
     * 
     * @return The modal coordinator
     */
    public ModalCoordinator getModalCoordinator() {
        return modalCoordinator;
    }
    
    /**
     * Gets the search radius manager instance.
     * 
     * @return The search radius manager
     */
    public SearchRadiusManager getRadiusManager() {
        return radiusManager;
    }
    
    /**
     * Gets the button action coordinator instance.
     * 
     * @return The button action coordinator
     */
    public ButtonActionCoordinator getButtonCoordinator() {
        return buttonCoordinator;
    }
    
    /**
     * Gets the cache manager instance.
     * 
     * @return The cache manager
     */
    public TownDataCacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Validates that all dependencies are still functional.
     * 
     * @return true if all dependencies are valid and functional
     */
    public boolean validateAllDependencies() {
        try {
            if (modalCoordinator == null || !modalCoordinator.validateDependencies()) {
                LOGGER.warn("ModalCoordinator validation failed");
                return false;
            }
            
            if (radiusManager == null) {
                LOGGER.warn("SearchRadiusManager is null");
                return false;
            }
            
            if (buttonCoordinator == null || !buttonCoordinator.validateDependencies()) {
                LOGGER.warn("ButtonActionCoordinator validation failed");
                return false;
            }
            
            if (cacheManager == null) {
                LOGGER.warn("TownDataCacheManager is null");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error during dependency validation", e);
            return false;
        }
    }
    
    /**
     * Performs cleanup of all managed dependencies.
     * Call this when the screen is closed to ensure proper resource cleanup.
     */
    public void cleanup() {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SCREEN_DEPENDENCIES, "Starting TownScreenDependencies cleanup");
            
            if (modalCoordinator != null) {
                modalCoordinator.cleanup();
            }
            
            if (buttonCoordinator != null) {
                buttonCoordinator.cleanup();
            }
            
            // Note: radiusManager and cacheManager don't currently have cleanup methods
            // but this provides a place to add them in the future if needed
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SCREEN_DEPENDENCIES, "TownScreenDependencies cleanup completed successfully");
            
        } catch (Exception e) {
            LOGGER.warn("Error during TownScreenDependencies cleanup", e);
        }
    }
}
