package com.yourdomain.businesscraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;

import java.util.function.Supplier;

public class SetPathCreationModePacket {
    private final BlockPos pos;
    private final boolean mode;

    public SetPathCreationModePacket(BlockPos pos, boolean mode) {
        this.pos = pos;
        this.mode = mode;
    }

    public static void encode(SetPathCreationModePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.mode);
    }

    public static SetPathCreationModePacket decode(FriendlyByteBuf buf) {
        return new SetPathCreationModePacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(SetPathCreationModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Level level = player.level();
                BlockEntity be = level.getBlockEntity(msg.pos);
                
                if (be instanceof TownBlockEntity townBlock) {
                    townBlock.setPathCreationMode(msg.mode);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 