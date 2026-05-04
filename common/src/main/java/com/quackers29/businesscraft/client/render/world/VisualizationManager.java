package com.quackers29.businesscraft.client.render.world;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizationManager {
    private static final VisualizationManager INSTANCE = new VisualizationManager();
    
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
            if (duration <= 0) return Long.MAX_VALUE;
            return Math.max(0, duration - (currentTime - startTime));
        }
        
        public float getProgress(long currentTime) {
            if (duration <= 0) return 0.0f;
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
    
    public static final String TYPE_PLATFORM = "platform";
    public static final String TYPE_ROUTE = "route";
    public static final String TYPE_DEBUG = "debug";
    public static final String TYPE_TERRITORY = "territory";
    public static final String TYPE_QUEST = "quest";
    public static final String TYPE_TOWN_BOUNDARY = "town_boundary";
    
    private VisualizationManager() {
        // Initialize default visualization types
        registerVisualizationType(new VisualizationTypeConfig(TYPE_PLATFORM)
            .defaultDuration(600)
            .maxActive(50)
            .allowMultiple(false));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_ROUTE)
            .defaultDuration(1200)
            .maxActive(20)
            .allowMultiple(true));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_DEBUG)
            .defaultDuration(0)
            .maxActive(200)
            .allowMultiple(true));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_TERRITORY)
            .defaultDuration(2400)
            .maxActive(30)
            .allowMultiple(false));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_QUEST)
            .defaultDuration(1800)
            .maxActive(10)
            .allowMultiple(false));
        
        registerVisualizationType(new VisualizationTypeConfig(TYPE_TOWN_BOUNDARY)
            .defaultDuration(600)
            .maxActive(50)
            .allowMultiple(false));
    }
    
    public static VisualizationManager getInstance() {
        return INSTANCE;
    }
    
    public void registerVisualizationType(VisualizationTypeConfig config) {
        typeConfigs.put(config.getTypeName(), config);
        activeVisualizations.put(config.getTypeName(), Collections.synchronizedList(new ArrayList<>()));
    }
    
    public void registerRenderer(String type, WorldVisualizationRenderer renderer) {
        registeredRenderers.put(type, renderer);
    }
    
    public void showVisualization(String type, BlockPos position, Object data) {
        VisualizationTypeConfig config = typeConfigs.get(type);
        if (config == null) {
            throw new IllegalArgumentException("Unknown visualization type: " + type);
        }
        
        showVisualization(type, position, data, config.getDefaultDuration());
    }
    
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
            typeVisualizations.sort(Comparator.comparingLong(VisualizationEntry::getStartTime));
            typeVisualizations.remove(0);
        }
        
        VisualizationEntry entry = new VisualizationEntry(type, position, data, currentTime, duration);
        typeVisualizations.add(entry);
    }
    
    public void hideVisualization(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            typeVisualizations.clear();
        }
    }
    
    public void hideVisualizationAt(String type, BlockPos position) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            typeVisualizations.removeIf(entry -> entry.getPosition().equals(position));
        }
    }
    
    public boolean shouldShowVisualization(String type, BlockPos position) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations == null) {
            return false;
        }
        
        long currentTime = getCurrentTime();
        
        return typeVisualizations.stream()
            .anyMatch(entry -> entry.getPosition().equals(position) && !entry.isExpired(currentTime));
    }
    
    public List<VisualizationEntry> getActiveVisualizations(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(typeVisualizations);
    }
    
    public List<VisualizationEntry> getVisualizationsAt(String type, BlockPos position) {
        return getActiveVisualizations(type).stream()
            .filter(entry -> entry.getPosition().equals(position))
            .filter(entry -> !entry.isExpired(getCurrentTime()))
            .toList();
    }
    
    public void cleanupExpired() {
        long currentTime = getCurrentTime();
        
        for (List<VisualizationEntry> typeVisualizations : activeVisualizations.values()) {
            typeVisualizations.removeIf(entry -> entry.isExpired(currentTime));
        }
    }
    
    public void cleanupExpired(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            long currentTime = getCurrentTime();
            typeVisualizations.removeIf(entry -> entry.isExpired(currentTime));
        }
    }
    
    public int getTotalActiveCount() {
        return activeVisualizations.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    public int getActiveCount(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        return typeVisualizations != null ? typeVisualizations.size() : 0;
    }
    
    public void clearAll() {
        for (List<VisualizationEntry> typeVisualizations : activeVisualizations.values()) {
            typeVisualizations.clear();
        }
    }
    
    public void clearType(String type) {
        List<VisualizationEntry> typeVisualizations = activeVisualizations.get(type);
        if (typeVisualizations != null) {
            typeVisualizations.clear();
        }
    }
    
    public void onLevelUnload() {
        for (Map.Entry<String, VisualizationTypeConfig> entry : typeConfigs.entrySet()) {
            if (entry.getValue().shouldCleanupOnLevelUnload()) {
                clearType(entry.getKey());
            }
        }
        
        for (WorldVisualizationRenderer renderer : registeredRenderers.values()) {
            renderer.cleanup();
        }
    }
    
    public WorldVisualizationRenderer getRenderer(String type) {
        return registeredRenderers.get(type);
    }
    
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(typeConfigs.keySet());
    }
    
    public VisualizationTypeConfig getTypeConfig(String type) {
        return typeConfigs.get(type);
    }
    
    protected long getCurrentTime() {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof Level level) {
                return level.getGameTime();
            }
        }
        return System.currentTimeMillis() / 50;
    }
    
    @Deprecated
    public void registerPlayerExitUI(BlockPos townBlockPos, long gameTime) {
        showVisualization(TYPE_PLATFORM, townBlockPos, null, 600);
    }
    
    @Deprecated
    public boolean shouldShowVisualization(BlockPos townBlockPos, long currentGameTime) {
        return shouldShowVisualization(TYPE_PLATFORM, townBlockPos);
    }
}
