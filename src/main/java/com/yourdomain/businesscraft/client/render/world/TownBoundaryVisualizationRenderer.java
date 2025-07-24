package com.yourdomain.businesscraft.client.render.world;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.ui.BoundarySyncRequestPacket;
import com.yourdomain.businesscraft.debug.DebugConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Town boundary visualization renderer that shows circular boundaries around towns
 * based on their population. Integrates with the existing visualization system.
 */
public class TownBoundaryVisualizationRenderer extends WorldVisualizationRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBoundaryVisualizationRenderer.class);
    
    // Simple green boundary color
    private static final LineRenderer3D.Color BOUNDARY_COLOR = LineRenderer3D.Color.GREEN;
    
    // Global sync tracking (shared across all boundary visualizations)
    private static long lastGlobalSyncTime = 0;
    private static final long GLOBAL_SYNC_INTERVAL_MS = 2000; // Request sync every 2 seconds
    
    /**
     * Data structure for town boundary visualization with dynamic updates
     */
    public static class TownBoundaryVisualizationData {
        private final BlockPos townPosition;
        private int cachedBoundaryRadius;
        private long lastUpdateTime;
        private static final long UPDATE_INTERVAL_MS = 1000; // Update every 1 second
        
        public TownBoundaryVisualizationData(BlockPos townPosition) {
            this.townPosition = townPosition;
            this.cachedBoundaryRadius = calculateRadius();
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public int getBoundaryRadius() {
            long currentTime = System.currentTimeMillis();
            
            // Update cached radius from cache
            if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
                cachedBoundaryRadius = calculateRadius();
                lastUpdateTime = currentTime;
            }
            return cachedBoundaryRadius;
        }
        
        public BlockPos getTownPosition() {
            return townPosition;
        }
        
        private int calculateRadius() {
            // Get boundary radius from the cache that was set when visualization packet was received
            int boundaryRadius = TownBoundaryPopulationCache.getBoundaryRadius(townPosition, 5);
            int newRadius = Math.max(boundaryRadius, 5); // Minimum 5 blocks radius
            
            // Debug log to see what boundary radius we're getting
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Calculating boundary radius: cached={}, final={}, pos={}", 
                boundaryRadius, newRadius, townPosition);
            
            // Debug log when radius changes
            if (newRadius != cachedBoundaryRadius) {
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                    "Boundary radius updated: {} -> {} (server boundary: {})", 
                    cachedBoundaryRadius, newRadius, boundaryRadius);
            }
            
            return newRadius;
        }
    }
    
    public TownBoundaryVisualizationRenderer() {
        super(new RenderConfig()
            .maxRenderDistance(256) // Larger than platforms to show boundaries at distance
            .chunkRadius(16)        // Wider search for town boundaries
            .renderStage(RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS));
    }
    
    @Override
    protected List<VisualizationData> getVisualizations(Level level, BlockPos playerPos) {
        List<VisualizationData> visualizations = new ArrayList<>();
        
        VisualizationManager manager = VisualizationManager.getInstance();
        
        // Iterate through nearby chunks to find TownBlockEntities (same as platforms)
        iterateNearbyChunks(level, playerPos, (chunk, chunkX, chunkZ) -> {
            chunk.getBlockEntities().forEach((pos, blockEntity) -> {
                // Check distance from player
                if (!isWithinRenderDistance(pos, playerPos)) {
                    return;
                }
                
                // Check if this is a TownBlockEntity with active boundary visualization
                if (blockEntity instanceof TownBlockEntity townBlockEntity) {
                    // Show boundary when town boundary visualization is active (triggered same time as platforms)
                    if (manager.shouldShowVisualization(VisualizationManager.TYPE_TOWN_BOUNDARY, pos)) {
                        UUID townId = townBlockEntity.getTownId();
                        if (townId != null) {
                            // Use cached population data from the visualization packet
                            TownBoundaryVisualizationData boundaryData = new TownBoundaryVisualizationData(pos);
                            
                            visualizations.add(new VisualizationData(
                                VisualizationManager.TYPE_TOWN_BOUNDARY, pos, boundaryData));
                            
                            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                                "Added boundary visualization at {} using cached population", pos);
                        }
                    }
                }
            });
        });
        
        return visualizations;
    }
    
    @Override
    protected void renderVisualization(RenderLevelStageEvent event, VisualizationData visualization) {
        if (!(visualization.getData() instanceof TownBoundaryVisualizationData boundaryData)) {
            return;
        }
        
        // Simple green circle - no color coding
        LineRenderer3D.Color boundaryColor = BOUNDARY_COLOR; // Always green
        
        // Create boundary configuration for circles - simple solid line
        BoundaryRenderer3D.BoundaryConfig config = new BoundaryRenderer3D.BoundaryConfig()
            .shape(BoundaryRenderer3D.BoundaryShape.CIRCLE)
            .lineConfig(new LineRenderer3D.LineConfig()
                .style(LineRenderer3D.LineStyle.SOLID) // Solid line like platforms
                .thickness(0.05f) // Same thickness as platforms
                .yOffset(0.1f)); // Slightly above ground
        
        // Render circular boundary around the town center
        BoundaryRenderer3D.renderCircularBoundaryFromCenter(
            event.getPoseStack(),
            visualization.getPosition(),
            boundaryData.getBoundaryRadius(),
            boundaryColor,
            config
        );
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
            "Rendered simple boundary circle with radius {} at position {}", 
            boundaryData.getBoundaryRadius(), visualization.getPosition());
    }
    
    
    @Override
    protected void onPreRender(RenderLevelStageEvent event, Level level) {
        // Clean up expired boundary visualizations
        VisualizationManager.getInstance().cleanupExpired(VisualizationManager.TYPE_TOWN_BOUNDARY);
        
        // Periodically request boundary sync for all active visualizations
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGlobalSyncTime >= GLOBAL_SYNC_INTERVAL_MS) {
            requestBoundaryUpdatesForActiveVisualizations(level);
            lastGlobalSyncTime = currentTime;
        }
    }
    
    /**
     * Requests boundary updates for all currently active town boundary visualizations
     */
    private void requestBoundaryUpdatesForActiveVisualizations(Level level) {
        VisualizationManager manager = VisualizationManager.getInstance();
        
        // Get all active boundary visualizations and request updates
        List<VisualizationManager.VisualizationEntry> activeEntries = 
            manager.getActiveVisualizations(VisualizationManager.TYPE_TOWN_BOUNDARY);
            
        for (VisualizationManager.VisualizationEntry entry : activeEntries) {
            BlockPos pos = entry.getPosition();
            ModMessages.sendToServer(new BoundarySyncRequestPacket(pos));
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Requested boundary sync for active town at {}", pos);
        }
        
        if (!activeEntries.isEmpty()) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Sent {} boundary sync requests to server", activeEntries.size());
        }
    }
    
    @Override
    public void cleanup() {
        // Clear all boundary visualizations on cleanup
        VisualizationManager.getInstance().clearType(VisualizationManager.TYPE_TOWN_BOUNDARY);
    }
    
    /**
     * Convenience method to show town boundary visualization alongside platform visualization
     * 
     * @param townBlockPos Position of the town block
     * @param gameTime Current game time
     */
    public static void showTownBoundaryVisualization(BlockPos townBlockPos, long gameTime) {
        VisualizationManager.getInstance().showVisualization(
            VisualizationManager.TYPE_TOWN_BOUNDARY, 
            townBlockPos, 
            null,  // Data will be fetched from TownBlockEntity during rendering
            600    // 30 seconds duration - same as platforms
        );
    }
    
    /**
     * Hides town boundary visualization at the specified position
     * 
     * @param townBlockPos Position of the town block
     */
    public static void hideTownBoundaryVisualization(BlockPos townBlockPos) {
        VisualizationManager.getInstance().hideVisualizationAt(
            VisualizationManager.TYPE_TOWN_BOUNDARY, 
            townBlockPos
        );
    }
    
    /**
     * Checks if town boundary visualization is active at a position
     * 
     * @param townBlockPos Position of the town block
     * @return true if boundary visualization is active
     */
    public static boolean isTownBoundaryVisualizationActive(BlockPos townBlockPos) {
        return VisualizationManager.getInstance().shouldShowVisualization(
            VisualizationManager.TYPE_TOWN_BOUNDARY, 
            townBlockPos
        );
    }
}