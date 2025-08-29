package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.PaymentBoardMenu;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.RegistryHelper;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import java.util.function.Supplier;

// Forge-specific imports for inventory handling
import net.minecraftforge.items.ItemStackHandler;

// Functional interface imports for menu factories
import com.quackers29.businesscraft.platform.forge.ForgeMenuHelper.SimpleMenuFactory;
import com.quackers29.businesscraft.platform.forge.ForgeMenuHelper.MenuFactory;

/**
 * Platform-agnostic menu registration using the RegistryHelper abstraction.
 * This system works across different mod loaders (Forge, Fabric, etc.).
 */
public class ModMenuTypes {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMenuTypes.class);
    
    // Platform abstraction helper - initialize lazily to avoid static initialization issues
    private static RegistryHelper REGISTRY = null;
    
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
        try {
            // Initialize REGISTRY if null
            if (REGISTRY == null) {
                REGISTRY = PlatformServices.getRegistryHelper();
            }
            
            // Get menu helper from platform services
            var menuHelper = PlatformServices.getMenuHelper();
        
        // Register menus using platform abstraction
        TOWN_INTERFACE = REGISTRY.registerMenu("town_interface", 
            () -> {
                DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "TOWN_INTERFACE supplier called - creating MenuType");
                return IForgeMenuType.create((windowId, inv, data) -> {
                    BlockPos pos = data.readBlockPos();
                    return new TownInterfaceMenu(windowId, inv, pos);
                });
            }
        );
        
        // Create functional interfaces for menu factories
        var tradeFactory = (SimpleMenuFactory<TradeMenu>)
            (windowId, inv) -> new TradeMenu(windowId, inv, new ItemStackHandler(2));

        var storageFactory = (SimpleMenuFactory<StorageMenu>)
            (windowId, inv) -> new StorageMenu(windowId, inv, new ItemStackHandler(18));

        var paymentFactory = (MenuFactory<PaymentBoardMenu>)
            (windowId, inv, data) -> new PaymentBoardMenu(windowId, inv, data);

        // Use direct menu type creation for now (Phase 3A approach)
        Supplier<MenuType<TradeMenu>> tradeMenuSupplier = () -> new MenuType<>((windowId, inv) -> new TradeMenu(windowId, inv, new ItemStackHandler(2)), null);

        Supplier<MenuType<StorageMenu>> storageMenuSupplier = () -> new MenuType<>((windowId, inv) -> new StorageMenu(windowId, inv, new ItemStackHandler(18)), null);

        Supplier<MenuType<PaymentBoardMenu>> paymentMenuSupplier = () -> IForgeMenuType.create((windowId, inv, data) -> new PaymentBoardMenu(windowId, inv, data));

        TRADE_MENU = REGISTRY.registerMenu("trade_menu", tradeMenuSupplier);
        STORAGE_MENU = REGISTRY.registerMenu("storage_menu", storageMenuSupplier);
        PAYMENT_BOARD_MENU = REGISTRY.registerMenu("payment_board_menu", paymentMenuSupplier);
        
        // Verify registrations were successful 
        if (TOWN_INTERFACE == null || TRADE_MENU == null || STORAGE_MENU == null || PAYMENT_BOARD_MENU == null) {
            throw new IllegalStateException("Menu registration failed - one or more menus not registered");
        }
        
            // Menu registration completed successfully
        } catch (Exception e) {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "CRITICAL EXCEPTION in ModMenuTypes.initialize(): {}: {}", 
                e.getClass().getSimpleName(), e.getMessage());
            LOGGER.error("Failed to initialize ModMenuTypes", e);
            throw new RuntimeException("Failed to initialize ModMenuTypes", e);
        }
    }
    
} 