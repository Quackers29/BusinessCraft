package com.quackers29.businesscraft.ui.tabs;

import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.components.containers.StandardTabContent;
import com.quackers29.businesscraft.config.registries.UpgradeRegistry;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upgrades tab implementation for the Town Interface.
 * Displays unlocked upgrades.
 */
public class UpgradesTab extends BaseTownTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradesTab.class);

    private StandardTabContent contentComponent;

    /**
     * Creates a new Upgrades tab.
     * 
     * @param parentScreen The parent screen
     * @param width        The width of the tab panel
     * @param height       The height of the tab panel
     */
    public UpgradesTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);

        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }

    @Override
    public void init(Consumer<Button> registerWidget) {
        // Add title first
        panel.addChild(createHeaderLabel("RESEARCH & UPGRADES"));

        // Create standardized content component
        contentComponent = createStandardContent(
                StandardTabContent.ContentType.CUSTOM_LIST,
                "UPGRADES");

        // Configure with upgrades data supplier
        contentComponent.withCustomData(() -> {
            Set<String> unlockedIds = parentScreen.getCacheManager().getCachedUpgrades();

            // Prepare arrays for the custom list columns
            int size = unlockedIds.size();
            String[] names = new String[size];
            String[] descriptions = new String[size];
            int[] progress = new int[size]; // Placeholder for now

            int i = 0;
            for (String id : unlockedIds) {
                UpgradeRegistry.UpgradeNode node = UpgradeRegistry.get(id);
                if (node != null) {
                    names[i] = node.displayName;
                    descriptions[i] = node.description;
                } else {
                    names[i] = id;
                    descriptions[i] = "Unknown Upgrade";
                }
                progress[i] = 100; // All listed upgrades are unlocked
                i++;
            }
            // Return 3 columns to match the expected format of CUSTOM_LIST in
            // StandardTabContent
            return new Object[] { names, descriptions, progress };
        });

        // Add to panel
        panel.addChild(contentComponent);
    }

    @Override
    public void update() {
        // This causes a refresh of the data when called
        if (contentComponent != null) {
            contentComponent.refresh();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Always forward scroll events to contentComponent
        if (contentComponent != null) {
            return contentComponent.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }
}
