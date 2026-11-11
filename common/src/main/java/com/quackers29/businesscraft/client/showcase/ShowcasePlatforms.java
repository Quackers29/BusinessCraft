package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Places one Platform Interface next to a showcase town and opens its screen.
 */
public final class ShowcasePlatforms {
    public static final BlockPos OFFSET = ShowcasePlatformPlacement.OFFSET;
    public static final BlockPos SECOND_OFFSET = ShowcasePlatformPlacement.SECOND_OFFSET;

    private ShowcasePlatforms() {
    }

    public static BlockPos interfacePos(BlockPos townPos) {
        return ShowcasePlatformPlacement.interfacePos(townPos);
    }

    public static BlockPos secondPos(BlockPos townPos) {
        return ShowcasePlatformPlacement.secondPos(townPos);
    }

    public static BlockPos lastPlaced() {
        return ShowcasePlatformPlacement.lastPlaced();
    }

    public static BlockPos lastSecond() {
        return ShowcasePlatformPlacement.lastSecond();
    }

    public static UUID firstPlatformId() {
        return ShowcasePlatformPlacement.firstPlatformId();
    }

    public static UUID secondPlatformId() {
        return ShowcasePlatformPlacement.secondPlatformId();
    }

    public static boolean ensure(ServerLevel level, BlockPos townPos) {
        return ShowcasePlatformPlacement.ensure(level, townPos);
    }

    public static void openScreen(Minecraft minecraft, BlockPos townPos) {
        openScreenForDestinationDemo(minecraft, townPos, null);
    }

    public enum DestinationDemoMode {
        ANY,
        AUTO,
        SPECIFIC
    }

    /**
     * Applies server-side platform destination state for showcase, then opens the interface screen.
     */
    public static void openScreenForDestinationDemo(Minecraft minecraft, BlockPos townPos,
            DestinationDemoMode mode) {
        BlockPos pos = lastPlaced() != null ? lastPlaced() : interfacePos(townPos);
        if (pos == null || minecraft.getSingleplayerServer() == null || minecraft.player == null) {
            return;
        }
        minecraft.setScreen(null);
        MinecraftServer server = minecraft.getSingleplayerServer();
        UUID playerId = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return;
            }
            if (mode != null) {
                applyDestinationDemo(player.serverLevel(), townPos, mode);
            }
            minecraft.execute(() -> openScreenOnClient(minecraft, pos, playerId, server));
        });
    }

    private static void openScreenOnClient(Minecraft minecraft, BlockPos pos, UUID playerId,
            MinecraftServer server) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof PlatformInterfaceBlock block) {
                block.use(state, level, pos, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            }
        });
    }

    static void applyDestinationDemo(ServerLevel level, BlockPos townPos, DestinationDemoMode mode) {
        applyDestinationDemo(level, townPos, firstPlatformId, mode);
    }

    /**
     * Applies destination demo state to a specific platform (used by Jade and platform UI showcase).
     */
    public static void applyDestinationDemo(ServerLevel level, BlockPos townPos, UUID platformId,
            DestinationDemoMode mode) {
        if (platformId == null || mode == null) {
            return;
        }
        if (!(level.getBlockEntity(townPos) instanceof TownInterfaceEntity townInterface)) {
            return;
        }
        Platform platform = townInterface.getPlatform(platformId);
        if (platform == null) {
            return;
        }
        platform.clearLearnedRoutes();
        for (UUID id : platform.getEnabledDestinations()) {
            platform.disableDestination(id);
        }
        TownManager manager = TownManager.get(level);
        UUID oak = townIdByName(manager, "Oakvale");
        UUID hill = townIdByName(manager, "Hillcrest");
        long tick = level.getGameTime();
        switch (mode) {
            case ANY -> platform.setDestinationMode(DestinationMode.ANY);
            case AUTO -> {
                platform.setDestinationMode(DestinationMode.AUTO);
                if (oak != null) {
                    platform.getLearnedRoutes().recordSuccess(oak, tick, 5.0, 2L * 60L * 60L * 20L, 12, 0.05);
                }
                if (hill != null) {
                    platform.getLearnedRoutes().recordSuccess(hill, tick, 3.0, 5L * 60L * 20L, 12, 0.05);
                }
            }
            case SPECIFIC -> {
                platform.setDestinationMode(DestinationMode.SPECIFIC);
                if (oak != null) {
                    platform.enableDestination(oak);
                }
                if (hill != null) {
                    platform.enableDestination(hill);
                }
            }
        }
        townInterface.setChanged();
        townInterface.syncPlatforms();
    }

    private static UUID townIdByName(TownManager manager, String name) {
        for (Town town : manager.getAllTowns().values()) {
            if (name.equals(town.getName())) {
                return town.getId();
            }
        }
        return null;
    }
}
