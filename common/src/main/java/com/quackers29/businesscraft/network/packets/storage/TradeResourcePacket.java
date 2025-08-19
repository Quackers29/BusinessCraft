package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for trading resources with towns.
 * Extends BaseBlockEntityPacket to handle block entity operations.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class TradeResourcePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeResourcePacket.class);
    private final Object itemToTrade;
    private final int slotId;
    
    /**
     * Create packet for sending.
     */
    public TradeResourcePacket(int x, int y, int z, Object itemToTrade, int slotId) {
        super(x, y, z);
        this.itemToTrade = itemToTrade;
        this.slotId = slotId;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static TradeResourcePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        Object itemStack = PlatformServices.getNetworkHelper().readItemStack(buffer);
        int slotId = PlatformServices.getNetworkHelper().readInt(buffer);
        return new TradeResourcePacket(pos[0], pos[1], pos[2], itemStack, slotId);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        super.encode(buffer);
        PlatformServices.getNetworkHelper().writeItemStack(buffer, itemToTrade);
        PlatformServices.getNetworkHelper().writeInt(buffer, slotId);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Player is trading resource in slot {} at [{}, {}, {}]", slotId, getX(), getY(), getZ());
        
        // Check if the item to trade is empty using direct access (Unified Architecture)
        if (!(itemToTrade instanceof ItemStack itemStack) || itemStack.isEmpty()) {
            LOGGER.warn("Received empty item in trade packet from player");
            return;
        }
        
        // Get the town interface block entity using platform services
        Object blockEntity = getBlockEntity(player);
        if (blockEntity == null) {
            LOGGER.warn("No block entity found at position [{}, {}, {}] for player", getX(), getY(), getZ());
            return;
        }

        Object townDataProvider = getTownDataProvider(blockEntity);
        if (townDataProvider == null) {
            LOGGER.warn("No town block entity found at position [{}, {}, {}] for player", getX(), getY(), getZ());
            return;
        }
        
        // Process the resource trade through platform services (complex inventory operations still need platform layer)
        Object paymentResult = PlatformServices.getBlockEntityHelper().processResourceTrade(
            townDataProvider, player, itemToTrade, slotId);
        
        // Send the payment result back to the client
        if (paymentResult != null) {
            PlatformServices.getNetworkHelper().sendPaymentResultPacket(player, paymentResult);
        }
        
        // Mark changed and sync using platform services
        markTownDataDirty(townDataProvider);
        
        // Force block update to sync changes
        PlatformServices.getPlatformHelper().forceBlockUpdate(player, getX(), getY(), getZ());
        
        LOGGER.debug("Successfully processed resource trade in slot {} at [{}, {}, {}]", 
                    slotId, getX(), getY(), getZ());
    }
    
    // Getters for testing
    public Object getItemToTrade() { return itemToTrade; }
    public int getSlotId() { return slotId; }
}