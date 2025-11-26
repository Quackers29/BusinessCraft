package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common event handler for town-related logic.
 * Centralizes logic that was previously duplicated across platforms.
 */
public class TownEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownEventHandler.class);

    // State for path creation mode
    private static BlockPos activeTownBlockPos = null;
    private static long lastClickTime = 0;
    private static boolean awaitingSecondClick = false;

    public static void setActiveTownBlock(BlockPos pos) {
        LOGGER.debug("Setting active town block to: {}", pos);
        activeTownBlockPos = pos;
        awaitingSecondClick = false;
    }

    public static void clearActiveTownBlock() {
        LOGGER.debug("Clearing active town block");
        activeTownBlockPos = null;
        awaitingSecondClick = false;
    }

    public static BlockPos getActiveTownBlockPos() {
        return activeTownBlockPos;
    }

    /**
     * Handles right-click block events for path creation.
     * 
     * @return true if the event was handled and should be cancelled/consumed
     */
    public static boolean onRightClickBlock(Player player, Level level, BlockPos clickedPos) {
        if (activeTownBlockPos == null)
            return false;

        // Skip if on client side - only process on server
        if (level.isClientSide())
            return false;

        // Debounce clicks - prevent multiple rapid clicks
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 500) {
            LOGGER.debug("Ignoring click due to debounce (time since last: {}ms)", currentTime - lastClickTime);
            return true; // Consume click to prevent double processing
        }
        lastClickTime = currentTime;

        BlockEntity be = level.getBlockEntity(activeTownBlockPos);
        LOGGER.debug("Right click event with active town block at {}", activeTownBlockPos);

        if (be instanceof TownInterfaceEntity townInterface && townInterface.isInPathCreationMode()) {
            LOGGER.debug("Path creation mode active, clicked: {}, awaitingSecondClick: {}", clickedPos,
                    awaitingSecondClick);

            if (!townInterface.isValidPathDistance(clickedPos)) {
                player.sendSystemMessage(Component.literal("Point too far from town!"));
                return true;
            }

            // First click - set start point
            if (!awaitingSecondClick) {
                townInterface.setPathStart(clickedPos);
                player.sendSystemMessage(
                        Component.literal("First point set! Now click to set the end point.")
                                .withStyle(ChatFormatting.YELLOW));
                awaitingSecondClick = true;
                LOGGER.debug("Set path start to {}, now awaiting second click", clickedPos);
            }
            // Second click - set end point
            else {
                townInterface.setPathEnd(clickedPos);
                townInterface.setPathCreationMode(false);

                // Update provider
                ITownDataProvider provider = townInterface.getTownDataProvider();
                if (provider != null) {
                    provider.setPathStart(townInterface.getPathStart());
                    provider.setPathEnd(clickedPos);
                    provider.markDirty();
                }

                player.sendSystemMessage(
                        Component.literal("Path created!")
                                .withStyle(ChatFormatting.GREEN));

                // Reset state
                awaitingSecondClick = false;
                activeTownBlockPos = null;
                LOGGER.debug("Set path end to {} and completed path creation", clickedPos);
            }

            return true;
        }

        return false;
    }
}
