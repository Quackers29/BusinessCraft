package com.quackers29.businesscraft.ui.tabs;

import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.components.containers.StandardTabContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Settings tab implementation for the Town Interface.
 * Provides configuration options for the town.
 * Now uses standardized tab content for consistency.
 */
public class SettingsTab extends BaseTownTab {
    private StandardTabContent contentComponent;
    
    /**
     * Creates a new Settings tab.
     * 
     * @param parentScreen The parent screen
     * @param width The width of the tab panel
     * @param height The height of the tab panel
     */
    public SettingsTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }
    
    @Override
    public void init(Consumer<Button> registerWidget) {
        // Add title first (like other tabs)
        panel.addChild(createHeaderLabel("SETTINGS"));
        
        // Create standardized content component
        contentComponent = createStandardContent(
            StandardTabContent.ContentType.BUTTON_GRID, 
            "SETTINGS"
        );
        
        // Configure with settings data supplier
        contentComponent.withButtonGridData(() -> {
                Map<String, Object[]> settingsData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
                
                // Add platforms row
                settingsData.put("Platforms:", new Object[] {
                    "Set Platforms", 
                    (Consumer<Void>) button -> {
                        parentScreen.playButtonClickSound();
                        // Open the platform management screen instead of just showing a message
                        parentScreen.openPlatformManagementScreen();
                    }
                });
                
                // Add search radius row - use the cached value for display
                settingsData.put("Search Radius:", new Object[] {
                    "Radius: " + parentScreen.getCurrentSearchRadius(), 
                    (Consumer<Void>) button -> {
                        // In TownBlockScreen, this would increase the radius
                        parentScreen.handleRadiusChange(0); // 0 = left click (increase)
                    }
                });
                
            return settingsData;
        });
        
        // Configure custom click handler for right-click on radius button
        contentComponent.withCustomClickHandler(button -> {
            if (button == 1) { // Right-click
                    parentScreen.handleRadiusChange(1); // Handle right click
                    return true;
                }
            return false; // Let normal handling proceed
        });
        
        // Add to panel
        panel.addChild(contentComponent);
    }
    
    @Override
    public void update() {
        // Force a refresh of the content to update displayed values (like radius)
        if (contentComponent != null) {
            contentComponent.refresh();
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Settings tab doesn't need scrolling, but we'll forward the event anyway
        return contentComponent != null && contentComponent.mouseScrolled(mouseX, mouseY, delta);
    }
} 
