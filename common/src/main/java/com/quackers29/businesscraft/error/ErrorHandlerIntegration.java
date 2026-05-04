package com.quackers29.businesscraft.error;

import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

public class ErrorHandlerIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlerIntegration.class);
    
    private static final ErrorHandler errorHandler = ErrorHandler.getInstance();
    
    public static <T> Result<T, BCError.Error> wrapTownOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "TownSystem");
    }
    
    public static <T> Result<T, BCError.Error> wrapTownOperation(Supplier<T> operation, String operationName, 
                                                               Function<Exception, T> recovery) {
        return errorHandler.tryExecute(operation, operationName, "TownSystem", recovery);
    }
    
    public static <T> Result<T, BCError.Error> wrapUIOperation(Supplier<T> operation, String operationName, 
                                                             String componentName) {
        return errorHandler.tryExecute(operation, operationName, componentName);
    }
    
    public static <T> Result<T, BCError.Error> wrapUIOperation(Supplier<T> operation, String operationName, 
                                                             String componentName, Function<Exception, T> recovery) {
        return errorHandler.tryExecute(operation, operationName, componentName, recovery);
    }
    
    public static <T> Result<T, BCError.Error> wrapNetworkOperation(Supplier<T> operation, String packetName) {
        return errorHandler.tryExecute(operation, "Processing " + packetName, "NetworkSystem");
    }
    
    public static <T> Result<T, BCError.Error> wrapDataOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "DataSystem");
    }
    
    public static <T> Result<T, BCError.Error> wrapDataOperation(Supplier<T> operation, String operationName, 
                                                               Function<Exception, T> recovery) {
        return errorHandler.tryExecute(operation, operationName, "DataSystem", recovery);
    }
    
    public static <T> Result<T, BCError.Error> wrapConfigOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "ConfigSystem");
    }
    
    public static <T> Result<T, BCError.Error> wrapPlatformOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "PlatformSystem");
    }
    
    public static <T> Result<T, BCError.Error> wrapEntityOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "EntitySystem");
    }
    
    public static <T> Result<T, BCError.Error> wrapModalOperation(Supplier<T> operation, String operationName, 
                                                                String screenName) {
        return errorHandler.tryExecute(operation, operationName, "Modal-" + screenName);
    }
    
    public static <T> Result<T, BCError.Error> wrapValidationOperation(Supplier<T> operation, String operationName) {
        return errorHandler.tryExecute(operation, operationName, "ValidationSystem");
    }
    
    public static Result<Void, BCError.Error> safeExecute(Runnable operation, String operationName, 
                                                         String componentName) {
        return errorHandler.tryExecute(() -> {
            operation.run();
            return null;
        }, operationName, componentName);
    }
    
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
    
    public static <T> Result<T, BCError.Error> fromLegacyTryCatch(Supplier<T> operation, String operationName, 
                                                                 String componentName, T defaultValue) {
        return errorHandler.tryExecute(operation, operationName, componentName, 
            (exception) -> {
                LOGGER.debug("Using default value due to error in {}: {}", componentName, operationName);
                return defaultValue;
            });
    }
    
    public static Result<Boolean, BCError.Error> safeBooleanOperation(Supplier<Boolean> operation, 
                                                                    String operationName, String componentName) {
        return fromLegacyTryCatch(operation, operationName, componentName, false);
    }
    
    public static Result<Integer, BCError.Error> safeIntegerOperation(Supplier<Integer> operation, 
                                                                    String operationName, String componentName) {
        return fromLegacyTryCatch(operation, operationName, componentName, 0);
    }
    
    public static Result<String, BCError.Error> safeStringOperation(Supplier<String> operation, 
                                                                  String operationName, String componentName) {
        return fromLegacyTryCatch(operation, operationName, componentName, "");
    }
    
    public static String getErrorMetricsSummary() {
        try {
            var errorCounts = errorHandler.getErrorCounts();
            var errorMetrics = errorHandler.getErrorMetrics();
            
            if (errorCounts.isEmpty()) {
                return "No errors recorded";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("Error Summary:\n");
            
            errorCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> summary.append(String.format("  %s: %d occurrences\n", 
                    entry.getKey(), entry.getValue())));
            
            long totalErrors = errorCounts.values().stream().mapToLong(Long::longValue).sum();
            summary.append(String.format("Total errors: %d\n", totalErrors));
            
            return summary.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to generate error metrics summary", e);
            return "Error metrics unavailable";
        }
    }
    
    public static void resetAllMetrics() {
        errorHandler.resetMetrics();
        LOGGER.info("All error metrics have been reset via integration utility");
    }
    
    public static void configureForEnvironment(boolean isProduction) {
        errorHandler.setEnableDetailedLogging(!isProduction);
        errorHandler.setEnableStackTraceLogging(!isProduction);
        errorHandler.setEnableMetrics(true);
        
        LOGGER.info("Error handler configured for {} environment", isProduction ? "production" : "development");
    }
    
    public static void registerBusinessCraftRecoveryStrategies() {
        // Town-specific recoveries
        errorHandler.registerRecoveryStrategy("TownNotFoundException", e -> {
            LOGGER.debug("Applying town not found recovery");
            return null;
        });
        
        // Platform-specific recoveries
        errorHandler.registerRecoveryStrategy("PlatformValidationException", e -> {
            LOGGER.debug("Applying platform validation recovery");
            return false;
        });
        
        // UI-specific recoveries
        errorHandler.registerRecoveryStrategy("ScreenInitializationException", e -> {
            LOGGER.debug("Applying screen initialization recovery");
            return false;
        });
        
        // Network-specific recoveries
        errorHandler.registerRecoveryStrategy("PacketProcessingException", e -> {
            LOGGER.debug("Applying packet processing recovery");
            return false;
        });
        
        LOGGER.info("BusinessCraft-specific recovery strategies registered");
    }
}
