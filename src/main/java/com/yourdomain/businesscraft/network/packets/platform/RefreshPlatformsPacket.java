package com.yourdomain.businesscraft.network.packets.platform;

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
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.ui.screens.platform.PlatformManagementScreen;
import com.yourdomain.businesscraft.debug.DebugConfig;

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
            
            if (be instanceof TownBlockEntity) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Received platform refresh packet for block at {}", pos);
                
                // If the PlatformManagementScreen is open, refresh it by reopening
                if (minecraft.screen instanceof PlatformManagementScreen) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Reopening platform management screen with fresh data");
                    PlatformManagementScreen.open(pos);
                }
                // Additional handling for other screens can be added here if needed
            }
        }
    }
} 