package com.quackers29.businesscraft.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central debug configuration system for BusinessCraft mod.
 * Controls debug logging output on a per-component basis to reduce log noise.
 */
public class DebugConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/DebugConfig");

    // Global override - when true, enables ALL debug logging regardless of
    // individual flags
    public static final boolean FORCE_ALL_DEBUG = false;

    // Core Systems - High Priority (Milestone 2)
    public static final boolean TOWN_BLOCK_ENTITY = false;
    public static final boolean TOWN_INTERFACE_MENU = false;
    public static final boolean NETWORK_PACKETS = false;

    // UI Management Systems (Milestone 3)
    public static final boolean UI_MANAGERS = false;
    public static final boolean SEARCH_RADIUS_MANAGER = false;
    public static final boolean MODAL_MANAGERS = false;
    public static final boolean TOWN_SCREEN_DEPENDENCIES = false;
    public static final boolean UI_RESOURCES_TAB = false;
    public static final boolean UI_GRID_BUILDER = false;
    public static final boolean UI_STANDARD_TAB_CONTENT = false;

    // Town & Data Systems (Milestone 3)
    public static final boolean TOWN_DATA_SYSTEMS = false;
    public static final boolean TOWN_SERVICE = false;
    public static final boolean TOWN_MANAGER = false;
    public static final boolean NBT_DATA_HELPER = false;
    public static final boolean SYNC_HELPERS = false;
    public static final boolean VISITOR_PROCESSING = false;
    public static final boolean UI_BASE_SCREEN = false;

    // Platform System
    public static final boolean PLATFORM_SYSTEM = false;
    public static final boolean PLATFORM_VISUALIZATION = false;

    // Entity System
    public static final boolean ENTITY_SYSTEM = false;
    public static final boolean TOURIST_ENTITY = false;
    public static final boolean TOURIST_SPAWNING = false;

    // Client Systems
    public static final boolean CLIENT_HANDLERS = false;
    public static final boolean KEY_HANDLERS = false;
    public static final boolean DEBUG_OVERLAY = false;

    // Storage Systems
    public static final boolean STORAGE_OPERATIONS = false;
    public static final boolean TRADE_OPERATIONS = false;
    public static final boolean SMART_GPI_DEBUG = false;

    // Configuration and Initialization
    public static final boolean CONFIG_SYSTEM = false;
    public static final boolean MOD_INITIALIZATION = false;

    // Error Handling
    public static final boolean ERROR_HANDLING = false;

    public static boolean isEnabled(boolean componentFlag) {
        return FORCE_ALL_DEBUG || componentFlag;
    }

    public static void debug(Logger logger, boolean componentFlag, String message, Object... args) {
        if (isEnabled(componentFlag)) {
            logger.info("[DEBUG] " + message, args);
        }
    }

    public static void debug(Logger logger, boolean componentFlag, String message) {
        if (isEnabled(componentFlag)) {
            logger.info("[DEBUG] " + message);
        }
    }

    public static void debug(org.apache.logging.log4j.Logger logger, boolean componentFlag, String message,
            Object... args) {
        if (isEnabled(componentFlag)) {
            logger.info("[DEBUG] " + message, args);
        }
    }

    public static void debug(org.apache.logging.log4j.Logger logger, boolean componentFlag, String message) {
        if (isEnabled(componentFlag)) {
            logger.info("[DEBUG] " + message);
        }
    }

    public static void logActiveDebuggers() {
        if (FORCE_ALL_DEBUG) {
            LOGGER.info("=== BusinessCraft Debug Config ===");
            LOGGER.info("GLOBAL DEBUG OVERRIDE: ENABLED - All debug logging is active");
            LOGGER.info("===================================");
            return;
        }

        boolean anyEnabled = false;
        StringBuilder enabledSystems = new StringBuilder();
        enabledSystems.append("=== BusinessCraft Debug Config ===\n");
        enabledSystems.append("Active debug logging for:\n");

        // Check each system and report if enabled
        if (TOWN_BLOCK_ENTITY) {
            enabledSystems.append("  - TOWN_BLOCK_ENTITY\n");
            anyEnabled = true;
        }
        if (TOWN_INTERFACE_MENU) {
            enabledSystems.append("  - TOWN_INTERFACE_MENU\n");
            anyEnabled = true;
        }
        if (NETWORK_PACKETS) {
            enabledSystems.append("  - NETWORK_PACKETS\n");
            anyEnabled = true;
        }
        if (UI_MANAGERS) {
            enabledSystems.append("  - UI_MANAGERS\n");
            anyEnabled = true;
        }
        if (SEARCH_RADIUS_MANAGER) {
            enabledSystems.append("  - SEARCH_RADIUS_MANAGER\n");
            anyEnabled = true;
        }
        if (MODAL_MANAGERS) {
            enabledSystems.append("  - MODAL_MANAGERS\n");
            anyEnabled = true;
        }
        if (TOWN_SCREEN_DEPENDENCIES) {
            enabledSystems.append("  - TOWN_SCREEN_DEPENDENCIES\n");
            anyEnabled = true;
        }
        if (TOWN_DATA_SYSTEMS) {
            enabledSystems.append("  - TOWN_DATA_SYSTEMS\n");
            anyEnabled = true;
        }
        if (TOWN_SERVICE) {
            enabledSystems.append("  - TOWN_SERVICE\n");
            anyEnabled = true;
        }
        if (TOWN_MANAGER) {
            enabledSystems.append("  - TOWN_MANAGER\n");
            anyEnabled = true;
        }
        if (NBT_DATA_HELPER) {
            enabledSystems.append("  - NBT_DATA_HELPER\n");
            anyEnabled = true;
        }
        if (SYNC_HELPERS) {
            enabledSystems.append("  - SYNC_HELPERS\n");
            anyEnabled = true;
        }
        if (PLATFORM_SYSTEM) {
            enabledSystems.append("  - PLATFORM_SYSTEM\n");
            anyEnabled = true;
        }
        if (PLATFORM_VISUALIZATION) {
            enabledSystems.append("  - PLATFORM_VISUALIZATION\n");
            anyEnabled = true;
        }
        if (ENTITY_SYSTEM) {
            enabledSystems.append("  - ENTITY_SYSTEM\n");
            anyEnabled = true;
        }
        if (TOURIST_ENTITY) {
            enabledSystems.append("  - TOURIST_ENTITY\n");
            anyEnabled = true;
        }
        if (TOURIST_SPAWNING) {
            enabledSystems.append("  - TOURIST_SPAWNING\n");
            anyEnabled = true;
        }
        if (CLIENT_HANDLERS) {
            enabledSystems.append("  - CLIENT_HANDLERS\n");
            anyEnabled = true;
        }
        if (KEY_HANDLERS) {
            enabledSystems.append("  - KEY_HANDLERS\n");
            anyEnabled = true;
        }
        if (DEBUG_OVERLAY) {
            enabledSystems.append("  - DEBUG_OVERLAY\n");
            anyEnabled = true;
        }
        if (STORAGE_OPERATIONS) {
            enabledSystems.append("  - STORAGE_OPERATIONS\n");
            anyEnabled = true;
        }
        if (TRADE_OPERATIONS) {
            enabledSystems.append("  - TRADE_OPERATIONS\n");
            anyEnabled = true;
        }
        if (CONFIG_SYSTEM) {
            enabledSystems.append("  - CONFIG_SYSTEM\n");
            anyEnabled = true;
        }
        if (MOD_INITIALIZATION) {
            enabledSystems.append("  - MOD_INITIALIZATION\n");
            anyEnabled = true;
        }
        if (ERROR_HANDLING) {
            enabledSystems.append("  - ERROR_HANDLING\n");
            anyEnabled = true;
        }

        if (anyEnabled) {
            enabledSystems.append("===================================");
            LOGGER.info(enabledSystems.toString());
        } else {
            LOGGER.info("=== BusinessCraft Debug Config ===");
            LOGGER.info("No debug logging enabled (clean logs mode)");
            LOGGER.info("===================================");
        }
    }

    public static boolean enableDebugCategory(String category) {
        LOGGER.info("Debug category toggle requested for: {} (requires restart to take effect)", category);
        return false;
    }

    public static int getEnabledFlagCount() {
        if (FORCE_ALL_DEBUG) {
            return -1; // Special value indicating all flags are enabled
        }

        int count = 0;
        if (TOWN_BLOCK_ENTITY)
            count++;
        if (TOWN_INTERFACE_MENU)
            count++;
        if (NETWORK_PACKETS)
            count++;
        if (UI_MANAGERS)
            count++;
        if (SEARCH_RADIUS_MANAGER)
            count++;
        if (MODAL_MANAGERS)
            count++;
        if (TOWN_SCREEN_DEPENDENCIES)
            count++;
        if (TOWN_DATA_SYSTEMS)
            count++;
        if (TOWN_SERVICE)
            count++;
        if (TOWN_MANAGER)
            count++;
        if (NBT_DATA_HELPER)
            count++;
        if (SYNC_HELPERS)
            count++;
        if (PLATFORM_SYSTEM)
            count++;
        if (PLATFORM_VISUALIZATION)
            count++;
        if (ENTITY_SYSTEM)
            count++;
        if (TOURIST_ENTITY)
            count++;
        if (TOURIST_SPAWNING)
            count++;
        if (CLIENT_HANDLERS)
            count++;
        if (KEY_HANDLERS)
            count++;
        if (DEBUG_OVERLAY)
            count++;
        if (STORAGE_OPERATIONS)
            count++;
        if (TRADE_OPERATIONS)
            count++;
        if (CONFIG_SYSTEM)
            count++;
        if (MOD_INITIALIZATION)
            count++;
        if (ERROR_HANDLING)
            count++;

        return count;
    }
}
