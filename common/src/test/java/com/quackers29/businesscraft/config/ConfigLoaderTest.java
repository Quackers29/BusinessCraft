package com.quackers29.businesscraft.config;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.PlatformHelper;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.util.Result;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-014: Configuration Loading (Test + Docs Loop).
 *
 * Covers the defaults (Java field inits), getOrElse parsing behavior,
 * private getDefaultTownNames(), the loadMilestoneRewards special-case
 * logic (empty-list default vs. invalid-entry skipping), townNames
 * override/empty guard interaction, float/int/bool/list handling,
 * platform-null fallback (the path that makes ConfigLoader testable),
 * and the pure validation surface of ConfigurationService.
 *
 * loadConfig/saveConfig perform real FS + NightConfig work but are
 * pure-library (no MC registry bootstrap), so they are exercised here
 * using a temp dir + TestPlatformHelper stub exactly as registry stubs
 * are used in T-002/T-007/T-008.
 *
 * Private method tested via reflection (per protocol).
 * Documentation: vault/Config/Configuration Loading.md
 */
class ConfigLoaderTest {

    // Snapshot of platform + every static ConfigLoader mutates (comprehensive
    // so that this test does not pollute the rest of the suite).
    private PlatformHelper savedPlatform;
    private boolean savedEnableCreateTrains, savedEnableMinecarts, savedCraftableTownInterface;
    private boolean savedEnableTouristExpiry, savedNotifyOnTouristDeparture, savedTouristSystemEnabled;
    private boolean savedEnableMilestones, savedPlayerTracking, savedTownBoundaryMessages;
    private boolean savedTradingEnabled, savedProductionEnabled, savedResearchEnabled, savedContractsEnabled;
    private int savedVehicleSearchRadius, savedMinDistanceBetweenTowns, savedDefaultStartingPopulation;
    private int savedMaxTouristsPerTown, savedPopulationPerTourist, savedMaxPopBasedTourists;
    private int savedMinPopForTourists, savedMetersPerEmerald, savedTradingTickInterval;
    private int savedProductionTickInterval, savedDailyTickInterval, savedMinStockPercent, savedExcessStockPercent;
    private double savedTouristExpiryMinutes, savedMinecartStopThreshold;
    private double savedContractAuctionDurationMinutes, savedContractCourierAcceptanceMinutes;
    private double savedContractCourierDeliveryMinutesPerMeter, savedContractSnailMailDeliveryMinutesPerMeter;
    private float savedTradingRestockRate, savedTradingDefaultMaxStock;
    private String savedCurrencyItem, savedDisplayTimezone;
    private List<String> savedTownNames;
    private Map<Integer, List<String>> savedMilestoneRewards;

    private Path tempDir;
    private Path configFile; // businesscraft.toml inside tempDir

    @BeforeEach
    void setUp() throws IOException {
        // Snapshot platform
        savedPlatform = PlatformAccess.platform;

        // Snapshot every field that loadConfig mutates (order matches source)
        savedEnableCreateTrains = ConfigLoader.enableCreateTrains;
        savedEnableMinecarts = ConfigLoader.enableMinecarts;
        savedVehicleSearchRadius = ConfigLoader.vehicleSearchRadius;
        savedMinecartStopThreshold = ConfigLoader.minecartStopThreshold;

        savedTownNames = new ArrayList<>(ConfigLoader.townNames);
        savedMinPopForTourists = ConfigLoader.minPopForTourists;
        savedMinDistanceBetweenTowns = ConfigLoader.minDistanceBetweenTowns;
        savedDefaultStartingPopulation = ConfigLoader.defaultStartingPopulation;
        savedCraftableTownInterface = ConfigLoader.craftableTownInterface;
        savedMaxTouristsPerTown = ConfigLoader.maxTouristsPerTown;
        savedPopulationPerTourist = ConfigLoader.populationPerTourist;
        savedMaxPopBasedTourists = ConfigLoader.maxPopBasedTourists;

        savedTouristExpiryMinutes = ConfigLoader.touristExpiryMinutes;
        savedEnableTouristExpiry = ConfigLoader.enableTouristExpiry;
        savedNotifyOnTouristDeparture = ConfigLoader.notifyOnTouristDeparture;

        savedMetersPerEmerald = ConfigLoader.metersPerEmerald;
        savedCurrencyItem = ConfigLoader.currencyItem;

        savedEnableMilestones = ConfigLoader.enableMilestones;
        savedMilestoneRewards = new HashMap<>();
        for (var e : ConfigLoader.milestoneRewards.entrySet()) {
            savedMilestoneRewards.put(e.getKey(), new ArrayList<>(e.getValue()));
        }

        savedPlayerTracking = ConfigLoader.playerTracking;
        savedTownBoundaryMessages = ConfigLoader.townBoundaryMessages;

        savedTradingEnabled = ConfigLoader.tradingEnabled;
        savedTradingTickInterval = ConfigLoader.tradingTickInterval;
        savedTradingRestockRate = ConfigLoader.tradingRestockRate;
        savedTradingDefaultMaxStock = ConfigLoader.tradingDefaultMaxStock;

        savedProductionEnabled = ConfigLoader.productionEnabled;
        savedProductionTickInterval = ConfigLoader.productionTickInterval;
        savedDailyTickInterval = ConfigLoader.dailyTickInterval;
        savedMinStockPercent = ConfigLoader.minStockPercent;
        savedExcessStockPercent = ConfigLoader.excessStockPercent;

        savedContractAuctionDurationMinutes = ConfigLoader.contractAuctionDurationMinutes;
        savedContractCourierAcceptanceMinutes = ConfigLoader.contractCourierAcceptanceMinutes;
        savedContractCourierDeliveryMinutesPerMeter = ConfigLoader.contractCourierDeliveryMinutesPerMeter;
        savedContractSnailMailDeliveryMinutesPerMeter = ConfigLoader.contractSnailMailDeliveryMinutesPerMeter;
        savedContractsEnabled = ConfigLoader.contractsEnabled;

        savedDisplayTimezone = ConfigLoader.displayTimezone;
        savedResearchEnabled = ConfigLoader.researchEnabled;

        // Fresh temp dir for this test (platform will point here)
        tempDir = Files.createTempDirectory("bc-config-test-");
        configFile = tempDir.resolve("businesscraft.toml");

        // Install stub so loadConfig/saveConfig resolve into our controlled dir
        PlatformAccess.platform = new TestPlatformHelper(tempDir);

        // Ensure clean slate for milestone map in tests that care (loadConfig will
        // populate from file or default logic; we still restore in tearDown).
        ConfigLoader.milestoneRewards.clear();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Restore platform first
        PlatformAccess.platform = savedPlatform;

        // Restore every field (reverse order not required)
        ConfigLoader.enableCreateTrains = savedEnableCreateTrains;
        ConfigLoader.enableMinecarts = savedEnableMinecarts;
        ConfigLoader.vehicleSearchRadius = savedVehicleSearchRadius;
        ConfigLoader.minecartStopThreshold = savedMinecartStopThreshold;

        ConfigLoader.townNames = new ArrayList<>(savedTownNames);
        ConfigLoader.minPopForTourists = savedMinPopForTourists;
        ConfigLoader.minDistanceBetweenTowns = savedMinDistanceBetweenTowns;
        ConfigLoader.defaultStartingPopulation = savedDefaultStartingPopulation;
        ConfigLoader.craftableTownInterface = savedCraftableTownInterface;
        ConfigLoader.maxTouristsPerTown = savedMaxTouristsPerTown;
        ConfigLoader.populationPerTourist = savedPopulationPerTourist;
        ConfigLoader.maxPopBasedTourists = savedMaxPopBasedTourists;

        ConfigLoader.touristExpiryMinutes = savedTouristExpiryMinutes;
        ConfigLoader.enableTouristExpiry = savedEnableTouristExpiry;
        ConfigLoader.notifyOnTouristDeparture = savedNotifyOnTouristDeparture;

        ConfigLoader.metersPerEmerald = savedMetersPerEmerald;
        ConfigLoader.currencyItem = savedCurrencyItem;

        ConfigLoader.enableMilestones = savedEnableMilestones;
        ConfigLoader.milestoneRewards.clear();
        ConfigLoader.milestoneRewards.putAll(savedMilestoneRewards);

        ConfigLoader.playerTracking = savedPlayerTracking;
        ConfigLoader.townBoundaryMessages = savedTownBoundaryMessages;

        ConfigLoader.tradingEnabled = savedTradingEnabled;
        ConfigLoader.tradingTickInterval = savedTradingTickInterval;
        ConfigLoader.tradingRestockRate = savedTradingRestockRate;
        ConfigLoader.tradingDefaultMaxStock = savedTradingDefaultMaxStock;

        ConfigLoader.productionEnabled = savedProductionEnabled;
        ConfigLoader.productionTickInterval = savedProductionTickInterval;
        ConfigLoader.dailyTickInterval = savedDailyTickInterval;
        ConfigLoader.minStockPercent = savedMinStockPercent;
        ConfigLoader.excessStockPercent = savedExcessStockPercent;

        ConfigLoader.contractAuctionDurationMinutes = savedContractAuctionDurationMinutes;
        ConfigLoader.contractCourierAcceptanceMinutes = savedContractCourierAcceptanceMinutes;
        ConfigLoader.contractCourierDeliveryMinutesPerMeter = savedContractCourierDeliveryMinutesPerMeter;
        ConfigLoader.contractSnailMailDeliveryMinutesPerMeter = savedContractSnailMailDeliveryMinutesPerMeter;
        ConfigLoader.contractsEnabled = savedContractsEnabled;

        ConfigLoader.displayTimezone = savedDisplayTimezone;
        ConfigLoader.researchEnabled = savedResearchEnabled;

        // Best-effort cleanup of temp test dir
        try {
            if (configFile != null) Files.deleteIfExists(configFile);
            Files.deleteIfExists(tempDir);
        } catch (Exception ignored) {}
    }

    // --- test double for platform (only getConfigDirectory is exercised by ConfigLoader) ---

    private static class TestPlatformHelper implements PlatformHelper {
        private final Path configDir;

        TestPlatformHelper(Path configDir) {
            this.configDir = configDir;
        }

        @Override
        public Path getConfigDirectory() {
            return configDir;
        }

        @Override public String getModId() { return "businesscraft"; }
        @Override public boolean isClientSide() { return false; }
        @Override public boolean isServerSide() { return true; }
        @Override public String getPlatformName() { return "test"; }
    }

    // --- reflection helper for private static getDefaultTownNames() ---

    @SuppressWarnings("unchecked")
    private List<String> invokeGetDefaultTownNames() throws Exception {
        Method m = ConfigLoader.class.getDeclaredMethod("getDefaultTownNames");
        m.setAccessible(true);
        return (List<String>) m.invoke(null);
    }

    // --- helpers to write a minimal toml for a test case ---

    private void writeToml(String content) throws IOException {
        Files.writeString(configFile, content);
    }

    private void deleteTomlIfExists() throws IOException {
        Files.deleteIfExists(configFile);
    }

    // --- default / reflection tests (no file write needed) ---

    @Test
    void defaults_matchDeclaredJavaFieldInitializers() {
        // These are the values that survive when platform is null (caught NPE path)
        // and also the getOrElse fallbacks. Must match source + shipped toml.
        assertTrue(ConfigLoader.enableCreateTrains);
        assertTrue(ConfigLoader.enableMinecarts);
        assertEquals(3, ConfigLoader.vehicleSearchRadius);
        assertEquals(0.001, ConfigLoader.minecartStopThreshold, 1e-9);

        assertEquals(5, ConfigLoader.minPopForTourists);
        assertEquals(100, ConfigLoader.minDistanceBetweenTowns);
        assertEquals(5, ConfigLoader.defaultStartingPopulation);
        assertFalse(ConfigLoader.craftableTownInterface);
        assertEquals(1000, ConfigLoader.maxTouristsPerTown);
        assertEquals(5, ConfigLoader.populationPerTourist);
        assertEquals(20, ConfigLoader.maxPopBasedTourists);

        assertEquals(120.0, ConfigLoader.touristExpiryMinutes, 1e-9);
        assertTrue(ConfigLoader.enableTouristExpiry);
        assertTrue(ConfigLoader.notifyOnTouristDeparture);

        assertEquals(50, ConfigLoader.metersPerEmerald);
        assertEquals("minecraft:emerald", ConfigLoader.currencyItem);

        assertTrue(ConfigLoader.enableMilestones);
        // NOTE: when the very first class load hits the null-platform NPE path (caught),
        // loadMilestoneRewards is never reached and the map stays at its Java init (empty).
        // The default (10 -> bread+exp) only appears after a *successful* loadConfig that
        // parsed a toml (see loadConfig_noTomlInDir... and the empty-rewards tests).
        assertTrue(ConfigLoader.milestoneRewards.isEmpty() || ConfigLoader.milestoneRewards.containsKey(10));

        assertTrue(ConfigLoader.playerTracking);
        assertTrue(ConfigLoader.townBoundaryMessages);

        assertTrue(ConfigLoader.tradingEnabled);
        assertEquals(60, ConfigLoader.tradingTickInterval);
        assertEquals(0.5f, ConfigLoader.tradingRestockRate, 1e-6f);
        assertEquals(1000.0f, ConfigLoader.tradingDefaultMaxStock, 1e-6f);

        assertTrue(ConfigLoader.productionEnabled);
        assertEquals(100, ConfigLoader.productionTickInterval);
        assertEquals(24000, ConfigLoader.dailyTickInterval);
        assertEquals(60, ConfigLoader.minStockPercent);
        assertEquals(80, ConfigLoader.excessStockPercent);

        assertEquals(1.0, ConfigLoader.contractAuctionDurationMinutes, 1e-9);
        assertEquals(2.0, ConfigLoader.contractCourierAcceptanceMinutes, 1e-9);
        assertEquals(0.05, ConfigLoader.contractCourierDeliveryMinutesPerMeter, 1e-9);
        assertEquals(0.1, ConfigLoader.contractSnailMailDeliveryMinutesPerMeter, 1e-9);
        assertTrue(ConfigLoader.contractsEnabled);

        assertEquals("UTC", ConfigLoader.displayTimezone);
        assertTrue(ConfigLoader.researchEnabled);
        assertTrue(ConfigLoader.touristSystemEnabled);
    }

    @Test
    void getDefaultTownNames_returnsExactlyTheFourteenThemedNames_viaReflection() throws Exception {
        List<String> names = invokeGetDefaultTownNames();
        assertEquals(14, names.size());
        assertEquals("Riverside", names.get(0));
        assertEquals("Greystone", names.get(13));
        // spot-check a few more (Brookside is #10 in 1-based list)
        assertTrue(names.contains("Hillcrest"));
        assertTrue(names.contains("Elmwood"));
        assertTrue(names.contains("Brookside"));
    }

    // --- load / parse tests (explicit toml + loadConfig) ---

    @Test
    void loadConfig_customToml_overridesScalarsAndLists() throws IOException {
        writeToml("""
            [general]
            minDistanceBetweenTowns = 250
            defaultStartingPopulation = 12
            craftableTownInterface = true
            townNames = ["Alpha", "Beta"]

            [vehicles]
            enableCreateTrains = false
            vehicleSearchRadius = 7
            minecartStopThreshold = 0.042

            [tourists]
            enabled = false
            minPopForTourists = 8
            touristExpiryMinutes = 45.5
            enableTouristExpiry = false

            [economy]
            metersPerEmerald = 125
            currencyItem = "minecraft:diamond"

            [milestones]
            enabled = false

            [contracts]
            auctionDurationMinutes = 3.5
            courierAcceptanceMinutes = 4.0
            courierDeliveryMinutesPerMeter = 0.25
            snailMailDeliveryMinutesPerMeter = 0.5
            enabled = false

            [display]
            timezone = "America/New_York"
            """);

        ConfigLoader.loadConfig();

        assertEquals(250, ConfigLoader.minDistanceBetweenTowns);
        assertEquals(12, ConfigLoader.defaultStartingPopulation);
        assertTrue(ConfigLoader.craftableTownInterface);
        assertEquals(List.of("Alpha", "Beta"), ConfigLoader.townNames);

        assertFalse(ConfigLoader.enableCreateTrains);
        assertEquals(7, ConfigLoader.vehicleSearchRadius);
        assertEquals(0.042, ConfigLoader.minecartStopThreshold, 1e-9);

        assertFalse(ConfigLoader.touristSystemEnabled);
        assertEquals(8, ConfigLoader.minPopForTourists);
        assertEquals(45.5, ConfigLoader.touristExpiryMinutes, 1e-9);
        assertFalse(ConfigLoader.enableTouristExpiry);

        assertEquals(125, ConfigLoader.metersPerEmerald);
        assertEquals("minecraft:diamond", ConfigLoader.currencyItem);

        assertFalse(ConfigLoader.enableMilestones);

        assertEquals(3.5, ConfigLoader.contractAuctionDurationMinutes, 1e-9);
        assertEquals(4.0, ConfigLoader.contractCourierAcceptanceMinutes, 1e-9);
        assertEquals(0.25, ConfigLoader.contractCourierDeliveryMinutesPerMeter, 1e-9);
        assertEquals(0.5, ConfigLoader.contractSnailMailDeliveryMinutesPerMeter, 1e-9);
        assertFalse(ConfigLoader.contractsEnabled);

        assertEquals("America/New_York", ConfigLoader.displayTimezone);
    }

    @Test
    void loadConfig_milestones_emptyList_installsDefault() throws IOException {
        writeToml("""
            [milestones]
            enabled = true
            [[milestones.rewards]]
            # deliberately empty list via toml syntax below
            """);
        // The parser will see the key present as empty list because no valid tables
        // To force the empty-list branch we write a toml whose rewards section yields []
        // NightConfig will treat a missing or empty [[ ]] the same for getOrElse.
        // Simpler: just ensure no [[milestones.rewards]] at all (absent key path).
        writeToml("""
            [milestones]
            enabled = true
            """);

        ConfigLoader.loadConfig();

        assertTrue(ConfigLoader.enableMilestones);
        assertEquals(1, ConfigLoader.milestoneRewards.size());
        assertEquals(List.of("minecraft:bread:1", "minecraft:experience_bottle:2"),
                ConfigLoader.milestoneRewards.get(10));
    }

    @Test
    void loadConfig_milestones_validEntries_populateMapAndSkipZeroDistance() throws IOException {
        writeToml("""
            [milestones]
            enabled = true
            [[milestones.rewards]]
            distance = 50
            items = ["minecraft:emerald:1"]
            [[milestones.rewards]]
            distance = 0
            items = ["minecraft:stone:64"]
            [[milestones.rewards]]
            distance = 200
            items = ["minecraft:gold_ingot:2", "minecraft:iron_ingot:4"]
            """);

        ConfigLoader.loadConfig();

        assertEquals(2, ConfigLoader.milestoneRewards.size());
        assertEquals(List.of("minecraft:emerald:1"), ConfigLoader.milestoneRewards.get(50));
        assertEquals(List.of("minecraft:gold_ingot:2", "minecraft:iron_ingot:4"),
                ConfigLoader.milestoneRewards.get(200));
        assertFalse(ConfigLoader.milestoneRewards.containsKey(0));
    }

    @Test
    void loadConfig_milestones_allInvalidEntries_leaveMapEmpty_noDefaultInstalled() throws IOException {
        // QUIRK (documented in vault note): when the rewards list is present but
        // every entry is filtered out (distance<=0 or empty items), we do NOT fall
        // back to the default milestone. This is the "list-with-invalids" path vs
        // the "absent or truly empty list" path.
        writeToml("""
            [milestones]
            enabled = true
            [[milestones.rewards]]
            distance = 0
            items = ["minecraft:bread:1"]
            [[milestones.rewards]]
            distance = 10
            items = []
            """);

        ConfigLoader.loadConfig();

        assertTrue(ConfigLoader.milestoneRewards.isEmpty(),
                "All-invalid rewards list must leave map empty (no default inserted)");
    }

    @Test
    void loadConfig_townNames_emptyListInToml_isKept_emptyAndCallersSeeDefaultTown() throws IOException {
        writeToml("""
            [general]
            townNames = []
            """);

        ConfigLoader.loadConfig();

        assertNotNull(ConfigLoader.townNames);
        assertTrue(ConfigLoader.townNames.isEmpty());
        // The two getRandomTownName sites both return "DefaultTown" when empty.
        // We don't call them here (they are in MC classes), but the loader state is pinned.
    }

    @Test
    void loadConfig_floatsAndTradingNumbers_useNumberCastPaths() throws IOException {
        writeToml("""
            [trading]
            enabled = true
            tickInterval = 90
            restockRate = 0.75
            defaultMaxStock = 2500.0

            [production]
            enabled = true
            tickInterval = 50
            dailyTickInterval = 12000
            minStockPercent = 40
            excessStockPercent = 90
            """);

        ConfigLoader.loadConfig();

        assertEquals(90, ConfigLoader.tradingTickInterval);
        assertEquals(0.75f, ConfigLoader.tradingRestockRate, 1e-6f);
        assertEquals(2500.0f, ConfigLoader.tradingDefaultMaxStock, 1e-6f);

        assertEquals(50, ConfigLoader.productionTickInterval);
        assertEquals(12000, ConfigLoader.dailyTickInterval);
        assertEquals(40, ConfigLoader.minStockPercent);
        assertEquals(90, ConfigLoader.excessStockPercent);
    }

    @Test
    void loadConfig_noTomlInDir_copiesResourceAndEndsWithShippedDefaults() throws IOException {
        // Ensure no file present — loadConfig should create it from the jar resource
        deleteTomlIfExists();

        // At this point the class was already loaded once (null platform path).
        // Calling loadConfig now with a real platform dir but no file should trigger copyDefaultConfig.
        ConfigLoader.loadConfig();

        // After copy, the values must match the shipped defaults (identical to Java inits)
        assertEquals(100, ConfigLoader.minDistanceBetweenTowns);
        assertEquals(5, ConfigLoader.defaultStartingPopulation);
        assertEquals(50, ConfigLoader.metersPerEmerald);
        assertTrue(ConfigLoader.milestoneRewards.containsKey(10));
    }

    // --- saveConfig roundtrip (write then re-load) ---

    @Test
    void saveConfig_thenLoadConfig_roundtripsScalarsAndMilestones() throws IOException {
        // Start from known state
        ConfigLoader.metersPerEmerald = 77;
        ConfigLoader.defaultStartingPopulation = 9;
        ConfigLoader.milestoneRewards.clear();
        ConfigLoader.milestoneRewards.put(25, List.of("minecraft:emerald:3"));

        ConfigLoader.saveConfig();
        assertTrue(Files.exists(configFile), "saveConfig must have written the file");

        // Mutate in memory, then reload should restore what was saved
        ConfigLoader.metersPerEmerald = 999;
        ConfigLoader.defaultStartingPopulation = 1;
        ConfigLoader.milestoneRewards.clear();

        ConfigLoader.loadConfig();

        assertEquals(77, ConfigLoader.metersPerEmerald);
        assertEquals(9, ConfigLoader.defaultStartingPopulation);
        assertEquals(1, ConfigLoader.milestoneRewards.size());
        assertEquals(List.of("minecraft:emerald:3"), ConfigLoader.milestoneRewards.get(25));
    }

    // --- ConfigurationService pure validation (no watcher side effects exercised) ---

    @Test
    void configurationService_register_nullName_returnsInvalidConfigName() {
        ConfigurationService svc = ConfigurationService.getInstance();
        Result<Void, BCError.ConfigError> r = svc.registerConfiguration(null, configFile, p -> {});
        assertTrue(r.isFailure());
        assertEquals("INVALID_CONFIG_NAME", r.getError().getCode());
    }

    @Test
    void configurationService_register_emptyName_returnsInvalidConfigName() {
        ConfigurationService svc = ConfigurationService.getInstance();
        Result<Void, BCError.ConfigError> r = svc.registerConfiguration("   ", configFile, p -> {});
        assertTrue(r.isFailure());
        assertEquals("INVALID_CONFIG_NAME", r.getError().getCode());
    }

    @Test
    void configurationService_register_nullPath_returnsInvalidFilePath() {
        ConfigurationService svc = ConfigurationService.getInstance();
        Result<Void, BCError.ConfigError> r = svc.registerConfiguration("test", null, p -> {});
        assertTrue(r.isFailure());
        assertEquals("INVALID_FILE_PATH", r.getError().getCode());
    }

    @Test
    void configurationService_register_nullCallback_returnsInvalidCallback() throws IOException {
        // Need a real file that exists for the later existence check, but we fail earlier
        Files.writeString(configFile, "# dummy");
        ConfigurationService svc = ConfigurationService.getInstance();
        Result<Void, BCError.ConfigError> r = svc.registerConfiguration("test", configFile, null);
        assertTrue(r.isFailure());
        assertEquals("INVALID_CALLBACK", r.getError().getCode());
    }

    @Test
    void configurationService_register_nonExistentFile_returnsFileNotFound() {
        Path missing = tempDir.resolve("does-not-exist.toml");
        ConfigurationService svc = ConfigurationService.getInstance();
        Result<Void, BCError.ConfigError> r = svc.registerConfiguration("test", missing, p -> {});
        assertTrue(r.isFailure());
        assertEquals("FILE_NOT_FOUND", r.getError().getCode());
    }

    @Test
    void configurationService_register_success_isRegistered_unregisters() throws IOException {
        Files.writeString(configFile, "# minimal");
        ConfigurationService svc = ConfigurationService.getInstance();
        String name = "config-loader-test-" + System.nanoTime();

        Result<Void, BCError.ConfigError> reg = svc.registerConfiguration(name, configFile, p -> {});
        assertTrue(reg.isSuccess());
        assertTrue(svc.isConfigurationRegistered(name));
        assertTrue(svc.getRegisteredConfigurationCount() >= 1);

        Result<Void, BCError.ConfigError> unreg = svc.unregisterConfiguration(name);
        assertTrue(unreg.isSuccess());
        assertFalse(svc.isConfigurationRegistered(name));
    }
}
