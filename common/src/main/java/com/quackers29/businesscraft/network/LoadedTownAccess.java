package com.quackers29.businesscraft.network;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Resolves a town interface for a player action without loading chunks and
 * without accepting clicks from across the world.
 * <p>
 * Reach matches {@code TownInterfaceMenu.stillValid}: 8 blocks (distance squared 64).
 */
public final class LoadedTownAccess {
    public static final double REACH_DISTANCE_SQR = 64.0D;

    private LoadedTownAccess() {
    }

    public static TownInterfaceEntity getIfLoadedInReach(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return null;
        }
        Level level = player.level();
        // hasChunkAt does not load or generate the chunk. getBlockEntity does.
        if (!level.hasChunkAt(pos)) {
            return null;
        }
        double distanceSqr = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (distanceSqr > REACH_DISTANCE_SQR) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TownInterfaceEntity town) {
            return town;
        }
        return null;
    }
}
