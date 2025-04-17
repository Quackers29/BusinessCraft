package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.components.BCComponent;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.BCPanel;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.components.UIGridBuilder;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles the creation and management of the Population tab in the Town Interface Screen.
 */
public class PopulationTab {
    private final TownInterfaceScreen screen;
    private final BCPanel panel;
    private final int textHighlight;
    private final int backgroundColor;
    private final int borderColor;

    public PopulationTab(TownInterfaceScreen screen, BCPanel panel, int textHighlight, int backgroundColor, int borderColor) {
        this.screen = screen;
        this.panel = panel;
        this.textHighlight = textHighlight;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public void create() {
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("POPULATION", panel.getInnerWidth());
        titleLabel.withTextColor(textHighlight).withShadow(true);
        panel.addChild(titleLabel);

        // Calculate dimensions for the population list - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid

        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;

        // Create a custom component to host the population list using UIGridBuilder
        BCComponent populationListHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance with scrolling
            private UIGridBuilder grid;
            private int scrollOffset = 0;
            private final int itemHeight = 16; // Reduced item height for more compact display
            private final int padding = 8; // Reduced padding for more content space
            private final int verticalPadding = 5; // Even smaller vertical padding
            private int maxVisible; // Will be calculated based on available height
            private boolean isDraggingScrollbar = false;
            private boolean isMiddleMouseScrolling = false; // Track middle mouse scrolling
            private double lastMouseY = 0; // Last mouse Y position for middle mouse scrolling
            private final int scrollbarWidth = 8;

            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Get visit history from menu
                List<ITownDataProvider.VisitHistoryRecord> visitHistory = getVisitHistory();

                // Calculate the number of visible items based on available height
                maxVisible = (availableHeight - (padding * 2)) / itemHeight;

                // Create or recreate the grid with the proper number of rows
                if (grid == null) {
                    createGrid(visitHistory);
                }

                // Draw the grid background and elements
                if (visitHistory.isEmpty()) {
                    // Create a simple grid with a "No visitors" message
                    if (grid == null) {
                        grid = new UIGridBuilder(x, y, width, height, 1, 1)
                            .withBackgroundColor(backgroundColor)
                            .withBorderColor(borderColor)
                            .withMargins(15, 10)
                            .withSpacing(15, 10)
                            .drawBorder(true);
                        grid.addLabel(0, 0, "No visitors recorded", textHighlight);
                    }
                }

                // Render the grid
                grid.render(guiGraphics, mouseX, mouseY);

                // Render scrollbar if needed
                if (visitHistory.size() > maxVisible) {
                    renderScrollbar(guiGraphics, mouseX, mouseY, visitHistory.size());
                }
            }

            private void createGrid(List<ITownDataProvider.VisitHistoryRecord> visitHistory) {
                // Calculate the number of rows based on the visit history
                int numRows = Math.max(1, visitHistory.size());

                // Use the new create method which automatically handles rows based on data
                grid = UIGridBuilder.create(x, y, width, height, 2) // Just define columns, rows will be determined by data
                    .withBackgroundColor(backgroundColor)
                    .withBorderColor(borderColor)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .withRowHeight(itemHeight) // Use compact row height
                    .drawBorder(true);

                // Add visit history data to the grid
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm");
                for (int i = 0; i < visitHistory.size(); i++) {
                    ITownDataProvider.VisitHistoryRecord record = visitHistory.get(i);
                    // Use reflection to access the record fields
                    String playerName = getPlayerName(record);
                    String visitTime = dateFormat.format(new Date(getVisitTime(record)));
                    grid.addLabel(0, i, playerName, textHighlight);
                    grid.addLabel(1, i, visitTime, textHighlight);
                }

                System.out.println("Population Tab: Created grid with " + visitHistory.size() + " visitors");
            }

            private String getPlayerName(ITownDataProvider.VisitHistoryRecord record) {
                try {
                    return (String) record.getClass().getMethod("getPlayerName").invoke(record);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Unknown";
                }
            }

            private long getVisitTime(ITownDataProvider.VisitHistoryRecord record) {
                try {
                    return (long) record.getClass().getMethod("getVisitTime").invoke(record);
                } catch (Exception e) {
                    e.printStackTrace();
                    return System.currentTimeMillis();
                }
            }

            private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, int totalItems) {
                // Calculate scrollbar dimensions
                int scrollbarHeight = Math.max(20, (maxVisible * height) / totalItems);
                int scrollbarY = y + padding + (scrollOffset * (height - scrollbarHeight - (padding * 2))) / (totalItems - maxVisible);
                int scrollbarX = x + width - scrollbarWidth - padding;

                // Draw scrollbar background
                guiGraphics.fill(scrollbarX, y + padding, scrollbarX + scrollbarWidth, y + height - padding, backgroundColor);

                // Draw scrollbar handle
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, borderColor);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Check if mouse is over the scrollbar
                if (isMouseOverScrollbar((int)mouseX, (int)mouseY)) {
                    isDraggingScrollbar = true;
                    return true;
                }

                // Pass the event to the grid if it was created
                if (grid != null) {
                    return grid.mouseClicked((int)mouseX, (int)mouseY, button);
                }
                return false;
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                // Handle scrollbar dragging
                if (isDraggingScrollbar) {
                    List<ITownDataProvider.VisitHistoryRecord> visitHistory = getVisitHistory();
                    int totalItems = visitHistory.size();
                    int scrollableHeight = height - (padding * 2);
                    int scrollbarHeight = Math.max(20, (maxVisible * height) / totalItems);
                    int maxScrollOffset = totalItems - maxVisible;

                    // Calculate new scroll offset based on mouse position
                    double scrollPercentage = (mouseY - (y + padding)) / (double)(scrollableHeight - scrollbarHeight);
                    scrollOffset = (int)(scrollPercentage * maxScrollOffset);
                    scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

                    return true;
                }

                // Handle middle mouse scrolling
                if (button == 2) { // Middle mouse button
                    if (!isMiddleMouseScrolling) {
                        isMiddleMouseScrolling = true;
                        lastMouseY = mouseY;
                    } else {
                        double delta = lastMouseY - mouseY;
                        if (Math.abs(delta) > 5) { // Threshold to prevent tiny movements
                            scrollOffset += (int)(delta / 10);
                            scrollOffset = Math.max(0, Math.min(scrollOffset, getVisitHistory().size() - maxVisible));
                            lastMouseY = mouseY;
                        }
                    }
                    return true;
                }

                // Pass the event to the grid if it was created
                if (grid != null) {
                    return grid.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                }
                return false;
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                // Reset dragging state
                if (isDraggingScrollbar) {
                    isDraggingScrollbar = false;
                    return true;
                }

                // Reset middle mouse scrolling state
                if (isMiddleMouseScrolling) {
                    isMiddleMouseScrolling = false;
                    return true;
                }

                // Pass the event to the grid if it was created
                if (grid != null) {
                    return grid.mouseReleased(mouseX, mouseY, button);
                }
                return false;
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                // Handle mouse wheel scrolling
                if (isMouseOver((int)mouseX, (int)mouseY)) {
                    scrollOffset += (int)(delta * 3); // Adjust scroll speed
                    scrollOffset = Math.max(0, Math.min(scrollOffset, getVisitHistory().size() - maxVisible));
                    return true;
                }
                return false;
            }

            private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
                List<ITownDataProvider.VisitHistoryRecord> visitHistory = getVisitHistory();
                if (visitHistory.size() <= maxVisible) {
                    return false; // No scrollbar needed
                }

                int scrollbarHeight = Math.max(20, (maxVisible * height) / visitHistory.size());
                int scrollbarY = y + padding + (scrollOffset * (height - scrollbarHeight - (padding * 2))) / (visitHistory.size() - maxVisible);
                int scrollbarX = x + width - scrollbarWidth - padding;

                return mouseX >= scrollbarX && mouseX < scrollbarX + scrollbarWidth &&
                       mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight;
            }

            @Override
            public boolean isMouseOver(int mouseX, int mouseY) {
                // Use a more lenient check to capture scrolling near the edges
                return mouseX >= x - 10 && mouseX < x + width + 10 && 
                       mouseY >= y - 10 && mouseY < y + height + 10;
            }
        };

        panel.addChild(populationListHost);
    }

    private List<ITownDataProvider.VisitHistoryRecord> getVisitHistory() {
        try {
            return (List<ITownDataProvider.VisitHistoryRecord>) screen.getClass().getDeclaredMethod("getCachedVisitHistory").invoke(screen);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public BCPanel getPanel() {
        return panel;
    }
} 