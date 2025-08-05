package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITownDataProvider;

/**
 * Service for world/level related operations
 */
public interface WorldService {
    /**
     * Check if a position is loaded in the world
     */
    boolean isPositionLoaded(ITownDataProvider.Position position);
    
    /**
     * Calculate distance between two positions
     */
    double calculateDistance(ITownDataProvider.Position pos1, ITownDataProvider.Position pos2);
}