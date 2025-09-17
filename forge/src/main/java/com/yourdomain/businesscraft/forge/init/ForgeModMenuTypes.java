package com.yourdomain.businesscraft.forge.init;

import com.yourdomain.businesscraft.forge.platform.ForgeRegistryHelper;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.menu.PaymentBoardMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Forge-specific menu type registrations
 */
public class ForgeModMenuTypes {
    public static MenuType<TownInterfaceMenu> TOWN_INTERFACE_MENU;
    public static MenuType<TradeMenu> TRADE_MENU;
    public static MenuType<StorageMenu> STORAGE_MENU;
    public static MenuType<PaymentBoardMenu> PAYMENT_BOARD_MENU;

    public static void register() {
        ForgeRegistryHelper registry = (ForgeRegistryHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.REGISTRY;

        // Create and register menu types
        TOWN_INTERFACE_MENU = IForgeMenuType.create(
            (windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                return new TownInterfaceMenu(windowId, inv, pos);
            }
        );
        registry.registerMenuType("town_interface", TOWN_INTERFACE_MENU);

        TRADE_MENU = IForgeMenuType.create(
            (windowId, inv, data) -> new TradeMenu(windowId, inv, new ItemStackHandler(2))
        );
        registry.registerMenuType("trade_menu", TRADE_MENU);

        STORAGE_MENU = IForgeMenuType.create(
            (windowId, inv, data) -> new StorageMenu(windowId, inv, new ItemStackHandler(18))
        );
        registry.registerMenuType("storage_menu", STORAGE_MENU);

        PAYMENT_BOARD_MENU = IForgeMenuType.create(
            (windowId, inv, data) -> new PaymentBoardMenu(windowId, inv, data)
        );
        registry.registerMenuType("payment_board_menu", PAYMENT_BOARD_MENU);
    }
}
