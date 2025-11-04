package com.quackers29.businesscraft.network.packets.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * Base class for packets that interact with a TownInterfaceEntity
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
     * Platform-agnostic: uses PlatformAccess to get sender from context
     */
    protected void handlePacket(Object context, BiConsumer<ServerPlayer, TownInterfaceEntity> handler) {
        Object senderObj = PlatformAccess.getNetwork().getSender(context);
        if (senderObj instanceof ServerPlayer player) {
            Level level = player.level();

            // First check if there's a TownInterfaceEntity at this position
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity) {
                TownInterfaceEntity townInterface = (TownInterfaceEntity) be;
                handler.accept(player, townInterface);
                return;
            }

            // If not, check if there's a TownInterfaceBlock at this position
            // In this case, we need to find the TownInterfaceEntity associated with it
            BlockState blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof TownInterfaceBlock) {
                // TownInterfaceBlock is backed by a TownInterfaceEntity, so get that
                BlockEntity townBe = level.getBlockEntity(pos);
                if (townBe instanceof TownInterfaceEntity) {
                    TownInterfaceEntity townInterface = (TownInterfaceEntity) townBe;
                    handler.accept(player, townInterface);
                    return;
                }
            }

            // If we get here, we couldn't find a suitable block entity
            LOGGER.warn("Received packet for position {} but no TownInterfaceEntity or TownInterfaceBlock found", pos);
        }
    }

    public BlockPos getPos() {
        return pos;
    }
} 
