package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.NetworkMessages;

/**
 * Fabric implementation of NetworkMessages
 */
public class FabricNetworkMessages implements NetworkMessages {
    @Override
    public void sendToPlayer(Object message, Object player) {
        // TODO: Implement Fabric networking for sending to specific player
    }

    @Override
    public void sendToAllPlayers(Object message) {
        // TODO: Implement Fabric networking for sending to all players
    }

    @Override
    public void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        // TODO: Implement Fabric networking for sending to players tracking a chunk
    }

    @Override
    public void sendToServer(Object message) {
        // TODO: Implement Fabric networking for client to server communication
    }
}
