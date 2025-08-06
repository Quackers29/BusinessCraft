package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITownDataProvider;

/**
 * Factory for creating platform-specific position instances
 */
public interface PositionFactory {
    /**
     * Create a position from coordinates
     */
    ITownDataProvider.Position createPosition(int x, int y, int z);
    
    /**
     * Convert a platform-specific position to our common position interface
     */
    ITownDataProvider.Position fromPlatformPosition(Object platformPosition);
}