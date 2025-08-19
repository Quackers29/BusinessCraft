package com.quackers29.businesscraft.network.packets.misc;

import com.quackers29.businesscraft.platform.PlatformServices;
// TownInterfaceEntity access through BlockEntityHelper platform services
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic base class for packets that interact with a TownInterfaceEntity.
 * This class provides common functionality while using platform services for networking.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public abstract class BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBlockEntityPacket.class);
    
    // Position data (platform-agnostic)
    protected final int x, y, z;
    
    public BaseBlockEntityPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * Decode constructor for network deserialization.
     * Platform implementations call this with decoded coordinates.
     */
    public BaseBlockEntityPacket(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        this.x = pos[0];
        this.y = pos[1]; 
        this.z = pos[2];
    }
    
    /**
     * Get the block position coordinates.
     * Platform modules can convert this to their specific BlockPos type.
     */
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    
    /**
     * Encode packet data for network transmission.
     * Default implementation writes the BlockPos using platform services.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
    }
    
    /**
     * Get the block entity at the packet coordinates through platform services.
     * Enhanced MultiLoader approach: Use platform services for cross-platform compatibility.
     * 
     * @param player Platform-specific player object
     * @return Block entity at coordinates, or null if not found
     */
    protected Object getBlockEntity(Object player) {
        return PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
    }
    
    /**
     * Get the town data provider from a block entity through platform services.
     * 
     * @param blockEntity Block entity to get town data from
     * @return Town data provider, or null if not applicable
     */
    protected Object getTownDataProvider(Object blockEntity) {
        return PlatformServices.getBlockEntityHelper().getTownDataProvider(blockEntity);
    }
    
    /**
     * Mark the town data as changed and sync through platform services.
     * Enhanced MultiLoader approach: Use platform services for cross-platform compatibility.
     * 
     * @param townDataProvider The town data provider to mark changed
     */
    protected void markTownDataDirty(Object townDataProvider) {
        if (townDataProvider != null) {
            PlatformServices.getBlockEntityHelper().markTownDataDirty(townDataProvider);
        }
    }
    
    /**
     * Handle the packet on the server side.
     * Subclasses implement specific packet logic.
     * Platform modules provide the player object through platform services.
     * 
     * @param player Platform-specific player object
     */
    public abstract void handle(Object player);
}