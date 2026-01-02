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
        if (contentComponent != null) {
            contentComponent.refresh();
        }
    }

    @Override
    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTimestamp >= UPDATE_THROTTLE_MS) {
            lastUpdateTimestamp = now;
            if (contentComponent != null) {
                contentComponent.refresh();
            }
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
                // Production Only
                Map<String, Float> productions = cache.getCachedActiveProductions();
                if (productions != null) {
                    for (Map.Entry<String, Float> entry : productions.entrySet()) {
                        String id = entry.getKey();

                        // Get nice name if possible
                        com.quackers29.businesscraft.production.ProductionRecipe recipe = com.quackers29.businesscraft.production.ProductionRegistry
                                .get(id);
                        String displayName = (recipe != null) ? recipe.getDisplayName() : id;

                        names.add(displayName);
                        // Column 2: Percentage
                        int pct = (int) (entry.getValue() * 100);
                        if (pct > 100)
                            pct = 100;
                        progress.add(pct + "%");

                        // Tooltip for production
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
                    progress.add("-");
                    tooltipList.add(null);
                }
            } else {
                // Upgrades View (Research + Unlocked + Locked)
                List<UpgradeNode> activeList = new ArrayList<>();
                List<UpgradeNode> unlockedList = new ArrayList<>();
                List<UpgradeNode> lockedList = new ArrayList<>();

                java.util.Set<String> unlockedIds = cache.getCachedUnlockedNodes();
                String currentResearchId = cache.getCachedCurrentResearch();
                UpgradeNode currentResearchNode = (currentResearchId != null && !currentResearchId.isEmpty())
                        ? UpgradeRegistry.get(currentResearchId)
                        : null;

                if (currentResearchNode != null) {
                    activeList.add(currentResearchNode);
                }

                // Categorize nodes
                for (UpgradeNode node : UpgradeRegistry.getAll()) {
                    if (currentResearchNode != null && node.getId().equals(currentResearchNode.getId())) {
                        continue; // Already in active
                    }
                    if (unlockedIds != null && unlockedIds.contains(node.getId())) {
                        unlockedList.add(node);
                    } else {
                        lockedList.add(node);
                    }
                }

                // Sort Locked by AI Score
                ClientTownState clientState = new ClientTownState(cache);
                lockedList.sort((n1, n2) -> {
                    double s1 = com.quackers29.businesscraft.town.ai.TownResearchAI.calculateScore(clientState, n1);
                    double s2 = com.quackers29.businesscraft.town.ai.TownResearchAI.calculateScore(clientState, n2);
                    return Double.compare(s2, s1); // Descending
                });

                // Helper to add node to lists
                Consumer<UpgradeNode> addNode = (node) -> {
                    names.add(node.getDisplayName());

                    StringBuilder sb = new StringBuilder();
                    sb.append(node.getDisplayName()).append("\n");
                    sb.append(node.getDescription()).append("\n\n");

                    // Status specific info
                    if (activeList.contains(node)) {
                        float currentDays = cache.getCachedResearchProgress();
                        int pct = (node.getResearchDays() > 0) ? (int) ((currentDays / node.getResearchDays()) * 100)
                                : 0;
                        if (pct > 100)
                            pct = 100;
                        progress.add(pct + "%");
                        sb.append("Status: Researching...").append("\n");
                    } else if (unlockedList.contains(node)) {
                        progress.add("Unlocked");
                        sb.append("Status: Unlocked").append("\n");
                    } else {
                        progress.add("Locked");
                        sb.append("Status: Locked").append("\n");
                    }

                    // Requirements
                    if (node.getCosts() != null && !node.getCosts().isEmpty()) {
                        sb.append("\nCost:\n");
                        node.getCosts().forEach(resAmt -> {
                            sb.append(" - ").append(resAmt.resourceId).append(": ").append(resAmt.amount).append("\n");
                        });
                    }

                    // AI Score (Town Priority)
                    if (lockedList.contains(node)) {
                        double score = com.quackers29.businesscraft.town.ai.TownResearchAI.calculateScore(clientState,
                                node);
                        sb.append("\nTown Priority: ").append(String.format("%.1f", score));
                    }

                    sb.append("\n\nEffects:\n");
                    for (com.quackers29.businesscraft.data.parsers.Effect eff : node.getEffects()) {
                        sb.append(" ").append(eff.getTarget()).append(": ").append(eff.getValue()).append("\n");
                    }
                    tooltipList.add(sb.toString());
                };

                // Add to main lists in order
                activeList.forEach(addNode);
                unlockedList.forEach(addNode);
                lockedList.forEach(addNode);

                if (names.isEmpty()) {
                    names.add("No Upgrades");
                    progress.add("-");
                    tooltipList.add(null);
                }
            }

            // Return 2 arrays + tooltips (Names, Progress/Status, Tooltips)
            return new Object[] { names.toArray(new String[0]), progress.toArray(new String[0]),
                    tooltipList.toArray(new String[0]) };
        });

        panel.addChild(contentComponent);
    }

    // Client-side implementation of local town state for AI scoring
    private static class ClientTownState implements com.quackers29.businesscraft.town.ai.ITownState {
        private final TownDataCacheManager cache;

        public ClientTownState(TownDataCacheManager cache) {
            this.cache = cache;
        }

        @Override
        public float getStock(String resourceId) {
            // Best effort from cache or 0
            // resourceId is like "minecraft:wheat"
            try {
                net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(resourceId);
                java.util.Map<net.minecraft.world.item.Item, Integer> resources = cache.getCachedResources();
                if (resources != null) {
                    for (Map.Entry<net.minecraft.world.item.Item, Integer> entry : resources.entrySet()) {
                        // Simple string check is risky but item registry lookup is hard on client
                        // thread without direct access sometimes
                        // But we have ResourceLocation.
                        if (com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItemKey(entry.getKey())
                                .equals(loc)) {
                            return entry.getValue();
                        }
                    }
                }
            } catch (Exception e) {
            }
            return 0;
        }

        @Override
        public float getStorageCap(String resourceId) {
            // We can check resource stats from synced cache
            try {
                net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(resourceId);
                net.minecraft.world.item.Item item = (net.minecraft.world.item.Item) com.quackers29.businesscraft.api.PlatformAccess
                        .getRegistry().getItem(loc);
                float[] stats = cache.getResourceStats(item);
                if (stats != null && stats.length >= 3)
                    return stats[2];
            } catch (Exception e) {
            }
            return 0;
        }

        @Override
        public float getProductionRate(String resourceId) {
            try {
                net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(resourceId);
                net.minecraft.world.item.Item item = (net.minecraft.world.item.Item) com.quackers29.businesscraft.api.PlatformAccess
                        .getRegistry().getItem(loc);
                float[] stats = cache.getResourceStats(item);
                if (stats != null && stats.length >= 3)
                    return stats[0];
            } catch (Exception e) {
            }
            return 0;
        }

        @Override
        public float getConsumptionRate(String resourceId) {
            try {
                net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(resourceId);
                net.minecraft.world.item.Item item = (net.minecraft.world.item.Item) com.quackers29.businesscraft.api.PlatformAccess
                        .getRegistry().getItem(loc);
                float[] stats = cache.getResourceStats(item);
                if (stats != null && stats.length >= 3)
                    return stats[1];
            } catch (Exception e) {
            }
            return 0;
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
