package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.EventHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.ChatFormatting;
import com.quackers29.businesscraft.util.PositionConverter;

/**
 * Platform-agnostic event handling for town path creation system.
 * Uses EventHelper abstraction for cross-platform compatibility.
 */
public class ModEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);
    private static BlockPos activeTownBlockPos = null;
    private static long lastClickTime = 0;
    private static boolean awaitingSecondClick = false;

    /**
     * Initialize the platform-agnostic event system by registering handlers.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        PlatformServices.getEventHelper().registerBlockInteractionEvent(
            new EventHelper.BlockInteractionHandler() {
                @Override
                public Object onBlockInteraction(Object player, Object level, Object hand, 
                                               Object pos, Object state, Object hitResult) {
                    return ModEvents.onBlockInteraction(player, level, hand, pos, state, hitResult);
                }
            }
        );
    }

    public static void setActiveTownBlock(BlockPos pos) {
        LOGGER.debug("Setting active town block to: {}", pos);
        activeTownBlockPos = pos;
        awaitingSecondClick = false;
    }

    /**
     * Platform-agnostic block interaction handler for path creation system.
     */
    private static Object onBlockInteraction(Object playerObj, Object levelObj, Object handObj,
                                           Object clickedPosObj, Object stateObj, Object hitResultObj) {
        // Cast objects to Minecraft types
        if (!(playerObj instanceof Player player) || !(levelObj instanceof Level level) || 
            !(clickedPosObj instanceof BlockPos clickedPos)) {
            return (Object) InteractionResult.PASS;
        }
        if (activeTownBlockPos == null) return (Object) InteractionResult.PASS;
        
        // If PlatformPathHandler is active, let it handle the event instead
        if (PlatformPathHandler.isActivePlatformSet()) {
            return (Object) InteractionResult.PASS;
        }
        
        // Skip if on client side - only process on server
        if (level.isClientSide()) return (Object) InteractionResult.PASS;
        
        // Debounce clicks - prevent multiple rapid clicks
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 500) {
            LOGGER.debug("Ignoring click due to debounce (time since last: {}ms)", currentTime - lastClickTime);
            return (Object) InteractionResult.PASS;
        }
        lastClickTime = currentTime;
        
        BlockEntity be = level.getBlockEntity(activeTownBlockPos);
        LOGGER.debug("Right click event with active town block at {}", activeTownBlockPos);
        
        if (be instanceof TownInterfaceEntity townInterface && townInterface.isInPathCreationMode() && !townInterface.isInPlatformCreationMode()) {
            LOGGER.debug("Path creation mode active, clicked: {}, awaitingSecondClick: {}", clickedPos, awaitingSecondClick);
            
            if (!townInterface.isValidPathDistance(clickedPos)) {
                player.sendSystemMessage(Component.literal("Point too far from town!"));
                return (Object) InteractionResult.FAIL;
            }
            
            // First click - set start point
            if (!awaitingSecondClick) {
                townInterface.setPathStart(clickedPos);
                player.sendSystemMessage(
                    Component.literal("First point set! Now click to set the end point.")
                        .withStyle(ChatFormatting.YELLOW)
                );
                awaitingSecondClick = true;
                LOGGER.debug("Set path start to {}, now awaiting second click", clickedPos);
            } 
            // Second click - set end point
            else {
                townInterface.setPathEnd(clickedPos);
                townInterface.setPathCreationMode(false);
                
                // Update provider
                Object providerObj = townInterface.getTownDataProvider();
                ITownDataProvider provider = providerObj instanceof ITownDataProvider ? (ITownDataProvider) providerObj : null;
                if (provider != null) {
                    provider.setPathStart(PositionConverter.toPosition(townInterface.getPathStartBlockPos()));
                    provider.setPathEnd(PositionConverter.toPosition(clickedPos));
                    provider.markDirty();
                }
                
                player.sendSystemMessage(
                    Component.literal("Path created!")
                        .withStyle(ChatFormatting.GREEN)
                );
                
                // Reset state
                awaitingSecondClick = false;
                activeTownBlockPos = null;
                LOGGER.debug("Set path end to {} and completed path creation", clickedPos);
            }
            
            return (Object) InteractionResult.SUCCESS;
        }
        
        return (Object) InteractionResult.PASS;
    }
}