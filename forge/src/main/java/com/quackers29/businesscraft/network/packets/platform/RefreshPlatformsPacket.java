package com.quackers29.businesscraft.network.packets.platform;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.ui.screens.platform.PlatformManagementScreenV2;
import com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet sent from server to client to refresh platform data
 */
public class RefreshPlatformsPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos pos;
    
    public RefreshPlatformsPacket(BlockPos pos) {
        this.pos = pos;
    }
    
    public RefreshPlatformsPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // This code runs on the client
            handleClient();
        });
        
        return true;
    }
    
    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Get the block entity
        if (minecraft.level != null) {
            BlockEntity be = minecraft.level.getBlockEntity(pos);
            
            if (be instanceof TownInterfaceEntity townInterfaceEntity) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Received platform refresh packet for block at {}", pos);
                
                // Clear platform cache for this town to force fresh data on next map view
                if (townInterfaceEntity.getTownId() != null) {
                    ClientTownMapCache.getInstance().clearTownPlatformData(townInterfaceEntity.getTownId());
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Cleared platform cache for town {}", townInterfaceEntity.getTownId());
                }
                
                // If the PlatformManagementScreenV2 is open, refresh data without reopening
                if (minecraft.screen instanceof PlatformManagementScreenV2 platformScreen) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Refreshing platform management screen data");
                    platformScreen.refreshPlatformData();
                }
                // Additional handling for other screens can be added here if needed
            }
        }
    }
} 