package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads platform stops from each town's centre block entity.
 * <p>
 * Platform paths live on that entity, not in the town saved data. Looking up
 * the entity loads the centre chunk. Placement does this once per attempt.
 * A tick must not call it for every kiosk, or distant centres stay loaded.
 */
public final class PlatformLinkResolver {
    private PlatformLinkResolver() {
    }

    public static List<PlatformLink.Stop> stops(ServerLevel level) {
        List<PlatformLink.Stop> stops = new ArrayList<>();
        if (level == null) {
            return stops;
        }
        for (Town town : TownManager.get(level).getAllTowns().values()) {
            BlockPos centre = town.getPosition();
            if (centre == null) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(centre);
            if (!(blockEntity instanceof TownInterfaceEntity centreEntity)) {
                continue;
            }
            for (Platform platform : centreEntity.getPlatforms()) {
                if (!platform.isComplete()) {
                    continue;
                }
                stops.add(new PlatformLink.Stop(town.getId(), platform.getId(), platform.getStartPos()));
                stops.add(new PlatformLink.Stop(town.getId(), platform.getId(), platform.getEndPos()));
            }
        }
        return stops;
    }

    public static PlatformLink.Choice choose(ServerLevel level, BlockPos place) {
        return PlatformLink.choose(place, stops(level));
    }
}
