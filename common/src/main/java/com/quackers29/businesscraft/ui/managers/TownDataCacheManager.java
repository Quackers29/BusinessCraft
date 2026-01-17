package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.quackers29.businesscraft.data.cache.TownDataCache;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.town.viewmodel.TradingViewModel;
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

    // Static cache for trading view-model (Global sync)
    private static TradingViewModel globalTradingViewModel;

    /**
     * Updates the global trading view-model.
     * Called by TradingViewModelSyncPacket.
     */
    public static void updateTradingViewModel(TradingViewModel viewModel) {
        globalTradingViewModel = viewModel;
    }

    /**
     * Creates a new cache manager.
     * 
     * @param dataCache The town data cache (can be null)
     * @param menu      The town interface menu for fallback data
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
     * FIX: Now uses view-model instead of ContainerData to avoid client-side sync
     * issues.
     *
     * @return The population count
     */
    public int getCachedPopulation() {
        // NEW: Use resource view-model which syncs reliably
        var viewModel = getResourceViewModel();
        if (viewModel != null) {
            return viewModel.getPopulation();
        }

        // FALLBACK: Legacy data sources (should rarely be used)
        if (dataCache != null) {
            return dataCache.getPopulation();
        }
        return menu.getTownPopulation();
    }

    /**
     * Gets the cached tourist count.
     * FIX: Now uses view-model instead of ContainerData to avoid client-side sync
     * issues.
     *
     * @return The current tourist count
     */
    public int getCachedTouristCount() {
        // NEW: Use resource view-model which syncs reliably
        var viewModel = getResourceViewModel();
        if (viewModel != null) {
            return viewModel.getTouristCount();
        }

        // FALLBACK: Legacy data sources (should rarely be used)
        if (dataCache != null) {
            return dataCache.getTouristCount();
        }
        return menu.getCurrentTourists();
    }

    /**
     * Gets the cached maximum tourists.
     * FIX: Now uses view-model instead of ContainerData to avoid client-side sync
     * issues.
     *
     * @return The maximum tourist count
     */
    public int getCachedMaxTourists() {
        // NEW: Use resource view-model which syncs reliably
        var viewModel = getResourceViewModel();
        if (viewModel != null) {
            return viewModel.getMaxTourists();
        }

        // FALLBACK: Legacy data sources (should rarely be used)
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
     * FIX: Now uses view-model instead of ContainerData to avoid client-side sync
     * issues.
     *
     * @return Formatted string like "5/10"
     */
    public String getTouristString() {
        // NEW: Use resource view-model which syncs reliably
        var viewModel = getResourceViewModel();
        if (viewModel != null) {
            return viewModel.getTouristString();
        }

        // FALLBACK: Legacy approach (should rarely be used)
        int current = menu.getCurrentTourists();
        int max = getCachedMaxTourists();
        if (current < 0)
            return "Loading...";
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

    // --- Overview Data ---
    // --- Overview Data (Delegated to TownInterfaceViewModel) ---
    // These fields are deprecated in favor of cachedInterfaceViewModel in
    // TownInterfaceEntity

    // Legacy update method kept briefly for compatibility but empty
    public void updateOverviewData(float happiness, String biome, String biomeVariant, String currentResearch,
            float researchProgress,
            int dailyTickInterval, Map<String, Float> activeProductions, Map<String, Integer> upgradeLevels,
            float populationCap, int totalTouristsArrived, double totalTouristDistance, float borderRadius,
            Map<String, Float> aiScores) {
        // No-op: Data now handled by TownInterfaceViewModel and other ViewModels
    }

    public float getCachedBorderRadius() {
        var vm = getInterfaceViewModel();
        return (vm != null) ? vm.getBorderRadius() : 50f;
    }

    public float getCachedAiScore(String nodeId) {
        var vm = getUpgradeViewModel();
        if (vm != null) {
            var info = vm.getUpgradeInfo(nodeId);
            return (info != null) ? info.getAiScore() : 0f;
        }
        return 0f;
    }

    /**
     * DEPRECATED: Use getUpgradeViewModel().getResearchSpeedMultiplier() instead.
     * 
     * This method violated server-authoritative architecture by accessing
     * UpgradeRegistry
     * and performing business logic calculations on the client.
     * 
     * @deprecated Replaced by server-calculated research speed in
     *             UpgradeStatusViewModel
     */
    @Deprecated
    public float getCachedResearchSpeed() {
        // NEW: Use server-authoritative view-model instead of client-side calculations
        var upgradeViewModel = getUpgradeViewModel();
        if (upgradeViewModel != null) {
            return upgradeViewModel.getResearchSpeedMultiplier();
        }
        return 1.0f;
    }

    /**
     * DEPRECATED: Use getUpgradeViewModel().getResearchSpeedTooltip() instead.
     * 
     * This method violated server-authoritative architecture by accessing
     * UpgradeRegistry
     * and performing business logic calculations on the client.
     * 
     * @deprecated Replaced by server-calculated research speed tooltip in
     *             UpgradeStatusViewModel
     */
    @Deprecated
    public String getResearchSpeedTooltip() {
        // NEW: Use server-authoritative view-model instead of client-side calculations
        var upgradeViewModel = getUpgradeViewModel();
        if (upgradeViewModel != null) {
            return upgradeViewModel.getResearchSpeedTooltip();
        }
        return "Loading...";
    }

    public int getCachedTotalTouristsArrived() {
        var vm = getInterfaceViewModel();
        return (vm != null) ? vm.getTotalTouristsArrived() : 0;
    }

    public double getCachedTotalTouristDistance() {
        var vm = getInterfaceViewModel();
        return (vm != null) ? vm.getTotalTouristDistance() : 0.0;
    }

    /**
     * Replaced by TownInterfaceViewModel.getHappinessDisplay() /
     * getHappinessStatus()
     * Returning raw float for compatibility where needed, parsed from display if
     * possible or 50f
     */
    public float getCachedHappiness() {
        var vm = getInterfaceViewModel();
        if (vm != null) {
            try {
                String disp = vm.getHappinessDisplay().replace("%", "").trim();
                return Float.parseFloat(disp);
            } catch (Exception e) {
            }
        }
        return 50f;
    }

    public String getCachedBiome() {
        var vm = getInterfaceViewModel();
        return (vm != null) ? vm.getBiomeFormatted() : "Unknown";
    }

    public String getCachedBiomeVariant() {
        var vm = getInterfaceViewModel();
        return (vm != null) ? vm.getBiomeVariantFormatted() : "Unknown";
    }

    public String getCachedCurrentResearch() {
        var vm = getUpgradeViewModel();
        if (vm != null) {
            // Find current research
            for (String uid : vm.getResearchableUpgradeIds()) {
                var info = vm.getUpgradeInfo(uid);
                if (info != null && info.isCurrentResearch())
                    return uid;
            }
        }
        return "";
    }

    public float getCachedResearchProgress() {
        var vm = getUpgradeViewModel();
        if (vm != null) {
            for (String uid : vm.getResearchableUpgradeIds()) {
                var info = vm.getUpgradeInfo(uid);
                if (info != null && info.isCurrentResearch())
                    return info.getProgressPercentage() * 100f; // Approx
            }
        }
        return 0f;
    }

    public int getCachedDailyTickInterval() {
        // This was config based, assumes standard
        return 24000;
    }

    public Map<String, Float> getCachedActiveProductions() {
        var vm = getProductionViewModel();
        // Convert ProductionViewModel to old map format for compatibility if needed?
        // Actually the UI (ProductionTab) has been updated to use ViewModel directly.
        // This method might be unused now or should be deprecated.
        return Collections.emptyMap();
    }

    public java.util.Set<String> getCachedUnlockedNodes() {
        var vm = getUpgradeViewModel();
        if (vm != null) {
            return new java.util.HashSet<>(vm.getUnlockedUpgradeIds());
        }
        return Collections.emptySet();
    }

    public int getCachedUpgradeLevel(String nodeId) {
        var vm = getUpgradeViewModel();
        if (vm != null) {
            var info = vm.getUpgradeInfo(nodeId);
            return (info != null) ? info.getCurrentLevel() : 0;
        }
        return 0;
    }

    public float getCachedPopulationCap() {
        var vm = getInterfaceViewModel();
        if (vm != null) {
            try {
                String disp = vm.getPopulationDisplay(); // "5/10"
                if (disp.contains("/")) {
                    return Float.parseFloat(disp.split("/")[1].trim());
                }
            } catch (Exception e) {
            }
        }
        return 10f; // Default cap
    }

    // ============================================================================
    // REMOVED IN PHASE 3.2: getResourceStats(Item)
    // ============================================================================
    // This deprecated method was removed as part of Phase 3.2 cleanup.
    // It accessed ClientSyncHelper.getClientResourceStats() which has been removed.
    //
    // REPLACEMENT: Use getResourceDisplayInfo(Item) which accesses the
    // server-authoritative TownResourceViewModel instead.
    // ============================================================================

    /**
     * NEW: Gets resource display information from the server-authoritative
     * view-model.
     * This contains pre-calculated display strings, eliminating client-side
     * calculations.
     * 
     * @param item The item to get display info for
     * @return The display info containing formatted strings and status, or null if
     *         not available
     */
    public com.quackers29.businesscraft.town.viewmodel.TownResourceViewModel.ResourceDisplayInfo getResourceDisplayInfo(
            Item item) {
        if (menu != null) {
            net.minecraft.world.level.block.entity.BlockEntity be = menu.getBlockEntity();
            if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity entity) {
                com.quackers29.businesscraft.town.viewmodel.TownResourceViewModel viewModel = entity
                        .getCachedResourceViewModel();
                if (viewModel != null) {
                    return viewModel.getResourceDisplay(item);
                }
            }
        }
        return null;
    }

    /**
     * NEW: Gets the complete resource view-model containing all display data.
     * This provides access to overall town status and economic trends.
     * 
     * @return The complete view-model, or null if not available
     */
    public com.quackers29.businesscraft.town.viewmodel.TownResourceViewModel getResourceViewModel() {
        if (menu != null) {
            net.minecraft.world.level.block.entity.BlockEntity be = menu.getBlockEntity();
            if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity entity) {
                return entity.getCachedResourceViewModel();
            }
        }
        return null;
    }

    /**
     * NEW: Gets the complete production view-model containing all production recipe
     * display data.
     * This eliminates client-side ProductionRegistry access and config file
     * reading.
     *
     * @return The complete production view-model, or null if not available
     */
    public com.quackers29.businesscraft.town.viewmodel.ProductionStatusViewModel getProductionViewModel() {
        if (menu != null) {
            net.minecraft.world.level.block.entity.BlockEntity be = menu.getBlockEntity();
            if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity entity) {
                return entity.getCachedProductionViewModel();
            }
        }
        return null;
    }

    /**
     * NEW: Gets production recipe display info by recipe ID from the
     * server-authoritative view-model.
     * This replaces ProductionRegistry.get() calls that violated the
     * server-authoritative pattern.
     *
     * @param recipeId The recipe ID to get display info for
     * @return The display info containing recipe name and details, or null if not
     *         available
     */
    public com.quackers29.businesscraft.town.viewmodel.ProductionStatusViewModel.ProductionRecipeInfo getProductionRecipeInfo(
            String recipeId) {
        var viewModel = getProductionViewModel();
        if (viewModel != null) {
            return viewModel.getRecipeInfo(recipeId);
        }
        return null;
    }

    /**
     * NEW: Gets the complete upgrade view-model containing all upgrade display
     * data.
     * This eliminates client-side UpgradeRegistry access and config file reading.
     *
     * REPLACES:
     * - UpgradeRegistry.get() calls in ProductionTab (lines 159, 174)
     * - UpgradeRegistry.getAll() calls in ProductionTab (line 174)
     * - Client-side research speed calculations (lines 155-172 in ProductionTab)
     * - Client-side cost multiplier calculations (lines 253-310 in ProductionTab)
     * - getCachedResearchSpeed() calculations (above)
     * - getResearchSpeedTooltip() calculations (above)
     *
     * @return The complete upgrade view-model, or null if not available
     */
    public com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel getUpgradeViewModel() {
        if (menu != null) {
            net.minecraft.world.level.block.entity.BlockEntity be = menu.getBlockEntity();
            if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity entity) {
                return entity.getCachedUpgradeViewModel();
            }
        }
        return null;
    }

    /**
     * NEW: Gets upgrade display info by node ID from the server-authoritative
     * view-model.
     * This replaces UpgradeRegistry.get() calls that violated the
     * server-authoritative pattern.
     *
     * @param nodeId The upgrade node ID to get display info for
     * @return The display info containing upgrade name, costs, effects, etc., or
     *         null if not available
     */
    public com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel.UpgradeDisplayInfo getUpgradeInfo(
            String nodeId) {
        var viewModel = getUpgradeViewModel();
        if (viewModel != null) {
            return viewModel.getUpgradeInfo(nodeId);
        }
        return null;
    }

    /**
     * NEW: Gets the town interface view-model containing all main overview display
     * data.
     * Replaces client-side fallbacks in TownInterfaceMenu.
     *
     * @return The view-model, or null if not available
     */
    public com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel getInterfaceViewModel() {
        if (menu != null) {
            net.minecraft.world.level.block.entity.BlockEntity be = menu.getBlockEntity();
            if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity entity) {
                return entity.getCachedInterfaceViewModel();
            }
        }
        return null;
    }

    public Map<String, String> getCachedTownStats() {
        if (dataCache != null) {
            // dataCache might not have a generic stats map, but we can construct it or
            // check if it has one.
            // Assuming dataCache has access to basic stats.
            // If not, we use the cached fields in this manager.
            // Map keys expected by ClientTownState: "Population"
            java.util.LinkedHashMap<String, String> stats = new java.util.LinkedHashMap<>();
            stats.put("Population", getCachedPopulation() + " (" + (int) getCachedPopulationCap() + ")");
            return stats;
        }
        return Collections.emptyMap();
    }

    public Map<String, String> getTourismStats() {
        java.util.LinkedHashMap<String, String> stats = new java.util.LinkedHashMap<>();
        stats.put("Current Tourists", String.valueOf(getCachedTouristCount()));
        return stats;
    }

    /**
     * NEW: Gets the complete trading view-model.
     * Replaces manual stock calculations in TradeModalManager.
     *
     * @return The trading view-model, or null if not available
     */
    public static TradingViewModel getTradingViewModel() {
        // Return globally synced view-model
        return globalTradingViewModel;
    }

}
