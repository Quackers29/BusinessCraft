package com.quackers29.businesscraft.town.data;

import java.util.Map;

/**
 * Platform-agnostic town data persistence interface.
 * This interface provides a clean abstraction layer over platform-specific
 * data storage systems (Forge's SavedData vs Fabric's PersistentState).
 * 
 * Uses the existing DataStorageHelper for actual platform-specific storage operations.
 * 
 * Enhanced MultiLoader approach: Business logic uses this interface,
 * platform modules provide concrete implementations.
 */
public interface ITownPersistence {
    
    /**
     * Save town data to persistent storage.
     * The data is stored in a platform-agnostic format using NBT-compatible structures.
     * 
     * @param townData Map containing all town data to persist
     *                 Keys should be strings, values should be NBT-serializable types
     */
    void save(Map<String, Object> townData);
    
    /**
     * Load town data from persistent storage.
     * Returns data in the same platform-agnostic format as save().
     * 
     * @return Map containing loaded town data, or empty map if no data exists
     */
    Map<String, Object> load();
    
    /**
     * Mark the persistence layer as dirty, indicating data needs to be saved.
     * This follows the standard Minecraft pattern for lazy saving.
     */
    void markDirty();
    
    /**
     * Get the unique identifier for this persistence context.
     * This is typically used for the saved data filename.
     * 
     * @return Unique identifier string (e.g., "businesscraft_towns")
     */
    String getIdentifier();
    
    /**
     * Check if the persistence layer has been modified and needs saving.
     * 
     * @return true if data has been modified since last save
     */
    boolean isDirty();
    
    /**
     * Clear the dirty flag, typically called after successful save.
     */
    void clearDirty();
    
    /**
     * Get the level/world context for this persistence layer.
     * Returns the platform-specific level object (ServerLevel).
     * 
     * @return Platform-specific level object
     */
    Object getLevel();
}