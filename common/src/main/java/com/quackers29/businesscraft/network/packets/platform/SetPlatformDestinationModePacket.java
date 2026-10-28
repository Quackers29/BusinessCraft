package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class SetPlatformDestinationModePacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos townCentrePos;
    private final UUID platformId;
    private final DestinationMode mode;

    public SetPlatformDestinationModePacket(BlockPos townCentrePos, UUID platformId, DestinationMode mode) {
        this.townCentrePos = townCentrePos;
        this.platformId = platformId;
        this.mode = mode;
    }

    public static SetPlatformDestinationModePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID platformId = buf.readUUID();
        DestinationMode mode = DestinationMode.fromConfig(buf.readUtf(), DestinationMode.AUTO);
        return new SetPlatformDestinationModePacket(pos, platformId, mode);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(townCentrePos);
        buf.writeUUID(platformId);
        buf.writeUtf(mode.toConfigValue());
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
            platform.setDestinationMode(mode);
            townInterface.setChanged();
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Player {} set platform {} destination mode to {} at {}",
                    player.getName().getString(), platformId, mode, townCentrePos);
            RefreshDestinationsPacket.sendForPlatform(player, townCentrePos, platformId, townInterface, platform);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
