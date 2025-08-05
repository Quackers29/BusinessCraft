package com.yourdomain.businesscraft.client.render.world;

import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.ui.BoundarySyncRequestPacket;
import com.yourdomain.businesscraft.debug.DebugConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    // Registry to track active boundary data by position
    private static final Map<BlockPos, TownBoundaryVisualizationData> activeBoundaryData = new ConcurrentHashMap<>();
    
    /**
     * Data structure for town boundary visualization with server-authoritative updates
     */
    public static class TownBoundaryVisualizationData {
        private final BlockPos townPosition;
        private volatile int serverBoundaryRadius; // Directly from server
        private volatile boolean hasReceivedServerData; // Track if we have real server data
        
        public TownBoundaryVisualizationData(BlockPos townPosition) {
            this.townPosition = townPosition;
            this.serverBoundaryRadius = 0; // No boundary until server responds
            this.hasReceivedServerData = false; // No server data yet
        }
        
        public int getBoundaryRadius() {
            // Only return radius if we have server data, otherwise 0 (hidden)
            return hasReceivedServerData ? Math.max(serverBoundaryRadius, 5) : 0;
        }
        
        public boolean shouldRender() {
            return hasReceivedServerData && serverBoundaryRadius > 0;
        }
        
        public void updateBoundaryRadius(int radius) {
            this.serverBoundaryRadius = radius;
            this.hasReceivedServerData = true; // Mark that we have server data
        }
        
        public BlockPos getTownPosition() {
            return townPosition;
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
                
                // Check if this is a TownInterfaceEntity with active boundary visualization
                if (blockEntity instanceof TownInterfaceEntity townInterfaceEntity) {
                    // Show boundary when town boundary visualization is active (triggered same time as platforms)
                    if (manager.shouldShowVisualization(VisualizationManager.TYPE_TOWN_BOUNDARY, pos)) {
                        UUID townId = townInterfaceEntity.getTownId();
                        if (townId != null) {
                            // Get or create boundary data for this position
                            TownBoundaryVisualizationData boundaryData = activeBoundaryData.computeIfAbsent(
                                pos, TownBoundaryVisualizationData::new);
                            
                            visualizations.add(new VisualizationData(
                                VisualizationManager.TYPE_TOWN_BOUNDARY, pos, boundaryData));
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
        
        // Only render if we have received server data and boundary > 0
        if (!boundaryData.shouldRender()) {
            return; // Don't render anything until server responds with real data
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
    }
    
    
    @Override
    protected void onPreRender(RenderLevelStageEvent event, Level level) {
        // Clean up boundary data for positions that no longer have active visualizations
        VisualizationManager manager = VisualizationManager.getInstance();
        List<BlockPos> activePositions = manager.getActiveVisualizations(VisualizationManager.TYPE_TOWN_BOUNDARY)
            .stream().map(entry -> entry.getPosition()).toList();
        
        activeBoundaryData.entrySet().removeIf(entry -> !activePositions.contains(entry.getKey()));
        
        // Clean up expired boundary visualizations
        manager.cleanupExpired(VisualizationManager.TYPE_TOWN_BOUNDARY);
        
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
        // Clear the boundary data registry
        activeBoundaryData.clear();
    }
    
    /**
     * Updates boundary radius for a specific town position.
     * Called from BoundarySyncResponsePacket.
     */
    public static void updateBoundaryRadius(BlockPos pos, int radius) {
        TownBoundaryVisualizationData data = activeBoundaryData.get(pos);
        if (data != null) {
            data.updateBoundaryRadius(radius);
        }
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