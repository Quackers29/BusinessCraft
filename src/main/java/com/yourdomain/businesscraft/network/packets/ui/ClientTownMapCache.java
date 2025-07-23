package com.yourdomain.businesscraft.network.packets.ui;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * Client-side cache for town map data.
 * Singleton class that stores town information for map display.
 */
public class ClientTownMapCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTownMapCache.class);
    private static ClientTownMapCache instance;
    
    private final Map<UUID, TownMapDataResponsePacket.TownMapInfo> townData = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, TownPlatformDataResponsePacket.PlatformInfo>> platformData = new ConcurrentHashMap<>();
    private long lastUpdateTime = 0;
    
    private ClientTownMapCache() {
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized ClientTownMapCache getInstance() {
        if (instance == null) {
            instance = new ClientTownMapCache();
        }
        return instance;
    }
    
    /**
     * Update the cached town data
     */
    public void updateTownData(Map<UUID, TownMapDataResponsePacket.TownMapInfo> newData) {
        int oldSize = townData.size();
        townData.clear();
        townData.putAll(newData);
        lastUpdateTime = System.currentTimeMillis();
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
            "Town map cache updated: {} -> {} towns", oldSize, townData.size());
    }
    
    /**
     * Get all cached town data
     */
    public Map<UUID, TownMapDataResponsePacket.TownMapInfo> getAllTowns() {
        return new HashMap<>(townData);
    }
    
    /**
     * Get town data by ID
     */
    public TownMapDataResponsePacket.TownMapInfo getTown(UUID id) {
        return townData.get(id);
    }
    
    /**
     * Check if the cache has data
     */
    public boolean hasData() {
        return !townData.isEmpty();
    }
    
    /**
     * Check if the cache data is stale (older than 30 seconds)
     */
    public boolean isStale() {
        return System.currentTimeMillis() - lastUpdateTime > 30000;
    }
    
    /**
     * Update platform data for a specific town
     */
    public void updateTownPlatformData(UUID townId, Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms) {
        platformData.put(townId, new HashMap<>(platforms));
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
            "Platform data updated for town {}: {} platforms", townId, platforms.size());
    }
    
    /**
     * Get platform data for a specific town
     */
    public Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> getTownPlatformData(UUID townId) {
        Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms = platformData.get(townId);
        return platforms != null ? new HashMap<>(platforms) : new HashMap<>();
    }
    
    /**
     * Check if platform data exists for a town
     */
    public boolean hasTownPlatformData(UUID townId) {
        return platformData.containsKey(townId);
    }
    
    /**
     * Clear platform data for a specific town
     */
    public void clearTownPlatformData(UUID townId) {
        platformData.remove(townId);
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
            "Platform data cleared for town {}", townId);
    }
    
    /**
     * Update town information for a specific town
     */
    public void updateTownInfo(UUID townId, String name, int population, int touristCount) {
        TownMapDataResponsePacket.TownMapInfo existingTown = townData.get(townId);
        if (existingTown != null) {
            // Create updated town info with fresh data
            TownMapDataResponsePacket.TownMapInfo updatedTown = new TownMapDataResponsePacket.TownMapInfo(
                townId, name, existingTown.position, population, touristCount
            );
            townData.put(townId, updatedTown);
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Town data updated for town {}: pop={}, tourists={}", townId, population, touristCount);
        }
    }
    
    /**
     * Clear the cache
     */
    public void clear() {
        townData.clear();
        platformData.clear();
        lastUpdateTime = 0;
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town map cache cleared");
    }
    
    /**
     * Get the number of cached towns
     */
    public int size() {
        return townData.size();
    }
}