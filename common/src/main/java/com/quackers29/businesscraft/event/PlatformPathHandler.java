package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.UUID;

public class PlatformPathHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static BlockPos activeTownBlockPos = null;
    private static UUID activePlatformId = null;
    private static long lastClickTime = 0;
    private static boolean awaitingSecondClick = false;

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

    /**
     * Initialize event callbacks. Should be called during mod initialization.
     */
    public static void initialize() {
        PlatformAccess.getEvents().registerRightClickBlockCallback(PlatformPathHandler::onRightClickBlock);
    }
    
    private static boolean onRightClickBlock(Player player, Level level, BlockPos clickedPos) {
        if (activeTownBlockPos == null || activePlatformId == null) return false;
        
        // Skip if on client side - only process on server
        if (level.isClientSide()) return false;
        
        // Debounce clicks - prevent multiple rapid clicks
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 500) {
            LOGGER.debug("Ignoring click due to debounce (time since last: {}ms)", currentTime - lastClickTime);
            return false;
        }
        lastClickTime = currentTime;
        
        BlockEntity be = level.getBlockEntity(activeTownBlockPos);
        LOGGER.debug("Right click event with active platform {} at {}", activePlatformId, activeTownBlockPos);
        
        if (be instanceof TownInterfaceEntity townInterface && townInterface.isInPlatformCreationMode()) {
            LOGGER.debug("Platform path creation mode active, clicked: {}, awaitingSecondClick: {}", clickedPos, awaitingSecondClick);
            
            // Check if the platform exists
            Platform platform = townInterface.getPlatform(activePlatformId);
            if (platform == null) {
                LOGGER.error("Platform {} not found in town block at {}", activePlatformId, activeTownBlockPos);
                clearActivePlatform();
                return true; // Cancel event
            }
            
            // Check if the clicked position is within town boundary
            String validationError = validatePositionWithinBoundary(clickedPos, townInterface, level);
            if (validationError != null) {
                player.sendSystemMessage(Component.literal(validationError));
                return true; // Cancel event
            }
            
            // First click - set start point
            if (!awaitingSecondClick) {
                townInterface.setPlatformPathStart(activePlatformId, clickedPos);
                player.sendSystemMessage(
                    Component.literal("Platform start point set! Now click to set the end point.")
                        .withStyle(ChatFormatting.YELLOW)
                );
                awaitingSecondClick = true;
                LOGGER.debug("Set platform {} path start to {}, now awaiting second click", activePlatformId, clickedPos);
            } 
            // Second click - set end point
            else {
                townInterface.setPlatformPathEnd(activePlatformId, clickedPos);
                townInterface.setPlatformCreationMode(false, null);
                
                player.sendSystemMessage(
                    Component.literal("Platform path created!")
                        .withStyle(ChatFormatting.GREEN)
                );
                
                // Force a block update to ensure clients get the updated data
                level.sendBlockUpdated(activeTownBlockPos, level.getBlockState(activeTownBlockPos), 
                                       level.getBlockState(activeTownBlockPos), 3);
                
                // Notify clients of the update
                PlatformAccess.getNetworkMessages().sendToAllTrackingChunk(new RefreshPlatformsPacket(activeTownBlockPos), level, activeTownBlockPos);
                
                // Reset state
                awaitingSecondClick = false;
                clearActivePlatform();
                LOGGER.debug("Set platform path end to {} and completed path creation", clickedPos);
            }
            
            return true; // Cancel event
        }
        
        return false; // Don't cancel event
    }
    
    /**
     * Validates if a position is within the town's boundary radius
     * @param pos The position to validate
     * @param townBlock The town block entity
     * @param level The server level
     * @return null if valid, error message if invalid
     */
    private static String validatePositionWithinBoundary(BlockPos pos, TownInterfaceEntity townInterface, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return "Server error: cannot validate position";
        }
        
        UUID townId = townInterface.getTownId();
        if (townId == null) {
            return "No town associated with this block";
        }
        
        Town town = TownManager.get(serverLevel).getTown(townId);
        if (town == null) {
            return "Town not found";
        }
        
        int boundaryRadius = town.getBoundaryRadius();
        double distance = Math.sqrt(pos.distSqr(townInterface.getBlockPos()));
        
        if (distance > boundaryRadius) {
            return String.format("Point too far from town! Distance: %.1f blocks, Town boundary: %d blocks (Population: %d)", 
                distance, boundaryRadius, town.getPopulation());
        }
        
        LOGGER.debug("Platform point validated: distance={}, boundary={}, population={}", 
            distance, boundaryRadius, town.getPopulation());
        return null;
    }
} 
