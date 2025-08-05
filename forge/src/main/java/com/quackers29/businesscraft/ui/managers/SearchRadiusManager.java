package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.platform.SetSearchRadiusPacket;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages search radius functionality for town interfaces.
 * Extracted from TownInterfaceScreen to follow Single Responsibility Principle.
 */
public class SearchRadiusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchRadiusManager.class);
    
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 100;
    private static final int SMALL_INCREMENT = 1;
    private static final int LARGE_INCREMENT = 10;
    
    private int currentRadius;
    private final TownInterfaceMenu menu;
    private final BlockPos blockPos;
    private final TownDataCacheManager cacheManager;
    
    /**
     * Creates a new SearchRadiusManager.
     * 
     * @param menu The town interface menu
     * @param blockPos The block position for network packets
     * @param cacheManager The cache manager for immediate UI updates
     */
    public SearchRadiusManager(TownInterfaceMenu menu, BlockPos blockPos, TownDataCacheManager cacheManager) {
        this.menu = menu;
        this.blockPos = blockPos;
        this.cacheManager = cacheManager;
        this.currentRadius = menu.getSearchRadius();
        
        DebugConfig.debug(LOGGER, DebugConfig.SEARCH_RADIUS_MANAGER, "SearchRadiusManager initialized with radius: {}", currentRadius);
    }
    
    /**
     * Handles search radius change based on mouse button and modifiers.
     * 
     * @param mouseButton 0 for left click (increase), 1 for right click (decrease)
     * @param isShift Whether shift is held (for larger increments)
     * @return RadiusChangeResult containing the new radius and feedback message
     */
    public RadiusChangeResult handleRadiusChange(int mouseButton, boolean isShift) {
        try {
            int newRadius = calculateNewRadius(mouseButton, isShift);
            return applyRadiusChange(newRadius, mouseButton == 1);
        } catch (Exception e) {
            LOGGER.warn("Error handling radius change", e);
            return new RadiusChangeResult(currentRadius, "Error updating search radius");
        }
    }
    
    /**
     * Gets the current search radius.
     * 
     * @return The current radius value
     */
    public int getCurrentRadius() {
        return currentRadius;
    }
    
    /**
     * Updates the current radius from external sources (e.g., server updates).
     * 
     * @param newRadius The new radius value
     */
    public void updateRadius(int newRadius) {
        if (isValidRadius(newRadius)) {
            this.currentRadius = newRadius;
            DebugConfig.debug(LOGGER, DebugConfig.SEARCH_RADIUS_MANAGER, "Radius updated to: {}", newRadius);
        } else {
            LOGGER.warn("Attempted to set invalid radius: {}", newRadius);
        }
    }
    
    /**
     * Calculates the new radius based on input parameters.
     */
    private int calculateNewRadius(int mouseButton, boolean isShift) {
        boolean isDecrease = (mouseButton == 1);
        int increment = isShift ? LARGE_INCREMENT : SMALL_INCREMENT;
        int delta = isDecrease ? -increment : increment;
        
        return clampRadius(currentRadius + delta);
    }
    
    /**
     * Applies the radius change and updates all necessary components.
     */
    private RadiusChangeResult applyRadiusChange(int newRadius, boolean isDecrease) {
        // Update cached value immediately for UI feedback
        currentRadius = newRadius;
        cacheManager.updateCachedSearchRadius(newRadius);
        
        // Send network packet to update server
        sendRadiusUpdatePacket(newRadius);
        
        // Update menu if possible
        updateMenuRadius(newRadius);
        
        // Generate feedback message
        String feedbackMessage = generateFeedbackMessage(newRadius, isDecrease);
        
        DebugConfig.debug(LOGGER, DebugConfig.SEARCH_RADIUS_MANAGER, "Radius changed to: {} ({})", newRadius, isDecrease ? "decreased" : "increased");
        
        return new RadiusChangeResult(newRadius, feedbackMessage);
    }
    
    /**
     * Sends the radius update packet to the server.
     */
    private void sendRadiusUpdatePacket(int newRadius) {
        try {
            ModMessages.sendToServer(new SetSearchRadiusPacket(blockPos, newRadius));
        } catch (Exception e) {
            LOGGER.error("Failed to send radius update packet", e);
        }
    }
    
    /**
     * Updates the menu's cached radius value if possible.
     */
    private void updateMenuRadius(int newRadius) {
        if (menu instanceof TownInterfaceMenu) {
            ((TownInterfaceMenu) menu).setClientSearchRadius(newRadius);
        }
    }
    
    /**
     * Generates user feedback message for radius changes.
     */
    private String generateFeedbackMessage(int newRadius, boolean isDecrease) {
        String action = isDecrease ? "decreased" : "increased";
        return String.format("Search radius %s to %d", action, newRadius);
    }
    
    /**
     * Clamps the radius to valid bounds.
     */
    private int clampRadius(int radius) {
        return Math.max(MIN_RADIUS, Math.min(radius, MAX_RADIUS));
    }
    
    /**
     * Validates if a radius value is within acceptable bounds.
     */
    private boolean isValidRadius(int radius) {
        return radius >= MIN_RADIUS && radius <= MAX_RADIUS;
    }
    
    /**
     * Result of a radius change operation.
     */
    public static class RadiusChangeResult {
        private final int newRadius;
        private final String feedbackMessage;
        
        public RadiusChangeResult(int newRadius, String feedbackMessage) {
            this.newRadius = newRadius;
            this.feedbackMessage = feedbackMessage;
        }
        
        public int getNewRadius() {
            return newRadius;
        }
        
        public String getFeedbackMessage() {
            return feedbackMessage;
        }
    }
}