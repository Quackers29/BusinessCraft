package com.quackers29.businesscraft.network.packets.town;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;
import java.util.UUID;

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
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing SetTownNamePacket for position ({}, {}, {}) with new name: '{}'", x, y, z, newName);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Core business logic - validate the new name
            String trimmedName = newName.trim();
            
            // Validation logic (platform-agnostic)
            String validationError = validateTownName(trimmedName);
            if (validationError != null) {
                // Send error message through platform services
                PlatformServices.getPlatformHelper().sendPlayerMessage(player, validationError, "RED");
                return;
            }
            
            String oldName = townData.getTownName();
            
            // Direct update using unified architecture - no abstraction layer needed
            townData.setTownName(trimmedName);
            
            // CRITICAL: Also update the actual Town object in TownManager
            UUID townId = townData.getTownId();
            if (townId != null) {
                // Get the level from the player (platform-specific player casting)
                try {
                    // Cast player to ServerPlayer to get the level
                    Object level = null;
                    if (player.getClass().getName().contains("ServerPlayer")) {
                        java.lang.reflect.Method getLevelMethod = player.getClass().getMethod("serverLevel");
                        level = getLevelMethod.invoke(player);
                    }
                    
                    if (level != null) {
                        // Get the town object and update its name
                        Object town = PlatformServices.getTownManagerService().getTown(level, townId);
                        if (town != null) {
                            // Use reflection to update the town name
                            java.lang.reflect.Method setNameMethod = town.getClass().getMethod("setName", String.class);
                            setNameMethod.invoke(town, trimmedName);
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Updated actual Town object name to '{}'", trimmedName);
                            
                            // Mark the town manager data as dirty for saving
                            PlatformServices.getTownManagerService().markDirty(level);
                        } else {
                            LOGGER.warn("Town object not found for ID: {}", townId);
                        }
                    } else {
                        LOGGER.warn("Could not get level from player for town name update");
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to update Town object name: {}", e.getMessage(), e);
                }
            }
            
            townData.markDirty(); // Direct dirty marking
            
            // Platform-specific operations still use platform services
            PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
            PlatformServices.getPlatformHelper().clearClientCaches();
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Town renamed successfully from '{}' to '{}'", oldName, trimmedName);
            
            // Send confirmation message
            String confirmMessage = "Town renamed to: " + trimmedName;
            PlatformServices.getPlatformHelper().sendPlayerMessage(player, confirmMessage, "GREEN");
            
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for town name change", x, y, z);
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