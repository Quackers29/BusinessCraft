package com.yourdomain.businesscraft.client.render.world;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple client-side cache to store town population data for boundary visualization.
 * This is needed because ContainerDataHelper only syncs when UI is open, but we need
 * population data for boundary visualization when UI is closed.
 */
public class TownBoundaryPopulationCache {
    private static final Map<BlockPos, Integer> populationCache = new ConcurrentHashMap<>();
    
    /**
     * Sets the population for a town at the given position
     */
    public static void setPopulation(BlockPos pos, int population) {
        populationCache.put(pos, population);
    }
    
    /**
     * Gets the cached population for a town at the given position
     * @param pos The town position
     * @param defaultValue The default value if no cached data exists
     * @return The population, or defaultValue if not cached
     */
    public static int getPopulation(BlockPos pos, int defaultValue) {
        return populationCache.getOrDefault(pos, defaultValue);
    }
    
    /**
     * Clears the cache (called on level unload)
     */
    public static void clear() {
        populationCache.clear();
    }
    
    /**
     * Removes a specific town from the cache
     */
    public static void remove(BlockPos pos) {
        populationCache.remove(pos);
    }
}