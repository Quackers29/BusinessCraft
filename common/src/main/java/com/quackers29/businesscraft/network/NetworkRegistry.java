package com.quackers29.businesscraft.network;

import java.util.HashMap;
import java.util.Map;

/**
 * Platform-agnostic network registry for BusinessCraft packets.
 * This class defines WHAT packets to register without depending on specific platform APIs.
 * Platform modules use these definitions to create actual packet registrations.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and IDs,
 * platform modules implement registration using their specific networking APIs.
 */
public class NetworkRegistry {
    
    // Packet registry definitions
    private static final Map<String, PacketDefinition> SERVER_BOUND = new HashMap<>();
    private static final Map<String, PacketDefinition> CLIENT_BOUND = new HashMap<>();
    
    // Packet ID counter for consistent IDs across platforms
    private static int nextPacketId = 0;
    
    /**
     * Initialize all packet definitions.
     * This defines which packets should be registered on each platform.
     */
    public static void initialize() {
        defineServerBoundPackets();
        defineClientBoundPackets();
    }
    
    private static void defineServerBoundPackets() {
        // Town management packets
        registerServerBound("toggle_tourist_spawning", "ToggleTouristSpawningPacket");
        registerServerBound("set_town_name", "SetTownNamePacket");
        
        // Platform management packets
        registerServerBound("set_search_radius", "SetSearchRadiusPacket");
        registerServerBound("add_platform", "AddPlatformPacket");
        registerServerBound("delete_platform", "DeletePlatformPacket");
        registerServerBound("set_platform_enabled", "SetPlatformEnabledPacket");
        registerServerBound("set_platform_path", "SetPlatformPathPacket");
        registerServerBound("reset_platform_path", "ResetPlatformPathPacket");
        registerServerBound("set_platform_path_creation_mode", "SetPlatformPathCreationModePacket");
        registerServerBound("refresh_platforms", "RefreshPlatformsPacket");
        registerServerBound("set_platform_destination", "SetPlatformDestinationPacket");
        
        // UI interaction packets
        registerServerBound("set_path_creation_mode", "SetPathCreationModePacket");
        registerServerBound("open_destinations_ui", "OpenDestinationsUIPacket");
        registerServerBound("refresh_destinations", "RefreshDestinationsPacket");
        registerServerBound("player_exit_ui", "PlayerExitUIPacket");
        registerServerBound("boundary_sync_request", "BoundarySyncRequestPacket");
        registerServerBound("open_town_interface", "OpenTownInterfacePacket");
        registerServerBound("open_payment_board", "OpenPaymentBoardPacket");
        registerServerBound("request_town_map_data", "RequestTownMapDataPacket");
        registerServerBound("request_town_platform_data", "RequestTownPlatformDataPacket");
        
        // Storage and trading packets
        registerServerBound("trade_resource", "TradeResourcePacket");
        registerServerBound("communal_storage", "CommunalStoragePacket");
        registerServerBound("payment_board_request", "PaymentBoardRequestPacket");
        registerServerBound("payment_board_claim", "PaymentBoardClaimPacket");
        registerServerBound("buffer_storage", "BufferStoragePacket");
        registerServerBound("personal_storage", "PersonalStoragePacket");
        registerServerBound("personal_storage_request", "PersonalStorageRequestPacket");
    }
    
    private static void defineClientBoundPackets() {
        // UI data response packets
        registerClientBound("platform_visualization", "PlatformVisualizationPacket");
        registerClientBound("boundary_sync_response", "BoundarySyncResponsePacket");
        registerClientBound("town_map_data_response", "TownMapDataResponsePacket");
        registerClientBound("town_platform_data_response", "TownPlatformDataResponsePacket");
        
        // Storage response packets
        registerClientBound("communal_storage_response", "CommunalStorageResponsePacket");
        registerClientBound("payment_board_response", "PaymentBoardResponsePacket");
        registerClientBound("buffer_storage_response", "BufferStorageResponsePacket");
        registerClientBound("buffer_slot_storage_response", "BufferSlotStorageResponsePacket");
        registerClientBound("personal_storage_response", "PersonalStorageResponsePacket");
        
        // Misc packets
        registerClientBound("payment_result", "PaymentResultPacket");
    }
    
    private static void registerServerBound(String name, String className) {
        SERVER_BOUND.put(name, new PacketDefinition(nextPacketId++, name, className, PacketDirection.SERVER_BOUND));
    }
    
    private static void registerClientBound(String name, String className) {
        CLIENT_BOUND.put(name, new PacketDefinition(nextPacketId++, name, className, PacketDirection.CLIENT_BOUND));
    }
    
    // Getter methods for platform modules
    public static Map<String, PacketDefinition> getServerBoundPackets() { return SERVER_BOUND; }
    public static Map<String, PacketDefinition> getClientBoundPackets() { return CLIENT_BOUND; }
    public static int getTotalPacketCount() { return nextPacketId; }
    
    // Definition classes
    public static class PacketDefinition {
        public final int id;
        public final String name;
        public final String className;
        public final PacketDirection direction;
        
        public PacketDefinition(int id, String name, String className, PacketDirection direction) {
            this.id = id;
            this.name = name;
            this.className = className;
            this.direction = direction;
        }
    }
    
    public enum PacketDirection {
        SERVER_BOUND,
        CLIENT_BOUND
    }
}