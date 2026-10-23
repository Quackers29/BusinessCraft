package com.quackers29.businesscraft.client.showcase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quackers29.businesscraft.client.showcase.testbed.ShowcaseTestbedCheck;
import com.quackers29.businesscraft.client.showcase.testbed.ShowcaseTestbedCheckRegistry;
import com.quackers29.businesscraft.client.showcase.testbed.ShowcaseTestbedContext;
import com.quackers29.businesscraft.client.showcase.testbed.ShowcaseTwoTownSetup;
import net.minecraft.client.Minecraft;
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
 * Dev testbed: two-town world + pluggable checks ({@link ShowcaseTestbedCheckRegistry}).
 */
public final class ShowcaseTestbed {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int JOIN_DELAY_TICKS = 40;
    private static final String CHECKS_PROPERTY = "businesscraft.showcase.testbed.checks";

    private enum Phase {
        JOIN,
        SETUP,
        RUN_CHECKS,
        QUIT
    }

    private static volatile Phase phase = Phase.JOIN;
    private static int joinTicks;
    private static boolean setupStarted;
    private static final ShowcaseTestbedContext context = new ShowcaseTestbedContext();
    private static List<ShowcaseTestbedCheck> checks = List.of();
    private static int checkIndex;
    private static ShowcaseTestbedCheck activeCheck;
    private static final List<Map<String, Object>> results = new ArrayList<>();
    private static int quitTicks;
    private static int exitCode;

    private ShowcaseTestbed() {
    }

    public static void reset(Minecraft minecraft) {
        phase = Phase.JOIN;
        joinTicks = 0;
        setupStarted = false;
        checkIndex = 0;
        activeCheck = null;
        results.clear();
        quitTicks = 0;
        exitCode = 0;
        String property = System.getProperty(CHECKS_PROPERTY, "all");
        checks = ShowcaseTestbedCheckRegistry.resolve(property);
        LOGGER.info("Showcase testbed checks: {}", checks.stream().map(ShowcaseTestbedCheck::name).toList());
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
        switch (phase) {
            case JOIN -> {
                if (++joinTicks >= JOIN_DELAY_TICKS) {
                    phase = Phase.SETUP;
                }
            }
            case SETUP -> {
                if (!setupStarted) {
                    setupStarted = true;
                    minecraft.getSingleplayerServer().execute(() -> {
                        boolean ok = ShowcaseTwoTownSetup.build(minecraft.getSingleplayerServer(), context);
                        if (!ok) {
                            recordCheck("_setup", false, Map.of("error", "two-town setup failed"));
                            failSetup();
                        } else {
                            recordCheck("_setup", true, context.snapshot());
                            phase = Phase.RUN_CHECKS;
                            startNextCheck(minecraft);
                        }
                    });
                }
            }
            case RUN_CHECKS -> {
                if (activeCheck == null) {
                    phase = Phase.QUIT;
                    return;
                }
                if (activeCheck.tick(minecraft, context)) {
                    boolean pass = activeCheck.passed();
                    recordCheck(activeCheck.name(), pass, activeCheck.details());
                    if (!pass) {
                        exitCode = 1;
                        LOGGER.error("SHOWCASE_TESTBED_FAIL check={} {}", activeCheck.name(),
                                activeCheck.failureMessage());
                    }
                    checkIndex++;
                    startNextCheck(minecraft);
                }
            }
            default -> {
            }
        }
    }

    private static void startNextCheck(Minecraft minecraft) {
        if (checkIndex >= checks.size()) {
            activeCheck = null;
            phase = Phase.QUIT;
            return;
        }
        activeCheck = checks.get(checkIndex);
        activeCheck.begin(minecraft, context);
        LOGGER.info("SHOWCASE_TESTBED_BEGIN {}", activeCheck.name());
    }

    private static void failSetup() {
        exitCode = 1;
        phase = Phase.QUIT;
    }

    private static void recordCheck(String name, boolean pass, Map<String, Object> details) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("check", name);
        entry.put("pass", pass);
        entry.put("details", details);
        results.add(entry);
        LOGGER.info("SHOWCASE_TESTBED_STEP {} {}", name, pass ? "PASS" : "FAIL");
    }

    private static void writeReport(Minecraft minecraft) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("loader", loaderName());
        report.put("checksRequested", checks.stream().map(ShowcaseTestbedCheck::name).toList());
        report.put("world", context.snapshot());
        report.put("results", results);
        report.put("exitCode", exitCode);
        Path path = minecraft.gameDirectory.toPath().resolve("showcase-testbed-report.json");
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(report, w);
        } catch (IOException e) {
            LOGGER.error("Failed to write testbed report", e);
        }
    }

    private static String loaderName() {
        String loader = System.getProperty("businesscraft.showcase.loader");
        return loader != null && !loader.isBlank() ? loader : "unknown";
    }
}
