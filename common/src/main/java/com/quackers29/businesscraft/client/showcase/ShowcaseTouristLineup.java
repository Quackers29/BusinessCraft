package com.quackers29.businesscraft.client.showcase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.tourist.TouristLook;
import com.quackers29.businesscraft.entity.tourist.TouristLooks;
import com.quackers29.businesscraft.init.CommonModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spawns every tourist look in a line and photographs the group plus each look
 * from the front and the side. Driven by {@code tools/showcase.sh tourists}.
 */
public final class ShowcaseTouristLineup {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/ShowcaseTourists");
    private static final int JOIN_DELAY_TICKS = 40;
    private static final int SETTLE_TICKS = 50;
    private static final int SHOT_TICKS = 12;
    private static final double SPACING = 1.85;

    private static int joinTicks;
    private static boolean buildQueued;
    private static volatile boolean buildDone;
    private static volatile String failure;
    private static volatile List<Spot> spots = List.of();
    private static volatile double floorY;
    private static int phase;
    private static int phaseTicks;
    private static int lookIndex;
    private static boolean sideShot;
    private static boolean quitting;
    private static int quitTicks;
    private static int saved;
    private static final List<String> shotNames = new ArrayList<>();

    private ShowcaseTouristLineup() {
    }

    public static void reset(Minecraft minecraft) {
        joinTicks = 0;
        buildQueued = false;
        buildDone = false;
        failure = null;
        spots = List.of();
        phase = 0;
        phaseTicks = 0;
        lookIndex = 0;
        sideShot = false;
        quitting = false;
        quitTicks = 0;
        saved = 0;
        shotNames.clear();
        minecraft.options.hideGui = true;
        minecraft.options.pauseOnLostFocus = false;
        minecraft.options.bobView().set(false);
        LOGGER.info("Showcase tourist lineup enabled");
    }

    public static void tick(Minecraft minecraft) {
        if (failure != null) {
            shutdown(minecraft, 1);
            return;
        }
        if (quitting) {
            shutdown(minecraft, 0);
            return;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        if (!buildDone) {
            joinTicks++;
            if (!buildQueued && joinTicks >= JOIN_DELAY_TICKS) {
                buildQueued = true;
                MinecraftServer server = minecraft.getSingleplayerServer();
                server.execute(() -> build(server));
            }
            return;
        }
        phaseTicks++;
        if (phase == 0) {
            if (phaseTicks == 1) {
                aimLineup(minecraft, true);
            }
            if (phaseTicks >= SETTLE_TICKS) {
                grab(minecraft, "tourists_lineup_front");
                phase = 1;
                phaseTicks = 0;
            }
            return;
        }
        if (phase == 1) {
            if (phaseTicks == 1) {
                aimLineup(minecraft, false);
            }
            if (phaseTicks >= SHOT_TICKS) {
                grab(minecraft, "tourists_lineup_side");
                phase = 2;
                phaseTicks = 0;
                lookIndex = 0;
                sideShot = false;
            }
            return;
        }
        if (phaseTicks == 1) {
            aimSpot(minecraft, spots.get(lookIndex), sideShot);
        }
        if (phaseTicks < SHOT_TICKS) {
            return;
        }
        Spot spot = spots.get(lookIndex);
        grab(minecraft, "tourist_" + spot.id + (sideShot ? "_side" : "_front"));
        if (!sideShot) {
            sideShot = true;
            phaseTicks = 0;
            return;
        }
        sideShot = false;
        phaseTicks = 0;
        lookIndex++;
        if (lookIndex >= spots.size()) {
            writeManifest(minecraft);
            quitting = true;
            quitTicks = 0;
        }
    }

    private static void build(MinecraftServer server) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                    ? null
                    : server.getPlayerList().getPlayers().get(0);
            if (player == null) {
                failure = "no player joined";
                return;
            }
            ServerLevel level = player.serverLevel();
            level.setDayTime(6000);
            level.setWeatherParameters(0, 0, false, false);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            player.setGameMode(GameType.SPECTATOR);

            List<TouristLook> looks = TouristLooks.all();
            if (looks.isEmpty()) {
                failure = "no tourist looks loaded";
                return;
            }
            BlockPos feet = player.blockPosition();
            int ground = feet.getY() - 1;
            int originX = feet.getX();
            int originZ = feet.getZ();
            int count = looks.size();
            int x0 = originX - 4;
            int x1 = originX + (int) Math.ceil((count - 1) * SPACING) + 6;
            int z0 = originZ - 5;
            int z1 = originZ + 18;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    level.setBlock(new BlockPos(x, ground, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                    for (int y = 1; y <= 8; y++) {
                        level.setBlock(new BlockPos(x, ground + y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                    if (z == originZ - 3) {
                        for (int y = 1; y <= 5; y++) {
                            level.setBlock(new BlockPos(x, ground + y, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 3);
                        }
                    }
                }
            }

            List<Spot> built = new ArrayList<>();
            var type = CommonModEntityTypes.TOURIST.get();
            if (type == null) {
                failure = "tourist entity type is not registered";
                return;
            }
            for (int i = 0; i < looks.size(); i++) {
                TouristLook look = looks.get(i);
                double x = originX + 1.5 + i * SPACING;
                double z = originZ + 0.5;
                double y = ground + 1.0;
                TouristEntity tourist = new TouristEntity(type, level);
                tourist.applyLook(look);
                tourist.setNoAi(true);
                tourist.setSilent(true);
                tourist.setCustomName(null);
                tourist.setCustomNameVisible(false);
                tourist.setPos(x, y, z);
                tourist.setYRot(0f);
                tourist.yRotO = 0f;
                tourist.setXRot(0f);
                tourist.xRotO = 0f;
                tourist.setYHeadRot(0f);
                tourist.yHeadRotO = 0f;
                tourist.setYBodyRot(0f);
                tourist.yBodyRotO = 0f;
                level.addFreshEntity(tourist);
                built.add(new Spot(look.id(), x, y, z));
            }
            spots = List.copyOf(built);
            floorY = ground + 1.0;
            LOGGER.info("Showcase spawned {} tourist looks", built.size());
            buildDone = true;
        } catch (RuntimeException e) {
            failure = e.toString();
            LOGGER.error("Showcase failed", e);
        }
    }

    private static void aimLineup(Minecraft minecraft, boolean front) {
        List<Spot> current = spots;
        if (current.isEmpty()) {
            return;
        }
        Spot first = current.get(0);
        Spot last = current.get(current.size() - 1);
        double cx = (first.x + last.x) / 2.0;
        double cz = first.z;
        double half = Math.abs(last.x - first.x) / 2.0 + 1.2;
        double distance = Math.max(8.0, half / Math.tan(Math.toRadians(35.0)) + 2.5);
        Vec3 eye = front
                ? new Vec3(cx, floorY + 1.55, cz + distance)
                : new Vec3(first.x - distance * 0.85, floorY + 1.9, cz + distance * 0.45);
        Vec3 target = new Vec3(cx, floorY + 1.15, cz);
        aim(minecraft, eye, target);
    }

    private static void aimSpot(Minecraft minecraft, Spot spot, boolean side) {
        Vec3 target = new Vec3(spot.x, spot.y + 1.25, spot.z);
        Vec3 eye = side
                ? new Vec3(spot.x - 3.3, spot.y + 1.45, spot.z + 1.15)
                : new Vec3(spot.x, spot.y + 1.45, spot.z + 3.5);
        aim(minecraft, eye, target);
    }

    private static void aim(Minecraft minecraft, Vec3 eye, Vec3 target) {
        float[] facing = ShowcaseSelection.facing(eye.x, eye.y, eye.z, target.x, target.y, target.z);
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        double feetY = eye.y - player.getEyeHeight();
        player.setYRot(facing[0]);
        player.setXRot(facing[1]);
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
            if (serverPlayer != null) {
                serverPlayer.teleportTo(serverPlayer.serverLevel(), eye.x, feetY, eye.z, facing[0], facing[1]);
                serverPlayer.setYHeadRot(facing[0]);
            }
        });
    }

    private static void grab(Minecraft minecraft, String name) {
        String file = "showcase_" + name + ".png";
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message ->
                LOGGER.info("Showcase shot {}: {}", file, message.getString()));
        shotNames.add(name);
        saved++;
    }

    private static void writeManifest(Minecraft minecraft) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("looks", spots.stream().map(spot -> spot.id).toList());
        manifest.put("shots", List.copyOf(shotNames));
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(minecraft.gameDirectory.toPath().resolve("showcase-tourists.json"), gson.toJson(manifest));
        } catch (IOException e) {
            failure = "could not write tourist manifest: " + e.getMessage();
            LOGGER.error("Showcase failed", e);
        }
    }

    private static void shutdown(Minecraft minecraft, int code) {
        if (!quitting) {
            quitting = true;
            quitTicks = 0;
            if (code != 0) {
                LOGGER.error("Showcase failed: {}", failure);
            }
        }
        if (++quitTicks >= 40) {
            if (code == 0 && failure == null) {
                LOGGER.info("Showcase tourists finished: {} screenshots", saved);
            }
            minecraft.stop();
            System.exit(failure == null ? code : 1);
        }
    }

    private record Spot(String id, double x, double y, double z) {
    }
}
