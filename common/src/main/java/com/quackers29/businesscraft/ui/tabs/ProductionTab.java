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
                Map<String, Float> productions = cache.getCachedActiveProductions();
                if (productions != null) {
                    for (Map.Entry<String, Float> entry : productions.entrySet()) {
                        String id = entry.getKey();

                        // Get nice name from server view-model (no client config access!)
                        var recipeInfo = cache.getProductionRecipeInfo(id);
                        String displayName = (recipeInfo != null) ? recipeInfo.getDisplayName() : id;

                        names.add(displayName);
                        // Column 2: Percentage
                        int pct = (int) (entry.getValue() * 100);
                        if (pct > 100)
                            pct = 100;
                        progress.add(pct + "%");

                        // Tooltip for production (using server view-model data, no client config!)
                        if (recipeInfo != null) {
                            StringBuilder sb = new StringBuilder();

                            // Use pre-formatted strings from server view-model
                            sb.append("Status: ").append(recipeInfo.getStatusText()).append("\n");
                            sb.append("Progress: ").append(recipeInfo.getProgressText()).append("\n\n");

                            // Server-calculated display strings (no client formulas!)
                            sb.append("Inputs: ").append(recipeInfo.getInputsText()).append("\n");
                            sb.append("Outputs: ").append(recipeInfo.getOutputsText()).append("\n\n");

                            // Server-calculated cycle time and rate
                            sb.append("Cycle Time: ").append(recipeInfo.getCycleTimeText()).append("\n");
                            sb.append("Production Rate: ").append(recipeInfo.getProductionRateText()).append("\n");

                            // TODO: Server-authoritative upgrade effects view-model needed
                            // For now, simplified tooltip without client-side upgrade calculations
                            // Full implementation requires server to include "active upgrades affecting this recipe" in view-model

                            tooltipList.add(sb.toString());
                        } else {
                            tooltipList.add(null);
                        }
                    }
                }

                if (names.isEmpty()) {
                    names.add("No Active Production");
                    progress.add("-");
                    tooltipList.add(null);
                }
            } else {
                // Upgrades View (Research + Unlocked + Locked)
                List<UpgradeDisplayEntry> activeList = new ArrayList<>();
                List<UpgradeDisplayEntry> unlockedList = new ArrayList<>();
                List<UpgradeDisplayEntry> lockedList = new ArrayList<>();

                java.util.Set<String> unlockedIds = cache.getCachedUnlockedNodes();
                String currentResearchId = cache.getCachedCurrentResearch();

                // Calculate research speed client-side
                float researchSpeedCalc = 1.0f;
                if (unlockedIds != null) {
                    for (String uid : unlockedIds) {
                        UpgradeNode unode = UpgradeRegistry.get(uid);
                        if (unode != null) {
                            int ulvl = cache.getCachedUpgradeLevel(uid);
                            for (com.quackers29.businesscraft.data.parsers.Effect eff : unode.getEffects()) {
                                if ("research".equals(eff.getTarget())) {
                                    researchSpeedCalc += unode.calculateEffectValue(eff, ulvl);
                                }
                            }
                        }
                    }
                }
                if (researchSpeedCalc < 0.1f)
                    researchSpeedCalc = 0.1f;
                final float finalResearchSpeed = researchSpeedCalc;

                for (UpgradeNode node : UpgradeRegistry.getAll()) {
                    int lvl = cache.getCachedUpgradeLevel(node.getId());

                    // Add Unlocked Entries
                    for (int i = 1; i <= lvl; i++) {
                        unlockedList.add(new UpgradeDisplayEntry(node, i, UIGridBuilder.StatusSymbol.UNLOCKED));
                    }

                    // Check Next Level (Researching or Locked)
                    boolean isMaxed = false;
                    if (!node.isRepeatable()) {
                        if (lvl >= 1)
                            isMaxed = true;
                    } else {
                        if (node.getMaxRepeats() != -1 && lvl >= node.getMaxRepeats())
                            isMaxed = true;
                    }

                    if (!isMaxed) {
                        int nextLvl = lvl + 1;
                        if (currentResearchId != null && node.getId().equals(currentResearchId)) {
                            activeList.add(new UpgradeDisplayEntry(node, nextLvl, "Researching..."));
                        } else {
                            // Check prereqs
                            boolean prereqsMet = true;
                            if (node.getPrereqNodes() != null) {
                                for (String pre : node.getPrereqNodes()) {
                                    if (unlockedIds == null || !unlockedIds.contains(pre)) {
                                        prereqsMet = false;
                                        break;
                                    }
                                }
                            }
                            // Show locked if prereqs met OR if it's a repeat (implies prereqs met)
                            // Repeated upgrades (lvl > 0) have met prereqs by definition.
                            if (lvl > 0 || prereqsMet) {
                                lockedList
                                        .add(new UpgradeDisplayEntry(node, nextLvl, UIGridBuilder.StatusSymbol.LOCKED));
                            }
                        }
                    }
                }

                // Sort Locked by AI Score (Synced from server)
                lockedList.sort((e1, e2) -> {
                    double s1 = cache.getCachedAiScore(e1.node.getId());
                    double s2 = cache.getCachedAiScore(e2.node.getId());
                    return Double.compare(s2, s1); // Descending
                });

                // Helper to add node to lists
                Consumer<UpgradeDisplayEntry> addEntry = (entry) -> {
                    String name = entry.node.getDisplayName();
                    if (entry.node.isRepeatable()) {
                        name += " (" + entry.level + ")";
                    }
                    names.add(name);

                    StringBuilder sb = new StringBuilder();
                    sb.append(name).append("\n");

                    if (entry.node.isRepeatable()) {
                        String maxStr = (entry.node.getMaxRepeats() == -1) ? "Infinite"
                                : String.valueOf(entry.node.getMaxRepeats());
                        sb.append("Max Level: ").append(maxStr).append("\n");
                    }

                    sb.append(entry.node.getDescription()).append("\n\n");

                    if (entry.status instanceof com.quackers29.businesscraft.ui.builders.UIGridBuilder.StatusSymbol) {
                        sb.append("Status: ").append(entry.status.toString()).append("\n");
                    } else {
                        sb.append("Status: ").append(entry.status).append("\n");
                    }

                    if ("Researching...".equals(entry.status)) {
                        float currentMinutes = cache.getCachedResearchProgress();

                        // Calculate Scaled Duration
                        float scaledDuration = entry.node.getResearchMinutes();
                        if (entry.node.isRepeatable() && entry.level > 1) {
                            float multiplier = (float) Math.pow(entry.node.getCostMultiplier(), entry.level - 1);
                            scaledDuration *= multiplier;
                        }

                        int pct = (scaledDuration > 0)
                                ? (int) ((currentMinutes / scaledDuration) * 100)
                                : 0;
                        if (pct > 100)
                            pct = 100;
                        progress.add(pct + "%");
                    } else {
                        progress.add(entry.status);
                    }

                    // Research Time
                    if (entry.node.getResearchMinutes() > 0) {
                        float baseMins = entry.node.getResearchMinutes();

                        // Apply scaling for repeatable upgrades based on user request
                        // Same multiplier as costs
                        float levelMult = (float) Math.pow(entry.node.getCostMultiplier(), entry.level - 1);
                        float scaledMins = baseMins * levelMult;

                        float activeMins = scaledMins / finalResearchSpeed;

                        sb.append("Research Time: ").append(String.format("%.1f", activeMins)).append("m");
                        if (finalResearchSpeed != 1.0f || levelMult != 1.0f) {
                            if (levelMult != 1.0f) {
                                sb.append(String.format(" (Base: %.1fm)", scaledMins));
                            } else {
                                sb.append(String.format(" (Base: %.1fm)", baseMins));
                            }
                        }
                        sb.append("\n");
                    }

                    // Requirements / Costs (Scaled)
                    if (entry.node.getCosts() != null && !entry.node.getCosts().isEmpty()) {
                        float costMult = (float) Math.pow(entry.node.getCostMultiplier(), entry.level - 1);

                        List<String> costs = new ArrayList<>();
                        List<String> reqs = new ArrayList<>();

                        entry.node.getCosts().forEach(resAmt -> {
                            float val = resAmt.amount * costMult;
                            String line = resAmt.resourceId + ": " + String.format("%.0f", val);
                            if (resAmt.resourceId.startsWith("tourism_") || resAmt.resourceId.equals("pop")) {
                                reqs.add(line);
                            } else {
                                costs.add(line);
                            }
                        });

                        if (!costs.isEmpty()) {
                            sb.append("\nCost:\n");
                            costs.forEach(s -> sb.append(" - ").append(s).append("\n"));
                        }
                        if (!reqs.isEmpty()) {
                            sb.append("\nRequires:\n");
                            reqs.forEach(s -> sb.append(" - ").append(s).append("\n"));
                        }
                    }

                    // AI Score (Town Priority) - only for locked
                    if (UIGridBuilder.StatusSymbol.LOCKED.equals(entry.status)) {
                        double score = cache.getCachedAiScore(entry.node.getId());
                        sb.append("\nTown Priority: ").append(String.format("%.1f", score));
                    }

                    sb.append("\n\nEffects:\n");
                    // Show base effects per level
                    for (com.quackers29.businesscraft.data.parsers.Effect eff : entry.node.getEffects()) {
                        String target = eff.getTarget();

                        // Calculate Scaled Value for this level
                        float val = entry.node.calculateEffectValue(eff, entry.level);

                        // heuristic for formatting (no client config access!)
                        boolean isPercentage = false;
                        var productionViewModel = cache.getProductionViewModel();
                        if ((productionViewModel != null && productionViewModel.hasRecipe(target))
                                || "research".equals(target)) {
                            isPercentage = true;
                        }

                        String valStr;
                        if (isPercentage) {
                            valStr = String.format("%+.0f%%", val * 100);
                        } else {
                            // integer check
                            if (val == (long) val) {
                                valStr = String.format("%+d", (long) val);
                            } else {
                                valStr = String.format("%+.1f", val);
                            }
                        }

                        sb.append(" ").append(target).append(": ").append(valStr).append("\n");
                    }

                    tooltipList.add(sb.toString());
                };

                // Add to main lists in order
                activeList.forEach(addEntry);
                unlockedList.forEach(addEntry);
                lockedList.forEach(addEntry);

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
