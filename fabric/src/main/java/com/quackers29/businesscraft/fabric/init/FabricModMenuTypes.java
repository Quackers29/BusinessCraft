package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.init.CommonModMenuTypes;
import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric menu type registration
 */
public class FabricModMenuTypes {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModMenuTypes.class);
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

    public static void register() {
        if (registrationAttempted && registrationSuccessful) {
            return; // Already registered successfully
        }

        registrationAttempted = true;
        LOGGER.info("Registering Fabric menu types...");

        try {
            CommonModMenuTypes.register();

            // Store in FabricMenuTypeHelper for PlatformAccess
            FabricMenuTypeHelper.setTownInterfaceMenuType(CommonModMenuTypes.TOWN_INTERFACE_MENU.get());
            FabricMenuTypeHelper.setTradeMenuType(CommonModMenuTypes.TRADE_MENU.get());
            FabricMenuTypeHelper.setStorageMenuType(CommonModMenuTypes.STORAGE_MENU.get());
            FabricMenuTypeHelper.setPaymentBoardMenuType(CommonModMenuTypes.PAYMENT_BOARD_MENU.get());

            registrationSuccessful = true;
            LOGGER.info("Fabric menu types registered successfully");
        } catch (Exception e) {
            LOGGER.error("Menu type registration failed: " + e.getMessage(), e);
        }
    }
}
