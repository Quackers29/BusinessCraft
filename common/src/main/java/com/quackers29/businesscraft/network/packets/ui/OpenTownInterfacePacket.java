package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to request opening the Town Interface UI.
 * 
 * Unified Architecture approach: Uses direct access to TownInterfaceEntity when possible,
 * falls back to platform services for complex UI operations.
 */
public class OpenTownInterfacePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTownInterfacePacket.class);
    
    /**
     * Create packet for sending.
     */
    public OpenTownInterfacePacket(int x, int y, int z) {
        super(x, y, z);
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public OpenTownInterfacePacket(Object buffer) {
        super(buffer);
    }
    
    /**
     * Static decode method for network registration.
     */
    public static OpenTownInterfacePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        return new OpenTownInterfacePacket(pos[0], pos[1], pos[2]);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing OpenTownInterfacePacket at position ({}, {}, {})", x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Basic validation through unified architecture
            if (!townData.isTownRegistered()) {
                LOGGER.warn("Town not registered at position ({}, {}, {}) for UI opening", x, y, z);
                return;
            }
            
            // Complex UI operations still use platform services (appropriate abstraction)
            Object blockEntity = getBlockEntity(player);
            boolean success = PlatformServices.getBlockEntityHelper().openTownInterfaceUI(blockEntity, player);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully opened Town Interface for player at ({}, {}, {})", x, y, z);
            } else {
                LOGGER.warn("Failed to open Town Interface for player at ({}, {}, {}) - UI opening failed", x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for UI opening", x, y, z);
        }
    }
}