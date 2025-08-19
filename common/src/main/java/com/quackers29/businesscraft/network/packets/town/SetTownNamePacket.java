package com.quackers29.businesscraft.network.packets.town;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic packet for setting a town name.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class SetTownNamePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetTownNamePacket.class);
    private final String newName;

    /**
     * Create packet for sending.
     */
    public SetTownNamePacket(int x, int y, int z, String newName) {
        super(x, y, z);
        this.newName = newName;
    }

    /**
     * Create packet from network buffer (decode constructor).
     */
    public SetTownNamePacket(Object buffer) {
        super(buffer);
        this.newName = PlatformServices.getNetworkHelper().readString(buffer);
    }

    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Writes BlockPos
        PlatformServices.getNetworkHelper().writeString(buffer, newName);
    }

    /**
     * Handle the packet on the server side.
     * This method contains the core business logic which is platform-agnostic.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Processing SetTownNamePacket for position ({}, {}, {}) with new name: '{}'", x, y, z, newName);
        
        // Platform services will provide block entity access
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        
        if (blockEntity != null) {
            // Use platform services to access town interface functionality
            Object townDataProvider = PlatformServices.getBlockEntityHelper().getTownDataProvider(blockEntity);
            
            if (townDataProvider != null) {
                // Core business logic - validate the new name
                String trimmedName = newName.trim();
                
                // Validation logic (platform-agnostic)
                String validationError = validateTownName(trimmedName);
                if (validationError != null) {
                    // Send error message through platform services
                    PlatformServices.getPlatformHelper().sendPlayerMessage(player, validationError, "RED");
                    return;
                }
                
                String oldName = PlatformServices.getBlockEntityHelper().getTownName(townDataProvider);
                
                // Update through platform services
                PlatformServices.getBlockEntityHelper().setTownName(townDataProvider, trimmedName);
                PlatformServices.getBlockEntityHelper().markTownDataDirty(townDataProvider);
                PlatformServices.getBlockEntityHelper().syncTownData(blockEntity);
                
                // Force block update through platform services
                PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
                
                // Clear client-side caches through platform services
                PlatformServices.getPlatformHelper().clearClientCaches();
                
                LOGGER.debug("Town renamed successfully from '{}' to '{}'", oldName, trimmedName);
                
                // Send confirmation message
                String confirmMessage = "Town renamed to: " + trimmedName;
                PlatformServices.getPlatformHelper().sendPlayerMessage(player, confirmMessage, "GREEN");
                
            } else {
                LOGGER.warn("No town data provider found at position ({}, {}, {})", x, y, z);
            }
        } else {
            LOGGER.warn("No block entity found at position ({}, {}, {}) for town name change", x, y, z);
        }
    }
    
    /**
     * Platform-agnostic town name validation.
     * Returns null if valid, error message if invalid.
     */
    private String validateTownName(String name) {
        if (name.isEmpty()) {
            return "Town name cannot be empty";
        }
        
        if (name.length() > 30) {
            return "Town name cannot exceed 30 characters";
        }
        
        // Add more validation rules as needed
        return null;
    }
    
    /**
     * Get the new town name for this packet.
     */
    public String getNewName() {
        return newName;
    }
}