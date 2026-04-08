package com.quackers29.businesscraft.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class ConfigLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE_NAME = "businesscraft.toml";
    private static final String DEFAULT_CONFIG_RESOURCE = "/assets/businesscraft/config/businesscraft.toml";

    // Vehicle-related config
    public static boolean enableCreateTrains = true;
    public static boolean enableMinecarts = true;
    public static int vehicleSearchRadius = 3;
    public static double minecartStopThreshold = 0.001;

    // Town-related config
    public static List<String> townNames = new ArrayList<>();
    public static int minPopForTourists = 5;
    public static int minDistanceBetweenTowns = 100;
    public static int defaultStartingPopulation = 5;
    public static int maxTouristsPerTown = 1000;
    public static int populationPerTourist = 5;
    public static int maxPopBasedTourists = 20;

    // Tourist-related config
    public static double touristExpiryMinutes = 120.0;
    public static boolean enableTouristExpiry = true;
    public static boolean notifyOnTouristDeparture = true;

    // Tourism economy config
    public static int metersPerEmerald = 50;

    // Distance milestone config
    public static boolean enableMilestones = true;
    public static Map<Integer, List<String>> milestoneRewards = new HashMap<>();

    // Player tracking config
    public static boolean playerTracking = true;
    public static boolean townBoundaryMessages = true;

    // Trading config
    public static boolean tradingEnabled = true;
    public static int tradingTickInterval = 60;
    public static float tradingRestockRate = 0.5f;
    public static float tradingDefaultMaxStock = 1000.0f;
    public static String currencyItem = "minecraft:emerald";

    // Production config
    public static boolean productionEnabled = true;
    public static int productionTickInterval = 100;
    public static int dailyTickInterval = 24000;
    public static int minStockPercent = 60;
    public static int excessStockPercent = 80;

    // Contract Timing Config
    public static double contractAuctionDurationMinutes = 1.0;
    public static double contractCourierAcceptanceMinutes = 2.0;
    public static double contractCourierDeliveryMinutesPerMeter = 0.05;
    public static double contractSnailMailDeliveryMinutesPerMeter = 0.1;

    // Display Config
    public static String displayTimezone = "UTC";

    // Phase 11: Global system toggles
    public static boolean touristSystemEnabled = true;
    public static boolean contractsEnabled = true;
    public static boolean researchEnabled = true;

    public static final ConfigLoader INSTANCE = new ConfigLoader();

    private ConfigLoader() {
        loadConfig();
        registerWithHotReload();
    }

    private void registerWithHotReload() {
        try {
            Path configDir = com.quackers29.businesscraft.api.PlatformAccess.platform.getConfigDirectory();
            Path configPath = configDir.resolve(CONFIG_FILE_NAME);
            ConfigurationService.getInstance().registerConfiguration(
                    "businesscraft-main",
                    configPath,
                    this::reloadFromFile);
        } catch (Exception e) {
            LOGGER.warn("Failed to register configuration for hot-reloading: {}", e.getMessage());
        }
    }

    private void reloadFromFile(Path filePath) {
        loadConfig();
    }

    public static void loadConfig() {
        try {
            Path configDir = com.quackers29.businesscraft.api.PlatformAccess.platform.getConfigDirectory();
            Path configFile = configDir.resolve(CONFIG_FILE_NAME);

            // Create config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // Copy default config from resources if file doesn't exist
            if (!Files.exists(configFile)) {
                copyDefaultConfig(configFile);
            }

            // Load config using NightConfig
            try (CommentedFileConfig config = CommentedFileConfig.builder(configFile)
                    .preserveInsertionOrder()
                    .build()) {

                config.load();

                // General settings
                minDistanceBetweenTowns = config.getIntOrElse("general.minDistanceBetweenTowns", 100);
                defaultStartingPopulation = config.getIntOrElse("general.defaultStartingPopulation", 5);
                townNames = config.getOrElse("general.townNames", getDefaultTownNames());

                // Vehicle settings
                enableCreateTrains = config.getOrElse("vehicles.enableCreateTrains", true);
                enableMinecarts = config.getOrElse("vehicles.enableMinecarts", true);
                vehicleSearchRadius = config.getIntOrElse("vehicles.vehicleSearchRadius", 3);
                minecartStopThreshold = config.getOrElse("vehicles.minecartStopThreshold", 0.001);

                // Tourist settings
                minPopForTourists = config.getIntOrElse("tourists.minPopForTourists", 5);
                maxTouristsPerTown = config.getIntOrElse("tourists.maxTouristsPerTown", 1000);
                populationPerTourist = config.getIntOrElse("tourists.populationPerTourist", 5);
                maxPopBasedTourists = config.getIntOrElse("tourists.maxPopBasedTourists", 20);
                touristExpiryMinutes = config.getOrElse("tourists.touristExpiryMinutes", 120.0);
enableTouristExpiry = config.getOrElse("tourists.enableTouristExpiry", true);
        notifyOnTouristDeparture = config.getOrElse("tourists.notifyOnTouristDeparture", true);
        touristSystemEnabled = config.getOrElse("tourists.enabled", true);

                // Economy settings
                metersPerEmerald = config.getIntOrElse("economy.metersPerEmerald", 50);
                currencyItem = config.getOrElse("economy.currencyItem", "minecraft:emerald");

                // Milestone settings
                enableMilestones = config.getOrElse("milestones.enabled", true);
                loadMilestoneRewards(config);

                // Contract settings
                contractAuctionDurationMinutes = config.getOrElse("contracts.auctionDurationMinutes", 1.0);
                contractCourierAcceptanceMinutes = config.getOrElse("contracts.courierAcceptanceMinutes", 2.0);
                contractCourierDeliveryMinutesPerMeter = config.getOrElse("contracts.courierDeliveryMinutesPerMeter", 0.05);
                contractSnailMailDeliveryMinutesPerMeter = config.getOrElse("contracts.snailMailDeliveryMinutesPerMeter", 0.1);
        contractsEnabled = config.getOrElse("contracts.enabled", true);

                // Production settings
                productionEnabled = config.getOrElse("production.enabled", true);
                productionTickInterval = config.getIntOrElse("production.tickInterval", 100);
                dailyTickInterval = config.getIntOrElse("production.dailyTickInterval", 24000);
                minStockPercent = config.getIntOrElse("production.minStockPercent", 60);
                excessStockPercent = config.getIntOrElse("production.excessStockPercent", 80);
        researchEnabled = config.getOrElse("research.enabled", true);

                // Trading settings
                tradingEnabled = config.getOrElse("trading.enabled", true);
                tradingTickInterval = config.getIntOrElse("trading.tickInterval", 60);
                tradingRestockRate = config.<Number>getOrElse("trading.restockRate", 0.5).floatValue();
                tradingDefaultMaxStock = config.<Number>getOrElse("trading.defaultMaxStock", 1000.0).floatValue();

                // Display settings
                displayTimezone = config.getOrElse("display.timezone", "UTC");
                com.quackers29.businesscraft.util.BCTimeUtils.setTimezone(displayTimezone);

                // Player settings
                playerTracking = config.getOrElse("player.playerTracking", true);
                townBoundaryMessages = config.getOrElse("player.townBoundaryMessages", true);
            }

            // Load registries after config
            loadRegistries();

            LOGGER.info("Loaded BusinessCraft configuration from {}", configFile);

        } catch (Exception e) {
            LOGGER.error("Failed to load config: {}", e.getMessage(), e);
            // Use defaults on error
        }
    }

    private static void copyDefaultConfig(Path targetPath) {
        try (InputStream is = ConfigLoader.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (is != null) {
                Files.copy(is, targetPath);
                LOGGER.info("Created default configuration file: {}", targetPath);
            } else {
                LOGGER.warn("Default config resource not found, creating minimal config");
                saveConfig();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to copy default config: {}", e.getMessage());
        }
    }

    private static void loadMilestoneRewards(CommentedFileConfig config) {
        milestoneRewards.clear();

        List<CommentedConfig> rewards = config.getOrElse("milestones.rewards", new ArrayList<>());
        if (rewards.isEmpty()) {
            // Default milestone
            List<String> defaultRewards = List.of("minecraft:bread:1", "minecraft:experience_bottle:2");
            milestoneRewards.put(10, new ArrayList<>(defaultRewards));
            return;
        }

        for (CommentedConfig reward : rewards) {
            int distance = reward.<Number>getOrElse("distance", 0).intValue();
            List<String> items = reward.getOrElse("items", new ArrayList<>());
            if (distance > 0 && !items.isEmpty()) {
                milestoneRewards.put(distance, new ArrayList<>(items));
            }
        }
    }

    private static void loadRegistries() {
        // Load registries for both integrated servers and display mapping
        com.quackers29.businesscraft.economy.ResourceRegistry.load();
        com.quackers29.businesscraft.production.ProductionRegistry.load();
        com.quackers29.businesscraft.production.UpgradeRegistry.load();
        com.quackers29.businesscraft.world.BiomeRegistry.load();
    }

    private static List<String> getDefaultTownNames() {
        return List.of("Riverside", "Hillcrest", "Meadowbrook", "Oakville", "Stonebridge",
                "Willowglen", "Sunnyvale", "Cedarwood", "Maplehaven", "Brookside",
                "Fairview", "Pinehurst", "Elmwood", "Greystone");
    }

    public static void saveConfig() {
        try {
            Path configDir = com.quackers29.businesscraft.api.PlatformAccess.platform.getConfigDirectory();
            Path configFile = configDir.resolve(CONFIG_FILE_NAME);

            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            try (CommentedFileConfig config = CommentedFileConfig.builder(configFile)
                    .preserveInsertionOrder()
                    .build()) {

                // General settings
                config.set("general.minDistanceBetweenTowns", minDistanceBetweenTowns);
                config.setComment("general.minDistanceBetweenTowns", " Minimum distance between towns in blocks");
                config.set("general.defaultStartingPopulation", defaultStartingPopulation);
                config.setComment("general.defaultStartingPopulation", " Default starting population for new towns");
                config.set("general.townNames", townNames);
                config.setComment("general.townNames", " List of random town names for auto-naming");

                // Vehicle settings
                config.set("vehicles.enableCreateTrains", enableCreateTrains);
                config.setComment("vehicles.enableCreateTrains", " Enable Create mod train integration");
                config.set("vehicles.enableMinecarts", enableMinecarts);
                config.setComment("vehicles.enableMinecarts", " Enable minecart detection for tourists");
                config.set("vehicles.vehicleSearchRadius", vehicleSearchRadius);
                config.setComment("vehicles.vehicleSearchRadius", " Search radius for vehicle detection (blocks)");
                config.set("vehicles.minecartStopThreshold", minecartStopThreshold);
                config.setComment("vehicles.minecartStopThreshold", " Minecart stop velocity threshold");

                // Tourist settings
                config.set("tourists.minPopForTourists", minPopForTourists);
                config.setComment("tourists.minPopForTourists", " Minimum population required to spawn tourists");
                config.set("tourists.maxTouristsPerTown", maxTouristsPerTown);
                config.setComment("tourists.maxTouristsPerTown", " Maximum tourists per town");
                config.set("tourists.populationPerTourist", populationPerTourist);
                config.setComment("tourists.populationPerTourist", " Population required per tourist slot");
                config.set("tourists.maxPopBasedTourists", maxPopBasedTourists);
                config.setComment("tourists.maxPopBasedTourists", " Maximum population-based tourists");
                config.set("tourists.touristExpiryMinutes", touristExpiryMinutes);
                config.setComment("tourists.touristExpiryMinutes", " Tourist expiry time in minutes (0 = never expire)");
                config.set("tourists.enableTouristExpiry", enableTouristExpiry);
                config.setComment("tourists.enableTouristExpiry", " Enable tourist expiry system");
config.set("tourists.notifyOnTouristDeparture", notifyOnTouristDeparture);
        config.setComment("tourists.notifyOnTouristDeparture", " Notify origin town when tourist departs");
        config.set("tourists.enabled", touristSystemEnabled);
        config.setComment("tourists.enabled", "Master switch - completely disables tourist spawning globally");

                // Economy settings
                config.set("economy.metersPerEmerald", metersPerEmerald);
                config.setComment("economy.metersPerEmerald", " Meters of travel per emerald earned");
                config.set("economy.currencyItem", currencyItem);
                config.setComment("economy.currencyItem", " Currency item for trading (format: modid:item)");

                // Milestone settings
                config.set("milestones.enabled", enableMilestones);
                config.setComment("milestones.enabled", " Enable distance milestone rewards");
                saveMilestoneRewards(config);

                // Contract settings
                config.set("contracts.auctionDurationMinutes", contractAuctionDurationMinutes);
                config.setComment("contracts.auctionDurationMinutes", " Auction duration in minutes");
                config.set("contracts.courierAcceptanceMinutes", contractCourierAcceptanceMinutes);
                config.setComment("contracts.courierAcceptanceMinutes", " Courier acceptance window in minutes");
                config.set("contracts.courierDeliveryMinutesPerMeter", contractCourierDeliveryMinutesPerMeter);
                config.setComment("contracts.courierDeliveryMinutesPerMeter", " Courier delivery time per meter (minutes)");
config.set("contracts.snailMailDeliveryMinutesPerMeter", contractSnailMailDeliveryMinutesPerMeter);
        config.setComment("contracts.snailMailDeliveryMinutesPerMeter", " Snail mail delivery time per meter (minutes)");
        config.set("contracts.enabled", contractsEnabled);
        config.setComment("contracts.enabled", "Enable contract system (auction, bidding, delivery)");

                // Production settings
                config.set("production.enabled", productionEnabled);
                config.setComment("production.enabled", " Enable automatic production system");
                config.set("production.tickInterval", productionTickInterval);
                config.setComment("production.tickInterval", " Ticks between production cycles");
                config.set("production.dailyTickInterval", dailyTickInterval);
                config.setComment("production.dailyTickInterval", " Daily tick interval for consumption");
                config.set("production.minStockPercent", minStockPercent);
                config.setComment("production.minStockPercent", " Minimum stock percentage before buying");
                config.set("production.excessStockPercent", excessStockPercent);
                config.setComment("production.excessStockPercent", " Excess stock percentage for selling");

                // Trading settings
                config.set("trading.enabled", tradingEnabled);
                config.setComment("trading.enabled", " Enable trading system");
                config.set("trading.tickInterval", tradingTickInterval);
                config.setComment("trading.tickInterval", " Ticks between trading cycles");
                config.set("trading.restockRate", (double) tradingRestockRate);
                config.setComment("trading.restockRate", " Restock rate multiplier");
                config.set("trading.defaultMaxStock", (double) tradingDefaultMaxStock);
                config.setComment("trading.defaultMaxStock", " Default max stock for items");

                // Display settings
                config.set("display.timezone", displayTimezone);
                config.setComment("display.timezone", " Timezone for time display (UTC, SYSTEM, or timezone ID like America/New_York)");

                // Player settings
                config.set("player.playerTracking", playerTracking);
                config.setComment("player.playerTracking", " Enable player tracking system");
config.set("player.townBoundaryMessages", townBoundaryMessages);
        config.setComment("player.townBoundaryMessages", " Show town boundary entry/exit messages");

        config.set("research.enabled", researchEnabled);
        config.setComment("research.enabled", "Enable research/upgrade system");

                config.save();
            }

            LOGGER.info("Saved BusinessCraft configuration to {}", configFile);

        } catch (Exception e) {
            LOGGER.error("Failed to save config: {}", e.getMessage(), e);
        }
    }

    private static void saveMilestoneRewards(CommentedFileConfig config) {
        List<CommentedConfig> rewardsList = new ArrayList<>();

        for (Map.Entry<Integer, List<String>> entry : milestoneRewards.entrySet()) {
            CommentedConfig reward = config.createSubConfig();
            reward.set("distance", entry.getKey());
            reward.set("items", entry.getValue());
            rewardsList.add(reward);
        }

        config.set("milestones.rewards", rewardsList);
        config.setComment("milestones.rewards", " Milestone rewards - distance in meters, items as \"modid:item:count\"");
    }
}
