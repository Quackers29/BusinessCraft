package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModelBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-to-server packet requesting a paginated list of contracts for a specific tab.
 * Server responds with ContractListSyncPacket.
 */
public class RequestContractListPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestContractListPacket.class);

    private final String tab; // "auction", "active", "history"
    private final int page;
    private final int pageSize;

    public RequestContractListPacket(String tab, int page, int pageSize) {
        this.tab = tab;
        this.page = page;
        this.pageSize = pageSize;
    }

    public RequestContractListPacket(FriendlyByteBuf buf) {
        this.tab = buf.readUtf();
        this.page = buf.readInt();
        this.pageSize = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(tab);
        buf.writeInt(page);
        buf.writeInt(pageSize);
    }

    public static void encode(RequestContractListPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static RequestContractListPacket decode(FriendlyByteBuf buf) {
        return new RequestContractListPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (senderObj instanceof ServerPlayer player) {
                net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
                ContractBoard board = ContractBoard.get(level);

                // Parse tab type
                ContractSummaryViewModelBuilder.Tab tabType = parseTab(tab);
                if (tabType == null) {
                    LOGGER.warn("Invalid tab type '{}' requested by {}", tab, player.getName().getString());
                    return;
                }

                // Build paginated result
                long serverTime = System.currentTimeMillis();
                ContractSummaryViewModelBuilder.ContractListResult result =
                        ContractSummaryViewModelBuilder.build(
                                board.getContracts(),
                                tabType,
                                page,
                                pageSize,
                                player,
                                serverTime
                        );

                // Send response
                ContractListSyncPacket response = new ContractListSyncPacket(
                        tab,
                        result,
                        serverTime,
                        board.getAllMarketPrices()
                );

                PlatformAccess.getNetworkMessages().sendToPlayer(response, player);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }

    private static ContractSummaryViewModelBuilder.Tab parseTab(String tab) {
        return switch (tab.toLowerCase()) {
            case "auction" -> ContractSummaryViewModelBuilder.Tab.AUCTION;
            case "active" -> ContractSummaryViewModelBuilder.Tab.ACTIVE;
            case "history" -> ContractSummaryViewModelBuilder.Tab.HISTORY;
            default -> null;
        };
    }

    // Getters
    public String getTab() { return tab; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
