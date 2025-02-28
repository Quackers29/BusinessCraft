package com.yourdomain.businesscraft.network;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;

/**
 * Packet sent from client to server to add a new platform to a town block
 */
public class AddPlatformPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    
    public AddPlatformPacket(BlockPos pos) {
        this.pos = pos;
    }
    
    public AddPlatformPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            
            if (be instanceof TownBlockEntity townBlock) {
                LOGGER.debug("Player {} is adding a new platform to town block at {}", player.getName().getString(), pos);
                
                if (townBlock.canAddMorePlatforms()) {
                    boolean added = townBlock.addPlatform();
                    if (added) {
                        LOGGER.debug("Successfully added new platform to town block at {}", pos);
                        townBlock.setChanged();
                        
                        // Force a block update to ensure clients get the updated data
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                        
                        // Notify client of the update
                        ModMessages.sendToAllTrackingChunk(new RefreshPlatformsPacket(pos), level, pos);
                    } else {
                        LOGGER.debug("Failed to add platform to town block at {} - already at max capacity", pos);
                    }
                } else {
                    LOGGER.debug("Failed to add platform to town block at {} - already at max capacity", pos);
                }
            }
        });
        
        return true;
    }
} 