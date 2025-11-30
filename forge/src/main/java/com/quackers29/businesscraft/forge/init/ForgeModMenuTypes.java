package com.quackers29.businesscraft.forge.init;

import com.quackers29.businesscraft.init.CommonModMenuTypes;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.menu.ContractBoardMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Forge-specific menu type registrations
 */
public class ForgeModMenuTypes {
        public static Supplier<MenuType<TownInterfaceMenu>> TOWN_INTERFACE_MENU;
        public static Supplier<MenuType<TradeMenu>> TRADE_MENU;
        public static Supplier<MenuType<StorageMenu>> STORAGE_MENU;
        public static Supplier<MenuType<PaymentBoardMenu>> PAYMENT_BOARD_MENU;
        public static Supplier<MenuType<ContractBoardMenu>> CONTRACT_BOARD_MENU;

        @SuppressWarnings("unchecked")
        public static void register() {
                CommonModMenuTypes.register();

                // Cast suppliers back to RegistryObject for consistency if needed, though
                // Supplier is fine
                // We need to cast because CommonModMenuTypes stores them as
                // Supplier<MenuType<T>>
                // but ForgeRegistryHelper returns RegistryObject (which implements Supplier)
                TOWN_INTERFACE_MENU = (Supplier<MenuType<TownInterfaceMenu>>) CommonModMenuTypes.TOWN_INTERFACE_MENU;
                TRADE_MENU = (Supplier<MenuType<TradeMenu>>) CommonModMenuTypes.TRADE_MENU;
                STORAGE_MENU = (Supplier<MenuType<StorageMenu>>) CommonModMenuTypes.STORAGE_MENU;
                PAYMENT_BOARD_MENU = (Supplier<MenuType<PaymentBoardMenu>>) CommonModMenuTypes.PAYMENT_BOARD_MENU;
                CONTRACT_BOARD_MENU = (Supplier<MenuType<ContractBoardMenu>>) CommonModMenuTypes.CONTRACT_BOARD_MENU;
        }
}
