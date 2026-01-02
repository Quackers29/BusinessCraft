package com.quackers29.businesscraft.town.ai;

/**
 * Interface defining the town state access required by the AI.
 * Allows AI logic to run on both Server (Town) and Client (Cache).
 */
public interface ITownState {
    float getStock(String resourceId);

    float getStorageCap(String resourceId);

    float getProductionRate(String resourceId);

    float getConsumptionRate(String resourceId);
}
