package com.quackers29.businesscraft.client.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for town map data with TTL-based invalidation.
 * 
 * This cache was temporarily removed during Enhanced MultiLoader Template migration
 * and is now restored in the common module for cross-platform compatibility.
 * 
 * Features:
 * - 30-second TTL (Time To Live) for cache entries
 * - Thread-safe concurrent access
 * - Singleton pattern for global access
 * - Cache invalidation support for real-time updates
 * - getAllTowns() method for map modal integration
 */
public class ClientTownMapCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTownMapCache.class);
    
    // Cache configuration
    private static final long CACHE_TTL_MS = 30_000; // 30 seconds as documented in done.md
    private static final long CLEANUP_INTERVAL_MS = 10_000; // 10 seconds cleanup cycle
    
    // Singleton instance
    private static volatile ClientTownMapCache instance;
    private static final Object LOCK = new Object();
    
    // Cache data structures
    private final Map<UUID, CachedTownData> townCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private long lastCleanupTime = System.currentTimeMillis();
    
    /**
     * Town data structure for caching.
     */
    public static class CachedTownData {
        private final UUID id;
        private final String name;
        private final int x, y, z;
        private final Map<String, Object> additionalData;
        private final long cacheTime;
        
        public CachedTownData(UUID id, String name, int x, int y, int z, Map<String, Object> additionalData) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.additionalData = new HashMap<>(additionalData != null ? additionalData : Collections.emptyMap());
            this.cacheTime = System.currentTimeMillis();
        }
        
        // Getters
        public UUID getId() { return id; }
        public String getName() { return name; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public Map<String, Object> getAdditionalData() { return new HashMap<>(additionalData); }
        public long getCacheTime() { return cacheTime; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - cacheTime > CACHE_TTL_MS;
        }
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private ClientTownMapCache() {
        LOGGER.debug("ClientTownMapCache initialized with {}ms TTL", CACHE_TTL_MS);
    }
    
    /**
     * Get singleton instance of ClientTownMapCache.
     * Thread-safe double-checked locking pattern.
     */
    public static ClientTownMapCache getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ClientTownMapCache();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get all cached towns for map display.
     * This is the primary method used by TownMapModal.
     * 
     * @return Map of town UUIDs to CachedTownData
     */
    public Map<UUID, CachedTownData> getAllTowns() {
        cleanupExpiredEntries();
        
        // Return a snapshot copy to prevent concurrent modification
        Map<UUID, CachedTownData> result = new HashMap<>();
        for (Map.Entry<UUID, CachedTownData> entry : townCache.entrySet()) {
            if (!entry.getValue().isExpired()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        
        LOGGER.debug("getAllTowns() returning {} cached towns", result.size());
        return result;
    }
    
    /**
     * Cache town data from server response.
     * 
     * @param townId Town UUID
     * @param name Town name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param additionalData Additional map data (platforms, boundaries, etc.)
     */
    public void cacheTownData(UUID townId, String name, int x, int y, int z, Map<String, Object> additionalData) {
        CachedTownData townData = new CachedTownData(townId, name, x, y, z, additionalData);
        townCache.put(townId, townData);
        cacheTimestamps.put("town_" + townId.toString(), System.currentTimeMillis());
        
        LOGGER.debug("Cached town data for '{}' at ({}, {}, {}) with {} additional properties", 
                    name, x, y, z, additionalData != null ? additionalData.size() : 0);
    }
    
    /**
     * Invalidate cache for a specific town.
     * Used when town data changes (renaming, etc.) as documented in done.md.
     * 
     * @param townId Town UUID to invalidate
     */
    public void invalidateTown(UUID townId) {
        townCache.remove(townId);
        cacheTimestamps.remove("town_" + townId.toString());
        LOGGER.debug("Invalidated cache for town: {}", townId);
    }
    
    /**
     * Invalidate all cached data.
     * Used for comprehensive cache invalidation as documented in done.md.
     */
    public void invalidateAll() {
        int removedCount = townCache.size();
        townCache.clear();
        cacheTimestamps.clear();
        LOGGER.debug("Invalidated all cached town data ({} entries removed)", removedCount);
    }
    
    /**
     * Check if town data is cached and not expired.
     * 
     * @param townId Town UUID
     * @return true if cached and valid
     */
    public boolean isCached(UUID townId) {
        CachedTownData data = townCache.get(townId);
        return data != null && !data.isExpired();
    }
    
    /**
     * Get specific town data from cache.
     * 
     * @param townId Town UUID
     * @return CachedTownData or null if not cached/expired
     */
    public CachedTownData getTownData(UUID townId) {
        CachedTownData data = townCache.get(townId);
        if (data != null && data.isExpired()) {
            townCache.remove(townId);
            cacheTimestamps.remove("town_" + townId.toString());
            return null;
        }
        return data;
    }
    
    /**
     * Update town name in cache (used for town renaming).
     * 
     * @param townId Town UUID
     * @param newName New town name
     */
    public void updateTownName(UUID townId, String newName) {
        CachedTownData oldData = townCache.get(townId);
        if (oldData != null) {
            CachedTownData updatedData = new CachedTownData(
                townId, newName, oldData.x, oldData.y, oldData.z, oldData.additionalData);
            townCache.put(townId, updatedData);
            cacheTimestamps.put("town_" + townId.toString(), System.currentTimeMillis());
            LOGGER.debug("Updated cached name for town {} to '{}'", townId, newName);
        }
    }
    
    /**
     * Cleanup expired entries (called automatically).
     */
    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return; // Skip cleanup if too soon
        }
        
        List<UUID> expiredTowns = new ArrayList<>();
        for (Map.Entry<UUID, CachedTownData> entry : townCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredTowns.add(entry.getKey());
            }
        }
        
        for (UUID expiredTown : expiredTowns) {
            townCache.remove(expiredTown);
            cacheTimestamps.remove("town_" + expiredTown.toString());
        }
        
        if (!expiredTowns.isEmpty()) {
            LOGGER.debug("Cleaned up {} expired town cache entries", expiredTowns.size());
        }
        
        lastCleanupTime = currentTime;
    }
    
    /**
     * Get cache statistics for debugging.
     */
    public Map<String, Object> getCacheStats() {
        cleanupExpiredEntries();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", townCache.size());
        stats.put("cacheHits", 0); // Could be implemented with counters if needed
        stats.put("ttlMs", CACHE_TTL_MS);
        stats.put("lastCleanup", lastCleanupTime);
        return stats;
    }
}