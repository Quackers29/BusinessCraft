package com.quackers29.businesscraft.client.showcase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Planned-shot results for {@code tools/showcase.sh}. One row per {@link Showcase} grab.
 * A callback that is not {@code screenshot.success} marks the shot failed.
 */
public final class ShowcaseShotManifest {
    public static final String FILE_NAME = "showcase-shots.json";
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static String scene = "";
    private static List<String> expected = List.of();
    private static final List<Map<String, Object>> shots = new ArrayList<>();
    private static final AtomicInteger pending = new AtomicInteger();
    private static boolean failed;

    private ShowcaseShotManifest() {
    }

    public static void begin(Minecraft minecraft, String sceneName, List<String> writtenShots) {
        scene = sceneName == null ? "" : sceneName;
        expected = List.copyOf(writtenShots);
        shots.clear();
        pending.set(0);
        failed = false;
        write(minecraft);
    }

    public static void grab(Minecraft minecraft, String name) {
        String file = "showcase_" + name + ".png";
        pending.incrementAndGet();
        try {
            Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message ->
                    Minecraft.getInstance().execute(() -> finishGrab(name, file, message)));
        } catch (RuntimeException exception) {
            pending.decrementAndGet();
            record(name, file, false, exception.toString());
            failed = true;
            write(minecraft);
            LOGGER.error("Showcase shot {} failed to start", file, exception);
        }
    }

    public static int pending() {
        return pending.get();
    }

    public static boolean hasFailure() {
        return failed;
    }

    private static void finishGrab(String name, String file, Component message) {
        boolean ok = message.getContents() instanceof TranslatableContents contents
                && "screenshot.success".equals(contents.getKey());
        String error = ok ? null : message.getString();
        record(name, file, ok, error);
        if (!ok) {
            failed = true;
            LOGGER.error("Showcase shot {} failed: {}", file, error);
        } else {
            LOGGER.info("Showcase shot {}: {}", file, message.getString());
        }
        pending.decrementAndGet();
        write(Minecraft.getInstance());
    }

    private static void record(String name, String file, boolean ok, String error) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("file", file);
        row.put("ok", ok);
        if (error != null) {
            row.put("error", error);
        }
        shots.add(row);
    }

    private static void write(Minecraft minecraft) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("scene", scene);
        root.put("expected", expected);
        root.put("shots", shots);
        root.put("pending", pending.get());
        root.put("ok", !failed && pending.get() == 0 && shots.size() == expected.size());
        try (Writer writer = Files.newBufferedWriter(
                minecraft.gameDirectory.toPath().resolve(FILE_NAME), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            failed = true;
            LOGGER.error("Failed to write shot manifest", e);
        }
    }
}
