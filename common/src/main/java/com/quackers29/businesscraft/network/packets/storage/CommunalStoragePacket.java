package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for communal storage operations.
 * This handles adding and removing items from town communal storage.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class CommunalStoragePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunalStoragePacket.class);
    
    private final Object itemStack; // Platform-agnostic item representation
    private final int slotId;
    private final boolean isAdd; // true for add, false for remove
    
    /**
     * Create packet for sending.
     */
    public CommunalStoragePacket(int x, int y, int z, Object itemStack, int slotId, boolean isAdd) {
        super(x, y, z);
        this.itemStack = itemStack;
        this.slotId = slotId;
        this.isAdd = isAdd;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static CommunalStoragePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        Object itemStack = PlatformServices.getNetworkHelper().readItemStack(buffer);
        int slotId = PlatformServices.getNetworkHelper().readInt(buffer);
        boolean isAdd = PlatformServices.getNetworkHelper().readBoolean(buffer);
        return new CommunalStoragePacket(pos[0], pos[1], pos[2], itemStack, slotId, isAdd);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeItemStack(buffer, itemStack);
        PlatformServices.getNetworkHelper().writeInt(buffer, slotId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, isAdd);
    }
    
    /**
     * Handle the packet on the server side.
     * This method processes communal storage add/remove operations.
     */
    @Override
    public void handle(Object player) {
        String operation = isAdd ? "add" : "remove";
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing communal storage {} operation for slot {} at position [{}, {}, {}]", 
                    operation, slotId, x, y, z);
        
        // Unified Architecture: Get TownInterfaceData for basic validation (reduces abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Basic validation through unified architecture
            if (!townData.isTownRegistered()) {
                LOGGER.warn("Town not registered at position ({}, {}, {}) for communal storage {}", x, y, z, operation);
                return;
            }
            
            // Complex inventory operations still use platform services (appropriate abstraction)
            // NOTE: Communal storage involves platform-specific ItemStack and container handling
            Object blockEntity = getBlockEntity(player); // Still needed for platform-specific storage operations
            
            boolean success;
            if (isAdd) {
                success = PlatformServices.getBlockEntityHelper().addToCommunalStorage(blockEntity, player, itemStack, slotId);
            } else {
                success = PlatformServices.getBlockEntityHelper().removeFromCommunalStorage(blockEntity, player, itemStack, slotId);
            }
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully processed communal storage {} operation for slot {} at ({}, {}, {})", 
                            operation, slotId, x, y, z);
            } else {
                LOGGER.warn("Failed to process communal storage {} operation for slot {} at ({}, {}, {})", 
                           operation, slotId, x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for communal storage {}", x, y, z, operation);
        }
    }
    
    // Getters for testing
    public Object getItemStack() { return itemStack; }
    public int getSlotId() { return slotId; }
    public boolean isAdd() { return isAdd; }
}