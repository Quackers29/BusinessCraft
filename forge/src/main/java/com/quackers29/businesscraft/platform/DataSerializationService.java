package com.quackers29.businesscraft.platform;

/**
 * Service for data serialization/deserialization
 */
public interface DataSerializationService {
    /**
     * Create a new data container for saving
     */
    Object createDataContainer();
    
    /**
     * Save a string value to the data container
     */
    void saveString(Object container, String key, String value);
    
    /**
     * Load a string value from the data container
     */
    String loadString(Object container, String key);
    
    /**
     * Save an integer value to the data container
     */
    void saveInt(Object container, String key, int value);
    
    /**
     * Load an integer value from the data container
     */
    int loadInt(Object container, String key);
    
    /**
     * Save a long value to the data container
     */
    void saveLong(Object container, String key, long value);
    
    /**
     * Load a long value from the data container
     */
    long loadLong(Object container, String key);
}