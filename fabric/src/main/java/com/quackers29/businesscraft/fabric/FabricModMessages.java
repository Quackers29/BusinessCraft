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

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import io.netty.buffer.Unpooled;
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
            // Register server-to-server packets (PLAY_TO_SERVER)
            registerServerPacket("toggle_tourist_spawning", ToggleTouristSpawningPacket.class);
            registerServerPacket("set_town_name", SetTownNamePacket.class);
            registerServerPacket("set_search_radius", SetSearchRadiusPacket.class);
            registerServerPacket("set_path_creation_mode", SetPathCreationModePacket.class);
            registerServerPacket("add_platform", AddPlatformPacket.class);
            registerServerPacket("delete_platform", DeletePlatformPacket.class);
            registerServerPacket("set_platform_enabled", SetPlatformEnabledPacket.class);
            registerServerPacket("set_platform_path", SetPlatformPathPacket.class);
            registerServerPacket("reset_platform_path", ResetPlatformPathPacket.class);
            registerServerPacket("set_platform_path_creation_mode", SetPlatformPathCreationModePacket.class);
            registerServerPacket("set_platform_destination", SetPlatformDestinationPacket.class);
            registerServerPacket("open_destinations_ui", OpenDestinationsUIPacket.class);
            registerServerPacket("player_exit_ui", PlayerExitUIPacket.class);
            registerServerPacket("boundary_sync_request", BoundarySyncRequestPacket.class);
            registerServerPacket("open_town_interface", OpenTownInterfacePacket.class);
            registerServerPacket("open_payment_board", OpenPaymentBoardPacket.class);
            registerServerPacket("request_town_map_data", RequestTownMapDataPacket.class);
            registerServerPacket("request_town_platform_data", RequestTownPlatformDataPacket.class);
            registerServerPacket("trade_resource", TradeResourcePacket.class);
            registerServerPacket("communal_storage", CommunalStoragePacket.class);
            registerServerPacket("payment_board_request", PaymentBoardRequestPacket.class);
            registerServerPacket("payment_board_claim", PaymentBoardClaimPacket.class);
            registerServerPacket("buffer_storage", BufferStoragePacket.class);
            registerServerPacket("personal_storage", PersonalStoragePacket.class);
            registerServerPacket("personal_storage_request", PersonalStorageRequestPacket.class);
            registerServerPacket("request_town_data", RequestTownDataPacket.class);

            // Register client-to-client packets (PLAY_TO_CLIENT) - these need to be
            // registered on both sides
            // Server side registration for sending
            registerClientPacketForServer("refresh_platforms", RefreshPlatformsPacket.class);
            registerClientPacketForServer("refresh_destinations", RefreshDestinationsPacket.class);
            registerClientPacketForServer("platform_visualization", PlatformVisualizationPacket.class);
            registerClientPacketForServer("boundary_sync_response", BoundarySyncResponsePacket.class);
            registerClientPacketForServer("town_map_data_response", TownMapDataResponsePacket.class);
            registerClientPacketForServer("town_platform_data_response", TownPlatformDataResponsePacket.class);
            registerClientPacketForServer("communal_storage_response", CommunalStorageResponsePacket.class);
            registerClientPacketForServer("payment_board_response", PaymentBoardResponsePacket.class);
            registerClientPacketForServer("buffer_storage_response", BufferStorageResponsePacket.class);
            registerClientPacketForServer("buffer_slot_storage_response", BufferSlotStorageResponsePacket.class);
            registerClientPacketForServer("personal_storage_response", PersonalStorageResponsePacket.class);
            registerClientPacketForServer("payment_result", PaymentResultPacket.class);
            registerClientPacketForServer("town_data_response", TownDataResponsePacket.class);

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
                    BoundarySyncResponsePacket.class, TownMapDataResponsePacket.class,
                    TownPlatformDataResponsePacket.class,
                    CommunalStorageResponsePacket.class, PaymentBoardResponsePacket.class,
                    BufferStorageResponsePacket.class,
                    BufferSlotStorageResponsePacket.class, PersonalStorageResponsePacket.class,
                    PaymentResultPacket.class,
                    TownDataResponsePacket.class
            };

            LOGGER.info("Fabric network messages registered successfully ({} server packets, {} client packets)",
                    26, CLIENT_PACKET_IDS.length);
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
    private static void registerServerPacket(String packetName, Class<?> packetClass) {
        try {
            // Create packet-specific identifier
            ResourceLocation packetId = new ResourceLocation(MOD_ID, packetName);

            ServerPlayNetworking.registerGlobalReceiver(packetId, (server, player, handler, buf, responseSender) -> {
                try {
                    // Convert PacketByteBuf to FriendlyByteBuf (they're compatible)
                    FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(buf);

                    // Try static decode method first, then constructor with FriendlyByteBuf
                    Object packet;
                    try {
                        java.lang.reflect.Method decodeMethod = packetClass.getMethod("decode", FriendlyByteBuf.class);
                        packet = decodeMethod.invoke(null, friendlyBuf);
                    } catch (NoSuchMethodException e) {
                        // Fall back to constructor with FriendlyByteBuf
                        java.lang.reflect.Constructor<?> constructor = packetClass
                                .getConstructor(FriendlyByteBuf.class);
                        packet = constructor.newInstance(friendlyBuf);
                    }

                    // Execute on server thread
                    Object finalPacket = packet;
                    server.execute(() -> {
                        try {
                            // Create a context object for the packet (Fabric doesn't have
                            // NetworkEvent.Context)
                            // Pass the player as the context - packets expect Object context
                            java.lang.reflect.Method handleMethod = packetClass.getMethod("handle", Object.class);
                            handleMethod.invoke(finalPacket, player);
                        } catch (Exception e) {
                            LOGGER.error("Error handling server packet {}", packetName, e);
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Error decoding server packet {}", packetName, e);
                    e.printStackTrace();
                }
            });

            LOGGER.debug("Registered server packet: {}", packetName);
        } catch (Exception e) {
            LOGGER.error("Error registering server packet {}", packetName, e);
            e.printStackTrace();
        }
    }

    /**
     * Register a client-side packet for server-side sending (PLAY_TO_CLIENT)
     * This just stores the information - actual client registration happens in
     * FabricClientSetup
     */
    private static void registerClientPacketForServer(String packetName, Class<?> packetClass) {
        // No-op for now - client registration happens in FabricClientSetup
        LOGGER.debug("Prepared client packet for registration: {}", packetName);
    }

    /**
     * Send a message to a specific player
     */
    public static void sendToPlayer(Object message, Object player) {
        try {
            if (!(player instanceof ServerPlayer))
                return;
            ServerPlayer serverPlayer = (ServerPlayer) player;

            // Get packet class name and create identifier
            String packetName = getPacketName(message.getClass());
            ResourceLocation packetId = new ResourceLocation(MOD_ID, packetName);

            // Create buffer
            FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(Unpooled.buffer());

            // Try static encode method first, then instance toBytes method
            try {
                java.lang.reflect.Method encodeMethod = message.getClass().getMethod("encode", message.getClass(),
                        FriendlyByteBuf.class);
                encodeMethod.invoke(null, message, friendlyBuf);
            } catch (NoSuchMethodException e) {
                // Fall back to instance toBytes method
                java.lang.reflect.Method toBytesMethod = message.getClass().getMethod("toBytes", FriendlyByteBuf.class);
                toBytesMethod.invoke(message, friendlyBuf);
            }

            // Send to player
            ServerPlayNetworking.send(serverPlayer, packetId, friendlyBuf);
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
            // Get server instance
            // This is a bit hacky, but we don't have direct access to the server instance
            // here easily
            // In a real impl we might want to pass it in or use a singleton
            // For now, we'll rely on the caller to use sendToPlayer if they have the server
            // Or we can try to get it from a player if we had one

            // Since we can't easily get the server here without context, we'll log a
            // warning
            // In a proper Fabric impl, we'd use PlayerLookup.all(server)
            LOGGER.warn("sendToAllPlayers called but server instance not available directly");

        } catch (Exception e) {
            LOGGER.error("Error in sendToAllPlayers", e);
        }
    }

    /**
     * Send a message to all players tracking a specific chunk
     */
    public static void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        try {
            if (!(level instanceof net.minecraft.server.level.ServerLevel))
                return;
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;

            if (pos instanceof net.minecraft.core.BlockPos) {
                net.minecraft.core.BlockPos blockPos = (net.minecraft.core.BlockPos) pos;

                for (ServerPlayer player : PlayerLookup.tracking(serverLevel, blockPos)) {
                    sendToPlayer(message, player);
                }
            } else if (pos instanceof net.minecraft.world.level.ChunkPos) {
                net.minecraft.world.level.ChunkPos chunkPos = (net.minecraft.world.level.ChunkPos) pos;

                for (ServerPlayer player : PlayerLookup.tracking(serverLevel, chunkPos)) {
                    sendToPlayer(message, player);
                }
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
            // Get packet class name and create identifier
            String packetName = getPacketName(message.getClass());
            ResourceLocation packetId = new ResourceLocation(MOD_ID, packetName);

            // Create buffer
            FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(Unpooled.buffer());

            // Try static encode method first, then instance toBytes method
            try {
                java.lang.reflect.Method encodeMethod = message.getClass().getMethod("encode", message.getClass(),
                        FriendlyByteBuf.class);
                encodeMethod.invoke(null, message, friendlyBuf);
            } catch (NoSuchMethodException e) {
                // Fall back to instance toBytes method
                java.lang.reflect.Method toBytesMethod = message.getClass().getMethod("toBytes", FriendlyByteBuf.class);
                toBytesMethod.invoke(message, friendlyBuf);
            }

            // Send to server
            ClientPlayNetworking.send(packetId, friendlyBuf);
        } catch (Exception e) {
            LOGGER.error("Error in sendToServer", e);
            e.printStackTrace();
        }
    }

    /**
     * Register client-side packet handlers (called from FabricClientSetup)
     * Registers all client-bound packets so the client can receive them from the
     * server
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

            // Register each client packet
            for (int i = 0; i < CLIENT_PACKET_IDS.length; i++) {
                String packetName = CLIENT_PACKET_IDS[i];
                Class<?> packetClass = CLIENT_PACKET_CLASSES[i];

                try {
                    // Create packet identifier
                    ResourceLocation packetId = new ResourceLocation(MOD_ID, packetName);

                    ClientPlayNetworking.registerGlobalReceiver(packetId, (client, handler, buf, responseSender) -> {
                        try {
                            // Convert PacketByteBuf to FriendlyByteBuf
                            FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(buf);

                            // Try static decode method first, then constructor with FriendlyByteBuf
                            Object packet;
                            try {
                                java.lang.reflect.Method decodeMethod = packetClass.getMethod("decode",
                                        FriendlyByteBuf.class);
                                packet = decodeMethod.invoke(null, friendlyBuf);
                            } catch (NoSuchMethodException e) {
                                // Fall back to constructor with FriendlyByteBuf
                                java.lang.reflect.Constructor<?> constructor = packetClass
                                        .getConstructor(FriendlyByteBuf.class);
                                packet = constructor.newInstance(friendlyBuf);
                            }

                            // Execute on client thread
                            Object finalPacket = packet;
                            client.execute(() -> {
                                try {
                                    // Call handle method with client as context
                                    java.lang.reflect.Method handleMethod = packetClass.getMethod("handle",
                                            Object.class);
                                    handleMethod.invoke(finalPacket, client);
                                } catch (Exception e) {
                                    LOGGER.error("Error handling client packet {}", packetName, e);
                                    e.printStackTrace();
                                }
                            });

                            LOGGER.debug("Registered client packet: {}", packetName);
                        } catch (Exception e) {
                            LOGGER.error("Error registering client packet {}", packetName, e);
                            e.printStackTrace();
                        }
                    });
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
}
