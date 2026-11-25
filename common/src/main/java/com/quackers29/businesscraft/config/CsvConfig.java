package com.quackers29.businesscraft.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for the editable CSV config files ({@code items.csv}, {@code productions.csv},
 * {@code upgrades.csv}, {@code biomes.csv}): installing the bundled copy on a fresh install and
 * splitting rows with the same rules everywhere.
 */
public final class CsvConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvConfig.class);

    /** Classpath folder holding the bundled default config files. */
    public static final String BUNDLED_CONFIG_DIR = "/assets/businesscraft/config/";

    private CsvConfig() {
    }

    /** One data row: the 1-based line number in the file plus its trimmed, unquoted columns. */
    public record Row(int lineNumber, String[] columns) {
    }

    /**
     * Makes sure {@code <configDir>/businesscraft/<fileName>} exists by copying the bundled
     * default from the jar when missing. Existing files are never overwritten.
     *
     * @return the config file (it may still not exist if the bundled copy is missing or unwritable)
     */
    public static File ensureInstalled(Path configDir, String fileName) {
        File target = configDir.resolve("businesscraft").resolve(fileName).toFile();
        if (target.exists()) {
            return target;
        }
        try (InputStream in = CsvConfig.class.getResourceAsStream(BUNDLED_CONFIG_DIR + fileName)) {
            if (in == null) {
                LOGGER.error("Bundled default {} is missing from the jar; cannot install it", fileName);
                return target;
            }
            Files.createDirectories(target.toPath().getParent());
            Files.copy(in, target.toPath());
            LOGGER.info("Installed default {}", target);
        } catch (IOException e) {
            LOGGER.error("Failed to install default {}", fileName, e);
        }
        return target;
    }

    /**
     * Quote-aware comma split. Commas inside double quotes do not split, trailing empty columns
     * are kept, every column is trimmed and one pair of enclosing quotes is removed.
     */
    public static String[] splitLine(String line) {
        String[] raw = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        for (int i = 0; i < raw.length; i++) {
            String col = raw[i].trim();
            if (col.length() >= 2 && col.startsWith("\"") && col.endsWith("\"")) {
                col = col.substring(1, col.length() - 1).trim();
            }
            raw[i] = col;
        }
        return raw;
    }

    /** Reads all data rows: skips the header (first line), blank lines and {@code #} comment lines. */
    public static List<Row> readRows(File file) throws IOException {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                rows.add(new Row(lineNumber, splitLine(trimmed)));
            }
        }
        return rows;
    }
}
