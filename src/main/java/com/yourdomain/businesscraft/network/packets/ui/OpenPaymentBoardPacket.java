package com.yourdomain.businesscraft.network.packets.ui;

import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Packet to request opening the Payment Board UI using proper Minecraft container system.
 * Sent from client to server to request opening the Payment Board.
 */
public class OpenPaymentBoardPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenPaymentBoardPacket.class);

    public OpenPaymentBoardPacket(BlockPos pos) {
        super(pos);
    }

    public OpenPaymentBoardPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            handlePacket(context, (player, townBlockEntity) -> {
                LOGGER.info("Opening Payment Board for player {} at position {}", player.getName().getString(), pos);
                
                // Use NetworkHooks to properly open the container with server-client sync
                NetworkHooks.openScreen(player, townBlockEntity.createPaymentBoardMenuProvider(), pos);
                LOGGER.info("Successfully opened Payment Board for player {}", player.getName().getString());
            });
        });
        return true;
    }
}