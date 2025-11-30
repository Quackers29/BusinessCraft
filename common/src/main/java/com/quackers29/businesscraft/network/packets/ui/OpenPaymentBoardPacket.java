package com.quackers29.businesscraft.network.packets.ui;

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

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void encode(OpenPaymentBoardPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static OpenPaymentBoardPacket decode(FriendlyByteBuf buf) {
        return new OpenPaymentBoardPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                // Open payment board menu
                // Assume TownBlockEntity.createPaymentBoardMenuProvider(pos)
                // Placeholder - use existing logic
                LOGGER.info("Opening Payment Board for player {} at {}", player.getName().getString(), pos);
                // PlatformAccess.getNetwork().openScreen(player, menuProvider, pos);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
