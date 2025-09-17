package com.yourdomain.businesscraft.forge.init;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.menu.PaymentBoardMenu;
import com.yourdomain.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Forge-specific menu type registrations
 */
public class ForgeModMenuTypes {
    // Town Interface Menu - our primary menu type for town management
    public static final MenuType<TownInterfaceMenu> TOWN_INTERFACE_MENU = IForgeMenuType.create(
        (windowId, inv, data) -> {
            BlockPos pos = data.readBlockPos();
            return new TownInterfaceMenu(windowId, inv, pos);
        }
    );

    // Trade Menu
    public static final MenuType<TradeMenu> TRADE_MENU = IForgeMenuType.create(
        (windowId, inv, data) -> new TradeMenu(windowId, inv, new ItemStackHandler(2))
    );

    // Storage Menu
    public static final MenuType<StorageMenu> STORAGE_MENU = IForgeMenuType.create(
        (windowId, inv, data) -> new StorageMenu(windowId, inv, new ItemStackHandler(18))
    );

    // Payment Board Menu
    public static final MenuType<PaymentBoardMenu> PAYMENT_BOARD_MENU = IForgeMenuType.create(
        (windowId, inv, data) -> new PaymentBoardMenu(windowId, inv, data)
    );

    public static void register() {
        ForgeRegistryHelper registry = (ForgeRegistryHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.REGISTRY;

        registry.registerMenuType("town_interface", TOWN_INTERFACE_MENU);
        registry.registerMenuType("trade_menu", TRADE_MENU);
        registry.registerMenuType("storage_menu", STORAGE_MENU);
        registry.registerMenuType("payment_board_menu", PAYMENT_BOARD_MENU);
    }
}
