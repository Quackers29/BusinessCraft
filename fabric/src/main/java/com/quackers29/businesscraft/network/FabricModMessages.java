package com.quackers29.businesscraft.network;

import com.quackers29.businesscraft.network.packets.platform.AddPlatformPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric networking implementation for BusinessCraft.
 * Handles packet registration and basic packet operations.
 */
public class FabricModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModMessages.class);
    private static final String MOD_ID = "businesscraft";
    
    // Packet identifiers
    public static final Identifier ADD_PLATFORM = new Identifier(MOD_ID, "add_platform");
    public static final Identifier DELETE_PLATFORM = new Identifier(MOD_ID, "delete_platform");
    public static final Identifier REFRESH_PLATFORMS = new Identifier(MOD_ID, "refresh_platforms");
    
    public static void initializeNetworking() {
        LOGGER.info("Initializing Fabric networking for BusinessCraft");
        
        // Register server-bound packets
        registerServerBoundPackets();
        
        // Register client-bound packets  
        registerClientBoundPackets();
        
        LOGGER.info("Fabric networking initialization complete");
    }
    
    private static void registerServerBoundPackets() {
        // Add Platform packet
        ServerPlayNetworking.registerGlobalReceiver(ADD_PLATFORM, (server, player, handler, buf, responseSender) -> {
            // Decode packet data
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            
            // Handle on server thread
            server.execute(() -> {
                try {
                    AddPlatformPacket packet = new AddPlatformPacket(x, y, z);
                    packet.handle(player);
                } catch (Exception e) {
                    LOGGER.error("Failed to handle AddPlatformPacket: {}", e.getMessage(), e);
                }
            });
        });
        
        LOGGER.debug("Registered server-bound packets");
    }
    
    private static void registerClientBoundPackets() {
        // Client-bound packets will be registered here
        LOGGER.debug("Registered client-bound packets");
    }
    
    /**
     * Send a packet to the server from client side.
     */
    public static <T> void sendToServer(T packet) {
        try {
            if (packet instanceof AddPlatformPacket addPacket) {
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                buf.writeInt(addPacket.getX());
                buf.writeInt(addPacket.getY());
                buf.writeInt(addPacket.getZ());
                ClientPlayNetworking.send(ADD_PLATFORM, buf);
                LOGGER.debug("Sent AddPlatformPacket to server: ({}, {}, {})", addPacket.getX(), addPacket.getY(), addPacket.getZ());
            } else {
                LOGGER.warn("Unsupported packet type for sendToServer: {}", packet.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send packet to server: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send a packet to a specific player.
     */
    public static <T> void sendToPlayer(T packet, ServerPlayerEntity player) {
        try {
            // Implementation for server-to-client packets
            LOGGER.debug("sendToPlayer not yet implemented for Fabric: {}", packet.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.error("Failed to send packet to player: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send a packet to all players.
     */
    public static <T> void sendToAllClients(T packet) {
        try {
            // Implementation for server-to-all-clients packets
            LOGGER.debug("sendToAllClients not yet implemented for Fabric: {}", packet.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.error("Failed to send packet to all clients: {}", e.getMessage(), e);
        }
    }
}