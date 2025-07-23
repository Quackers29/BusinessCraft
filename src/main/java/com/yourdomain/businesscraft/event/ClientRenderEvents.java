package com.yourdomain.businesscraft.event;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.client.PlatformVisualizationManager;
import com.yourdomain.businesscraft.client.renderer.PlatformLineRenderer;
import com.yourdomain.businesscraft.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-side rendering events for platform visualization.
 * Handles world-space line rendering for platform paths and boundaries.
 */
@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientRenderEvents {
    
    private static final float LINE_WIDTH = 2.0f; // Not used anymore, thickness handled by multiple parallel lines
    
    // Colors for visualization
    private static final float[] PATH_COLOR = {0.0f, 1.0f, 0.0f, 0.8f}; // Green with transparency
    private static final float[] BOUNDARY_COLOR = {1.0f, 0.3f, 0.0f, 0.8f}; // Orange with transparency
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Only render during the translucent stage to ensure proper blending
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        Level level = Minecraft.getInstance().level;
        if (level == null || !level.isClientSide) {
            return;
        }
        
        long currentGameTime = level.getGameTime();
        
        // Clean up expired visualizations
        PlatformVisualizationManager.getInstance().cleanupExpired(currentGameTime);
        
        // Search for TownBlockEntity instances within a reasonable range of the player
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        
        BlockPos playerPos = mc.player.blockPosition();
        int maxRenderDistance = 128; // Maximum distance to render platform lines
        int chunkRadius = 8; // Check chunks within this radius
        
        // Iterate through loaded chunks near the player
        for (int chunkX = (playerPos.getX() >> 4) - chunkRadius; chunkX <= (playerPos.getX() >> 4) + chunkRadius; chunkX++) {
            for (int chunkZ = (playerPos.getZ() >> 4) - chunkRadius; chunkZ <= (playerPos.getZ() >> 4) + chunkRadius; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                
                var chunk = level.getChunk(chunkX, chunkZ);
                // Iterate through block entities in this chunk
                chunk.getBlockEntities().forEach((pos, blockEntity) -> {
                    // Check distance from player
                    if (pos.distSqr(playerPos) > maxRenderDistance * maxRenderDistance) {
                        return;
                    }
                    
                    // Check if this is a TownBlockEntity and should show visualization
                    if (blockEntity instanceof TownBlockEntity townBlockEntity) {
                        // Only render if this town is registered for visualization
                        if (PlatformVisualizationManager.getInstance().shouldShowVisualization(pos, currentGameTime)) {
                            renderPlatformsForTown(event, townBlockEntity);
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Renders platforms for a specific town block entity
     * @param event The render level stage event
     * @param townBlockEntity The town block entity to render platforms for
     */
    private static void renderPlatformsForTown(RenderLevelStageEvent event, TownBlockEntity townBlockEntity) {
        // Get platforms from the town block entity (automatically returns client-side data)
        List<Platform> platforms = townBlockEntity.getPlatforms();
        if (platforms.isEmpty()) {
            return;
        }
        
        // Get the actual search radius from the town block entity
        int searchRadius = townBlockEntity.getSearchRadius();
        
        for (Platform platform : platforms) {
            if (!platform.isEnabled() || !platform.isComplete()) {
                continue;
            }
            
            BlockPos startPos = platform.getStartPos();
            BlockPos endPos = platform.getEndPos();
            
            // Render the platform path as a solid green line
            PlatformLineRenderer.renderPath(
                event.getPoseStack(),
                startPos, endPos,
                PATH_COLOR[0], PATH_COLOR[1], PATH_COLOR[2], PATH_COLOR[3],
                LINE_WIDTH
            );
            
            // Render the search boundary as a solid orange rectangle
            PlatformLineRenderer.renderBoundary(
                event.getPoseStack(),
                startPos, endPos, searchRadius,
                BOUNDARY_COLOR[0], BOUNDARY_COLOR[1], BOUNDARY_COLOR[2], BOUNDARY_COLOR[3],
                LINE_WIDTH
            );
        }
    }
    
    /**
     * Clean up visualization state when the player changes worlds
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            PlatformVisualizationManager.getInstance().clearAll();
        }
    }
}