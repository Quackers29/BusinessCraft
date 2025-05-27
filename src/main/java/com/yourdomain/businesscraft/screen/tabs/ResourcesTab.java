package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.StandardTabContent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resources tab implementation for the Town Interface.
 * Displays all town resources in a scrollable grid.
 * Now uses standardized tab content for consistency.
 */
public class ResourcesTab extends BaseTownTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesTab.class);
    private StandardTabContent contentComponent;
    
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
            LOGGER.debug("Resources Tab: Providing {} items to content component", resources.size());
            return resources;
        });
        
        // Add the content component to the panel
        panel.addChild(contentComponent);
    }
    
    @Override
    public void update() {
        // This tab doesn't need any periodic updates beyond the standard rendering
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Forward scroll events to the standardized content component
        if (contentComponent != null) {
            LOGGER.debug("ResourcesTab forwarding scroll: {}", delta);
            return contentComponent.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }
} 