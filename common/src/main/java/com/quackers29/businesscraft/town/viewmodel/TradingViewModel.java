package com.quackers29.businesscraft.town.viewmodel;

import java.util.Collections;
import java.util.Map;

/**
 * View-Model for the trading interface.
 * Contains all pre-calculated display data needed to render the trading screen.
 * This is server-authoritative data - the client simply renders this info.
 */
public class TradingViewModel {

    // Info for each tradable resource
    private final Map<String, TradingResourceInfo> resourceInfo;

    // Global trading settings/status
    private final String currencyName;
    private final String lastUpdatedText;

    public TradingViewModel(Map<String, TradingResourceInfo> resourceInfo, String currencyName,
            String lastUpdatedText) {
        this.resourceInfo = resourceInfo;
        this.currencyName = currencyName;
        this.lastUpdatedText = lastUpdatedText;
    }

    public Map<String, TradingResourceInfo> getResourceInfo() {
        return Collections.unmodifiableMap(resourceInfo);
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getLastUpdatedText() {
        return lastUpdatedText;
    }

    /**
     * Data record for a single tradable resource.
     */
    public record TradingResourceInfo(
            String resourceId,
            String displayName,
            // Stock information
            String stockDisplay, // e.g. "150 / 500"
            float currentStock, // Raw value for progress bars
            float maxStock, // Raw value for progress bars

            // Price information
            float pricePerUnit, // Raw price
            String priceDisplay, // e.g. "10.5 Emeralds"

            // Transaction flags (server-calculated based on stock/funds)
            boolean canBuy,
            boolean canSell,

            // Status
            String statusText, // e.g. "In Stock", "Full"
            String tooltipText // Detailed tooltip
    ) {
    }
}
