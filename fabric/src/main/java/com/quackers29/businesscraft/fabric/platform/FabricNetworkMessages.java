package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.NetworkMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Fabric implementation of NetworkMessages
 */
public class FabricNetworkMessages implements NetworkMessages {
    @Override
    public void sendToPlayer(Object message, ServerPlayer player) {
        // TODO: Implement Fabric networking for sending to specific player
    }

    @Override
    public void sendToAllPlayers(Object message) {
        // TODO: Implement Fabric networking for sending to all players
    }

    @Override
    public void sendToAllTrackingChunk(Object message, Level level, BlockPos pos) {
        // TODO: Implement Fabric networking for sending to players tracking a chunk
    }

    @Override
    public void sendToServer(Object message) {
        // TODO: Implement Fabric networking for client to server communication
    }
}
