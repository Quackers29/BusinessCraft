package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for network message sending.
 * Implementations will handle platform-specific message sending.
 */
public interface NetworkMessages {
    /**
     * Send a message to a specific player
     */
    void sendToPlayer(Object message, Object player);

    /**
     * Send a message to all players
     */
    void sendToAllPlayers(Object message);

    /**
     * Send a message to all players tracking a chunk
     */
    void sendToAllTrackingChunk(Object message, Object level, Object pos);

    /**
     * Send a message to the server
     */
    void sendToServer(Object message);

    /**
     * Send ResourceSyncPacket if supported on this platform
     * Fabric: sends packet for immediate resource UI updates
     * Forge: does nothing (uses existing sync mechanisms)
     */
    default void sendResourceSyncPacketIfSupported(Object pos, Object resources, Object player) {
        // Default implementation: do nothing (Forge behavior)
        // Fabric will override this to send the actual packet
    }
}
