package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.MenuTypeHelper;

/**
 * Fabric implementation of MenuTypeHelper
 * Menu types are stored as static fields and set during registration
 */
public class FabricMenuTypeHelper implements MenuTypeHelper {
    // Menu type instances - these will be set during mod initialization
    private static Object townInterfaceMenuType;
    private static Object tradeMenuType;
    private static Object storageMenuType;
    private static Object paymentBoardMenuType;

    @Override
    public Object getTownInterfaceMenuType() {
        return townInterfaceMenuType;
    }

    @Override
    public Object getTradeMenuType() {
        return tradeMenuType;
    }

    @Override
    public Object getStorageMenuType() {
        return storageMenuType;
    }

    @Override
    public Object getPaymentBoardMenuType() {
        return paymentBoardMenuType;
    }
    
    /**
     * Set the menu types - called during mod initialization
     */
    public static void setTownInterfaceMenuType(Object menuType) {
        townInterfaceMenuType = menuType;
    }

    public static void setTradeMenuType(Object menuType) {
        tradeMenuType = menuType;
    }

    public static void setStorageMenuType(Object menuType) {
        storageMenuType = menuType;
    }

    public static void setPaymentBoardMenuType(Object menuType) {
        paymentBoardMenuType = menuType;
    }
}
