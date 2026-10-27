package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.PlatformPathSync;
import com.quackers29.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class PlatformPathHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void initialize() {
        PlatformAccess.getEvents().registerRightClickBlockCallback(PlatformPathHandler::onRightClickBlock);
        PlatformAccess.getEvents().registerPlayerLogoutCallback(player -> PlatformPathSessions.clear(player.getUUID()));
    }

    private static boolean onRightClickBlock(Player player, Level level, BlockPos clickedPos) {
        if (level.isClientSide()) {
            return false;
        }

        PlatformPathSessions.Session session = PlatformPathSessions.get(player.getUUID());
        if (session == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - session.lastClickTime < 500) {
            LOGGER.debug("Ignoring click due to debounce (time since last: {}ms)", currentTime - session.lastClickTime);
            return false;
        }
        session.lastClickTime = currentTime;

        BlockEntity be = level.getBlockEntity(session.townBlockPos);
        LOGGER.debug("Right click event with active platform {} at {}", session.platformId, session.townBlockPos);

        if (!(be instanceof TownInterfaceEntity townInterface)) {
            PlatformPathSessions.clear(player.getUUID());
            return false;
        }

        Platform platform = townInterface.getPlatform(session.platformId);
        if (platform == null) {
            LOGGER.error("Platform {} not found in town block at {}", session.platformId, session.townBlockPos);
            PlatformPathSessions.clear(player.getUUID());
            return true;
        }

        String validationError = validatePositionWithinBoundary(clickedPos, townInterface, level);
        if (validationError != null) {
            player.sendSystemMessage(Component.literal(validationError));
            return true;
        }

        if (!session.awaitingSecondClick) {
            townInterface.setPlatformPathStart(session.platformId, clickedPos);
            player.sendSystemMessage(
                    Component.translatable("businesscraft.platform_path_start_set",
                            clickedPos.getX(), clickedPos.getY(), clickedPos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
            session.awaitingSecondClick = true;
            if (player instanceof ServerPlayer serverPlayer) {
                PlatformPathSync.show(serverPlayer, townInterface, platform);
            }
            LOGGER.debug("Set platform {} path start to {}, now awaiting second click", session.platformId, clickedPos);
        } else {
            townInterface.setPlatformPathEnd(session.platformId, clickedPos);
            if (player instanceof ServerPlayer serverPlayer) {
                PlatformPathSync.show(serverPlayer, townInterface, townInterface.getPlatform(session.platformId));
            }

            player.sendSystemMessage(
                    Component.translatable("businesscraft.platform_path_end_set",
                            clickedPos.getX(), clickedPos.getY(), clickedPos.getZ())
                            .withStyle(ChatFormatting.GREEN));

            level.sendBlockUpdated(session.townBlockPos, level.getBlockState(session.townBlockPos),
                    level.getBlockState(session.townBlockPos), 3);

            PlatformAccess.getNetworkMessages().sendToAllTrackingChunk(
                    new RefreshPlatformsPacket(session.townBlockPos), level, session.townBlockPos);

            PlatformPathSessions.clear(player.getUUID());
            LOGGER.debug("Set platform path end to {} and completed path creation", clickedPos);
        }

        return true;
    }

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
            return String.format("Point too far from town! Distance: %.1f blocks, town boundary: %d blocks",
                    distance, boundaryRadius);
        }

        LOGGER.debug("Platform point validated: distance={}, boundary={}, population={}",
                distance, boundaryRadius, town.getPopulation());
        return null;
    }
}
