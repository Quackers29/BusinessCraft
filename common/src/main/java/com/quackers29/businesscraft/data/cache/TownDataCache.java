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

    private static final long DEFAULT_TTL = 5000; // 5 seconds

    private final Map<String, CacheEntry<?>> cache = new HashMap<>();

    private final ITownDataProvider dataProvider;
    public TownDataCache(ITownDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
    public String getTownName() {
        return getOrFetchValue("townName", dataProvider::getTownName, DEFAULT_TTL);
    }
    public int getPopulation() {
        return getOrFetchValue("population", () -> (int) dataProvider.getPopulation(), DEFAULT_TTL);
    }
    public int getTouristCount() {
        return getOrFetchValue("touristCount", () -> (int) dataProvider.getTouristCount(), DEFAULT_TTL);
    }
    public int getMaxTourists() {
        return getOrFetchValue("maxTourists", () -> (int) dataProvider.getMaxTourists(), DEFAULT_TTL);
    }
    public int getSearchRadius() {
        return getOrFetchValue("searchRadius", dataProvider::getSearchRadius, DEFAULT_TTL);
    }
    public int getWorkUnits() {
        return getOrFetchValue("workUnits", () -> (int) dataProvider.getWorkUnits(), DEFAULT_TTL);
    }
    public int getWorkUnitCap() {
        return getOrFetchValue("workUnitCap", () -> (int) dataProvider.getWorkUnitCap(), DEFAULT_TTL);
    }
    public Map<Item, Long> getAllResources() {
        return getOrFetchValue("allResources", dataProvider::getAllResources, DEFAULT_TTL);
    }
    public Map<Item, Long> getAllCommunalStorageItems() {
        return getOrFetchValue("communalStorage", dataProvider::getAllCommunalStorageItems, DEFAULT_TTL);
    }
    public List<ITownDataProvider.VisitHistoryRecord> getVisitHistory() {
        return getOrFetchValue("visitHistory", dataProvider::getVisitHistory, DEFAULT_TTL);
    }
    public Map<Item, Long> getPersonalStorageItems(UUID playerId) {
        return getOrFetchValue("personalStorage:" + playerId,
                () -> dataProvider.getPersonalStorageItems(playerId), DEFAULT_TTL);
    }
    public void invalidate(String key) {
        LOGGER.debug("Invalidating cache entry: {}", key);
        cache.remove(key);
    }
    public void invalidateAll() {
        LOGGER.debug("Invalidating all cache entries");
        cache.clear();
    }
    @SuppressWarnings("unchecked")
    private <T> T getOrFetchValue(String key, Supplier<T> fetcher, long ttl) {
        CacheEntry<?> rawEntry = cache.get(key);
        CacheEntry<T> entry = null;

        if (rawEntry == null || rawEntry.isExpired()) {
            T value = fetcher.get();
            entry = new CacheEntry<>(value, ttl);
            cache.put(key, entry);
        } else {
            entry = (CacheEntry<T>) rawEntry;
        }

        return entry.getValue();
    }

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

    @FunctionalInterface
    private interface Supplier<T> {
        T get();
    }
}
