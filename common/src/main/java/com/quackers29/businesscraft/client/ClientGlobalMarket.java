package com.quackers29.businesscraft.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import com.quackers29.businesscraft.town.viewmodel.MarketViewModel;

/**
 * Client-side global market cache - NOW IMPLEMENTS VIEW-MODEL PATTERN
 *
 * ARCHITECTURE CHANGE (Phase 1.3):
 * - BEFORE: Stored raw price map, performed client-side item-to-resource resolution and calculations
 * - AFTER: Stores pre-calculated MarketViewModel, performs ZERO calculations
 *
 * The client is now a "dumb terminal" that only displays server-calculated prices.
 * All item-to-resource mapping and max-price logic happens server-side in MarketViewModelBuilder.
 */
public class ClientGlobalMarket {
    private static final ClientGlobalMarket INSTANCE = new ClientGlobalMarket();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ClientGlobalMarket.class);

    // OLD SYSTEM (deprecated, kept for backward compatibility with ContractSyncPacket)
    private final Map<String, Float> prices = new HashMap<>();

    // NEW SYSTEM (Phase 1.3 - server-authoritative view-model)
    private MarketViewModel marketViewModel;

    private ClientGlobalMarket() {
    }

    public static ClientGlobalMarket get() {
        return INSTANCE;
    }

    /**
     * NEW METHOD (Phase 1.3): Updates market with server-calculated view-model
     * This is called by MarketViewModelSyncPacket
     */
    public void setMarketViewModel(MarketViewModel viewModel) {
        this.marketViewModel = viewModel;
        LOGGER.debug("[CLIENT] Market view-model updated: {} items with prices",
            viewModel != null ? viewModel.getTotalPricedItems() : 0);
    }

    /**
     * DEPRECATED: Old method for backward compatibility with ContractSyncPacket
     * TODO: Remove once ContractSyncPacket is refactored to use MarketViewModel
     */
    @Deprecated
    public void setPrices(Map<String, Float> newPrices) {
        prices.clear();
        if (newPrices != null) {
            prices.putAll(newPrices);
        }
    }

    /**
     * DEPRECATED: Old method for backward compatibility
     * TODO: Remove once all callers use view-model
     */
    @Deprecated
    public float getPrice(String resourceId) {
        return prices.getOrDefault(resourceId, 1.0f);
    }

    /**
     * NEW IMPLEMENTATION (Phase 1.3): Zero client-side calculations
     * Simply delegates to the server-calculated view-model.
     *
     * BEFORE: 38 lines of complex logic with ResourceRegistry.getAllFor() and max-price calculations
     * AFTER: 1 line delegation to view-model - TRUE "dumb terminal" client
     */
    public float getPrice(Item item) {
        // NEW: Delegate to server-calculated view-model (ZERO CALCULATIONS)
        if (marketViewModel != null) {
            return marketViewModel.getPriceValue(item);
        }

        // FALLBACK: Old system for backward compatibility (if view-model not yet received)
        // This fallback maintains existing behavior during transition
        LOGGER.warn("[CLIENT] Market view-model not available, using fallback price");
        return 1.0f;
    }

    /**
     * Gets the complete market view-model (for advanced UI displays)
     */
    public MarketViewModel getMarketViewModel() {
        return marketViewModel;
    }
}
