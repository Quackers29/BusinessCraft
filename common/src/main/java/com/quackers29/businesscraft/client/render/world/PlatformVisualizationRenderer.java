package com.quackers29.businesscraft.client.render.world;

import com.quackers29.businesscraft.api.RenderHelper;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-specific implementation of the modular visualization renderer.
 * 
 * Renders platform paths and boundaries using the new modular 3D line rendering
 * system
 * while maintaining exact compatibility with the original platform
 * visualization.
 */
public class PlatformVisualizationRenderer extends WorldVisualizationRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformVisualizationRenderer.class);

    private static final LineRenderer3D.Color PATH_COLOR = LineRenderer3D.Color.GREEN;
    private static final LineRenderer3D.Color BOUNDARY_COLOR = LineRenderer3D.Color.ORANGE;

    /**
     * Data structure for platform visualization data
     */
    public static class PlatformVisualizationData {
        private final List<Platform> platforms;
        private final int searchRadius;

        public PlatformVisualizationData(List<Platform> platforms, int searchRadius) {
            this.platforms = new ArrayList<>(platforms);
            this.searchRadius = searchRadius;
        }

        public List<Platform> getPlatforms() {
            return platforms;
        }

        public int getSearchRadius() {
            return searchRadius;
        }
    }

    public PlatformVisualizationRenderer() {
        super(new RenderConfig()
                .maxRenderDistance(128)
                .chunkRadius(8)
                .renderStage(RenderHelper.RenderStage.AFTER_TRANSLUCENT_BLOCKS));
    }

    @Override
    protected List<VisualizationData> getVisualizations(Level level, BlockPos playerPos) {
        List<VisualizationData> visualizations = new ArrayList<>();

        // Use the generic visualization manager to get active platform visualizations
        VisualizationManager manager = VisualizationManager.getInstance();
        List<VisualizationManager.VisualizationEntry> activeVisualizations = manager
                .getActiveVisualizations(VisualizationManager.TYPE_PLATFORM);

        // Log occasionally
        long time = System.currentTimeMillis();
        if (time % 2000 < 50) {
            LOGGER.info("[PLATFORM] Active visualizations: {}", activeVisualizations.size());
        }

        // For each active platform visualization, find the corresponding
        // TownBlockEntity
        // and extract platform data
        iterateNearbyChunks(level, playerPos, (chunk, chunkX, chunkZ) -> {
            chunk.getBlockEntities().forEach((pos, blockEntity) -> {
                // Check distance from player
                if (!isWithinRenderDistance(pos, playerPos)) {
                    return;
                }

                // Check if this is a TownInterfaceEntity with active visualization
                if (blockEntity instanceof TownInterfaceEntity townInterfaceEntity) {
                    // Check if this town has active platform visualization
                    if (manager.shouldShowVisualization(VisualizationManager.TYPE_PLATFORM, pos)) {
                        // Get platform data from the town interface entity
                        List<Platform> platforms = townInterfaceEntity.getPlatforms();
                        int searchRadius = townInterfaceEntity.getSearchRadius();

                        if (time % 2000 < 50) {
                            LOGGER.info("[PLATFORM] Town at {} has {} platforms", pos, platforms.size());
                        }

                        if (!platforms.isEmpty()) {
                            PlatformVisualizationData data = new PlatformVisualizationData(platforms, searchRadius);
                            visualizations.add(new VisualizationData(
                                    VisualizationManager.TYPE_PLATFORM, pos, data));
                        }
                    }
                }
            });
        });

        if (time % 2000 < 50) {
            LOGGER.info("[PLATFORM] Returning {} visualizations", visualizations.size());
        }

        return visualizations;
    }

    @Override
    protected void renderVisualization(Object renderEvent, VisualizationData visualization) {
        PlatformVisualizationData platformData = visualization.getData(PlatformVisualizationData.class);
        if (platformData == null) {
            return;
        }

        RenderHelper renderHelper = com.quackers29.businesscraft.api.PlatformAccess.getRender();
        if (renderHelper == null)
            return;

        Object poseStackObj = renderHelper.getPoseStack(renderEvent);
        if (!(poseStackObj instanceof com.mojang.blaze3d.vertex.PoseStack poseStack)) {
            return;
        }

        // Configure line rendering to match original appearance
        LineRenderer3D.LineConfig lineConfig = new LineRenderer3D.LineConfig()
                .thickness(0.05f) // Same thickness as original
                .yOffset(1.1f); // Same Y offset as original

        PathRenderer3D.PathConfig pathConfig = new PathRenderer3D.PathConfig()
                .interpolation(PathRenderer3D.InterpolationType.STEPPED) // Same algorithm as original
                .lineConfig(lineConfig);

        BoundaryRenderer3D.BoundaryConfig boundaryConfig = new BoundaryRenderer3D.BoundaryConfig()
                .shape(BoundaryRenderer3D.BoundaryShape.RECTANGLE)
                .lineConfig(lineConfig);

        // Render each platform
        for (Platform platform : platformData.getPlatforms()) {
            if (!platform.isEnabled() || !platform.isComplete()) {
                continue;
            }

            BlockPos startPos = platform.getStartPos();
            BlockPos endPos = platform.getEndPos();

            // Render the platform path using the new modular path renderer
            PathRenderer3D.renderPath(
                    poseStack,
                    startPos, endPos,
                    PATH_COLOR,
                    pathConfig);

            // Render the search boundary using the new modular boundary renderer
            BoundaryRenderer3D.renderRectangularBoundary(
                    poseStack,
                    startPos, endPos,
                    platformData.getSearchRadius(),
                    BOUNDARY_COLOR,
                    boundaryConfig);
        }
    }

    @Override
    protected void onPreRender(Object renderEvent, Level level) {
        // Clean up expired platform visualizations
        VisualizationManager.getInstance().cleanupExpired(VisualizationManager.TYPE_PLATFORM);
    }

    @Override
    public void cleanup() {
        // Clear all platform visualizations on cleanup
        VisualizationManager.getInstance().clearType(VisualizationManager.TYPE_PLATFORM);
    }

    /**
     * Convenience method to show platform visualization (replaces old API)
     * Also triggers town boundary visualization at the same time
     * 
     * @param townBlockPos Position of the town block
     * @param gameTime     Current game time
     */
    public static void showPlatformVisualization(BlockPos townBlockPos, long gameTime) {
        VisualizationManager manager = VisualizationManager.getInstance();

        // Show platform visualization
        manager.showVisualization(
                VisualizationManager.TYPE_PLATFORM,
                townBlockPos,
                null, // Data will be fetched from TownBlockEntity during rendering
                600 // 30 seconds duration
        );

        // Also show town boundary visualization
        manager.showVisualization(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos,
                null, // Data will be fetched from TownBlockEntity during rendering
                600 // 30 seconds duration - same as platforms
        );
    }

    /**
     * Convenience method to hide platform visualization
     * Also hides town boundary visualization at the same time
     * 
     * @param townBlockPos Position of the town block
     */
    public static void hidePlatformVisualization(BlockPos townBlockPos) {
        VisualizationManager manager = VisualizationManager.getInstance();

        // Hide platform visualization
        manager.hideVisualizationAt(
                VisualizationManager.TYPE_PLATFORM,
                townBlockPos);

        // Also hide town boundary visualization
        manager.hideVisualizationAt(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos);
    }

    /**
     * Check if platform visualization is active for a town block
     * 
     * @param townBlockPos Position of the town block
     * @return true if visualization is active
     */
    public static boolean isPlatformVisualizationActive(BlockPos townBlockPos) {
        return VisualizationManager.getInstance().shouldShowVisualization(
                VisualizationManager.TYPE_PLATFORM,
                townBlockPos);
    }
}
