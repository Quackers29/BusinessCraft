package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.yourdomain.businesscraft.data.cache.TownDataCache;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages town data caching and provides consolidated access to cached data.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class TownDataCacheManager {
    private final TownDataCache dataCache;
    private final TownInterfaceMenu menu;
    
    // Cached values for UI updates
    private int cachedPopulation;
    private int cachedTourists;
    private int cachedMaxTourists;
    private int cachedSearchRadius;
    
    /**
     * Creates a new cache manager.
     * 
     * @param dataCache The town data cache (can be null)
     * @param menu The town interface menu for fallback data
     */
    public TownDataCacheManager(TownDataCache dataCache, TownInterfaceMenu menu) {
        this.dataCache = dataCache;
        this.menu = menu;
        
        // Initialize cached values
        refreshCachedValues();
    }
    
    /**
     * Refreshes all cached values from the cache or menu.
     */
    public void refreshCachedValues() {
        this.cachedPopulation = getCachedPopulation();
        this.cachedTourists = getCachedTouristCount();
        this.cachedMaxTourists = getCachedMaxTourists();
        this.cachedSearchRadius = getCachedSearchRadius();
    }
    
    /**
     * Gets the cached town name.
     * 
     * @return The town name
     */
    public String getCachedTownName() {
        if (dataCache != null) {
            return dataCache.getTownName();
        }
        return menu.getTownName();
    }
    
    /**
     * Gets the cached population count.
     * 
     * @return The population count
     */
    public int getCachedPopulation() {
        if (dataCache != null) {
            return dataCache.getPopulation();
        }
        return menu.getTownPopulation();
    }
    
    /**
     * Gets the cached tourist count.
     * 
     * @return The current tourist count
     */
    public int getCachedTouristCount() {
        if (dataCache != null) {
            return dataCache.getTouristCount();
        }
        return menu.getCurrentTourists();
    }
    
    /**
     * Gets the cached maximum tourists.
     * 
     * @return The maximum tourist count
     */
    public int getCachedMaxTourists() {
        if (dataCache != null) {
            return dataCache.getMaxTourists();
        }
        return menu.getMaxTourists();
    }
    
    /**
     * Gets the cached search radius.
     * 
     * @return The search radius
     */
    public int getCachedSearchRadius() {
        if (dataCache != null) {
            return dataCache.getSearchRadius();
        }
        return menu.getSearchRadius();
    }
    
    /**
     * Gets the cached resources map.
     * 
     * @return Map of items to quantities
     */
    public Map<Item, Integer> getCachedResources() {
        if (dataCache != null) {
            return dataCache.getAllResources();
        }
        return menu.getAllResources();
    }
    
    /**
     * Gets the cached visit history.
     * 
     * @return List of visit history records
     */
    public List<VisitHistoryRecord> getCachedVisitHistory() {
        if (dataCache != null) {
            return dataCache.getVisitHistory();
        }
        return Collections.emptyList(); // Fallback when cache is not available
    }
    
    /**
     * Gets a formatted tourist string for display.
     * 
     * @return Formatted string like "5/10"
     */
    public String getTouristString() {
        int current = menu.getCurrentTourists();
        int max = getCachedMaxTourists();
        if(current < 0) return "Loading...";
        return current + "/" + max;
    }
    
    /**
     * Invalidates the cache if available.
     */
    public void invalidateCache() {
        if (dataCache != null) {
            dataCache.invalidateAll();
        }
    }
    
    /**
     * Updates the cached search radius value.
     * 
     * @param newRadius The new search radius
     */
    public void updateCachedSearchRadius(int newRadius) {
        this.cachedSearchRadius = newRadius;
    }
    
    /**
     * Gets the locally cached population (for UI updates).
     * 
     * @return The locally cached population
     */
    public int getLocalCachedPopulation() {
        return cachedPopulation;
    }
    
    /**
     * Gets the locally cached tourist count (for UI updates).
     * 
     * @return The locally cached tourist count
     */
    public int getLocalCachedTourists() {
        return cachedTourists;
    }
    
    /**
     * Gets the locally cached max tourists (for UI updates).
     * 
     * @return The locally cached max tourists
     */
    public int getLocalCachedMaxTourists() {
        return cachedMaxTourists;
    }
    
    /**
     * Gets the locally cached search radius (for UI updates).
     * 
     * @return The locally cached search radius
     */
    public int getLocalCachedSearchRadius() {
        return cachedSearchRadius;
    }
} 