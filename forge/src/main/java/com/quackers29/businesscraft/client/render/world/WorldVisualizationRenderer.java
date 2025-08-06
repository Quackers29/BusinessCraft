package com.quackers29.businesscraft.client.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.quackers29.businesscraft.platform.EventHelper;

import java.util.List;

/**
 * Abstract base class for all world overlay visualization renderers.
 * 
 * Provides a common framework for rendering 3D overlays in the world, including:
 * - Common setup and cleanup operations
 * - Render state management
 * - Distance-based culling
 * - Integration with the Forge rendering pipeline
 * 
 * Implementations should extend this class and provide specific visualization logic.
 */
public abstract class WorldVisualizationRenderer {
    
    /**
     * Configuration for visualization rendering
     */
    public static class RenderConfig {
        private int maxRenderDistance = 128;
        private int chunkRadius = 8;
        private boolean enableDistanceCulling = true;
        private boolean enableChunkCulling = true;
        private String renderStage = "after_translucent_blocks";
        
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
        
        // Getters
        public int getMaxRenderDistance() { return maxRenderDistance; }
        public int getChunkRadius() { return chunkRadius; }
        public boolean isDistanceCullingEnabled() { return enableDistanceCulling; }
        public boolean isChunkCullingEnabled() { return enableChunkCulling; }
        public String getRenderStage() { return renderStage; }
    }
    
    /**
     * Data structure representing a single visualization item
     */
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
    
    /**
     * Registers this renderer with the EventHelper system
     * 
     * @param eventHelper The event helper instance to register with
     */
    public void register(EventHelper eventHelper) {
        eventHelper.registerRenderLevelEvent(this::render);
    }
    
    /**
     * Main render method called by the event system
     * 
     * @param poseStack The pose stack for rendering
     * @param bufferSource The buffer source for rendering
     * @param level The level being rendered
     */
    public final void render(PoseStack poseStack, MultiBufferSource bufferSource, Level level) {
        // The stage checking is now handled by the EventHelper registration
        
        if (level == null || !level.isClientSide) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        
        // Pre-render setup
        if (!shouldRender(level, mc.player.blockPosition())) {
            return;
        }
        
        onPreRender(poseStack, bufferSource, level);
        
        // Get visualizations to render
        List<VisualizationData> visualizations = getVisualizations(level, mc.player.blockPosition());
        
        if (visualizations.isEmpty()) {
            onPostRender(poseStack, bufferSource, level);
            return;
        }
        
        // Render each visualization with distance culling
        BlockPos playerPos = mc.player.blockPosition();
        int maxDistanceSquared = config.getMaxRenderDistance() * config.getMaxRenderDistance();
        
        for (VisualizationData visualization : visualizations) {
            // Distance culling
            if (config.isDistanceCullingEnabled()) {
                if (visualization.getPosition().distSqr(playerPos) > maxDistanceSquared) {
                    continue;
                }
            }
            
            // Chunk culling
            if (config.isChunkCullingEnabled()) {
                if (!isChunkLoaded(level, visualization.getPosition())) {
                    continue;
                }
            }
            
            // Render this visualization
            renderVisualization(poseStack, bufferSource, level, visualization);
        }
        
        // Post-render cleanup
        onPostRender(poseStack, bufferSource, level);
    }
    
    /**
     * Determines whether this renderer should render at all
     * 
     * @param level The current level
     * @param playerPos The player's position
     * @return true if rendering should proceed
     */
    protected boolean shouldRender(Level level, BlockPos playerPos) {
        return true; // Default: always render
    }
    
    /**
     * Called before any visualizations are rendered
     * Use this for global render state setup
     * 
     * @param poseStack The pose stack for rendering
     * @param bufferSource The buffer source for rendering
     * @param level The current level
     */
    protected void onPreRender(PoseStack poseStack, MultiBufferSource bufferSource, Level level) {
        // Default: no pre-render setup
    }
    
    /**
     * Called after all visualizations are rendered
     * Use this for global render state cleanup
     * 
     * @param poseStack The pose stack for rendering
     * @param bufferSource The buffer source for rendering
     * @param level The current level
     */
    protected void onPostRender(PoseStack poseStack, MultiBufferSource bufferSource, Level level) {
        // Default: no post-render cleanup
    }
    
    /**
     * Gets the list of visualizations to render for the current frame
     * 
     * @param level The current level
     * @param playerPos The player's position
     * @return List of visualization data to render
     */
    protected abstract List<VisualizationData> getVisualizations(Level level, BlockPos playerPos);
    
    /**
     * Renders a single visualization
     * 
     * @param poseStack The pose stack for rendering
     * @param bufferSource The buffer source for rendering
     * @param level The current level
     * @param visualization The visualization to render
     */
    protected abstract void renderVisualization(PoseStack poseStack, MultiBufferSource bufferSource, Level level, VisualizationData visualization);
    
    /**
     * Called when the level is unloaded to clean up any resources
     */
    public void cleanup() {
        // Default: no cleanup needed
    }
    
    /**
     * Helper method to check if a chunk is loaded
     */
    protected boolean isChunkLoaded(Level level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return level.hasChunk(chunkX, chunkZ);
    }
    
    /**
     * Helper method to iterate through loaded chunks near the player
     * 
     * @param level The current level
     * @param playerPos The player's position
     * @param chunkProcessor Callback to process each chunk
     */
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
    
    /**
     * Functional interface for chunk processing
     */
    @FunctionalInterface
    protected interface ChunkProcessor {
        void processChunk(net.minecraft.world.level.chunk.LevelChunk chunk, int chunkX, int chunkZ);
    }
    
    /**
     * Helper method to get the render configuration
     */
    public RenderConfig getConfig() {
        return config;
    }
    
    /**
     * Helper method to check if a position is within render distance
     */
    protected boolean isWithinRenderDistance(BlockPos pos, BlockPos playerPos) {
        if (!config.isDistanceCullingEnabled()) {
            return true;
        }
        
        int maxDistanceSquared = config.getMaxRenderDistance() * config.getMaxRenderDistance();
        return pos.distSqr(playerPos) <= maxDistanceSquared;
    }
    
    /**
     * Static helper method for common render state setup
     */
    protected static void setupCommonRenderState() {
        // Common setup can be added here if needed by multiple renderers
    }
    
    /**
     * Static helper method for common render state cleanup
     */
    protected static void cleanupCommonRenderState() {
        // Common cleanup can be added here if needed by multiple renderers
    }
}