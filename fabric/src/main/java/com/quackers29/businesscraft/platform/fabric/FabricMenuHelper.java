package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.MenuHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of MenuHelper using Yarn mappings.
 * Simplified to match common interface pattern for unified architecture.
 */
public class FabricMenuHelper implements MenuHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricMenuHelper.class);
    
    @Override
    public void refreshActiveMenu(Object player, String refreshType) {
        LOGGER.debug("FABRIC MENU HELPER: refreshActiveMenu not yet implemented");
        // TODO: Implement Fabric-specific menu refreshing
    }
}