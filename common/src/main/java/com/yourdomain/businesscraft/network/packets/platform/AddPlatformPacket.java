package com.yourdomain.businesscraft.network.packets.platform;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * Packet for adding a new platform to a town
 */
public class AddPlatformPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos blockPos;
    
    public AddPlatformPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static AddPlatformPacket decode(FriendlyByteBuf buf) {
        return new AddPlatformPacket(buf.readBlockPos());
    }
    
    /**
     * Handle the packet on the receiving side
     */
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            try {
                // Get player and world from context
                ServerPlayer player = context.getSender();
                if (player == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "AddPlatformPacket: No player sender");
                    return;
                }

                Level level = player.level();

                // Check if the block entity is valid
                BlockEntity be = level.getBlockEntity(blockPos);
                if (be instanceof TownInterfaceEntity townInterface) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Player {} is adding a new platform to town block at {}", player.getName().getString(), blockPos);
                
                if (townInterface.canAddMorePlatforms()) {
                    boolean added = townInterface.addPlatform();
                    if (added) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "Successfully added new platform to town block at {}", blockPos);
                        townInterface.setChanged();
                        
                        // Force a block update to ensure clients get the updated data
                        level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);
                        
                        // Notify client of the update
                        PlatformAccess.getNetworkMessages().sendToAllTrackingChunk(new RefreshPlatformsPacket(blockPos), level, blockPos);
                    } else {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Failed to add platform to town block at {} - already at max capacity", blockPos);
                    }
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Failed to add platform to town block at {} - already at max capacity", blockPos);
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "AddPlatformPacket: Block entity at {} is not a TownInterfaceEntity", blockPos);
            }
            } catch (Exception e) {
                LOGGER.error("Error handling AddPlatformPacket", e);
                // Don't crash the server, but log the error
            }
        });
        context.setPacketHandled(true);
        return true;
    }
} 