package com.quackers29.businesscraft.network.packets.ui;

import java.util.UUID;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet for requesting town map data from the server.
 * This is sent by the client when opening the town map modal.
 */
public class RequestTownMapDataPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTownMapDataPacket.class);
    
    public RequestTownMapDataPacket() {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static RequestTownMapDataPacket decode(FriendlyByteBuf buf) {
        return new RequestTownMapDataPacket();
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            try {
                Object senderObj = PlatformAccess.getNetwork().getSender(context);
                if (!(senderObj instanceof ServerPlayer player)) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Received RequestTownMapDataPacket from null player");
                    return;
                }

                ServerLevel serverLevel = (ServerLevel) player.level();
                TownManager townManager = TownManager.get(serverLevel);

                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Processing town map data request for player: {}", player.getName().getString());

                Map<UUID, Town> allTowns = townManager.getAllTowns();
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Server has {} towns to send", allTowns.size());

                // No boundary calculation here - that's done per-town on click
                TownMapDataResponsePacket responsePacket = new TownMapDataResponsePacket();
                for (Map.Entry<UUID, Town> entry : allTowns.entrySet()) {
                    Town town = entry.getValue();
                    responsePacket.addTown(
                        town.getId(),
                        town.getName(),
                        town.getPosition(),
                        (int) town.getPopulation(),
                        (int) town.getTouristCount()
                    );
                }

                PlatformAccess.getNetworkMessages().sendToPlayer(responsePacket, player);

                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                    "Sent town map data response with {} towns to player: {}",
                    allTowns.size(), player.getName().getString());

            } catch (Exception e) {
                LOGGER.error("Error handling RequestTownMapDataPacket", e);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
