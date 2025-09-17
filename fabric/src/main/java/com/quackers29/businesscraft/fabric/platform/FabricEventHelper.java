package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.EventHelper;

/**
 * Fabric implementation of EventHelper
 */
public class FabricEventHelper implements EventHelper {
    @Override
    public void registerPlatformEvent(Object eventBus, Object target) {
        // Fabric event registration will be handled through Fabric API events
        // This is a placeholder for now
    }
}
