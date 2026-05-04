package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.viewmodel.ContractDetailViewModel;
import com.quackers29.businesscraft.contract.viewmodel.ContractDetailViewModelBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Client-to-server packet requesting full details for a specific contract.
 * Server responds with ContractDetailSyncPacket.
 */
public class RequestContractDetailPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestContractDetailPacket.class);

    private final UUID contractId;

    public RequestContractDetailPacket(UUID contractId) {
        this.contractId = contractId;
    }

    public RequestContractDetailPacket(FriendlyByteBuf buf) {
        this.contractId = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(contractId);
    }

    public static void encode(RequestContractDetailPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static RequestContractDetailPacket decode(FriendlyByteBuf buf) {
        return new RequestContractDetailPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
                ContractBoard board = ContractBoard.get(level);
                Contract contract = board.getContract(contractId);

                if (contract == null) {
                    LOGGER.warn("Contract {} not found, requested by {}", contractId, player.getName().getString());
                    return;
                }

                long serverTime = System.currentTimeMillis();
                ContractDetailViewModel detail = ContractDetailViewModelBuilder.build(contract, player, serverTime);

                if (detail != null) {
                    ContractDetailSyncPacket response = new ContractDetailSyncPacket(detail, serverTime);
                    PlatformAccess.getNetworkMessages().sendToPlayer(response, player);
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }

    public UUID getContractId() { return contractId; }
}
