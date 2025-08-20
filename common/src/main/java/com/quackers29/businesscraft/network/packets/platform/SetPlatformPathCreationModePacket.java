package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for setting platform path creation mode.
 * 
 * Unified Architecture approach: Direct access to TownInterfaceEntity methods
 * instead of platform service wrapper calls.
 */
public class SetPlatformPathCreationModePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformPathCreationModePacket.class);
    private final String platformId;
    private final boolean mode;
    
    /**
     * Create packet for sending.
     */
    public SetPlatformPathCreationModePacket(int x, int y, int z, String platformId, boolean mode) {
        super(x, y, z);
        this.platformId = platformId;
        this.mode = mode;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static SetPlatformPathCreationModePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        boolean mode = PlatformServices.getNetworkHelper().readBoolean(buffer);
        return new SetPlatformPathCreationModePacket(pos[0], pos[1], pos[2], platformId, mode);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, mode);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing SetPlatformPathCreationModePacket: platform {} mode={} at ({}, {}, {})", 
            platformId, mode, x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Direct business logic access - no abstraction layer needed
            townData.setPathCreationMode(mode, platformId);
            
            // Platform-specific client-side state management still uses platform services
            if (mode) {
                PlatformServices.getPlatformHelper().setActivePlatformForPathCreation(x, y, z, platformId);
            } else {
                PlatformServices.getPlatformHelper().clearActivePlatformForPathCreation();
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully set platform {} path creation mode to {} at ({}, {}, {})", 
                        platformId, mode, x, y, z);
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for platform path creation mode", x, y, z);
        }
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
    public boolean getMode() { return mode; }
}