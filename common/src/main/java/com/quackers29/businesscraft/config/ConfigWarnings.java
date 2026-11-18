package com.quackers29.businesscraft.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects problems found in the editable CSV config files. Each problem is logged once at WARN
 * and kept so tests (and tooling) can assert a config is clean. Cleared at the start of every
 * registry reload by {@link ConfigLoader}.
 */
public final class ConfigWarnings {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigWarnings.class);
    private static final List<String> WARNINGS = Collections.synchronizedList(new ArrayList<>());

    private ConfigWarnings() {
    }

    public static void clear() {
        WARNINGS.clear();
    }

    /** Problem tied to a file row (1-based line number). */
    public static void warn(String file, int line, String message) {
        add(file + " line " + line + ": " + message);
    }

    /** Problem tied to a named entry (recipe, node, kit) rather than a line. */
    public static void warn(String file, String entry, String message) {
        add(file + " [" + entry + "]: " + message);
    }

    private static void add(String text) {
        WARNINGS.add(text);
        LOGGER.warn("Config problem: {}", text);
    }

    public static List<String> all() {
        synchronized (WARNINGS) {
            return List.copyOf(WARNINGS);
        }
    }
}
