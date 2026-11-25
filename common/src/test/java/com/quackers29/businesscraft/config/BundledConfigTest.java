package com.quackers29.businesscraft.config;

import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.testutil.ProductionTestEnv;
import com.quackers29.businesscraft.world.BiomeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Review item E1: the CSV files shipped in the jar are exactly what a fresh install gets, and
 * they must pass validation with zero warnings. This would have caught the unknown
 * {@code border_basic} starting node in biomes.csv.
 */
class BundledConfigTest {

    @TempDir
    Path tempDir;

    private ProductionTestEnv env;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        env = ProductionTestEnv.install(tempDir);
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void freshInstall_bundledCsvsLoadWithZeroWarnings() {
        env.loadBundledRegistries();
        ConfigValidator.validate();

        assertEquals(java.util.List.of(), ConfigWarnings.all(), "bundled CSVs must be warning-free");
    }

    @Test
    void freshInstall_writesAllFourFilesFromTheJar() {
        env.loadBundledRegistries();

        for (String name : java.util.List.of("items.csv", "productions.csv", "upgrades.csv", "biomes.csv")) {
            assertTrue(Files.exists(env.businesscraftDir().resolve(name)), name + " should be installed");
        }
    }

    @Test
    void freshInstall_hasTheDesignedRecipesAndNodes() {
        env.loadBundledRegistries();

        assertEquals(9, ProductionRegistry.getAll().size());
        assertEquals(12, UpgradeRegistry.getAll().size());
        // tourism generation depends on this recipe existing
        assertNotNull(ProductionRegistry.get("tourism"));
        assertNotNull(ProductionRegistry.get("taxes"));
        assertNotNull(ProductionRegistry.get("work_unit_production"));
        assertNotNull(UpgradeRegistry.get("basic_settlement"));
        assertNull(UpgradeRegistry.get("basic_taxes"), "stale fallback id must be gone");
        assertNull(ProductionRegistry.get("basic_farming"), "stale fallback id must be gone");
    }

    @Test
    void freshInstall_everyBiomeKitStartsWithBasicSettlement() {
        env.loadBundledRegistries();

        assertFalse(BiomeRegistry.getAllKits().isEmpty());
        for (BiomeRegistry.BiomeKit kit : BiomeRegistry.getAllKits()) {
            assertTrue(kit.startingNodes.contains("basic_settlement"), kit.variantId);
        }
    }

    @Test
    void bundledToml_hasNoDeadTickIntervalAndCommentsAboveKeys() throws Exception {
        String toml = new String(BundledConfigTest.class
                .getResourceAsStream("/assets/businesscraft/config/businesscraft.toml").readAllBytes());
        String production = toml.substring(toml.indexOf("[production]"), toml.indexOf("[trading]"));
        assertFalse(production.contains("tickInterval = 100"), "production.tickInterval was dead and is removed");
        assertTrue(production.contains("dailyTickInterval"));

        String research = toml.substring(toml.indexOf("[research]"), toml.indexOf("[jade]"));
        assertTrue(research.indexOf('#') < research.indexOf("enabled"), "research comment must sit above the key");
    }
}
