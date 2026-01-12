package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.data.parsers.Effect;
import com.quackers29.businesscraft.production.UpgradeNode;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.components.TownUpgradeComponent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SERVER-SIDE ONLY class that builds UpgradeStatusViewModel objects.
 *
 * This class contains ALL the business logic for:
 * - Loading upgrade definitions from config files
 * - Calculating upgrade costs with level scaling
 * - Calculating research times with research speed modifiers
 * - Determining upgrade status and prerequisites
 * - Formatting all display strings for the client
 *
 * The client NEVER uses this class - it only receives the resulting view-model.
 * This implements the server-authoritative "View-Model" pattern.
 */
public class UpgradeStatusViewModelBuilder {

    /**
     * Builds a complete upgrade status view-model for the client.
     * Contains ALL calculations and formatting logic - client performs ZERO operations.
     *
     * @param town The town to generate view-model for
     * @return Complete view-model ready for client display
     */
    public static UpgradeStatusViewModel buildUpgradeViewModel(Town town) {
        if (town == null) {
            return createEmptyViewModel();
        }

        TownUpgradeComponent upgrades = town.getUpgrades();
        Map<String, UpgradeStatusViewModel.UpgradeDisplayInfo> upgradeInfo = new HashMap<>();
        
        List<String> unlockedIds = new ArrayList<>();
        List<String> researchableIds = new ArrayList<>();
        List<String> lockedIds = new ArrayList<>();

        String currentResearchId = upgrades.getCurrentResearchNode();
        float currentResearchProgress = upgrades.getResearchProgress();
        Set<String> unlockedNodes = upgrades.getUnlockedNodes();
        Map<String, Float> aiScores = upgrades.getAiScores();

        // SERVER-SIDE CALCULATION: Research speed multiplier
        float researchSpeed = calculateResearchSpeed(town, upgrades);

        // Process each upgrade node from the server's config (SERVER-SIDE CONFIG ACCESS)
        for (UpgradeNode node : UpgradeRegistry.getAll()) {
            if (node == null) continue;

            String nodeId = node.getId();
            int currentLevel = upgrades.getUpgradeLevel(nodeId);
            boolean isUnlocked = currentLevel > 0;
            
            // SERVER-SIDE: Determine if upgrade is maxed
            boolean isMaxed = false;
            if (!node.isRepeatable()) {
                isMaxed = (currentLevel >= 1);
            } else {
                if (node.getMaxRepeats() != -1 && currentLevel >= node.getMaxRepeats()) {
                    isMaxed = true;
                }
            }

            // SERVER-SIDE: Check prerequisites
            boolean prerequisitesMet = checkPrerequisites(node, unlockedNodes);

            // SERVER-SIDE: Check if can afford
            boolean canAfford = checkCanAfford(town, upgrades, nodeId);

            // Determine status
            boolean isCurrentResearch = nodeId.equals(currentResearchId);
            boolean canResearch = !isMaxed && prerequisitesMet && canAfford && !isCurrentResearch;

            // SERVER-SIDE: Calculate progress percentage
            float progressPercentage = 0f;
            if (isCurrentResearch) {
                float scaledResearchTime = upgrades.getScaledResearchMinutes(nodeId);
                if (scaledResearchTime > 0) {
                    progressPercentage = Math.min(currentResearchProgress / scaledResearchTime, 1.0f);
                }
            }

            // SERVER-SIDE: Format ALL display strings
            String displayName = node.getDisplayName(); // From server CSV
            String statusText = formatStatusText(isUnlocked, isMaxed, isCurrentResearch, prerequisitesMet, canAfford);
            String progressText = formatProgressText(isCurrentResearch, progressPercentage);
            
            // SERVER-SIDE: Calculate research times with multipliers
            float baseResearchMinutes = node.getResearchMinutes();
            float scaledResearchMinutes = upgrades.getScaledResearchMinutes(nodeId);
            float activeResearchMinutes = scaledResearchMinutes / researchSpeed;
            
            String researchTimeText = formatResearchTime(activeResearchMinutes);
            String baseResearchTimeText = formatBaseResearchTime(baseResearchMinutes, scaledResearchMinutes, researchSpeed);
            
            // SERVER-SIDE: Format costs with level scaling
            String costsText = formatCosts(upgrades, nodeId);
            String requirementsText = formatRequirements(upgrades, nodeId);
            
            // SERVER-SIDE: Format effects
            String effectsText = formatEffects(node, currentLevel + 1); // Show effects for next level
            
            // SERVER-SIDE: Format prerequisites
            String prerequisitesText = formatPrerequisites(node);
            
            // Get AI score
            float aiScore = aiScores.getOrDefault(nodeId, 0f);

            // Create display info (all calculations complete)
            UpgradeStatusViewModel.UpgradeDisplayInfo info =
                new UpgradeStatusViewModel.UpgradeDisplayInfo(
                    nodeId, displayName, node.getCategory(), node.getDescription(),
                    currentLevel, node.getMaxRepeats(), node.isRepeatable(), isUnlocked,
                    isMaxed, canResearch, isCurrentResearch, statusText, progressText,
                    researchTimeText, baseResearchTimeText, costsText, requirementsText,
                    effectsText, prerequisitesText, aiScore, progressPercentage
                );

            upgradeInfo.put(nodeId, info);

            // Categorize for UI lists
            // Always add to upgradeInfo map, but categorize based on current state
            if (isUnlocked) {
                // Add to unlocked list (UI will show all levels 1 to currentLevel)
                unlockedIds.add(nodeId);
            }
            
            // If not maxed, also show the next level as researchable or locked
            if (!isMaxed) {
                if (prerequisitesMet) {
                    researchableIds.add(nodeId);
                } else {
                    lockedIds.add(nodeId);
                }
            }
        }

        // Sort researchable by AI score (descending)
        researchableIds.sort((a, b) -> {
            float scoreA = aiScores.getOrDefault(a, 0f);
            float scoreB = aiScores.getOrDefault(b, 0f);
            return Float.compare(scoreB, scoreA);
        });

        // SERVER-SIDE: Calculate overall progress percentage for current research
        float globalProgressPercentage = 0f;
        if (currentResearchId != null) {
            float scaledResearchTime = upgrades.getScaledResearchMinutes(currentResearchId);
            if (scaledResearchTime > 0) {
                globalProgressPercentage = Math.min(currentResearchProgress / scaledResearchTime, 1.0f);
            }
        }

        // SERVER-SIDE: Calculate overall summaries
        String totalUnlockedUpgrades = formatTotalUnlocked(unlockedIds.size());
        String currentResearchStatus = formatCurrentResearch(currentResearchId, upgradeInfo, globalProgressPercentage);
        String researchSpeedText = formatResearchSpeedText(researchSpeed);
        String researchSpeedTooltip = formatResearchSpeedTooltip(town, upgrades);

        return new UpgradeStatusViewModel(upgradeInfo, unlockedIds, researchableIds, lockedIds,
                                         totalUnlockedUpgrades, currentResearchStatus,
                                         researchSpeedText, researchSpeedTooltip, researchSpeed);
    }

    // ===== PRIVATE HELPER METHODS (SERVER-SIDE CALCULATIONS) =====

    private static UpgradeStatusViewModel createEmptyViewModel() {
        return new UpgradeStatusViewModel(
            new HashMap<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            "No Upgrades",
            "Idle - No Research",
            "Research Speed: 100%",
            "Base Speed: 100%",
            1.0f
        );
    }

    /**
     * SERVER-SIDE: Calculates the current research speed multiplier based on unlocked upgrades.
     * This replaces the client-side calculation in TownDataCacheManager.getCachedResearchSpeed()
     */
    private static float calculateResearchSpeed(Town town, TownUpgradeComponent upgrades) {
        float speed = 1.0f;
        Set<String> unlockedNodes = upgrades.getUnlockedNodes();
        
        if (unlockedNodes != null) {
            for (String uid : unlockedNodes) {
                UpgradeNode unode = UpgradeRegistry.get(uid);
                if (unode != null) {
                    int ulvl = upgrades.getUpgradeLevel(uid);
                    for (Effect eff : unode.getEffects()) {
                        if ("research".equals(eff.getTarget())) {
                            speed += unode.calculateEffectValue(eff, ulvl);
                        }
                    }
                }
            }
        }
        
        return Math.max(0.1f, speed);
    }

    /**
     * SERVER-SIDE: Formats research speed tooltip with breakdown of all contributing upgrades.
     * This replaces the client-side calculation in TownDataCacheManager.getResearchSpeedTooltip()
     */
    private static String formatResearchSpeedTooltip(Town town, TownUpgradeComponent upgrades) {
        StringBuilder sb = new StringBuilder();
        sb.append("Base Speed: 100%");
        
        Set<String> unlockedNodes = upgrades.getUnlockedNodes();
        if (unlockedNodes != null) {
            for (String uid : unlockedNodes) {
                UpgradeNode unode = UpgradeRegistry.get(uid);
                if (unode != null) {
                    int ulvl = upgrades.getUpgradeLevel(uid);
                    for (Effect eff : unode.getEffects()) {
                        if ("research".equals(eff.getTarget())) {
                            float val = unode.calculateEffectValue(eff, ulvl);
                            if (val != 0) {
                                sb.append("\n");
                                String name = unode.getDisplayName();
                                if (unode.isRepeatable()) {
                                    name += " (" + ulvl + ")";
                                }
                                sb.append(name).append(": ").append(String.format("%+.0f%%", val * 100));
                            }
                        }
                    }
                }
            }
        }
        
        return sb.toString();
    }

    private static boolean checkPrerequisites(UpgradeNode node, Set<String> unlockedNodes) {
        if (node.getPrereqNodes() == null || node.getPrereqNodes().isEmpty()) {
            return true;
        }

        for (String prereqId : node.getPrereqNodes()) {
            if (!unlockedNodes.contains(prereqId)) {
                return false;
            }
        }

        return true;
    }

    private static boolean checkCanAfford(Town town, TownUpgradeComponent upgrades, String nodeId) {
        List<ResourceAmount> costs = upgrades.getUpgradeCost(nodeId);
        
        for (ResourceAmount cost : costs) {
            float stock = town.getTrading().getStock(cost.resourceId);
            if (stock < cost.amount) {
                return false;
            }
        }
        
        return true;
    }

    private static String formatStatusText(boolean isUnlocked, boolean isMaxed, boolean isCurrentResearch,
                                          boolean prerequisitesMet, boolean canAfford) {
        if (isCurrentResearch) {
            return "Researching...";
        } else if (isMaxed) {
            return "Maxed";
        } else if (isUnlocked) {
            return "Unlocked";
        } else if (!prerequisitesMet) {
            return "Prerequisites Missing";
        } else if (!canAfford) {
            return "Cannot Afford";
        } else {
            return "Available";
        }
    }

    private static String formatProgressText(boolean isCurrentResearch, float progressPercentage) {
        if (isCurrentResearch) {
            return String.format("%.0f%% Complete", progressPercentage * 100);
        } else {
            return "Not Started";
        }
    }

    private static String formatResearchTime(float minutes) {
        if (minutes <= 0) {
            return "Instant";
        } else if (minutes < 1.0f) {
            return String.format("%.1f seconds", minutes * 60);
        } else {
            return String.format("%.1f minutes", minutes);
        }
    }

    private static String formatBaseResearchTime(float baseMinutes, float scaledMinutes, float researchSpeed) {
        if (baseMinutes <= 0) {
            return "";
        }

        // Show base time in parentheses if different from active time
        if (Math.abs(scaledMinutes - baseMinutes) > 0.01f || Math.abs(researchSpeed - 1.0f) > 0.01f) {
            return String.format("(Base: %.1fm, Scaled: %.1fm)", baseMinutes, scaledMinutes);
        }

        return "";
    }

    /**
     * SERVER-SIDE: Formats upgrade costs with level-based scaling.
     * Separates actual costs from requirements.
     */
    private static String formatCosts(TownUpgradeComponent upgrades, String nodeId) {
        List<ResourceAmount> costs = upgrades.getUpgradeCost(nodeId);
        
        List<String> costStrings = new ArrayList<>();
        for (ResourceAmount cost : costs) {
            // Skip requirements (tourism stats, pop, etc.)
            if (cost.resourceId.startsWith("tourism_") || cost.resourceId.equals("pop")) {
                continue;
            }
            
            String resourceName = formatResourceName(cost.resourceId);
            String amountStr = formatAmount(cost.amount);
            costStrings.add(resourceName + ": " + amountStr);
        }
        
        if (costStrings.isEmpty()) {
            return "None";
        }
        
        return String.join(", ", costStrings);
    }

    /**
     * SERVER-SIDE: Formats upgrade requirements (non-consumable, like tourism stats).
     */
    private static String formatRequirements(TownUpgradeComponent upgrades, String nodeId) {
        List<ResourceAmount> costs = upgrades.getUpgradeCost(nodeId);
        
        List<String> reqStrings = new ArrayList<>();
        for (ResourceAmount cost : costs) {
            // Only include requirements (tourism stats, pop, etc.)
            if (cost.resourceId.startsWith("tourism_") || cost.resourceId.equals("pop")) {
                String resourceName = formatResourceName(cost.resourceId);
                String amountStr = formatAmount(cost.amount);
                reqStrings.add(resourceName + ": " + amountStr);
            }
        }
        
        if (reqStrings.isEmpty()) {
            return "None";
        }
        
        return String.join(", ", reqStrings);
    }

    /**
     * SERVER-SIDE: Formats upgrade effects for display.
     */
    private static String formatEffects(UpgradeNode node, int level) {
        if (node.getEffects() == null || node.getEffects().isEmpty()) {
            return "None";
        }

        List<String> effectStrings = new ArrayList<>();
        for (Effect effect : node.getEffects()) {
            float value = node.calculateEffectValue(effect, level);
            String target = formatResourceName(effect.getTarget());
            
            String valueStr;
            if (effect.isPercentage()) {
                // Percentage-based effects (e.g., production speed multipliers)
                valueStr = String.format("%+.0f%%", value * 100);
            } else {
                // Flat value effects (e.g., storage capacity)
                if (value == (int) value) {
                    valueStr = String.format("%+d", (int) value);
                } else {
                    valueStr = String.format("%+.1f", value);
                }
            }
            
            effectStrings.add(target + ": " + valueStr);
        }
        
        return String.join(", ", effectStrings);
    }

    /**
     * SERVER-SIDE: Formats prerequisite nodes for display.
     */
    private static String formatPrerequisites(UpgradeNode node) {
        if (node.getPrereqNodes() == null || node.getPrereqNodes().isEmpty()) {
            return "None";
        }

        List<String> prereqNames = new ArrayList<>();
        for (String prereqId : node.getPrereqNodes()) {
            UpgradeNode prereqNode = UpgradeRegistry.get(prereqId);
            if (prereqNode != null) {
                prereqNames.add(prereqNode.getDisplayName());
            } else {
                prereqNames.add(prereqId);
            }
        }

        return String.join(", ", prereqNames);
    }

    private static String formatResourceName(String resourceId) {
        // Convert "iron_ingot" to "Iron Ingot"
        // Convert "tourism_count" to "Tourism Count"
        String[] parts = resourceId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(part.substring(0, 1).toUpperCase())
              .append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String formatAmount(float amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0f);
        } else if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000.0f);
        } else if (amount == (int) amount) {
            return String.valueOf((int) amount);
        } else {
            return String.format("%.1f", amount);
        }
    }

    private static String formatTotalUnlocked(int count) {
        if (count == 0) {
            return "No Upgrades Unlocked";
        } else if (count == 1) {
            return "1 Upgrade Unlocked";
        } else {
            return count + " Upgrades Unlocked";
        }
    }

    private static String formatCurrentResearch(String currentResearchId, 
                                               Map<String, UpgradeStatusViewModel.UpgradeDisplayInfo> upgradeInfo,
                                               float progressPercentage) {
        if (currentResearchId == null || currentResearchId.isEmpty()) {
            return "Idle - Select Research";
        }

        UpgradeStatusViewModel.UpgradeDisplayInfo info = upgradeInfo.get(currentResearchId);
        if (info != null) {
            return String.format("Researching: %s (%.0f%%)", info.getDisplayName(), progressPercentage * 100);
        }

        return "Researching: " + currentResearchId;
    }

    private static String formatResearchSpeedText(float researchSpeed) {
        return String.format("Research Speed: %.0f%%", researchSpeed * 100);
    }
}
