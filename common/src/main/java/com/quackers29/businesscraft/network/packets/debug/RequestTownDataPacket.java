package com.quackers29.businesscraft.network.packets.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.client.TownDebugOverlay;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request packet for town debug data (client -> server)
 */
public class RequestTownDataPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTownDataPacket.class);

    public RequestTownDataPacket() {
    }

    public static RequestTownDataPacket decode(FriendlyByteBuf buf) {
        return new RequestTownDataPacket();
    }

    public void encode(FriendlyByteBuf buf) {
        // No data to encode - empty packet
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) {
                LOGGER.warn("RequestTownDataPacket received from null player");
                return;
            }

            List<TownDebugOverlay.TownDebugData> townDataList = new ArrayList<>();

            // Gather data from all loaded levels
            for (ServerLevel level : player.getServer().getAllLevels()) {
                TownManager townManager = TownManager.get(level);
                Map<UUID, Town> towns = townManager.getAllTowns();

                // Convert Town objects to TownDebugData
                for (Map.Entry<UUID, Town> entry : towns.entrySet()) {
                    Town town = entry.getValue();
                    townDataList.add(new TownDebugOverlay.TownDebugData(
                            town.getId().toString(),
                            town.getName(),
                            town.getPosition().toShortString(),
                            town.getPopulation(),
                            0, // Removed bread count
                            town.isTouristSpawningEnabled(),
                            town.canSpawnTourists(),
                            town.getPathStart() != null ? town.getPathStart().toShortString() : null,
                            town.getPathEnd() != null ? town.getPathEnd().toShortString() : null,
                            town.getSearchRadius(),
                            town.getTotalVisitors()));
                }
            }

            // Send the data back to the client
            TownDataResponsePacket responsePacket = new TownDataResponsePacket(townDataList);
            PlatformAccess.getNetworkMessages().sendToPlayer(responsePacket, player);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
