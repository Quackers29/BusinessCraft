package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.ui.PlatformVisualizationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pushes one platform's path to the client and turns on the same path line the Town Centre uses.
 */
public final class PlatformPathSync {
    private PlatformPathSync() {
    }

    public static void show(ServerPlayer player, TownInterfaceEntity town, Platform platform) {
        if (player == null || town == null || platform == null || platform.getId() == null) {
            return;
        }
        town.syncPlatforms();
        BlockPos anchor = platform.getInterfacePos() != null ? platform.getInterfacePos() : town.getBlockPos();
        if (PlatformAccess.getNetworkMessages() != null) {
            PlatformAccess.getNetworkMessages().sendToPlayer(
                    new PlatformVisualizationPacket(anchor, platform.getId()), player);
        }
        if (platform.isComplete() && player.level() instanceof ServerLevel serverLevel) {
            TownInterfaceBlock.spawnIndicator(serverLevel, platform);
        }
    }
}
