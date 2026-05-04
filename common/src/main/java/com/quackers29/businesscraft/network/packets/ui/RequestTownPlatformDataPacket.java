package com.quackers29.businesscraft.network.packets.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;

import java.util.UUID;
import java.util.List;

/**
 * Packet sent from client to server to request detailed platform data for a
 * specific town.
 * This is sent when the user clicks on a town in the map view.
 */
public class RequestTownPlatformDataPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTownPlatformDataPacket.class);

    private final UUID townId;

    public RequestTownPlatformDataPacket(UUID townId) {
        this.townId = townId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(townId);
    }

    public static RequestTownPlatformDataPacket decode(FriendlyByteBuf buf) {
        UUID townId = buf.readUUID();
        return new RequestTownPlatformDataPacket(townId);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            try {
                Object senderObj = PlatformAccess.getNetwork().getSender(context);
                if (!(senderObj instanceof ServerPlayer player)) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Received RequestTownPlatformDataPacket with null sender");
                    return;
                }

                ServerLevel level = player.serverLevel();
                TownManager townManager = TownManager.get(level);
                Town town = townManager.getTown(townId);

                if (town == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Town not found for platform data request: {}", townId);
                    return;
                }

                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Processing platform data request for town: {} ({})", town.getName(), townId);

                if (level.getBlockEntity(town.getPosition()) instanceof TownInterfaceEntity townEntity) {
                    List<Platform> platforms = townEntity.getPlatforms();

                    TownPlatformDataResponsePacket response = new TownPlatformDataResponsePacket(townId);

                    // Get live boundary radius from town (single source of truth)
                    int boundaryRadius = town.getBoundaryRadius();
                    response.setTownInfo(town.getName(), (int) town.getPopulation(), (int) town.getTouristCount(), boundaryRadius);

                    for (Platform platform : platforms) {
                        response.addPlatform(
                                platform.getId(),
                                platform.getName(),
                                platform.isEnabled(),
                                platform.getStartPos(),
                                platform.getEndPos(),
                                platform.getEnabledDestinations());
                    }

                    PlatformAccess.getNetworkMessages().sendToPlayer(response, player);

                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Sent platform data response for town {} with {} platforms",
                            town.getName(), platforms.size());
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "No town block entity found at position {} for town {}",
                            town.getPosition(), town.getName());
                }

            } catch (Exception e) {
                LOGGER.error("Error handling RequestTownPlatformDataPacket", e);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
