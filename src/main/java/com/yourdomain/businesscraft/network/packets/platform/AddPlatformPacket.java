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
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.platform.RefreshPlatformsPacket;

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
            // Get player and world from context
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            Level level = player.level();
            
            // Check if the block entity is valid
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TownBlockEntity townBlock) {
                LOGGER.debug("Player {} is adding a new platform to town block at {}", player.getName().getString(), blockPos);
                
                if (townBlock.canAddMorePlatforms()) {
                    boolean added = townBlock.addPlatform();
                    if (added) {
                        LOGGER.debug("Successfully added new platform to town block at {}", blockPos);
                        townBlock.setChanged();
                        
                        // Force a block update to ensure clients get the updated data
                        level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);
                        
                        // Notify client of the update
                        ModMessages.sendToAllTrackingChunk(new RefreshPlatformsPacket(blockPos), level, blockPos);
                    } else {
                        LOGGER.debug("Failed to add platform to town block at {} - already at max capacity", blockPos);
                    }
                } else {
                    LOGGER.debug("Failed to add platform to town block at {} - already at max capacity", blockPos);
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
} 