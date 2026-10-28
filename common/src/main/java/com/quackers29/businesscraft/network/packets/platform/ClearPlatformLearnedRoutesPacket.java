package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket;
import com.quackers29.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class ClearPlatformLearnedRoutesPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos townCentrePos;
    private final UUID platformId;

    public ClearPlatformLearnedRoutesPacket(BlockPos townCentrePos, UUID platformId) {
        this.townCentrePos = townCentrePos;
        this.platformId = platformId;
    }

    public static ClearPlatformLearnedRoutesPacket decode(FriendlyByteBuf buf) {
        return new ClearPlatformLearnedRoutesPacket(buf.readBlockPos(), buf.readUUID());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(townCentrePos);
        buf.writeUUID(platformId);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) {
                return;
            }
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(townCentrePos);
            if (!(be instanceof TownInterfaceEntity townInterface)) {
                return;
            }
            Platform platform = townInterface.getPlatform(platformId);
            if (platform == null) {
                return;
            }
            platform.clearLearnedRoutes();
            townInterface.setChanged();
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Player {} cleared learned routes for platform {} at {}",
                    player.getName().getString(), platformId, townCentrePos);
            RefreshDestinationsPacket.sendForPlatform(player, townCentrePos, platformId, townInterface, platform);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
