package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import org.slf4j.Logger;
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
     * This method contains the core server-side logic which is platform-agnostic.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Opening Town Interface for player at position [{}, {}, {}]", x, y, z);
        
        // Get the town interface using unified access
        TownInterfaceEntity townInterface = getTownInterfaceEntity(player);
        if (townInterface == null) {
            LOGGER.error("Failed to get TownInterfaceEntity at position: [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Open the Town Interface UI (still uses platform services for complex UI operations)
        boolean success = PlatformServices.getBlockEntityHelper().openTownInterfaceUI(townInterface, player);
        
        if (success) {
            LOGGER.debug("Successfully opened Town Interface for player at [{}, {}, {}]", x, y, z);
        } else {
            LOGGER.warn("Failed to open Town Interface for player at [{}, {}, {}]", x, y, z);
        }
    }
}