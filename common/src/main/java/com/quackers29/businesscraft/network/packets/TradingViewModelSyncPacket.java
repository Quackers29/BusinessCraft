package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.viewmodel.TradingViewModel;
import com.quackers29.businesscraft.ui.managers.TownDataCacheManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Packet to sync TradingViewModel from server to client.
 */
public class TradingViewModelSyncPacket {

    private final TradingViewModel viewModel;

    public TradingViewModelSyncPacket(TradingViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public TradingViewModelSyncPacket(FriendlyByteBuf buf) {
        // Read currency name
        String currencyName = buf.readUtf();
        String lastUpdated = buf.readUtf();

        // Read map size
        int size = buf.readInt();
        Map<String, TradingViewModel.TradingResourceInfo> map = new HashMap<>();

        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            String displayName = buf.readUtf();
            String stockDisplay = buf.readUtf();
            float currentStock = buf.readFloat();
            float maxStock = buf.readFloat();
            float pricePerUnit = buf.readFloat();
            String priceDisplay = buf.readUtf();
            boolean canBuy = buf.readBoolean();
            boolean canSell = buf.readBoolean();
            String statusText = buf.readUtf();
            String tooltipText = buf.readUtf();

            map.put(id, new TradingViewModel.TradingResourceInfo(
                    id, displayName, stockDisplay, currentStock, maxStock,
                    pricePerUnit, priceDisplay, canBuy, canSell, statusText, tooltipText));
        }

        this.viewModel = new TradingViewModel(map, currencyName, lastUpdated);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(viewModel.getCurrencyName());
        buf.writeUtf(viewModel.getLastUpdatedText());

        Map<String, TradingViewModel.TradingResourceInfo> map = viewModel.getResourceInfo();
        buf.writeInt(map.size());

        for (TradingViewModel.TradingResourceInfo info : map.values()) {
            buf.writeUtf(info.resourceId());
            buf.writeUtf(info.displayName());
            buf.writeUtf(info.stockDisplay());
            buf.writeFloat(info.currentStock());
            buf.writeFloat(info.maxStock());
            buf.writeFloat(info.pricePerUnit());
            buf.writeUtf(info.priceDisplay());
            buf.writeBoolean(info.canBuy());
            buf.writeBoolean(info.canSell());
            buf.writeUtf(info.statusText());
            buf.writeUtf(info.tooltipText());
        }
    }

    public static void encode(TradingViewModelSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static TradingViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new TradingViewModelSyncPacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            TownDataCacheManager.updateTradingViewModel(viewModel);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
