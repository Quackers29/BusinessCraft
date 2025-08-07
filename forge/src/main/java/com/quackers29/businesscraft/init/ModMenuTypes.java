package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.RegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import java.util.function.Supplier;

// Forge-specific imports for inventory handling
import net.minecraftforge.items.ItemStackHandler;

/**
 * Platform-agnostic menu registration using the RegistryHelper abstraction.
 * This system works across different mod loaders (Forge, Fabric, etc.).
 */
public class ModMenuTypes {
    // Platform abstraction helper
    private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();
    
    // Platform-agnostic menu registrations
    public static Supplier<MenuType<TownInterfaceMenu>> TOWN_INTERFACE;
    public static Supplier<MenuType<TradeMenu>> TRADE_MENU;
    public static Supplier<MenuType<StorageMenu>> STORAGE_MENU;
    public static Supplier<MenuType<PaymentBoardMenu>> PAYMENT_BOARD_MENU;

    /**
     * Initialize all menu registrations using platform abstraction.
     * This is the Forge-specific implementation with actual menu registration.
     */
    public static void initialize() {
        // First verify platform services (from common)
        com.quackers29.businesscraft.init.ModMenuTypes.initialize();
        
        // Get platform-agnostic menu helper
        var menuHelper = PlatformServices.getMenuHelper();
        
        // Register menus using platform abstraction
        TOWN_INTERFACE = REGISTRY.registerMenu("town_interface",
            menuHelper.createDataDrivenMenuType((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                return new TownInterfaceMenu(windowId, inv, pos);
            })
        );
        
        TRADE_MENU = REGISTRY.registerMenu("trade_menu",
            menuHelper.createSimpleMenuType((windowId, inv) -> 
                new TradeMenu(windowId, inv, new ItemStackHandler(2))
            )
        );
        
        STORAGE_MENU = REGISTRY.registerMenu("storage_menu",
            menuHelper.createSimpleMenuType((windowId, inv) -> 
                new StorageMenu(windowId, inv, new ItemStackHandler(18))
            )
        );
        
        PAYMENT_BOARD_MENU = REGISTRY.registerMenu("payment_board_menu",
            menuHelper.createDataDrivenMenuType((windowId, inv, data) -> 
                new PaymentBoardMenu(windowId, inv, data)
            )
        );
        
        // Verify registrations were successful 
        if (TOWN_INTERFACE == null || TRADE_MENU == null || STORAGE_MENU == null || PAYMENT_BOARD_MENU == null) {
            throw new IllegalStateException("Menu registration failed - one or more menus not registered");
        }
    }
} 