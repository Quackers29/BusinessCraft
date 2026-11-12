package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.LoadedTownAccess;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenPaymentBoardPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenPaymentBoardPacket.class);

    private final BlockPos pos;

    public OpenPaymentBoardPacket(BlockPos pos) {
        this.pos = pos;
    }

    public OpenPaymentBoardPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void encode(OpenPaymentBoardPacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static OpenPaymentBoardPacket decode(FriendlyByteBuf buf) {
        return new OpenPaymentBoardPacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                var townInterface = LoadedTownAccess.getIfLoadedInReach(player, pos);
                if (townInterface != null) {
                    // Open the PaymentBoardMenu using the entity's provider method
                    LOGGER.info("Opening Payment Board for player {} at {}", player.getName().getString(), pos);
                    PlatformAccess.getNetwork().openScreen(player, townInterface.createPaymentBoardMenuProvider(), pos);
                } else {
                    LOGGER.debug("No Town Centre in reach at {}", pos);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}

