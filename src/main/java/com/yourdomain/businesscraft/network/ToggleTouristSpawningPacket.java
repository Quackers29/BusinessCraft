package com.yourdomain.businesscraft.network;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleTouristSpawningPacket {
    private final BlockPos pos;

    public ToggleTouristSpawningPacket(BlockPos pos) {
        this.pos = pos;
    }

    public ToggleTouristSpawningPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Level level = player.level();
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TownBlockEntity townBlock) {
                    townBlock.getContainerData().set(3, 
                        townBlock.getContainerData().get(3) == 0 ? 1 : 0);
                }
            }
        });
        context.setPacketHandled(true);
    }
} 