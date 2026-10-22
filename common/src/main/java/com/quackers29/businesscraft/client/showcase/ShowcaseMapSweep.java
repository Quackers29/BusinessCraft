package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.ui.modal.specialized.TownMapModal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the town map modal once and captures a PNG (used by {@code map-sweep} showcase mode).
 */
public final class ShowcaseMapSweep {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final int JOIN_DELAY_TICKS = 40;
    private static final int SETTLE_TICKS = 80;

    private enum Phase {
        JOIN,
        BUILD,
        OPEN_MAP,
        WAIT_MAP,
        CAPTURE,
        QUIT
    }

    private static volatile Phase phase = Phase.JOIN;
    private static int ticks;
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
                phase = Phase.OPEN_MAP;
                ticks = 0;
            }
            case OPEN_MAP -> {
                if (ticks == 1) {
                    ShowcaseNavigator.runStep(minecraft, townPos, "open-town");
                } else if (ticks == 10) {
                    ShowcaseNavigator.runStep(minecraft, townPos, "set-tab:overview");
                } else if (ticks == 15) {
                    ShowcaseNavigator.runStep(minecraft, townPos, "press-button:id:" + ShowcaseButtonIds.MAP_VIEW);
                } else if (ticks >= SETTLE_TICKS) {
                    phase = Phase.WAIT_MAP;
                    ticks = 0;
                }
            }
            case WAIT_MAP -> {
                if (minecraft.screen instanceof TownMapModal) {
                    phase = Phase.CAPTURE;
                    ticks = 0;
                } else if (ticks >= 120) {
                    LOGGER.error("Map sweep: TownMapModal did not open (screen={})", minecraft.screen);
                    exitCode = 1;
                    phase = Phase.QUIT;
                }
            }
            case CAPTURE -> {
                if (!(minecraft.screen instanceof TownMapModal)) {
                    exitCode = 1;
                    phase = Phase.QUIT;
                    return;
                }
                if (ticks < 10) {
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
}
