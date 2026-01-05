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
        return getBestUpgradeTarget(town, scores, idleTicks);
    }

    /**
     * Identifies the best upgrade target for the town based on priorities.
     * Used by both the AI (to start research) and the Want System (to identify
     * resource needs).
     */
    public static String getBestUpgradeTarget(Town town) {
        // Calculate fresh scores
        Map<String, Float> scores = calculatePriorities(town);
        // Assume 0 idle ticks for "current status" check - we want the absolute best
        // option right now
        // or should we check patience?
        // For "Wants", we probably want the thing the town *would* pick if it had the
        // resources.
        // So we should ignore patience thresholds that might mask high-cost items?
        // Actually, selectBestResearch logic filters OUT items below threshold.
        // But for WANTED resources, we specifically want items we CAN'T afford yet.
        // So let's just pick the highest score that isn't maxed.

        String bestNode = null;
        float maxScore = -1.0f;

        for (Map.Entry<String, Float> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestNode = entry.getKey();
            }
        }
        return bestNode;
    }

    public static String getBestUpgradeTarget(Town town, Map<String, Float> scores, long idleTicks) {
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
            } else if (target.equals("research")) {
                // Research Speed: Priority based on AVERAGE of TOP 50% Priority Upgrades (Time)
                // User Request: "make it the top half of the research priorities"
                // We calculate the priority of everything else, take the top half, and average
                // *their* research times.

                List<Map.Entry<Double, Double>> weightedTimes = new ArrayList<>(); // Key: Score, Value: ScaledTime

                for (UpgradeNode potential : UpgradeRegistry.getAll()) {
                    // 1. Ignore Research Speed upgrades to avoid recursion
                    boolean isResearchUpgrade = false;
                    for (Effect e : potential.getEffects()) {
                        if ("research".equals(e.getTarget())) {
                            isResearchUpgrade = true;
                            break;
                        }
                    }
                    if (isResearchUpgrade)
                        continue;

                    // 2. Check availability (Prereqs met, Not Maxed)
                    // We only "want" things we can actually research.
                    int currentLevel = town.getUpgradeLevel(potential.getId());
                    boolean isMaxed = false;
                    if (!potential.isRepeatable()) {
                        if (currentLevel >= 1)
                            isMaxed = true;
                    } else {
                        if (potential.getMaxRepeats() != -1 && currentLevel >= potential.getMaxRepeats())
                            isMaxed = true;
                    }

                    if (isMaxed)
                        continue;

                    // Check prereqs
                    boolean prereqsMet = true;
                    if (potential.getPrereqNodes() != null) {
                        for (String pre : potential.getPrereqNodes()) {
                            if (!town.isUnlocked(pre)) {
                                prereqsMet = false;
                                break;
                            }
                        }
                    }
                    if (!prereqsMet)
                        continue;

                    // 3. Calculate Priority for this upgrade
                    double pScore = calculateScore(town, potential);

                    // 4. Calculate Time for NEXT level
                    float baseMins = potential.getResearchMinutes();
                    double scaledMins = baseMins;
                    if (baseMins > 0) {
                        if (potential.isRepeatable() && currentLevel > 0) {
                            scaledMins *= Math.pow(potential.getCostMultiplier(), currentLevel);
                        }
                        weightedTimes.add(new AbstractMap.SimpleEntry<>(pScore, scaledMins));
                    }
                }

                // 5. Sort by Priority (Score) Descending
                weightedTimes.sort((e1, e2) -> Double.compare(e2.getKey(), e1.getKey()));

                // 6. Take Top 50%
                if (!weightedTimes.isEmpty()) {
                    int countToTake = (int) Math.ceil(weightedTimes.size() / 2.0);
                    double sumTime = 0;

                    for (int i = 0; i < countToTake; i++) {
                        sumTime += weightedTimes.get(i).getValue();
                    }

                    score += sumTime / countToTake;
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
                    } else if (output.resourceId.equals("border")) {
                        priority = calculateBorderPriority(town);
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
            } else if (target.equals("border")) {
                score += calculateBorderPriority(town);
            }
        }

        return Math.max(0.0, score);
    }

    private static double calculateBorderPriority(ITownState townState) {
        // Border Expansion:
        // Logic based on Density vs Baseline Density
        double priority = 0.0;
        if (townState instanceof com.quackers29.businesscraft.town.Town t) {
            String biomeId = t.getBiome();
            String variant = t.getBiomeVariant();
            var kit = com.quackers29.businesscraft.world.BiomeRegistry.getSpecificKit(biomeId, variant);

            float startPop = com.quackers29.businesscraft.config.ConfigLoader.defaultStartingPopulation;
            float startBorder = 10f;
            if (kit != null && kit.startingValues != null) {
                if (kit.startingValues.containsKey("pop")) {
                    startPop = kit.startingValues.get("pop");
                }
                startBorder = kit.startingValues.getOrDefault("border", 10f);
            }
            // Avoid div by zero
            if (startBorder < 1)
                startBorder = 1;

            // Area Density = Pop / (R * R)
            // (Pi cancels out in ratio)
            double baselineDensity = startPop / (startBorder * startBorder);

            float currentPop = townState.getStock("pop");
            float currentBorder = townState.getBoundaryRadius();
            if (currentBorder < 1)
                currentBorder = 1;

            double currentDensity = currentPop / (currentBorder * currentBorder);

            // Ratio > 1 means more crowded than baseline
            double crowding = currentDensity / baselineDensity;

            // User requested: "want of a town border that is still at its original starter
            // stats ... should be 0"
            // So if Crowding == 1.0, Priority == 0.
            // "as the pop increases the border want will go up but in proportion of the
            // area increase"

            if (crowding <= 1.0) {
                priority = 0.0;
            } else {
                // Crowding > 1.0
                // If Pop Doubles (Crowding 2.0), we want High Priority (e.g. 100).
                priority = (crowding - 1.0) * 100.0;
            }
        } else {
            // Fallback
            priority = 0.0;
        }
        return Math.min(100.0, Math.max(0.0, priority));
    }
}
