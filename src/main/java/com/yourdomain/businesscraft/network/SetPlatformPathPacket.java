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
import com.yourdomain.businesscraft.network.ModMessages;
import net.minecraft.world.level.Level;

/**
 * Packet sent from client to server to set a platform's path start or end point
 */
public class SetPlatformPathPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    private final UUID platformId;
    private final BlockPos pathPos;
    private final boolean isStart;
    
    public SetPlatformPathPacket(BlockPos pos, UUID platformId, BlockPos pathPos, boolean isStart) {
        this.pos = pos;
        this.platformId = platformId;
        this.pathPos = pathPos;
        this.isStart = isStart;
    }
    
    public SetPlatformPathPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
        this.pathPos = buf.readBlockPos();
        this.isStart = buf.readBoolean();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
        buf.writeBlockPos(pathPos);
        buf.writeBoolean(isStart);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            
            if (be instanceof TownBlockEntity townBlock) {
                String pointType = isStart ? "start" : "end";
                LOGGER.debug("Player {} is setting platform {} {} point to {} at {}", 
                    player.getName().getString(), platformId, pointType, pathPos, pos);
                
                // Set the appropriate path point
                if (isStart) {
                    townBlock.setPlatformPathStart(platformId, pathPos);
                } else {
                    townBlock.setPlatformPathEnd(platformId, pathPos);
                }
                
                LOGGER.debug("Successfully set platform {} {} point to {} at {}", 
                    platformId, pointType, pathPos, pos);
                
                townBlock.setChanged();
                
                // Force a block update to ensure clients get the updated data
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                
                // Notify clients of the update (only affects the specific platform's path points)
                ModMessages.sendToAllTrackingChunk(new RefreshPlatformsPacket(pos), level, pos);
            }
        });
        
        return true;
    }
} 