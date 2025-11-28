package com.quackers29.businesscraft.forge.init;

import com.quackers29.businesscraft.forge.platform.ForgeRegistryHelper;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.Supplier;

/**
 * Forge-specific menu type registrations
 */
public class ForgeModMenuTypes {
        public static Supplier<MenuType<TownInterfaceMenu>> TOWN_INTERFACE_MENU;
        public static Supplier<MenuType<TradeMenu>> TRADE_MENU;
        public static Supplier<MenuType<StorageMenu>> STORAGE_MENU;
        public static Supplier<MenuType<PaymentBoardMenu>> PAYMENT_BOARD_MENU;

        public static void register() {
                ForgeRegistryHelper registry = (ForgeRegistryHelper) com.quackers29.businesscraft.forge.BusinessCraftForge.REGISTRY;

                // Create and register menu types
                TOWN_INTERFACE_MENU = registry.registerMenuType("town_interface", () -> IForgeMenuType.create(
                                (windowId, inv, data) -> {
                                        BlockPos pos = data.readBlockPos();
                                        return new TownInterfaceMenu(windowId, inv, pos);
                                }));

                TRADE_MENU = registry.registerMenuType("trade_menu", () -> IForgeMenuType.create(
                                (windowId, inv, data) -> new TradeMenu(windowId, inv, new ItemStackHandler(2))));

                STORAGE_MENU = registry.registerMenuType("storage_menu", () -> IForgeMenuType.create(
                                (windowId, inv, data) -> new StorageMenu(windowId, inv, new ItemStackHandler(18))));

                PAYMENT_BOARD_MENU = registry.registerMenuType("payment_board_menu", () -> IForgeMenuType.create(
                                (windowId, inv, data) -> new PaymentBoardMenu(windowId, inv, data)));
        }
}
