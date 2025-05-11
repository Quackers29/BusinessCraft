package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.components.BCComponent;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import com.yourdomain.businesscraft.screen.components.UIGridBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Settings tab implementation for the Town Interface.
 * Provides configuration options for the town.
 */
public class SettingsTab extends BaseTownTab {
    private static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    private static final int TEXT_HIGHLIGHT = 0xFFDDFFFF;      // Light cyan highlight text
    private static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    private static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    
    private BCComponent gridHost;
    
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
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("SETTINGS", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Calculate dimensions for the settings grid
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Create a custom component to host the settings grid
        gridHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance
            private UIGridBuilder grid;
            
            // Initialize the grid when rendering
            private UIGridBuilder createGrid() {
                // Use the new utility method for label-button pairs
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
                
                return UIGridBuilder.createLabelButtonGrid(
                    x, y, getWidth(), getHeight(),
                    TEXT_COLOR, PRIMARY_COLOR,
                    settingsData)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .drawBorder(true);
            }
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Create/update the grid
                grid = createGrid();
                
                // Render the grid
                grid.render(guiGraphics, mouseX, mouseY);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // If this is a right-click, handle the radius change manually
                if (button == 1) {
                    // Approximate if the click is in the button area
                    // This is a simplified approach that assumes the radius button is at a specific position
                    parentScreen.handleRadiusChange(1); // Handle right click
                    return true;
                }
                
                // For all other cases, delegate to the grid
                if (grid != null) {
                    return grid.mouseClicked((int)mouseX, (int)mouseY, button);
                }
                return false;
            }
        };
        
        // Add the grid host to the panel
        panel.addChild(gridHost);
    }
    
    @Override
    public void update() {
        // Force a refresh of the grid to update displayed values
        if (gridHost != null) {
            gridHost.setVisible(false);
            gridHost.setVisible(true);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Settings tab doesn't need scrolling, but we'll forward the event anyway
        return gridHost != null && gridHost.mouseScrolled(mouseX, mouseY, delta);
    }
} 