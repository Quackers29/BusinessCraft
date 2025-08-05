package com.quackers29.businesscraft.error;

/**
 * Enumeration of all possible errors in BusinessCraft.
 * Provides consistent error handling across platform implementations.
 */
public enum BCError {
    // Town-related errors
    TOWN_NOT_FOUND("Town not found"),
    TOWN_ALREADY_EXISTS("Town already exists at this location"),
    INVALID_TOWN_NAME("Invalid town name"),
    TOWN_NAME_TOO_LONG("Town name is too long (max 32 characters)"),
    TOWN_NAME_TOO_SHORT("Town name is too short (min 2 characters)"),
    INVALID_TOWN_NAME_CHARACTERS("Town name contains invalid characters"),
    TOWN_OPERATION_FAILED("Town operation failed"),
    
    // Tourist-related errors
    TOURIST_SPAWNING_DISABLED("Tourist spawning is disabled for this town"),
    MAX_TOURISTS_REACHED("Maximum tourist capacity reached"),
    TOURIST_CREATION_FAILED("Failed to create tourist entity"),
    
    // Platform-related errors
    PLATFORM_NOT_FOUND("Platform not found"),
    PLATFORM_INVALID_PATH("Platform path is invalid"),
    PLATFORM_CREATION_FAILED("Failed to create platform"),
    
    // Storage-related errors
    STORAGE_FULL("Storage is full"),
    INSUFFICIENT_RESOURCES("Insufficient resources"),
    STORAGE_ACCESS_DENIED("Access to storage denied"),
    
    // Configuration errors
    CONFIG_LOAD_FAILED("Failed to load configuration"),
    CONFIG_INVALID_VALUE("Invalid configuration value"),
    
    // Network/serialization errors
    SERIALIZATION_FAILED("Failed to serialize data"),
    DESERIALIZATION_FAILED("Failed to deserialize data"),
    NETWORK_ERROR("Network communication error"),
    
    // General errors
    INTERNAL_ERROR("Internal error occurred"),
    INVALID_ARGUMENT("Invalid argument provided"),
    OPERATION_NOT_SUPPORTED("Operation not supported"),
    PERMISSION_DENIED("Permission denied"),
    
    // World/position errors
    INVALID_POSITION("Invalid position"),
    POSITION_NOT_LOADED("Position is not loaded"),
    WORLD_NOT_FOUND("World not found");
    
    private final String message;
    
    BCError(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return name() + ": " + message;
    }
}