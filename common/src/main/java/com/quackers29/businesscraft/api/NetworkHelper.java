package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for network operations.
 * Implementations will handle platform-specific networking.
 */
public interface NetworkHelper {
    /**
     * Register a network message/packet
     */
    <T> void registerMessage(int index, Class<T> messageType, Object encoder, Object decoder);

    /**
     * Send a message to a specific player
     */
    void sendToPlayer(Object message, Object player);

    /**
     * Send a message to all players
     */
    void sendToAllPlayers(Object message);

    /**
     * Send a message to the server
     */
    void sendToServer(Object message);

    /**
     * Send a message to all players tracking a chunk
     */
    void sendToAllTrackingChunk(Object message, Object level, Object pos);

    /**
     * Check if we're running on the client side
     */
    boolean isClientSide();

    /**
     * Get the current network context (platform-specific)
     */
    Object getCurrentContext();

    /**
     * Enqueue work to be executed on the appropriate thread
     */
    void enqueueWork(Object context, Runnable work);

    /**
     * Get the sender player from the context
     */
    Object getSender(Object context);

    /**
     * Mark the packet as handled
     */
    void setPacketHandled(Object context);

    /**
     * Open a screen using platform-specific hooks
     */
    void openScreen(Object player, Object menuProvider);
}
