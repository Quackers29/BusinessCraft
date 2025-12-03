package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.CourierContract;
import com.quackers29.businesscraft.contract.SellContract;
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
                Contract contract = board.getContract(contractId);
                if (contract == null || contract.isExpired()) {
                    LOGGER.warn("Invalid bid on non-existent/expired contract {} by {}", contractId,
                            player.getName().getString());
                    return;
                }
                if (contract instanceof CourierContract cc && amount == 0f && cc.getCourierId() == null) {
                    // Player accepting courier mission - delegate to ContractBoard
                    board.addBid(contractId, player.getUUID(), amount, level);
                    // Logging handled in ContractBoard
                } else if (contract instanceof SellContract sc && amount > 0f) {
                    // Normal SellContract bid
                    int quantity = sc.getQuantity();
                    if (quantity <= 0) {
                        LOGGER.warn("Bid rejected on 0-quantity SellContract {} by {}", contractId,
                                player.getName().getString());
                        return;
                    }
                    board.addBid(contractId, player.getUUID(), amount, level);
                    LOGGER.info("Player {} bid {} on SellContract {}", player.getName().getString(), amount,
                            contractId);
                } else {
                    LOGGER.warn("Invalid bid parameters for contract {} by {}", contractId,
                            player.getName().getString());
                    return;
                }
                // Sync updated contracts
                PlatformAccess.getNetworkMessages().sendToPlayer(
                        new ContractSyncPacket(board.getContracts()), player);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
