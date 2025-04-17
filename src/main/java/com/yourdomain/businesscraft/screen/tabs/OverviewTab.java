package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.components.BCComponent;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.BCPanel;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.components.UIGridBuilder;
import net.minecraft.client.gui.GuiGraphics;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles the creation and management of the Overview tab in the Town Interface Screen.
 */
public class OverviewTab {
    private final TownInterfaceScreen screen;
    private final BCPanel panel;
    private final int textHighlight;
    private final int textColor;
    private final int backgroundColor;
    private final int borderColor;

    public OverviewTab(TownInterfaceScreen screen, BCPanel panel, int textHighlight, int textColor, int backgroundColor, int borderColor) {
        this.screen = screen;
        this.panel = panel;
        this.textHighlight = textHighlight;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public void create() {
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("TOWN OVERVIEW", panel.getInnerWidth());
        titleLabel.withTextColor(textHighlight).withShadow(true);
        panel.addChild(titleLabel);

        // Calculate dimensions for the town info grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid

        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;

        // Add animated town info component with appropriate dimensions
        BCComponent gridHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance
            private UIGridBuilder grid;
            private float alpha = 0.0f;
            private long startTime;
            private boolean animationStarted = false;

            // Initialize the grid when rendering
            private UIGridBuilder createGrid() {
                // Use the new utility method for label-value pairs
                Map<String, String> overviewData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
                overviewData.put("Town Name:", getTownName());
                overviewData.put("Population:", String.valueOf(getPopulation()));
                overviewData.put("Tourists:", getTouristInfo());

                return UIGridBuilder.createLabelValueGrid(
                    x, y, getWidth(), getHeight(),
                    textColor, textHighlight,
                    overviewData)
                    .withBackgroundColor(backgroundColor)
                    .withBorderColor(borderColor)
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

    private String getTownName() {
        try {
            return (String) screen.getClass().getDeclaredMethod("getCachedTownName").invoke(screen);
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    private int getPopulation() {
        try {
            return (int) screen.getClass().getDeclaredMethod("getCachedPopulation").invoke(screen);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getTouristInfo() {
        try {
            return (String) screen.getClass().getDeclaredMethod("getTouristString").invoke(screen);
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    public BCPanel getPanel() {
        return panel;
    }
} 