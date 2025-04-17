package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.components.BCComponent;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.BCPanel;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.components.UIGridBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the creation and management of the Resources tab in the Town Interface Screen.
 */
public class ResourcesTab {
    private final TownInterfaceScreen screen;
    private final BCPanel panel;
    private final int textHighlight;
    private final int backgroundColor;
    private final int borderColor;

    public ResourcesTab(TownInterfaceScreen screen, BCPanel panel, int textHighlight, int backgroundColor, int borderColor) {
        this.screen = screen;
        this.panel = panel;
        this.textHighlight = textHighlight;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public void create() {
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("RESOURCES", panel.getInnerWidth());
        titleLabel.withTextColor(textHighlight).withShadow(true);
        panel.addChild(titleLabel);

        // Calculate dimensions for the resource grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid

        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;

        // Create a custom component to host the resource list using UIGridBuilder
        BCComponent resourceListHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance with scrolling
            private UIGridBuilder grid;

            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Get resources from menu
                Map<Item, Integer> resources = getResources();

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
                            .withBackgroundColor(backgroundColor)
                            .withBorderColor(borderColor)
                            .withMargins(15, 10)
                            .withSpacing(15, 10)
                            .drawBorder(true);
                        grid.addLabel(0, 0, "No resources available", textHighlight);
                    }
                }

                // Render the grid
                grid.render(guiGraphics, mouseX, mouseY);
            }

            private void createGrid(List<Map.Entry<Item, Integer>> resources) {
                // Calculate the number of rows based on the resources
                int numRows = Math.max(1, resources.size());

                // Use the new create method which automatically handles rows based on data
                grid = UIGridBuilder.create(x, y, width, height, 2) // Just define columns, rows will be determined by data
                    .withBackgroundColor(backgroundColor)
                    .withBorderColor(borderColor)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .withRowHeight(14) // Use 14px row height to fit more rows
                    .drawBorder(true);

                // Convert resources list to a map
                Map<Item, Integer> resourceMap = new HashMap<>();
                for (Map.Entry<Item, Integer> entry : resources) {
                    resourceMap.put(entry.getKey(), entry.getValue());
                }

                // Use the new withItemQuantityPairs method to populate the grid
                grid.withItemQuantityPairs(resourceMap, textHighlight);

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
                // Always try to handle scrolling, even if the mouse is not exactly over the grid
                // This matches the resources tab's approach that works reliably
                if (grid != null) {
                    System.out.println("Resources Tab forwarding scroll: " + delta);
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

    private Map<Item, Integer> getResources() {
        try {
            return (Map<Item, Integer>) screen.getClass().getDeclaredMethod("getCachedResources").invoke(screen);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public BCPanel getPanel() {
        return panel;
    }
} 