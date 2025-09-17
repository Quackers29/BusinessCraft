package com.quackers29.businesscraft.fabric.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Fabric event registration
 */
public class FabricModEvents {
    public static void register() {
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            // TODO: Handle server starting event
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // TODO: Handle server stopping event
        });
    }
}
