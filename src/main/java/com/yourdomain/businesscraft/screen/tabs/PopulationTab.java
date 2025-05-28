package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.StandardTabContent;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Population tab implementation for the Town Interface.
 * Displays citizens and their information with scrolling support.
 * Now uses standardized tab content for consistency.
 */
public class PopulationTab extends BaseTownTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopulationTab.class);
    
    // Sample citizen data (would be replaced with real data in production)
    private static final String[] names = {"John Smith", "Emma Johnson", "Alex Lee", "Sofia Garcia", 
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang"};
    private static final String[] jobs = {"Miner", "Farmer", "Builder", "Trader", 
                        "Blacksmith", "Scholar","Blacksmith", "Scholar","Blacksmith", "Scholar"};
    private static final int[] levels = {3, 2, 4, 1, 5, 2, 3, 4, 2, 5};
    
    private StandardTabContent contentComponent;
    
    /**
     * Creates a new Population tab.
     * 
     * @param parentScreen The parent screen
     * @param width The width of the tab panel
     * @param height The height of the tab panel
     */
    public PopulationTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }
    
    @Override
    public void init(Consumer<Button> registerWidget) {
        // Add title first (like other tabs)
        panel.addChild(createHeaderLabel("POPULATION"));
                
        // Create standardized content component
        contentComponent = createStandardContent(
            StandardTabContent.ContentType.CUSTOM_LIST, 
            "POPULATION"
        );
                            
        // Configure with population data supplier
        contentComponent.withCustomData(() -> new Object[]{names, jobs, levels});
        
        // Add to panel
        panel.addChild(contentComponent);
    }
    
    @Override
    public void update() {
        // This causes a refresh of the population data when called
        if (contentComponent != null) {
            contentComponent.refresh();
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Always forward scroll events to contentComponent
        if (contentComponent != null) {
            LOGGER.debug("PopulationTab forwarding scroll: {}", delta);
            return contentComponent.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }
} 