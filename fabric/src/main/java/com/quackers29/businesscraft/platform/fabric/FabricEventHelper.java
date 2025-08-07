package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.EventHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Fabric implementation of EventHelper using Yarn mappings.
 * Implements cross-platform event handling using Fabric Event API.
 */
public class FabricEventHelper implements EventHelper {
    
    @Override
    public void registerServerStartingEvent(ServerStartingHandler handler) {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> handler.onServerStarting());
    }
    
    @Override
    public void registerServerStoppingEvent(ServerStoppingHandler handler) {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> handler.onServerStopping());
    }
    
    @Override
    public void registerPlayerLoginEvent(PlayerLoginHandler handler) {
        ServerPlayConnectionEvents.JOIN.register((handler1, sender, server) -> handler.onPlayerLogin());
    }
}