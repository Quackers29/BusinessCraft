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

    private void write(FriendlyByteBuf buf) {
        buf.writeUUID(contractId);
        buf.writeFloat(amount);
    }

    public static void encode(BidContractPacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static BidContractPacket decode(FriendlyByteBuf buf) {
        return new BidContractPacket(buf);
    }

    public void handle(Object context) {
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
                } else if (contract instanceof SellContract sc) {
                    if (amount > 0f) {
                        // Normal SellContract bid
                        long quantity = sc.getQuantity();
                        if (quantity <= 0) {
                            LOGGER.warn("Bid rejected on 0-quantity SellContract {} by {}", contractId,
                                    player.getName().getString());
                            return;
                        }
                        board.addBid(contractId, player.getUUID(), amount, level);
                    } else if (amount == 0f && sc.isAuctionClosed() && !sc.isCourierAssigned()) {
                        // Courier acceptance for SellContract
                        board.addBid(contractId, player.getUUID(), amount, level);
                        // Logging handled in ContractBoard
                    } else {
                        LOGGER.warn("Invalid bid amount {} for SellContract {} by {}", amount, contractId,
                                player.getName().getString());
                        return;
                    }
                } else {
                    LOGGER.warn("Invalid bid parameters for contract {} by {}", contractId,
                            player.getName().getString());
                    return;
                }
                PlatformAccess.getNetworkMessages().sendToPlayer(
                        new ContractSyncPacket(board.getContracts(), board.getAllMarketPrices()), player);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}

