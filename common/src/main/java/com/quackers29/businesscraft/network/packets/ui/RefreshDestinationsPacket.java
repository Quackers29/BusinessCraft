package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.PlatformLearnedRoutes;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Packet sent from server to client to refresh the destinations UI
 */
public class RefreshDestinationsPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshDestinationsPacket.class);
    private final BlockPos pos;
    private final UUID platformId;
    private final Map<UUID, String> townNames = new HashMap<>();
    private final Map<UUID, Boolean> enabledState = new HashMap<>();
    private final Map<UUID, Integer> townDistances = new HashMap<>();
    private final Map<UUID, String> townDirections = new HashMap<>();
    private DestinationMode destinationMode = DestinationMode.AUTO;
    private final List<LearnedRouteEntry> learnedRoutes = new ArrayList<>();

    public record LearnedRouteEntry(UUID townId, String name, float strength) {
    }

    public RefreshDestinationsPacket(BlockPos pos, UUID platformId) {
        this.pos = pos;
        this.platformId = platformId;
    }

    public RefreshDestinationsPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.platformId = buf.readUUID();
        this.destinationMode = DestinationMode.fromConfig(buf.readUtf(), DestinationMode.AUTO);

        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            boolean enabled = buf.readBoolean();
            int distance = buf.readInt();
            String direction = buf.readUtf();
            townNames.put(id, name);
            enabledState.put(id, enabled);
            townDistances.put(id, distance);
            townDirections.put(id, direction);
        }

        int learnedSize = buf.readInt();
        for (int i = 0; i < learnedSize; i++) {
            learnedRoutes.add(new LearnedRouteEntry(buf.readUUID(), buf.readUtf(), buf.readFloat()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(platformId);
        buf.writeUtf(destinationMode.toConfigValue());

        buf.writeInt(townNames.size());
        for (Map.Entry<UUID, String> entry : townNames.entrySet()) {
            UUID townId = entry.getKey();
            buf.writeUUID(townId);
            buf.writeUtf(entry.getValue());
            buf.writeBoolean(enabledState.getOrDefault(townId, false));
            buf.writeInt(townDistances.getOrDefault(townId, 0));
            buf.writeUtf(townDirections.getOrDefault(townId, ""));
        }

        buf.writeInt(learnedRoutes.size());
        for (LearnedRouteEntry route : learnedRoutes) {
            buf.writeUUID(route.townId());
            buf.writeUtf(route.name());
            buf.writeFloat(route.strength());
        }
    }

    public static RefreshDestinationsPacket decode(FriendlyByteBuf buf) {
        return new RefreshDestinationsPacket(buf);
    }

    public void addTown(UUID townId, String name, boolean enabled, int distance, String direction) {
        townNames.put(townId, name);
        enabledState.put(townId, enabled);
        townDistances.put(townId, distance);
        townDirections.put(townId, direction);
    }

    public void setDestinationMode(DestinationMode mode) {
        if (mode != null) {
            this.destinationMode = mode;
        }
    }

    public void addLearnedRoute(UUID townId, String name, float strength) {
        learnedRoutes.add(new LearnedRouteEntry(townId, name, strength));
    }

    public static void sendForPlatform(ServerPlayer player, BlockPos townCentrePos, UUID platformId,
            TownInterfaceEntity townInterface, Platform platform) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RefreshDestinationsPacket responsePacket = new RefreshDestinationsPacket(townCentrePos, platformId);
        responsePacket.setDestinationMode(platform.getDestinationMode());

        Town originTown = townInterface.getTown();
        BlockPos originPos = originTown != null && originTown.getPosition() != null
                ? originTown.getPosition()
                : townCentrePos;

        long tick = serverLevel.getGameTime();
        platform.getLearnedRoutes().applyDecay(tick, ConfigLoader.touristAutoScoreHalfLifeTicks,
                ConfigLoader.touristAutoFadeTripMultiplier, ConfigLoader.touristAutoPruneFloor);

        UUID originId = originTown != null ? originTown.getId() : null;
        var valid = new java.util.HashSet<>(TownManager.get(serverLevel).getAllTowns().keySet());
        double total = platform.getLearnedRoutes().totalScoreFor(valid, originId);
        int show = Math.min(5, ConfigLoader.touristAutoMaxLearnedTowns);
        for (var entry : platform.getLearnedRoutes().topRoutes(show, valid, originId)) {
            UUID destId = entry.getKey();
            double score = entry.getValue();
            Town dest = TownManager.get(serverLevel).getTown(destId);
            String name = dest != null ? dest.getName() : "Unknown";
            float strength = total > 0 ? (float) (score / total) : 0f;
            responsePacket.addLearnedRoute(destId, name, strength);
        }

        townInterface.getAllTownsForDestination(serverLevel).forEach((id, townName) -> {
            if (originTown != null && id.equals(originTown.getId())) {
                return;
            }
            boolean isEnabled = platform.isDestinationEnabled(id);
            int distance = 100;
            String direction = "";
            Town destTown = TownManager.get(serverLevel).getTown(id);
            if (destTown != null && destTown.getPosition() != null) {
                BlockPos destPos = destTown.getPosition();
                double dx = destPos.getX() - originPos.getX();
                double dy = destPos.getY() - originPos.getY();
                double dz = destPos.getZ() - originPos.getZ();
                distance = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance <= 1) {
                    return;
                }
                direction = calculateDirection(dx, dz);
            }
            responsePacket.addTown(id, townName, isEnabled, distance, direction);
        });

        PlatformAccess.getNetworkMessages().sendToPlayer(responsePacket, player);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper == null) {
                LOGGER.warn("ClientHelper not available (server side?)");
                return;
            }

            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof net.minecraft.world.level.Level level) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);

                if (be instanceof TownInterfaceEntity townInterface) {
                    Platform platform = null;
                    for (Platform p : townInterface.getPlatforms()) {
                        if (p.getId().equals(platformId)) {
                            platform = p;
                            break;
                        }
                    }

                    if (platform != null) {
                        clientHelper.openDestinationsScreen(
                                pos,
                                platformId,
                                platform.getName(),
                                townNames,
                                enabledState,
                                townDistances,
                                townDirections,
                                destinationMode,
                                learnedRoutes);
                    }
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    private static String calculateDirection(double dx, double dz) {
        if (dx == 0 && dz == 0) {
            return "";
        }
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        if (angle < 0) {
            angle += 360;
        }
        if (angle >= 337.5 || angle < 22.5) {
            return "E";
        }
        if (angle >= 22.5 && angle < 67.5) {
            return "SE";
        }
        if (angle >= 67.5 && angle < 112.5) {
            return "S";
        }
        if (angle >= 112.5 && angle < 157.5) {
            return "SW";
        }
        if (angle >= 157.5 && angle < 202.5) {
            return "W";
        }
        if (angle >= 202.5 && angle < 247.5) {
            return "NW";
        }
        if (angle >= 247.5 && angle < 292.5) {
            return "N";
        }
        return "NE";
    }
}
