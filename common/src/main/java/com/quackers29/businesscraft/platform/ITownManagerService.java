package com.quackers29.businesscraft.platform;

import java.util.Map;
import java.util.UUID;

/**
 * Platform-agnostic interface for town management operations.
 * This interface provides a common API for town management across mod loaders.
 * 
 * All methods use Object types for platform compatibility, with implementations
 * handling the casting to appropriate platform-specific types.
 */
public interface ITownManagerService {
    
    /**
     * Register a new town at the specified location.
     * @param level The server level (ServerLevel)
     * @param pos The position for the town (BlockPos)
     * @param name The town name
     * @return The UUID of the created town
     */
    UUID registerTown(Object level, Object pos, String name);
    
    /**
     * Get a town by its UUID.
     * @param level The server level (ServerLevel)
     * @param id The town UUID
     * @return The town or null if not found
     */
    Object getTown(Object level, UUID id);
    
    /**
     * Get all towns in the level.
     * @param level The server level (ServerLevel)
     * @return Map of all towns by UUID
     */
    Map<UUID, Object> getAllTowns(Object level);
    
    /**
     * Check if a town can be placed at the specified position.
     * @param level The server level (ServerLevel)
     * @param pos The position to check (BlockPos)
     * @return true if a town can be placed
     */
    boolean canPlaceTownAt(Object level, Object pos);
    
    /**
     * Get the error message for why a town cannot be placed at a position.
     * @param level The server level (ServerLevel)
     * @param pos The position to check (BlockPos)
     * @return Error message or null if placement is valid
     */
    String getTownPlacementError(Object level, Object pos);
    
    /**
     * Update resources for a town.
     * @param level The server level (ServerLevel)
     * @param townId The town UUID
     * @param breadCount The new bread count
     */
    void updateResources(Object level, UUID townId, int breadCount);
    
    /**
     * Add a resource to a town.
     * @param level The server level (ServerLevel)
     * @param townId The town UUID
     * @param item The item to add (ItemStack)
     * @param count The quantity to add
     */
    void addResource(Object level, UUID townId, Object item, int count);
    
    /**
     * Remove a town from the level.
     * @param level The server level (ServerLevel)
     * @param id The town UUID to remove
     */
    void removeTown(Object level, UUID id);
    
    /**
     * Mark the town data as dirty for saving.
     * @param level The server level (ServerLevel)
     */
    void markDirty(Object level);
    
    /**
     * Handle server stopping event.
     * @param level The server level (ServerLevel)
     */
    void onServerStopping(Object level);
    
    /**
     * Clear all town manager instances.
     */
    void clearInstances();
    
    /**
     * Clear all towns from a level.
     * @param level The server level (ServerLevel)
     * @return Number of towns removed
     */
    int clearAllTowns(Object level);
}