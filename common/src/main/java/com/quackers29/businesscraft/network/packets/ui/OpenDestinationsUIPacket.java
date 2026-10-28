package com.quackers29.businesscraft.network.packets.ui;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet for opening the destinations UI for a platform
 */
public class OpenDestinationsUIPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDestinationsUIPacket.class);
    private final BlockPos blockPos;
    private final UUID platformId;

    public OpenDestinationsUIPacket(BlockPos blockPos, UUID platformId) {
        this.blockPos = blockPos;
        this.platformId = platformId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUUID(platformId);
    }

    public static OpenDestinationsUIPacket decode(FriendlyByteBuf buf) {
        return new OpenDestinationsUIPacket(buf.readBlockPos(), buf.readUUID());
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) {
                return;
            }

            Level level = player.level();

            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TownInterfaceEntity townInterface) {
                Platform platform = townInterface.getPlatform(platformId);
                if (platform != null) {
                    RefreshDestinationsPacket.sendForPlatform(player, blockPos, platformId, townInterface, platform);
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Sent destinations data to player {}", player.getName().getString());
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
