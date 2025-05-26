package com.yourdomain.businesscraft.init;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.menu.TownBlockMenu;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.items.ItemStackHandler;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BusinessCraft.MOD_ID);

    // Town Block Menu
    public static final RegistryObject<MenuType<TownBlockMenu>> TOWN_BLOCK = registerMenu(
            "town_block",
            (windowId, inv, data) -> new TownBlockMenu(windowId, inv, data)
    );
    
    // Town Interface Menu - our new menu type for the UI showcase
    public static final RegistryObject<MenuType<TownInterfaceMenu>> TOWN_INTERFACE = registerMenu(
            "town_interface",
            (windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                return new TownInterfaceMenu(windowId, inv, pos);
            }
    );
    
    // Trade Menu
    public static final RegistryObject<MenuType<TradeMenu>> TRADE_MENU = registerMenu(
            "trade_menu",
            (windowId, inv, data) -> new TradeMenu(windowId, inv, new ItemStackHandler(2))
    );
    
    // Storage Menu
    public static final RegistryObject<MenuType<StorageMenu>> STORAGE_MENU = registerMenu(
            "storage_menu",
            (windowId, inv, data) -> new StorageMenu(windowId, inv, new ItemStackHandler(18))
    );

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenu(String name, IContainerFactory<T> factory) {
        return MENU_TYPES.register(name, () -> IForgeMenuType.create(factory));
    }
} 