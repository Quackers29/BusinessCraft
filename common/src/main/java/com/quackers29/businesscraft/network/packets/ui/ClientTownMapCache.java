package com.quackers29.businesscraft.network.packets.ui;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Client-side cache for town map data.
 * Singleton class that stores town information for map display.
 */
public class ClientTownMapCache {
    private static final int CACHE_EXPIRY_MS = 30000;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTownMapCache.class);
    private static ClientTownMapCache instance;
    
    private final Map<UUID, TownMapDataResponsePacket.TownMapInfo> townData = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, TownPlatformDataResponsePacket.PlatformInfo>> platformData = new ConcurrentHashMap<>();
    private long lastUpdateTime = 0;
    
    private ClientTownMapCache() {
    }
    
    public static synchronized ClientTownMapCache getInstance() {
        if (instance == null) {
            instance = new ClientTownMapCache();
        }
        return instance;
    }
    
    public void updateTownData(Map<UUID, TownMapDataResponsePacket.TownMapInfo> newData) {
        int oldSize = townData.size();
        townData.clear();
        townData.putAll(newData);
        lastUpdateTime = System.currentTimeMillis();
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
            "Town map cache updated: {} -> {} towns", oldSize, townData.size());
    }
    
    public Map<UUID, TownMapDataResponsePacket.TownMapInfo> getAllTowns() {
        return new HashMap<>(townData);
    }

    public TownMapDataResponsePacket.TownMapInfo getTown(UUID id) {
        return townData.get(id);
    }

    public boolean hasData() {
        return !townData.isEmpty();
    }

    public boolean isStale() {
        return System.currentTimeMillis() - lastUpdateTime > CACHE_EXPIRY_MS;
    }

    public void updateTownPlatformData(UUID townId, Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms) {
        platformData.put(townId, new HashMap<>(platforms));
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
            "Platform data updated for town {}: {} platforms", townId, platforms.size());
    }
    
    public Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> getTownPlatformData(UUID townId) {
        Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms = platformData.get(townId);
        return platforms != null ? new HashMap<>(platforms) : new HashMap<>();
    }

    public boolean hasTownPlatformData(UUID townId) {
        return platformData.containsKey(townId);
    }

    public void clearTownPlatformData(UUID townId) {
        platformData.remove(townId);
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
            "Platform data cleared for town {}", townId);
    }
    
    public void updateTownInfo(UUID townId, String name, int population, int touristCount) {
        TownMapDataResponsePacket.TownMapInfo existingTown = townData.get(townId);
        if (existingTown != null) {
            // Create updated town info with fresh data (no boundary - that's live in platform packets)
            TownMapDataResponsePacket.TownMapInfo updatedTown = new TownMapDataResponsePacket.TownMapInfo(
                townId, name, existingTown.position, population, touristCount
            );
            townData.put(townId, updatedTown);
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Town data updated for town {}: pop={}, tourists={}", townId, population, touristCount);
        }
    }
    
    public void clear() {
        townData.clear();
        platformData.clear();
        lastUpdateTime = 0;
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town map cache cleared");
    }
    
    public int size() {
        return townData.size();
    }
}
