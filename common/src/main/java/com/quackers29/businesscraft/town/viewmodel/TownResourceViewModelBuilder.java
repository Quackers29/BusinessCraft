package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * SERVER-SIDE ONLY class that builds TownResourceViewModel objects.
 * 
 * This class contains ALL the business logic for calculating resource
 * statistics,
 * production rates, capacity utilization, and status indicators.
 * 
 * The client NEVER uses this class - it only receives the resulting view-model.
 * This implements the server-authoritative "View-Model" pattern.
 */
public class TownResourceViewModelBuilder {

    /**
     * Builds a complete resource view-model for the client.
     * Contains ALL calculations and formatting logic - client performs ZERO math.
     * 
     * @param town The town to generate view-model for
     * @return Complete view-model ready for client display
     */
    public static TownResourceViewModel buildResourceViewModel(Town town) {
        if (town == null) {
            return createEmptyViewModel();
        }

        Map<Item, TownResourceViewModel.ResourceDisplayInfo> displayData = new HashMap<>();
        Map<Item, Integer> resources = town.getAllResources();

        // Calculate factor to convert daily rates to per-hour rates (SERVER-SIDE ONLY)
        // Rate/Day * (72000 ticks/hour / DailyTickInterval)
        float hourFactor = 72000f / (float) ConfigLoader.dailyTickInterval;

        int shortageCount = 0;
        int fullCapacityCount = 0;

        // FIRST: Process Work Units as a special resource (not in regular resources map)
        int workUnits = town.getWorkUnits();
        int workUnitCap = town.getWorkUnitCap();
        if (workUnits > 0 || workUnitCap > 0) {
            displayData.put(net.minecraft.world.item.Items.AIR, createWorkUnitsDisplayInfo(
                    workUnits, workUnitCap, hourFactor, town));
            // Note: Work units don't contribute to shortage/fullCapacity counts
        }

        // Process each regular resource (ALL CALCULATIONS HAPPEN HERE)
        for (Map.Entry<Item, Integer> entry : resources.entrySet()) {
            Item item = entry.getKey();
            int currentAmount = entry.getValue();

            ResourceType resourceType = ResourceRegistry.getFor(item);
            if (resourceType == null)
                continue;

            String resourceId = resourceType.getId();

            // SERVER-SIDE CALCULATIONS (client never sees these)
            float prodPerDay = town.getProduction().getProductionRate(resourceId);
            float consPerDay = town.getProduction().getConsumptionRate(resourceId);
            float prodPerHour = prodPerDay * hourFactor;
            float consPerHour = consPerDay * hourFactor;
            float capacity = town.getTrading().getStorageCap(resourceId);
            float inTransitAmount = town.getInTransitResourceCount(item);

            // FORMAT ALL VALUES AS DISPLAY STRINGS (client receives these ready-to-display)
            String displayName = getItemDisplayName(item);
            String currentAmountStr = formatAmount(currentAmount);
            String productionRateStr = formatProductionRate(prodPerHour);
            String consumptionRateStr = formatConsumptionRate(consPerHour);
            String capacityStr = formatCapacity(capacity);
            String capacityPercentageStr = formatCapacityPercentage(currentAmount, capacity);
            String inTransitStr = formatInTransit(inTransitAmount);

            // CALCULATE STATUS INDICATORS (server-side business logic)
            boolean isShortage = isResourceShortage(currentAmount, consPerHour, prodPerHour);
            boolean isCapacityFull = isCapacityFull(currentAmount, capacity);
            String statusIndicator = calculateStatusIndicator(prodPerHour, consPerHour, isShortage, isCapacityFull);

            // Track overall statistics
            if (isShortage)
                shortageCount++;
            if (isCapacityFull)
                fullCapacityCount++;

            // CALCULATE ACTIVE EFFECTS (server-side business logic)
            java.util.List<String> activeEffects = calculateActiveEffects(town, item, currentAmount, capacity);

            // Create display info (all calculations complete)
            TownResourceViewModel.ResourceDisplayInfo displayInfo = new TownResourceViewModel.ResourceDisplayInfo(
                    displayName, currentAmountStr, productionRateStr, consumptionRateStr,
                    capacityStr, capacityPercentageStr, inTransitStr, statusIndicator,
                    isShortage, isCapacityFull,
                    activeEffects);

            displayData.put(item, displayInfo);
        }

        // Calculate overall town status (SERVER-SIDE BUSINESS LOGIC)
        String totalResourcesDisplay = formatTotalResources(resources.size());
        String overallStatus = calculateOverallStatus(shortageCount, fullCapacityCount, resources.size());
        String economicTrend = calculateEconomicTrend(town);

        // FIX: Include overview stats in view-model to avoid ContainerData issues
        int population = town.getPopulation();
        int touristCount = town.getTouristCount();
        int maxTourists = town.getMaxTourists();

        return new TownResourceViewModel(displayData, totalResourcesDisplay, overallStatus, economicTrend,
                population, touristCount, maxTourists);
    }

    // ===== PRIVATE HELPER METHODS (SERVER-SIDE CALCULATIONS) =====

    private static TownResourceViewModel createEmptyViewModel() {
        return new TownResourceViewModel(
                new HashMap<>(),
                "No Resources",
                "No Data",
                "Unknown",
                0, // population
                0, // touristCount
                0  // maxTourists
        );
    }

    /**
     * Creates display info for Work Units (special resource not stored in regular resources map)
     */
    private static TownResourceViewModel.ResourceDisplayInfo createWorkUnitsDisplayInfo(
            int workUnits, int workUnitCap, float hourFactor, Town town) {
        
        // Work units are a special resource - get production/consumption rates
        float prodPerDay = town.getProduction().getProductionRate("wu");
        float consPerDay = town.getProduction().getConsumptionRate("wu");
        float prodPerHour = prodPerDay * hourFactor;
        float consPerHour = consPerDay * hourFactor;
        
        // Format display strings
        String displayName = "Work Units";
        String currentAmountStr = formatAmount(workUnits);
        String productionRateStr = formatProductionRate(prodPerHour);
        String consumptionRateStr = formatConsumptionRate(consPerHour);
        String capacityStr = formatCapacity(workUnitCap);
        String capacityPercentageStr = formatCapacityPercentage(workUnits, workUnitCap);
        String inTransitStr = ""; // Work units don't have in-transit
        
        // Calculate status
        boolean isShortage = isResourceShortage(workUnits, consPerHour, prodPerHour);
        boolean isCapacityFull = isCapacityFull(workUnits, workUnitCap);
        String statusIndicator = calculateStatusIndicator(prodPerHour, consPerHour, isShortage, isCapacityFull);
        
        // Work units typically don't have upgrade effects displayed separately
        java.util.List<String> activeEffects = new java.util.ArrayList<>();
        
        return new TownResourceViewModel.ResourceDisplayInfo(
                displayName, currentAmountStr, productionRateStr, consumptionRateStr,
                capacityStr, capacityPercentageStr, inTransitStr, statusIndicator,
                isShortage, isCapacityFull, activeEffects);
    }

    private static String getItemDisplayName(Item item) {
        // TODO: Could be enhanced with localization support
        return item.getDescriptionId(); // Fallback to basic name
    }

    private static String formatAmount(int amount) {
        if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000.0f);
        }
        return String.valueOf(amount);
    }

    private static String formatProductionRate(float prodPerHour) {
        if (prodPerHour <= 0) {
            return "No Production";
        }
        if (prodPerHour < 1.0f) {
            return String.format("+%.1f/hr", prodPerHour);
        }
        return String.format("+%.0f/hr", prodPerHour);
    }

    private static String formatConsumptionRate(float consPerHour) {
        if (consPerHour <= 0) {
            return "No Consumption";
        }
        if (consPerHour < 1.0f) {
            return String.format("-%.1f/hr", consPerHour);
        }
        return String.format("-%.0f/hr", consPerHour);
    }

    private static String formatCapacity(float capacity) {
        if (capacity <= 0 || capacity >= Float.MAX_VALUE / 2) {
            return "Unlimited";
        }
        if (capacity >= 1000) {
            return String.format("%.1fK", capacity / 1000.0f);
        }
        return String.format("%.0f", capacity);
    }

    private static String formatCapacityPercentage(int current, float capacity) {
        if (capacity <= 0 || capacity >= Float.MAX_VALUE / 2) {
            return "";
        }
        float percentage = (current / capacity) * 100f;
        if (percentage >= 100f) {
            return "Full";
        }
        return String.format("%.0f%%", percentage);
    }

    private static String formatInTransit(float inTransit) {
        if (inTransit <= 0) {
            return "";
        }
        if (inTransit < 1.0f) {
            return String.format("+%.1f Incoming", inTransit);
        }
        return String.format("+%.0f Incoming", inTransit);
    }

    private static boolean isResourceShortage(int current, float consumption, float production) {
        // Business logic: shortage if less than 1 hour of consumption remaining
        // and consumption exceeds production
        if (consumption <= 0)
            return false;

        float hoursRemaining = current / consumption;
        boolean consumptionExceedsProduction = consumption > production;

        return hoursRemaining < 1.0f && consumptionExceedsProduction;
    }

    private static boolean isCapacityFull(int current, float capacity) {
        if (capacity <= 0 || capacity >= Float.MAX_VALUE / 2)
            return false;

        float utilizationPercentage = (current / capacity) * 100f;
        return utilizationPercentage >= 90f; // Consider 90%+ as "full"
    }

    private static String calculateStatusIndicator(float production, float consumption,
            boolean isShortage, boolean isCapacityFull) {
        if (isShortage) {
            return "Shortage";
        }
        if (isCapacityFull) {
            return "Full";
        }

        float netFlow = production - consumption;
        if (netFlow > 1.0f) {
            return "Surplus";
        } else if (netFlow < -1.0f) {
            return "Declining";
        } else {
            return "Balanced";
        }
    }

    private static String formatTotalResources(int resourceCount) {
        if (resourceCount == 0) {
            return "No Resources";
        } else if (resourceCount == 1) {
            return "1 Resource Type";
        } else {
            return resourceCount + " Resource Types";
        }
    }

    private static String calculateOverallStatus(int shortageCount, int fullCapacityCount, int totalResources) {
        if (totalResources == 0) {
            return "No Economy";
        }

        float shortagePercentage = (float) shortageCount / totalResources;
        float fullPercentage = (float) fullCapacityCount / totalResources;

        if (shortagePercentage > 0.3f) {
            return "Resource Crisis";
        } else if (shortagePercentage > 0.1f) {
            return "Economic Stress";
        } else if (fullPercentage > 0.5f) {
            return "Storage Full";
        } else {
            return "Healthy Economy";
        }
    }

    private static String calculateEconomicTrend(Town town) {
        // This could be enhanced with historical data analysis
        // For now, provide a basic trend based on current production vs consumption

        Map<Item, Integer> resources = town.getAllResources();
        if (resources.isEmpty()) {
            return "Unknown";
        }

        int positiveTrend = 0;
        int negativeTrend = 0;

        for (Item item : resources.keySet()) {
            ResourceType resourceType = ResourceRegistry.getFor(item);
            if (resourceType == null)
                continue;

            String resourceId = resourceType.getId();
            float prodPerDay = town.getProduction().getProductionRate(resourceId);
            float consPerDay = town.getProduction().getConsumptionRate(resourceId);

            float netFlow = prodPerDay - consPerDay;
            if (netFlow > 0.5f) {
                positiveTrend++;
            } else if (netFlow < -0.5f) {
                negativeTrend++;
            }
        }

        if (positiveTrend > negativeTrend * 1.5) {
            return "Growing";
        } else if (negativeTrend > positiveTrend * 1.5) {
            return "Declining";
        } else {
            return "Stable";
        }
    }

    private static java.util.List<String> calculateActiveEffects(Town town, Item item, int currentAmount,
            float capacity) {
        java.util.List<String> effects = new java.util.ArrayList<>();

        java.util.Set<String> unlockedNodes = town.getUpgrades().getUnlockedNodes();
        if (unlockedNodes == null || unlockedNodes.isEmpty()) {
            return effects;
        }

        net.minecraft.resources.ResourceLocation itemLoc = com.quackers29.businesscraft.api.PlatformAccess.getRegistry()
                .getItemKey(item);
        String itemStr = itemLoc.toString();

        for (String nodeId : unlockedNodes) {
            com.quackers29.businesscraft.production.UpgradeNode node = com.quackers29.businesscraft.production.UpgradeRegistry
                    .get(nodeId);
            if (node == null)
                continue;

            int lvl = town.getUpgradeLevel(nodeId);

            for (com.quackers29.businesscraft.data.parsers.Effect eff : node.getEffects()) {
                boolean matches = false;
                String type = "";
                String target = eff.getTarget();

                if (target.equals("storage_cap_all")) {
                    matches = true;
                    type = "Cap";
                } else if (target.equals("storage_cap_" + itemStr)) {
                    matches = true;
                    type = "Cap";
                } else {
                    // Check if this upgrade affects production of this item
                    // This mimic the conservative check from client but can be more accurate here
                    // if needed
                }

                if (matches) {
                    float val = node.calculateEffectValue(eff, lvl);
                    String valStr;
                    if (type.equals("Speed")) {
                        valStr = String.format("%+.0f%%", val * 100);
                    } else {
                        if (val == (long) val) {
                            valStr = String.format("%+d", (long) val);
                        } else {
                            valStr = String.format("%+.1f", val);
                        }
                    }

                    String name = node.getDisplayName();
                    if (node.isRepeatable()) {
                        name += " (" + lvl + ")";
                    }

                    effects.add(name + ": " + valStr + " " + type);
                }
            }
        }

        return effects;
    }
}