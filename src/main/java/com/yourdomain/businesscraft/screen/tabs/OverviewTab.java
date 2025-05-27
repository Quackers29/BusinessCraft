package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.TownInterfaceTheme;
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
 * Overview tab implementation for the Town Interface.
 * Displays general town information with an animated presentation.
 */
public class OverviewTab extends BaseTownTab {
    private static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    private static final int TEXT_HIGHLIGHT = TownInterfaceTheme.TEXT_HIGHLIGHT;
    // Use centralized theme constants
    private static final int BACKGROUND_COLOR = TownInterfaceTheme.BACKGROUND_COLOR;
    private static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;
    
    private BCComponent gridHost;
    
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
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("TOWN OVERVIEW", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Calculate dimensions for the town info grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Add animated town info component with appropriate dimensions
        gridHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance
            private UIGridBuilder grid;
            private float alpha = 0.0f;
            private long startTime;
            private boolean animationStarted = false;
            
            // Initialize the grid when rendering
            private UIGridBuilder createGrid() {
                // Use the new utility method for label-value pairs
                Map<String, String> overviewData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
                overviewData.put("Town Name:", parentScreen.getCachedTownName());
                overviewData.put("Population:", String.valueOf(parentScreen.getCachedPopulation()));
                overviewData.put("Tourists:", parentScreen.getTouristString());
                
                return UIGridBuilder.createLabelValueGrid(
                    x, y, getWidth(), getHeight(),
                    TEXT_COLOR, TEXT_HIGHLIGHT,
                    overviewData)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .drawBorder(true);
            }
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Start animation if not started
                if (!animationStarted) {
                    startTime = System.currentTimeMillis();
                    animationStarted = true;
                }
                
                // Calculate alpha based on time (fade in over 500ms, starting after title appears)
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > 300) { // Start after title animation
                    alpha = Math.min(1.0f, (elapsed - 300) / 500.0f);
                }
                
                // Only render if we have some visibility
                if (alpha > 0.01f) {
                    // Save current pose
                    guiGraphics.pose().pushPose();
                    
                    // Apply alpha transformation
                    int alphaInt = (int)(alpha * 255.0f);
                    guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
                    
                    // Create/update the grid
                    grid = createGrid();
                    
                    // Render the grid with alpha applied
                    grid.render(guiGraphics, mouseX, mouseY);
                    
                    // Restore original pose and color
                    guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    guiGraphics.pose().popPose();
                }
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Pass the event to the grid if it was created
                if (grid != null) {
                    return grid.mouseClicked((int)mouseX, (int)mouseY, button);
                }
                return false;
            }
        };
        
        // Add the grid host component to the panel
        panel.addChild(gridHost);
    }
    
    @Override
    public void update() {
        // This causes a refresh of the overview data when called
        if (gridHost != null) {
            // Force recreation of the grid with updated data on next render
            gridHost.setVisible(false);
            gridHost.setVisible(true);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // No scrolling needed for overview tab, but forward events anyway
        return gridHost != null && gridHost.mouseScrolled(mouseX, mouseY, delta);
    }
} 