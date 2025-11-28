package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public class CommonModMenuTypes {
    public static Supplier<MenuType<TownInterfaceMenu>> TOWN_INTERFACE_MENU;
    public static Supplier<MenuType<TradeMenu>> TRADE_MENU;
    public static Supplier<MenuType<StorageMenu>> STORAGE_MENU;
    public static Supplier<MenuType<PaymentBoardMenu>> PAYMENT_BOARD_MENU;

    public static void register() {
        TOWN_INTERFACE_MENU = PlatformAccess.getRegistry().registerExtendedMenuType("town_interface",
                TownInterfaceMenu::new);

        TRADE_MENU = PlatformAccess.getRegistry().registerExtendedMenuType("trade_menu",
                TradeMenu::new);

        STORAGE_MENU = PlatformAccess.getRegistry().registerExtendedMenuType("storage_menu",
                StorageMenu::new);

        PAYMENT_BOARD_MENU = PlatformAccess.getRegistry().registerExtendedMenuType("payment_board_menu",
                PaymentBoardMenu::new);
    }
}
