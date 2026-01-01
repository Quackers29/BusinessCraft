package com.quackers29.businesscraft.ui.tabs;

import com.quackers29.businesscraft.ui.managers.TownDataCacheManager;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.components.containers.StandardTabContent;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.production.UpgradeNode;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Production tab implementation (replaces Population tab).
 * Displays active research and production processes.
 */
public class ProductionTab extends BaseTownTab {

    private StandardTabContent contentComponent;

    public ProductionTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }

    private enum ViewMode {
        ACTIVE, UPGRADES
    }

    private ViewMode viewMode = ViewMode.ACTIVE;

    @Override
    public void handleAction(String action) {
        if ("view_active_production".equals(action)) {
            viewMode = ViewMode.ACTIVE;
            update();
        } else if ("view_upgrades".equals(action)) {
            viewMode = ViewMode.UPGRADES;
            update();
        }
    }

    @Override
    public void init(Consumer<Button> registerWidget) {
        panel.addChild(createHeaderLabel("PRODUCTION / UPGRADES"));

        contentComponent = createStandardContent(
                StandardTabContent.ContentType.PRODUCTION_LIST,
                "PRODUCTION");

        // Configure data supplier
        contentComponent.withCustomData(() -> {
            TownDataCacheManager cache = parentScreen.getCacheManager();

            List<String> names = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<String> progress = new ArrayList<>();
            List<String> tooltipList = new ArrayList<>();

            if (viewMode == ViewMode.ACTIVE) {
                // Research
                String currentResearch = cache.getCachedCurrentResearch();
                if (currentResearch != null && !currentResearch.isEmpty()) {
                    names.add(currentResearch);
                    types.add("Research");

                    float currentDays = cache.getCachedResearchProgress();
                    UpgradeNode node = UpgradeRegistry.get(currentResearch);
                    if (node != null && node.getResearchDays() > 0) {
                        int pct = (int) ((currentDays / node.getResearchDays()) * 100);
                        if (pct > 100)
                            pct = 100;
                        progress.add(pct + "%");

                        // Tooltip for research
                        StringBuilder sb = new StringBuilder();
                        sb.append(node.getDisplayName()).append("\n");
                        sb.append("Researching...").append("\n\n");
                        sb.append(node.getDescription());
                        tooltipList.add(sb.toString());
                    } else {
                        progress.add(String.format("%.1f d", currentDays));
                        tooltipList.add(null);
                    }
                }

                // Production
                Map<String, Float> productions = cache.getCachedActiveProductions();
                if (productions != null) {
                    for (Map.Entry<String, Float> entry : productions.entrySet()) {
                        String id = entry.getKey();
                        names.add(id);
                        types.add("Production");
                        int pct = (int) (entry.getValue() * 100);
                        if (pct > 100)
                            pct = 100;
                        progress.add(pct + "%");

                        // Tooltip for production
                        com.quackers29.businesscraft.production.ProductionRecipe recipe = com.quackers29.businesscraft.production.ProductionRegistry
                                .get(id);
                        if (recipe != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Base Cycle: ").append(recipe.getBaseCycleTimeDays()).append(" days\n");

                            // Check modifier
                            java.util.Set<String> unlocked = cache.getCachedUnlockedNodes();
                            float modifier = 0f;
                            if (unlocked != null) {
                                for (String nodeId : unlocked) {
                                    UpgradeNode unode = UpgradeRegistry.get(nodeId);
                                    if (unode != null) {
                                        for (com.quackers29.businesscraft.data.parsers.Effect eff : unode
                                                .getEffects()) {
                                            if (eff.getTarget().equals(id)) {
                                                modifier += eff.getValue(); // additive speed
                                            }
                                        }
                                    }
                                }
                            }

                            if (modifier > 0) {
                                float currentCycle = recipe.getBaseCycleTimeDays() / modifier;
                                sb.append("Current Cycle: ").append(String.format("%.2f", currentCycle))
                                        .append(" days\n");
                                sb.append("Speed: ").append(String.format("%.0f%%", modifier * 100));
                            } else {
                                sb.append("Normal Speed (100%)");
                            }
                            tooltipList.add(sb.toString());
                        } else {
                            tooltipList.add(null);
                        }
                    }
                }

                if (names.isEmpty()) {
                    names.add("No Active Production");
                    types.add("-");
                    progress.add("-");
                    tooltipList.add(null);
                }
            } else {
                // Upgrades View
                // 1. Current Research
                String currentResearch = cache.getCachedCurrentResearch();
                if (currentResearch != null && !currentResearch.isEmpty()) {
                    names.add(currentResearch);
                    types.add("Researching");

                    float currentDays = cache.getCachedResearchProgress();
                    UpgradeNode node = UpgradeRegistry.get(currentResearch);
                    if (node != null && node.getResearchDays() > 0) {
                        int pct = (int) ((currentDays / node.getResearchDays()) * 100);
                        if (pct > 100)
                            pct = 100;
                        progress.add(pct + "%");

                        StringBuilder sb = new StringBuilder();
                        sb.append(node.getDisplayName()).append("\n");
                        sb.append(node.getDescription()).append("\n\n");
                        sb.append("Effects:\n");
                        for (com.quackers29.businesscraft.data.parsers.Effect eff : node.getEffects()) {
                            sb.append(" ").append(eff.getTarget()).append(": ").append(eff.getValue()).append("\n");
                        }
                        tooltipList.add(sb.toString());
                    } else {
                        progress.add("0%");
                        tooltipList.add(null);
                    }
                }

                // 2. Unlocked Upgrades
                java.util.Set<String> unlocked = cache.getCachedUnlockedNodes();
                if (unlocked != null && !unlocked.isEmpty()) {
                    for (String nodeId : unlocked) {
                        names.add(nodeId);
                        types.add("Upgrade");
                        progress.add("Unlocked");

                        UpgradeNode node = UpgradeRegistry.get(nodeId);
                        if (node != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(node.getDisplayName()).append("\n");
                            sb.append(node.getDescription()).append("\n\n");
                            sb.append("Effects:\n");
                            for (com.quackers29.businesscraft.data.parsers.Effect eff : node.getEffects()) {
                                sb.append(" ").append(eff.getTarget()).append(": ").append(eff.getValue()).append("\n");
                            }
                            tooltipList.add(sb.toString());
                        } else {
                            tooltipList.add(null);
                        }
                    }
                }

                if (names.isEmpty()) {
                    names.add("No Upgrades");
                    types.add("-");
                    progress.add("-");
                    tooltipList.add(null);
                }
            }

            return new Object[] { names.toArray(new String[0]), types.toArray(new String[0]),
                    progress.toArray(new String[0]), tooltipList.toArray(new String[0]) };
        });

        panel.addChild(contentComponent);
    }

    @Override
    public void update() {
        if (contentComponent != null) {
            contentComponent.refresh();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentComponent != null) {
            return contentComponent.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }
}
