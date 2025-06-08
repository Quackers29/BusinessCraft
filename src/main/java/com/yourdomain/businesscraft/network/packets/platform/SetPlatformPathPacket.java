package com.yourdomain.businesscraft.network.packets.platform;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.network.ModMessages;
import net.minecraft.world.level.Level;
import com.yourdomain.businesscraft.platform.Platform;

/**
 * Packet for setting a platform's path
 */
public class SetPlatformPathPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformPathPacket.class);
    private final BlockPos blockPos;
    private final UUID platformId;
    private final BlockPos startPos;
    private final BlockPos endPos;
    
    public SetPlatformPathPacket(BlockPos blockPos, UUID platformId, BlockPos startPos, BlockPos endPos) {
        this.blockPos = blockPos;
        this.platformId = platformId;
        this.startPos = startPos;
        this.endPos = endPos;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUUID(platformId);
        buf.writeBlockPos(startPos);
        buf.writeBlockPos(endPos);
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static SetPlatformPathPacket decode(FriendlyByteBuf buf) {
        return new SetPlatformPathPacket(
            buf.readBlockPos(),
            buf.readUUID(),
            buf.readBlockPos(),
            buf.readBlockPos()
        );
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
                // Find the platform
                Platform platform = townBlock.getPlatform(platformId);
                if (platform != null) {
                    // Update start and end positions
                    platform.setStartPos(startPos);
                    platform.setEndPos(endPos);
                    LOGGER.info("Updated platform {} path from {} to {}", platformId, startPos, endPos);
                    
                    // Sync the town block
                    townBlock.setChanged();
                    level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), 
                        level.getBlockState(blockPos), 3);
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
} 