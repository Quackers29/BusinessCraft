package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModel;
import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModelBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packet to sync paginated contract summaries from server to client.
 * Replaces ContractSyncPacket for list view with lightweight summaries.
 */
public class ContractListSyncPacket {

    private final String tab; // "auction", "active", "history"
    private final List<ContractSummaryViewModel> contracts;
    private final int page;
    private final int pageSize;
    private final int totalCount;
    private final boolean hasMore;
    private final long serverCurrentTime;
    private final Map<String, Float> marketPrices;

    public ContractListSyncPacket(
            String tab,
            List<ContractSummaryViewModel> contracts,
            int page,
            int pageSize,
            int totalCount,
            boolean hasMore,
            long serverCurrentTime,
            Map<String, Float> marketPrices) {
        this.tab = tab;
        this.contracts = contracts;
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.hasMore = hasMore;
        this.serverCurrentTime = serverCurrentTime;
        this.marketPrices = marketPrices;
    }

    /**
     * Convenience constructor from builder result.
     */
    public ContractListSyncPacket(
            String tab,
            ContractSummaryViewModelBuilder.ContractListResult result,
            long serverCurrentTime,
            Map<String, Float> marketPrices) {
        this(
                tab,
                result.contracts(),
                result.page(),
                result.pageSize(),
                result.totalCount(),
                result.hasMore(),
                serverCurrentTime,
                marketPrices
        );
    }

    /**
     * Deserialize from network buffer.
     */
    public ContractListSyncPacket(FriendlyByteBuf buf) {
        this.tab = buf.readUtf();
        this.page = buf.readInt();
        this.pageSize = buf.readInt();
        this.totalCount = buf.readInt();
        this.hasMore = buf.readBoolean();
        this.serverCurrentTime = buf.readLong();

        // Read contracts
        int contractCount = buf.readInt();
        this.contracts = new ArrayList<>(contractCount);
        for (int i = 0; i < contractCount; i++) {
            this.contracts.add(new ContractSummaryViewModel(buf));
        }

        // Read market prices
        int priceCount = buf.readInt();
        this.marketPrices = new HashMap<>(priceCount);
        for (int i = 0; i < priceCount; i++) {
            this.marketPrices.put(buf.readUtf(), buf.readFloat());
        }
    }

    /**
     * Serialize to network buffer.
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(tab);
        buf.writeInt(page);
        buf.writeInt(pageSize);
        buf.writeInt(totalCount);
        buf.writeBoolean(hasMore);
        buf.writeLong(serverCurrentTime);

        // Write contracts
        buf.writeInt(contracts.size());
        for (ContractSummaryViewModel vm : contracts) {
            vm.toBytes(buf);
        }

        // Write market prices
        buf.writeInt(marketPrices.size());
        marketPrices.forEach((k, v) -> {
            buf.writeUtf(k);
            buf.writeFloat(v);
        });
    }

    public static void encode(ContractListSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static ContractListSyncPacket decode(FriendlyByteBuf buf) {
        return new ContractListSyncPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Update client-side cache with the new contract list
            com.quackers29.businesscraft.ui.managers.TownDataCacheManager
                    .updateContractList(tab, contracts, page, pageSize, totalCount, hasMore, serverCurrentTime);

            // Update market prices
            com.quackers29.businesscraft.client.ClientGlobalMarket.get().setPrices(marketPrices);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }

    // Getters for client-side access
    public String getTab() { return tab; }
    public List<ContractSummaryViewModel> getContracts() { return contracts; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotalCount() { return totalCount; }
    public boolean hasMore() { return hasMore; }
    public long getServerCurrentTime() { return serverCurrentTime; }
    public Map<String, Float> getMarketPrices() { return marketPrices; }
}
