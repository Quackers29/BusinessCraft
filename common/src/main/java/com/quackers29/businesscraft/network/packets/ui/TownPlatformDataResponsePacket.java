package com.quackers29.businesscraft.network.packets.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * Packet sent from server to client containing detailed platform data for a
 * specific town.
 * This includes platform positions, paths, and enabled destinations.
 */
public class TownPlatformDataResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownPlatformDataResponsePacket.class);
    private static final int MAX_STRING_LENGTH = 32767;

    private final UUID townId;
    private final Map<UUID, PlatformInfo> platforms = new java.util.LinkedHashMap<>();
    private TownInfo townInfo;

    public TownPlatformDataResponsePacket(UUID townId) {
        this.townId = townId;
    }

    public void addPlatform(UUID platformId, String name, boolean enabled,
            BlockPos startPos, BlockPos endPos, Set<UUID> enabledDestinations,
            boolean legacy, BlockPos interfacePos) {
        platforms.put(platformId, new PlatformInfo(platformId, name, enabled, startPos, endPos, enabledDestinations,
                legacy, interfacePos));
    }

    public void setTownInfo(String name, int population, int touristCount, int boundaryRadius) {
        this.townInfo = new TownInfo(name, population, touristCount, boundaryRadius);
    }

    public UUID getTownId() {
        return townId;
    }

    public Map<UUID, PlatformInfo> getPlatforms() {
        return platforms;
    }

    public TownInfo getTownInfo() {
        return townInfo;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(townId);

        buf.writeBoolean(townInfo != null);
        if (townInfo != null) {
            buf.writeUtf(townInfo.name, MAX_STRING_LENGTH);
            buf.writeInt(townInfo.population);
            buf.writeInt(townInfo.touristCount);
            buf.writeInt(townInfo.boundaryRadius);
        }

        buf.writeInt(platforms.size());

        for (PlatformInfo platform : platforms.values()) {
            buf.writeUUID(platform.id);
            buf.writeUtf(platform.name, MAX_STRING_LENGTH);
            buf.writeBoolean(platform.enabled);

            buf.writeBoolean(platform.startPos != null);
            if (platform.startPos != null) {
                buf.writeBlockPos(platform.startPos);
            }

            buf.writeBoolean(platform.endPos != null);
            if (platform.endPos != null) {
                buf.writeBlockPos(platform.endPos);
            }

            buf.writeInt(platform.enabledDestinations.size());
            for (UUID destId : platform.enabledDestinations) {
                buf.writeUUID(destId);
            }

            buf.writeBoolean(platform.legacy);
            buf.writeBoolean(platform.interfacePos != null);
            if (platform.interfacePos != null) {
                buf.writeBlockPos(platform.interfacePos);
            }
        }
    }

    public static TownPlatformDataResponsePacket decode(FriendlyByteBuf buf) {
        UUID townId = buf.readUUID();
        TownPlatformDataResponsePacket packet = new TownPlatformDataResponsePacket(townId);

        boolean hasTownInfo = buf.readBoolean();
        if (hasTownInfo) {
            String townName = buf.readUtf(MAX_STRING_LENGTH);
            int population = buf.readInt();
            int touristCount = buf.readInt();
            int boundaryRadius = buf.readInt();
            packet.setTownInfo(townName, population, touristCount, boundaryRadius);
        }

        int platformCount = buf.readInt();
        for (int i = 0; i < platformCount; i++) {
            UUID platformId = buf.readUUID();
            String name = buf.readUtf(MAX_STRING_LENGTH);
            boolean enabled = buf.readBoolean();

            boolean hasStartPos = buf.readBoolean();
            BlockPos startPos = hasStartPos ? buf.readBlockPos() : null;

            boolean hasEndPos = buf.readBoolean();
            BlockPos endPos = hasEndPos ? buf.readBlockPos() : null;

            int destCount = buf.readInt();
            Set<UUID> enabledDestinations = new java.util.HashSet<>();
            for (int j = 0; j < destCount; j++) {
                enabledDestinations.add(buf.readUUID());
            }

            boolean legacy = buf.readBoolean();
            boolean hasInterface = buf.readBoolean();
            BlockPos interfacePos = hasInterface ? buf.readBlockPos() : null;

            packet.addPlatform(platformId, name, enabled, startPos, endPos, enabledDestinations, legacy, interfacePos);
        }

        return packet;
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () ->
                com.quackers29.businesscraft.network.ClientPacketDispatcher.dispatch(this));
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    public static class PlatformInfo {
        public final UUID id;
        public final String name;
        public final boolean enabled;
        public final BlockPos startPos;
        public final BlockPos endPos;
        public final Set<UUID> enabledDestinations;
        public final boolean legacy;
        public final BlockPos interfacePos;

        public PlatformInfo(UUID id, String name, boolean enabled, BlockPos startPos, BlockPos endPos,
                Set<UUID> enabledDestinations, boolean legacy, BlockPos interfacePos) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.startPos = startPos;
            this.endPos = endPos;
            this.enabledDestinations = new java.util.HashSet<>(enabledDestinations);
            this.legacy = legacy;
            this.interfacePos = interfacePos;
        }
    }

    public static class TownInfo {
        public final String name;
        public final int population;
        public final int touristCount;
        public final int boundaryRadius;

        public TownInfo(String name, int population, int touristCount, int boundaryRadius) {
            this.name = name;
            this.population = population;
            this.touristCount = touristCount;
            this.boundaryRadius = boundaryRadius;
        }
    }
}
