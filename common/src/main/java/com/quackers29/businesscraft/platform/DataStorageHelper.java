package com.quackers29.businesscraft.platform;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Platform-agnostic interface for data storage operations.
 * This interface abstracts the differences between Forge's SavedData
 * and Fabric's PersistentState systems.
 */
public interface DataStorageHelper {
    
    /**
     * Get or create persistent data for a level.
     * @param level The server level (ServerLevel)
     * @param name The data identifier name
     * @param loader Function to load data from NBT (CompoundTag)
     * @param creator Supplier to create new data instance
     * @param <T> The data type
     * @return The data instance
     */
    <T> T getOrCreateData(Object level, String name, 
                         Function<Object, T> loader, 
                         Supplier<T> creator);
    
    /**
     * Mark the data as dirty for saving.
     * @param level The server level (ServerLevel) 
     * @param name The data identifier name
     */
    void markDirty(Object level, String name);
}