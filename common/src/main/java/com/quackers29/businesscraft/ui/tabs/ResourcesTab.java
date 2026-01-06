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
import com.quackers29.businesscraft.economy.ResourceRegistry;
import net.minecraft.world.item.Item;

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
    private Map<Item, Integer> lastKnownResources = null;
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
            java.util.LinkedHashMap<Item, Integer> resources = new java.util.LinkedHashMap<>();

            // 1. Add Work Units (Process as a virtual resource)
            com.quackers29.businesscraft.menu.TownInterfaceMenu menu = parentScreen.getMenu();
            if (menu != null) {
                int wu = menu.getWorkUnits();
                int cap = menu.getWorkUnitCap();
                // Show if we have WU or capacity for it
                if (wu > 0 || cap > 0) {
                    // Use AIR as the placeholder for Work Units (Invisible)
                    // The name will be overridden to "Work Units"
                    resources.put(net.minecraft.world.item.Items.AIR, wu);
                }
            }

            // 2. Add standard resources
            // Get resources from the parent screen using the public getter
            Map<Item, Integer> cachedResources = parentScreen.getCachedResources();
            // Sort by amount desc or name? The cache might be unordered.
            // We'll just add them.
            resources.putAll(cachedResources);

            // 3. Merge in wanted resources (deficits)
            com.quackers29.businesscraft.block.entity.TownInterfaceEntity te = parentScreen.getMenu()
                    .getTownInterfaceEntity();
            if (te != null) {
                Map<Item, Integer> wanted = te.getClientSyncHelper().getClientWantedResources();
                wanted.forEach((item, amount) -> {
                    // Ensure wanted items are visible in the list even if stock is 0
                    if (!resources.containsKey(item)) {
                        resources.put(item, 0);
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
            Map<Item, Integer> resources = parentScreen.getCachedResources();
            Map<Item, String> tooltips = new java.util.HashMap<>();
            TownDataCacheManager cache = parentScreen.getCacheManager();

            if (cache != null) {
                java.util.Set<String> unlocked = cache.getCachedUnlockedNodes();
                Map<Item, Integer> wanted = java.util.Collections.emptyMap();

                com.quackers29.businesscraft.block.entity.TownInterfaceEntity te = parentScreen.getMenu()
                        .getTownInterfaceEntity();
                if (te != null) {
                    wanted = te.getClientSyncHelper().getClientWantedResources();
                }

                // Add tooltip for Work Units (AIR)
                com.quackers29.businesscraft.menu.TownInterfaceMenu menu = parentScreen.getMenu();
                if (menu != null && (menu.getWorkUnits() > 0 || menu.getWorkUnitCap() > 0)) {
                    StringBuilder sb = new StringBuilder();
                    int wu = menu.getWorkUnits();
                    int cap = menu.getWorkUnitCap();
                    sb.append("§eWork Units§r\n");
                    sb.append(String.format("Available: %d\n", wu));
                    sb.append(String.format("Capacity: %d\n", cap));

                    sb.append("§7Used for production tasks.\n");
                    sb.append("§7Regenerates over time.");

                    tooltips.put(net.minecraft.world.item.Items.AIR, sb.toString());
                }

                for (Item item : resources.keySet()) {
                    StringBuilder sb = new StringBuilder();

                    // Wanted status
                    if (wanted.containsKey(item)) {
                        int deficit = wanted.get(item);
                        sb.append(String.format("§cWANTED: Deficit %d", deficit)); // Negative value
                        sb.append("\n");
                    }

                    // Quantity (Line 1)
                    int quantity = resources.getOrDefault(item, 0);
                    sb.append(String.format("Quantity: %d", quantity));

                    // Add Escrow info
                    if (te != null) {
                        Map<Item, Integer> escrow = te.getClientSyncHelper().getClientEscrowedResources();
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

                    // Base stats
                    float[] stats = cache.getResourceStats(item);
                    if (stats != null && stats.length >= 3) {
                        sb.append(String.format("Production: +%.1f/m\nConsumption: -%.1f/m\nCapacity: %.0f",
                                stats[0], stats[1], stats[2]));
                        // Check for In-Transit (Index 3)
                        if (stats.length >= 4 && stats[3] > 0) {
                            sb.append(String.format("\nIn Transit: %.0f", stats[3]));
                        }
                    }

                    // Append relevant upgrade effects
                    net.minecraft.resources.ResourceLocation itemLoc = com.quackers29.businesscraft.api.PlatformAccess
                            .getRegistry().getItemKey(item);
                    String itemStr = itemLoc.toString();

                    if (unlocked != null) {
                        boolean headerAdded = false;

                        for (String nodeId : unlocked) {
                            com.quackers29.businesscraft.production.UpgradeNode node = com.quackers29.businesscraft.production.UpgradeRegistry
                                    .get(nodeId);
                            if (node == null)
                                continue;

                            int lvl = cache.getCachedUpgradeLevel(nodeId);

                            for (com.quackers29.businesscraft.data.parsers.Effect eff : node.getEffects()) {
                                boolean matches = false;
                                String type = "";

                                String target = eff.getTarget();

                                if (target.equals("storage_cap_all")) {
                                    matches = true;
                                    type = "Cap";
                                } else if (target.equals("storage_cap_" + itemStr)) {
                                    matches = true;
                                    type = "Cap";
                                } else {
                                    // Check recipe outputs
                                    com.quackers29.businesscraft.production.ProductionRecipe recipe = com.quackers29.businesscraft.production.ProductionRegistry
                                            .get(target);
                                    if (recipe != null) {
                                        for (com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount out : recipe
                                                .getOutputs()) {
                                            // Check direct match
                                            if (out.resourceId.equals(itemStr)) {
                                                matches = true;
                                                type = "Speed";
                                                break;
                                            }
                                            // Check resolved match (e.g. food -> minecraft:wheat)
                                            com.quackers29.businesscraft.economy.ResourceType resType = ResourceRegistry
                                                    .get(out.resourceId);
                                            if (resType != null) {
                                                String resolvedKey = resType.getCanonicalItemId().toString();
                                                if (resolvedKey.equals(itemStr)) {
                                                    matches = true;
                                                    type = "Speed";
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }

                                if (matches) {
                                    if (!headerAdded) {
                                        sb.append("\n\nActive Effects:");
                                        headerAdded = true;
                                    }
                                    float val = node.calculateEffectValue(eff, lvl);

                                    String valStr;
                                    if (type.equals("Speed")) {
                                        valStr = String.format("%+.0f%%", val * 100);
                                    } else {
                                        if (val == (long) val) {
                                            valStr = String.format("%+d", (long) val);
                                        } else {
                                            valStr = String.format("%+.1f", val);
                                        }
                                    }

                                    String name = node.getDisplayName();
                                    if (node.isRepeatable())
                                        name += " (" + lvl + ")";

                                    sb.append("\n ").append(name).append(": ").append(valStr).append(" ").append(type);
                                }
                            }
                        }
                    }
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
            Map<Item, Integer> currentResources = parentScreen.getCachedResources();
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
    private boolean hasResourcesChanged(Map<Item, Integer> currentResources) {
        if (lastKnownResources == null) {
            return !currentResources.isEmpty();
        }

        // Check if the maps are different
        if (lastKnownResources.size() != currentResources.size()) {
            return true;
        }

        // Check if any values have changed
        for (Map.Entry<Item, Integer> entry : currentResources.entrySet()) {
            Integer lastValue = lastKnownResources.get(entry.getKey());
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
    private void checkForResourceChanges(Map<Item, Integer> currentResources) {
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
}
