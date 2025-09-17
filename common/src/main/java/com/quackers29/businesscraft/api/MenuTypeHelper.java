package com.quackers29.businesscraft.api;

import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
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
