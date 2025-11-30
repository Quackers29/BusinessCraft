package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for accessing menu/screen types.
 * Implementations will provide access to registered menu types.
 */
public interface MenuTypeHelper {
    /**
     * Get the Payment Board menu type
     */
    Object getPaymentBoardMenuType();

    /**
     * Get the Contract Board menu type
     */
    Object getContractBoardMenuType();

    /**
     * Get the Town Interface menu type
     */
    Object getTownInterfaceMenuType();

    /**
     * Get the Trade menu type
     */
    Object getTradeMenuType();

    /**
     * Get the Storage menu type
     */
    Object getStorageMenuType();
}
