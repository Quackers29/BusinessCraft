package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.network.PacketRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.quackers29.businesscraft.debug.DebugConfig;

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
            List<String> clientPacketIds = new ArrayList<>();
            List<Class<?>> clientPacketClasses = new ArrayList<>();

            for (PacketRegistry.PacketDefinition<?> packet : PacketRegistry.getPackets()) {
                if (packet.direction() == PacketRegistry.NetworkDirection.PLAY_TO_SERVER) {
                    registerServerPacket(packet);
                } else {
                    // Register client-to-client packets (PLAY_TO_CLIENT)
                    // Server side registration for sending
                    registerClientPacketForServer(packet.name(), packet.packetClass());

                    // Store for client-side registration
                    clientPacketIds.add(packet.name());
                    clientPacketClasses.add(packet.packetClass());
                }
            }

            // Store identifiers for client-side registration
            CLIENT_PACKET_IDS = clientPacketIds.toArray(new String[0]);
            CLIENT_PACKET_CLASSES = clientPacketClasses.toArray(new Class<?>[0]);

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Client packet arrays initialized with {} packets",
                    CLIENT_PACKET_IDS.length);

            LOGGER.info("Fabric network messages registered successfully ({} total packets)",
                    PacketRegistry.getPackets().size());
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
    /**
     * Register a server-side packet handler (PLAY_TO_SERVER)
     */
    private static <MSG> void registerServerPacket(PacketRegistry.PacketDefinition<MSG> packetDef) {
        String packetName = packetDef.name();
        try {
            // Create packet-specific identifier
            ResourceLocation packetId = new ResourceLocation(MOD_ID, packetName);

            ServerPlayNetworking.registerGlobalReceiver(packetId, (server, player, handler, buf, responseSender) -> {
                try {
                    LOGGER.info("[PACKET RECEIVED] Server received packet: {} from player: {}", packetName,
                            player.getName().getString());

                    // Convert PacketByteBuf to FriendlyByteBuf (they're compatible)
                    FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(buf);

                    // Use the decoder from the definition
                    MSG packet = packetDef.decoder().apply(friendlyBuf);
                    LOGGER.info("[PACKET DECODED] Successfully decoded packet: {}", packetName);

                    // Execute on server thread
                    server.execute(() -> {
                        try {
                            LOGGER.info("[PACKET HANDLING] About to handle packet: {} for player: {}", packetName,
                                    player.getName().getString());
                            // Use the handler from the definition
                            packetDef.handler().accept(packet, player);
                            LOGGER.info("[PACKET HANDLED] Successfully handled packet: {}", packetName);
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

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "[SEND TO PLAYER] Preparing to send packet: {} with ID: {}", packetName, packetId);

            // Create buffer
            FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(Unpooled.buffer());

            // Try instance encode method first, then instance toBytes method
            try {
                java.lang.reflect.Method encodeMethod = message.getClass().getMethod("encode", FriendlyByteBuf.class);
                encodeMethod.invoke(message, friendlyBuf);
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "[SEND TO PLAYER] Encoded packet using instance encode method");
            } catch (NoSuchMethodException e) {
                // Fall back to instance toBytes method
                java.lang.reflect.Method toBytesMethod = message.getClass().getMethod("toBytes", FriendlyByteBuf.class);
                toBytesMethod.invoke(message, friendlyBuf);
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "[SEND TO PLAYER] Encoded packet using instance toBytes method");
            }

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "[SEND TO PLAYER] Buffer size: {} bytes",
                    friendlyBuf.writerIndex());

            // Send to player
            ServerPlayNetworking.send(serverPlayer, packetId, friendlyBuf);
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "[SEND TO PLAYER] Successfully sent packet: {} to player: {}", packetName,
                    serverPlayer.getName().getString());
        } catch (Exception e) {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Error in sendToPlayer", e);
        }
    }

    private static net.minecraft.server.MinecraftServer serverInstance;

    public static void setServer(net.minecraft.server.MinecraftServer server) {
        serverInstance = server;
    }

    /**
     * Send a message to all players on the server
     */
    public static void sendToAllPlayers(Object message) {
        try {
            if (serverInstance != null) {
                for (ServerPlayer player : PlayerLookup.all(serverInstance)) {
                    sendToPlayer(message, player);
                }
            } else {
                LOGGER.warn("sendToAllPlayers called but server instance is null");
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

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "[SEND TO SERVER] Preparing to send packet: {} with ID: {}", packetName, packetId);

            // Create buffer
            FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(Unpooled.buffer());

            // Try instance encode method first, then instance toBytes method
            try {
                java.lang.reflect.Method encodeMethod = message.getClass().getMethod("encode", FriendlyByteBuf.class);
                encodeMethod.invoke(message, friendlyBuf);
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "[SEND TO SERVER] Encoded packet using instance encode method");
            } catch (NoSuchMethodException e) {
                // Fall back to instance toBytes method
                java.lang.reflect.Method toBytesMethod = message.getClass().getMethod("toBytes", FriendlyByteBuf.class);
                toBytesMethod.invoke(message, friendlyBuf);
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "[SEND TO SERVER] Encoded packet using instance toBytes method");
            }

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "[SEND TO SERVER] Buffer size: {} bytes",
                    friendlyBuf.writerIndex());

            // Send to server
            ClientPlayNetworking.send(packetId, friendlyBuf);
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "[SEND TO SERVER] Successfully sent packet: {}",
                    packetName);
        } catch (Exception e) {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Error in sendToServer", e);
        }
    }

    /**
     * Register client-side packet handlers (called from FabricClientSetup)
     * Registers all client-bound packets so the client can receive them from the
     * server
     */
    public static void registerClientPackets() {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Registering Fabric client packet handlers...");

        try {
            if (CLIENT_PACKET_IDS == null) {
                LOGGER.error("CLIENT_PACKET_IDS is null!");
                return;
            }
            if (CLIENT_PACKET_CLASSES == null) {
                LOGGER.error("CLIENT_PACKET_CLASSES is null!");
                return;
            }

            if (CLIENT_PACKET_IDS.length != CLIENT_PACKET_CLASSES.length) {
                LOGGER.error("Mismatch between client packet IDs and classes count");
                return;
            }

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Registering {} client packet handlers",
                    CLIENT_PACKET_IDS.length);

            // Register each client packet
            for (int i = 0; i < CLIENT_PACKET_IDS.length; i++) {
                String packetName = CLIENT_PACKET_IDS[i];
                Class<?> packetClass = CLIENT_PACKET_CLASSES[i];

                try {
                    // Find definition
                    PacketRegistry.PacketDefinition<?> definition = null;
                    for (PacketRegistry.PacketDefinition<?> def : PacketRegistry.getPackets()) {
                        if (def.name().equals(packetName)) {
                            definition = def;
                            break;
                        }
                    }

                    if (definition == null) {
                        LOGGER.error("Could not find definition for client packet: {}", packetName);
                        continue;
                    }

                    final PacketRegistry.PacketDefinition<?> finalDef = definition;

                    // Create packet identifier
                    ResourceLocation packetId = new ResourceLocation(MOD_ID, packetName);
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Registering client packet: {} -> {}",
                            packetName, packetId);

                    ClientPlayNetworking.registerGlobalReceiver(packetId, (client, handler, buf, responseSender) -> {
                        try {
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Received client packet: {}",
                                    packetName);
                            // Convert PacketByteBuf to FriendlyByteBuf
                            FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(buf);

                            // Use decoder
                            Object packet = finalDef.decoder().apply(friendlyBuf);

                            // Execute on client thread
                            client.execute(() -> {
                                try {
                                    // Use handler
                                    @SuppressWarnings("unchecked")
                                    BiConsumer<Object, Object> packetHandler = (BiConsumer<Object, Object>) finalDef
                                            .handler();
                                    packetHandler.accept(packet, client);
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
