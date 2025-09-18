package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.EventHelper;

/**
 * Fabric implementation of EventHelper
 */
public class FabricEventHelper implements EventHelper {
    @Override
    public void registerModEvent(Object listener) {
        // Fabric event registration will be handled through Fabric API events
        // This is a placeholder for now
    }

    @Override
    public void registerPlatformEvent(Object listener) {
        // Fabric event registration will be handled through Fabric API events
        // This is a placeholder for now
    }

    @Override
    public void addServerStoppingListener(java.util.function.Consumer<Void> listener) {
        // This will be handled in the main mod class
    }

    @Override
    public void addServerStartedListener(java.util.function.Consumer<Void> listener) {
        // This will be handled in the main mod class
    }

    @Override
    public void addLevelUnloadListener(java.util.function.Consumer<Void> listener) {
        // This will be handled in the main mod class
    }

    @Override
    public void setActiveTownBlock(Object pos) {
        // Implementation would go here
    }

    @Override
    public void clearActiveTownBlock() {
        // Implementation would go here
    }
}
