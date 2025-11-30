package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.MenuTypeHelper;
import com.quackers29.businesscraft.forge.init.ForgeModMenuTypes;

/**
 * Forge implementation of MenuTypeHelper
 */
public class ForgeMenuTypeHelper implements MenuTypeHelper {
    @Override
    public Object getPaymentBoardMenuType() {
        return ForgeModMenuTypes.PAYMENT_BOARD_MENU.get();
    }

    @Override
    public Object getContractBoardMenuType() {
        return ForgeModMenuTypes.CONTRACT_BOARD_MENU.get();
    }
}
