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
    
    public static synchronized ConfigurationService getInstance() {
        if (instance == null) {
            instance = new ConfigurationService();
        }
        return instance;
    }
    
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
            if (!Files.exists(filePath)) {
                return Result.failure(new BCError.ConfigError("FILE_NOT_FOUND", 
                    "Configuration file does not exist: " + filePath));
            }
            
            Path parentDir = filePath.getParent();
            if (parentDir == null) {
                return Result.failure(new BCError.ConfigError("INVALID_DIRECTORY", 
                    "Cannot determine parent directory for: " + filePath));
            }
            
            String dirKey = parentDir.toString();
            if (!watchKeys.containsKey(dirKey)) {
                WatchKey watchKey = parentDir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
                watchKeys.put(dirKey, watchKey);
                LOGGER.info("Registered file watcher for directory: {}", parentDir);
            }
            
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
    
    public Result<Void, BCError.ConfigError> unregisterConfiguration(String configName) {
        if (configName == null || !configurations.containsKey(configName)) {
            return Result.failure(new BCError.ConfigError("CONFIG_NOT_FOUND", 
                "Configuration not found: " + configName));
        }
        
        configurations.remove(configName);
        LOGGER.info("Unregistered configuration: {}", configName);
        return Result.success(null);
    }
    
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
    
    private void startWatching() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        executorService.submit(this::watchForChanges);
        LOGGER.info("Started configuration file watching service");
    }
    
    private void watchForChanges() {
        while (isRunning) {
            try {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
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
                    
                    handleFileChange(key, filename, kind);
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    LOGGER.warn("Watch key is no longer valid, removing from registry");
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
    
    private void handleFileChange(WatchKey key, Path filename, WatchEvent.Kind<?> kind) {
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
        
        for (Map.Entry<String, ConfigEntry> entry : configurations.entrySet()) {
            ConfigEntry config = entry.getValue();
            
            if (config.filePath.equals(fullPath)) {
                long now = System.currentTimeMillis();
                if (now - config.lastModified < 1000) {
                    continue;
                }
                
                LOGGER.info("Configuration file changed: {} ({})", entry.getKey(), kind);
                
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
    
    public int getRegisteredConfigurationCount() {
        return configurations.size();
    }
    
    public boolean isConfigurationRegistered(String configName) {
        return configurations.containsKey(configName);
    }
    
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
