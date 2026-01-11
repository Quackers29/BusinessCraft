package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.TownResourceViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NEW server-authoritative resource sync packet implementing the "View-Model" pattern.
 * 
 * This packet REPLACES the old ResourceSyncPacket which violated the server-authoritative
 * principle by sending raw data that required client-side calculations.
 * 
 * KEY DIFFERENCES FROM OLD PACKET:
 * - Sends pre-calculated display strings instead of raw numbers
 * - Client performs ZERO calculations - only renders received data  
 * - Implements true "dumb terminal" client architecture
 * - All business logic happens server-side in TownResourceViewModelBuilder
 */
public class ResourceViewModelSyncPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceViewModelSyncPacket.class);
    
    private final BlockPos pos;
    private final TownResourceViewModel resourceViewModel;
    
    /**
     * Creates a new view-model sync packet (SERVER-SIDE)
     * @param pos Block position of the town interface
     * @param resourceViewModel Pre-calculated view-model from server
     */
    public ResourceViewModelSyncPacket(BlockPos pos, TownResourceViewModel resourceViewModel) {
        this.pos = pos;
        this.resourceViewModel = resourceViewModel;
    }
    
    /**
     * Deserializes packet from network buffer (CLIENT-SIDE)
     */
    public ResourceViewModelSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.resourceViewModel = new TownResourceViewModel(buf);
    }
    
    /**
     * Serializes packet to network buffer (SERVER-SIDE)
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        resourceViewModel.toBytes(buf);
    }
    
    public static void encode(ResourceViewModelSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    public static ResourceViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new ResourceViewModelSyncPacket(buf);
    }
    
    /**
     * Handles packet on client side - PURE DISPLAY LOGIC ONLY
     * No calculations, no business logic, just updating the display cache
     */
    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            LOGGER.debug("[CLIENT] ResourceViewModelSyncPacket received for pos {}", pos);
            
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) return;
            
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity entity) {
                // Update the client cache with pre-calculated view-model
                // NO CALCULATIONS HAPPEN HERE - client is truly a "dumb terminal"
                entity.updateResourceViewModel(resourceViewModel);
                
                // Refresh open UI if TownInterfaceScreen is active  
                if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen screen) {
                    screen.getMenu().refreshDataSlots();
                }
                
                LOGGER.debug("[CLIENT] Resource view-model updated: {} resources, status: {}", 
                    resourceViewModel.getResourceCount(), 
                    resourceViewModel.getOverallStatus());
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}