package com.yourdomain.businesscraft.network.packets.platform;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.platform.Platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to reset the path of a platform
 */
public class ResetPlatformPathPacket {
    private final BlockPos townBlockPos;
    private final UUID platformId;
    
    public ResetPlatformPathPacket(BlockPos townBlockPos, UUID platformId) {
        this.townBlockPos = townBlockPos;
        this.platformId = platformId;
    }
    
    public static void encode(ResetPlatformPathPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.townBlockPos);
        buf.writeUUID(msg.platformId);
    }
    
    public static ResetPlatformPathPacket decode(FriendlyByteBuf buf) {
        return new ResetPlatformPathPacket(
            buf.readBlockPos(),
            buf.readUUID()
        );
    }
    
    public static void handle(ResetPlatformPathPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.townBlockPos);
            
            if (be instanceof TownBlockEntity townBlockEntity) {
                Platform platform = townBlockEntity.getPlatform(msg.platformId);
                if (platform != null) {
                    platform.setStartPos(null);
                    platform.setEndPos(null);
                    townBlockEntity.setChanged();
                    
                    // Notify nearby players of the change
                    level.sendBlockUpdated(msg.townBlockPos, 
                                         level.getBlockState(msg.townBlockPos),
                                         level.getBlockState(msg.townBlockPos), 
                                         3);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 