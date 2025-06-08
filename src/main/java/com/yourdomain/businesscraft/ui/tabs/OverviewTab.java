package com.yourdomain.businesscraft.ui.tabs;

import com.yourdomain.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.yourdomain.businesscraft.ui.templates.TownInterfaceTheme;
import com.yourdomain.businesscraft.ui.components.basic.BCComponent;
import com.yourdomain.businesscraft.ui.layout.BCFlowLayout;
import com.yourdomain.businesscraft.ui.components.basic.BCLabel;
import com.yourdomain.businesscraft.ui.builders.BCComponentFactory;
import com.yourdomain.businesscraft.ui.builders.UIGridBuilder;
import com.yourdomain.businesscraft.ui.components.containers.StandardTabContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Overview tab implementation for the Town Interface.
 * Displays general town information with an animated presentation.
 * Now uses standardized tab content for consistency.
 */
public class OverviewTab extends BaseTownTab {
    private StandardTabContent contentComponent;
    
    /**
     * Creates a new Overview tab.
     * 
     * @param parentScreen The parent screen
     * @param width The width of the tab panel
     * @param height The height of the tab panel
     */
    public OverviewTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }
    
    @Override
    public void init(Consumer<Button> registerWidget) {
        // Add title
        BCLabel titleLabel = createHeaderLabel("TOWN OVERVIEW");
        panel.addChild(titleLabel);
        
        // Create standardized content component for label-value display
        contentComponent = createStandardContent(StandardTabContent.ContentType.LABEL_VALUE_GRID, "TOWN OVERVIEW");
        
        // Configure the data supplier for the overview information
        contentComponent.withLabelValueData(() -> {
                Map<String, String> overviewData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
                overviewData.put("Town Name:", parentScreen.getCachedTownName());
                overviewData.put("Population:", String.valueOf(parentScreen.getCachedPopulation()));
                overviewData.put("Tourists:", parentScreen.getTouristString());
            return overviewData;
        });
        
        // Add the content component to the panel
        panel.addChild(contentComponent);
    }
    
    @Override
    public void update() {
        // This causes a refresh of the overview data when called
        if (contentComponent != null) {
            contentComponent.refresh();
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // No scrolling needed for overview tab, but forward events anyway
        return contentComponent != null && contentComponent.mouseScrolled(mouseX, mouseY, delta);
    }
} 