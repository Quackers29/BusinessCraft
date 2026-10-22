package com.quackers29.businesscraft.client.showcase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
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
 * Walks {@link ShowcaseRouteRegistry} routes, captures one PNG per view, and writes a manifest.
 */
public final class ShowcaseAllViews {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final int SETTLE_TICKS = 25;
    private static final int BETWEEN_ROUTES_TICKS = 10;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private enum Phase {
        STEPS,
        SETTLE,
        GAP
    }

    private static List<ShowcaseRoute> routes = List.of();
    private static int routeIndex;
    private static int stepIndex;
    private static int waitTicks;
    private static int settleTicks;
    private static int gapTicks;
    private static Phase phase = Phase.STEPS;
    private static String stepFailure;
    private static final List<Map<String, Object>> manifestEntries = new ArrayList<>();
    private static boolean quitting;
    private static int quitTicks;
    private static int saved;
    private static int exitCode;
    private static boolean initialized;

    private ShowcaseAllViews() {
    }

    public static void reset(Minecraft minecraft) {
        routes = ShowcaseRouteRegistry.allRoutes();
        routeIndex = 0;
        stepIndex = 0;
        waitTicks = 0;
        settleTicks = 0;
        gapTicks = 0;
        phase = Phase.STEPS;
        stepFailure = null;
        manifestEntries.clear();
        quitting = false;
        quitTicks = 0;
        saved = 0;
        exitCode = 0;
        initialized = true;
        deleteManifest(minecraft);
        LOGGER.info("Showcase all-views: {} routes", routes.size());
    }

    public static void tick(Minecraft minecraft, BlockPos townPos) {
        if (!initialized) {
            reset(minecraft);
        }
        if (stepFailure != null || exitCode != 0) {
            finish(minecraft);
            return;
        }
        if (quitting) {
            shutdown(minecraft);
            return;
        }
        if (routeIndex >= routes.size()) {
            writeManifest(minecraft, true);
            quitting = true;
            return;
        }
        if (gapTicks > 0) {
            gapTicks--;
            return;
        }
        ShowcaseRoute route = routes.get(routeIndex);
        switch (phase) {
            case STEPS -> runSteps(minecraft, townPos, route);
            case SETTLE -> runSettle(minecraft, route);
            case GAP -> {
            }
        }
    }

    private static void runSteps(Minecraft minecraft, BlockPos townPos, ShowcaseRoute route) {
        if (stepIndex >= route.steps().size()) {
            phase = Phase.SETTLE;
            settleTicks = SETTLE_TICKS;
            return;
        }
        String step = route.steps().get(stepIndex);
        if (ShowcaseNavigator.isWaitStep(step)) {
            if (ShowcaseNavigator.waitConditionMet(minecraft, step, null)) {
                stepIndex++;
                waitTicks = 0;
            } else if (++waitTicks > ShowcaseNavigator.waitTimeoutTicks(step)) {
                failure(minecraft, route.name(), "timeout on " + step);
            }
            return;
        }
        String err = ShowcaseNavigator.runStep(minecraft, townPos, step);
        if (err != null) {
            failure(minecraft, route.name(), err);
            return;
        }
        stepIndex++;
        waitTicks = 0;
    }

    private static void runSettle(Minecraft minecraft, ShowcaseRoute route) {
        if (settleTicks-- > 0) {
            return;
        }
        String pngName = route.fileBaseName() + ".png";
        Screenshot.grab(minecraft.gameDirectory, pngName, minecraft.getMainRenderTarget(), message ->
                LOGGER.info("Showcase view {}: {}", pngName, message.getString()));
        saved++;
        recordRoute(minecraft, route, pngName, true, null);
        LOGGER.info("Showcase captured route {} ({}/{})", route.name(), routeIndex + 1, routes.size());
        minecraft.setScreen(null);
        routeIndex++;
        stepIndex = 0;
        waitTicks = 0;
        phase = Phase.STEPS;
        gapTicks = BETWEEN_ROUTES_TICKS;
        if (routeIndex >= routes.size()) {
            writeManifest(minecraft, true);
            quitting = true;
        }
    }

    private static void failure(Minecraft minecraft, String routeName, String reason) {
        LOGGER.error("Showcase route {} failed: {}", routeName, reason);
        recordRoute(minecraft, findRoute(routeName), null, false, reason);
        exitCode = 1;
        stepFailure = reason;
        writeManifest(minecraft, false);
        quitting = true;
    }

    private static ShowcaseRoute findRoute(String name) {
        for (ShowcaseRoute route : routes) {
            if (route.name().equals(name)) {
                return route;
            }
        }
        return new ShowcaseRoute(name, List.of());
    }

    private static void recordRoute(Minecraft minecraft, ShowcaseRoute route, String png, boolean ok, String error) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("route", route.name());
        entry.put("steps", route.steps());
        entry.put("loader", loaderName());
        entry.put("ok", ok);
        if (png != null) {
            entry.put("screenshot", png);
        }
        if (error != null) {
            entry.put("error", error);
        }
        manifestEntries.add(entry);
        writeManifest(minecraft, false);
    }

    private static String loaderName() {
        String loader = System.getProperty("businesscraft.showcase.loader");
        if (loader == null || loader.isBlank()) {
            loader = System.getenv("BUSINESSCRAFT_SHOWCASE_LOADER");
        }
        return loader != null && !loader.isBlank() ? loader : "unknown";
    }

    private static Path manifestPath(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("showcase-views-manifest.json");
    }

    private static void deleteManifest(Minecraft minecraft) {
        try {
            Files.deleteIfExists(manifestPath(minecraft));
        } catch (IOException e) {
            LOGGER.warn("Could not delete old manifest", e);
        }
    }

    private static void writeManifest(Minecraft minecraft, boolean complete) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("loader", loaderName());
        root.put("complete", complete);
        root.put("routes", manifestEntries);
        root.put("registry", ShowcaseRouteRegistry.allRoutes().stream().map(ShowcaseRoute::name).toList());
        try (Writer writer = Files.newBufferedWriter(manifestPath(minecraft), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write showcase manifest", e);
        }
    }

    private static void finish(Minecraft minecraft) {
        if (!quitting) {
            exitCode = 1;
            quitting = true;
            quitTicks = 0;
        }
        shutdown(minecraft);
    }

    private static void shutdown(Minecraft minecraft) {
        if (++quitTicks >= 40) {
            if (exitCode == 0) {
                LOGGER.info("Showcase all-views finished: {} views", saved);
            }
            minecraft.stop();
            System.exit(exitCode);
        }
    }

    public static int savedCount() {
        return saved;
    }

    public static int expectedRouteCount() {
        return ShowcaseRouteRegistry.allRoutes().size();
    }
}
