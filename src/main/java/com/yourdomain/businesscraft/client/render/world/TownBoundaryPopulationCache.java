package com.yourdomain.businesscraft.client.render.world;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple client-side cache to store town boundary data for boundary visualization.
 * This is needed because ContainerDataHelper only syncs when UI is open, but we need
 * boundary data for boundary visualization when UI is closed.
 */
public class TownBoundaryPopulationCache {
    private static final Map<BlockPos, Integer> boundaryCache = new ConcurrentHashMap<>();
    
    /**
     * Sets the population for a town at the given position (for backward compatibility)
     */
    public static void setPopulation(BlockPos pos, int population) {
        // For now, assume population = boundary radius (1:1 ratio)
        boundaryCache.put(pos, population);
    }
    
    /**
     * Sets the boundary radius for a town at the given position
     */
    public static void setBoundaryRadius(BlockPos pos, int boundaryRadius) {
        boundaryCache.put(pos, boundaryRadius);
    }
    
    /**
     * Gets the cached population for a town at the given position (for backward compatibility)
     * @param pos The town position
     * @param defaultValue The default value if no cached data exists
     * @return The population, or defaultValue if not cached
     */
    public static int getPopulation(BlockPos pos, int defaultValue) {
        return boundaryCache.getOrDefault(pos, defaultValue);
    }
    
    /**
     * Gets the cached boundary radius for a town at the given position
     * @param pos The town position
     * @param defaultValue The default value if no cached data exists
     * @return The boundary radius, or defaultValue if not cached
     */
    public static int getBoundaryRadius(BlockPos pos, int defaultValue) {
        return boundaryCache.getOrDefault(pos, defaultValue);
    }
    
    /**
     * Clears the cache (called on level unload)
     */
    public static void clear() {
        boundaryCache.clear();
    }
    
    /**
     * Removes a specific town from the cache
     */
    public static void remove(BlockPos pos) {
        boundaryCache.remove(pos);
    }
}