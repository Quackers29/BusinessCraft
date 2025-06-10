package com.yourdomain.businesscraft.util;

/**
 * Common error types for BusinessCraft operations.
 * Provides standardized error handling across the application.
 */
public class BCError {
    
    /**
     * Base class for all BusinessCraft errors.
     */
    public static abstract class Error {
        private final String message;
        private final String code;
        
        protected Error(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getCode() {
            return code;
        }
        
        @Override
        public String toString() {
            return code + ": " + message;
        }
    }
    
    /**
     * Errors related to validation failures.
     */
    public static class ValidationError extends Error {
        public ValidationError(String code, String message) {
            super(code, message);
        }
        
        public ValidationError(String message) {
            super("VALIDATION_ERROR", message);
        }
        
        public static ValidationError required(String fieldName) {
            return new ValidationError(fieldName + " is required");
        }
        
        public static ValidationError invalid(String fieldName, String reason) {
            return new ValidationError(fieldName + " is invalid: " + reason);
        }
    }
    
    /**
     * Errors related to town operations.
     */
    public static class TownError extends Error {
        public TownError(String code, String message) {
            super(code, message);
        }
        
        public static TownError notFound(String townId) {
            return new TownError("TOWN_NOT_FOUND", "Town not found: " + townId);
        }
        
        public static TownError alreadyExists(String townName) {
            return new TownError("TOWN_ALREADY_EXISTS", "Town already exists: " + townName);
        }
        
        public static TownError insufficientResources(String resource, int required, int available) {
            return new TownError("INSUFFICIENT_RESOURCES", 
                String.format("Insufficient %s: required %d, available %d", resource, required, available));
        }
        
        public static TownError invalidDistance(int distance, int minimum) {
            return new TownError("INVALID_DISTANCE", 
                String.format("Town too close: distance %d, minimum required %d", distance, minimum));
        }
    }
    
    /**
     * Errors related to UI operations.
     */
    public static class UIError extends Error {
        public UIError(String code, String message) {
            super(code, message);
        }
        
        public static UIError modalCreationFailed(String modalType, String reason) {
            return new UIError("MODAL_CREATION_FAILED", 
                String.format("Failed to create %s modal: %s", modalType, reason));
        }
        
        public static UIError screenInitializationFailed(String screenType) {
            return new UIError("SCREEN_INIT_FAILED", 
                "Failed to initialize " + screenType + " screen");
        }
        
        public static UIError invalidScreenState(String expectedState, String actualState) {
            return new UIError("INVALID_SCREEN_STATE", 
                String.format("Invalid screen state: expected %s, got %s", expectedState, actualState));
        }
    }
    
    /**
     * Errors related to network operations.
     */
    public static class NetworkError extends Error {
        public NetworkError(String code, String message) {
            super(code, message);
        }
        
        public static NetworkError packetSerializationFailed(String packetType) {
            return new NetworkError("PACKET_SERIALIZATION_FAILED", 
                "Failed to serialize packet: " + packetType);
        }
        
        public static NetworkError invalidPacketData(String packetType, String reason) {
            return new NetworkError("INVALID_PACKET_DATA", 
                String.format("Invalid %s packet data: %s", packetType, reason));
        }
    }
    
    /**
     * Errors related to data persistence.
     */
    public static class DataError extends Error {
        public DataError(String code, String message) {
            super(code, message);
        }
        
        public static DataError saveFailure(String dataType, String reason) {
            return new DataError("SAVE_FAILURE", 
                String.format("Failed to save %s: %s", dataType, reason));
        }
        
        public static DataError loadFailure(String dataType, String reason) {
            return new DataError("LOAD_FAILURE", 
                String.format("Failed to load %s: %s", dataType, reason));
        }
        
        public static DataError corruptedData(String dataType) {
            return new DataError("CORRUPTED_DATA", 
                "Data corruption detected in " + dataType);
        }
    }
    
    /**
     * Errors related to configuration operations.
     */
    public static class ConfigError extends Error {
        public ConfigError(String code, String message) {
            super(code, message);
        }
        
        public static ConfigError fileNotFound(String filePath) {
            return new ConfigError("CONFIG_FILE_NOT_FOUND", "Configuration file not found: " + filePath);
        }
        
        public static ConfigError invalidFormat(String filePath, String reason) {
            return new ConfigError("INVALID_CONFIG_FORMAT", 
                String.format("Invalid configuration format in %s: %s", filePath, reason));
        }
        
        public static ConfigError reloadFailed(String configName, String reason) {
            return new ConfigError("CONFIG_RELOAD_FAILED", 
                String.format("Failed to reload configuration %s: %s", configName, reason));
        }
    }
    
    /**
     * Generic errors for unexpected situations.
     */
    public static class UnexpectedError extends Error {
        public UnexpectedError(String message) {
            super("UNEXPECTED_ERROR", message);
        }
        
        public static UnexpectedError fromException(Exception e) {
            return new UnexpectedError("Unexpected error: " + e.getMessage());
        }
    }
}