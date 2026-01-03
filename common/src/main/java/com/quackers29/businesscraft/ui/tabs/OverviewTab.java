package com.quackers29.businesscraft.ui.tabs;

import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.components.basic.BCLabel;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.components.containers.StandardTabContent;
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
     * @param width        The width of the tab panel
     * @param height       The height of the tab panel
     */
    public OverviewTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);

        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
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

            // Population: 16 (40)
            String popStr = parentScreen.getCachedPopulation() + " ("
                    + (int) parentScreen.getCacheManager().getCachedPopulationCap() + ")";
            overviewData.put("Population:", popStr);

            // Happiness
            float hap = parentScreen.getCacheManager().getCachedHappiness();
            String hapStr = String.format("%.0f%%", hap);
            if (hap >= 80)
                hapStr += " (High)";
            else if (hap <= 30)
                hapStr += " (Low)";
            else
                hapStr += " (Normal)";
            overviewData.put("Happiness:", hapStr);

            overviewData.put("Tourists:", parentScreen.getTouristString());

            // Add cumulative tourism stats
            int totalTourists = parentScreen.getCacheManager().getCachedTotalTouristsArrived();
            double totalDist = parentScreen.getCacheManager().getCachedTotalTouristDistance();
            String distStr;
            if (totalDist >= 1000) {
                distStr = String.format("%.1fkm", totalDist / 1000.0);
            } else {
                distStr = String.format("%dm", (int) totalDist);
            }
            overviewData.put("Tourism:", String.format("%d (%s)", totalTourists, distStr));

            String biome = parentScreen.getCacheManager().getCachedBiome();
            String variant = parentScreen.getCacheManager().getCachedBiomeVariant();
            if (!variant.equals("Unknown")) {
                overviewData.put("Biome:", variant + " (" + biome + ")");
            } else {
                overviewData.put("Biome:", biome);
            }
            return overviewData;
        });

        // Add the content component to the panel
        panel.addChild(contentComponent);
    }

    @Override
    public void update() {
        // This causes a refresh of the overview data when called
        // We don't want to refresh the grid here as it resets scroll state
        // if (contentComponent != null) {
        // contentComponent.refresh();
        // }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // No scrolling needed for overview tab, but forward events anyway
        return contentComponent != null && contentComponent.mouseScrolled(mouseX, mouseY, delta);
    }
}
