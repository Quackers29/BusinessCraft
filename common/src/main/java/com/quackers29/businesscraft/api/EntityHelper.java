package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for accessing entity types.
 * Implementations will provide access to registered entity types.
 */
public interface EntityHelper {
    /**
     * Get the Tourist entity type
     */
    Object getTouristEntityType();

    /**
     * Set the current town block ID for path creation mode.
     * 
     * @param player The player to set data for
     * @param townId The UUID of the town block entity
     */
    void setPathCreationTarget(Object player, java.util.UUID townId);

    /**
     * Clear the current town block ID for path creation mode.
     * 
     * @param player The player to clear data for
     */
    void clearPathCreationTarget(Object player);

    /**
     * Get a player by UUID
     */
    Object getPlayerByUUID(Object level, java.util.UUID uuid);
}
