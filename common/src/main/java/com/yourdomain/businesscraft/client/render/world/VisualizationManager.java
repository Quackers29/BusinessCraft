package com.yourdomain.businesscraft.client.render.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic visualization management system that tracks multiple types of visualizations
 * with independent timing and state management.
 * 
 * This replaces and extends the original PlatformVisualizationManager to support
 * multiple visualization types throughout the mod.
 */
public class VisualizationManager {
    private static final VisualizationManager INSTANCE = new VisualizationManager();
    
    /**
     * Data structure for a single visualization entry
     */
    public static class VisualizationEntry {
        private final String type;
        private final BlockPos position;
        private final Object data;
        private final long startTime;
        private final long duration;
        private final Map<String, Object> properties;
        
        public VisualizationEntry(String type, BlockPos position, Object data, long startTime, long duration) {
            this.type = type;
            this.position = position;
            this.data = data;
            this.startTime = startTime;
            this.duration = duration;
            this.properties = new HashMap<>();
        }
        
        public String getType() { return type; }
        public BlockPos getPosition() { return position; }
        public Object getData() { return data; }
        public long getStartTime() { return startTime; }
        public long getDuration() { return duration; }
        public Map<String, Object> getProperties() { return properties; }
        
        public boolean isExpired(long currentTime) {
            return duration > 0 && (currentTime - startTime) > duration;
        }
        
        public long getRemainingTime(long currentTime) {
            if (duration <= 0) return Long.MAX_VALUE; // Permanent visualization
            return Math.max(0, duration - (currentTime - startTime));
        }
        
        public float getProgress(long currentTime) {
            if (duration <= 0) return 0.0f; // Permanent visualization
            return Math.min(1.0f, (float)(currentTime - startTime) / duration);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getData(Class<T> clazz) {
            return clazz.isInstance(data) ? (T) data : null;
        }
        
        public VisualizationEntry setProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getProperty(String key, Class<T> clazz) {
            Object value = properties.get(key);
            return clazz.isInstance(value) ? (T) value : null;
        }
    }
    
    /**
     * Configuration for visualization types
     */
    public static class VisualizationTypeConfig {
        private final String typeName;
        private long defaultDuration;
        private int maxActiveVisualizations;
        private boolean allowMultiplePerPosition;
        private boolean cleanupOnLevelUnload;
        
        public VisualizationTypeConfig(String typeName) {
            this.typeName = typeName;
            this.defaultDuration = 600; // 30 seconds in ticks
            this.maxActiveVisualizations = 100;
            this.allowMultiplePerPosition = false;
            this.cleanupOnLevelUnload = true;
        }
        
        public VisualizationTypeConfig defaultDuration(long ticks) {
            this.defaultDuration = ticks;
            return this;
        }
        
        public VisualizationTypeConfig maxActive(int max) {
            this.maxActiveVisualizations = max;
            return this;
        }
        
        public VisualizationTypeConfig allowMultiple(boolean allow) {
            this.allowMultiplePerPosition = allow;
            return this;
        }
        
        public VisualizationTypeConfig cleanupOnUnload(boolean cleanup) {
            this.cleanupOnLevelUnload = cleanup;
            return this;
        }
        
        // Getters
        public String getTypeName() { return typeName; }
        public long getDefaultDuration() { return defaultDuration; }
        public int getMaxActiveVisualizations() { return maxActiveVisualizations; }
        public boolean isMultiplePerPositionAllowed() { return allowMultiplePerPosition; }
        public boolean shouldCleanupOnLevelUnload() { return cleanupOnLevelUnload; }
    }
    
    // Thread-safe storage for visualization entries and configurations
    private final Map<String, VisualizationTypeConfig> typeConfigs = new ConcurrentHashMap<>();
    private final Map<String, List<VisualizationEntry>> activeVisualizations = new ConcurrentHashMap<>();
    private final Map<String, WorldVisualizationRenderer> registeredRenderers = new ConcurrentHashMap<>();
    
    // Predefined visualization types
    public static final String TYPE_PLATFORM = "platform";
    public static final String TYPE_ROUTE = "route";
    public static final String TYPE_DEBUG = "debug";
    public static final String TYPE_TERRITORY = "territory";
    public static final String TYPE_QUEST = "quest";
    public static final String TYPE_TOWN_BOUNDARY = "town_boundary";
    
    private VisualizationManager() {
        // Initialize default visualization types
        registerVisualizationType(new VisualizationTypeConfig(TYPE_PLATFORM)
            .defaultDuration(600) // 30 seconds
            .maxActive(50)
            .allowMultiple(false));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_ROUTE)
            .defaultDuration(1200) // 60 seconds
            .maxActive(20)
            .allowMultiple(true));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_DEBUG)
            .defaultDuration(0) // Permanent until manually removed
            .maxActive(200)
            .allowMultiple(true));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_TERRITORY)
            .defaultDuration(2400) // 120 seconds
            .maxActive(30)
            .allowMultiple(false));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_QUEST)
            .defaultDuration(1800) // 90 seconds
            .maxActive(10)
            .allowMultiple(false));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_TOWN_BOUNDARY)
            .defaultDuration(600) // 30 seconds - same as platforms
            .maxActive(50)
            .allowMultiple(false)); // One boundary per town
    }
    
    public static VisualizationManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Registers a new visualization type with configuration
     */
    public void registerVisualizationType(VisualizationTypeConfig config) {
        typeConfigs.put(config.getTypeName(), config);
        activeVisualizations.put(config.getTypeName(), Collections.synchronizedList(new ArrayList<>()));
    }
    
    /**
     * Registers a renderer for a specific visualization type
     */
    public void registerRenderer(String type, WorldVisualizationRenderer renderer) {
        registeredRenderers.put(type, renderer);
    }
    
    /**
     * Shows a visualization with default duration
     */
    public void showVisualization(String type, BlockPos position, Object data) {
        VisualizationTypeConfig config = typeConfigs.get(type);
        if (config == null) {
            throw new IllegalArgumentException("Unknown visualization type: " + type);
        }
        
        showVisualization(type, position, data, config.getDefaultDuration());
    }
    
    /**
     * Shows a visualization with custom duration
     */
    public void showVisualization(String type, BlockPos position, Object data, long duration) {
        VisualizationTypeConfig config = typeConfigs.get(type);
        if (config == null) {
            throw new IllegalArgumentException("Unknown visualization type: " + type);
        }
        
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations == null) {
            return;
        }
        
        long currentTime = getCurrentTime();
        
        // Remove existing visualization at this position if not allowing multiples
        if (!config.isMultiplePerPositionAllowed()) {
            typeVisualizations.removeIf(entry -> entry.getPosition().equals(position));
        }
        
        // Check max active limit
        if (typeVisualizations.size() >= config.getMaxActiveVisualizations()) {
            // Remove oldest visualization
            typeVisualizations.sort(Comparator.comparingLong(VisualizationEntry::getStartTime));
            typeVisualizations.remove(0);
        }
        
        // Add new visualization
        VisualizationEntry entry = new VisualizationEntry(type, position, data, currentTime, duration);
        typeVisualizations.add(entry);
    }
    
    /**
     * Hides all visualizations of a specific type
     */
    public void hideVisualization(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            typeVisualizations.clear();
        }
    }
    
    /**
     * Hides visualizations at a specific position
     */
    public void hideVisualizationAt(String type, BlockPos position) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            typeVisualizations.removeIf(entry -> entry.getPosition().equals(position));
        }
    }
    
    /**
     * Checks if a visualization should be shown
     */
    public boolean shouldShowVisualization(String type, BlockPos position) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations == null) {
            return false;
        }
        
        long currentTime = getCurrentTime();
        
        return typeVisualizations.stream()
            .anyMatch(entry -> entry.getPosition().equals(position) && !entry.isExpired(currentTime));
    }
    
    /**
     * Gets all active visualizations of a specific type
     */
    public List<VisualizationEntry> getActiveVisualizations(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations == null) {
            return Collections.emptyList();
        }
        
        // Return a copy to avoid concurrent modification
        return new ArrayList<>(typeVisualizations);
    }
    
    /**
     * Gets all active visualizations at a specific position
     */
    public List<VisualizationEntry> getVisualizationsAt(String type, BlockPos position) {
        return getActiveVisualizations(type).stream()
            .filter(entry -> entry.getPosition().equals(position))
            .filter(entry -> !entry.isExpired(getCurrentTime()))
            .toList();
    }
    
    /**
     * Cleans up expired visualizations for all types
     */
    public void cleanupExpired() {
        long currentTime = getCurrentTime();
        
        for (List<VisualizationEntry> typeVisualizations : activeVisualizations.values()) {
            typeVisualizations.removeIf(entry -> entry.isExpired(currentTime));
        }
    }
    
    /**
     * Cleans up expired visualizations for a specific type
     */
    public void cleanupExpired(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            long currentTime = getCurrentTime();
            typeVisualizations.removeIf(entry -> entry.isExpired(currentTime));
        }
    }
    
    /**
     * Gets the total number of active visualizations across all types
     */
    public int getTotalActiveCount() {
        return activeVisualizations.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Gets the number of active visualizations for a specific type
     */
    public int getActiveCount(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        return typeVisualizations != null ? typeVisualizations.size() : 0;
    }
    
    /**
     * Clears all visualizations (useful for level changes)
     */
    public void clearAll() {
        for (List<VisualizationEntry> typeVisualizations : activeVisualizations.values()) {
            typeVisualizations.clear();
        }
    }
    
    /**
     * Clears all visualizations of a specific type
     */
    public void clearType(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            typeVisualizations.clear();
        }
    }
    
    /**
     * Called when a level is unloaded to clean up visualizations
     */
    public void onLevelUnload() {
        for (Map.Entry<String, VisualizationTypeConfig> entry : typeConfigs.entrySet()) {
            if (entry.getValue().shouldCleanupOnLevelUnload()) {
                clearType(entry.getKey());
            }
        }
        
        // Cleanup renderers
        for (WorldVisualizationRenderer renderer : registeredRenderers.values()) {
            renderer.cleanup();
        }
    }
    
    /**
     * Gets the renderer for a specific visualization type
     */
    public WorldVisualizationRenderer getRenderer(String type) {
        return registeredRenderers.get(type);
    }
    
    /**
     * Gets all registered visualization types
     */
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(typeConfigs.keySet());
    }
    
    /**
     * Gets the configuration for a visualization type
     */
    public VisualizationTypeConfig getTypeConfig(String type) {
        return typeConfigs.get(type);
    }
    
    /**
     * Helper method to get current game time
     * Can be overridden for testing or different timing sources
     */
    protected long getCurrentTime() {
        Level level = net.minecraft.client.Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : System.currentTimeMillis() / 50; // Fallback to system time in ticks
    }
    
    /**
     * Platform compatibility: Bridge method for old PlatformVisualizationManager API
     */
    @Deprecated
    public void registerPlayerExitUI(BlockPos townBlockPos, long gameTime) {
        showVisualization(TYPE_PLATFORM, townBlockPos, null, 600); // 30 seconds
    }
    
    /**
     * Platform compatibility: Bridge method for old PlatformVisualizationManager API
     */
    @Deprecated
    public boolean shouldShowVisualization(BlockPos townBlockPos, long currentGameTime) {
        return shouldShowVisualization(TYPE_PLATFORM, townBlockPos);
    }
}