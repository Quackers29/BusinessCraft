package com.quackers29.businesscraft.ui.tabs;

import com.quackers29.businesscraft.ui.managers.TownDataCacheManager;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.components.containers.StandardTabContent;
import com.quackers29.businesscraft.ui.components.basic.BCLabel;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.production.UpgradeNode;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;

/**
 * Production tab implementation (replaces Population tab).
 * Displays active research and production processes.
 */
public class ProductionTab extends BaseTownTab {

    private StandardTabContent contentComponent;
    private BCLabel headerLabel;

    public ProductionTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }

    private enum ViewMode {
        ACTIVE, UPGRADES
    }

    private ViewMode viewMode = ViewMode.ACTIVE;
    private long lastUpdateTimestamp = 0;
    private static final long UPDATE_THROTTLE_MS = 2000; // 2 seconds

    @Override
    public void handleAction(String action) {
        if ("view_active_production".equals(action)) {
            viewMode = ViewMode.ACTIVE;
            forceUpdate(); // Immediate update on action
        } else if ("view_upgrades".equals(action)) {
            viewMode = ViewMode.UPGRADES;
            forceUpdate(); // Immediate update on action
        }
    }

    private void forceUpdate() {
        lastUpdateTimestamp = System.currentTimeMillis();
        updateHeader();
        if (contentComponent != null) {
            contentComponent.refresh();
        }
    }

    private void updateHeader() {
        if (headerLabel != null) {
            String title = (viewMode == ViewMode.ACTIVE) ? "PRODUCTION" : "UPGRADES";
            headerLabel.setText(Component.literal(title));
        }
    }

    @Override
    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTimestamp >= UPDATE_THROTTLE_MS) {
            lastUpdateTimestamp = now;
            updateHeader();
            if (contentComponent != null) {
                contentComponent.refresh();
            }
        }
    }

    @Override
    public void init(Consumer<Button> registerWidget) {
        headerLabel = createHeaderLabel("PRODUCTION");
        panel.addChild(headerLabel);

        contentComponent = createStandardContent(
                StandardTabContent.ContentType.PRODUCTION_LIST,
                "PRODUCTION");

        // Configure data supplier
        contentComponent.withCustomData(() -> {
            TownDataCacheManager cache = parentScreen.getCacheManager();

            List<String> names = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Object> progress = new ArrayList<>();
            List<String> tooltipList = new ArrayList<>();

            if (viewMode == ViewMode.ACTIVE) {
                // Production Only
                // NEW: Use server-authoritative view-model directly
                var productionViewModel = cache.getProductionViewModel();

                if (productionViewModel != null) {
                    for (com.quackers29.businesscraft.town.viewmodel.ProductionStatusViewModel.ProductionRecipeInfo info : productionViewModel
                            .getProductionInfo().values()) {
                        // All display strings come from server view-model
                        names.add(info.getDisplayName());

                        // Column 2: Percentage
                        int pct = (int) (info.getProgressPercentage() * 100);
                        progress.add(pct + "%");

                        // Tooltip from server string components
                        StringBuilder sb = new StringBuilder();
                        sb.append("Status: ").append(info.getStatusText()).append("\n");
                        sb.append("Progress: ").append(info.getProgressText()).append("\n\n");

                        sb.append("Inputs: ").append(info.getInputsText()).append("\n");
                        sb.append("Outputs: ").append(info.getOutputsText()).append("\n\n");

                        sb.append("Cycle Time: ").append(info.getCycleTimeText()).append("\n");
                        sb.append("Production Rate: ").append(info.getProductionRateText()).append("\n");

                        tooltipList.add(sb.toString());
                    }
                } else {
                    // ViewModel not yet received
                    names.add("Loading production data...");
                    progress.add("");
                    tooltipList.add("Waiting for server...");
                    return new Object[] { names.toArray(new String[0]), progress.toArray(),
                            tooltipList.toArray(new String[0]) };
                }

                if (names.isEmpty()) {
                    names.add("No Active Production");
                    progress.add("-");
                    tooltipList.add(null);
                }
            } else {
                // Upgrades View (Research + Unlocked + Locked)
                // NEW: Use server-authoritative view-model instead of UpgradeRegistry
                var upgradeViewModel = cache.getUpgradeViewModel();

                if (upgradeViewModel == null) {
                    // Fallback if view-model not yet received from server
                    names.add("Loading upgrades...");
                    tooltipList.add("Waiting for server data...");
                    progress.add("");
                    return new Object[] { names.toArray(new String[0]), progress.toArray(),
                            tooltipList.toArray(new String[0]) };
                }

                // NEW: Build display from server-calculated view-model data (NO CLIENT
                // CALCULATIONS)
                // Helper to add upgrades to display
                java.util.function.BiConsumer<com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel.UpgradeDisplayInfo, Integer> addUpgrade = (
                        info, level) -> {
                    // ALL DATA FROM SERVER VIEW-MODEL (NO CLIENT CALCULATIONS)
                    String displayName = info.getDisplayName();
                    if (info.isRepeatable()) {
                        displayName += " (" + level + ")";
                    }
                    names.add(displayName);

                    // Use server-calculated status and progress
                    if (info.isCurrentResearch()) {
                        int pct = (int) (info.getProgressPercentage() * 100);
                        progress.add(pct + "%");
                    } else if (info.isUnlocked() && level <= info.getCurrentLevel()) {
                        progress.add(UIGridBuilder.StatusSymbol.UNLOCKED);
                    } else {
                        progress.add(UIGridBuilder.StatusSymbol.LOCKED);
                    }

                    // Build tooltip from SERVER-PROVIDED STRINGS (NO CALCULATIONS)
                    StringBuilder sb = new StringBuilder();
                    sb.append(displayName).append("\n");

                    if (info.isRepeatable()) {
                        String maxStr = (info.getMaxLevel() == -1) ? "Infinite" : String.valueOf(info.getMaxLevel());
                        sb.append("Max Level: ").append(maxStr).append("\n");
                    }

                    sb.append(info.getDescription()).append("\n\n");
                    sb.append("Status: ").append(info.getStatusText()).append("\n");

                    if (info.isCurrentResearch()) {
                        sb.append("Progress: ").append(info.getProgressText()).append("\n");
                    }

                    // All these strings are PRE-CALCULATED by server
                    if (!info.getResearchTimeText().isEmpty()) {
                        sb.append("Research Time: ").append(info.getResearchTimeText());
                        if (!info.getBaseResearchTimeText().isEmpty()) {
                            sb.append(" ").append(info.getBaseResearchTimeText());
                        }
                        sb.append("\n");
                    }

                    if (!info.getCostsText().equals("None")) {
                        sb.append("\nCost:\n - ").append(info.getCostsText().replace(", ", "\n - ")).append("\n");
                    }

                    if (!info.getRequirementsText().equals("None")) {
                        sb.append("\nRequires:\n - ").append(info.getRequirementsText().replace(", ", "\n - "))
                                .append("\n");
                    }

                    if (!info.getPrerequisitesText().equals("None")) {
                        sb.append("\nPrerequisites: ").append(info.getPrerequisitesText()).append("\n");
                    }

                    // AI Score from server
                    if (!info.isUnlocked() && info.getAiScore() > 0) {
                        sb.append("\nTown Priority: ").append(String.format("%.1f", info.getAiScore())).append("\n");
                    }

                    if (!info.getEffectsText().equals("None")) {
                        sb.append("\nEffects:\n - ").append(info.getEffectsText().replace(", ", "\n - ")).append("\n");
                    }

                    tooltipList.add(sb.toString());
                };

                // Process unlocked upgrades (all levels)
                for (String nodeId : upgradeViewModel.getUnlockedUpgradeIds()) {
                    var info = upgradeViewModel.getUpgradeInfo(nodeId);
                    if (info == null)
                        continue;

                    for (int lv = 1; lv <= info.getCurrentLevel(); lv++) {
                        addUpgrade.accept(info, lv);
                    }
                }

                // Process current research
                for (String nodeId : upgradeViewModel.getResearchableUpgradeIds()) {
                    var info = upgradeViewModel.getUpgradeInfo(nodeId);
                    if (info != null && info.isCurrentResearch()) {
                        addUpgrade.accept(info, info.getCurrentLevel() + 1);
                        break;
                    }
                }

                // Process researchable/locked upgrades (already sorted by AI score on server)
                for (String nodeId : upgradeViewModel.getResearchableUpgradeIds()) {
                    var info = upgradeViewModel.getUpgradeInfo(nodeId);
                    if (info != null && !info.isCurrentResearch()) {
                        addUpgrade.accept(info, info.getCurrentLevel() + 1);
                    }
                }

                // Process locked upgrades (prerequisites not met)
                for (String nodeId : upgradeViewModel.getLockedUpgradeIds()) {
                    var info = upgradeViewModel.getUpgradeInfo(nodeId);
                    if (info != null) {
                        addUpgrade.accept(info, info.getCurrentLevel() + 1);
                    }
                }

                if (names.isEmpty()) {
                    names.add("No Upgrades");
                    progress.add("-");
                    tooltipList.add(null);
                }
            }

            // Return 2 arrays + tooltips (Names, Progress/Status, Tooltips)
            // progress is now Object[]
            return new Object[] { names.toArray(new String[0]), progress.toArray(),
                    tooltipList.toArray(new String[0]) };
        });

        panel.addChild(contentComponent);
    }

    private static class UpgradeDisplayEntry {
        UpgradeNode node;
        int level;
        Object status; // Changed from String to Object

        public UpgradeDisplayEntry(UpgradeNode node, int level, Object status) {
            this.node = node;
            this.level = level;
            this.status = status;
        }
    }

    // Client-side implementation of local town state for AI scoring - REMOVED
    // (Centralized on Server)
    /*
     * private static class ClientTownState implements
     * com.quackers29.businesscraft.town.ai.ITownState {
     * // Removed to prevent duplication.
     * }
     */

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentComponent != null) {
            return contentComponent.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }
}
