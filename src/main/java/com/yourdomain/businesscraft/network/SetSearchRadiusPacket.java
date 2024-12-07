package com.yourdomain.businesscraft.network;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetSearchRadiusPacket {
    private final BlockPos pos;
    private final int radius;

    public SetSearchRadiusPacket(BlockPos pos, int radius) {
        this.pos = pos;
        this.radius = radius;
    }

    public SetSearchRadiusPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.radius = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(radius);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                if (level.getBlockEntity(pos) instanceof TownBlockEntity townBlock) {
                    townBlock.setSearchRadius(radius);
                }
            }
        });
        context.setPacketHandled(true);
    }
} 