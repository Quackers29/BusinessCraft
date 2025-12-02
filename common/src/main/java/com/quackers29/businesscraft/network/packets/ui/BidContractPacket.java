package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.ContractBoard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class BidContractPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BidContractPacket.class);

    private final UUID contractId;
    private final float amount;

    public BidContractPacket(UUID contractId, float amount) {
        this.contractId = contractId;
        this.amount = amount;
    }

    public BidContractPacket(FriendlyByteBuf buf) {
        this.contractId = buf.readUUID();
        this.amount = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(contractId);
        buf.writeFloat(amount);
    }

    public static void encode(BidContractPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static BidContractPacket decode(FriendlyByteBuf buf) {
        return new BidContractPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
                ContractBoard board = ContractBoard.get(level);
                board.addBid(contractId, player.getUUID(), amount);
                LOGGER.info("Player {} bid {} on contract {}", player.getName().getString(), amount, contractId);
                // Sync updated to player
                PlatformAccess.getNetworkMessages().sendToPlayer(
                        new ContractSyncPacket(board.getContracts()), player);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
