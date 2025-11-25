package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import java.util.UUID;

/**
 * Packet to reset the path of a platform
 */
public class ResetPlatformPathPacket {
    private static final Logger LOGGER = LogManager.getLogger(ResetPlatformPathPacket.class);
    private final BlockPos townInterfacePos;
    private final UUID platformId;
    
    public ResetPlatformPathPacket(BlockPos townInterfacePos, UUID platformId) {
        this.townInterfacePos = townInterfacePos;
        this.platformId = platformId;
    }
    
    public static void encode(ResetPlatformPathPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.townInterfacePos);
        buf.writeUUID(msg.platformId);
    }
    
    /**
     * Serialize packet data for Fabric networking
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(townInterfacePos);
        buf.writeUUID(platformId);
    }
    
    public static ResetPlatformPathPacket decode(FriendlyByteBuf buf) {
        return new ResetPlatformPathPacket(
            buf.readBlockPos(),
            buf.readUUID()
        );
    }
    
    public static void handle(ResetPlatformPathPacket msg, Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) return;
            
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.townInterfacePos);
            
            if (be instanceof TownInterfaceEntity townInterfaceEntity) {
                Platform platform = townInterfaceEntity.getPlatform(msg.platformId);
                if (platform != null) {
                    platform.setStartPos(null);
                    platform.setEndPos(null);
                    townInterfaceEntity.setChanged();
                    
                    // NEW: Explicitly send BE data packet to the requesting player
                    ClientboundBlockEntityDataPacket bePacket = ClientboundBlockEntityDataPacket.create(townInterfaceEntity);
                    player.connection.send(bePacket);
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Sent explicit BE sync packet to {} for reset platform {} at {}",
                        player.getName().getString(), msg.platformId, msg.townInterfacePos);
                    
                    // Notify nearby players of the change
                    level.sendBlockUpdated(msg.townInterfacePos, 
                                         level.getBlockState(msg.townInterfacePos),
                                         level.getBlockState(msg.townInterfacePos), 
                                         3);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
} 
