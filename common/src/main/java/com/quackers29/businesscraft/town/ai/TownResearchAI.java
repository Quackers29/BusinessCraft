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

    /**
     * Calculates priorities for all relevant upgrades.
     * Returns a map of NodeID -> Score.
     * Includes all upgrades that are not maxed out.
     */
    public static Map<String, Float> calculatePriorities(Town town) {
        Map<String, Float> scores = new HashMap<>();
        Set<String> unlocked = town.getUpgrades().getUnlockedNodes();

        for (UpgradeNode node : UpgradeRegistry.getAll()) {
            // Check if maxed
            boolean isMaxed = false;
            int lvl = town.getUpgrades().getUpgradeLevel(node.getId());
            if (!node.isRepeatable()) {
                if (lvl >= 1)
                    isMaxed = true;
            } else {
                if (node.getMaxRepeats() != -1 && lvl >= node.getMaxRepeats())
                    isMaxed = true;
            }

            if (!isMaxed) {
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

                // We calculate score if prereqs met (or even if not, but prioritized low? No,
                // strict prereq tree).
                // Actually, UI shows "Locked" for next steps.
                // Let's only score nodes where prereqs are met (Available to research or
                // available soon).
                if (prereqsMet) {
                    double score = calculateScore(town, node);
                    scores.put(node.getId(), (float) score);
                }
            }
        }
        return scores;
    }

    public static String selectBestResearch(Town town, Map<String, Float> scores, long idleTicks) {
        // 1. Find the absolute best score (unaffordable included) to set the bar
        float maxPossibleScore = 0f;
        for (float s : scores.values()) {
            if (s > maxPossibleScore)
                maxPossibleScore = s;
        }

        // 2. Calculate Patience Threshold
        // We wait up to 1 day for resources to accumulate
        // Threshold decays from MaxScore down to 0 over 1 day.
        float waitProgress = (float) idleTicks
                / (float) com.quackers29.businesscraft.config.ConfigLoader.dailyTickInterval;
        if (waitProgress > 1.0f)
            waitProgress = 1.0f;

        float threshold = maxPossibleScore * (1.0f - waitProgress);

        String bestNode = null;
        double bestBiasedScore = -1.0;

        for (Map.Entry<String, Float> entry : scores.entrySet()) {
            String id = entry.getKey();
            float rawScore = entry.getValue();

            // Patience: Don't settle for low-score upgrades if we are "saving up"
            // But if the upgrade itself is High Score (>= threshold), we take it.
            if (rawScore < threshold)
                continue;

            if (town.getUpgrades().canAffordResearch(id)) {
                // Bias
                double biased = rawScore + (Math.random() * 2.0);
                if (biased > bestBiasedScore) {
                    bestBiasedScore = biased;
                    bestNode = id;
                }
            }
        }
        return bestNode;
    }

    public static double calculateScore(ITownState town, UpgradeNode node) {
        double score = 0.0;

        for (Effect effect : node.getEffects()) {
            String target = effect.getTarget();
            float value = effect.getValue();

            // 1. Capacity Upgrades (0 to 100 Priority)
            // 100% Full = 100 Priority
            // 0% Full = 0 Priority
            if (target.contains("_cap")) {
                if (target.equals("storage_cap_all")) {
                    double maxFullness = 0;
                    for (ResourceType type : ResourceRegistry.getAll()) {
                        float cap = town.getStorageCap(type.getId());
                        float current = town.getStock(type.getId());
                        if (cap > 0) {
                            double ratio = current / cap;
                            if (ratio > maxFullness)
                                maxFullness = ratio;
                        }
                    }
                    // Quadratic: 100 * ratio^2
                    score += 100.0 * (maxFullness * maxFullness);

                } else if (target.equals("pop_cap")) {
                    float cap = town.getStorageCap("pop");
                    float current = town.getStock("pop");
                    if (cap > 0) {
                        double ratio = current / cap;
                        score += 100.0 * (ratio * ratio);
                    }
                } else if (target.equals("tourist_cap")) {
                    float cap = town.getStorageCap("tourist");
                    float current = town.getStock("tourist");
                    if (cap > 0) {
                        double ratio = current / cap;
                        score += 100.0 * (ratio * ratio);
                    }
                } else if (target.startsWith("storage_cap_")) {
                    String resId = target.substring("storage_cap_".length());
                    float cap = town.getStorageCap(resId);
                    float current = town.getStock(resId);
                    if (cap > 0) {
                        double ratio = current / cap;
                        score += 100.0 * (ratio * ratio);
                    }
                }
            }

            // 2. Production Upgrades (Speed)
            // 0 Prod / >0 Cons = 100 Priority (Critical Deficit)
            // Prod == Cons = 50 Priority (Balanced)
            // Prod >= 2*Cons = 0 Priority (Surplus)
            ProductionRecipe recipe = ProductionRegistry.get(target);
            if (recipe != null) {
                double recipeScore = 0.0;
                boolean allFull = true;

                for (var output : recipe.getOutputs()) {
                    float cap = town.getStorageCap(output.resourceId);
                    float current = town.getStock(output.resourceId);
                    double fullness = (cap > 0) ? (current / cap) : 0;
                    if (fullness < 0.95) {
                        allFull = false;
                    }

                    float prod = town.getProductionRate(output.resourceId);
                    float cons = town.getConsumptionRate(output.resourceId);

                    double priority = 50.0; // Baseline

                    // Special Handling for "Accumulation" resources (Tourists, Population)
                    // These aren't consumed in a flow, but accumulated to a cap.
                    // We want to fill them as fast as possible -> Priority increases as we are
                    // emptier.
                    if (output.resourceId.equals("tourist") || output.resourceId.equals("pop")) {
                        // Score = 100 * (1.0 - Fullness)
                        if (cap > 0) {
                            priority = 100.0 * (1.0 - (current / cap));
                        } else {
                            priority = 50.0; // No cap? Balanced.
                        }
                    } else if (cons <= 0.0001f) {
                        // No consumption (and not an accumulation resource)
                        if (prod > 0) {
                            // Surplus -> 0 Priority
                            priority = 0.0;
                        } else {
                            // No prod, no cons -> Balanced -> 50 Priority
                            priority = 50.0;
                        }
                    } else {
                        // Has consumption
                        double ratio = prod / cons;
                        // Score = 50 * (2.0 - Ratio)
                        // Ratio 0 -> 100
                        // Ratio 1 -> 50
                        // Ratio 2 -> 0
                        priority = 50.0 * (2.0 - ratio);
                    }

                    // Clamp
                    if (priority < 0)
                        priority = 0;
                    if (priority > 100)
                        priority = 100;

                    recipeScore += priority;
                }

                // Average the score across outputs if multiple?
                // Or just sum? Previously we summed deficit scores.
                // But now we have a 0-100 scale per output.
                // If a recipe produces 2 items, and both are in deficit, is it 200 priority?
                // Probably yes. Speeding up a multi-output recipe is very valuable.

                if (allFull) {
                    // Deprioritize significantly if storage is full (redundant production)
                    score -= 50.0;
                } else {
                    score += recipeScore;
                }
            }
        }

        return Math.max(0.0, score);
    }
}
