package com.yourdomain.businesscraft.config;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_PATH = "config/businesscraft.properties";

    // Default configuration values
    public static int breadPerPop = 10;
    public static int minPopForTourists = 10;
    public static List<String> townNames = Arrays.asList(
            "Springfield", "Rivertown", "Maplewood", "Lakeside", "Greenfield");
    public static int breadForNewVillager = 64;

    public static void loadConfig() {
        File configFile = new File(CONFIG_PATH);
        File configDir = configFile.getParentFile();

        try {
            if (!configDir.exists() && !configDir.mkdirs()) {
                LOGGER.error("Failed to create config directory");
                return;
            }

            Properties props = new Properties();

            if (!configFile.exists()) {
                LOGGER.info("Creating default configuration file");
                createDefaultConfig(configFile);
            }

            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }

            // Load configuration values
            loadValues(props);
            logConfiguration();

        } catch (Exception e) {
            LOGGER.error("Failed to load configuration: {}", e.getMessage());
            LOGGER.info("Using default values");
        }
    }

    private static void loadValues(Properties props) {
        breadPerPop = Integer.parseInt(props.getProperty("breadPerPop", String.valueOf(breadPerPop)));
        minPopForTourists = Integer.parseInt(props.getProperty("minPopForTourists", String.valueOf(minPopForTourists)));
        String townNamesStr = props.getProperty("townNames");
        if (townNamesStr != null && !townNamesStr.isEmpty()) {
            townNames = Arrays.asList(townNamesStr.split(","));
        }
        breadForNewVillager = Integer.parseInt(props.getProperty("breadForNewVillager", String.valueOf(breadForNewVillager)));
    }

    private static void logConfiguration() {
        LOGGER.info("Configuration loaded:");
        LOGGER.info("Bread per Population: {}", breadPerPop);
        LOGGER.info("Minimum Population for Tourists: {}", minPopForTourists);
        LOGGER.info("Town Names: {}", townNames);
        LOGGER.info("Bread for New Villager: {}", breadForNewVillager);
    }

    private static void createDefaultConfig(File configFile) throws IOException {
        Properties props = new Properties();
        props.setProperty("breadPerPop", String.valueOf(breadPerPop));
        props.setProperty("minPopForTourists", String.valueOf(minPopForTourists));
        props.setProperty("townNames", String.join(",", townNames));
        props.setProperty("breadForNewVillager", String.valueOf(breadForNewVillager));

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "BusinessCraft Configuration File");
        }
    }
}