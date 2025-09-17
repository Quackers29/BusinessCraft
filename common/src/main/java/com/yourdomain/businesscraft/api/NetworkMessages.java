package com.yourdomain.businesscraft.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Platform-agnostic interface for network message sending.
 * Implementations will handle platform-specific message sending.
 */
public interface NetworkMessages {
    /**
     * Send a message to a specific player
     */
    void sendToPlayer(Object message, ServerPlayer player);

    /**
     * Send a message to all players
     */
    void sendToAllPlayers(Object message);

    /**
     * Send a message to all players tracking a chunk
     */
    void sendToAllTrackingChunk(Object message, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos);

    /**
     * Send a message to the server
     */
    void sendToServer(Object message);
}
