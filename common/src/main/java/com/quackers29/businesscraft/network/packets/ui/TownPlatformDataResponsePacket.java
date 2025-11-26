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

    /**
     * Add platform data to the packet
     */
    public void addPlatform(UUID platformId, String name, boolean enabled,
            BlockPos startPos, BlockPos endPos, Set<UUID> enabledDestinations) {
        platforms.put(platformId, new PlatformInfo(platformId, name, enabled, startPos, endPos, enabledDestinations));
    }

    /**
     * Set town information
     */
    public void setTownInfo(String name, int population, int touristCount, int boundaryRadius) {
        this.townInfo = new TownInfo(name, population, touristCount, boundaryRadius);
    }

    /**
     * Get the town ID this data is for
     */
    public UUID getTownId() {
        return townId;
    }

    /**
     * Get all platform data
     */
    public Map<UUID, PlatformInfo> getPlatforms() {
        return platforms;
    }

    /**
     * Get town information
     */
    public TownInfo getTownInfo() {
        return townInfo;
    }

    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(townId);

        // Write town info
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

            // Nullable startPos
            buf.writeBoolean(platform.startPos != null);
            if (platform.startPos != null) {
                buf.writeBlockPos(platform.startPos);
            }

            // Nullable endPos
            buf.writeBoolean(platform.endPos != null);
            if (platform.endPos != null) {
                buf.writeBlockPos(platform.endPos);
            }

            // Write enabled destinations
            buf.writeInt(platform.enabledDestinations.size());
            for (UUID destId : platform.enabledDestinations) {
                buf.writeUUID(destId);
            }
        }
    }

    /**
     * Serialize packet data for Fabric networking (S2C)
     */
    public void toBytes(FriendlyByteBuf buf) {
        encode(buf);
    }

    /**
     * Decode the packet data from the buffer
     */
    public static TownPlatformDataResponsePacket decode(FriendlyByteBuf buf) {
        UUID townId = buf.readUUID();
        TownPlatformDataResponsePacket packet = new TownPlatformDataResponsePacket(townId);

        // Read town info
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

            // Nullable startPos
            boolean hasStartPos = buf.readBoolean();
            BlockPos startPos = hasStartPos ? buf.readBlockPos() : null;

            // Nullable endPos
            boolean hasEndPos = buf.readBoolean();
            BlockPos endPos = hasEndPos ? buf.readBlockPos() : null;

            // Read enabled destinations
            int destCount = buf.readInt();
            Set<UUID> enabledDestinations = new java.util.HashSet<>();
            for (int j = 0; j < destCount; j++) {
                enabledDestinations.add(buf.readUUID());
            }

            packet.addPlatform(platformId, name, enabled, startPos, endPos, enabledDestinations);
        }

        return packet;
    }

    /**
     * Handle the packet when received on the client
     */
    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            try {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Received platform data response for town {} with {} platforms",
                        townId, platforms.size());

                // Update the client-side town map cache with platform data
                ClientTownMapCache.getInstance().updateTownPlatformData(townId, platforms);

                // Also update town information in cache if available
                if (townInfo != null) {
                    ClientTownMapCache.getInstance().updateTownInfo(townId, townInfo.name, townInfo.population,
                            townInfo.touristCount);
                }

                // CRITICAL FIX: Also update the TownInterfaceEntity's platform manager
                // Find the town block entity and sync the platform data to it
                com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
                if (clientHelper != null) {
                    Object levelObj = clientHelper.getClientLevel();
                    if (levelObj instanceof net.minecraft.world.level.Level level) {
                        // Search for TownInterfaceEntity with this townId
                        // This is a bit expensive but necessary for visualization to work
                        BlockPos foundPos = findTownBlockPos(level, townId);
                        if (foundPos != null) {
                            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(foundPos);
                            if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity townEntity) {
                                // Build NBT tag with platform data
                                net.minecraft.nbt.CompoundTag platformTag = new net.minecraft.nbt.CompoundTag();
                                net.minecraft.nbt.ListTag platformsTag = new net.minecraft.nbt.ListTag();

                                for (PlatformInfo platform : platforms.values()) {
                                    net.minecraft.nbt.CompoundTag pTag = new net.minecraft.nbt.CompoundTag();
                                    pTag.putUUID("Id", platform.id); // Capital I
                                    pTag.putString("Name", platform.name); // Capital N
                                    pTag.putBoolean("Enabled", platform.enabled); // Capital E

                                    // Use StartX/Y/Z and EndX/Y/Z format instead of long
                                    if (platform.startPos != null) {
                                        pTag.putInt("StartX", platform.startPos.getX());
                                        pTag.putInt("StartY", platform.startPos.getY());
                                        pTag.putInt("StartZ", platform.startPos.getZ());
                                    }
                                    if (platform.endPos != null) {
                                        pTag.putInt("EndX", platform.endPos.getX());
                                        pTag.putInt("EndY", platform.endPos.getY());
                                        pTag.putInt("EndZ", platform.endPos.getZ());
                                    }

                                    // Add enabled destinations in the correct format
                                    net.minecraft.nbt.CompoundTag destTag = new net.minecraft.nbt.CompoundTag();
                                    destTag.putInt("Count", platform.enabledDestinations.size());
                                    int destIndex = 0;
                                    for (java.util.UUID destId : platform.enabledDestinations) {
                                        destTag.putUUID("Dest" + destIndex, destId);
                                        destIndex++;
                                    }
                                    pTag.put("Destinations", destTag);

                                    platformsTag.add(pTag);
                                }

                                platformTag.put("platforms", platformsTag);

                                // Update the platform manager with this data
                                townEntity.updateClientPlatformsFromPacket(platformTag);

                                LOGGER.info(
                                        "[PLATFORM] Updated TownInterfaceEntity at {} with {} platforms from packet",
                                        foundPos, platforms.size());
                            }
                        }
                    }
                }

                // Try to refresh any open town map modals
                if (clientHelper != null) {
                    Object currentScreen = clientHelper.getCurrentScreen();
                    if (currentScreen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal) {
                        mapModal.refreshPlatformData(townId, platforms);

                        // Also update town info if available
                        if (townInfo != null) {
                            mapModal.refreshTownData(townId, townInfo);
                        }

                        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS,
                                "Refreshed open town map modal with platform and town data for town {}", townId);
                    }
                }

            } catch (Exception e) {
                LOGGER.error("Error handling TownPlatformDataResponsePacket", e);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    /**
     * Helper method to find the BlockPos of a TownInterfaceEntity with the given
     * townId
     */
    private BlockPos findTownBlockPos(net.minecraft.world.level.Level level, java.util.UUID townId) {
        // Search nearby chunks for the town block
        // This is called on the client so we can't search the entire world
        // We'll search within render distance
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null)
            return null;

        Object playerObj = clientHelper.getClientPlayer();
        if (!(playerObj instanceof net.minecraft.world.entity.player.Player player))
            return null;

        BlockPos playerPos = player.blockPosition();
        int searchRadius = 128; // Search within 128 blocks

        for (int x = -searchRadius; x <= searchRadius; x += 16) {
            for (int z = -searchRadius; z <= searchRadius; z += 16) {
                BlockPos chunkPos = playerPos.offset(x, 0, z);
                net.minecraft.world.level.chunk.ChunkAccess chunk = level.getChunk(chunkPos);
                if (chunk != null) {
                    for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity townEntity) {
                            if (townId.equals(townEntity.getTownId())) {
                                return pos;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Data class for platform information
     */
    public static class PlatformInfo {
        public final UUID id;
        public final String name;
        public final boolean enabled;
        public final BlockPos startPos;
        public final BlockPos endPos;
        public final Set<UUID> enabledDestinations;

        public PlatformInfo(UUID id, String name, boolean enabled, BlockPos startPos, BlockPos endPos,
                Set<UUID> enabledDestinations) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.startPos = startPos;
            this.endPos = endPos;
            this.enabledDestinations = new java.util.HashSet<>(enabledDestinations);
        }
    }

    /**
     * Data class for town information
     */
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
