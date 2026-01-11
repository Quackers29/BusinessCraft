package com.quackers29.businesscraft.town.viewmodel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import com.quackers29.businesscraft.api.PlatformAccess;

import java.util.HashMap;
import java.util.Map;

/**
 * View-Model for resource display on the client side.
 * Contains ONLY pre-calculated display data from the server.
 * Client performs ZERO calculations - only rendering display strings.
 * 
 * This implements the "dumb terminal" pattern where:
 * - Server calculates all values and formats display strings
 * - Client receives ready-to-display data
 * - No business logic duplication between client/server
 */
public class TownResourceViewModel {
    
    /**
     * Resource display information for a single resource
     */
    public static class ResourceDisplayInfo {
        private final String displayName;           // "Coal"
        private final String currentAmount;         // "250"
        private final String productionRate;       // "+15/hr" or "No Production"
        private final String consumptionRate;      // "-8/hr" or "No Consumption"  
        private final String capacity;             // "500" or "Unlimited"
        private final String capacityPercentage;   // "50%" or "Full"
        private final String inTransit;            // "+12 Incoming" or ""
        private final String statusIndicator;      // "Surplus", "Shortage", "Balanced"
        private final boolean isResourceShortage;  // true if resource is critically low
        private final boolean isCapacityFull;      // true if at/near capacity limit
        
        public ResourceDisplayInfo(String displayName, String currentAmount, 
                                 String productionRate, String consumptionRate,
                                 String capacity, String capacityPercentage,
                                 String inTransit, String statusIndicator,
                                 boolean isResourceShortage, boolean isCapacityFull) {
            this.displayName = displayName;
            this.currentAmount = currentAmount;
            this.productionRate = productionRate;
            this.consumptionRate = consumptionRate;
            this.capacity = capacity;
            this.capacityPercentage = capacityPercentage;
            this.inTransit = inTransit;
            this.statusIndicator = statusIndicator;
            this.isResourceShortage = isResourceShortage;
            this.isCapacityFull = isCapacityFull;
        }
        
        public ResourceDisplayInfo(FriendlyByteBuf buf) {
            this.displayName = buf.readUtf();
            this.currentAmount = buf.readUtf();
            this.productionRate = buf.readUtf();
            this.consumptionRate = buf.readUtf();
            this.capacity = buf.readUtf();
            this.capacityPercentage = buf.readUtf();
            this.inTransit = buf.readUtf();
            this.statusIndicator = buf.readUtf();
            this.isResourceShortage = buf.readBoolean();
            this.isCapacityFull = buf.readBoolean();
        }
        
        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(displayName);
            buf.writeUtf(currentAmount);
            buf.writeUtf(productionRate);
            buf.writeUtf(consumptionRate);
            buf.writeUtf(capacity);
            buf.writeUtf(capacityPercentage);
            buf.writeUtf(inTransit);
            buf.writeUtf(statusIndicator);
            buf.writeBoolean(isResourceShortage);
            buf.writeBoolean(isCapacityFull);
        }
        
        // Getters for client display
        public String getDisplayName() { return displayName; }
        public String getCurrentAmount() { return currentAmount; }
        public String getProductionRate() { return productionRate; }
        public String getConsumptionRate() { return consumptionRate; }
        public String getCapacity() { return capacity; }
        public String getCapacityPercentage() { return capacityPercentage; }
        public String getInTransit() { return inTransit; }
        public String getStatusIndicator() { return statusIndicator; }
        public boolean isResourceShortage() { return isResourceShortage; }
        public boolean isCapacityFull() { return isCapacityFull; }
    }
    
    // Map of resources to their display information
    private final Map<Item, ResourceDisplayInfo> resourceDisplayData;
    
    // Overall town resource summary (pre-calculated)
    private final String totalResourcesDisplay;     // "15 different resources"
    private final String overallStatus;             // "Healthy Economy", "Resource Crisis", etc.
    private final String economicTrend;             // "Growing", "Stable", "Declining"
    
    public TownResourceViewModel(Map<Item, ResourceDisplayInfo> resourceDisplayData,
                                String totalResourcesDisplay,
                                String overallStatus, 
                                String economicTrend) {
        this.resourceDisplayData = new HashMap<>(resourceDisplayData);
        this.totalResourcesDisplay = totalResourcesDisplay;
        this.overallStatus = overallStatus;
        this.economicTrend = economicTrend;
    }
    
    public TownResourceViewModel(FriendlyByteBuf buf) {
        // Read resource display data
        int resourceCount = buf.readInt();
        this.resourceDisplayData = new HashMap<>();
        
        for (int i = 0; i < resourceCount; i++) {
            var resourceLocation = buf.readResourceLocation();
            var item = (Item) PlatformAccess.getRegistry().getItem(resourceLocation);
            if (item != null) {
                var displayInfo = new ResourceDisplayInfo(buf);
                this.resourceDisplayData.put(item, displayInfo);
            }
        }
        
        // Read summary data
        this.totalResourcesDisplay = buf.readUtf();
        this.overallStatus = buf.readUtf();
        this.economicTrend = buf.readUtf();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        // Write resource display data
        buf.writeInt(resourceDisplayData.size());
        
        resourceDisplayData.forEach((item, displayInfo) -> {
            var key = (ResourceLocation) PlatformAccess.getRegistry().getItemKey(item);
            buf.writeResourceLocation(key);
            displayInfo.toBytes(buf);
        });
        
        // Write summary data
        buf.writeUtf(totalResourcesDisplay);
        buf.writeUtf(overallStatus);
        buf.writeUtf(economicTrend);
    }
    
    // Client-side getters (NO CALCULATIONS)
    public Map<Item, ResourceDisplayInfo> getResourceDisplayData() {
        return resourceDisplayData;
    }
    
    public ResourceDisplayInfo getResourceDisplay(Item item) {
        return resourceDisplayData.get(item);
    }
    
    public String getTotalResourcesDisplay() {
        return totalResourcesDisplay;
    }
    
    public String getOverallStatus() {
        return overallStatus;
    }
    
    public String getEconomicTrend() {
        return economicTrend;
    }
    
    public boolean hasResource(Item item) {
        return resourceDisplayData.containsKey(item);
    }
    
    public int getResourceCount() {
        return resourceDisplayData.size();
    }
}