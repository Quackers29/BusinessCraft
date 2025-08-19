package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
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
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        System.out.println("SET PLATFORM PATH CREATION MODE PACKET: Player is setting platform " + platformId + " path creation mode to " + mode + " at [" + x + ", " + y + ", " + z + "]");
        
        // Get the town interface block entity using platform services
        Object blockEntity = getBlockEntity(player);
        if (blockEntity == null) {
            LOGGER.warn("No block entity found at [{}, {}, {}]", x, y, z);
            return;
        }

        Object townDataProvider = getTownDataProvider(blockEntity);
        if (townDataProvider == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Set the platform path creation mode through platform services
        // NOTE: Platform service handles complex platform operations
        boolean success = PlatformServices.getBlockEntityHelper().setPlatformCreationMode(townDataProvider, mode, platformId);
        
        if (!success) {
            LOGGER.warn("Failed to set platform creation mode at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Update the platform path handler state through platform services (client-side state management)
        if (mode) {
            PlatformServices.getPlatformHelper().setActivePlatformForPathCreation(x, y, z, platformId);
        } else {
            PlatformServices.getPlatformHelper().clearActivePlatformForPathCreation();
        }
        
        LOGGER.debug("Successfully set platform {} path creation mode to {} at [{}, {}, {}]", 
                    platformId, mode, x, y, z);
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
    public boolean getMode() { return mode; }
}