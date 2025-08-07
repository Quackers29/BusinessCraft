package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic network helper interface for Enhanced MultiLoader approach.
 * This interface provides a common API for packet operations across Forge and Fabric.
 * 
 * Platform modules implement this interface using their specific networking APIs:
 * - Forge: SimpleChannel with FriendlyByteBuf
 * - Fabric: Networking API with PacketByteBuf
 */
public interface NetworkHelper {
    
    /**
     * Register a client-to-server packet type.
     * Platform implementations handle the actual registration using their specific APIs.
     * 
     * @param packetId Unique identifier for this packet type
     * @param packetClass The packet class
     * @param encoder Function to serialize packet data
     * @param decoder Function to deserialize packet data  
     * @param handler Function to handle the packet on server side
     * @param <T> The packet type
     */
    <T> void registerClientToServerPacket(String packetId, 
                                         Class<T> packetClass,
                                         PacketEncoder<T> encoder,
                                         PacketDecoder<T> decoder,
                                         PacketHandler<T> handler);
    
    /**
     * Register a server-to-client packet type.
     * Platform implementations handle the actual registration using their specific APIs.
     * 
     * @param packetId Unique identifier for this packet type
     * @param packetClass The packet class
     * @param encoder Function to serialize packet data
     * @param decoder Function to deserialize packet data
     * @param handler Function to handle the packet on client side
     * @param <T> The packet type
     */
    <T> void registerServerToClientPacket(String packetId,
                                         Class<T> packetClass,
                                         PacketEncoder<T> encoder,
                                         PacketDecoder<T> decoder,
                                         ClientPacketHandler<T> handler);
    
    /**
     * Send a packet from client to server.
     * Platform implementations handle the actual sending using their specific APIs.
     * 
     * @param packet The packet to send
     */
    <T> void sendToServer(T packet);
    
    /**
     * Send a packet from server to a specific client.
     * Platform implementations handle the actual sending using their specific APIs.
     * 
     * @param packet The packet to send
     * @param player The target player (platform modules convert to their player type)
     */
    <T> void sendToClient(T packet, Object player);
    
    /**
     * Send a packet from server to all clients.
     * Platform implementations handle the actual sending using their specific APIs.
     * 
     * @param packet The packet to send
     */
    <T> void sendToAllClients(T packet);
    
    /**
     * Platform-agnostic packet encoder interface.
     * Platform implementations provide a buffer wrapper that abstracts the actual buffer type.
     * 
     * @param <T> The packet type
     */
    @FunctionalInterface
    interface PacketEncoder<T> {
        /**
         * Encode packet data to a platform-specific buffer.
         * The buffer parameter is a wrapper that abstracts FriendlyByteBuf/PacketByteBuf.
         */
        void encode(T packet, Object buffer);
    }
    
    /**
     * Platform-agnostic packet decoder interface.
     * Platform implementations provide a buffer wrapper that abstracts the actual buffer type.
     * 
     * @param <T> The packet type
     */
    @FunctionalInterface
    interface PacketDecoder<T> {
        /**
         * Decode packet data from a platform-specific buffer.
         * The buffer parameter is a wrapper that abstracts FriendlyByteBuf/PacketByteBuf.
         */
        T decode(Object buffer);
    }
    
    /**
     * Platform-agnostic server-side packet handler interface.
     * Platform implementations convert their player type to a common interface.
     * 
     * @param <T> The packet type
     */
    @FunctionalInterface
    interface PacketHandler<T> {
        /**
         * Handle a packet on the server side.
         * The player parameter is abstracted through platform services.
         */
        void handle(T packet, Object player);
    }
    
    /**
     * Platform-agnostic client-side packet handler interface.
     * 
     * @param <T> The packet type
     */
    @FunctionalInterface
    interface ClientPacketHandler<T> {
        /**
         * Handle a packet on the client side.
         */
        void handle(T packet);
    }
    
    /**
     * Get a platform-specific buffer wrapper for encoding/decoding operations.
     * This allows common module code to work with buffers without platform dependencies.
     * 
     * @return A buffer wrapper that abstracts platform-specific buffer types
     */
    Object createBuffer();
    
    /**
     * Write a BlockPos to the buffer using platform-specific methods.
     * This abstracts the differences between Forge and Fabric BlockPos serialization.
     */
    void writeBlockPos(Object buffer, int x, int y, int z);
    
    /**
     * Read a BlockPos from the buffer using platform-specific methods.
     * Returns an array [x, y, z] to avoid platform-specific BlockPos dependencies.
     */
    int[] readBlockPos(Object buffer);
    
    /**
     * Write a string to the buffer using platform-specific methods.
     */
    void writeString(Object buffer, String value);
    
    /**
     * Read a string from the buffer using platform-specific methods.
     */
    String readString(Object buffer);
    
    /**
     * Write a boolean to the buffer using platform-specific methods.
     */
    void writeBoolean(Object buffer, boolean value);
    
    /**
     * Read a boolean from the buffer using platform-specific methods.
     */
    boolean readBoolean(Object buffer);
    
    /**
     * Write an integer to the buffer using platform-specific methods.
     */
    void writeInt(Object buffer, int value);
    
    /**
     * Read an integer from the buffer using platform-specific methods.
     */
    int readInt(Object buffer);
    
    /**
     * Write a UUID to the buffer using platform-specific methods.
     */
    void writeUUID(Object buffer, String uuid);
    
    /**
     * Read a UUID from the buffer using platform-specific methods.
     */
    String readUUID(Object buffer);
    
    /**
     * Write an ItemStack to the buffer using platform-specific methods.
     * Platform implementations handle ItemStack serialization for their format.
     */
    void writeItemStack(Object buffer, Object itemStack);
    
    /**
     * Read an ItemStack from the buffer using platform-specific methods.
     * Platform implementations handle ItemStack deserialization for their format.
     */
    Object readItemStack(Object buffer);
    
    /**
     * Send a refresh destinations packet to a player.
     * Platform implementations handle packet construction and sending for platform management.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate  
     * @param z Block Z coordinate
     * @param platformId Platform UUID string
     * @param townDestinations Map of town UUID strings to enabled states
     * @param townNames Map of town UUID strings to town names
     * @param distances Map of town UUID strings to distances
     * @param directions Map of town UUID strings to direction strings
     */
    void sendRefreshDestinationsPacket(Object player, int x, int y, int z, String platformId,
                                     java.util.Map<String, Boolean> townDestinations,
                                     java.util.Map<String, String> townNames,
                                     java.util.Map<String, Integer> distances,
                                     java.util.Map<String, String> directions);
    
    /**
     * Send a refresh platforms packet to all players tracking a chunk.
     * Platform implementations handle packet construction and sending to tracking players.
     * 
     * @param player Platform-specific player object (for level access)
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     */
    void sendRefreshPlatformsPacketToChunk(Object player, int x, int y, int z);
    
    /**
     * Send a payment result packet to a player.
     * Platform implementations handle packet construction and sending for trade results.
     * 
     * @param player Platform-specific player object
     * @param paymentItemStack Platform-specific ItemStack payment result
     */
    void sendPaymentResultPacket(Object player, Object paymentItemStack);
    
    /**
     * Send a payment board response packet to a player.
     * Platform implementations handle packet construction and sending for payment board data.
     * 
     * @param player Platform-specific player object
     * @param unclaimedRewards List of unclaimed reward objects
     */
    void sendPaymentBoardResponsePacket(Object player, java.util.List<Object> unclaimedRewards);
    
    /**
     * Send a buffer slot storage response packet to a player.
     * Platform implementations handle packet construction and sending for buffer storage data.
     * 
     * @param player Platform-specific player object
     * @param bufferSlots Buffer storage slot data
     */
    void sendBufferSlotStorageResponsePacket(Object player, Object bufferSlots);
}