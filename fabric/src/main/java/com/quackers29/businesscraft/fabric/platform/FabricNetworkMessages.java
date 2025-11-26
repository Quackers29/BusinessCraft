package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.NetworkMessages;
import com.quackers29.businesscraft.fabric.FabricModMessages;

/**
 * Fabric implementation of NetworkMessages
 * Delegates to FabricModMessages for actual networking
 */
public class FabricNetworkMessages implements NetworkMessages {
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void sendToPlayer(Object message, Object player) {
        FabricModMessages.sendToPlayer(message, player);
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void sendToAllPlayers(Object message) {
        FabricModMessages.sendToAllPlayers(message);
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        FabricModMessages.sendToAllTrackingChunk(message, level, pos);
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void sendToServer(Object message) {
        FabricModMessages.sendToServer(message);
    }

    @Override
    public void sendResourceSyncPacketIfSupported(Object pos, Object resources, Object player) {
        try {
            if (pos instanceof net.minecraft.core.BlockPos blockPos &&
                resources instanceof java.util.Map map &&
                player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {

                // Create and send the ResourceSyncPacket
                var packetClass = Class.forName("com.quackers29.businesscraft.network.packets.ResourceSyncPacket");
                var constructor = packetClass.getConstructor(net.minecraft.core.BlockPos.class, java.util.Map.class);
                var packet = constructor.newInstance(blockPos, map);

                FabricModMessages.sendToPlayer(packet, serverPlayer);
            }
        } catch (Exception e) {
            // Log error but don't crash - this is an enhancement feature
            System.err.println("FabricNetworkMessages: Failed to send ResourceSyncPacket: " + e.getMessage());
        }
    }
}
