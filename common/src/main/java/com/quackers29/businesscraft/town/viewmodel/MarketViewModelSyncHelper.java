package com.quackers29.businesscraft.town.viewmodel;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.MarketViewModelSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends {@link MarketViewModel} to clients (login, town UI open, or after
 * {@link com.quackers29.businesscraft.economy.GlobalMarket} mutations).
 */
public final class MarketViewModelSyncHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketViewModelSyncHelper.class);

    private MarketViewModelSyncHelper() {
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        MarketViewModel viewModel = MarketViewModelBuilder.buildMarketViewModel();
        MarketViewModelSyncPacket packet = new MarketViewModelSyncPacket(viewModel);
        PlatformAccess.getNetworkMessages().sendToPlayer(packet, player);
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS,
                "Market view-model sent to player {} - {} priced items, {}",
                player.getName().getString(), viewModel.getTotalPricedItems(), viewModel.getMarketStatus());
    }
}
