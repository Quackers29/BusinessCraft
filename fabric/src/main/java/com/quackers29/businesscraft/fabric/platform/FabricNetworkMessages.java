package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.NetworkMessages;
import com.quackers29.businesscraft.fabric.FabricModMessages;

/**
 * Fabric implementation of NetworkMessages
 * Delegates to FabricModMessages for actual networking
 */
public class FabricNetworkMessages implements NetworkMessages {
    @Override
    public void sendToPlayer(Object message, Object player) {
        FabricModMessages.sendToPlayer(message, player);
    }

    @Override
    public void sendToAllPlayers(Object message) {
        FabricModMessages.sendToAllPlayers(message);
    }

    @Override
    public void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        FabricModMessages.sendToAllTrackingChunk(message, level, pos);
    }

    @Override
    public void sendToServer(Object message) {
        FabricModMessages.sendToServer(message);
    }
}
