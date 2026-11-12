package com.quackers29.businesscraft.network;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.UUID;

/**
 * Resolves a town interface for a player action without loading chunks and
 * without accepting clicks from across the world.
 * <p>
 * Reach matches {@code TownInterfaceMenu.stillValid}: 8 blocks (distance squared 64).
 * Platform actions are also accepted from that platform's kiosk, which uses the
 * same reach in {@code PlatformInterfaceMenu.stillValid}.
 */
public final class LoadedTownAccess {
    public static final double REACH_DISTANCE_SQR = 64.0D;
    /** Start of the server log line written for every refused action. */
    public static final String REJECT_MARKER = "BC_REACH_REJECT";

    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Reach");

    private LoadedTownAccess() {
    }

    /** Squared distance from a point to the centre of a block. */
    public static double distanceSqrToCentre(double x, double y, double z, BlockPos pos) {
        double dx = x - (pos.getX() + 0.5D);
        double dy = y - (pos.getY() + 0.5D);
        double dz = z - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    /** True when the point is at most 8 blocks from the centre of {@code pos}. */
    public static boolean withinReach(double x, double y, double z, BlockPos pos) {
        return pos != null && distanceSqrToCentre(x, y, z, pos) <= REACH_DISTANCE_SQR;
    }

    /**
     * True when the point can act on a platform: it is in reach of the Town Centre
     * or of the platform's kiosk. A platform without a kiosk has a null kiosk position.
     */
    public static boolean withinPlatformReach(double x, double y, double z, BlockPos townPos, BlockPos kioskPos) {
        return withinReach(x, y, z, townPos) || withinReach(x, y, z, kioskPos);
    }

    public static TownInterfaceEntity getIfLoadedInReach(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return null;
        }
        TownInterfaceEntity town = loadedTown(player, pos);
        if (town == null) {
            return null;
        }
        if (!withinReach(player.getX(), player.getY(), player.getZ(), pos)) {
            reject(player, pos, "out of reach of the Town Centre");
            return null;
        }
        return town;
    }

    /**
     * Town interface for an action on one platform. Accepts a player standing at the
     * Town Centre or at that platform's kiosk. An unknown platform id needs Town Centre reach.
     */
    public static TownInterfaceEntity getForPlatform(ServerPlayer player, BlockPos townPos, UUID platformId) {
        if (player == null || townPos == null) {
            return null;
        }
        TownInterfaceEntity town = loadedTown(player, townPos);
        if (town == null) {
            return null;
        }
        Platform platform = platformId == null ? null : town.getPlatform(platformId);
        BlockPos kioskPos = platform == null ? null : platform.getInterfacePos();
        if (!withinPlatformReach(player.getX(), player.getY(), player.getZ(), townPos, kioskPos)) {
            reject(player, townPos, "out of reach of the Town Centre and the platform kiosk");
            return null;
        }
        return town;
    }

    private static TownInterfaceEntity loadedTown(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        // hasChunkAt does not load or generate the chunk. getBlockEntity does.
        if (!level.hasChunkAt(pos)) {
            reject(player, pos, "chunk is not loaded");
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TownInterfaceEntity town) {
            return town;
        }
        return null;
    }

    private static void reject(ServerPlayer player, BlockPos pos, String reason) {
        double distance = Math.sqrt(distanceSqrToCentre(player.getX(), player.getY(), player.getZ(), pos));
        LOGGER.warn("{} player={} town={},{},{} distance={} reason={}", REJECT_MARKER,
                player.getGameProfile().getName(), pos.getX(), pos.getY(), pos.getZ(),
                String.format(Locale.ROOT, "%.1f", distance), reason);
    }
}
