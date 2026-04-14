package com.quackers29.businesscraft.town.viewmodel;
import com.quackers29.businesscraft.town.viewmodel.IViewModel;

import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * View-Model for production recipe display on the client side.
 * Contains ONLY pre-calculated display data from the server.
 * Client performs ZERO calculations or config file reads.
 *
 * This implements the "dumb terminal" pattern where:
 * - Server calculates all production rates and formats display strings
 * - Client receives ready-to-display data with recipe names from server config
 * - No production recipe registry or formula access on client
 */
public class ProductionStatusViewModel implements IViewModel {

    /**
     * Display information for a single production recipe
     */
    public static class ProductionRecipeInfo {
        private final String recipeId;              // e.g., "iron_production"
        private final String displayName;           // e.g., "Iron Production" (from server CSV)
        private final String statusText;            // e.g., "Active", "Paused", "No Resources"
        private final String progressText;          // e.g., "45% Complete", "Ready"
        private final String cycleTimeText;         // e.g., "2.5 days/cycle"
        private final String inputsText;            // e.g., "Coal: 10, Iron Ore: 5"
        private final String outputsText;           // e.g., "Iron: 3"
        private final String productionRateText;    // e.g., "+12/hour"
        private final boolean isActive;             // true if currently producing
        private final boolean hasRequirements;      // true if requirements met
        private final float progressPercentage;     // 0.0 to 1.0 (for progress bars)

        public ProductionRecipeInfo(String recipeId, String displayName, String statusText,
                                   String progressText, String cycleTimeText, String inputsText,
                                   String outputsText, String productionRateText, boolean isActive,
                                   boolean hasRequirements, float progressPercentage) {
            this.recipeId = recipeId;
            this.displayName = displayName;
            this.statusText = statusText;
            this.progressText = progressText;
            this.cycleTimeText = cycleTimeText;
            this.inputsText = inputsText;
            this.outputsText = outputsText;
            this.productionRateText = productionRateText;
            this.isActive = isActive;
            this.hasRequirements = hasRequirements;
            this.progressPercentage = progressPercentage;
        }

        public ProductionRecipeInfo(FriendlyByteBuf buf) {
            this.recipeId = buf.readUtf();
            this.displayName = buf.readUtf();
            this.statusText = buf.readUtf();
            this.progressText = buf.readUtf();
            this.cycleTimeText = buf.readUtf();
            this.inputsText = buf.readUtf();
            this.outputsText = buf.readUtf();
            this.productionRateText = buf.readUtf();
            this.isActive = buf.readBoolean();
            this.hasRequirements = buf.readBoolean();
            this.progressPercentage = buf.readFloat();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(recipeId);
            buf.writeUtf(displayName);
            buf.writeUtf(statusText);
            buf.writeUtf(progressText);
            buf.writeUtf(cycleTimeText);
            buf.writeUtf(inputsText);
            buf.writeUtf(outputsText);
            buf.writeUtf(productionRateText);
            buf.writeBoolean(isActive);
            buf.writeBoolean(hasRequirements);
            buf.writeFloat(progressPercentage);
        }

        // Getters for client display (NO CALCULATIONS)
        public String getRecipeId() { return recipeId; }
        public String getDisplayName() { return displayName; }
        public String getStatusText() { return statusText; }
        public String getProgressText() { return progressText; }
        public String getCycleTimeText() { return cycleTimeText; }
        public String getInputsText() { return inputsText; }
        public String getOutputsText() { return outputsText; }
        public String getProductionRateText() { return productionRateText; }
        public boolean isActive() { return isActive; }
        public boolean hasRequirements() { return hasRequirements; }
        public float getProgressPercentage() { return progressPercentage; }
    }

    // Map of recipe IDs to their display information
    private final Map<String, ProductionRecipeInfo> productionInfo;

    // Overall production summary (pre-calculated by server)
    private final String totalActiveProductions;    // e.g., "5 active productions"
    private final String overallStatus;             // e.g., "All Running", "2 Stalled"
    private final String economicOutput;            // e.g., "High Output", "Low Efficiency"

    public ProductionStatusViewModel(Map<String, ProductionRecipeInfo> productionInfo,
                                    String totalActiveProductions,
                                    String overallStatus,
                                    String economicOutput) {
        this.productionInfo = new HashMap<>(productionInfo);
        this.totalActiveProductions = totalActiveProductions;
        this.overallStatus = overallStatus;
        this.economicOutput = economicOutput;
    }

    public ProductionStatusViewModel(FriendlyByteBuf buf) {
        // Read production info
        int count = buf.readInt();
        this.productionInfo = new HashMap<>();

        for (int i = 0; i < count; i++) {
            ProductionRecipeInfo info = new ProductionRecipeInfo(buf);
            this.productionInfo.put(info.getRecipeId(), info);
        }

        // Read summary data
        this.totalActiveProductions = buf.readUtf();
        this.overallStatus = buf.readUtf();
        this.economicOutput = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Write production info
        buf.writeInt(productionInfo.size());

        productionInfo.values().forEach(info -> info.toBytes(buf));

        // Write summary data
        buf.writeUtf(totalActiveProductions);
        buf.writeUtf(overallStatus);
        buf.writeUtf(economicOutput);
    }

    // Client-side getters (NO CALCULATIONS)
    public Map<String, ProductionRecipeInfo> getProductionInfo() {
        return productionInfo;
    }

    public ProductionRecipeInfo getRecipeInfo(String recipeId) {
        return productionInfo.get(recipeId);
    }

    public String getTotalActiveProductions() {
        return totalActiveProductions;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public String getEconomicOutput() {
        return economicOutput;
    }

    public boolean hasRecipe(String recipeId) {
        return productionInfo.containsKey(recipeId);
    }

    public int getRecipeCount() {
        return productionInfo.size();
    }
}