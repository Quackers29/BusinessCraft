package com.yourdomain.businesscraft.api;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.menu.PaymentBoardMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Platform-agnostic interface for accessing menu/screen types.
 * Implementations will provide access to registered menu types.
 */
public interface MenuTypeHelper {
    /**
     * Get the Town Interface menu type
     */
    MenuType<TownInterfaceMenu> getTownInterfaceMenuType();

    /**
     * Get the Trade menu type
     */
    MenuType<TradeMenu> getTradeMenuType();

    /**
     * Get the Storage menu type
     */
    MenuType<StorageMenu> getStorageMenuType();

    /**
     * Get the Payment Board menu type
     */
    MenuType<PaymentBoardMenu> getPaymentBoardMenuType();
}
