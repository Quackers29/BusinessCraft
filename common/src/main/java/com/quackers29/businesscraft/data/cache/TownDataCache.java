package com.quackers29.businesscraft.data.cache;

import com.quackers29.businesscraft.api.ITownDataProvider;
import net.minecraft.world.item.Item;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side cache for town data to reduce network requests
 * and provide faster UI updates.
 */
public class TownDataCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownDataCache.class);

    // Cache entry TTL in milliseconds
    private static final long DEFAULT_TTL = 5000; // 5 seconds

    // Map of cache key to CacheEntry
    private final Map<String, CacheEntry<?>> cache = new HashMap<>();

    // Backing data provider
    private final ITownDataProvider dataProvider;

    /**
     * Create a new cache for the given data provider
     */
    public TownDataCache(ITownDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    /**
     * Get the town name, either from cache or from the provider
     */
    public String getTownName() {
        return getOrFetchValue("townName", dataProvider::getTownName, DEFAULT_TTL);
    }

    /**
     * Get the town population, either from cache or from the provider
     */
    public int getPopulation() {
        return getOrFetchValue("population", () -> (int) dataProvider.getPopulation(), DEFAULT_TTL);
    }

    /**
     * Get the tourist count, either from cache or from the provider
     */
    public int getTouristCount() {
        return getOrFetchValue("touristCount", () -> (int) dataProvider.getTouristCount(), DEFAULT_TTL);
    }

    /**
     * Get the max tourists, either from cache or from the provider
     */
    public int getMaxTourists() {
        return getOrFetchValue("maxTourists", () -> (int) dataProvider.getMaxTourists(), DEFAULT_TTL);
    }

    /**
     * Get the search radius, either from cache or from the provider
     */
    public int getSearchRadius() {
        return getOrFetchValue("searchRadius", dataProvider::getSearchRadius, DEFAULT_TTL);
    }

    /**
     * Get work units, either from cache or from the provider
     */
    public int getWorkUnits() {
        return getOrFetchValue("workUnits", () -> (int) dataProvider.getWorkUnits(), DEFAULT_TTL);
    }

    /**
     * Get work unit cap, either from cache or from the provider
     */
    public int getWorkUnitCap() {
        return getOrFetchValue("workUnitCap", () -> (int) dataProvider.getWorkUnitCap(), DEFAULT_TTL);
    }

    /**
     * Get all resources, either from cache or from the provider
     */
    public Map<Item, Long> getAllResources() {
        return getOrFetchValue("allResources", dataProvider::getAllResources, DEFAULT_TTL);
    }

    /**
     * Get all communal storage items, either from cache or from the provider
     */
    public Map<Item, Long> getAllCommunalStorageItems() {
        return getOrFetchValue("communalStorage", dataProvider::getAllCommunalStorageItems, DEFAULT_TTL);
    }

    /**
     * Get visit history, either from cache or from the provider
     */
    public List<ITownDataProvider.VisitHistoryRecord> getVisitHistory() {
        return getOrFetchValue("visitHistory", dataProvider::getVisitHistory, DEFAULT_TTL);
    }

    /**
     * Get the personal storage items for a player, either from cache or from the
     * provider
     */
    public Map<Item, Long> getPersonalStorageItems(UUID playerId) {
        return getOrFetchValue("personalStorage:" + playerId,
                () -> dataProvider.getPersonalStorageItems(playerId), DEFAULT_TTL);
    }

    /**
     * Force refresh a specific cache entry
     */
    public void invalidate(String key) {
        LOGGER.debug("Invalidating cache entry: {}", key);
        cache.remove(key);
    }

    /**
     * Force refresh all cache entries
     */
    public void invalidateAll() {
        LOGGER.debug("Invalidating all cache entries");
        cache.clear();
    }

    /**
     * Get a value from cache or fetch it if not present or expired
     */
    @SuppressWarnings("unchecked")
    private <T> T getOrFetchValue(String key, Supplier<T> fetcher, long ttl) {
        CacheEntry<?> rawEntry = cache.get(key);
        CacheEntry<T> entry = null;

        // If entry doesn't exist or is expired, fetch a new value
        if (rawEntry == null || rawEntry.isExpired()) {
            T value = fetcher.get();
            entry = new CacheEntry<>(value, ttl);
            cache.put(key, entry);
        } else {
            // Safe cast since we know the entry was created with the correct type
            entry = (CacheEntry<T>) rawEntry;
        }

        return entry.getValue();
    }

    /**
     * Cache entry with expiration time
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long expirationTime;

        public CacheEntry(T value, long ttl) {
            this.value = value;
            this.expirationTime = Instant.now().toEpochMilli() + ttl;
        }

        public boolean isExpired() {
            return Instant.now().toEpochMilli() > expirationTime;
        }

        public T getValue() {
            return value;
        }
    }

    /**
     * Supplier interface for cache values
     */
    @FunctionalInterface
    private interface Supplier<T> {
        T get();
    }
}
