package com.yourdomain.businesscraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import java.util.function.BiConsumer;

/**
 * Base class for packets that interact with a TownBlockEntity
 * Reduces code duplication between similar packets
 */
public abstract class BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBlockEntityPacket.class);
    protected final BlockPos pos;

    public BaseBlockEntityPacket(BlockPos pos) {
        this.pos = pos;
    }

    public BaseBlockEntityPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    /**
     * Handle the packet by finding the block entity and applying an action
     */
    protected void handlePacket(NetworkEvent.Context context, BiConsumer<ServerPlayer, TownBlockEntity> handler) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownBlockEntity) {
                TownBlockEntity townBlock = (TownBlockEntity) be;
                handler.accept(player, townBlock);
            } else {
                LOGGER.warn("Received packet for position {} but no TownBlockEntity found", pos);
            }
        }
    }
    
    public BlockPos getPos() {
        return pos;
    }
} 