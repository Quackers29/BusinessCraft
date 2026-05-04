package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.client.ClientGlobalMarket;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.viewmodel.MarketViewModel;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NEW server-authoritative market price sync packet implementing the "View-Model" pattern.
 *
 * This packet REPLACES the market price sync logic in ContractSyncPacket which violated
 * the server-authoritative principle by sending raw price maps that required client-side
 * item-to-resource resolution and price calculations.
 *
 * KEY DIFFERENCES FROM OLD APPROACH:
 * - Sends pre-calculated prices for ALL items (not just resource IDs)
 * - Client performs ZERO item-to-resource mapping (no ResourceRegistry.getAllFor on client)
 * - Client performs ZERO max-price calculations for multi-type items
 * - Implements true "dumb terminal" client architecture for market prices
 * - All business logic happens server-side in MarketViewModelBuilder
 *
 * IMPORTANT: Market prices are GLOBAL (not per-block-entity or per-town)
 * - GlobalMarket is a singleton on the server
 * - ClientGlobalMarket is a singleton on the client
 * - This packet syncs the global market state to all connected clients
 */
public class MarketViewModelSyncPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketViewModelSyncPacket.class);

    private final MarketViewModel marketViewModel;

    /**
     * Creates a new market view-model sync packet (SERVER-SIDE)
     * @param marketViewModel Pre-calculated market view-model from server
     */
    public MarketViewModelSyncPacket(MarketViewModel marketViewModel) {
        this.marketViewModel = marketViewModel;
    }

    /**
     * Deserializes packet from network buffer (CLIENT-SIDE)
     */
    public MarketViewModelSyncPacket(FriendlyByteBuf buf) {
        this.marketViewModel = new MarketViewModel(buf);
    }

    /**
     * Serializes packet to network buffer (SERVER-SIDE)
     */
    public void toBytes(FriendlyByteBuf buf) {
        marketViewModel.toBytes(buf);
    }

    public static void encode(MarketViewModelSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static MarketViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new MarketViewModelSyncPacket(buf);
    }

    /**
     * Handles packet on client side - PURE DISPLAY LOGIC ONLY
     * No calculations, no business logic, just updating the global market cache
     */
    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            DebugConfig.debug(LOGGER, DebugConfig.GLOBAL_MARKET, "[CLIENT] MarketViewModelSyncPacket received");

            // Update the global client market cache with pre-calculated view-model
            // NO CALCULATIONS HAPPEN HERE - client is truly a "dumb terminal"
            ClientGlobalMarket.get().setMarketViewModel(marketViewModel);

            DebugConfig.debug(LOGGER, DebugConfig.GLOBAL_MARKET,
                "[CLIENT] Market view-model updated: {} items with known prices, status: {}",
                marketViewModel.getTotalPricedItems(),
                marketViewModel.getMarketStatus());
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
