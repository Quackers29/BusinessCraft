package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

/**
 * Platform-agnostic server-to-client packet for communal storage data synchronization.
 * This updates the client-side communal storage UI with current storage contents.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class CommunalStorageResponsePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunalStorageResponsePacket.class);
    
    private final Map<Integer, Object> storageItems; // slot -> ItemStack mapping
    private final boolean isSuccess;
    
    /**
     * Create packet for sending successful response.
     */
    public CommunalStorageResponsePacket(int x, int y, int z, Map<Integer, Object> storageItems) {
        super(x, y, z);
        this.storageItems = storageItems != null ? new HashMap<>(storageItems) : new HashMap<>();
        this.isSuccess = true;
    }
    
    /**
     * Create packet for sending error response.
     */
    public CommunalStorageResponsePacket(int x, int y, int z, boolean isSuccess) {
        super(x, y, z);
        this.storageItems = new HashMap<>();
        this.isSuccess = isSuccess;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static CommunalStorageResponsePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        boolean isSuccess = PlatformServices.getNetworkHelper().readBoolean(buffer);
        
        Map<Integer, Object> storageItems = new HashMap<>();
        if (isSuccess) {
            int mapSize = PlatformServices.getNetworkHelper().readInt(buffer);
            for (int i = 0; i < mapSize; i++) {
                int slotId = PlatformServices.getNetworkHelper().readInt(buffer);
                Object itemStack = PlatformServices.getNetworkHelper().readItemStack(buffer);
                storageItems.put(slotId, itemStack);
            }
        }
        
        return new CommunalStorageResponsePacket(pos[0], pos[1], pos[2], storageItems);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeBoolean(buffer, isSuccess);
        
        if (isSuccess) {
            PlatformServices.getNetworkHelper().writeInt(buffer, storageItems.size());
            for (Map.Entry<Integer, Object> entry : storageItems.entrySet()) {
                PlatformServices.getNetworkHelper().writeInt(buffer, entry.getKey());
                PlatformServices.getNetworkHelper().writeItemStack(buffer, entry.getValue());
            }
        }
    }
    
    /**
     * Handle the packet on the client side.
     * This method updates the client-side communal storage UI.
     * Note: This packet is for client-side UI updates, so we still use platform services
     * for UI operations which haven't been unified yet.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing communal storage response with {} items at position [{}, {}, {}]", 
                    storageItems.size(), x, y, z);
        
        if (!isSuccess) {
            LOGGER.warn("Received failed communal storage response at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Update client-side communal storage UI through platform services
        // Note: Client-side UI operations still use platform services as they involve complex GUI interactions
        boolean success = PlatformServices.getBlockEntityHelper().updateCommunalStorageUI(player, x, y, z, storageItems);
        
        if (success) {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully updated communal storage UI with {} items at [{}, {}, {}]", 
                        storageItems.size(), x, y, z);
        } else {
            LOGGER.warn("Failed to update communal storage UI at [{}, {}, {}]", x, y, z);
        }
    }
    
    // Getters for testing
    public Map<Integer, Object> getStorageItems() { return new HashMap<>(storageItems); }
    public boolean isSuccess() { return isSuccess; }
}