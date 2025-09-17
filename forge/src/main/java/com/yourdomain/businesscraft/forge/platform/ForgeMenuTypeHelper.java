package com.yourdomain.businesscraft.forge.platform;

import com.yourdomain.businesscraft.api.MenuTypeHelper;
import com.yourdomain.businesscraft.forge.init.ForgeModMenuTypes;
import net.minecraft.world.inventory.MenuType;

/**
 * Forge implementation of MenuTypeHelper
 */
public class ForgeMenuTypeHelper implements MenuTypeHelper {
    @Override
    public MenuType<?> getTownInterfaceMenuType() {
        return ForgeModMenuTypes.TOWN_INTERFACE_MENU;
    }

    @Override
    public MenuType<?> getTradeMenuType() {
        return ForgeModMenuTypes.TRADE_MENU;
    }

    @Override
    public MenuType<?> getStorageMenuType() {
        return ForgeModMenuTypes.STORAGE_MENU;
    }

    @Override
    public MenuType<?> getPaymentBoardMenuType() {
        return ForgeModMenuTypes.PAYMENT_BOARD_MENU;
    }
}
