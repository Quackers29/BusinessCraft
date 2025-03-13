package com.yourdomain.businesscraft.network;

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
import com.yourdomain.businesscraft.screen.TownBlockScreen;
import com.yourdomain.businesscraft.screen.PlatformManagementScreen;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;

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
                LOGGER.debug("Received platform refresh packet for block at {}", pos);
                
                // If the TownBlockScreen is open, refresh it
                if (minecraft.screen instanceof TownBlockScreen townScreen) {
                    // Check if this screen is for the same block
                    if (townScreen.getBlockPos().equals(pos)) {
                        LOGGER.debug("Refreshing platforms tab in TownBlockScreen");
                        townScreen.refreshPlatforms();
                    }
                }
                // If the PlatformManagementScreen is open, refresh it by reopening
                else if (minecraft.screen instanceof PlatformManagementScreen) {
                    LOGGER.debug("Reopening platform management screen with fresh data");
                    PlatformManagementScreen.open(pos);
                }
                // If the TownInterfaceScreen is open, refresh it
                else if (minecraft.screen instanceof TownInterfaceScreen townInterface) {
                    LOGGER.debug("Refreshing platform data in TownInterfaceScreen");
                    // The screen will get fresh data from BlockEntity next time it's needed
                }
            }
        }
    }
} 