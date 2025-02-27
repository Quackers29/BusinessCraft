package com.yourdomain.businesscraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.event.ModEvents;
import com.yourdomain.businesscraft.api.ITownDataProvider;

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

    public BlockPos getPos() {
        return pos;
    }

    public boolean isEnteringMode() {
        return mode;
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Level level = player.level();
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TownBlockEntity townBlock) {
                    if (mode) {
                        // Start path creation mode
                        player.getPersistentData().putUUID("CurrentTownBlock", 
                                townBlock.getTownId());
                    } else {
                        // Path creation complete
                        ITownDataProvider provider = townBlock.getTownDataProvider();
                        if (provider != null && townBlock.getPathStart() != null && townBlock.getPathEnd() != null) {
                            // Update provider with path data
                            provider.setPathStart(townBlock.getPathStart());
                            provider.setPathEnd(townBlock.getPathEnd());
                            provider.markDirty();
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
} 