package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.CourierContract;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Packet sent from client to server when a player accepts a contract.
 */
public class AcceptContractPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptContractPacket.class);

    private final UUID contractId;

    public AcceptContractPacket(UUID contractId) {
        this.contractId = contractId;
    }

    public AcceptContractPacket(FriendlyByteBuf buf) {
        this.contractId = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(contractId);
    }

    // Static methods for Forge network registration
    public static void encode(AcceptContractPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static AcceptContractPacket decode(FriendlyByteBuf buf) {
        return new AcceptContractPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                ContractBoard board = ContractBoard.getInstance();
                Contract contract = board.getContract(contractId);

                if (contract instanceof CourierContract courierContract) {
                    // Logic to accept the contract
                    if (courierContract.getCourierId() == null) {
                        courierContract.setCourierId(player.getUUID());
                        board.updateContract(courierContract);
                        LOGGER.info("Player {} accepted contract {}", player.getName().getString(), contractId);

                        // Sync back to client so UI updates
                        PlatformAccess.getNetworkMessages().sendToPlayer(
                                new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(
                                        board.getContracts()),
                                player);
                    } else {
                        LOGGER.warn("Player {} tried to accept already assigned contract {}",
                                player.getName().getString(), contractId);
                    }
                } else {
                    LOGGER.warn("Player {} tried to accept invalid or non-courier contract {}",
                            player.getName().getString(), contractId);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
