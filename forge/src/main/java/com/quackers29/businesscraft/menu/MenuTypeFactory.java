package com.quackers29.businesscraft.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import com.quackers29.businesscraft.BusinessCraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for accessing MenuTypes without direct field access.
 * This bypasses Enhanced MultiLoader Template classloader boundary issues
 * by using registry lookups instead of static field references.
 */
public class MenuTypeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuTypeFactory.class);
    
    /**
     * Get TownInterface MenuType using registry lookup to bypass EML field access issues.
     * @return MenuType or null if not found
     */
    @SuppressWarnings("unchecked")
    public static MenuType<TownInterfaceMenu> getTownInterfaceMenuType() {
        try {
            ResourceLocation id = new ResourceLocation(BusinessCraft.MOD_ID, "town_interface");
            MenuType<?> menuType = BuiltInRegistries.MENU.get(id);
            
            if (menuType != null) {
                return (MenuType<TownInterfaceMenu>) menuType;
            } else {
                LOGGER.warn("TownInterface MenuType not found in registry - registration may be incomplete");
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve TownInterface MenuType from registry: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get Trade MenuType using registry lookup.
     */
    @SuppressWarnings("unchecked")
    public static MenuType<TradeMenu> getTradeMenuType() {
        try {
            ResourceLocation id = new ResourceLocation(BusinessCraft.MOD_ID, "trade_menu");
            MenuType<?> menuType = BuiltInRegistries.MENU.get(id);
            return menuType != null ? (MenuType<TradeMenu>) menuType : null;
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve Trade MenuType: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get Storage MenuType using registry lookup.
     */
    @SuppressWarnings("unchecked") 
    public static MenuType<StorageMenu> getStorageMenuType() {
        try {
            ResourceLocation id = new ResourceLocation(BusinessCraft.MOD_ID, "storage_menu");
            MenuType<?> menuType = BuiltInRegistries.MENU.get(id);
            return menuType != null ? (MenuType<StorageMenu>) menuType : null;
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve Storage MenuType: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get PaymentBoard MenuType using registry lookup.
     */
    @SuppressWarnings("unchecked")
    public static MenuType<PaymentBoardMenu> getPaymentBoardMenuType() {
        try {
            ResourceLocation id = new ResourceLocation(BusinessCraft.MOD_ID, "payment_board_menu");
            MenuType<?> menuType = BuiltInRegistries.MENU.get(id);
            return menuType != null ? (MenuType<PaymentBoardMenu>) menuType : null;
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve PaymentBoard MenuType: {}", e.getMessage());
            return null;
        }
    }
}