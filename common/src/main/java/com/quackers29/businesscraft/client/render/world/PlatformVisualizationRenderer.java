package com.quackers29.businesscraft.client.render.world;

import com.quackers29.businesscraft.api.RenderHelper;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.PlatformPathView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
        /** True when the player is looking at one platform, so an in-progress point still shows. */
        private final boolean showPartial;

        public PlatformVisualizationData(List<Platform> platforms) {
            this(platforms, false);
        }

        public PlatformVisualizationData(List<Platform> platforms, boolean showPartial) {
            this.platforms = new ArrayList<>(platforms);
            this.showPartial = showPartial;
        }

        public List<Platform> getPlatforms() {
            return platforms;
        }

        public boolean showPartial() {
            return showPartial;
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

                if (!manager.shouldShowVisualization(VisualizationManager.TYPE_PLATFORM, pos)) {
                    return;
                }
                UUID onlyPlatform = filterAt(manager, pos);
                if (blockEntity instanceof TownInterfaceEntity townInterfaceEntity) {
                    List<Platform> platforms = PlatformPathView.select(townInterfaceEntity.getPlatforms(), onlyPlatform);
                    if (time % 2000 < 50) {
                        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_VISUALIZATION,
                                "[PLATFORM] Town at {} has {} platforms", pos, platforms.size());
                    }
                    if (!platforms.isEmpty()) {
                        visualizations.add(new VisualizationData(
                                VisualizationManager.TYPE_PLATFORM, pos,
                                new PlatformVisualizationData(platforms, onlyPlatform != null)));
                    }
                } else if (blockEntity instanceof PlatformInterfaceEntity link && link.getTownCentre() != null) {
                    BlockEntity townBe = level.getBlockEntity(link.getTownCentre());
                    if (townBe instanceof TownInterfaceEntity town) {
                        UUID platformId = onlyPlatform != null ? onlyPlatform : link.getPlatformId();
                        List<Platform> platforms = PlatformPathView.select(town.getPlatforms(), platformId);
                        if (!platforms.isEmpty()) {
                            visualizations.add(new VisualizationData(
                                    VisualizationManager.TYPE_PLATFORM, pos,
                                    new PlatformVisualizationData(platforms, true)));
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
            boolean complete = platform.isComplete();
            if (complete && (platform.isEnabled() || platformData.showPartial())) {
                BlockPos startPos = platform.getStartPos();
                BlockPos endPos = platform.getEndPos();
                PathRenderer3D.renderPath(poseStack, startPos, endPos, PATH_COLOR, pathConfig);
                BoundaryRenderer3D.renderRectangularBoundary(
                        poseStack, startPos, endPos, platform.getSearchRadius(), BOUNDARY_COLOR, boundaryConfig);
            } else if (platformData.showPartial()) {
                BlockPos point = platform.getStartPos() != null ? platform.getStartPos() : platform.getEndPos();
                if (point != null) {
                    PathRenderer3D.renderPath(poseStack, point, point, PATH_COLOR, pathConfig);
                }
            }
        }
    }

    private static UUID filterAt(VisualizationManager manager, BlockPos pos) {
        for (VisualizationManager.VisualizationEntry entry : manager
                .getActiveVisualizations(VisualizationManager.TYPE_PLATFORM)) {
            if (pos.equals(entry.getPosition()) && entry.getData() instanceof UUID id) {
                return id;
            }
        }
        return null;
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
                null, // Every platform on the Town Centre. Data is read from the block entity.
                600 // 30 seconds duration
        );

        manager.showVisualization(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos,
                null,
                600
        );
    }

    /**
     * The same path line as the Town Centre, limited to one platform. Anchored on the
     * Platform Interface block so it can show while that block is selected.
     */
    public static void showPlatformPath(BlockPos interfacePos, UUID platformId, long gameTime) {
        if (interfacePos == null || platformId == null) {
            return;
        }
        VisualizationManager.getInstance().showVisualization(
                VisualizationManager.TYPE_PLATFORM,
                interfacePos,
                platformId,
                600);
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
