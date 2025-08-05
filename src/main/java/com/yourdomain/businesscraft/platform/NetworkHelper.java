package com.yourdomain.businesscraft.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Platform abstraction interface for networking operations.
 * This interface provides a common API for sending packets across mod loaders.
 */
public interface NetworkHelper {
    
    /**
     * Registers a packet type for client-to-server communication.
     * @param messageClass The packet class
     * @param encoder The packet encoder
     * @param decoder The packet decoder  
     * @param handler The packet handler
     * @param <T> The packet type
     */
    <T> void registerClientToServerPacket(Class<T> messageClass, 
                                         PacketEncoder<T> encoder,
                                         PacketDecoder<T> decoder,
                                         PacketHandler<T> handler);
    
    /**
     * Registers a packet type for server-to-client communication.
     * @param messageClass The packet class
     * @param encoder The packet encoder
     * @param decoder The packet decoder
     * @param handler The packet handler
     * @param <T> The packet type
     */
    <T> void registerServerToClientPacket(Class<T> messageClass,
                                         PacketEncoder<T> encoder,
                                         PacketDecoder<T> decoder,
                                         PacketHandler<T> handler);
    
    /**
     * Sends a packet from client to server.
     * @param packet The packet to send
     */
    void sendToServer(Object packet);
    
    /**
     * Sends a packet from server to a specific client.
     * @param packet The packet to send
     * @param player The target player
     */
    void sendToClient(Object packet, ServerPlayer player);
    
    /**
     * Sends a packet from server to all clients.
     * @param packet The packet to send
     */
    void sendToAllClients(Object packet);
    
    /**
     * Functional interface for packet encoding.
     * @param <T> The packet type
     */
    @FunctionalInterface
    interface PacketEncoder<T> {
        void encode(T packet, FriendlyByteBuf buffer);
    }
    
    /**
     * Functional interface for packet decoding.
     * @param <T> The packet type
     */
    @FunctionalInterface
    interface PacketDecoder<T> {
        T decode(FriendlyByteBuf buffer); 
    }
    
    /**
     * Functional interface for packet handling.  
     * @param <T> The packet type
     */
    @FunctionalInterface
    interface PacketHandler<T> {
        void handle(T packet, Player player);
    }
}