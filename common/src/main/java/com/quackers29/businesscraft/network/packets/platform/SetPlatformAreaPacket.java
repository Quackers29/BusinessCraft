package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Sets one platform's search radius and spread. The server clamps both, and spread cannot
 * exceed the radius.
 */
public class SetPlatformAreaPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos blockPos;
    private final UUID platformId;
    private final int searchRadius;
    private final int spread;

    public SetPlatformAreaPacket(BlockPos blockPos, UUID platformId, int searchRadius, int spread) {
        this.blockPos = blockPos;
        this.platformId = platformId;
        this.searchRadius = searchRadius;
        this.spread = spread;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUUID(platformId);
        buf.writeInt(searchRadius);
        buf.writeInt(spread);
    }

    public void toBytes(FriendlyByteBuf buf) {
        encode(buf);
    }

    public static SetPlatformAreaPacket decode(FriendlyByteBuf buf) {
        return new SetPlatformAreaPacket(buf.readBlockPos(), buf.readUUID(), buf.readInt(), buf.readInt());
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) {
                return;
            }
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(blockPos);
            if (!(be instanceof TownInterfaceEntity townInterface)) {
                return;
            }
            Platform platform = townInterface.getPlatform(platformId);
            if (platform == null) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "SetPlatformAreaPacket: platform {} missing at {}", platformId, blockPos);
                return;
            }
            platform.setSearchRadius(searchRadius);
            platform.setSpread(spread);
            townInterface.setChanged();
            if (level instanceof ServerLevel) {
                player.connection.send(ClientboundBlockEntityDataPacket.create(townInterface));
            }
            level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);
            PlatformAccess.getNetworkMessages().sendToAllTrackingChunk(
                    new RefreshPlatformsPacket(blockPos), level, blockPos);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
