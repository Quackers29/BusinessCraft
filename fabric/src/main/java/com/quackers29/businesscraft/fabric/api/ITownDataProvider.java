package com.quackers29.businesscraft.fabric.api;

/**
 * Fabric implementation of ITownDataProvider interface.
 * This provides town data access using Fabric-specific APIs.
 */
public interface ITownDataProvider {

    /**
     * Get the position of this town data provider
     */
    Object getPosition(); // Will be BlockPos at runtime

    /**
     * Get the town ID associated with this provider
     */
    String getTownId();

    /**
     * Check if this provider has a valid town
     */
    boolean hasValidTown();

    /**
     * Get the display name for this provider
     */
    Object getDisplayName(); // Will be Component at runtime

    /**
     * Update the provider's data
     */
    void updateData();

    /**
     * Check if the provider can provide the specified item type
     */
    boolean canProvideItem(Object item); // Will be Item at runtime

    /**
     * Get the amount of the specified item available
     */
    int getItemAmount(Object item); // Will be Item at runtime

    // Additional platform-specific methods would be defined here
    // This provides the basic interface structure for Fabric
}
