package com.quackers29.businesscraft.ui.tabs;

import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.components.basic.BCLabel;
import com.quackers29.businesscraft.ui.components.containers.StandardTabContent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.ui.managers.TownDataCacheManager;
// NOTE: ResourceRegistry import removed - UI now uses TownResourceViewModel (server-authoritative)
import com.quackers29.businesscraft.town.viewmodel.TownResourceViewModel;

/**
 * Resources tab implementation for the Town Interface.
 * Displays all town resources in a scrollable grid with auto-refresh.
 * Now uses standardized tab content for consistency and includes
 * automatic refresh when resources change.
 */
public class ResourcesTab extends BaseTownTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesTab.class);
    private StandardTabContent contentComponent;

    // Auto-refresh tracking
    private Map<Item, Long> lastKnownResources = null;
    private int refreshCounter = 0;
    private static final int REFRESH_CHECK_INTERVAL = 20; // Check every 20 ticks (1 second)

    /**
     * Creates a new Resources tab.
     * 
     * @param parentScreen The parent screen
     * @param width        The width of the tab panel
     * @param height       The height of the tab panel
     */
    public ResourcesTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);

        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }

    @Override
    public void init(Consumer<Button> registerWidget) {
        // Add title
        BCLabel titleLabel = createHeaderLabel("RESOURCES");
        panel.addChild(titleLabel);

        // Create standardized content component for item list display
        contentComponent = createStandardContent(StandardTabContent.ContentType.ITEM_LIST, "RESOURCES");

        // Configure the data supplier for the resource information
        contentComponent.withItemListData(() -> {
            // Use LinkedHashMap to preserve order (WU first)
            java.util.LinkedHashMap<Item, Long> resources = new java.util.LinkedHashMap<>();

            // Get the resource view-model from cache (server-authoritative)
            TownDataCacheManager cache = parentScreen.getCacheManager();
            TownResourceViewModel resourceViewModel = cache != null ? cache.getResourceViewModel() : null;

            // 1. Check if work units should be displayed (from view-model)
            if (resourceViewModel != null) {
                Map<Item, TownResourceViewModel.ResourceDisplayInfo> displayData = resourceViewModel.getResourceDisplayData();
                
                // Work units should be first if present (Items.AIR key)
                if (displayData.containsKey(net.minecraft.world.item.Items.AIR)) {
                    TownResourceViewModel.ResourceDisplayInfo wuInfo = displayData.get(net.minecraft.world.item.Items.AIR);
                    // Parse the current amount from the display string
                    long amount = parseAmount(wuInfo.getCurrentAmount());
                    resources.put(net.minecraft.world.item.Items.AIR, amount);
                }
            }

            // 2. Add standard resources from cached data
            Map<Item, Long> cachedResources = parentScreen.getCachedResources();
            resources.putAll(cachedResources);

            // 3. Merge in wanted resources (deficits)
            com.quackers29.businesscraft.block.entity.TownInterfaceEntity te = parentScreen.getMenu()
                    .getTownInterfaceEntity();
            if (te != null) {
                Map<Item, Long> wanted = te.getClientSyncHelper().getClientWantedResources();
                wanted.forEach((item, amount) -> {
                    // Ensure wanted items are visible in the list even if stock is 0
                    if (!resources.containsKey(item)) {
                        resources.put(item, 0L);
                    }
                    // Do not overwrite existing stock with deficit
                });
            }

            DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB,
                    "Resources Tab: Providing {} items to content component (includes WU)", resources.size());

            // Check for resource changes and trigger refresh if needed
            checkForResourceChanges(cachedResources);

            return resources;
        });

        // Configure name overrides
        contentComponent.withItemNameData(() -> {
            Map<Item, String> names = new java.util.HashMap<>();
            names.put(net.minecraft.world.item.Items.AIR, "Work Units");
            return names;
        });

        // Configure tooltips for resource information
        contentComponent.withItemTooltipData(() -> {
            Map<Item, Long> resources = parentScreen.getCachedResources();
            Map<Item, String> tooltips = new java.util.HashMap<>();
            TownDataCacheManager cache = parentScreen.getCacheManager();

            if (cache != null) {
                java.util.Set<String> unlocked = cache.getCachedUnlockedNodes();
                Map<Item, Long> wanted = java.util.Collections.emptyMap();

                com.quackers29.businesscraft.block.entity.TownInterfaceEntity te = parentScreen.getMenu()
                        .getTownInterfaceEntity();
                if (te != null) {
                    wanted = te.getClientSyncHelper().getClientWantedResources();
                }

                // Add tooltip for Work Units (AIR) from view-model
                TownResourceViewModel resourceViewModel = cache.getResourceViewModel();
                if (resourceViewModel != null) {
                    Map<Item, TownResourceViewModel.ResourceDisplayInfo> displayData = resourceViewModel.getResourceDisplayData();
                    if (displayData.containsKey(net.minecraft.world.item.Items.AIR)) {
                        TownResourceViewModel.ResourceDisplayInfo wuInfo = displayData.get(net.minecraft.world.item.Items.AIR);
                        StringBuilder sb = new StringBuilder();
                        sb.append("§e").append(wuInfo.getDisplayName()).append("§r\n");
                        sb.append("Available: ").append(wuInfo.getCurrentAmount()).append("\n");
                        sb.append("Capacity: ").append(wuInfo.getCapacity()).append("\n");
                        sb.append("Production: ").append(wuInfo.getProductionRate()).append("\n");
                        sb.append("Consumption: ").append(wuInfo.getConsumptionRate()).append("\n");
                        sb.append("Status: ").append(wuInfo.getStatusIndicator()).append("\n");
                        sb.append("§7Used for production tasks.");

                        tooltips.put(net.minecraft.world.item.Items.AIR, sb.toString());
                    }
                }

                for (Item item : resources.keySet()) {
                    StringBuilder sb = new StringBuilder();

                    // Wanted status
                    if (wanted.containsKey(item)) {
                        int deficit = wanted.get(item).intValue();
                        sb.append(String.format("§cWANTED: Deficit %d", deficit)); // Negative value
                        sb.append("\n");
                    }

                    // Quantity (Line 1)
                    long quantity = resources.getOrDefault(item, 0L);
                    sb.append(String.format("Quantity: %d", quantity));

                    // Add Escrow info
                    if (te != null) {
                        Map<Item, Long> escrow = te.getClientSyncHelper().getClientEscrowedResources();
                        if (escrow.containsKey(item) && escrow.get(item) > 0) {
                            sb.append(String.format(" (+%d Escrow)", escrow.get(item)));
                        }
                    }
                    sb.append("\n");

                    // GPI & Wealth (Line 2)
                    float gpi = com.quackers29.businesscraft.client.ClientGlobalMarket.get().getPrice(item);
                    float totalWealth = gpi * quantity;
                    sb.append(String.format("§6GPI: %.2f (~%.0f)", gpi, totalWealth));
                    sb.append("\n");

                    // PHASE 3.2: Use ResourceDisplayInfo from view-model for stats (server-authoritative)
                    // Replaces deprecated getResourceStats() method
                    TownResourceViewModel.ResourceDisplayInfo info = cache != null
                            && cache.getResourceViewModel() != null
                                    ? cache.getResourceViewModel().getResourceDisplay(item)
                                    : null;

                    // Base stats from view-model (pre-calculated by server)
                    if (info != null) {
                        sb.append("Production: ").append(info.getProductionRate()).append("\n");
                        sb.append("Consumption: ").append(info.getConsumptionRate()).append("\n");
                        sb.append("Capacity: ").append(info.getCapacity());

                        // Check for In-Transit
                        if (info.getInTransit() != null && !info.getInTransit().isEmpty()) {
                            sb.append("\n").append(info.getInTransit());
                        }
                    }

                    if (info != null && info.getActiveEffects() != null && !info.getActiveEffects().isEmpty()) {
                        sb.append("\n\nActive Effects:");
                        for (String effect : info.getActiveEffects()) {
                            sb.append("\n ").append(effect);
                        }
                    }

                    /*
                     * DEPRECATED: Client-side calculation removed to enforce Phase 1.2 View-Model
                     * architecture
                     * Logic moved to TownResourceViewModelBuilder (server-side)
                     */
                    tooltips.put(item, sb.toString());
                }
            }
            return tooltips;
        });

        // Add the content component to the panel
        panel.addChild(contentComponent);

        // Initialize last known resources
        lastKnownResources = parentScreen.getCachedResources();
    }

    @Override
    public void update() {
        // Increment refresh counter
        refreshCounter++;

        // Check for resource changes periodically
        if (refreshCounter >= REFRESH_CHECK_INTERVAL) {
            refreshCounter = 0;

            // Force a cache refresh from the parent screen
            if (parentScreen.getCacheManager() != null) {
                parentScreen.getCacheManager().refreshCachedValues();
            }

            // Check if resources have changed
            Map<Item, Long> currentResources = parentScreen.getCachedResources();
            if (hasResourcesChanged(currentResources)) {
                DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB,
                        "Resources changed detected, refreshing content component");

                // Force refresh the content component
                if (contentComponent != null) {
                    contentComponent.refresh();
                }

                // Update our tracking
                lastKnownResources = Map.copyOf(currentResources);
            }
        }
    }

    /**
     * Checks if resources have changed since last update
     */
    private boolean hasResourcesChanged(Map<Item, Long> currentResources) {
        if (lastKnownResources == null) {
            return !currentResources.isEmpty();
        }

        // Check if the maps are different
        if (lastKnownResources.size() != currentResources.size()) {
            return true;
        }

        // Check if any values have changed
        for (Map.Entry<Item, Long> entry : currentResources.entrySet()) {
            Long lastValue = lastKnownResources.get(entry.getKey());
            if (lastValue == null || !lastValue.equals(entry.getValue())) {
                return true;
            }
        }

        // Check if any items were removed
        for (Item item : lastKnownResources.keySet()) {
            if (!currentResources.containsKey(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Called during data supplier execution to check for immediate changes
     */
    private void checkForResourceChanges(Map<Item, Long> currentResources) {
        if (hasResourcesChanged(currentResources)) {
            // Update our tracking immediately
            lastKnownResources = Map.copyOf(currentResources);
            DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB,
                    "Immediate resource change detected during data supply");
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Forward scroll events to the standardized content component
        if (contentComponent != null) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB, "ResourcesTab forwarding scroll: {}", delta);
            return contentComponent.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }

    /**
     * Force refresh the resources display
     * Can be called externally when resource changes are detected
     */
    public void forceRefresh() {
        DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB, "Force refresh requested for ResourcesTab");

        // Force cache refresh
        if (parentScreen.getCacheManager() != null) {
            parentScreen.getCacheManager().refreshCachedValues();
        }

        // Force content component refresh
        if (contentComponent != null) {
            contentComponent.refresh();
        }

        // Update tracking
        lastKnownResources = parentScreen.getCachedResources();
        refreshCounter = 0; // Reset counter
    }

    /**
     * Parse integer amount from display string (e.g., "250", "1.5K" -> 1500)
     */
    private long parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return 0;
        }
        
        try {
            // Handle "K" suffix (thousands)
            if (amountStr.endsWith("K")) {
                String numPart = amountStr.substring(0, amountStr.length() - 1);
                float value = Float.parseFloat(numPart);
                return (long) (value * 1000);
            }
            
            // Handle plain integer
            return Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB,
                    "Failed to parse amount string: {}", amountStr);
            return 0;
        }
    }
}
