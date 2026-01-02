package com.quackers29.businesscraft.config;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.logging.LogUtils;
import com.quackers29.businesscraft.util.Result;

public class ConfigLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    // INSTANCE moved to bottom to ensure all static fields are initialized first

    // Vehicle-related config
    public static boolean enableCreateTrains = true;
    public static boolean enableMinecarts = true;
    public static int vehicleSearchRadius = 3;
    public static double minecartStopThreshold = 0.001;

    // Town-related config
    public static List<String> townNames = new ArrayList<>();

    public static int minPopForTourists = 5;
    public static int minDistanceBetweenTowns = 100; // Minimum distance between towns in blocks
    public static int defaultStartingPopulation = 5; // Default starting population for new towns
    public static int maxTouristsPerTown = 10; // Maximum number of tourists per town
    public static int populationPerTourist = 10; // Population required for each tourist (1 tourist per 10 population)
    public static int maxPopBasedTourists = 20; // Maximum population-based tourists (200 pop = 20 tourists)

    // Tourist-related config
    public static double touristExpiryMinutes = 120.0; // Tourist expiry time in minutes (default: 120 = 2 hours)
    public static boolean enableTouristExpiry = true; // Whether tourist expiry is enabled
    public static boolean notifyOnTouristDeparture = true; // Whether to notify origin town when tourist quits or dies

    // Tourism economy config
    public static int metersPerEmerald = 50; // How many meters a tourist needs to travel to earn 1 emerald

    // Distance milestone config
    public static boolean enableMilestones = true; // Whether distance milestone rewards are enabled
    public static Map<Integer, List<String>> milestoneRewards = new HashMap<>(); // Distance -> List of "item:count"
                                                                                 // rewards

    // Player tracking config
    public static boolean playerTracking = true; // Master toggle for all player tracking functionality
    public static boolean townBoundaryMessages = true; // Whether to show town entry/exit messages

    // Trading config
    public static boolean tradingEnabled = true;
    public static int tradingTickInterval = 60;
    public static float tradingRestockRate = 0.5f;
    public static float tradingDefaultMaxStock = 1000.0f;

    // Production config
    public static boolean productionEnabled = true;
    public static int productionTickInterval = 100;
    public static int dailyTickInterval = 24000;
    public static int minStockPercent = 60;
    public static int excessStockPercent = 80;

    public static final ConfigLoader INSTANCE = new ConfigLoader();

    private ConfigLoader() {
        loadConfig();
        registerWithHotReload();
    }

    /**
     * Registers this configuration with the hot-reloadable configuration service.
     */
    private void registerWithHotReload() {
        try {
            Path configDir = com.quackers29.businesscraft.api.PlatformAccess.platform.getConfigDirectory();
            Path configPath = configDir.resolve("businesscraft.properties");
            ConfigurationService.getInstance().registerConfiguration(
                    "businesscraft-main",
                    configPath,
                    this::reloadFromFile);
            LOGGER.info("Registered BusinessCraft configuration for hot-reloading");
        } catch (Exception e) {
            LOGGER.warn("Failed to register configuration for hot-reloading: {}", e.getMessage());
        }
    }

    /**
     * Reloads configuration from the specified file path.
     * Used as a callback for the hot-reload service.
     */
    private void reloadFromFile(Path filePath) {
        LOGGER.info("Hot-reloading configuration from: {}", filePath);
        loadConfig();
    }

    public static void loadConfig() {
        Properties props = new Properties();

        try {
            Path configDir = com.quackers29.businesscraft.api.PlatformAccess.platform.getConfigDirectory();
            File configFile = configDir.resolve("businesscraft.properties").toFile();

            if (!configFile.exists()) {
                if (!configFile.getParentFile().exists()) {
                    configFile.getParentFile().mkdirs();
                }
                saveConfig();
                return;
            }

            FileReader reader = new FileReader(configFile);
            props.load(reader);
            reader.close();

            // Vehicle settings
            enableCreateTrains = Boolean.parseBoolean(props.getProperty("enableCreateTrains", "true"));
            enableMinecarts = Boolean.parseBoolean(props.getProperty("enableMinecarts", "true"));
            vehicleSearchRadius = Integer.parseInt(props.getProperty("vehicleSearchRadius", "3"));
            minecartStopThreshold = Double.parseDouble(props.getProperty("minecartStopThreshold", "0.001"));

            // Town settings

            minPopForTourists = Integer.parseInt(props.getProperty("minPopForTourists", "5"));
            minDistanceBetweenTowns = Integer.parseInt(props.getProperty("minDistanceBetweenTowns", "100"));
            defaultStartingPopulation = Integer.parseInt(props.getProperty("defaultStartingPopulation", "5"));
            maxTouristsPerTown = Integer.parseInt(props.getProperty("maxTouristsPerTown", "10"));
            populationPerTourist = Integer.parseInt(props.getProperty("populationPerTourist", "10"));
            maxPopBasedTourists = Integer.parseInt(props.getProperty("maxPopBasedTourists", "20"));

            // Load tourist-related config
            touristExpiryMinutes = Double.parseDouble(props.getProperty("touristExpiryMinutes", "120.0"));
            enableTouristExpiry = Boolean.parseBoolean(props.getProperty("enableTouristExpiry", "true"));
            notifyOnTouristDeparture = Boolean.parseBoolean(props.getProperty("notifyOnTouristDeparture", "true"));

            // Load tourism economy config
            metersPerEmerald = Integer.parseInt(props.getProperty("metersPerEmerald", "50"));

            // Load milestone config
            enableMilestones = Boolean.parseBoolean(props.getProperty("enableMilestones", "true"));
            loadMilestoneRewards(props);

            // Load player tracking config
            playerTracking = Boolean.parseBoolean(props.getProperty("playerTracking", "true"));
            townBoundaryMessages = Boolean.parseBoolean(props.getProperty("townBoundaryMessages", "true"));

            // Load town names
            String namesStr = props.getProperty("townNames", "");
            townNames = new ArrayList<>(Arrays.asList(namesStr.split(",")));
            if (townNames.isEmpty()) {
                townNames.addAll(Arrays.asList("Riverside", "Hillcrest", "Meadowbrook", "Oakville"));
            }

            // Load trading config
            tradingEnabled = Boolean.parseBoolean(props.getProperty("tradingEnabled", "true"));
            tradingTickInterval = Integer.parseInt(props.getProperty("tradingTickInterval", "60"));
            tradingRestockRate = Float.parseFloat(props.getProperty("tradingRestockRate", "0.5"));
            tradingDefaultMaxStock = Float.parseFloat(props.getProperty("tradingDefaultMaxStock", "1000.0"));

            // Load production config
            productionEnabled = Boolean.parseBoolean(props.getProperty("productionEnabled", "true"));
            productionTickInterval = Integer.parseInt(props.getProperty("productionTickInterval", "100"));
            minStockPercent = Integer.parseInt(props.getProperty("minStockPercent", "60"));
            excessStockPercent = Integer.parseInt(props.getProperty("excessStockPercent", "80"));

            // Load Registries
            com.quackers29.businesscraft.economy.ResourceRegistry.load();
            com.quackers29.businesscraft.production.ProductionRegistry.load();
            com.quackers29.businesscraft.production.UpgradeRegistry.load();
            com.quackers29.businesscraft.world.BiomeRegistry.load();

        } catch (IOException e) {
            LOGGER.error("Failed to load config: {}", e.getMessage());
        }

        // Log settings
        LOGGER.info("Enable Create Trains: {}", enableCreateTrains);
        LOGGER.info("Enable Minecarts: {}", enableMinecarts);
        LOGGER.info("Vehicle Search Radius: {}", vehicleSearchRadius);

        LOGGER.info("Min Pop For Tourists: {}", minPopForTourists);
        LOGGER.info("Min Distance Between Towns: {}", minDistanceBetweenTowns);
        LOGGER.info("Default Starting Population: {}", defaultStartingPopulation);
        LOGGER.info("Max Tourists Per Town: {}", maxTouristsPerTown);
        LOGGER.info("Population Per Tourist: {}", populationPerTourist);
        LOGGER.info("Max Population-based Tourists: {}", maxPopBasedTourists);
        LOGGER.info("Town Names: {}", townNames);

        // Log tourist-related settings
        LOGGER.info("Tourist Expiry Minutes: {}", touristExpiryMinutes);
        LOGGER.info("Enable Tourist Expiry: {}", enableTouristExpiry);
        LOGGER.info("Notify On Tourist Departure: {}", notifyOnTouristDeparture);

        LOGGER.info("Meters Per Emerald: {}", metersPerEmerald);
        LOGGER.info("Enable Milestones: {}", enableMilestones);
        LOGGER.info("Milestone Rewards: {}", milestoneRewards.size() + " configured");

        // Log player tracking settings
        LOGGER.info("Player Tracking: {}", playerTracking);
        LOGGER.info("Town Boundary Messages: {}", townBoundaryMessages);
    }

    public static void saveConfig() {
        Properties props = new Properties();

        // Vehicle settings
        props.setProperty("enableCreateTrains", String.valueOf(enableCreateTrains));
        props.setProperty("enableMinecarts", String.valueOf(enableMinecarts));
        props.setProperty("vehicleSearchRadius", String.valueOf(vehicleSearchRadius));
        props.setProperty("minecartStopThreshold", String.valueOf(minecartStopThreshold));

        // Town settings

        props.setProperty("minPopForTourists", String.valueOf(minPopForTourists));
        props.setProperty("minDistanceBetweenTowns", String.valueOf(minDistanceBetweenTowns));
        props.setProperty("defaultStartingPopulation", String.valueOf(defaultStartingPopulation));
        props.setProperty("maxTouristsPerTown", String.valueOf(maxTouristsPerTown));
        props.setProperty("populationPerTourist", String.valueOf(populationPerTourist));
        props.setProperty("maxPopBasedTourists", String.valueOf(maxPopBasedTourists));
        props.setProperty("townNames", String.join(",", townNames));

        // Save tourist-related config
        props.setProperty("touristExpiryMinutes", String.valueOf(touristExpiryMinutes));
        props.setProperty("enableTouristExpiry", String.valueOf(enableTouristExpiry));
        props.setProperty("notifyOnTouristDeparture", String.valueOf(notifyOnTouristDeparture));

        // Save tourism economy config
        props.setProperty("metersPerEmerald", String.valueOf(metersPerEmerald));

        // Save milestone config
        props.setProperty("enableMilestones", String.valueOf(enableMilestones));
        saveMilestoneRewards(props);

        // Save player tracking config
        props.setProperty("playerTracking", String.valueOf(playerTracking));
        props.setProperty("townBoundaryMessages", String.valueOf(townBoundaryMessages));

        // Save trading config
        props.setProperty("tradingEnabled", String.valueOf(tradingEnabled));
        props.setProperty("tradingTickInterval", String.valueOf(tradingTickInterval));
        props.setProperty("tradingRestockRate", String.valueOf(tradingRestockRate));
        props.setProperty("tradingDefaultMaxStock", String.valueOf(tradingDefaultMaxStock));

        // Save production config
        props.setProperty("productionEnabled", String.valueOf(productionEnabled));
        props.setProperty("productionTickInterval", String.valueOf(productionTickInterval));
        props.setProperty("minStockPercent", String.valueOf(minStockPercent));
        props.setProperty("excessStockPercent", String.valueOf(excessStockPercent));

        try {
            Path configDir = com.quackers29.businesscraft.api.PlatformAccess.platform.getConfigDirectory();
            File configFile = configDir.resolve("businesscraft.properties").toFile();

            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            FileWriter writer = new FileWriter(configFile);
            props.store(writer, "BusinessCraft Configuration");
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    /**
     * Loads milestone rewards from properties file.
     * Expected format: milestone1_distance=10,
     * milestone1_rewards=minecraft:bread:1,minecraft:experience_bottle:2
     */
    private static void loadMilestoneRewards(Properties props) {
        // Ensure milestoneRewards is initialized
        if (milestoneRewards == null) {
            milestoneRewards = new HashMap<>();
        }
        milestoneRewards.clear();

        // Set default milestone for testing if none exist
        boolean hasAnyMilestone = false;
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("milestone") && key.endsWith("_distance")) {
                hasAnyMilestone = true;
                break;
            }
        }

        if (!hasAnyMilestone) {
            // Add default test milestone: 10m distance with bread and XP bottle
            List<String> defaultRewards = Arrays.asList("minecraft:bread:1", "minecraft:experience_bottle:2");
            milestoneRewards.put(10, defaultRewards);
            LOGGER.info("Added default test milestone: 10m -> bread + XP bottle");
            return;
        }

        // Load configured milestones
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("milestone") && key.endsWith("_distance")) {
                try {
                    // Extract milestone number from key (e.g., "milestone1_distance" -> "1")
                    String milestoneNum = key.substring(9, key.length() - 9); // Remove "milestone" and "_distance"
                    int distance = Integer.parseInt(props.getProperty(key));

                    // Look for corresponding rewards
                    String rewardsKey = "milestone" + milestoneNum + "_rewards";
                    String rewardsStr = props.getProperty(rewardsKey, "");

                    if (!rewardsStr.isEmpty()) {
                        List<String> rewards = Arrays.asList(rewardsStr.split(","));
                        milestoneRewards.put(distance, rewards);
                        LOGGER.info("MILESTONE CONFIG - Loaded milestone: {}m -> {}", distance, rewards);
                    } else {
                        LOGGER.warn("MILESTONE CONFIG - Milestone distance {} found but no rewards configured",
                                distance);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid milestone distance in config: {}", key);
                }
            }
        }
    }

    /**
     * Saves milestone rewards to properties file.
     */
    private static void saveMilestoneRewards(Properties props) {
        // Ensure milestoneRewards is initialized
        if (milestoneRewards == null) {
            milestoneRewards = new HashMap<>();
        }

        int milestoneIndex = 1;
        for (Map.Entry<Integer, List<String>> entry : milestoneRewards.entrySet()) {
            props.setProperty("milestone" + milestoneIndex + "_distance", String.valueOf(entry.getKey()));
            props.setProperty("milestone" + milestoneIndex + "_rewards", String.join(",", entry.getValue()));
            milestoneIndex++;
        }
    }
}
