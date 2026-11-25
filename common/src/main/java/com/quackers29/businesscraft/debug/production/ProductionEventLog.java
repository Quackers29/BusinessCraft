package com.quackers29.businesscraft.debug.production;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes one CSV per run and a per-town summary when the run ends.
 * Registered as the single {@link ProductionEventListener} while the toggle is on.
 */
public final class ProductionEventLog implements ProductionEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionEventLog.class);
    public static final List<String> COLUMNS = List.of(
            "real_time", "game_tick", "town_name", "town_id", "event_type",
            "subject_id", "amount", "details", "stock");

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter REAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private static ProductionEventLog openLog;

    private final Path directory;
    private final Path eventsFile;
    private final String stamp;
    private final Map<String, TownRollup> towns = new TreeMap<>();
    private BufferedWriter writer;
    private java.io.FileOutputStream stream;
    private int rowsSinceSync;
    private boolean closed;

    private ProductionEventLog(Path directory, String stamp) {
        this.directory = directory;
        this.stamp = stamp;
        this.eventsFile = directory.resolve("production-events-" + stamp + ".csv");
    }

    public static boolean isOpen() {
        return openLog != null && !openLog.closed;
    }

    public static Path eventsFile() {
        return openLog == null ? null : openLog.eventsFile;
    }

    public static Path directory() {
        return openLog == null ? null : openLog.directory;
    }

    public Path eventsPath() {
        return eventsFile;
    }

    public Path summaryCsvPath() {
        return directory.resolve("production-summary-" + stamp + ".csv");
    }

    public Path summaryMarkdownPath() {
        return directory.resolve("production-summary-" + stamp + ".md");
    }

    /**
     * Opens the log for a dedicated or integrated server. Does nothing when the
     * toml toggle (and any run override) is off.
     */
    public static void onServerStarted(Path runDirectory) {
        if (!ConfigLoader.productionEventLog || isOpen()) {
            return;
        }
        Path dir = resolveDirectory(runDirectory);
        try {
            open(dir);
        } catch (IOException e) {
            LOGGER.error("Could not open production event log in {}", dir, e);
        }
    }

    public static void onServerStopping() {
        close();
    }

    public static Path resolveDirectory(Path runDirectory) {
        String configured = ConfigLoader.productionEventLogDir;
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path root = runDirectory == null ? Path.of(".") : runDirectory;
        return root.resolve("businesscraft-debug");
    }

    public static synchronized ProductionEventLog open(Path directory) throws IOException {
        close();
        Files.createDirectories(directory);
        String stamp = STAMP.format(Instant.now());
        ProductionEventLog log = new ProductionEventLog(directory, stamp);
        log.stream = new java.io.FileOutputStream(log.eventsFile.toFile());
        log.writer = new BufferedWriter(new OutputStreamWriter(log.stream, StandardCharsets.UTF_8));
        log.writer.write(Csv.row(COLUMNS.toArray(String[]::new)));
        log.writer.newLine();
        log.writer.flush();
        log.stream.getFD().sync();
        ProductionEventBus.add(log);
        openLog = log;
        LOGGER.info("Production event log: {}", log.eventsFile.toAbsolutePath());
        return log;
    }

    public static synchronized void close() {
        ProductionEventLog log = openLog;
        if (log == null) {
            return;
        }
        openLog = null;
        log.closeAndSummarize();
    }

    /**
     * One snapshot row per loaded town, or the named town. The log must already be open.
     *
     * @return rows written, or -1 when the log is off
     */
    public static int snapshot(MinecraftServer server, String townFilter) {
        if (!isOpen() || server == null) {
            return -1;
        }
        int written = 0;
        String filter = townFilter == null ? "" : townFilter.trim();
        for (ServerLevel level : server.getAllLevels()) {
            TownManager manager = TownManager.get(level);
            if (level != null) {
                ProductionEventContext.setGameTick(level.getGameTime());
            }
            for (Town town : manager.getAllTowns().values()) {
                if (!filter.isEmpty() && !matches(town, filter)) {
                    continue;
                }
                ProductionEvents.snapshot(town);
                written++;
            }
        }
        flushOpen();
        return written;
    }

    private static boolean matches(Town town, String filter) {
        if (town.getName() != null && town.getName().equalsIgnoreCase(filter)) {
            return true;
        }
        return town.getId() != null && town.getId().toString().equalsIgnoreCase(filter);
    }

    private static void flushOpen() {
        ProductionEventLog log = openLog;
        if (log != null) {
            log.sync();
        }
    }

    @Override
    public void onEvent(ProductionEvent event) {
        if (closed || event == null) {
            return;
        }
        try {
            writer.write(Csv.row(
                    REAL_TIME.format(Instant.now()),
                    Long.toString(event.gameTick()),
                    event.townName(),
                    event.townId(),
                    event.eventType(),
                    event.subjectId(),
                    event.amount() == null ? "" : Detail.number(event.amount()),
                    event.details(),
                    event.stock()));
            writer.newLine();
            writer.flush();
            rowsSinceSync++;
            if (rowsSinceSync >= 25) {
                stream.getFD().sync();
                rowsSinceSync = 0;
            }
            absorb(event);
        } catch (IOException e) {
            LOGGER.error("Failed to write production event {}", event.eventType(), e);
        }
    }

    private void sync() {
        try {
            if (writer != null) {
                writer.flush();
            }
            if (stream != null) {
                stream.getFD().sync();
            }
            rowsSinceSync = 0;
        } catch (IOException e) {
            LOGGER.error("Failed to flush production event log", e);
        }
    }

    private void closeAndSummarize() {
        if (closed) {
            return;
        }
        closed = true;
        ProductionEventBus.remove(this);
        try {
            writeSummaries();
        } catch (IOException e) {
            LOGGER.error("Failed to write production event summary", e);
        }
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to close production event log", e);
        }
        writer = null;
        stream = null;
        LOGGER.info("Production event log closed: {}", eventsFile.toAbsolutePath());
    }

    private void writeSummaries() throws IOException {
        Path csv = directory.resolve("production-summary-" + stamp + ".csv");
        Path markdown = directory.resolve("production-summary-" + stamp + ".md");
        List<String> csvRows = new ArrayList<>();
        csvRows.add(Csv.row("town_name", "town_id", "metric", "key", "total"));
        StringBuilder md = new StringBuilder();
        md.append("# Production event summary\n\n");
        md.append("Events: `").append(eventsFile.getFileName()).append("`\n\n");
        if (towns.isEmpty()) {
            md.append("No towns recorded.\n");
        }
        for (TownRollup rollup : towns.values()) {
            md.append("## ").append(rollup.name.isEmpty() ? rollup.id : rollup.name).append("\n\n");
            md.append("- id: `").append(rollup.id).append("`\n");
            md.append("- events: ").append(rollup.events).append("\n\n");
            appendMetric(csvRows, md, rollup, "produced", rollup.produced);
            appendMetric(csvRows, md, rollup, "consumed", rollup.consumed);
            appendCount(csvRows, md, rollup, "research_bought", rollup.researchBought);
            appendMetric(csvRows, md, rollup, "research_cost", rollup.researchCost);
            appendCount(csvRows, md, rollup, "contract", rollup.contracts);
            appendCount(csvRows, md, rollup, "stall", rollup.stalls);
            md.append('\n');
        }
        Files.writeString(csv, String.join(System.lineSeparator(), csvRows) + System.lineSeparator(),
                StandardCharsets.UTF_8);
        Files.writeString(markdown, md.toString(), StandardCharsets.UTF_8);
        LOGGER.info("Production event summary: {}", csv.toAbsolutePath());
    }

    private static void appendMetric(List<String> csvRows, StringBuilder md, TownRollup rollup,
            String metric, Map<String, Double> values) {
        if (values.isEmpty()) {
            return;
        }
        md.append("### ").append(metric).append("\n\n");
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String total = Detail.number(entry.getValue());
            csvRows.add(Csv.row(rollup.name, rollup.id, metric, entry.getKey(), total));
            md.append("- ").append(entry.getKey()).append(": ").append(total).append('\n');
        }
        md.append('\n');
    }

    private static void appendCount(List<String> csvRows, StringBuilder md, TownRollup rollup,
            String metric, Map<String, Long> values) {
        if (values.isEmpty()) {
            return;
        }
        md.append("### ").append(metric).append("\n\n");
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            String total = Long.toString(entry.getValue());
            csvRows.add(Csv.row(rollup.name, rollup.id, metric, entry.getKey(), total));
            md.append("- ").append(entry.getKey()).append(": ").append(total).append('\n');
        }
        md.append('\n');
    }

    private void absorb(ProductionEvent event) {
        String key = event.townId() + "|" + event.townName();
        TownRollup rollup = towns.computeIfAbsent(key, ignored -> new TownRollup(event.townName(), event.townId()));
        rollup.events++;
        Map<String, String> details = parseDetails(event.details());
        switch (event.eventType()) {
            case ProductionEvent.RECIPE_COMPLETED -> {
                addAmounts(rollup.produced, details.get("outputs"));
                addAmounts(rollup.consumed, details.get("inputs"));
            }
            case ProductionEvent.RECIPE_STALLED -> {
                String reason = details.getOrDefault("reason", "");
                if (!reason.isEmpty()) {
                    rollup.stalls.merge(reason, 1L, Long::sum);
                }
            }
            case ProductionEvent.RESEARCH_STARTED -> {
                rollup.researchBought.merge(event.subjectId(), 1L, Long::sum);
                addAmounts(rollup.researchCost, details.get("cost"));
            }
            case ProductionEvent.CONTRACT_LISTED -> rollup.contracts.merge("listed", 1L, Long::sum);
            case ProductionEvent.CONTRACT_BID -> rollup.contracts.merge("bid", 1L, Long::sum);
            case ProductionEvent.CONTRACT_WON -> rollup.contracts.merge("won", 1L, Long::sum);
            case ProductionEvent.CONTRACT_COURIER_DISPATCHED ->
                rollup.contracts.merge("courier_dispatched", 1L, Long::sum);
            case ProductionEvent.CONTRACT_DELIVERED -> rollup.contracts.merge("delivered", 1L, Long::sum);
            case ProductionEvent.CONTRACT_EXPIRED -> rollup.contracts.merge("expired", 1L, Long::sum);
            case ProductionEvent.CONTRACT_CANCELLED -> rollup.contracts.merge("cancelled", 1L, Long::sum);
            default -> {
            }
        }
    }

    static Map<String, String> parseDetails(String details) {
        Map<String, String> pairs = new LinkedHashMap<>();
        if (details == null || details.isEmpty()) {
            return pairs;
        }
        for (String part : details.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            pairs.put(part.substring(0, eq), part.substring(eq + 1));
        }
        return pairs;
    }

    static void addAmounts(Map<String, Double> into, String packed) {
        if (packed == null || packed.isEmpty()) {
            return;
        }
        for (String part : packed.split(",")) {
            int colon = part.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String id = part.substring(0, colon);
            try {
                double amount = Double.parseDouble(part.substring(colon + 1));
                into.merge(id, amount, Double::sum);
            } catch (NumberFormatException ignored) {
                // A malformed detail does not abort the summary.
            }
        }
    }

    private static final class TownRollup {
        final String name;
        final String id;
        long events;
        final Map<String, Double> produced = new TreeMap<>();
        final Map<String, Double> consumed = new TreeMap<>();
        final Map<String, Long> researchBought = new TreeMap<>();
        final Map<String, Double> researchCost = new TreeMap<>();
        final Map<String, Long> contracts = new TreeMap<>();
        final Map<String, Long> stalls = new TreeMap<>();

        TownRollup(String name, String id) {
            this.name = name == null ? "" : name;
            this.id = id == null ? "" : id;
        }
    }
}
