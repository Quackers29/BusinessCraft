package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;

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
    private final BlockPos townInterfacePos;
    private final UUID platformId;
    
    public ResetPlatformPathPacket(BlockPos townInterfacePos, UUID platformId) {
        this.townInterfacePos = townInterfacePos;
        this.platformId = platformId;
    }
    
    public static void encode(ResetPlatformPathPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.townInterfacePos);
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
            BlockEntity be = level.getBlockEntity(msg.townInterfacePos);
            
            if (be instanceof TownInterfaceEntity townInterfaceEntity) {
                Platform platform = townInterfaceEntity.getPlatform(msg.platformId);
                if (platform != null) {
                    platform.setStartPos(null);
                    platform.setEndPos(null);
                    townInterfaceEntity.setChanged();
                    
                    // Notify nearby players of the change
                    level.sendBlockUpdated(msg.townInterfacePos, 
                                         level.getBlockState(msg.townInterfacePos),
                                         level.getBlockState(msg.townInterfacePos), 
                                         3);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 