package com.quackers29.businesscraft.client.showcase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import com.quackers29.businesscraft.ui.screens.town.TownLeaderboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
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

/**
 * Showcase scenario: town A at showcase pad, boundary negative/positive placement, all-views on town B.
 */
public final class ShowcaseTwoTowns {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int JOIN_DELAY_TICKS = 40;

    private static volatile Phase phase = Phase.JOIN;

    private enum Phase {
        JOIN,
        PLACE_A,
        NEGATIVE,
        POSITIVE,
        ALL_VIEWS,
        VERIFY_OPEN_TRADE,
        VERIFY_MAP,
        VERIFY_RANKING_WAIT,
        QUIT
    }

    private static int joinTicks;
    private static boolean placeAStarted;
    private static BlockPos townA = BlockPos.ZERO;
    private static BlockPos townB = BlockPos.ZERO;
    private static BlockPos negativeAttemptPos = BlockPos.ZERO;
    private static int requiredDistanceBlocks;
    private static int defaultBorderRadius;
    private static int townABoundaryRadius;
    private static String rejectionMessage;
    private static final List<Map<String, Object>> steps = new ArrayList<>();
    private static int verifyTicks;
    private static int quitTicks;
    private static int exitCode;

    private ShowcaseTwoTowns() {
    }

    public static void reset(Minecraft minecraft) {
        phase = Phase.JOIN;
        joinTicks = 0;
        placeAStarted = false;
        townA = BlockPos.ZERO;
        townB = BlockPos.ZERO;
        steps.clear();
        verifyTicks = 0;
        quitTicks = 0;
        exitCode = 0;
        ShowcaseAllViews.setEmbeddedMode(true);
        ShowcaseAllViews.reset(minecraft);
    }

    public static void tick(Minecraft minecraft) {
        if (phase == Phase.QUIT) {
            if (++quitTicks >= 40) {
                writeReport(minecraft);
                minecraft.stop();
                System.exit(exitCode);
            }
            return;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        MinecraftServer server = minecraft.getSingleplayerServer();
        switch (phase) {
            case JOIN -> {
                if (++joinTicks >= JOIN_DELAY_TICKS) {
                    phase = Phase.PLACE_A;
                }
            }
            case PLACE_A -> {
                if (!placeAStarted) {
                    placeAStarted = true;
                    server.execute(() -> runPlaceA(server, minecraft));
                }
            }
            case ALL_VIEWS -> {
                if (!ShowcaseAllViews.isComplete()) {
                    ShowcaseAllViews.tick(minecraft, Showcase.getActiveTownBlockPos());
                } else if (ShowcaseAllViews.exitCode() != 0) {
                    recordStep("all_views_town_b", false, Map.of("exitCode", ShowcaseAllViews.exitCode()));
                    fail("all_views_town_b");
                } else {
                    recordStep("all_views_town_b", true, Map.of(
                            "activeTown", townB.toShortString(),
                            "savedViews", ShowcaseAllViews.savedCount()));
                    phase = Phase.VERIFY_OPEN_TRADE;
                    verifyTicks = 0;
                }
            }
            case VERIFY_OPEN_TRADE -> tickVerifyTrade(minecraft);
            case VERIFY_MAP -> tickVerifyMap(minecraft);
            case VERIFY_RANKING_WAIT -> tickVerifyRanking(minecraft);
            default -> {
            }
        }
    }

    private static void runPlaceA(MinecraftServer server, Minecraft minecraft) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            ServerLevel level = player.serverLevel();
            level.setDayTime(6000);
            level.setWeatherParameters(0, 0, false, false);
            player.setGameMode(GameType.CREATIVE);

            BlockPos feet = player.blockPosition();
            townA = new BlockPos(feet.getX() + 8, feet.getY(), feet.getZ());
            ShowcaseWorldBuild.PlaceAttemptResult result = ShowcaseWorldBuild.placeTownInterfaceLikePlayer(player, level, townA);
            boolean ok = result.blockRemains() && result.townCountAfter() == 1 && result.townIdAtPos() != null;
            recordStep("place_town_a", ok, Map.of("position", townA.toShortString(), "townCount", result.townCountAfter()));
            if (!ok) {
                fail("place_town_a");
                return;
            }
            Showcase.setActiveTownBlockPos(townA);
            defaultBorderRadius = ConfigLoader.defaultBorderRadius;
            Town town = findTownAt(level, townA);
            townABoundaryRadius = town != null ? town.getBoundaryRadius() : defaultBorderRadius;
            requiredDistanceBlocks = defaultBorderRadius + townABoundaryRadius;
            negativeAttemptPos = townA.offset(requiredDistanceBlocks - 1, 0, 0);
            townB = townA.offset(requiredDistanceBlocks, 0, 0);

            LOGGER.info(
                    "SHOWCASE_TWO_TOWNS_MIN_DISTANCE required={} defaultBorder={} townA_border={} measurement=centre_to_centre_block_pos",
                    requiredDistanceBlocks, defaultBorderRadius, townABoundaryRadius);
            recordStep("read_min_distance", true, Map.of(
                    "defaultBorderRadius", defaultBorderRadius,
                    "townABoundaryRadius", townABoundaryRadius,
                    "requiredCentreDistance", requiredDistanceBlocks,
                    "negativeAttemptDistance", requiredDistanceBlocks - 1,
                    "positiveAttemptDistance", requiredDistanceBlocks,
                    "measurement", "sqrt(distSqr) between BlockPos centres >= newTownBorder + existingTownBorder"));

            runNegative(server, minecraft);
        } catch (RuntimeException e) {
            LOGGER.error("Showcase two-towns failed", e);
            recordStep("place_town_a", false, Map.of("error", e.toString()));
            fail("place_town_a");
        }
    }

    private static void runNegative(MinecraftServer server, Minecraft minecraft) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        TownManager manager = TownManager.get(level);
        rejectionMessage = manager.getTownPlacementError(negativeAttemptPos);
        int townsBefore = ShowcaseWorldBuild.countTowns(level);
        ShowcaseWorldBuild.PlaceAttemptResult result = ShowcaseWorldBuild.placeTownInterfaceLikePlayer(player, level,
                negativeAttemptPos);
        boolean rejected = !result.blockRemains() && result.townCountAfter() == townsBefore && townsBefore == 1;
        if (rejectionMessage == null) {
            rejectionMessage = result.placementErrorMessage();
        }
        LOGGER.info("SHOWCASE_TWO_TOWNS_REJECT_MSG: {}", rejectionMessage);
        recordStep("negative_placement", rejected, Map.of(
                "attemptPosition", negativeAttemptPos.toShortString(),
                "centreDistanceFromA", requiredDistanceBlocks - 1,
                "requiredMinimum", requiredDistanceBlocks,
                "rejectionMessage", rejectionMessage != null ? rejectionMessage : "",
                "blockRemains", result.blockRemains(),
                "townCountAfter", result.townCountAfter()));
        if (!rejected) {
            fail("negative_placement");
            return;
        }
        runPositive(server, minecraft);
    }

    private static void runPositive(MinecraftServer server, Minecraft minecraft) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        ShowcaseWorldBuild.PlaceAttemptResult result = ShowcaseWorldBuild.placeTownInterfaceLikePlayer(player, level, townB);
        boolean ok = result.blockRemains() && result.townCountAfter() == 2 && result.townIdAtPos() != null;
        recordStep("positive_placement", ok, Map.of(
                "attemptPosition", townB.toShortString(),
                "centreDistanceFromA", requiredDistanceBlocks,
                "townCountAfter", result.townCountAfter()));
        if (!ok) {
            fail("positive_placement");
            return;
        }
        Showcase.setActiveTownBlockPos(townB);
        ShowcaseSetup.resetForTests();
        ShowcaseSetup.prepareOnce(server, townB);
        player.setGameMode(GameType.SPECTATOR);
        minecraft.execute(() -> {
            ShowcaseAllViews.setEmbeddedMode(true);
            ShowcaseAllViews.reset(minecraft);
            phase = Phase.ALL_VIEWS;
        });
    }

    private static void tickVerifyTrade(Minecraft minecraft) {
        verifyTicks++;
        BlockPos active = Showcase.getActiveTownBlockPos();
        if (verifyTicks == 1) {
            ShowcaseNavigator.runStep(minecraft, active, "open-town");
        } else if (verifyTicks == 5) {
            ShowcaseNavigator.runStep(minecraft, active, "set-tab:resources");
        } else if (verifyTicks == 10) {
            ShowcaseNavigator.runStep(minecraft, active, "press-button:id:" + ShowcaseButtonIds.TRADE);
        } else if (verifyTicks >= 80) {
            boolean tradeOk = minecraft.screen instanceof BCModalInventoryScreen<?>;
            recordStep("verify_trade_ui_town_b", tradeOk, Map.of("screen", String.valueOf(minecraft.screen)));
            if (!tradeOk) {
                fail("verify_trade_ui_town_b");
                return;
            }
            verifyTicks = 0;
            phase = Phase.VERIFY_MAP;
            minecraft.setScreen(null);
        }
    }

    private static void tickVerifyMap(Minecraft minecraft) {
        verifyTicks++;
        BlockPos active = Showcase.getActiveTownBlockPos();
        if (verifyTicks == 1) {
            ShowcaseNavigator.runStep(minecraft, active, "open-town");
        } else if (verifyTicks == 5) {
            ShowcaseNavigator.runStep(minecraft, active, "set-tab:overview");
        } else if (verifyTicks == 10) {
            ShowcaseNavigator.runStep(minecraft, active, "press-button:id:" + ShowcaseButtonIds.MAP_VIEW);
        } else if (verifyTicks >= 100) {
            int mapCount = ClientTownMapCache.getInstance().getAllTowns().size();
            boolean mapOk = mapCount >= 2;
            LOGGER.info("SHOWCASE_VERIFY_MAP_TOWNS={}", mapCount);
            recordStep("verify_map_two_towns", mapOk, Map.of("clientMapTownCount", mapCount));
            if (!mapOk) {
                fail("verify_map_two_towns");
                return;
            }
            int serverTowns = serverTownCount(minecraft);
            recordStep("verify_server_two_towns", serverTowns >= 2, Map.of("count", serverTowns));
            if (serverTowns < 2) {
                fail("verify_server_two_towns");
                return;
            }
            verifyTicks = 0;
            phase = Phase.VERIFY_RANKING_WAIT;
            minecraft.setScreen(null);
        }
    }

    private static void tickVerifyRanking(Minecraft minecraft) {
        verifyTicks++;
        BlockPos active = Showcase.getActiveTownBlockPos();
        if (verifyTicks == 1) {
            ShowcaseNavigator.runStep(minecraft, active, "set-tab:overview");
        } else if (verifyTicks == 5) {
            ShowcaseNavigator.runStep(minecraft, active, "press-button:id:" + ShowcaseButtonIds.RANKING);
        } else if (verifyTicks >= 100) {
            boolean rankingOk = false;
            int count = 0;
            if (minecraft.screen instanceof TownLeaderboardScreen screen) {
                count = screen.getTownDataCount();
                rankingOk = count >= 2;
            }
            LOGGER.info("SHOWCASE_VERIFY_RANKING_TOWNS={}", count);
            recordStep("verify_ranking_two_towns", rankingOk, Map.of("townCount", count));
            if (!rankingOk) {
                fail("verify_ranking_two_towns");
                return;
            }
            phase = Phase.QUIT;
            return;
        }
    }

    private static int serverTownCount(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return 0;
        }
        return ShowcaseWorldBuild.countTowns(server.overworld());
    }

    private static Town findTownAt(ServerLevel level, BlockPos pos) {
        for (Town town : TownManager.get(level).getAllTowns().values()) {
            if (town.getPosition().equals(pos)) {
                return town;
            }
        }
        return null;
    }

    private static void recordStep(String name, boolean pass, Map<String, Object> details) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("step", name);
        entry.put("pass", pass);
        entry.put("details", details);
        steps.add(entry);
        LOGGER.info("SHOWCASE_TWO_TOWNS_STEP {} {}", name, pass ? "PASS" : "FAIL");
    }

    private static void fail(String step) {
        exitCode = 1;
        LOGGER.error("SHOWCASE_TWO_TOWNS_FAIL at {}", step);
        phase = Phase.QUIT;
    }

    private static void writeReport(Minecraft minecraft) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("loader", loaderName());
        report.put("townA", townA.toShortString());
        report.put("townB", townB.toShortString());
        report.put("negativeAttempt", negativeAttemptPos.toShortString());
        report.put("requiredCentreDistance", requiredDistanceBlocks);
        report.put("defaultBorderRadius", defaultBorderRadius);
        report.put("rejectionMessage", rejectionMessage);
        report.put("steps", steps);
        report.put("exitCode", exitCode);
        Path path = minecraft.gameDirectory.toPath().resolve("showcase-two-towns-report.json");
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(report, w);
        } catch (IOException e) {
            LOGGER.error("Failed to write two-towns report", e);
        }
    }

    private static String loaderName() {
        String loader = System.getProperty("businesscraft.showcase.loader");
        return loader != null && !loader.isBlank() ? loader : "unknown";
    }
}
