package com.quackers29.businesscraft.town.ai;

import com.quackers29.businesscraft.data.parsers.Effect;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.production.ProductionRecipe;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeNode;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.town.Town;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TownResearchAI {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownResearchAI.class);

    public static String selectNextResearch(Town town) {
        Set<String> unlocked = town.getUpgrades().getUnlockedNodes();
        List<UpgradeNode> candidates = new ArrayList<>();

        // Find available upgrades
        // Find available upgrades
        for (UpgradeNode node : UpgradeRegistry.getAll()) {
            // Check prereqs
            boolean prereqsMet = true;
            if (node.getPrereqNodes() != null) {
                for (String pre : node.getPrereqNodes()) {
                    if (!unlocked.contains(pre)) {
                        prereqsMet = false;
                        break;
                    }
                }
            }

            if (prereqsMet && town.getUpgrades().canAffordResearch(node.getId())) {
                candidates.add(node);
            }
        }

        if (candidates.isEmpty()) {
            // Log occasionally or verbose
            // LOGGER.debug("AI: No candidates for town {}", town.getName());
            return null;
        }

        // Score candidates
        UpgradeNode bestNode = null;
        double bestScore = -1.0;

        for (UpgradeNode node : candidates) {
            double score = calculateScore(town, node);
            // Random bias to prevent getting stuck (only applied for selection, not UI
            // display)
            double biasedScore = score + (Math.random() * 2.0);

            LOGGER.debug("AI Candidate: {} Score: {} (Biased: {})", node.getId(), score, biasedScore);
            if (biasedScore > bestScore) {
                bestScore = biasedScore;
                bestNode = node;
            }
        }

        if (bestNode != null) {
            LOGGER.info("AI selected research: {} (Score: {}) for town {}", bestNode.getId(),
                    String.format("%.2f", bestScore), town.getName());
            return bestNode.getId();
        }

        return null;
    }

    public static double calculateScore(ITownState town, UpgradeNode node) {
        double score = 0.0;

        for (Effect effect : node.getEffects()) {
            String target = effect.getTarget();
            float value = effect.getValue();

            // 1. Storage Capacity Upgrades
            if (target.startsWith("storage_cap_")) {
                if (target.equals("storage_cap_all")) {
                    // Check average fullness across important resources
                    double avgFullness = 0;
                    int count = 0;
                    for (ResourceType type : ResourceRegistry.getAll()) {
                        float cap = town.getStorageCap(type.getId());
                        float current = town.getStock(type.getId());
                        if (cap > 0) {
                            avgFullness += (current / cap);
                            count++;
                        }
                    }
                    if (count > 0)
                        avgFullness /= count;

                    if (avgFullness > 0.8)
                        score += 10.0; // High priority if things are generally full
                    score += avgFullness * 5.0;
                } else {
                    // Specific resource cap
                    String resId = target.substring("storage_cap_".length());
                    float cap = town.getStorageCap(resId);
                    float current = town.getStock(resId);
                    if (cap > 0) {
                        double fullness = current / cap;
                        if (fullness > 0.9)
                            score += 20.0; // Critical priority
                        else if (fullness > 0.75)
                            score += 10.0;
                        score += fullness * 5.0;
                    }
                }
            }

            // 2. Production Upgrades (Speed)
            // Target is likely a production recipe ID
            ProductionRecipe recipe = ProductionRegistry.get(target);
            if (recipe != null) {
                // This upgrade improves/unlocks this recipe

                // Check if we need the outputs
                for (var output : recipe.getOutputs()) {
                    // Estimate deficit
                    float prod = town.getProductionRate(output.resourceId);
                    float cons = town.getConsumptionRate(output.resourceId);

                    if (cons > prod) {
                        // Deficit!
                        score += 15.0;
                        score += (cons - prod) * 2.0; // Proportional to deficit
                    } else {
                        // Surplus
                        score += 1.0; // Low priority
                    }

                    // Also check if low on stock
                    float cap = town.getStorageCap(output.resourceId);
                    float current = town.getStock(output.resourceId);
                    if (cap > 0 && (current / cap) < 0.2) {
                        score += 5.0; // Boost if low stock
                    }
                }
            }
        }

        return score;
    }
}
