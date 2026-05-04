package com.quackers29.businesscraft.client.render.world;

import com.quackers29.businesscraft.api.RenderHelper;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.ui.BoundarySyncRequestPacket;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Renders circular town boundaries from server-synced radius; integrates with {@link VisualizationManager}. */
public class TownBoundaryVisualizationRenderer extends WorldVisualizationRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBoundaryVisualizationRenderer.class);

    private static final LineRenderer3D.Color BOUNDARY_COLOR = LineRenderer3D.Color.GREEN;

    private static long lastGlobalSyncTime = 0;
    private static final long GLOBAL_SYNC_INTERVAL_MS = 2000;

    private static final Map<BlockPos, TownBoundaryVisualizationData> activeBoundaryData = new ConcurrentHashMap<>();

    public static class TownBoundaryVisualizationData {
        private final BlockPos townPosition;
        private volatile int serverBoundaryRadius;
        private volatile boolean hasReceivedServerData;

        public TownBoundaryVisualizationData(BlockPos townPosition) {
            this.townPosition = townPosition;
            this.serverBoundaryRadius = 0;
            this.hasReceivedServerData = false;
        }

        public int getBoundaryRadius() {
            return hasReceivedServerData ? Math.max(serverBoundaryRadius, 5) : 0;
        }

        public boolean shouldRender() {
            return hasReceivedServerData && serverBoundaryRadius > 0;
        }

        public void updateBoundaryRadius(int radius) {
            this.serverBoundaryRadius = radius;
            this.hasReceivedServerData = true;
        }

        public BlockPos getTownPosition() {
            return townPosition;
        }
    }

    public TownBoundaryVisualizationRenderer() {
        super(new RenderConfig()
                .maxRenderDistance(256)
                .chunkRadius(16)
                .renderStage(RenderHelper.RenderStage.AFTER_TRANSLUCENT_BLOCKS));
    }

    @Override
    protected List<VisualizationData> getVisualizations(Level level, BlockPos playerPos) {
        List<VisualizationData> visualizations = new ArrayList<>();

        VisualizationManager manager = VisualizationManager.getInstance();

        iterateNearbyChunks(level, playerPos, (chunk, chunkX, chunkZ) -> {
            chunk.getBlockEntities().forEach((pos, blockEntity) -> {
                if (!isWithinRenderDistance(pos, playerPos)) {
                    return;
                }

                if (blockEntity instanceof TownInterfaceEntity townInterfaceEntity) {
                    if (manager.shouldShowVisualization(VisualizationManager.TYPE_TOWN_BOUNDARY, pos)) {
                        UUID townId = townInterfaceEntity.getTownId();
                        if (townId != null) {
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
    protected void renderVisualization(Object renderEvent, VisualizationData visualization) {
        if (!(visualization.getData() instanceof TownBoundaryVisualizationData boundaryData)) {
            return;
        }

        if (!boundaryData.shouldRender()) {
            return;
        }

        RenderHelper renderHelper = PlatformAccess.getRender();
        if (renderHelper == null)
            return;

        Object poseStackObj = renderHelper.getPoseStack(renderEvent);
        if (!(poseStackObj instanceof com.mojang.blaze3d.vertex.PoseStack poseStack)) {
            return;
        }

        LineRenderer3D.Color boundaryColor = BOUNDARY_COLOR;

        BoundaryRenderer3D.BoundaryConfig config = new BoundaryRenderer3D.BoundaryConfig()
                .shape(BoundaryRenderer3D.BoundaryShape.CIRCLE)
                .lineConfig(new LineRenderer3D.LineConfig()
                        .style(LineRenderer3D.LineStyle.SOLID)
                        .thickness(0.05f)
                        .yOffset(0.1f));

        BoundaryRenderer3D.renderCircularBoundaryFromCenter(
                poseStack,
                visualization.getPosition(),
                boundaryData.getBoundaryRadius(),
                boundaryColor,
                config);
    }

    @Override
    protected void onPreRender(Object renderEvent, Level level) {
        VisualizationManager manager = VisualizationManager.getInstance();
        List<BlockPos> activePositions = manager.getActiveVisualizations(VisualizationManager.TYPE_TOWN_BOUNDARY)
                .stream().map(entry -> entry.getPosition()).toList();

        activeBoundaryData.entrySet().removeIf(entry -> !activePositions.contains(entry.getKey()));

        manager.cleanupExpired(VisualizationManager.TYPE_TOWN_BOUNDARY);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGlobalSyncTime >= GLOBAL_SYNC_INTERVAL_MS) {
            requestBoundaryUpdatesForActiveVisualizations(level);
            lastGlobalSyncTime = currentTime;
        }
    }

    private void requestBoundaryUpdatesForActiveVisualizations(Level level) {
        VisualizationManager manager = VisualizationManager.getInstance();

        List<VisualizationManager.VisualizationEntry> activeEntries = manager
                .getActiveVisualizations(VisualizationManager.TYPE_TOWN_BOUNDARY);

        for (VisualizationManager.VisualizationEntry entry : activeEntries) {
            BlockPos pos = entry.getPosition();
            PlatformAccess.getNetworkMessages().sendToServer(new BoundarySyncRequestPacket(pos));
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
        VisualizationManager.getInstance().clearType(VisualizationManager.TYPE_TOWN_BOUNDARY);
        activeBoundaryData.clear();
    }

    public static void updateBoundaryRadius(BlockPos pos, int radius) {
        TownBoundaryVisualizationData data = activeBoundaryData.get(pos);
        if (data != null) {
            data.updateBoundaryRadius(radius);
        }
    }

    public static void showTownBoundaryVisualization(BlockPos townBlockPos, long gameTime) {
        VisualizationManager.getInstance().showVisualization(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos,
                null,
                600
        );
    }

    public static void hideTownBoundaryVisualization(BlockPos townBlockPos) {
        VisualizationManager.getInstance().hideVisualizationAt(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos);
    }

    public static boolean isTownBoundaryVisualizationActive(BlockPos townBlockPos) {
        return VisualizationManager.getInstance().shouldShowVisualization(
                VisualizationManager.TYPE_TOWN_BOUNDARY,
                townBlockPos);
    }
}
