package com.quackers29.businesscraft.platform;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Platform-agnostic network helper interface using Yarn mappings.
 * Provides cross-platform networking operations for the Enhanced MultiLoader approach.
 */
public interface NetworkHelper {
    
    /**
     * Register a server-bound packet
     * @param packetId The packet identifier
     * @param decoder Function to decode packet from buffer
     * @param handler Function to handle the packet
     */
    <T> void registerServerBoundPacket(Identifier packetId, PacketDecoder<T> decoder, PacketHandler<T> handler);
    
    /**
     * Register a client-bound packet
     * @param packetId The packet identifier
     * @param decoder Function to decode packet from buffer
     * @param handler Function to handle the packet
     */
    <T> void registerClientBoundPacket(Identifier packetId, PacketDecoder<T> decoder, ClientPacketHandler<T> handler);
    
    /**
     * Send a packet to a specific player
     * @param player The target player
     * @param packetId The packet identifier
     * @param packet The packet data
     */
    <T> void sendToPlayer(ServerPlayerEntity player, Identifier packetId, T packet, PacketEncoder<T> encoder);
    
    /**
     * Send a packet to all players
     * @param packetId The packet identifier
     * @param packet The packet data
     */
    <T> void sendToAll(Identifier packetId, T packet, PacketEncoder<T> encoder);
    
    /**
     * Send a packet to the server
     * @param packetId The packet identifier
     * @param packet The packet data
     */
    <T> void sendToServer(Identifier packetId, T packet, PacketEncoder<T> encoder);
    
    /**
     * Interface for decoding packets from network buffers
     */
    @FunctionalInterface
    interface PacketDecoder<T> {
        T decode(PacketByteBuf buffer);
    }
    
    /**
     * Interface for encoding packets to network buffers
     */
    @FunctionalInterface
    interface PacketEncoder<T> {
        void encode(T packet, PacketByteBuf buffer);
    }
    
    /**
     * Interface for handling server-side packets
     */
    @FunctionalInterface
    interface PacketHandler<T> {
        void handle(T packet, ServerPlayerEntity player);
    }
    
    /**
     * Interface for handling client-side packets
     */
    @FunctionalInterface
    interface ClientPacketHandler<T> {
        void handle(T packet);
    }
}