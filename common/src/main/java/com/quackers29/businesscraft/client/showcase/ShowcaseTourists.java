package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Spawns a small group of tourists for showcase screenshots.
 */
public final class ShowcaseTourists {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/ShowcaseTourists");
    private static final String MODE = "tourists";

    private static boolean configured;
    private static int joinTicks;
    private static boolean buildQueued;
    private static volatile boolean buildDone;
    private static volatile String failure;
    private static int phaseTicks;
    private static int phase;
    private static boolean quitting;
    private static int quitTicks;
    private static int saved;

    private ShowcaseTourists() {
    }

    public static boolean isMode(String property) {
        return property != null && MODE.equalsIgnoreCase(property.trim());
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        if (failure != null) {
            shutdown(minecraft, 1);
            return;
        }
        if (!buildDone) {
            joinTicks++;
            if (!buildQueued && joinTicks >= 40) {
                buildQueued = true;
                queueBuild(minecraft.getSingleplayerServer());
            }
            return;
        }

        phaseTicks++;
        if (phase == 0 && phaseTicks >= 60) {
            aimAtTourists(minecraft);
            grab(minecraft, "tourists_world");
            phase = 1;
            phaseTicks = 0;
        } else if (phase == 1 && phaseTicks >= 20) {
            shutdown(minecraft, 0);
        }
    }

    public static void reset(Minecraft minecraft) {
        configured = true;
        joinTicks = 0;
        buildQueued = false;
        buildDone = false;
        failure = null;
        phase = 0;
        phaseTicks = 0;
        quitting = false;
        quitTicks = 0;
        saved = 0;
        LOGGER.info("Showcase tourists scenario enabled");
    }

    private static void queueBuild(MinecraftServer server) {
        server.execute(() -> {
            try {
                ShowcaseBuildHelper.buildSingleTown(server, townPos -> {
                    spawnTourists(server, townPos);
                    buildDone = true;
                }, err -> failure = err);
            } catch (RuntimeException e) {
                failure = e.toString();
            }
        });
    }

    private static void spawnTourists(MinecraftServer server, BlockPos townPos) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        BlockEntity be = level.getBlockEntity(townPos);
        if (!(be instanceof TownInterfaceEntity townInterface) || townInterface.getTownId() == null) {
            failure = "no town at showcase block";
            return;
        }
        Town origin = TownManager.get(level).getTown(townInterface.getTownId());
        if (origin == null) {
            failure = "town data missing for showcase block";
            return;
        }
        UUID destId = UUID.randomUUID();
        String[] destNames = {"Hillcrest", "Oakvale", "Rivertown"};
        BlockPos spawnBase = townPos.east(3);
        for (int i = 0; i < 3; i++) {
            TouristEntity tourist = new TouristEntity(
                    (net.minecraft.world.entity.EntityType<TouristEntity>) PlatformAccess.getEntities()
                            .getTouristEntityType(),
                    level,
                    origin,
                    null,
                    destId,
                    destNames[i]);
            tourist.setPos(spawnBase.getX() + 0.5, spawnBase.getY(), spawnBase.getZ() + i * 2 + 0.5);
            level.addFreshEntity(tourist);
        }
        Showcase.setActiveTownBlockPos(townPos);
    }

    private static void aimAtTourists(Minecraft minecraft) {
        BlockPos pos = Showcase.getActiveTownBlockPos();
        Vec3 target = Vec3.atCenterOf(pos.east(3));
        Vec3 eye = target.add(2.5, 1.8, 1.5);
        float[] facing = ShowcaseSelection.facing(eye.x, eye.y, eye.z, target.x, target.y + 1, target.z);
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
            if (serverPlayer != null) {
                serverPlayer.teleportTo(serverPlayer.serverLevel(), eye.x, eye.y - serverPlayer.getEyeHeight(),
                        eye.z, facing[0], facing[1]);
                serverPlayer.setYHeadRot(facing[0]);
            }
        });
    }

    private static void grab(Minecraft minecraft, String name) {
        String file = "showcase_" + name + ".png";
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message ->
                LOGGER.info("Showcase shot {}: {}", file, message.getString()));
        saved++;
    }

    private static void shutdown(Minecraft minecraft, int code) {
        if (!quitting) {
            quitting = true;
            quitTicks = 0;
            if (code != 0) {
                LOGGER.error("Showcase tourists failed: {}", failure);
            } else {
                LOGGER.info("Showcase tourists finished: {} screenshots", saved);
            }
            Showcase.setProcessExitCode(code);
        }
        if (++quitTicks >= 40) {
            minecraft.stop();
            System.exit(code);
        }
    }
}
