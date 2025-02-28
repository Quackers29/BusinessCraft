package com.yourdomain.businesscraft.event;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.RefreshPlatformsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.UUID;

@Mod.EventBusSubscriber
public class PlatformPathHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static BlockPos activeTownBlockPos = null;
    private static UUID activePlatformId = null;
    private static long lastClickTime = 0;
    private static boolean awaitingSecondClick = false;
    private static final int MAX_PATH_DISTANCE = 50;

    public static void setActivePlatform(BlockPos pos, UUID platformId) {
        LOGGER.debug("Setting active platform {} for town block at {}", platformId, pos);
        activeTownBlockPos = pos;
        activePlatformId = platformId;
        awaitingSecondClick = false;
    }

    public static void clearActivePlatform() {
        activeTownBlockPos = null;
        activePlatformId = null;
        awaitingSecondClick = false;
        LOGGER.debug("Cleared active platform");
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (activeTownBlockPos == null || activePlatformId == null) return;
        
        // Skip if on client side - only process on server
        if (event.getLevel().isClientSide()) return;
        
        // Debounce clicks - prevent multiple rapid clicks
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 500) {
            LOGGER.debug("Ignoring click due to debounce (time since last: {}ms)", currentTime - lastClickTime);
            return;
        }
        lastClickTime = currentTime;
        
        Level level = event.getLevel();
        BlockEntity be = level.getBlockEntity(activeTownBlockPos);
        LOGGER.debug("Right click event with active platform {} at {}", activePlatformId, activeTownBlockPos);
        
        if (be instanceof TownBlockEntity townBlock && townBlock.isInPlatformCreationMode()) {
            BlockPos clickedPos = event.getPos();
            LOGGER.debug("Platform path creation mode active, clicked: {}, awaitingSecondClick: {}", clickedPos, awaitingSecondClick);
            
            // Check if the platform exists
            Platform platform = townBlock.getPlatform(activePlatformId);
            if (platform == null) {
                LOGGER.error("Platform {} not found in town block at {}", activePlatformId, activeTownBlockPos);
                clearActivePlatform();
                event.setCanceled(true);
                return;
            }
            
            // Check if the clicked position is within range
            if (!isValidPathDistance(clickedPos, townBlock.getBlockPos())) {
                event.getEntity().sendSystemMessage(Component.literal("Point too far from town!"));
                event.setCanceled(true);
                return;
            }
            
            // First click - set start point
            if (!awaitingSecondClick) {
                townBlock.setPlatformPathStart(activePlatformId, clickedPos);
                event.getEntity().sendSystemMessage(
                    Component.literal("Platform start point set! Now click to set the end point.")
                        .withStyle(ChatFormatting.YELLOW)
                );
                awaitingSecondClick = true;
                LOGGER.debug("Set platform {} path start to {}, now awaiting second click", activePlatformId, clickedPos);
            } 
            // Second click - set end point
            else {
                townBlock.setPlatformPathEnd(activePlatformId, clickedPos);
                townBlock.setPlatformCreationMode(false, null);
                
                event.getEntity().sendSystemMessage(
                    Component.literal("Platform path created!")
                        .withStyle(ChatFormatting.GREEN)
                );
                
                // Force a block update to ensure clients get the updated data
                level.sendBlockUpdated(activeTownBlockPos, level.getBlockState(activeTownBlockPos), 
                                       level.getBlockState(activeTownBlockPos), 3);
                
                // Notify clients of the update
                ModMessages.sendToAllTrackingChunk(new RefreshPlatformsPacket(activeTownBlockPos), level, activeTownBlockPos);
                
                // Reset state
                awaitingSecondClick = false;
                clearActivePlatform();
                LOGGER.debug("Set platform path end to {} and completed path creation", clickedPos);
            }
            
            event.setCanceled(true);
        }
    }
    
    private static boolean isValidPathDistance(BlockPos pos, BlockPos townPos) {
        return pos.distManhattan(townPos) <= MAX_PATH_DISTANCE;
    }
} 