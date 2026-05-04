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

import com.quackers29.businesscraft.debug.DebugConfig;

/** Renders platform paths and search boundaries for the platform visualization type. */
public class PlatformVisualizationRenderer extends WorldVisualizationRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformVisualizationRenderer.class);

    private static final LineRenderer3D.Color PATH_COLOR = LineRenderer3D.Color.GREEN;
    private static final LineRenderer3D.Color BOUNDARY_COLOR = LineRenderer3D.Color.ORANGE;

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

        VisualizationManager manager = VisualizationManager.getInstance();
        List<VisualizationManager.VisualizationEntry> activeVisualizations = manager
                .getActiveVisualizations(VisualizationManager.TYPE_PLATFORM);

        long time = System.currentTimeMillis();
        if (time % 2000 < 50) {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_VISUALIZATION, "[PLATFORM] Active visualizations: {}", activeVisualizations.size());
        }

        iterateNearbyChunks(level, playerPos, (chunk, chunkX, chunkZ) -> {
            chunk.getBlockEntities().forEach((pos, blockEntity) -> {
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
                            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_VISUALIZATION, "[PLATFORM] Town at {} has {} platforms", pos, platforms.size());
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
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_VISUALIZATION, "[PLATFORM] Returning {} visualizations", visualizations.size());
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

        LineRenderer3D.LineConfig lineConfig = new LineRenderer3D.LineConfig()
                .thickness(0.05f)
                .yOffset(1.1f);

        PathRenderer3D.PathConfig pathConfig = new PathRenderer3D.PathConfig()
                .interpolation(PathRenderer3D.InterpolationType.STEPPED)
                .lineConfig(lineConfig);

        BoundaryRenderer3D.BoundaryConfig boundaryConfig = new BoundaryRenderer3D.BoundaryConfig()
                .shape(BoundaryRenderer3D.BoundaryShape.RECTANGLE)
                .lineConfig(lineConfig);

        for (Platform platform : platformData.getPlatforms()) {
            if (!platform.isEnabled() || !platform.isComplete()) {
                continue;
            }

            BlockPos startPos = platform.getStartPos();
            BlockPos endPos = platform.getEndPos();

            PathRenderer3D.renderPath(
                    poseStack,
                    startPos, endPos,
                    PATH_COLOR,
                    pathConfig);

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
        VisualizationManager.getInstance().cleanupExpired(VisualizationManager.TYPE_PLATFORM);
    }

    @Override
    public void cleanup() {
        VisualizationManager.getInstance().clearType(VisualizationManager.TYPE_PLATFORM);
    }

    public static void showPlatformVisualization(BlockPos townBlockPos, long gameTime) {
        VisualizationManager manager = VisualizationManager.getInstance();

        manager.showVisualization(
                VisualizationManager.TYPE_PLATFORM,
                townBlockPos,
                null, // Data will be fetched from TownBlockEntity during rendering
                600 // 30 seconds duration
        );

        manager.showVisualization(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos,
                null, // Data will be fetched from TownBlockEntity during rendering
                600 // 30 seconds duration - same as platforms
        );
    }

    public static void hidePlatformVisualization(BlockPos townBlockPos) {
        VisualizationManager manager = VisualizationManager.getInstance();

        manager.hideVisualizationAt(
                VisualizationManager.TYPE_PLATFORM,
                townBlockPos);

        manager.hideVisualizationAt(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos);
    }

    public static boolean isPlatformVisualizationActive(BlockPos townBlockPos) {
        return VisualizationManager.getInstance().shouldShowVisualization(
                VisualizationManager.TYPE_PLATFORM,
                townBlockPos);
    }
}
