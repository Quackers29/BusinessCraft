package com.yourdomain.businesscraft.forge.event;

import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.ChatFormatting;

/**
 * Forge-specific event handlers
 */
@Mod.EventBusSubscriber(modid = "businesscraft")
public class ForgeModEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeModEvents.class);
    private static BlockPos activeTownBlockPos = null;
    private static long lastClickTime = 0;
    private static boolean awaitingSecondClick = false;

    public static void setActiveTownBlock(BlockPos pos) {
        LOGGER.debug("Setting active town block to: {}", pos);
        activeTownBlockPos = pos;
        awaitingSecondClick = false;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (activeTownBlockPos == null) return;

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
        LOGGER.debug("Right click event with active town block at {}", activeTownBlockPos);

        if (be instanceof TownInterfaceEntity townInterface && townInterface.isInPathCreationMode()) {
            BlockPos clickedPos = event.getPos();
            LOGGER.debug("Path creation mode active, clicked: {}, awaitingSecondClick: {}", clickedPos, awaitingSecondClick);

            if (!townInterface.isValidPathDistance(clickedPos)) {
                event.getEntity().sendSystemMessage(Component.literal("Point too far from town!"));
                event.setCanceled(true);
                return;
            }

            // First click - set start point
            if (!awaitingSecondClick) {
                townInterface.setPathStart(clickedPos);
                event.getEntity().sendSystemMessage(
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
                ITownDataProvider provider = townInterface.getTownDataProvider();
                if (provider != null) {
                    provider.setPathStart(townInterface.getPathStart());
                    provider.setPathEnd(clickedPos);
                    provider.markDirty();
                }

                event.getEntity().sendSystemMessage(
                    Component.literal("Path created!")
                        .withStyle(ChatFormatting.GREEN)
                );

                // Reset state
                awaitingSecondClick = false;
                activeTownBlockPos = null;
                LOGGER.debug("Set path end to {} and completed path creation", clickedPos);
            }

            event.setCanceled(true);
        }
    }
}
