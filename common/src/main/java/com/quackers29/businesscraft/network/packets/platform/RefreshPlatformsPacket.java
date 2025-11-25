package com.quackers29.businesscraft.network.packets.platform;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.quackers29.businesscraft.api.PlatformAccess;
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

    /**
     * Serialize packet data for Fabric networking (S2C)
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    // Static decode method for Forge network registration
    public static RefreshPlatformsPacket decode(FriendlyByteBuf buf) {
        return new RefreshPlatformsPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // This code runs on the client
            handleClient();
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }

    private void handleClient() {
        try {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper == null) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "ClientHelper not available (server side?)");
                return;
            }

            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Processing RefreshPlatformsPacket for pos {}", pos);

            // Get the block entity
            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof net.minecraft.world.level.Level level) {
                BlockEntity be = level.getBlockEntity(pos);

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

                        // Request fresh platform data from server
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                                "Requesting fresh platform data from server for town {}", townId);
                        PlatformAccess.getNetworkMessages().sendToServer(
                                new com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket(
                                        townId));
                    } else {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                                "Town ID is null, skipping cache clear");
                    }

                    // If the PlatformManagementScreenV2 is open, it will be updated when the
                    // response packet arrives
                    Object currentScreen = clientHelper.getCurrentScreen();
                    if (currentScreen instanceof PlatformManagementScreenV2) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                                "PlatformManagementScreenV2 is open, fresh data will be applied when response arrives");
                    }
                    // Additional handling for other screens can be added here if needed
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Block entity at {} is not TownInterfaceEntity, is: {}", pos,
                            be != null ? be.getClass().getSimpleName() : "null");
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Client level is null");
            }
        } catch (Exception e) {
            LOGGER.error("Error processing RefreshPlatformsPacket", e);
            // Don't crash the client, but log the error
        }
    }
}
