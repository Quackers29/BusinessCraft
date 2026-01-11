package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.production.ProductionRecipe;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;

import java.util.HashMap;
import java.util.Map;

/**
 * SERVER-SIDE ONLY class that builds ProductionStatusViewModel objects.
 *
 * This class contains ALL the business logic for:
 * - Loading production recipes from config files
 * - Evaluating production formulas
 * - Calculating production rates and cycle times
 * - Determining production status and requirements
 *
 * The client NEVER uses this class - it only receives the resulting view-model.
 * This implements the server-authoritative "View-Model" pattern.
 */
public class ProductionStatusViewModelBuilder {

    /**
     * Builds a complete production status view-model for the client.
     * Contains ALL calculations and formatting logic - client performs ZERO operations.
     *
     * @param town The town to generate view-model for
     * @return Complete view-model ready for client display
     */
    public static ProductionStatusViewModel buildProductionViewModel(Town town) {
        if (town == null) {
            return createEmptyViewModel();
        }

        Map<String, ProductionStatusViewModel.ProductionRecipeInfo> productionInfo = new HashMap<>();

        // Get all registered production recipes (SERVER-SIDE CONFIG ACCESS)
        Map<String, Float> townProgress = town.getProduction().getActiveRecipes();
        int activeCount = 0;
        int stalledCount = 0;

        // Process each production recipe from the server's config
        for (ProductionRecipe recipe : ProductionRegistry.getAll()) {
            if (recipe == null) continue;

            String recipeId = recipe.getId();

            // SERVER-SIDE CALCULATIONS (client never sees these)
            float progress = townProgress.getOrDefault(recipeId, 0f);
            float cycleTime = recipe.getBaseCycleTimeMinutes() / (24f * 60f); // Convert minutes to days

            // Calculate if recipe is active and has requirements met
            boolean hasRequirements = checkRequirementsMet(town, recipe);
            boolean isActive = progress > 0 && hasRequirements;

            if (isActive) {
                activeCount++;
            } else if (!hasRequirements && progress > 0) {
                stalledCount++;
            }

            // FORMAT ALL VALUES AS DISPLAY STRINGS (client receives these ready-to-display)
            String displayName = recipe.getDisplayName(); // From server CSV
            String statusText = formatStatusText(isActive, hasRequirements, progress);
            String progressText = formatProgressText(progress, cycleTime);
            String cycleTimeText = formatCycleTime(cycleTime);
            String inputsText = formatResourceAmounts(recipe.getInputs());
            String outputsText = formatResourceAmounts(recipe.getOutputs());
            String productionRateText = formatProductionRate(town, recipe, cycleTime);
            float progressPercentage = calculateProgressPercentage(progress, cycleTime);

            // Create display info (all calculations complete)
            ProductionStatusViewModel.ProductionRecipeInfo info =
                new ProductionStatusViewModel.ProductionRecipeInfo(
                    recipeId, displayName, statusText, progressText, cycleTimeText,
                    inputsText, outputsText, productionRateText, isActive,
                    hasRequirements, progressPercentage
                );

            productionInfo.put(recipeId, info);
        }

        // Calculate overall production status (SERVER-SIDE BUSINESS LOGIC)
        String totalActiveProductions = formatTotalProductions(activeCount);
        String overallStatus = calculateOverallStatus(activeCount, stalledCount);
        String economicOutput = calculateEconomicOutput(town);

        return new ProductionStatusViewModel(productionInfo, totalActiveProductions,
                                            overallStatus, economicOutput);
    }

    // ===== PRIVATE HELPER METHODS (SERVER-SIDE CALCULATIONS) =====

    private static ProductionStatusViewModel createEmptyViewModel() {
        return new ProductionStatusViewModel(
            new HashMap<>(),
            "No Productions",
            "Inactive",
            "No Output"
        );
    }

    private static boolean checkRequirementsMet(Town town, ProductionRecipe recipe) {
        // Simplified check - actual condition evaluation is complex and handled by TownProductionComponent
        // For the view-model, we provide a basic assessment
        // If there are conditions, assume they need to be checked (the town production component handles this)

        // For now, return true as a conservative estimate
        // The actual production component will handle detailed requirement checking
        // This view-model is primarily for display purposes

        return true; // Simplified for view-model display
    }

    private static String formatStatusText(boolean isActive, boolean hasRequirements, float progress) {
        if (isActive) {
            return "Active";
        } else if (progress > 0 && !hasRequirements) {
            return "Resource Shortage";
        } else if (!hasRequirements) {
            return "Requirements Not Met";
        } else {
            return "Ready";
        }
    }

    private static String formatProgressText(float progress, float cycleTime) {
        if (progress <= 0) {
            return "Not Started";
        }

        float percentage = (progress / cycleTime) * 100f;
        if (percentage >= 100f) {
            return "Completed";
        }

        return String.format("%.0f%% Complete", percentage);
    }

    private static String formatCycleTime(float cycleTimeDays) {
        if (cycleTimeDays < 1.0f) {
            float hours = cycleTimeDays * 24f;
            return String.format("%.1f hours/cycle", hours);
        } else if (cycleTimeDays == 1.0f) {
            return "1 day/cycle";
        } else {
            return String.format("%.1f days/cycle", cycleTimeDays);
        }
    }

    private static String formatResourceAmounts(java.util.List<ResourceAmount> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return "None";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < amounts.size(); i++) {
            ResourceAmount amount = amounts.get(i);
            if (i > 0) {
                sb.append(", ");
            }

            String resourceName = formatResourceName(amount.resourceId);
            String amountStr = formatAmount(amount.amount);

            sb.append(resourceName).append(": ").append(amountStr);
        }

        return sb.toString();
    }

    private static String formatResourceName(String resourceId) {
        // Convert "iron_ingot" to "Iron Ingot"
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
        if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000.0f);
        } else if (amount == (int) amount) {
            return String.valueOf((int) amount);
        } else {
            return String.format("%.1f", amount);
        }
    }

    private static String formatProductionRate(Town town, ProductionRecipe recipe, float cycleTime) {
        if (cycleTime <= 0 || recipe.getOutputs().isEmpty()) {
            return "No Output";
        }

        // Calculate production rate per hour
        // This is server-side calculation using the actual game configuration
        float cyclesPerDay = 1.0f / cycleTime;
        float cyclesPerHour = cyclesPerDay / 24.0f; // Approximation

        // Get first output as representative rate
        ResourceAmount firstOutput = recipe.getOutputs().get(0);
        float outputPerCycle = firstOutput.amount;
        float outputPerHour = outputPerCycle * cyclesPerHour;

        return String.format("+%.1f/hr", outputPerHour);
    }

    private static float calculateProgressPercentage(float progress, float cycleTime) {
        if (cycleTime <= 0) {
            return 0f;
        }

        float percentage = progress / cycleTime;
        return Math.min(Math.max(percentage, 0f), 1f); // Clamp to [0, 1]
    }

    private static String formatTotalProductions(int activeCount) {
        if (activeCount == 0) {
            return "No Active Productions";
        } else if (activeCount == 1) {
            return "1 Active Production";
        } else {
            return activeCount + " Active Productions";
        }
    }

    private static String calculateOverallStatus(int activeCount, int stalledCount) {
        if (activeCount == 0 && stalledCount == 0) {
            return "Inactive";
        } else if (stalledCount > 0) {
            return stalledCount + " Stalled";
        } else {
            return "All Running";
        }
    }

    private static String calculateEconomicOutput(Town town) {
        // This could be enhanced with actual output rate calculations
        // For now, provide a basic assessment based on active productions

        int activeCount = 0;
        Map<String, Float> progress = town.getProduction().getActiveRecipes();

        for (String recipeId : progress.keySet()) {
            if (progress.get(recipeId) > 0) {
                activeCount++;
            }
        }

        if (activeCount == 0) {
            return "No Output";
        } else if (activeCount < 3) {
            return "Low Output";
        } else if (activeCount < 6) {
            return "Medium Output";
        } else {
            return "High Output";
        }
    }
}