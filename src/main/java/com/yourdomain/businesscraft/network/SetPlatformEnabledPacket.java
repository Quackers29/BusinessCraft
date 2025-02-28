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
 * Packet sent from client to server to toggle a platform's enabled state
 */
public class SetPlatformEnabledPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    private final UUID platformId;
    private final boolean enabled;
    
    public SetPlatformEnabledPacket(BlockPos pos, UUID platformId, boolean enabled) {
        this.pos = pos;
        this.platformId = platformId;
        this.enabled = enabled;
    }
    
    public SetPlatformEnabledPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
        this.enabled = buf.readBoolean();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
        buf.writeBoolean(enabled);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            
            if (be instanceof TownBlockEntity townBlock) {
                LOGGER.debug("Player {} is setting platform {} enabled state to {} at {}", 
                    player.getName().getString(), platformId, enabled, pos);
                
                // Get the platform
                com.yourdomain.businesscraft.platform.Platform platform = townBlock.getPlatform(platformId);
                if (platform != null) {
                    // Update its state
                    platform.setEnabled(enabled);
                    LOGGER.debug("Successfully set platform {} enabled state to {} at {}", 
                        platformId, enabled, pos);
                    townBlock.setChanged();
                    
                    // Force a block update to ensure clients get the updated data
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    
                    // Notify clients of the update
                    ModMessages.sendToAllTrackingChunk(new RefreshPlatformsPacket(pos), level, pos);
                } else {
                    LOGGER.debug("Failed to set platform {} enabled state - platform not found at {}", 
                        platformId, pos);
                }
            }
        });
        
        return true;
    }
} 