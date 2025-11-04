package com.quackers29.businesscraft.network.packets.platform;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import net.minecraft.world.level.Level;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet for deleting a platform from a town
 */
public class DeletePlatformPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos blockPos;
    private final UUID platformId;
    
    public DeletePlatformPacket(BlockPos blockPos, UUID platformId) {
        this.blockPos = blockPos;
        this.platformId = platformId;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUUID(platformId);
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static DeletePlatformPacket decode(FriendlyByteBuf buf) {
        return new DeletePlatformPacket(buf.readBlockPos(), buf.readUUID());
    }
    
    /**
     * Handle the packet on the receiving side
     */
    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Get player and world from context
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) return;
            
            Level level = player.level();
            
            // Check if the block entity is valid
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TownInterfaceEntity townInterface) {
                // Delete the platform
                boolean deleted = townInterface.removePlatform(platformId);
                if (deleted) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Deleted platform {} from town at {}", platformId, blockPos);
                    
                    // Sync the town block
                    townInterface.setChanged();
                    level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), 
                        level.getBlockState(blockPos), 3);
                    
                    // Notify clients of the update
                    PlatformAccess.getNetworkMessages().sendToAllTrackingChunk(new RefreshPlatformsPacket(blockPos), level, blockPos);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Failed to delete platform {} from town at {}", platformId, blockPos);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
} 
