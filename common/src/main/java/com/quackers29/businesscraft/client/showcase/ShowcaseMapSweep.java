package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.ui.modal.specialized.TownMapModal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Opens the town map modal once and captures a PNG (used by {@code map-sweep} showcase mode).
 */
public final class ShowcaseMapSweep {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final int JOIN_DELAY_TICKS = 40;
    private static final List<String> MAP_ROUTE = List.of(
            "open-town",
            "wait-for-screen:TownInterfaceScreen:160",
            "set-tab:overview",
            "wait-tab:overview",
            "press-button:id:" + ShowcaseButtonIds.MAP_VIEW,
            "wait-for-screen:TownMapModal:160");

    private enum Phase {
        JOIN,
        BUILD,
        ROUTE,
        CAPTURE,
        QUIT
    }

    private static volatile Phase phase = Phase.JOIN;
    private static int ticks;
    private static int stepIndex;
    private static int waitTicks;
    private static boolean buildQueued;
    private static volatile boolean buildDone;
    private static volatile BlockPos townPos = BlockPos.ZERO;
    private static int exitCode;
    private static int quitTicks;

    private ShowcaseMapSweep() {
    }

    public static void reset(Minecraft minecraft) {
        phase = Phase.JOIN;
        ticks = 0;
        stepIndex = 0;
        waitTicks = 0;
        buildQueued = false;
        buildDone = false;
        exitCode = 0;
        quitTicks = 0;
    }

    public static void tick(Minecraft minecraft) {
        if (phase == Phase.QUIT) {
            if (++quitTicks >= 40) {
                minecraft.stop();
                System.exit(exitCode);
            }
            return;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        switch (phase) {
            case JOIN -> {
                if (ticks >= JOIN_DELAY_TICKS) {
                    phase = Phase.BUILD;
                    ticks = 0;
                }
            }
            case BUILD -> {
                if (!buildDone) {
                    if (!buildQueued) {
                        buildQueued = true;
                        MinecraftServer server = minecraft.getSingleplayerServer();
                        server.execute(() -> ShowcaseBuildHelper.buildSingleTown(server, pos -> {
                            townPos = pos;
                            buildDone = true;
                        }, err -> {
                            LOGGER.error("Map sweep build failed: {}", err);
                            exitCode = 1;
                            phase = Phase.QUIT;
                        }));
                    }
                    return;
                }
                ShowcaseSetup.prepareOnce(minecraft.getSingleplayerServer(), townPos);
                phase = Phase.ROUTE;
                ticks = 0;
                stepIndex = 0;
                waitTicks = 0;
            }
            case ROUTE -> runRouteStep(minecraft);
            case CAPTURE -> {
                if (!(minecraft.screen instanceof TownMapModal)) {
                    exitCode = 1;
                    phase = Phase.QUIT;
                    return;
                }
                if (ticks < 15) {
                    return;
                }
                String tag = System.getProperty("businesscraft.showcase.mapTag", "map");
                String file = "showcase_map_" + tag + ".png";
                Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message ->
                        LOGGER.info("Map sweep shot {}: {}", file, message.getString()));
                LOGGER.info("SHOWCASE_MAP_SWEEP_CAPTURED {}", file);
                phase = Phase.QUIT;
            }
            default -> {
            }
        }
    }

    private static void runRouteStep(Minecraft minecraft) {
        if (stepIndex >= MAP_ROUTE.size()) {
            phase = Phase.CAPTURE;
            ticks = 0;
            return;
        }
        String step = MAP_ROUTE.get(stepIndex);
        if (ShowcaseNavigator.isWaitStep(step)) {
            if (ShowcaseNavigator.waitConditionMet(minecraft, step, null)) {
                stepIndex++;
                waitTicks = 0;
            } else if (++waitTicks > ShowcaseNavigator.waitTimeoutTicks(step)) {
                LOGGER.error("Map sweep route timeout on {}", step);
                exitCode = 1;
                phase = Phase.QUIT;
            }
            return;
        }
        String err = ShowcaseNavigator.runStep(minecraft, townPos, step);
        if (err != null) {
            LOGGER.error("Map sweep route failed: {}", err);
            exitCode = 1;
            phase = Phase.QUIT;
            return;
        }
        stepIndex++;
        waitTicks = 0;
    }
}
