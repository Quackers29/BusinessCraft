package com.quackers29.businesscraft.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/**
 * Platform-agnostic interface for network operations.
 * Implementations will handle platform-specific networking.
 */
public interface NetworkHelper {
    /**
     * Register a network message/packet
     */
    <T> void registerMessage(int index, Class<T> messageType, FriendlyByteBuf.Writer<T> encoder,
                           FriendlyByteBuf.Reader<T> decoder);

    /**
     * Send a message to a specific player
     */
    void sendToPlayer(Object message, ServerPlayer player);

    /**
     * Send a message to all players
     */
    void sendToAllPlayers(Object message);

    /**
     * Send a message to the server
     */
    void sendToServer(Object message);

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
    ServerPlayer getSender(Object context);

    /**
     * Mark the packet as handled
     */
    void setPacketHandled(Object context);

    /**
     * Open a screen using platform-specific hooks
     */
    void openScreen(ServerPlayer player, MenuProvider menuProvider);
}
