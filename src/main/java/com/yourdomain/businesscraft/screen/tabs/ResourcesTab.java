package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.components.BCComponent;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import com.yourdomain.businesscraft.screen.components.UIGridBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Resources tab implementation for the Town Interface.
 * Displays all town resources in a scrollable grid.
 */
public class ResourcesTab extends BaseTownTab {
    private static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    private static final int TEXT_HIGHLIGHT = 0xFFDDFFFF;      // Light cyan highlight text
    private static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    
    private BCComponent resourceListHost;
    
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
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("RESOURCES", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Calculate dimensions for the resource grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Create a custom component to host the resource list using UIGridBuilder
        resourceListHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance with scrolling
            private UIGridBuilder grid;
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Get resources from the parent screen using the public getter
                Map<Item, Integer> resources = parentScreen.getCachedResources();
                
                // Convert to a sorted list for display
                List<Map.Entry<Item, Integer>> sortedResources = new ArrayList<>(resources.entrySet());
                
                // Sort by item name
                sortedResources.sort((a, b) -> {
                    String nameA = a.getKey().getDescriptionId();
                    String nameB = b.getKey().getDescriptionId();
                    return nameA.compareToIgnoreCase(nameB);
                });
                
                // Create or recreate the grid with the proper number of rows
                if (grid == null) {
                    createGrid(sortedResources);
                }
                
                // Draw the grid background and elements
                if (sortedResources.isEmpty()) {
                    // Create a simple grid with a "No resources" message
                    if (grid == null) {
                        grid = new UIGridBuilder(x, y, width, height, 1, 1)
                            .withBackgroundColor(BACKGROUND_COLOR)
                            .withBorderColor(BORDER_COLOR)
                            .withMargins(15, 10)
                            .withSpacing(15, 10)
                            .drawBorder(true);
                        grid.addLabel(0, 0, "No resources available", TEXT_COLOR);
                    }
                }
                
                // Render the grid
                grid.render(guiGraphics, mouseX, mouseY);
            }
            
            private void createGrid(List<Map.Entry<Item, Integer>> resources) {
                // Calculate the number of rows based on the resources
                int numRows = Math.max(1, resources.size());
                
                // Use the create method which automatically handles rows based on data
                grid = UIGridBuilder.create(x, y, width, height, 2) // Just define columns, rows will be determined by data
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .withRowHeight(14) // Use 14px row height to fit more rows
                    .drawBorder(true);
                
                // Convert resources list to a map
                Map<Item, Integer> resourceMap = new HashMap<>();
                for (Map.Entry<Item, Integer> entry : resources) {
                    resourceMap.put(entry.getKey(), entry.getValue());
                }
                
                // Use the withItemQuantityPairs method to populate the grid
                grid.withItemQuantityPairs(resourceMap, TEXT_HIGHLIGHT);
                
                System.out.println("Resources Tab: Created grid with " + resources.size() + " items");
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Always try to handle mouse clicks if we have a grid
                if (grid != null) {
                    return grid.mouseClicked((int)mouseX, (int)mouseY, button);
                }
                return false;
            }
            
            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                // Always process scrolling regardless of mouse position
                if (grid != null) {
                    // Convert delta to integer scrolling amount (1 or -1)
                    int scrollAmount = delta > 0 ? 1 : -1;
                    
                    // Apply scrolling to the grid - log for debugging
                    System.out.println("Resources Tab processing scroll in component: " + scrollAmount);
                    
                    // Always force scroll processing even if cursor isn't directly over grid
                    // This ensures consistent behavior with middle mouse wheel
                    return grid.mouseScrolled(mouseX, mouseY, delta);
                }
                return false;
            }
            
            public boolean isMouseOver(int mouseX, int mouseY) {
                // Use a more lenient check to capture scrolling near the edges
                return mouseX >= x - 10 && mouseX < x + width + 10 && 
                       mouseY >= y - 10 && mouseY < y + height + 10;
            }
        };
        
        panel.addChild(resourceListHost);
    }
    
    @Override
    public void update() {
        // This tab doesn't need any periodic updates beyond the standard rendering
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Always forward scroll events to resourceListHost regardless of mouse position
        if (resourceListHost != null) {
            // Log for debugging
            System.out.println("ResourcesTab forwarding scroll: " + delta);
            
            // Try with original coordinates first
            if (resourceListHost.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
            
            // If that didn't work, try with slight position adjustments
            // This is a technique used in the population tab to ensure scroll events are captured
            for (int i = 0; i < 9; i++) {
                double adjustedX = mouseX + (i % 3 - 1) * 5; // Try offsets of -5, 0, +5
                double adjustedY = mouseY + (i / 3 - 1) * 5;
                if (resourceListHost.mouseScrolled(adjustedX, adjustedY, delta)) {
                    System.out.println("ResourcesTab scroll succeeded with adjustment " + i);
                    return true;
                }
            }
        }
        return false;
    }
} 