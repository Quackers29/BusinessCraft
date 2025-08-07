package com.quackers29.businesscraft.network;

/**
 * Common networking coordinator for BusinessCraft.
 * This class initializes platform-agnostic networking definitions and coordinates
 * with platform modules for actual packet registration.
 * 
 * Enhanced MultiLoader approach: Common module defines networking structure,
 * platform modules implement registration using their specific APIs (SimpleChannel, Fabric Networking API, etc.).
 */
public class CommonNetworking {
    
    /**
     * Initialize common networking systems.
     * This is called by platform modules during mod initialization.
     */
    public static void initialize() {
        // Initialize packet definitions
        NetworkRegistry.initialize();
    }
    
    /**
     * Get the total number of packets defined.
     * Useful for platform modules to verify complete registration.
     */
    public static int getPacketCount() {
        return NetworkRegistry.getTotalPacketCount();
    }
}