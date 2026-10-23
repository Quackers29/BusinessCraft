package com.quackers29.businesscraft.client.showcase.testbed;

import com.quackers29.businesscraft.client.showcase.Showcase;
import com.quackers29.businesscraft.client.showcase.ShowcaseWorldBuild;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Places town A at the showcase pad and town B at the minimum legal centre distance (same layout as two-towns).
 */
public final class ShowcaseTwoTownSetup {
    private ShowcaseTwoTownSetup() {
    }

    public static boolean build(MinecraftServer server, ShowcaseTestbedContext context) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(0, 0, false, false);
        player.setGameMode(GameType.CREATIVE);

        BlockPos feet = player.blockPosition();
        BlockPos townA = new BlockPos(feet.getX() + 8, feet.getY(), feet.getZ());
        ShowcaseWorldBuild.PlaceAttemptResult a = ShowcaseWorldBuild.placeTownInterfaceLikePlayer(player, level, townA);
        if (!a.blockRemains() || a.townCountAfter() != 1 || a.townIdAtPos() == null) {
            return false;
        }
        Town townAData = findTownAt(level, townA);
        int required = ConfigLoader.defaultBorderRadius
                + (townAData != null ? townAData.getBoundaryRadius() : ConfigLoader.defaultBorderRadius);
        BlockPos townB = townA.offset(required, 0, 0);
        ShowcaseWorldBuild.PlaceAttemptResult b = ShowcaseWorldBuild.placeTownInterfaceLikePlayer(player, level, townB);
        if (!b.blockRemains() || b.townCountAfter() != 2 || b.townIdAtPos() == null) {
            return false;
        }

        context.setTownAPos(townA);
        context.setTownBPos(townB);
        context.setTownAId(a.townIdAtPos());
        context.setTownBId(b.townIdAtPos());
        context.setRequiredCentreDistance(required);

        Showcase.setActiveTownBlockPos(townB);
        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(level, townB.getX() + 0.5, townB.getY(), townB.getZ() + 3.5, 180f, 0f);
        player.setYHeadRot(180f);
        return true;
    }

    private static Town findTownAt(ServerLevel level, BlockPos pos) {
        for (Town town : TownManager.get(level).getAllTowns().values()) {
            if (town.getPosition().equals(pos)) {
                return town;
            }
        }
        return null;
    }
}
