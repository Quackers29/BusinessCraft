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
     * Get TownInterfaceData at the packet coordinates (Unified Architecture).
     * This replaces the over-abstracted BlockEntityHelper pattern with direct data access.
     * 
     * @param player Platform-specific player object
     * @return TownInterfaceData object, or null if not found
     */
    protected com.quackers29.businesscraft.town.TownInterfaceData getTownInterfaceData(Object player) {
        return PlatformServices.getPlatformHelper().getTownInterfaceData(player, x, y, z);
    }
    
    // === LEGACY METHODS (DEPRECATED - Use getTownInterfaceData() instead) ===
    
    /**
     * @deprecated Use getTownInterfaceData() for unified architecture
     */
    @Deprecated
    protected Object getBlockEntity(Object player) {
        return PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
    }
    
    /**
     * @deprecated Use getTownInterfaceData() for unified architecture
     */
    @Deprecated
    protected Object getTownDataProvider(Object blockEntity) {
        return PlatformServices.getBlockEntityHelper().getTownDataProvider(blockEntity);
    }
    
    /**
     * @deprecated Use TownInterfaceData.markDirty() for unified architecture
     */
    @Deprecated
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