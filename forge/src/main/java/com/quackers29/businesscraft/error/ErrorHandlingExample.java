package com.quackers29.businesscraft.error;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * Demonstration of how to integrate the unified error handling middleware
 * with existing BusinessCraft components. Shows migration patterns from
 * traditional try-catch blocks to the Result pattern with error middleware.
 * 
 * This class serves as both documentation and example for developers
 * retrofitting existing code with the new error handling system.
 */
public class ErrorHandlingExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingExample.class);
    
    /**
     * Example: Traditional error handling (BEFORE refactoring)
     * This shows the old pattern that should be migrated.
     */
    public static boolean createTownOldWay(ServerLevel level, BlockPos pos, String townName) {
        try {
            TownManager townManager = TownManager.get(level);
            Town createdTown = townManager.createTown(pos.getX(), pos.getY(), pos.getZ(), townName);
            UUID townId = createdTown != null ? createdTown.getId() : null;
            
            if (townId != null) {
                // Additional setup logic here
                LOGGER.info("Town '{}' created successfully at {} with ID {}", townName, pos, townId);
                return true;
            } else {
                LOGGER.warn("Failed to create town '{}' - unknown reason", townName);
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error creating town '{}' at {}", townName, pos, e);
            return false;
        }
    }
    
    /**
     * Example: New error handling pattern (AFTER refactoring)
     * This shows the recommended approach using the error middleware.
     */
    public static Result<Town, BCError.Error> createTownNewWay(ServerLevel level, BlockPos pos, String townName) {
        return ErrorHandlerIntegration.wrapTownOperation(
            () -> {
                TownManager townManager = TownManager.get(level);
                Town createdTown = townManager.createTown(pos.getX(), pos.getY(), pos.getZ(), townName);
            UUID townId = createdTown != null ? createdTown.getId() : null;
                
                if (townId == null) {
                    throw new RuntimeException("Failed to create town - unknown reason");
                }
                
                // Get the created town
                Town town = townManager.getTown(townId);
                if (town == null) {
                    throw new RuntimeException("Town was created but could not be retrieved");
                }
                
                // Additional setup logic here
                LOGGER.info("Town '{}' created successfully at {}", townName, pos);
                return town;
            },
            String.format("Creating town '%s' at %s", townName, pos)
        );
    }
    
    /**
     * Example: New error handling with recovery (ADVANCED pattern)
     * This shows how to provide fallback behavior when operations fail.
     */
    public static Result<Town, BCError.Error> createTownWithRecovery(ServerLevel level, BlockPos pos, String townName) {
        return ErrorHandlerIntegration.wrapTownOperation(
            () -> {
                TownManager townManager = TownManager.get(level);
                Town createdTown = townManager.createTown(pos.getX(), pos.getY(), pos.getZ(), townName);
            UUID townId = createdTown != null ? createdTown.getId() : null;
                
                if (townId == null) {
                    throw new RuntimeException("Failed to create town - unknown reason");
                }
                
                Town town = townManager.getTown(townId);
                if (town == null) {
                    throw new RuntimeException("Town was created but could not be retrieved");
                }
                
                return town;
            },
            String.format("Creating town '%s' at %s", townName, pos),
            (exception) -> {
                // Recovery strategy: create a minimal town with basic settings
                LOGGER.warn("Primary town creation failed, attempting recovery for '{}'", townName);
                
                try {
                    TownManager townManager = TownManager.get(level);
                    // Attempt to create with default settings
                    Town recoveryTown = townManager.createTown(pos.getX(), pos.getY(), pos.getZ(), townName + "_recovered");
                    UUID recoveryId = recoveryTown != null ? recoveryTown.getId() : null;
                    if (recoveryId != null) {
                        return townManager.getTown(recoveryId);
                    }
                    return null;
                } catch (Exception recoveryException) {
                    LOGGER.error("Recovery also failed for town '{}'", townName, recoveryException);
                    return null;
                }
            }
        );
    }
    
    /**
     * Example: Using the Result pattern in calling code
     * This shows how consuming code should handle Result types.
     */
    public static void exampleUsageOfResultPattern(ServerLevel level, BlockPos pos, String townName) {
        // Simple usage with onSuccess/onFailure
        createTownNewWay(level, pos, townName)
            .onSuccess(town -> {
                LOGGER.info("Successfully created town: {}", town.getName());
                // Additional success handling here
            })
            .onFailure(error -> {
                LOGGER.error("Failed to create town: {}", error.getMessage());
                // Error handling here
            });
        
        // Advanced usage with map/flatMap for chaining operations
        Result<String, BCError.Error> townInfoResult = createTownNewWay(level, pos, townName)
            .map(town -> String.format("Town '%s' has %d residents", 
                town.getName(), town.getPopulation()))
            .mapError(error -> new BCError.UIError("DISPLAY_ERROR", 
                "Could not display town information: " + error.getMessage()));
        
        // Extract value with default
        String townInfo = townInfoResult.getOrElse("No town information available");
        LOGGER.info("Town info: {}", townInfo);
        
        // Pattern matching style usage
        switch (townInfoResult.isSuccess() ? "SUCCESS" : "FAILURE") {
            case "SUCCESS":
                String info = townInfoResult.getValue();
                // Handle success case
                LOGGER.info("Town created successfully: {}", info);
                break;
            case "FAILURE":
                BCError.Error error = townInfoResult.getError();
                // Handle error case
                LOGGER.error("Town creation failed: {} ({})", error.getMessage(), error.getCode());
                break;
        }
    }
    
    /**
     * Example: Migrating UI operations to error middleware
     */
    public static Result<Boolean, BCError.Error> renderUIComponentSafely(String componentName) {
        return ErrorHandlerIntegration.wrapUIOperation(
            () -> {
                // Simulate UI rendering that might fail
                if (componentName == null || componentName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Component name cannot be null or empty");
                }
                
                // Simulate rendering logic
                LOGGER.debug("Rendering UI component: {}", componentName);
                
                // Simulate potential rendering issues
                if (componentName.contains("invalid")) {
                    throw new RuntimeException("Invalid component configuration");
                }
                
                return true;
            },
            String.format("Rendering component '%s'", componentName),
            "UIRenderer"
        );
    }
    
    /**
     * Example: Migrating data operations to error middleware
     */
    public static Result<Boolean, BCError.Error> saveDataSafely(String data, String filename) {
        return ErrorHandlerIntegration.wrapDataOperation(
            () -> {
                if (data == null) {
                    throw new IllegalArgumentException("Data cannot be null");
                }
                
                if (filename == null || filename.trim().isEmpty()) {
                    throw new IllegalArgumentException("Filename cannot be null or empty");
                }
                
                // Simulate data saving logic
                LOGGER.debug("Saving data to file: {}", filename);
                
                // Simulate potential I/O issues
                if (filename.contains("readonly")) {
                    throw new RuntimeException("Cannot write to read-only file");
                }
                
                return true;
            },
            String.format("Saving data to '%s'", filename),
            (exception) -> {
                // Recovery: attempt to save to backup location
                LOGGER.warn("Primary save failed, attempting backup save");
                try {
                    String backupFilename = filename + ".backup";
                    LOGGER.debug("Saving to backup location: {}", backupFilename);
                    return true;
                } catch (Exception backupException) {
                    LOGGER.error("Backup save also failed", backupException);
                    return false;
                }
            }
        );
    }
    
    /**
     * Example: Void operations (operations that don't return values)
     */
    public static Result<Void, BCError.Error> performMaintenanceTask(String taskName) {
        return ErrorHandlerIntegration.safeExecute(
            () -> {
                LOGGER.info("Performing maintenance task: {}", taskName);
                
                // Simulate maintenance work
                if (taskName.contains("dangerous")) {
                    throw new RuntimeException("Dangerous maintenance task failed");
                }
                
                // Maintenance logic here
                try {
                    Thread.sleep(100); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Maintenance task interrupted", e);
                }
                
                LOGGER.info("Maintenance task '{}' completed successfully", taskName);
            },
            String.format("Maintenance task '%s'", taskName),
            "MaintenanceSystem",
            () -> {
                // Recovery action for void operations
                LOGGER.warn("Maintenance task failed, performing cleanup");
                // Cleanup logic here
            }
        );
    }
    
    /**
     * Example: Boolean operations with safe default
     */
    public static boolean isValidConfigurationSafely(String configKey) {
        Result<Boolean, BCError.Error> result = ErrorHandlerIntegration.safeBooleanOperation(
            () -> {
                if (configKey == null) {
                    throw new IllegalArgumentException("Config key cannot be null");
                }
                
                // Simulate configuration validation
                return !configKey.contains("invalid");
            },
            String.format("Validating config key '%s'", configKey),
            "ConfigValidator"
        );
        
        // Extract boolean value with false as default for any errors
        return result.getOrElse(false);
    }
    
    /**
     * Example: Demonstrating error metrics and reporting
     */
    public static void demonstrateErrorReporting() {
        // Initialize error handler for BusinessCraft
        ErrorHandlerIntegration.configureForEnvironment(false); // Development mode
        ErrorHandlerIntegration.registerBusinessCraftRecoveryStrategies();
        
        // Simulate some operations that will generate errors for demonstration
        createTownNewWay(null, null, null); // Will generate errors
        renderUIComponentSafely("invalid_component"); // Will generate errors
        saveDataSafely(null, "test.dat"); // Will generate errors
        
        // Generate and log error reports
        String summaryReport = ErrorReporter.generateSummaryReport();
        LOGGER.info("Error Summary:\n{}", summaryReport);
        
        String comprehensiveReport = ErrorReporter.generateComprehensiveReport();
        LOGGER.debug("Comprehensive Error Report:\n{}", comprehensiveReport);
        
        // JSON report for machine consumption
        String jsonReport = ErrorReporter.generateJsonReport();
        LOGGER.debug("JSON Error Report: {}", jsonReport);
        
        // Integration utility metrics
        String metricsInfo = ErrorHandlerIntegration.getErrorMetricsSummary();
        LOGGER.info("Error Metrics Summary:\n{}", metricsInfo);
    }
    
    /**
     * Example: Legacy code migration pattern
     * Shows a step-by-step approach to migrating existing try-catch blocks.
     */
    public static void migrateLegacyCode() {
        // Step 1: Identify legacy try-catch pattern
        // (This is what we're migrating FROM)
        
        // Step 2: Wrap in ErrorHandlerIntegration.fromLegacyTryCatch
        Result<String, BCError.Error> result = ErrorHandlerIntegration.fromLegacyTryCatch(
            () -> {
                // Original legacy code here (may throw exceptions)
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Simulated legacy error");
                }
                return "Legacy operation succeeded";
            },
            "Legacy operation migration",
            "LegacySystem",
            "Default value for legacy operation" // Safe default
        );
        
        // Step 3: Use Result pattern in calling code
        result.onSuccess(value -> LOGGER.info("Legacy migration successful: {}", value))
              .onFailure(error -> LOGGER.warn("Legacy migration had error: {}", error.getMessage()));
    }
}