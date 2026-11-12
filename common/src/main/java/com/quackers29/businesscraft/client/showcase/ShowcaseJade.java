package com.quackers29.businesscraft.client.showcase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Jade/WAILA tooltip screenshots: Town Centre, Platform Interface (Auto + Specific), tourist entity.
 * Requires Jade on the dev client classpath ({@code tools/showcase.sh jade forge}).
 */
public final class ShowcaseJade {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/ShowcaseJade");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int JOIN_DELAY_TICKS = 40;
    private static final int SETTLE_TICKS = 80;

  /** Shots written to disk (see tools/showcase.sh jade validation). */
    public static final List<String> EXPECTED_SHOTS = List.of(
            "jade_town_centre",
            "jade_platform_auto",
            "jade_platform_specific",
            "jade_tourist");

    private static int joinTicks;
    private static boolean buildQueued;
    private static volatile boolean buildDone;
    private static volatile String failure;
    private static volatile BlockPos townPos = BlockPos.ZERO;
    private static volatile UUID touristId;
    private static int shotIndex;
    private static int phaseTicks;
    private static boolean quitting;
    private static int quitTicks;
    private static int saved;

    private ShowcaseJade() {
    }

    public static void reset(Minecraft minecraft) {
        joinTicks = 0;
        buildQueued = false;
        buildDone = false;
        failure = null;
        townPos = BlockPos.ZERO;
        touristId = null;
        shotIndex = 0;
        phaseTicks = 0;
        quitting = false;
        quitTicks = 0;
        saved = 0;
        minecraft.options.pauseOnLostFocus = false;
        minecraft.options.hideGui = false;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        if (!jadePresent()) {
            failure = "Jade mod not loaded on client (need Jade runtime dep for runClient)";
            LOGGER.error("Showcase jade: {}", failure);
        } else {
            LOGGER.info("Showcase jade tooltips enabled");
        }
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
            if (++joinTicks >= JOIN_DELAY_TICKS && !buildQueued) {
                buildQueued = true;
                minecraft.getSingleplayerServer().execute(() -> build(minecraft.getSingleplayerServer()));
            }
            return;
        }
        if (shotIndex >= EXPECTED_SHOTS.size()) {
            quitting = true;
            return;
        }
        phaseTicks++;
        if (phaseTicks == 1) {
            aimForShot(minecraft, EXPECTED_SHOTS.get(shotIndex));
        }
        if (phaseTicks < SETTLE_TICKS) {
            return;
        }
        grab(minecraft, EXPECTED_SHOTS.get(shotIndex));
        shotIndex++;
        phaseTicks = 0;
    }

    private static void build(MinecraftServer server) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            if (player == null) {
                failure = "no player joined";
                return;
            }
            ServerLevel level = player.serverLevel();
            ShowcaseWorldBuild.pinDaytime(level);
            BlockPos feet = player.blockPosition();
            BlockPos placeAt = new BlockPos(feet.getX() + 8, feet.getY(), feet.getZ());
            var result = ShowcaseWorldBuild.placeTownInterfaceLikePlayer(player, level, placeAt);
            if (!result.blockRemains()) {
                failure = "town centre was not placed";
                return;
            }
            townPos = placeAt.immutable();
            Showcase.setActiveTownBlockPos(townPos);
            ShowcaseTourismSeed.seed(level, townPos);
            if (!ShowcasePlatforms.ensure(level, townPos)) {
                failure = "platform interface was not placed";
                return;
            }
            ShowcasePlatforms.applyDestinationDemo(level, townPos, ShowcasePlatforms.firstPlatformId(),
                    ShowcasePlatforms.DestinationDemoMode.AUTO);
            ShowcasePlatforms.applyDestinationDemo(level, townPos, ShowcasePlatforms.secondPlatformId(),
                    ShowcasePlatforms.DestinationDemoMode.SPECIFIC);

            if (level.getBlockEntity(ShowcasePlatforms.interfacePos(townPos)) instanceof PlatformInterfaceEntity pie) {
                pie.syncLamp();
            }
            if (level.getBlockEntity(ShowcasePlatforms.secondPos(townPos)) instanceof PlatformInterfaceEntity pie2) {
                pie2.syncLamp();
            }

            List<TouristEntity> tourists = level.getEntitiesOfClass(TouristEntity.class,
                    new AABB(townPos).inflate(32));
            if (tourists.isEmpty()) {
                failure = "no tourist found for jade shot";
                return;
            }
            touristId = tourists.get(0).getUUID();
            player.setGameMode(GameType.CREATIVE);
            buildDone = true;
            LOGGER.info("Showcase jade scene ready at {}", townPos);
        } catch (RuntimeException ex) {
            failure = ex.toString();
            LOGGER.error("Showcase jade build failed", ex);
        }
    }

    private static void aimForShot(Minecraft minecraft, String shot) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return;
        }
        Vec3 target = switch (shot) {
            case "jade_town_centre" -> Vec3.atCenterOf(townPos);
            case "jade_platform_auto" -> Vec3.atCenterOf(ShowcasePlatforms.interfacePos(townPos));
            case "jade_platform_specific" -> Vec3.atCenterOf(ShowcasePlatforms.secondPos(townPos));
            case "jade_tourist" -> {
                TouristEntity tourist = findTourist(server);
                yield tourist != null ? tourist.getEyePosition(0) : Vec3.atCenterOf(townPos.east(2));
            }
            default -> Vec3.atCenterOf(townPos);
        };
        Vec3 eye = target.add(0, 0.15, 2.25);
        if ("jade_tourist".equals(shot)) {
            eye = target.add(0, 0.05, 2.8);
        }
        float[] facing = ShowcaseSelection.facing(eye.x, eye.y, eye.z, target.x, target.y, target.z);
        minecraft.options.hideGui = false;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        final double feetY = eye.y - minecraft.player.getEyeHeight();
        final double ex = eye.x;
        final double ez = eye.z;
        final float yaw = facing[0];
        final float pitch = facing[1];
        var player = minecraft.player;
        player.setYRot(yaw);
        player.setXRot(pitch);
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(player.getUUID());
            if (sp != null) {
                sp.setGameMode(GameType.CREATIVE);
                sp.teleportTo(sp.serverLevel(), ex, feetY, ez, yaw, pitch);
                sp.setYHeadRot(yaw);
            }
        });
    }

    private static TouristEntity findTourist(MinecraftServer server) {
        if (touristId == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            var entity = level.getEntity(touristId);
            if (entity instanceof TouristEntity tourist) {
                return tourist;
            }
        }
        return null;
    }

    private static void grab(Minecraft minecraft, String name) {
        String file = "showcase_" + name + ".png";
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message ->
                LOGGER.info("Showcase jade shot {}: {}", file, message.getString()));
        saved++;
    }

    private static void shutdown(Minecraft minecraft, int code) {
        if (++quitTicks >= 40) {
            writeManifest(minecraft, code);
            Showcase.setProcessExitCode(code);
            LOGGER.info("Showcase jade finished: {} screenshots, exit {}", saved, code);
            minecraft.stop();
            System.exit(code);
        }
    }

    private static void writeManifest(Minecraft minecraft, int exitCode) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("scenario", "jade");
        root.put("exitCode", exitCode);
        root.put("shots", new ArrayList<>(EXPECTED_SHOTS));
        root.put("saved", saved);
        root.put("jadePresent", jadePresent());
        Path out = minecraft.gameDirectory.toPath().resolve("showcase-jade.json");
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", out, e);
        }
    }

    private static boolean jadePresent() {
        try {
            Class.forName("snownee.jade.Jade", false, ShowcaseJade.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
