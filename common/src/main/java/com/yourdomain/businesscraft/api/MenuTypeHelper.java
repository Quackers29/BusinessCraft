package com.yourdomain.businesscraft.api;

import net.minecraft.world.inventory.MenuType;

/**
 * Platform-agnostic interface for accessing menu/screen types.
 * Implementations will provide access to registered menu types.
 */
public interface MenuTypeHelper {
    /**
     * Get the Town Interface menu type
     */
    MenuType<?> getTownInterfaceMenuType();

    /**
     * Get the Trade menu type
     */
    MenuType<?> getTradeMenuType();

    /**
     * Get the Storage menu type
     */
    MenuType<?> getStorageMenuType();

    /**
     * Get the Payment Board menu type
     */
    MenuType<?> getPaymentBoardMenuType();
}
