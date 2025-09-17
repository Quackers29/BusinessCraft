package com.yourdomain.businesscraft.network.packets.platform;

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
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.yourdomain.businesscraft.network.packets.ui.ClientTownMapCache;
import net.minecraft.world.level.Level;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.town.Town;

/**
 * Packet for toggling platform enabled state
 */
public class SetPlatformEnabledPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos blockPos;
    private final UUID platformId;
    private final boolean enabled;
    
    public SetPlatformEnabledPacket(BlockPos blockPos, UUID platformId, boolean enabled) {
        this.blockPos = blockPos;
        this.platformId = platformId;
        this.enabled = enabled;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUUID(platformId);
        buf.writeBoolean(enabled);
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static SetPlatformEnabledPacket decode(FriendlyByteBuf buf) {
        return new SetPlatformEnabledPacket(buf.readBlockPos(), buf.readUUID(), buf.readBoolean());
    }
    
    /**
     * Handle the packet on the receiving side
     */
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // Get player and world from context
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            
            // Check if the block entity is valid
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TownInterfaceEntity townInterface) {
                // Find the platform
                Platform platform = townInterface.getPlatform(platformId);
                if (platform != null) {
                    // Update enabled state
                    platform.setEnabled(enabled);
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Player {} is setting platform {} enabled state to {} at {}", 
                        player.getName().getString(), platformId, enabled, blockPos);
                    
                    // Sync the town interface
                    townInterface.setChanged();
                    level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), 
                        level.getBlockState(blockPos), 3);
                    
                    // Notify clients of the update
                    PlatformAccess.getNetworkMessages().sendToAllTrackingChunk(new RefreshPlatformsPacket(blockPos), level, blockPos);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Failed to set platform {} enabled state - platform not found at {}", 
                        platformId, blockPos);
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
} 