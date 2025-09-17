package com.yourdomain.businesscraft.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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
}
