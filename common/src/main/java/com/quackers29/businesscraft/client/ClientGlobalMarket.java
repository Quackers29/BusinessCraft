package com.quackers29.businesscraft.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.viewmodel.MarketViewModel;

public class ClientGlobalMarket {
    private static final ClientGlobalMarket INSTANCE = new ClientGlobalMarket();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ClientGlobalMarket.class);

    private final Map<String, Float> prices = new HashMap<>();
    private MarketViewModel marketViewModel;

    private ClientGlobalMarket() {
    }

    public static ClientGlobalMarket get() {
        return INSTANCE;
    }

    public void reset() {
        prices.clear();
        marketViewModel = null;
        LOGGER.info("ClientGlobalMarket reset - all cached prices cleared");
    }

    public void setMarketViewModel(MarketViewModel viewModel) {
        this.marketViewModel = viewModel;
        DebugConfig.debug(LOGGER, DebugConfig.GLOBAL_MARKET,
                "[CLIENT] Market view-model updated: {} items with prices",
                viewModel != null ? viewModel.getTotalPricedItems() : 0);
    }

    @Deprecated
    public void setPrices(Map<String, Float> newPrices) {
        prices.clear();
        if (newPrices != null) {
            prices.putAll(newPrices);
        }
    }

    @Deprecated
    public float getPrice(String resourceId) {
        return prices.getOrDefault(resourceId, 1.0f);
    }

    public float getPrice(Item item) {
        if (marketViewModel != null) {
            return marketViewModel.getPriceValue(item);
        }
        LOGGER.warn("[CLIENT] Market view-model not available, using fallback price");
        return 1.0f;
    }

    public MarketViewModel getMarketViewModel() {
        return marketViewModel;
    }
}
