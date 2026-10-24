package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

/**
 * Seeds visit history and waiting tourists so showcase town/overview shots show tourism UI lines.
 */
final class ShowcaseTourismSeed {
    private ShowcaseTourismSeed() {
    }

    static void seed(ServerLevel level, BlockPos townBlockPos) {
        BlockEntity be = level.getBlockEntity(townBlockPos);
        if (!(be instanceof TownInterfaceEntity townInterface) || townInterface.getTownId() == null) {
            return;
        }
        Town town = TownManager.get(level).getTown(townInterface.getTownId());
        if (town == null) {
            return;
        }

        TownManager manager = TownManager.get(level);
        BlockPos oakPos = townBlockPos.offset(-200, 0, 0);
        BlockPos hillPos = townBlockPos.offset(200, 0, 0);
        UUID oakId = manager.registerTown(oakPos, "Oakvale");
        UUID hillId = manager.registerTown(hillPos, "Hillcrest");
        if (oakId != null) {
            town.recordVisit(oakId, 2, oakPos);
        }
        if (hillId != null) {
            town.recordVisit(hillId, 1, hillPos);
        }

        BlockPos spawn = townBlockPos.east(2);
        spawnWaitingTourist(level, town, spawn, "Hillcrest");
        spawnWaitingTourist(level, town, spawn.south(2), "Rivertown");

        townInterface.updateInterfaceVM();
        townInterface.syncAllDirtyTownVMsToPlayers();
    }

    private static void spawnWaitingTourist(ServerLevel level, Town origin, BlockPos pos, String destName) {
        TouristEntity tourist = new TouristEntity(
                (net.minecraft.world.entity.EntityType<TouristEntity>) PlatformAccess.getEntities()
                        .getTouristEntityType(),
                level,
                origin,
                null,
                UUID.randomUUID(),
                destName);
        tourist.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(tourist);
    }
}
