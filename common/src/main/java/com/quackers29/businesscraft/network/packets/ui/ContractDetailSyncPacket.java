package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.viewmodel.ContractDetailViewModel;
import com.quackers29.businesscraft.ui.managers.TownDataCacheManager;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server-to-client packet containing full contract details.
 * Sent in response to RequestContractDetailPacket.
 */
public class ContractDetailSyncPacket {

    private final ContractDetailViewModel detail;
    private final long serverCurrentTime;

    public ContractDetailSyncPacket(ContractDetailViewModel detail, long serverCurrentTime) {
        this.detail = detail;
        this.serverCurrentTime = serverCurrentTime;
    }

    public ContractDetailSyncPacket(FriendlyByteBuf buf) {
        this.serverCurrentTime = buf.readLong();
        this.detail = new ContractDetailViewModel(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(serverCurrentTime);
        detail.toBytes(buf);
    }

    public static void encode(ContractDetailSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static ContractDetailSyncPacket decode(FriendlyByteBuf buf) {
        return new ContractDetailSyncPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Update client-side cache with contract detail
            TownDataCacheManager.updateContractDetail(detail, serverCurrentTime);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }

    // Getters
    public ContractDetailViewModel getDetail() { return detail; }
    public long getServerCurrentTime() { return serverCurrentTime; }
}
