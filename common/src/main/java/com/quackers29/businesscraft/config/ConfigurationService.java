package com.quackers29.businesscraft.config;

import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Hot-reloadable configuration service that monitors configuration files
 * and automatically reloads them when changes are detected.
 * Uses the Result pattern for type-safe error handling.
 */
public class ConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationService.class);
    
    private static ConfigurationService instance;
    
    private final Map<String, ConfigEntry> configurations = new ConcurrentHashMap<>();
    private final Map<String, WatchKey> watchKeys = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
        r -> {
            Thread t = new Thread(r, "ConfigurationService-Watcher");
            t.setDaemon(true);
            return t;
        }
    );
    
    private WatchService watchService;
    private volatile boolean isRunning = false;
    
    private ConfigurationService() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            startWatching();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize file watching service", e);
        }
    }
    
    /**
     * Gets the singleton instance of the configuration service.
     * 
     * @return The configuration service instance
     */
    public static synchronized ConfigurationService getInstance() {
        if (instance == null) {
            instance = new ConfigurationService();
        }
        return instance;
    }
    
    /**
     * Registers a configuration file for hot-reloading.
     * 
     * @param configName Unique name for this configuration
     * @param filePath Path to the configuration file
     * @param reloadCallback Callback to execute when file changes
     * @return Result indicating success or failure
     */
    public Result<Void, BCError.ConfigError> registerConfiguration(String configName, Path filePath, Consumer<Path> reloadCallback) {
        if (configName == null || configName.trim().isEmpty()) {
            return Result.failure(new BCError.ConfigError("INVALID_CONFIG_NAME", "Configuration name cannot be null or empty"));
        }
        
        if (filePath == null) {
            return Result.failure(new BCError.ConfigError("INVALID_FILE_PATH", "File path cannot be null"));
        }
        
        if (reloadCallback == null) {
            return Result.failure(new BCError.ConfigError("INVALID_CALLBACK", "Reload callback cannot be null"));
        }
        
        try {
            // Ensure the file exists
            if (!Files.exists(filePath)) {
                return Result.failure(new BCError.ConfigError("FILE_NOT_FOUND", 
                    "Configuration file does not exist: " + filePath));
            }
            
            // Get the parent directory for watching
            Path parentDir = filePath.getParent();
            if (parentDir == null) {
                return Result.failure(new BCError.ConfigError("INVALID_DIRECTORY", 
                    "Cannot determine parent directory for: " + filePath));
            }
            
            // Register watch key for the directory if not already registered
            String dirKey = parentDir.toString();
            if (!watchKeys.containsKey(dirKey)) {
                WatchKey watchKey = parentDir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
                watchKeys.put(dirKey, watchKey);
                LOGGER.info("Registered file watcher for directory: {}", parentDir);
            }
            
            // Store configuration entry
            ConfigEntry entry = new ConfigEntry(filePath, reloadCallback, System.currentTimeMillis());
            configurations.put(configName, entry);
            
            LOGGER.info("Registered configuration '{}' for hot-reloading: {}", configName, filePath);
            return Result.success(null);
            
        } catch (IOException e) {
            return Result.failure(new BCError.ConfigError("WATCH_REGISTRATION_FAILED", 
                "Failed to register file watcher: " + e.getMessage()));
        } catch (Exception e) {
            return Result.failure(new BCError.ConfigError("REGISTRATION_FAILED", 
                "Failed to register configuration: " + e.getMessage()));
        }
    }
    
    /**
     * Unregisters a configuration from hot-reloading.
     * 
     * @param configName Name of the configuration to unregister
     * @return Result indicating success or failure
     */
    public Result<Void, BCError.ConfigError> unregisterConfiguration(String configName) {
        if (configName == null || !configurations.containsKey(configName)) {
            return Result.failure(new BCError.ConfigError("CONFIG_NOT_FOUND", 
                "Configuration not found: " + configName));
        }
        
        configurations.remove(configName);
        LOGGER.info("Unregistered configuration: {}", configName);
        return Result.success(null);
    }
    
    /**
     * Manually triggers a reload of a specific configuration.
     * 
     * @param configName Name of the configuration to reload
     * @return Result indicating success or failure
     */
    public Result<Void, BCError.ConfigError> reloadConfiguration(String configName) {
        ConfigEntry entry = configurations.get(configName);
        if (entry == null) {
            return Result.failure(new BCError.ConfigError("CONFIG_NOT_FOUND", 
                "Configuration not found: " + configName));
        }
        
        try {
            LOGGER.info("Manually reloading configuration: {}", configName);
            entry.reloadCallback.accept(entry.filePath);
            entry.lastModified = System.currentTimeMillis();
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new BCError.ConfigError("RELOAD_FAILED", 
                "Failed to reload configuration '" + configName + "': " + e.getMessage()));
        }
    }
    
    /**
     * Reloads all registered configurations.
     * 
     * @return Result indicating overall success or failure
     */
    public Result<Void, BCError.ConfigError> reloadAllConfigurations() {
        LOGGER.info("Reloading all configurations...");
        
        int successCount = 0;
        int totalCount = configurations.size();
        StringBuilder errors = new StringBuilder();
        
        for (String configName : configurations.keySet()) {
            Result<Void, BCError.ConfigError> result = reloadConfiguration(configName);
            if (result.isSuccess()) {
                successCount++;
            } else {
                errors.append(configName).append(": ").append(result.getError().getMessage()).append("; ");
            }
        }
        
        if (successCount == totalCount) {
            LOGGER.info("Successfully reloaded all {} configurations", totalCount);
            return Result.success(null);
        } else {
            String errorMessage = String.format("Reloaded %d/%d configurations. Failures: %s", 
                successCount, totalCount, errors.toString());
            return Result.failure(new BCError.ConfigError("PARTIAL_RELOAD_FAILURE", errorMessage));
        }
    }
    
    /**
     * Starts the file watching service.
     */
    private void startWatching() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        executorService.submit(this::watchForChanges);
        LOGGER.info("Started configuration file watching service");
    }
    
    /**
     * Main file watching loop.
     */
    private void watchForChanges() {
        while (isRunning) {
            try {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue; // No events, continue polling
                }
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        LOGGER.warn("File watch event overflow detected");
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path filename = pathEvent.context();
                    
                    // Check if this file change affects any of our registered configurations
                    handleFileChange(key, filename, kind);
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    LOGGER.warn("Watch key is no longer valid, removing from registry");
                    // Remove the invalid key from our map
                    watchKeys.values().removeIf(k -> k.equals(key));
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("Error in file watching loop", e);
            }
        }
        
        LOGGER.info("File watching service stopped");
    }
    
    /**
     * Handles a file change event.
     */
    private void handleFileChange(WatchKey key, Path filename, WatchEvent.Kind<?> kind) {
        // Find the directory this watch key belongs to
        String changedDir = null;
        for (Map.Entry<String, WatchKey> entry : watchKeys.entrySet()) {
            if (entry.getValue().equals(key)) {
                changedDir = entry.getKey();
                break;
            }
        }
        
        if (changedDir == null) {
            return;
        }
        
        Path fullPath = Paths.get(changedDir, filename.toString());
        
        // Check if this file matches any of our registered configurations
        for (Map.Entry<String, ConfigEntry> entry : configurations.entrySet()) {
            ConfigEntry config = entry.getValue();
            
            if (config.filePath.equals(fullPath)) {
                // Debounce rapid file changes (some editors create multiple events)
                long now = System.currentTimeMillis();
                if (now - config.lastModified < 1000) { // 1 second debounce
                    continue;
                }
                
                LOGGER.info("Configuration file changed: {} ({})", entry.getKey(), kind);
                
                // Schedule reload with a small delay to ensure file writing is complete
                executorService.schedule(() -> {
                    try {
                        config.reloadCallback.accept(config.filePath);
                        config.lastModified = System.currentTimeMillis();
                        LOGGER.info("Successfully reloaded configuration: {}", entry.getKey());
                    } catch (Exception e) {
                        LOGGER.error("Failed to reload configuration '{}': {}", entry.getKey(), e.getMessage(), e);
                    }
                }, 250, TimeUnit.MILLISECONDS);
                
                break;
            }
        }
    }
    
    /**
     * Gets the number of registered configurations.
     * 
     * @return Number of registered configurations
     */
    public int getRegisteredConfigurationCount() {
        return configurations.size();
    }
    
    /**
     * Checks if a configuration is registered.
     * 
     * @param configName Name of the configuration
     * @return True if registered
     */
    public boolean isConfigurationRegistered(String configName) {
        return configurations.containsKey(configName);
    }
    
    /**
     * Shuts down the configuration service.
     */
    public void shutdown() {
        LOGGER.info("Shutting down configuration service...");
        
        isRunning = false;
        
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            LOGGER.error("Error closing watch service", e);
        }
        
        configurations.clear();
        watchKeys.clear();
        
        LOGGER.info("Configuration service shutdown complete");
    }
    
    /**
     * Internal class to hold configuration entry information.
     */
    private static class ConfigEntry {
        final Path filePath;
        final Consumer<Path> reloadCallback;
        volatile long lastModified;
        
        ConfigEntry(Path filePath, Consumer<Path> reloadCallback, long lastModified) {
            this.filePath = filePath;
            this.reloadCallback = reloadCallback;
            this.lastModified = lastModified;
        }
    }
}
