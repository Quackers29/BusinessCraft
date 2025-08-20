package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

/**
 * Platform-agnostic server-to-client packet for buffer slot storage data synchronization.
 * This updates the client-side payment board buffer UI with current buffer contents.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class BufferSlotStorageResponsePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferSlotStorageResponsePacket.class);
    
    private final Map<Integer, Object> bufferSlots; // slot -> ItemStack mapping
    private final boolean isSuccess;
    
    /**
     * Create packet for sending successful response.
     */
    public BufferSlotStorageResponsePacket(int x, int y, int z, Map<Integer, Object> bufferSlots) {
        super(x, y, z);
        this.bufferSlots = bufferSlots != null ? new HashMap<>(bufferSlots) : new HashMap<>();
        this.isSuccess = true;
    }
    
    /**
     * Create packet for sending error response.
     */
    public BufferSlotStorageResponsePacket(int x, int y, int z, boolean isSuccess) {
        super(x, y, z);
        this.bufferSlots = new HashMap<>();
        this.isSuccess = isSuccess;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static BufferSlotStorageResponsePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        boolean isSuccess = PlatformServices.getNetworkHelper().readBoolean(buffer);
        
        Map<Integer, Object> bufferSlots = new HashMap<>();
        if (isSuccess) {
            int mapSize = PlatformServices.getNetworkHelper().readInt(buffer);
            for (int i = 0; i < mapSize; i++) {
                int slotId = PlatformServices.getNetworkHelper().readInt(buffer);
                Object itemStack = PlatformServices.getNetworkHelper().readItemStack(buffer);
                bufferSlots.put(slotId, itemStack);
            }
        }
        
        return new BufferSlotStorageResponsePacket(pos[0], pos[1], pos[2], bufferSlots);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeBoolean(buffer, isSuccess);
        
        if (isSuccess) {
            PlatformServices.getNetworkHelper().writeInt(buffer, bufferSlots.size());
            for (Map.Entry<Integer, Object> entry : bufferSlots.entrySet()) {
                PlatformServices.getNetworkHelper().writeInt(buffer, entry.getKey());
                PlatformServices.getNetworkHelper().writeItemStack(buffer, entry.getValue());
            }
        }
    }
    
    /**
     * Handle the packet on the client side.
     * Unified Architecture approach: Client-side UI operations appropriately use platform services.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing buffer slot storage response with {} items at position ({}, {}, {})", 
                    bufferSlots.size(), x, y, z);
        
        if (!isSuccess) {
            LOGGER.warn("Received failed buffer slot storage response at ({}, {}, {})", x, y, z);
            return;
        }
        
        // Client-side UI operations appropriately use platform services (complex GUI interactions)
        boolean success = PlatformServices.getBlockEntityHelper().updateBufferStorageUI(player, x, y, z, bufferSlots);
        
        if (success) {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully updated buffer storage UI with {} items at ({}, {}, {})", 
                        bufferSlots.size(), x, y, z);
        } else {
            LOGGER.warn("Failed to update buffer storage UI at ({}, {}, {})", x, y, z);
        }
    }
    
    /**
     * Alternative client-side handler.
     * Unified Architecture approach: Client-side operations appropriately use platform services.
     */
    public void handleClient() {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Received buffer slot storage update with {} items", bufferSlots.size());
        
        try {
            // Get the current client screen through platform services
            Object currentScreen = PlatformServices.getPlatformHelper().getCurrentScreen();
            
            if (currentScreen != null) {
                // Client-side UI operations appropriately use platform services (complex GUI interactions)
                boolean success = PlatformServices.getBlockEntityHelper().updateBufferStorageUI(null, x, y, z, bufferSlots);
                
                if (success) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully updated buffer storage UI with {} items at ({}, {}, {})", 
                                bufferSlots.size(), x, y, z);
                } else {
                    LOGGER.warn("Failed to update buffer storage UI at ({}, {}, {})", x, y, z);
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "No current screen to update with buffer storage data");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling buffer slot storage update", e);
        }
    }
    
    // Getters for testing
    public Map<Integer, Object> getBufferSlots() { return new HashMap<>(bufferSlots); }
    public boolean isSuccess() { return isSuccess; }
}