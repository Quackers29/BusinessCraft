package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.MenuTypeHelper;
import com.quackers29.businesscraft.forge.init.ForgeModMenuTypes;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Forge implementation of MenuTypeHelper
 */
public class ForgeMenuTypeHelper implements MenuTypeHelper {
    @Override
    public MenuType<TownInterfaceMenu> getTownInterfaceMenuType() {
        return ForgeModMenuTypes.TOWN_INTERFACE_MENU;
    }

    @Override
    public MenuType<TradeMenu> getTradeMenuType() {
        return ForgeModMenuTypes.TRADE_MENU;
    }

    @Override
    public MenuType<StorageMenu> getStorageMenuType() {
        return ForgeModMenuTypes.STORAGE_MENU;
    }

    @Override
    public MenuType<PaymentBoardMenu> getPaymentBoardMenuType() {
        return ForgeModMenuTypes.PAYMENT_BOARD_MENU;
    }
}
