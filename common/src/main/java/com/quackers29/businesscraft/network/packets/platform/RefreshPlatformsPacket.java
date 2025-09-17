package com.quackers29.businesscraft.network.packets.platform;

import java.util.UUID;
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

    // Static decode method for Forge network registration
    public static RefreshPlatformsPacket decode(FriendlyByteBuf buf) {
        return new RefreshPlatformsPacket(buf);
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
        try {
            Minecraft minecraft = Minecraft.getInstance();

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                "Processing RefreshPlatformsPacket for pos {}", pos);

            // Get the block entity
            if (minecraft.level != null) {
                BlockEntity be = minecraft.level.getBlockEntity(pos);

                if (be instanceof TownInterfaceEntity townInterfaceEntity) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Found TownInterfaceEntity at {}", pos);

                    // Clear platform cache for this town to force fresh data on next map view
                    UUID townId = townInterfaceEntity.getTownId();
                    if (townId != null) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Clearing platform cache for town {}", townId);
                        ClientTownMapCache.getInstance().clearTownPlatformData(townId);
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Platform cache cleared successfully");
                    } else {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Town ID is null, skipping cache clear");
                    }

                    // If the PlatformManagementScreenV2 is open, refresh data without reopening
                    if (minecraft.screen instanceof PlatformManagementScreenV2 platformScreen) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "PlatformManagementScreenV2 is open, refreshing data");
                        platformScreen.refreshPlatformData();
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Platform screen data refreshed successfully");
                    } else {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "PlatformManagementScreenV2 is not open, skipping refresh");
                    }
                    // Additional handling for other screens can be added here if needed
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Block entity at {} is not TownInterfaceEntity, is: {}", pos, be != null ? be.getClass().getSimpleName() : "null");
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Minecraft level is null");
            }
        } catch (Exception e) {
            LOGGER.error("Error processing RefreshPlatformsPacket", e);
            // Don't crash the client, but log the error
        }
    }
} 
