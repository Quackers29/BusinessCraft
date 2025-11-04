package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.network.packets.town.ToggleTouristSpawningPacket;
import com.quackers29.businesscraft.network.packets.town.SetTownNamePacket;
import com.quackers29.businesscraft.network.packets.platform.SetSearchRadiusPacket;
import com.quackers29.businesscraft.network.packets.platform.AddPlatformPacket;
import com.quackers29.businesscraft.network.packets.platform.DeletePlatformPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformEnabledPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathPacket;
import com.quackers29.businesscraft.network.packets.platform.ResetPlatformPathPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import com.quackers29.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformDestinationPacket;
import com.quackers29.businesscraft.network.packets.ui.SetPathCreationModePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenDestinationsUIPacket;
import com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket;
import com.quackers29.businesscraft.network.packets.ui.PlayerExitUIPacket;
import com.quackers29.businesscraft.network.packets.ui.PlatformVisualizationPacket;
import com.quackers29.businesscraft.network.packets.ui.BoundarySyncRequestPacket;
import com.quackers29.businesscraft.network.packets.ui.BoundarySyncResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenTownInterfacePacket;
import com.quackers29.businesscraft.network.packets.ui.OpenPaymentBoardPacket;
import com.quackers29.businesscraft.network.packets.ui.RequestTownMapDataPacket;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.TradeResourcePacket;
import com.quackers29.businesscraft.network.packets.storage.CommunalStoragePacket;
import com.quackers29.businesscraft.network.packets.storage.CommunalStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardRequestPacket;
import com.quackers29.businesscraft.network.packets.storage.PaymentBoardClaimPacket;
import com.quackers29.businesscraft.network.packets.storage.BufferStoragePacket;
import com.quackers29.businesscraft.network.packets.storage.BufferStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStoragePacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageRequestPacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.network.packets.debug.RequestTownDataPacket;
import com.quackers29.businesscraft.network.packets.debug.TownDataResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric networking setup for BusinessCraft mod
 * Registers all packets using Fabric's networking API
 */
public class FabricModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModMessages.class);
    private static final String MOD_ID = "businesscraft";

    /**
     * Register all network messages using Fabric's networking API
     */
    public static void register() {
        LOGGER.info("Registering Fabric network messages...");

        try {
            // Use reflection to access Fabric's networking APIs
            // Classes are loaded lazily when actually needed
            ClassLoader classLoader = FabricModMessages.class.getClassLoader();
            
            // Load Fabric networking classes with fallback
            Class<?> serverPlayNetworkingClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking");
            Class<?> clientPlayNetworkingClass = loadClassWithFallback(classLoader, "net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking");
            Class<?> identifierClass = loadClassWithFallback(classLoader, "net.minecraft.util.Identifier");
            Class<?> packetByteBufClass = classLoader.loadClass("net.minecraft.network.PacketByteBuf");
            Class<?> friendlyByteBufClass = classLoader.loadClass("net.minecraft.network.FriendlyByteBuf");
            
            // Create Identifier for the mod channel
            Object identifier = identifierClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, "main");
            
            // Register server-to-server packets (PLAY_TO_SERVER)
            registerServerPacket(serverPlayNetworkingClass, identifier, "toggle_tourist_spawning", ToggleTouristSpawningPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "set_town_name", SetTownNamePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "set_search_radius", SetSearchRadiusPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "set_path_creation_mode", SetPathCreationModePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "add_platform", AddPlatformPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "delete_platform", DeletePlatformPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "set_platform_enabled", SetPlatformEnabledPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "set_platform_path", SetPlatformPathPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "reset_platform_path", ResetPlatformPathPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "set_platform_path_creation_mode", SetPlatformPathCreationModePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "set_platform_destination", SetPlatformDestinationPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "open_destinations_ui", OpenDestinationsUIPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "player_exit_ui", PlayerExitUIPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "boundary_sync_request", BoundarySyncRequestPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "open_town_interface", OpenTownInterfacePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "open_payment_board", OpenPaymentBoardPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "request_town_map_data", RequestTownMapDataPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "request_town_platform_data", RequestTownPlatformDataPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "trade_resource", TradeResourcePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "communal_storage", CommunalStoragePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "payment_board_request", PaymentBoardRequestPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "payment_board_claim", PaymentBoardClaimPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "buffer_storage", BufferStoragePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "personal_storage", PersonalStoragePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "personal_storage_request", PersonalStorageRequestPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerServerPacket(serverPlayNetworkingClass, identifier, "request_town_data", RequestTownDataPacket.class, packetByteBufClass, friendlyByteBufClass);
            
            // Register client-to-client packets (PLAY_TO_CLIENT) - these need to be registered on both sides
            // Server side registration for sending
            registerClientPacketForServer(identifier, "refresh_platforms", RefreshPlatformsPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "refresh_destinations", RefreshDestinationsPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "platform_visualization", PlatformVisualizationPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "boundary_sync_response", BoundarySyncResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "town_map_data_response", TownMapDataResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "town_platform_data_response", TownPlatformDataResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "communal_storage_response", CommunalStorageResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "payment_board_response", PaymentBoardResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "buffer_storage_response", BufferStorageResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "buffer_slot_storage_response", BufferSlotStorageResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "personal_storage_response", PersonalStorageResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "payment_result", PaymentResultPacket.class, packetByteBufClass, friendlyByteBufClass);
            registerClientPacketForServer(identifier, "town_data_response", TownDataResponsePacket.class, packetByteBufClass, friendlyByteBufClass);
            
            // Client side registration for receiving - will be done in FabricClientSetup
            // Store identifiers for client-side registration
            CLIENT_PACKET_IDS = new String[] {
                "refresh_platforms", "refresh_destinations", "platform_visualization",
                "boundary_sync_response", "town_map_data_response", "town_platform_data_response",
                "communal_storage_response", "payment_board_response", "buffer_storage_response",
                "buffer_slot_storage_response", "personal_storage_response", "payment_result",
                "town_data_response"
            };
            CLIENT_PACKET_CLASSES = new Class[] {
                RefreshPlatformsPacket.class, RefreshDestinationsPacket.class, PlatformVisualizationPacket.class,
                BoundarySyncResponsePacket.class, TownMapDataResponsePacket.class, TownPlatformDataResponsePacket.class,
                CommunalStorageResponsePacket.class, PaymentBoardResponsePacket.class, BufferStorageResponsePacket.class,
                BufferSlotStorageResponsePacket.class, PersonalStorageResponsePacket.class, PaymentResultPacket.class,
                TownDataResponsePacket.class
            };
            
            LOGGER.info("Fabric network messages registered successfully ({} server packets, {} client packets)", 
                24, CLIENT_PACKET_IDS.length);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Fabric API classes not available during initialization. Network message registration will be skipped.");
            LOGGER.warn("Missing class: " + e.getMessage());
            LOGGER.warn("Network messages will be registered when classes become available.");
            // Don't fail the mod initialization - network can be registered later if needed
        } catch (Exception e) {
            LOGGER.error("Error registering network messages", e);
            e.printStackTrace();
        }
    }
    
    // Store client packet info for client-side registration
    public static String[] CLIENT_PACKET_IDS;
    public static Class<?>[] CLIENT_PACKET_CLASSES;
    
    /**
     * Register a server-side packet handler (PLAY_TO_SERVER)
     */
    private static void registerServerPacket(Class<?> serverPlayNetworkingClass, Object identifier, 
            String packetName, Class<?> packetClass, Class<?> packetByteBufClass, Class<?> friendlyByteBufClass) {
        try {
            // Create packet-specific identifier
            Object packetId = identifier.getClass().getConstructor(String.class, String.class)
                .newInstance(MOD_ID, packetName);
            
            // Get the ServerPlayNetworking.registerReceiver method
            java.lang.reflect.Method registerMethod = serverPlayNetworkingClass.getMethod(
                "registerReceiver",
                identifier.getClass(),
                java.util.function.BiConsumer.class
            );
            
            // Create handler that converts PacketByteBuf to FriendlyByteBuf and calls packet handle
            java.util.function.BiConsumer<Object, Object> handler = (player, buf) -> {
                try {
                    // Convert PacketByteBuf to FriendlyByteBuf (they're compatible)
                    Object friendlyBuf = friendlyByteBufClass.cast(buf);
                    
                    // Try static decode method first, then constructor with FriendlyByteBuf
                    Object packet;
                    try {
                        java.lang.reflect.Method decodeMethod = packetClass.getMethod("decode", friendlyByteBufClass);
                        packet = decodeMethod.invoke(null, friendlyBuf);
                    } catch (NoSuchMethodException e) {
                        // Fall back to constructor with FriendlyByteBuf
                        java.lang.reflect.Constructor<?> constructor = packetClass.getConstructor(friendlyByteBufClass);
                        packet = constructor.newInstance(friendlyBuf);
                    }
                    
                    // Create a context object for the packet (Fabric doesn't have NetworkEvent.Context)
                    // Pass the player as the context - packets expect Object context
                    java.lang.reflect.Method handleMethod = packetClass.getMethod("handle", Object.class);
                    handleMethod.invoke(packet, player);
                } catch (Exception e) {
                    LOGGER.error("Error handling server packet {}", packetName, e);
                    e.printStackTrace();
                }
            };
            
            registerMethod.invoke(null, packetId, handler);
            LOGGER.debug("Registered server packet: {}", packetName);
        } catch (Exception e) {
            LOGGER.error("Error registering server packet {}", packetName, e);
            e.printStackTrace();
        }
    }
    
    /**
     * Register a client-side packet for server-side sending (PLAY_TO_CLIENT)
     * This just stores the information - actual client registration happens in FabricClientSetup
     */
    private static void registerClientPacketForServer(Object identifier, String packetName, 
            Class<?> packetClass, Class<?> packetByteBufClass, Class<?> friendlyByteBufClass) {
        // No-op for now - client registration happens in FabricClientSetup
        LOGGER.debug("Prepared client packet for registration: {}", packetName);
    }

    /**
     * Send a message to a specific player
     */
    public static void sendToPlayer(Object message, Object player) {
        try {
            ClassLoader classLoader = FabricModMessages.class.getClassLoader();
            Class<?> serverPlayNetworkingClass = classLoader.loadClass("net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking");
            Class<?> identifierClass = classLoader.loadClass("net.minecraft.util.Identifier");
            Class<?> packetByteBufClass = classLoader.loadClass("net.minecraft.network.PacketByteBuf");
            Class<?> friendlyByteBufClass = classLoader.loadClass("net.minecraft.network.FriendlyByteBuf");
            
            // Get packet class name and create identifier
            String packetName = getPacketName(message.getClass());
            Object packetId = identifierClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, packetName);
            
            // Create buffer
            Object friendlyBuf = friendlyByteBufClass.getConstructor().newInstance();
            
            // Try static encode method first, then instance toBytes method
            try {
                java.lang.reflect.Method encodeMethod = message.getClass().getMethod("encode", message.getClass(), friendlyByteBufClass);
                encodeMethod.invoke(null, message, friendlyBuf);
            } catch (NoSuchMethodException e) {
                // Fall back to instance toBytes method
                java.lang.reflect.Method toBytesMethod = message.getClass().getMethod("toBytes", friendlyByteBufClass);
                toBytesMethod.invoke(message, friendlyBuf);
            }
            
            // Wrap in PacketByteBuf
            Object packetBuf = packetByteBufClass.getConstructor(friendlyByteBufClass).newInstance(friendlyBuf);
            
            // Send to player
            java.lang.reflect.Method sendMethod = serverPlayNetworkingClass.getMethod("send", Object.class, Object.class);
            sendMethod.invoke(null, player, packetBuf);
        } catch (Exception e) {
            LOGGER.error("Error in sendToPlayer", e);
            e.printStackTrace();
        }
    }

    /**
     * Send a message to all players on the server
     */
    public static void sendToAllPlayers(Object message) {
        try {
            ClassLoader classLoader = FabricModMessages.class.getClassLoader();
            Class<?> playerLookupClass = classLoader.loadClass("net.fabricmc.fabric.api.networking.v1.PlayerLookup");
            Class<?> serverPlayNetworkingClass = classLoader.loadClass("net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking");
            Class<?> minecraftServerClass = classLoader.loadClass("net.minecraft.server.MinecraftServer");
            
            // Get server instance
            Object server = minecraftServerClass.getMethod("getServer").invoke(null);
            if (server == null) return;
            
            // Get all players
            java.lang.reflect.Method allMethod = playerLookupClass.getMethod("all", minecraftServerClass);
            java.lang.Iterable<?> players = (java.lang.Iterable<?>) allMethod.invoke(null, server);
            
            // Send to each player
            for (Object player : players) {
                sendToPlayer(message, player);
            }
        } catch (Exception e) {
            LOGGER.error("Error in sendToAllPlayers", e);
        }
    }

    /**
     * Send a message to all players tracking a specific chunk
     */
    public static void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        try {
            ClassLoader classLoader = FabricModMessages.class.getClassLoader();
            Class<?> playerLookupClass = classLoader.loadClass("net.fabricmc.fabric.api.networking.v1.PlayerLookup");
            Class<?> serverPlayNetworkingClass = classLoader.loadClass("net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking");
            
            // Get tracking players
            java.lang.reflect.Method trackingMethod = playerLookupClass.getMethod("tracking", level.getClass(), pos.getClass());
            java.lang.Iterable<?> players = (java.lang.Iterable<?>) trackingMethod.invoke(null, level, pos);
            
            // Send to each player
            for (Object player : players) {
                sendToPlayer(message, player);
            }
        } catch (Exception e) {
            LOGGER.error("Error in sendToAllTrackingChunk", e);
        }
    }

    /**
     * Send a message from client to server
     */
    public static void sendToServer(Object message) {
        try {
            ClassLoader classLoader = FabricModMessages.class.getClassLoader();
            Class<?> clientPlayNetworkingClass = classLoader.loadClass("net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking");
            Class<?> identifierClass = classLoader.loadClass("net.minecraft.util.Identifier");
            Class<?> packetByteBufClass = classLoader.loadClass("net.minecraft.network.PacketByteBuf");
            Class<?> friendlyByteBufClass = classLoader.loadClass("net.minecraft.network.FriendlyByteBuf");
            
            // Get packet class name and create identifier
            String packetName = getPacketName(message.getClass());
            Object packetId = identifierClass.getConstructor(String.class, String.class)
                .newInstance(MOD_ID, packetName);
            
            // Create buffer
            Object friendlyBuf = friendlyByteBufClass.getConstructor().newInstance();
            
            // Try static encode method first, then instance toBytes method
            try {
                java.lang.reflect.Method encodeMethod = message.getClass().getMethod("encode", message.getClass(), friendlyByteBufClass);
                encodeMethod.invoke(null, message, friendlyBuf);
            } catch (NoSuchMethodException e) {
                // Fall back to instance toBytes method
                java.lang.reflect.Method toBytesMethod = message.getClass().getMethod("toBytes", friendlyByteBufClass);
                toBytesMethod.invoke(message, friendlyBuf);
            }
            
            // Wrap in PacketByteBuf
            Object packetBuf = packetByteBufClass.getConstructor(friendlyByteBufClass).newInstance(friendlyBuf);
            
            // Send to server
            java.lang.reflect.Method sendMethod = clientPlayNetworkingClass.getMethod("send", identifierClass, Object.class);
            sendMethod.invoke(null, packetId, packetBuf);
        } catch (Exception e) {
            LOGGER.error("Error in sendToServer", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Register client-side packet handlers (called from FabricClientSetup)
     * Registers all client-bound packets so the client can receive them from the server
     */
    public static void registerClientPackets() {
        LOGGER.info("Registering Fabric client packet handlers...");
        
        try {
            if (CLIENT_PACKET_IDS == null || CLIENT_PACKET_CLASSES == null) {
                LOGGER.warn("Client packet IDs or classes not initialized - skipping client packet registration");
                return;
            }
            
            if (CLIENT_PACKET_IDS.length != CLIENT_PACKET_CLASSES.length) {
                LOGGER.error("Mismatch between client packet IDs and classes count");
                return;
            }
            
            ClassLoader classLoader = FabricModMessages.class.getClassLoader();
            Class<?> clientPlayNetworkingClass = classLoader.loadClass("net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking");
            Class<?> identifierClass = classLoader.loadClass("net.minecraft.util.Identifier");
            Class<?> packetByteBufClass = classLoader.loadClass("net.minecraft.network.PacketByteBuf");
            Class<?> friendlyByteBufClass = classLoader.loadClass("net.minecraft.network.FriendlyByteBuf");
            
            // Register each client packet
            for (int i = 0; i < CLIENT_PACKET_IDS.length; i++) {
                String packetName = CLIENT_PACKET_IDS[i];
                Class<?> packetClass = CLIENT_PACKET_CLASSES[i];
                
                try {
                    // Create packet identifier
                    Object packetId = identifierClass.getConstructor(String.class, String.class)
                        .newInstance(MOD_ID, packetName);
                    
                    // Get the ClientPlayNetworking.registerReceiver method
                    java.lang.reflect.Method registerMethod = clientPlayNetworkingClass.getMethod(
                        "registerReceiver",
                        identifierClass,
                        java.util.function.BiConsumer.class
                    );
                    
                    // Create handler that decodes packet and calls handle method
                    java.util.function.BiConsumer<Object, Object> handler = (client, buf) -> {
                        try {
                            // Convert PacketByteBuf to FriendlyByteBuf
                            Object friendlyBuf = friendlyByteBufClass.cast(buf);
                            
                            // Try static decode method first, then constructor with FriendlyByteBuf
                            Object packet;
                            try {
                                java.lang.reflect.Method decodeMethod = packetClass.getMethod("decode", friendlyByteBufClass);
                                packet = decodeMethod.invoke(null, friendlyBuf);
                            } catch (NoSuchMethodException e) {
                                // Fall back to constructor with FriendlyByteBuf
                                java.lang.reflect.Constructor<?> constructor = packetClass.getConstructor(friendlyByteBufClass);
                                packet = constructor.newInstance(friendlyBuf);
                            }
                            
                            // Call handle method with client as context
                            java.lang.reflect.Method handleMethod = packetClass.getMethod("handle", Object.class);
                            handleMethod.invoke(packet, client);
                        } catch (Exception e) {
                            LOGGER.error("Error handling client packet {}", packetName, e);
                            e.printStackTrace();
                        }
                    };
                    
                    registerMethod.invoke(null, packetId, handler);
                    LOGGER.debug("Registered client packet: {}", packetName);
                } catch (Exception e) {
                    LOGGER.error("Error registering client packet {}", packetName, e);
                    e.printStackTrace();
                }
            }
            
            LOGGER.info("Fabric client packet handlers registered successfully ({} packets)", CLIENT_PACKET_IDS.length);
        } catch (Exception e) {
            LOGGER.error("Error registering client packet handlers", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Get packet name from packet class
     */
    private static String getPacketName(Class<?> packetClass) {
        String simpleName = packetClass.getSimpleName();
        // Convert CamelCase to snake_case
        return simpleName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * Load class with fallback classloaders
     */
    private static Class<?> loadClassWithFallback(ClassLoader classLoader, String className) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e1) {
            try {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if (contextClassLoader != null) {
                    return contextClassLoader.loadClass(className);
                }
            } catch (ClassNotFoundException e2) {
                // Fall through
            }
            throw new ClassNotFoundException("Could not load " + className + " from any classloader", e1);
        }
    }
}
