package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.RegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import java.util.function.Supplier;

// Legacy Forge imports - kept for backwards compatibility during transition
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.items.ItemStackHandler;

public class ModMenuTypes {
    // Legacy Forge registration system - kept for backwards compatibility
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BusinessCraft.MOD_ID);

    // Platform abstraction helper
    private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();
    
    // Platform-agnostic menu registrations
    public static Supplier<MenuType<TownInterfaceMenu>> TOWN_INTERFACE_PLATFORM;
    public static Supplier<MenuType<TradeMenu>> TRADE_MENU_PLATFORM;
    public static Supplier<MenuType<StorageMenu>> STORAGE_MENU_PLATFORM;
    public static Supplier<MenuType<PaymentBoardMenu>> PAYMENT_BOARD_MENU_PLATFORM;

    // Legacy Forge registered menus - kept for backwards compatibility
    public static final RegistryObject<MenuType<TownInterfaceMenu>> TOWN_INTERFACE = registerMenu(
            "town_interface",
            (windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                return new TownInterfaceMenu(windowId, inv, pos);
            }
    );
    
    public static final RegistryObject<MenuType<TradeMenu>> TRADE_MENU = registerMenu(
            "trade_menu",
            (windowId, inv, data) -> new TradeMenu(windowId, inv, new ItemStackHandler(2))
    );
    
    public static final RegistryObject<MenuType<StorageMenu>> STORAGE_MENU = registerMenu(
            "storage_menu",
            (windowId, inv, data) -> new StorageMenu(windowId, inv, new ItemStackHandler(18))
    );
    
    public static final RegistryObject<MenuType<PaymentBoardMenu>> PAYMENT_BOARD_MENU = registerMenu(
            "payment_board_menu",
            (windowId, inv, data) -> new PaymentBoardMenu(windowId, inv, data)
    );

    /**
     * Initialize platform-agnostic menu registration.
     * This will eventually replace the legacy Forge system.
     */
    public static void initializePlatformRegistration() {
        // Register menus using platform abstraction
        TOWN_INTERFACE_PLATFORM = REGISTRY.registerMenu("town_interface_platform",
                () -> IForgeMenuType.create((windowId, inv, data) -> {
                    BlockPos pos = data.readBlockPos();
                    return new TownInterfaceMenu(windowId, inv, pos);
                })
        );
        
        TRADE_MENU_PLATFORM = REGISTRY.registerMenu("trade_menu_platform",
                () -> IForgeMenuType.create((windowId, inv, data) -> 
                    new TradeMenu(windowId, inv, new ItemStackHandler(2))
                )
        );
        
        STORAGE_MENU_PLATFORM = REGISTRY.registerMenu("storage_menu_platform",
                () -> IForgeMenuType.create((windowId, inv, data) -> 
                    new StorageMenu(windowId, inv, new ItemStackHandler(18))
                )
        );
        
        PAYMENT_BOARD_MENU_PLATFORM = REGISTRY.registerMenu("payment_board_menu_platform",
                () -> IForgeMenuType.create((windowId, inv, data) -> 
                    new PaymentBoardMenu(windowId, inv, data)
                )
        );
    }

    // Legacy helper method
    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenu(String name, IContainerFactory<T> factory) {
        return MENU_TYPES.register(name, () -> IForgeMenuType.create(factory));
    }
} 