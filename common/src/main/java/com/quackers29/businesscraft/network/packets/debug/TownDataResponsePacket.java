package com.quackers29.businesscraft.network.packets.debug;

import net.minecraft.network.FriendlyByteBuf;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.client.TownDebugOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Response packet for town debug data (server -> client)
 */
public class TownDataResponsePacket {
    private final List<TownDebugOverlay.TownDebugData> townData;

    public TownDataResponsePacket(List<TownDebugOverlay.TownDebugData> townData) {
        this.townData = townData != null ? new ArrayList<>(townData) : new ArrayList<>();
    }

    public TownDataResponsePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        townData = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            String name = buf.readUtf();
            String position = buf.readUtf();
            int population = buf.readInt();
            int breadCount = buf.readInt();
            boolean touristSpawningEnabled = buf.readBoolean();
            boolean canSpawnTourists = buf.readBoolean();
            String pathStart = buf.readBoolean() ? buf.readUtf() : null;
            String pathEnd = buf.readBoolean() ? buf.readUtf() : null;
            int searchRadius = buf.readInt();
            int totalVisitors = buf.readInt();

            townData.add(new TownDebugOverlay.TownDebugData(
                id, name, position, population, breadCount,
                touristSpawningEnabled, canSpawnTourists,
                pathStart, pathEnd, searchRadius, totalVisitors
            ));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(townData.size());

        for (TownDebugOverlay.TownDebugData data : townData) {
            buf.writeUtf(data.id);
            buf.writeUtf(data.name);
            buf.writeUtf(data.position);
            buf.writeInt(data.population);
            buf.writeInt(data.breadCount);
            buf.writeBoolean(data.touristSpawningEnabled);
            buf.writeBoolean(data.canSpawnTourists);
            buf.writeBoolean(data.pathStart != null);
            if (data.pathStart != null) buf.writeUtf(data.pathStart);
            buf.writeBoolean(data.pathEnd != null);
            if (data.pathEnd != null) buf.writeUtf(data.pathEnd);
            buf.writeInt(data.searchRadius);
            buf.writeInt(data.totalVisitors);
        }
    }

    public static TownDataResponsePacket decode(FriendlyByteBuf buf) {
        return new TownDataResponsePacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Update the overlay with the received data
            TownDebugOverlay.setTownData(townData);
            // No notification message - silently update the data
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}

