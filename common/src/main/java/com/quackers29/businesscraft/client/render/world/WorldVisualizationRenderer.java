package com.quackers29.businesscraft.client.render.world;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RenderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class WorldVisualizationRenderer {

    public static class RenderConfig {
        private int maxRenderDistance = 128;
        private int chunkRadius = 8;
        private boolean enableDistanceCulling = true;
        private boolean enableChunkCulling = true;
        private String renderStage = RenderHelper.RenderStage.AFTER_TRANSLUCENT_BLOCKS;
        
        public RenderConfig maxRenderDistance(int distance) {
            this.maxRenderDistance = distance;
            return this;
        }
        
        public RenderConfig chunkRadius(int radius) {
            this.chunkRadius = radius;
            return this;
        }
        
        public RenderConfig distanceCulling(boolean enable) {
            this.enableDistanceCulling = enable;
            return this;
        }
        
        public RenderConfig chunkCulling(boolean enable) {
            this.enableChunkCulling = enable;
            return this;
        }
        
        public RenderConfig renderStage(String stage) {
            this.renderStage = stage;
            return this;
        }
        
        public int getMaxRenderDistance() { return maxRenderDistance; }
        public int getChunkRadius() { return chunkRadius; }
        public boolean isDistanceCullingEnabled() { return enableDistanceCulling; }
        public boolean isChunkCullingEnabled() { return enableChunkCulling; }
        public String getRenderStage() { return renderStage; }
    }
    
    public static class VisualizationData {
        private final String type;
        private final BlockPos position;
        private final Object data;
        private final long creationTime;
        
        public VisualizationData(String type, BlockPos position, Object data) {
            this.type = type;
            this.position = position;
            this.data = data;
            this.creationTime = System.currentTimeMillis();
        }
        
        public String getType() { return type; }
        public BlockPos getPosition() { return position; }
        public Object getData() { return data; }
        public long getCreationTime() { return creationTime; }
        
        @SuppressWarnings("unchecked")
        public <T> T getData(Class<T> clazz) {
            return clazz.isInstance(data) ? (T) data : null;
        }
    }
    
    protected final RenderConfig config;
    
    public WorldVisualizationRenderer() {
        this(new RenderConfig());
    }
    
    public WorldVisualizationRenderer(RenderConfig config) {
        this.config = config;
    }
    
    public final void render(String renderStage, float partialTick, Object renderEvent) {
        RenderHelper renderHelper = PlatformAccess.getRender();
        if (renderHelper == null) {
            return;
        }
        
        // Check if we should render at this stage
        String expectedStage = config.getRenderStage();
        boolean stageMatches = renderHelper.isRenderStage(renderEvent, expectedStage);
        
        if (!stageMatches) {
            return;
        }
        
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) return;
        
        Object levelObj = clientHelper.getClientLevel();
        if (!(levelObj instanceof Level level) || !level.isClientSide) {
            return;
        }
        
        Object playerObj = clientHelper.getClientPlayer();
        if (playerObj == null) {
            return;
        }
        
        net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) playerObj;
        
        // Pre-render setup
        if (!shouldRender(level, player.blockPosition())) {
            return;
        }
        
        onPreRender(renderEvent, level);
        
        // Get visualizations to render
        List<VisualizationData> visualizations = getVisualizations(level, player.blockPosition());
        
        if (visualizations.isEmpty()) {
            onPostRender(renderEvent, level);
            return;
        }
        
        // Render each visualization with distance culling
        BlockPos playerPos = player.blockPosition();
        int maxDistanceSquared = config.getMaxRenderDistance() * config.getMaxRenderDistance();
        
        for (VisualizationData visualization : visualizations) {
            if (config.isDistanceCullingEnabled()) {
                if (visualization.getPosition().distSqr(playerPos) > maxDistanceSquared) {
                    continue;
                }
            }
            
            if (config.isChunkCullingEnabled()) {
                if (!isChunkLoaded(level, visualization.getPosition())) {
                    continue;
                }
            }
            
            renderVisualization(renderEvent, visualization);
        }
        
        // Post-render cleanup
        onPostRender(renderEvent, level);
    }

    protected boolean shouldRender(Level level, BlockPos playerPos) {
        return true;
    }

    protected void onPreRender(Object renderEvent, Level level) {
    }

    protected void onPostRender(Object renderEvent, Level level) {
    }

    protected abstract List<VisualizationData> getVisualizations(Level level, BlockPos playerPos);

    protected abstract void renderVisualization(Object renderEvent, VisualizationData visualization);

    public void cleanup() {
    }

    protected boolean isChunkLoaded(Level level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return level.hasChunk(chunkX, chunkZ);
    }
    
    protected void iterateNearbyChunks(Level level, BlockPos playerPos, ChunkProcessor chunkProcessor) {
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        
        for (int chunkX = playerChunkX - config.getChunkRadius(); 
             chunkX <= playerChunkX + config.getChunkRadius(); chunkX++) {
            for (int chunkZ = playerChunkZ - config.getChunkRadius(); 
                 chunkZ <= playerChunkZ + config.getChunkRadius(); chunkZ++) {
                
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                
                var chunk = level.getChunk(chunkX, chunkZ);
                chunkProcessor.processChunk(chunk, chunkX, chunkZ);
            }
        }
    }
    
    @FunctionalInterface
    protected interface ChunkProcessor {
        void processChunk(net.minecraft.world.level.chunk.LevelChunk chunk, int chunkX, int chunkZ);
    }

    public RenderConfig getConfig() {
        return config;
    }

    protected boolean isWithinRenderDistance(BlockPos pos, BlockPos playerPos) {
        if (!config.isDistanceCullingEnabled()) {
            return true;
        }
        
        int maxDistanceSquared = config.getMaxRenderDistance() * config.getMaxRenderDistance();
        return pos.distSqr(playerPos) <= maxDistanceSquared;
    }
    
}
