package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.PlatformLearnedRoutes;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * AUTO destination learning: spawn selection, arrival credit, expiry penalty, global purge.
 */
public final class TouristDestinationLearning {

    private static final UUID ANY_TOWN = new UUID(0, 0);

    private TouristDestinationLearning() {
    }

    public static UUID selectAutoDestination(ServerLevel level, Platform platform, UUID originTownId, Random random) {
        long tick = level.getGameTime();
        decayPlatform(platform, tick);

        Set<UUID> valid = validDestinationIds(level);
        PlatformLearnedRoutes learned = platform.getLearnedRoutes();
        double share = learned.effectiveLearnedShare(
                ConfigLoader.touristAutoMaxLearnedShare,
                ConfigLoader.touristAutoMinScoreForLearning,
                valid,
                originTownId);

        if (share > 0 && random.nextDouble() < share) {
            BlockPos originPos = townBlockPos(level, originTownId);
            UUID picked = learned.pickWeightedDestination(random, valid, originTownId, destId -> {
                if (originPos == null) {
                    return 0;
                }
                Town town = TownManager.get(level).getTown(destId);
                if (town == null || town.getPosition() == null) {
                    return 0;
                }
                return Math.sqrt(originPos.distSqr(town.getPosition()));
            });
            if (picked != null) {
                return picked;
            }
        }
        return ANY_TOWN;
    }

    /**
     * Partial credit when an open-ticket tourist rides through a town without alighting.
     */
    public static void onPassedStop(ServerLevel level, Platform originPlatform, UUID passedTownId) {
        if (originPlatform == null || passedTownId == null) {
            return;
        }
        if (originPlatform.getDestinationMode() != DestinationMode.AUTO) {
            return;
        }
        if (ANY_TOWN.equals(passedTownId)) {
            return;
        }
        double passWeight = ConfigLoader.touristAutoPassReachWeight;
        if (passWeight <= 0) {
            return;
        }
        long tick = level.getGameTime();
        decayPlatform(originPlatform, tick);
        originPlatform.getLearnedRoutes().recordSuccess(
                passedTownId,
                tick,
                passWeight,
                0,
                ConfigLoader.touristAutoMaxLearnedTowns,
                ConfigLoader.touristAutoPruneFloor);
    }

    private static BlockPos townBlockPos(ServerLevel level, UUID townId) {
        Town town = TownManager.get(level).getTown(townId);
        return town == null ? null : town.getPosition();
    }

    public static void onArrival(ServerLevel level, Platform originPlatform, UUID arrivalTownId,
            double payableDistance, long tripDurationTicks) {
        if (originPlatform == null || arrivalTownId == null) {
            return;
        }
        if (originPlatform.getDestinationMode() != DestinationMode.AUTO) {
            return;
        }
        if (payableDistance < ConfigLoader.minPayoutDistance) {
            return;
        }
        if (arrivalTownId.equals(ANY_TOWN)) {
            return;
        }

        long tick = level.getGameTime();
        decayPlatform(originPlatform, tick);

        double weight = 1.0;
        if (ConfigLoader.minPayoutDistance > 0) {
            weight += Math.min(3.0, payableDistance / ConfigLoader.minPayoutDistance);
        }

        originPlatform.getLearnedRoutes().recordSuccess(
                arrivalTownId,
                tick,
                weight,
                Math.max(0, tripDurationTicks),
                ConfigLoader.touristAutoMaxLearnedTowns,
                ConfigLoader.touristAutoPruneFloor);
    }

    public static void onExpiredWithoutArrival(ServerLevel level, Platform originPlatform,
            UUID spawnedDestinationTownId) {
        if (originPlatform == null || spawnedDestinationTownId == null) {
            return;
        }
        if (originPlatform.getDestinationMode() != DestinationMode.AUTO) {
            return;
        }
        if (ANY_TOWN.equals(spawnedDestinationTownId)) {
            return;
        }

        long tick = level.getGameTime();
        decayPlatform(originPlatform, tick);
        originPlatform.getLearnedRoutes().recordFailure(
                spawnedDestinationTownId,
                tick,
                ConfigLoader.touristAutoExpiryPenalty,
                ConfigLoader.touristAutoMaxLearnedTowns,
                ConfigLoader.touristAutoPruneFloor);
    }

    public static void purgeTownFromAllPlatforms(ServerLevel level, UUID removedTownId) {
        if (removedTownId == null) {
            return;
        }
        for (Town town : TownManager.get(level).getAllTowns().values()) {
            TownInterfaceEntity townInterface = townInterfaceAt(level, town.getPosition());
            if (townInterface == null) {
                continue;
            }
            for (Platform platform : townInterface.getPlatforms()) {
                platform.disableDestination(removedTownId);
                platform.getLearnedRoutes().removeDestination(removedTownId);
            }
            townInterface.setChanged();
        }
        TownManager.get(level).markDirty();
    }

    public static Platform resolveOriginPlatform(ServerLevel level, UUID originTownId, String platformIdStr) {
        if (originTownId == null || platformIdStr == null || platformIdStr.isBlank()) {
            return null;
        }
        Town origin = TownManager.get(level).getTown(originTownId);
        if (origin == null) {
            return null;
        }
        TownInterfaceEntity townInterface = townInterfaceAt(level, origin.getPosition());
        if (townInterface == null) {
            return null;
        }
        try {
            UUID platformId = UUID.fromString(platformIdStr);
            return townInterface.getPlatform(platformId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static TownInterfaceEntity townInterfaceAt(ServerLevel level, BlockPos pos) {
        if (pos == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof TownInterfaceEntity tie ? tie : null;
    }

    private static Set<UUID> validDestinationIds(ServerLevel level) {
        Set<UUID> valid = new HashSet<>();
        for (UUID id : TownManager.get(level).getAllTowns().keySet()) {
            valid.add(id);
        }
        return valid;
    }

    private static void decayPlatform(Platform platform, long tick) {
        platform.getLearnedRoutes().applyDecay(
                tick,
                ConfigLoader.touristAutoScoreHalfLifeTicks,
                ConfigLoader.touristAutoFadeTripMultiplier,
                ConfigLoader.touristAutoPruneFloor);
    }

    public static double halfLifeMinutesToTicks(double minutes) {
        return Math.max(1.0, minutes) * 60.0 * 20.0;
    }
}
