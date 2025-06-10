package com.yourdomain.businesscraft.error;

import com.yourdomain.businesscraft.util.Result;
import com.yourdomain.businesscraft.util.BCError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Integration utilities for retrofitting error handling throughout the BusinessCraft codebase.
 * Provides convenient methods to wrap existing functionality with unified error handling.
 * Designed for gradual migration of existing code to the new error handling middleware.
 */
public class ErrorHandlerIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlerIntegration.class);
    
    private static final ErrorHandler errorHandler = ErrorHandler.getInstance();
    
    /**
     * Wraps a town operation with error handling.
     * Convenience method for town-related operations.
     */
    public static <T> Result<T, BCError.Error> wrapTownOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "TownSystem");
    }
    
    /**
     * Wraps a town operation with error handling and recovery.
     */
    public static <T> Result<T, BCError.Error> wrapTownOperation(Supplier<T> operation, String operationName, 
                                                               Function<Exception, T> recovery) {
        return errorHandler.tryExecute(operation, operationName, "TownSystem", recovery);
    }
    
    /**
     * Wraps a UI operation with error handling.
     * Convenience method for UI-related operations.
     */
    public static <T> Result<T, BCError.Error> wrapUIOperation(Supplier<T> operation, String operationName, 
                                                             String componentName) {
        return errorHandler.tryExecute(operation, operationName, componentName);
    }
    
    /**
     * Wraps a UI operation with error handling and recovery.
     */
    public static <T> Result<T, BCError.Error> wrapUIOperation(Supplier<T> operation, String operationName, 
                                                             String componentName, Function<Exception, T> recovery) {
        return errorHandler.tryExecute(operation, operationName, componentName, recovery);
    }
    
    /**
     * Wraps a network operation with error handling.
     * Convenience method for network packet operations.
     */
    public static <T> Result<T, BCError.Error> wrapNetworkOperation(Supplier<T> operation, String packetName) {
        return errorHandler.tryExecute(operation, "Processing " + packetName, "NetworkSystem");
    }
    
    /**
     * Wraps a data operation with error handling.
     * Convenience method for data persistence operations.
     */
    public static <T> Result<T, BCError.Error> wrapDataOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "DataSystem");
    }
    
    /**
     * Wraps a data operation with error handling and recovery.
     */
    public static <T> Result<T, BCError.Error> wrapDataOperation(Supplier<T> operation, String operationName, 
                                                               Function<Exception, T> recovery) {
        return errorHandler.tryExecute(operation, operationName, "DataSystem", recovery);
    }
    
    /**
     * Wraps a configuration operation with error handling.
     */
    public static <T> Result<T, BCError.Error> wrapConfigOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "ConfigSystem");
    }
    
    /**
     * Wraps a platform operation with error handling.
     */
    public static <T> Result<T, BCError.Error> wrapPlatformOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "PlatformSystem");
    }
    
    /**
     * Wraps an entity operation with error handling.
     */
    public static <T> Result<T, BCError.Error> wrapEntityOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "EntitySystem");
    }
    
    /**
     * Wraps a modal operation with error handling.
     * Convenience method for modal screen operations.
     */
    public static <T> Result<T, BCError.Error> wrapModalOperation(Supplier<T> operation, String operationName, 
                                                                String screenName) {
        return errorHandler.tryExecute(operation, operationName, "Modal-" + screenName);
    }
    
    /**
     * Wraps a validation operation with error handling.
     */
    public static <T> Result<T, BCError.Error> wrapValidationOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "ValidationSystem");
    }
    
    /**
     * Safely executes a void operation (operations that don't return values).
     * Returns Result<Void, BCError.Error> for consistency.
     */
    public static Result<Void, BCError.Error> safeExecute(Runnable operation, String operationName, 
                                                         String componentName) {
        return errorHandler.tryExecute(() -> {
            operation.run();
            return null;
        }, operationName, componentName);
    }
    
    /**
     * Safely executes a void operation with recovery.
     */
    public static Result<Void, BCError.Error> safeExecute(Runnable operation, String operationName, 
                                                         String componentName, Runnable recovery) {
        return errorHandler.tryExecute(() -> {
            operation.run();
            return null;
        }, operationName, componentName, (exception) -> {
            if (recovery != null) recovery.run();
            return null;
        });
    }
    
    /**
     * Legacy wrapper method to gradually migrate existing try-catch blocks.
     * Converts traditional exception handling to Result pattern.
     */
    public static <T> Result<T, BCError.Error> fromLegacyTryCatch(Supplier<T> operation, String operationName, 
                                                                 String componentName, T defaultValue) {
        return errorHandler.tryExecute(operation, operationName, componentName, 
            (exception) -> {
                LOGGER.debug("Using default value due to error in {}: {}", componentName, operationName);
                return defaultValue;
            });
    }
    
    /**
     * Helper method to handle common boolean operations with false as default.
     */
    public static Result<Boolean, BCError.Error> safeBooleanOperation(Supplier<Boolean> operation, 
                                                                    String operationName, String componentName) {
        return fromLegacyTryCatch(operation, operationName, componentName, false);
    }
    
    /**
     * Helper method to handle common integer operations with 0 as default.
     */
    public static Result<Integer, BCError.Error> safeIntegerOperation(Supplier<Integer> operation, 
                                                                    String operationName, String componentName) {
        return fromLegacyTryCatch(operation, operationName, componentName, 0);
    }
    
    /**
     * Helper method to handle common string operations with empty string as default.
     */
    public static Result<String, BCError.Error> safeStringOperation(Supplier<String> operation, 
                                                                  String operationName, String componentName) {
        return fromLegacyTryCatch(operation, operationName, componentName, "");
    }
    
    /**
     * Gets error metrics summary for monitoring dashboards.
     */
    public static String getErrorMetricsSummary() {
        try {
            var errorCounts = errorHandler.getErrorCounts();
            var errorMetrics = errorHandler.getErrorMetrics();
            
            if (errorCounts.isEmpty()) {
                return "No errors recorded";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("Error Summary:\n");
            
            // Top 5 most frequent errors
            errorCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> summary.append(String.format("  %s: %d occurrences\n", 
                    entry.getKey(), entry.getValue())));
            
            // Overall statistics
            long totalErrors = errorCounts.values().stream().mapToLong(Long::longValue).sum();
            summary.append(String.format("Total errors: %d\n", totalErrors));
            
            return summary.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to generate error metrics summary", e);
            return "Error metrics unavailable";
        }
    }
    
    /**
     * Resets all error metrics (useful for testing or fresh starts).
     */
    public static void resetAllMetrics() {
        errorHandler.resetMetrics();
        LOGGER.info("All error metrics have been reset via integration utility");
    }
    
    /**
     * Configures error handler settings for production vs development.
     */
    public static void configureForEnvironment(boolean isProduction) {
        errorHandler.setEnableDetailedLogging(!isProduction);
        errorHandler.setEnableStackTraceLogging(!isProduction);
        errorHandler.setEnableMetrics(true); // Always enable metrics
        
        LOGGER.info("Error handler configured for {} environment", isProduction ? "production" : "development");
    }
    
    /**
     * Registers custom recovery strategies for domain-specific exceptions.
     */
    public static void registerBusinessCraftRecoveryStrategies() {
        // Town-specific recoveries
        errorHandler.registerRecoveryStrategy("TownNotFoundException", e -> {
            LOGGER.debug("Applying town not found recovery");
            return null; // Return null town as safe default
        });
        
        // Platform-specific recoveries
        errorHandler.registerRecoveryStrategy("PlatformValidationException", e -> {
            LOGGER.debug("Applying platform validation recovery");
            return false; // Return false for validation failures
        });
        
        // UI-specific recoveries
        errorHandler.registerRecoveryStrategy("ScreenInitializationException", e -> {
            LOGGER.debug("Applying screen initialization recovery");
            return false; // Return false for UI initialization failures
        });
        
        // Network-specific recoveries
        errorHandler.registerRecoveryStrategy("PacketProcessingException", e -> {
            LOGGER.debug("Applying packet processing recovery");
            return false; // Return false for packet processing failures
        });
        
        LOGGER.info("BusinessCraft-specific recovery strategies registered");
    }
}