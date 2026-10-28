package com.quackers29.businesscraft.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.quackers29.businesscraft.economy.JourneyProgress;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.town.data.TouristDestinationLearning;
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
    public static int defaultStartingPopulation = 5;
    /** Default town boundary radius (blocks) when no {@code border} modifier is set. */
    public static int defaultBorderRadius = 50;
    /**
     * Highest search radius one platform may use, in blocks. Each platform still starts at 10.
     * Spread cannot exceed that platform's own radius.
     */
    public static int maxPlatformSearchRadius = 100;
    public static boolean craftableTownInterface = true;
    public static int maxTouristsPerTown = 1000;
    public static int populationPerTourist = 5;
    public static int maxPopBasedTourists = 20;

    // Tourist-related config
    public static double touristExpiryMinutes = 120.0;
    public static boolean enableTouristExpiry = true;
    public static boolean notifyOnTouristDeparture = true;
    /**
     * Distance (blocks) required to reach tourist ranks 2, 3, …. Rank 1 is everyone below the first entry.
     * Defaults line up with the linear fare: 500 m ≈ 5 emeralds, 6000 m ≈ 60 emeralds (past the bread milestone).
     */
    public static int[] touristLevelDistances = JourneyProgress.DEFAULT_TOURIST_LEVELS.clone();
    /**
     * Cumulative visitor-blocks required to reach town notoriety tiers 2, 3, ….
     */
    public static int[] notorietyDistances = JourneyProgress.DEFAULT_TOWN_TIERS.clone();

    // Tourism economy config
    /** Blocks traveled per emerald (linear curve: exponent = 1). Default 100 → 1 km ≈ 10 emeralds/tourist. */
    public static int metersPerEmerald = 100;
    // Minimum average travel distance (blocks) for an arrival batch to earn any payment.
    // Blocks the adjacent-town exploit (~55-block spacing farming the 1-emerald floor).
    public static int minPayoutDistance = 100;
    /** Power exponent for the optional non-linear fare curve (1.0 = linear via metersPerEmerald). */
    public static double payoutCurveExponent = 1.0;
    /** Reference distance (blocks) when payoutCurveExponent != 1.0. */
    public static int payoutReferenceBlocks = 1000;
    /** Emeralds per tourist at payoutReferenceBlocks when using a non-linear exponent. */
    public static double payoutEmeraldsPerTouristAtReference = 10.0;

    // Distance milestone config
    public static boolean enableMilestones = true;
    public static Map<Integer, List<String>> milestoneRewards = new HashMap<>();

    // Player tracking config
    public static boolean playerTracking = true;
    public static boolean townBoundaryMessages = true;

    // Trading config
    public static boolean tradingEnabled = false;
    public static int tradingTickInterval = 60;
    public static float tradingRestockRate = 0.5f;
    public static float tradingDefaultMaxStock = 1000.0f;
    public static String currencyItem = "minecraft:emerald";

    // Production config
    public static boolean productionEnabled = false;
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

    /** Default destination mode for newly placed platforms (any, auto, specific). */
    public static DestinationMode defaultDestinationMode = DestinationMode.AUTO;
    /** Max fraction of AUTO spawns that use a learned destination once confidence is full (0–1). */
    public static double touristAutoMaxLearnedShare = 0.7;
    /** Score half-life for learned routes, in in-game minutes (converted to ticks at use). */
    public static double touristAutoScoreHalfLifeMinutes = 10080.0;
    public static double touristAutoScoreHalfLifeTicks = 10080.0 * 60.0 * 20.0;
    public static double touristAutoPruneFloor = 0.05;
    public static int touristAutoMaxLearnedTowns = 12;
    /** Total learned score required before AUTO prefers learned routes over pure exploration. */
    public static double touristAutoMinScoreForLearning = 2.0;
    /** Score removed when a learned-destination tourist expires without arriving. */
    public static double touristAutoExpiryPenalty = 1.0;
    /** Multiplier applied to average trip duration (ticks) when computing per-route score half-life. */
    public static double touristAutoFadeTripMultiplier = 2.0;
    /** Base probability (0–1) an open-ticket tourist stays on at an intermediate platform stop. */
    public static double touristAutoRideOnBaseChance = 0.45;
    /** Added stay probability per block traveled divided by {@link #touristAutoRideOnDistanceReferenceBlocks}. */
    public static double touristAutoRideOnDistanceBonusScale = 0.35;
    /** Blocks used to normalize traveled distance for ride-on stay chance. */
    public static double touristAutoRideOnDistanceReferenceBlocks = 500.0;
    /** Stay probability subtracted per town already skipped on this trip. */
    public static double touristAutoRideOnSkipPenalty = 0.12;
    /** Upper cap on stay probability at a single stop. */
    public static double touristAutoRideOnMaxStayChance = 0.92;
    /** After this many ride-on skips, the next stop must be an alight. */
    public static int touristAutoRideOnMaxSkips = 4;
    /** Fraction of a full arrival credit when a town is passed without alighting. */
    public static double touristAutoPassReachWeight = 0.35;
    /** Spawn weight multiplier strength for farther learned towns (0 = score-only). */
    public static double touristAutoDistanceBias = 0.75;
    /** Random jitter on spawn weights (0 = none, 1 = up to ±100%). */
    public static double touristAutoRandomness = 0.35;

    public static boolean contractsEnabled = false;
    public static boolean researchEnabled = false;

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
                    filePath -> loadConfig());
        } catch (Exception e) {
            LOGGER.warn("Failed to register configuration for hot-reloading: {}", e.getMessage());
        }
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
                // Town spacing uses boundary radii (TownBoundaryService), not a flat min-distance key
                defaultStartingPopulation = config.getIntOrElse("general.defaultStartingPopulation", 5);
                defaultBorderRadius = config.getIntOrElse("general.defaultBorderRadius", 50);
                craftableTownInterface = config.getOrElse("general.craftableTownInterface", true);
                int configuredMaxRadius = config.getIntOrElse("platforms.maxSearchRadius", 100);
                maxPlatformSearchRadius = Math.max(1, configuredMaxRadius);
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
                defaultDestinationMode = readDestinationMode(config, "tourists.defaultDestinationMode",
                        DestinationMode.AUTO);
                touristAutoMaxLearnedShare = readShare(config, "tourists.autoMaxLearnedShare", 0.7);
                touristAutoScoreHalfLifeMinutes = readPositiveDouble(config, "tourists.autoScoreHalfLifeMinutes",
                        10080.0);
                touristAutoScoreHalfLifeTicks = TouristDestinationLearning.halfLifeMinutesToTicks(
                        touristAutoScoreHalfLifeMinutes);
                touristAutoPruneFloor = readNonNegativeDouble(config, "tourists.autoPruneFloor", 0.05);
                touristAutoMaxLearnedTowns = readPositiveInt(config, "tourists.autoMaxLearnedTowns", 12);
                touristAutoMinScoreForLearning = readPositiveDouble(config, "tourists.autoMinScoreForLearning", 2.0);
                touristAutoExpiryPenalty = readNonNegativeDouble(config, "tourists.autoExpiryPenalty", 1.0);
                touristAutoFadeTripMultiplier = readPositiveDouble(config, "tourists.autoFadeTripMultiplier", 2.0);
                touristAutoRideOnBaseChance = readShare(config, "tourists.autoRideOnBaseChance", 0.45);
                touristAutoRideOnDistanceBonusScale = readShare(config, "tourists.autoRideOnDistanceBonusScale", 0.35);
                touristAutoRideOnDistanceReferenceBlocks = readPositiveDouble(config,
                        "tourists.autoRideOnDistanceReferenceBlocks", 500.0);
                touristAutoRideOnSkipPenalty = readShare(config, "tourists.autoRideOnSkipPenalty", 0.12);
                touristAutoRideOnMaxStayChance = readShare(config, "tourists.autoRideOnMaxStayChance", 0.92);
                touristAutoRideOnMaxSkips = readPositiveInt(config, "tourists.autoRideOnMaxSkips", 4);
                touristAutoPassReachWeight = readNonNegativeDouble(config, "tourists.autoPassReachWeight", 0.35);
                touristAutoDistanceBias = readNonNegativeDouble(config, "tourists.autoDistanceBias", 0.75);
                touristAutoRandomness = readShare(config, "tourists.autoRandomness", 0.35);
                touristLevelDistances = readRisingInts(config, "tourists.levelDistances",
                        JourneyProgress.DEFAULT_TOURIST_LEVELS);
                notorietyDistances = readRisingInts(config, "economy.notorietyDistances",
                        JourneyProgress.DEFAULT_TOWN_TIERS);

                // Economy settings
                metersPerEmerald = config.getIntOrElse("economy.metersPerEmerald", 100);
                minPayoutDistance = config.getIntOrElse("economy.minPayoutDistance", 100);
                payoutCurveExponent = config.getOrElse("economy.payoutCurveExponent", 1.0);
                payoutReferenceBlocks = config.getIntOrElse("economy.payoutReferenceBlocks", 1000);
                payoutEmeraldsPerTouristAtReference = config
                        .getOrElse("economy.payoutEmeraldsPerTouristAtReference", 10.0);
                currencyItem = config.getOrElse("economy.currencyItem", "minecraft:emerald");

                // Milestone settings
                enableMilestones = config.getOrElse("milestones.enabled", true);
                loadMilestoneRewards(config);

                // Contract settings
                contractAuctionDurationMinutes = config.getOrElse("contracts.auctionDurationMinutes", 1.0);
                contractCourierAcceptanceMinutes = config.getOrElse("contracts.courierAcceptanceMinutes", 2.0);
                contractCourierDeliveryMinutesPerMeter = config.getOrElse("contracts.courierDeliveryMinutesPerMeter", 0.05);
                contractSnailMailDeliveryMinutesPerMeter = config.getOrElse("contracts.snailMailDeliveryMinutesPerMeter", 0.1);
                contractsEnabled = config.getOrElse("contracts.enabled", false);

                // Production settings
                productionEnabled = config.getOrElse("production.enabled", false);
                productionTickInterval = config.getIntOrElse("production.tickInterval", 100);
                dailyTickInterval = config.getIntOrElse("production.dailyTickInterval", 24000);
                minStockPercent = config.getIntOrElse("production.minStockPercent", 60);
                excessStockPercent = config.getIntOrElse("production.excessStockPercent", 80);
                researchEnabled = config.getOrElse("research.enabled", false);

                // Trading settings
                tradingEnabled = config.getOrElse("trading.enabled", false);
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
            // Tourism-only beta defaults: 1000m apple, 5000m bread
            milestoneRewards.put(1000, new ArrayList<>(List.of("minecraft:apple:1")));
            milestoneRewards.put(5000, new ArrayList<>(List.of("minecraft:bread:1")));
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
                config.set("general.defaultStartingPopulation", defaultStartingPopulation);
                config.setComment("general.defaultStartingPopulation", " Default starting population for new towns");
                config.set("general.defaultBorderRadius", defaultBorderRadius);
                config.setComment("general.defaultBorderRadius", " Default town boundary radius in blocks when no border modifier is set. Also used for new-town placement spacing.");
                config.set("general.craftableTownInterface", craftableTownInterface);
                config.setComment("general.craftableTownInterface", " When false: Town Centre is nearly indestructible in survival. Creative players can still break it. When true (default): block is craftable and breakable.");
                config.set("general.townNames", townNames);
                config.setComment("general.townNames", " List of random town names for auto-naming");

                config.set("platforms.maxSearchRadius", maxPlatformSearchRadius);
                config.setComment("platforms.maxSearchRadius",
                        " Highest search radius a platform may use, in blocks. Default 100. "
                                + "Each platform still starts at 10 with spread 0. "
                                + "Spread cannot exceed that platform's own search radius.");

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
                config.set("tourists.defaultDestinationMode", defaultDestinationMode.toConfigValue());
                config.setComment("tourists.defaultDestinationMode",
                        " Default destination mode for newly placed platforms: any, auto, or specific");
                config.set("tourists.autoMaxLearnedShare", touristAutoMaxLearnedShare);
                config.setComment("tourists.autoMaxLearnedShare",
                        " AUTO: max fraction of spawns using learned destinations once confidence is full (0.0–1.0)");
                config.set("tourists.autoScoreHalfLifeMinutes", touristAutoScoreHalfLifeMinutes);
                config.setComment("tourists.autoScoreHalfLifeMinutes",
                        " AUTO: half-life for learned route scores, in in-game minutes (decays on world time, not wall clock)");
                config.set("tourists.autoPruneFloor", touristAutoPruneFloor);
                config.setComment("tourists.autoPruneFloor", " AUTO: learned scores below this are removed");
                config.set("tourists.autoMaxLearnedTowns", touristAutoMaxLearnedTowns);
                config.setComment("tourists.autoMaxLearnedTowns", " AUTO: maximum learned destination towns kept per platform");
                config.set("tourists.autoMinScoreForLearning", touristAutoMinScoreForLearning);
                config.setComment("tourists.autoMinScoreForLearning",
                        " AUTO: total learned score before learned destinations are preferred over exploration");
                config.set("tourists.autoExpiryPenalty", touristAutoExpiryPenalty);
                config.setComment("tourists.autoExpiryPenalty",
                        " AUTO: score penalty when a learned-destination tourist expires without arriving");
                config.set("tourists.autoFadeTripMultiplier", touristAutoFadeTripMultiplier);
                config.setComment("tourists.autoFadeTripMultiplier",
                        " AUTO: per-route score half-life is at least (average trip ticks × this multiplier), "
                                + "but never below autoScoreHalfLifeMinutes");
                config.set("tourists.autoRideOnBaseChance", touristAutoRideOnBaseChance);
                config.setComment("tourists.autoRideOnBaseChance",
                        " AUTO: base chance (0–1) an Any-Town tourist stays on at an intermediate platform stop");
                config.set("tourists.autoRideOnDistanceBonusScale", touristAutoRideOnDistanceBonusScale);
                config.setComment("tourists.autoRideOnDistanceBonusScale",
                        " AUTO: added stay chance per (blocks traveled ÷ autoRideOnDistanceReferenceBlocks)");
                config.set("tourists.autoRideOnDistanceReferenceBlocks", touristAutoRideOnDistanceReferenceBlocks);
                config.setComment("tourists.autoRideOnDistanceReferenceBlocks",
                        " AUTO: block distance that counts as 1.0 for ride-on distance bonus");
                config.set("tourists.autoRideOnSkipPenalty", touristAutoRideOnSkipPenalty);
                config.setComment("tourists.autoRideOnSkipPenalty",
                        " AUTO: stay chance reduction per town already skipped on this trip");
                config.set("tourists.autoRideOnMaxStayChance", touristAutoRideOnMaxStayChance);
                config.setComment("tourists.autoRideOnMaxStayChance", " AUTO: cap on stay probability at one stop");
                config.set("tourists.autoRideOnMaxSkips", touristAutoRideOnMaxSkips);
                config.setComment("tourists.autoRideOnMaxSkips",
                        " AUTO: after this many skipped stops, the tourist must alight at the next stop");
                config.set("tourists.autoPassReachWeight", touristAutoPassReachWeight);
                config.setComment("tourists.autoPassReachWeight",
                        " AUTO: learned score added when a town is passed without alighting (fraction of arrival)");
                config.set("tourists.autoDistanceBias", touristAutoDistanceBias);
                config.setComment("tourists.autoDistanceBias",
                        " AUTO: spawn weight boost for farther learned towns (0 = score only)");
                config.set("tourists.autoRandomness", touristAutoRandomness);
                config.setComment("tourists.autoRandomness",
                        " AUTO: random jitter on learned spawn weights (0–1, fraction of weight)");
                config.set("tourists.levelDistances", toList(touristLevelDistances));
                config.setComment("tourists.levelDistances",
                        " Blocks traveled to reach tourist ranks 2, 3, ... (rank 1 is below the first number). "
                                + "Default 500/1500/3000/6000 lines up with the linear fare "
                                + "(about 5/15/30/60 emeralds per tourist at metersPerEmerald=100).");
                config.set("economy.notorietyDistances", toList(notorietyDistances));
                config.setComment("economy.notorietyDistances",
                        " Cumulative visitor-blocks to reach town notoriety tiers 2, 3, ... "
                                + "(Wayside below the first number, then Halt, Station, Destination, Landmark). "
                                + "Default 2000/10000/40000/120000.");

                // Economy settings
                config.set("economy.metersPerEmerald", metersPerEmerald);
                config.setComment("economy.metersPerEmerald",
                        " Blocks traveled per emerald per tourist when payoutCurveExponent = 1.0 (linear). "
                                + "Default 100 → ~10 emeralds/tourist at 1 km; 300/500/1500 blocks pay 3/5/15.");
                config.set("economy.minPayoutDistance", minPayoutDistance);
                config.setComment("economy.minPayoutDistance",
                        " Minimum average travel distance (blocks) before an arrival batch earns any payment");
                config.set("economy.payoutCurveExponent", payoutCurveExponent);
                config.setComment("economy.payoutCurveExponent",
                        " Fare curve exponent. 1.0 = linear (distance ÷ metersPerEmerald). "
                                + "Other values use (distance ÷ payoutReferenceBlocks)^exponent × payoutEmeraldsPerTouristAtReference.");
                config.set("economy.payoutReferenceBlocks", payoutReferenceBlocks);
                config.setComment("economy.payoutReferenceBlocks",
                        " Reference distance (blocks) for non-linear curves (ignored when exponent = 1.0)");
                config.set("economy.payoutEmeraldsPerTouristAtReference", payoutEmeraldsPerTouristAtReference);
                config.setComment("economy.payoutEmeraldsPerTouristAtReference",
                        " Emeralds per tourist at payoutReferenceBlocks for non-linear curves (ignored when exponent = 1.0)");
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

    /** Rising positive ints. A missing, empty, or unsorted list keeps {@code fallback}. */
    private static int[] readRisingInts(CommentedFileConfig config, String key, int[] fallback) {
        if (!config.contains(key)) {
            return fallback.clone();
        }
        Object raw = config.get(key);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return fallback.clone();
        }
        int[] parsed = new int[list.size()];
        int previous = 0;
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            int number;
            if (value instanceof Number numeric) {
                number = numeric.intValue();
            } else {
                try {
                    number = Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException e) {
                    return fallback.clone();
                }
            }
            if (number <= previous) {
                return fallback.clone();
            }
            parsed[i] = number;
            previous = number;
        }
        return parsed;
    }

    private static List<Integer> toList(int[] values) {
        List<Integer> list = new ArrayList<>();
        if (values != null) {
            for (int value : values) {
                list.add(value);
            }
        }
        return list;
    }

    private static DestinationMode readDestinationMode(CommentedFileConfig config, String key,
            DestinationMode fallback) {
        if (!config.contains(key)) {
            return fallback;
        }
        return DestinationMode.fromConfig(config.getOrElse(key, fallback.toConfigValue()), fallback);
    }

    private static double readShare(CommentedFileConfig config, String key, double fallback) {
        double value = readDouble(config, key, fallback);
        if (value < 0 || value > 1) {
            LOGGER.warn("Config {}={} out of range [0,1], using {}", key, value, fallback);
            return fallback;
        }
        return value;
    }

    private static double readPositiveDouble(CommentedFileConfig config, String key, double fallback) {
        double value = readDouble(config, key, fallback);
        if (value <= 0) {
            LOGGER.warn("Config {}={} must be positive, using {}", key, value, fallback);
            return fallback;
        }
        return value;
    }

    private static double readNonNegativeDouble(CommentedFileConfig config, String key, double fallback) {
        double value = readDouble(config, key, fallback);
        if (value < 0) {
            LOGGER.warn("Config {}={} must be non-negative, using {}", key, value, fallback);
            return fallback;
        }
        return value;
    }

    private static int readPositiveInt(CommentedFileConfig config, String key, int fallback) {
        if (!config.contains(key)) {
            return fallback;
        }
        int value = config.getIntOrElse(key, fallback);
        if (value <= 0) {
            LOGGER.warn("Config {}={} must be positive, using {}", key, value, fallback);
            return fallback;
        }
        return value;
    }

    private static double readDouble(CommentedFileConfig config, String key, double fallback) {
        if (!config.contains(key)) {
            return fallback;
        }
        Object raw = config.get(key);
        if (raw instanceof Number numeric) {
            return numeric.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            LOGGER.warn("Config {} invalid, using {}", key, fallback);
            return fallback;
        }
    }
}
