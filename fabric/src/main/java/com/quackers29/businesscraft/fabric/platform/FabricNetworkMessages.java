package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.NetworkMessages;
import com.quackers29.businesscraft.fabric.FabricModMessages;

/**
 * Fabric implementation of NetworkMessages
 * Delegates to FabricModMessages for actual networking
 */
public class FabricNetworkMessages implements NetworkMessages {
    // @Override - temporarily removed due to classpath issue with common module
    // interfaces
    public void sendToPlayer(Object message, Object player) {
        FabricModMessages.sendToPlayer(message, player);
    }

    // @Override - temporarily removed due to classpath issue with common module
    // interfaces
    public void sendToAllPlayers(Object message) {
        FabricModMessages.sendToAllPlayers(message);
    }

    // @Override - temporarily removed due to classpath issue with common module
    // interfaces
    public void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        FabricModMessages.sendToAllTrackingChunk(message, level, pos);
    }

    // @Override - temporarily removed due to classpath issue with common module
    // interfaces
    public void sendToServer(Object message) {
        FabricModMessages.sendToServer(message);
    }
}
