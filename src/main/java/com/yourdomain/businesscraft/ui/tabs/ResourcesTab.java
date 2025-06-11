package com.yourdomain.businesscraft.ui.tabs;

import com.yourdomain.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.yourdomain.businesscraft.ui.layout.BCFlowLayout;
import com.yourdomain.businesscraft.ui.components.basic.BCLabel;
import com.yourdomain.businesscraft.ui.components.containers.StandardTabContent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

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
     * @param width The width of the tab panel
     * @param height The height of the tab panel
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
                // Get resources from the parent screen using the public getter
                Map<Item, Integer> resources = parentScreen.getCachedResources();
            DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB, "Resources Tab: Providing {} items to content component", resources.size());
                
            // Check for resource changes and trigger refresh if needed
            checkForResourceChanges(resources);
            
            return resources;
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
                DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB, "Resources changed detected, refreshing content component");
                
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
            DebugConfig.debug(LOGGER, DebugConfig.UI_RESOURCES_TAB, "Immediate resource change detected during data supply");
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