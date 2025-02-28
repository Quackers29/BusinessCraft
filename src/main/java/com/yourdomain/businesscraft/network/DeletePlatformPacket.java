package com.yourdomain.businesscraft.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.world.level.Level;

/**
 * Packet sent from client to server to delete a platform from a town block
 */
public class DeletePlatformPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    private final UUID platformId;
    
    public DeletePlatformPacket(BlockPos pos, UUID platformId) {
        this.pos = pos;
        this.platformId = platformId;
    }
    
    public DeletePlatformPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            
            if (be instanceof TownBlockEntity townBlock) {
                LOGGER.debug("Player {} is deleting platform {} from town block at {}", 
                    player.getName().getString(), platformId, pos);
                
                boolean removed = townBlock.removePlatform(platformId);
                if (removed) {
                    LOGGER.debug("Successfully deleted platform {} from town block at {}", platformId, pos);
                    townBlock.setChanged();
                    
                    // Force a block update to ensure clients get the updated data
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    
                    // Notify clients of the update
                    ModMessages.sendToAllTrackingChunk(new RefreshPlatformsPacket(pos), level, pos);
                } else {
                    LOGGER.debug("Failed to delete platform {} from town block at {}", platformId, pos);
                }
            }
        });
        
        return true;
    }
} 