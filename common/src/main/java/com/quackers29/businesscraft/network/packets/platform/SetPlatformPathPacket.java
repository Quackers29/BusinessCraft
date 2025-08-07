package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for setting a platform's path coordinates.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class SetPlatformPathPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformPathPacket.class);
    private final int blockX, blockY, blockZ;
    private final String platformId;
    private final int startX, startY, startZ;
    private final int endX, endY, endZ;
    
    /**
     * Create packet for sending.
     */
    public SetPlatformPathPacket(int blockX, int blockY, int blockZ, String platformId,
                               int startX, int startY, int startZ,
                               int endX, int endY, int endZ) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.platformId = platformId;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static SetPlatformPathPacket decode(Object buffer) {
        int[] blockPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        int[] startPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        int[] endPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        
        return new SetPlatformPathPacket(
            blockPos[0], blockPos[1], blockPos[2], platformId,
            startPos[0], startPos[1], startPos[2],
            endPos[0], endPos[1], endPos[2]
        );
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, blockX, blockY, blockZ);
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, startX, startY, startZ);
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, endX, endY, endZ);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        LOGGER.debug("Player is setting platform {} path from [{}, {}, {}] to [{}, {}, {}] at [{}, {}, {}]", 
                    platformId, startX, startY, startZ, endX, endY, endZ, blockX, blockY, blockZ);
        
        // Get the town interface block entity through platform services
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, blockX, blockY, blockZ);
        if (blockEntity == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", blockX, blockY, blockZ);
            return;
        }
        
        // Set the platform path through platform services
        boolean success = PlatformServices.getBlockEntityHelper().setPlatformPath(
            blockEntity, platformId, startX, startY, startZ, endX, endY, endZ);
            
        if (!success) {
            LOGGER.warn("Failed to set platform {} path from [{}, {}, {}] to [{}, {}, {}] at [{}, {}, {}]", 
                       platformId, startX, startY, startZ, endX, endY, endZ, blockX, blockY, blockZ);
            return;
        }
        
        // Mark the block entity as changed and sync to client
        PlatformServices.getBlockEntityHelper().markBlockEntityChanged(blockEntity);
        PlatformServices.getPlatformHelper().forceBlockUpdate(player, blockX, blockY, blockZ);
        
        LOGGER.debug("Successfully set platform {} path from [{}, {}, {}] to [{}, {}, {}] at [{}, {}, {}]", 
                    platformId, startX, startY, startZ, endX, endY, endZ, blockX, blockY, blockZ);
    }
    
    // Getters for testing
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public String getPlatformId() { return platformId; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getStartZ() { return startZ; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }
    public int getEndZ() { return endZ; }
}