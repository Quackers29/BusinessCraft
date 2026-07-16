package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.PlatformHelper;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.production.UpgradeNode;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.world.BiomeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 2 toggle honesty: biome-kit filtering under research/production toggles,
 * and calculateWants gated by researchEnabled.
 */
class ToggleHonestyTest {

    private static final UUID TOWN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final BlockPos TOWN_POS = new BlockPos(10, 64, 10);

    @TempDir
    Path tempDir;

    private Town town;
    private Map<String, UpgradeNode> savedNodes;
    private Map<String, ResourceType> savedResources;
    private Object savedPlatform;
    private Object savedRegistry;

    private boolean savedResearch;
    private boolean savedProduction;
    private int savedDefaultPop;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() throws Exception {
        savedResearch = ConfigLoader.researchEnabled;
        savedProduction = ConfigLoader.productionEnabled;
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        ConfigLoader.defaultStartingPopulation = 5;

        savedNodes = snapshotUpgradeNodes();
        savedResources = snapshotResources();
        savedPlatform = PlatformAccess.platform;
        savedRegistry = PlatformAccess.registry;

        PlatformAccess.platform = new TestPlatformHelper(tempDir);
        PlatformAccess.registry = new TestRegistryHelper();

        Path bcDir = tempDir.resolve("businesscraft");
        Files.createDirectories(bcDir);
        // Mimic production upgrades.csv: basic_settlement grants tourism + tourist_cap;
        // farming_basic cascades from basic_settlement effect name.
        Files.writeString(bcDir.resolve("upgrades.csv"),
                "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n" +
                        "basic_settlement,housing,Basic Settlement,,,Unlocks basic survival,0,,pop_cap:100;tourist_cap:2;storage_cap_all:2000;happiness:50;tourism;farming_basic\n" +
                        "farming_basic,farming,Farming,,basic_settlement,Starts food production,0.5,food:100,farming\n" +
                        "tourism_basic,general,Tourism,,basic_settlement,Tourism,0.5,money:100,tourism\n" +
                        "research_basic,general,Research Basic,,basic_settlement,Research speed boost,0.25,wood:2000,research:0.25\n");
        UpgradeRegistry.load();

        town = new Town(TOWN_ID, TOWN_POS, "ToggleTown");
    }

    @AfterEach
    void tearDown() throws Exception {
        ConfigLoader.researchEnabled = savedResearch;
        ConfigLoader.productionEnabled = savedProduction;
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
        PlatformAccess.platform = (PlatformHelper) savedPlatform;
        PlatformAccess.registry = (RegistryHelper) savedRegistry;
        restoreUpgradeNodes(savedNodes);
        restoreResources(savedResources);
    }

    @Test
    void researchOff_keepsBasicSettlement_skipsOtherNodes_preservesTourism() {
        ConfigLoader.researchEnabled = false;
        ConfigLoader.productionEnabled = false;

        BiomeRegistry.BiomeKit kit = kit(
                List.of("basic_settlement", "tourism_basic", "research_basic"),
                Map.of("pop", 6f, "happiness", 70f, "tourist", 2f, "tourist_cap", 3f, "border", 10f,
                        "food", 100f, "wood", 50f, "money", 80f));

        TownManager.applyBiomeKit(town, kit);

        assertTrue(town.getUpgrades().isUnlocked("basic_settlement"));
        assertFalse(town.getUpgrades().isUnlocked("tourism_basic"), "extra research nodes must be skipped");
        assertFalse(town.getUpgrades().isUnlocked("research_basic"));
        // Cascade from basic_settlement effect name matching a node id
        assertTrue(town.getUpgrades().isUnlocked("farming_basic"));

        assertTrue(town.getUpgrades().getModifier("tourism") > 0f, "Option C needs tourism modifier");
        assertTrue(town.getUpgrades().getModifier("tourist_cap") >= 2f);

        assertEquals(6, town.getPopulation());
        // getHappiness = base (70 from kit) + upgrade modifier (50 from basic_settlement), clamped to 100
        assertEquals(100f, town.getHappiness(), 1e-4f);
        assertEquals(2, town.getPendingTouristSpawns());
        assertEquals(10f, town.getUpgrades().getModifier("border"), 1e-4f);
        // kit tourist_cap flat + node tourist_cap:2 → at least 3 from flat alone
        assertTrue(town.getUpgrades().getModifier("tourist_cap") >= 3f);
    }

    @Test
    void researchOn_unlocksAllStartingNodes() {
        ConfigLoader.researchEnabled = true;
        ConfigLoader.productionEnabled = false;

        BiomeRegistry.BiomeKit kit = kit(
                List.of("basic_settlement", "tourism_basic", "research_basic"),
                Map.of("pop", 5f));

        TownManager.applyBiomeKit(town, kit);

        assertTrue(town.getUpgrades().isUnlocked("basic_settlement"));
        assertTrue(town.getUpgrades().isUnlocked("tourism_basic"));
        assertTrue(town.getUpgrades().isUnlocked("research_basic"));
    }

    @Test
    void productionOff_skipsResourceGrants_productionOn_addsThem() throws Exception {
        seedResource("food", Items.BREAD);

        ConfigLoader.researchEnabled = false;
        ConfigLoader.productionEnabled = false;

        BiomeRegistry.BiomeKit kit = kit(
                List.of("basic_settlement"),
                Map.of("pop", 5f, "food", 1400f));

        TownManager.applyBiomeKit(town, kit);
        assertEquals(0, town.getResourceCount(Items.BREAD), "no production stock when production off");

        // Fresh town for the on case
        town = new Town(UUID.randomUUID(), TOWN_POS, "ProdOnTown");
        ConfigLoader.productionEnabled = true;
        TownManager.applyBiomeKit(town, kit);
        assertEquals(1400, town.getResourceCount(Items.BREAD));
    }

    @Test
    void calculateWants_researchOff_clearsAndNoOps() throws Exception {
        ConfigLoader.researchEnabled = false;

        // Seed a stale want then ensure calculateWants clears it
        Field wantsField = Town.class.getDeclaredField("wantedResources");
        wantsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Item, Long> wants = (Map<Item, Long>) wantsField.get(town);
        wants.put(Items.OAK_LOG, -10L);

        town.calculateWants();
        assertTrue(town.getWantedResources().isEmpty());
    }

    // --- helpers ---

    private static BiomeRegistry.BiomeKit kit(List<String> nodes, Map<String, Float> values) {
        return new BiomeRegistry.BiomeKit(
                List.of("minecraft:plains"),
                "test",
                "Test Kit",
                100,
                new ArrayList<>(nodes),
                new HashMap<>(values),
                "test");
    }

    private void seedResource(String id, Item item) throws Exception {
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
        ResourceType rt = new ResourceType(id, rl);
        rt.getEquivalents().put(rl, 1.0f);
        Field f = ResourceRegistry.class.getDeclaredField("RESOURCES");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ResourceType> map = (Map<String, ResourceType>) f.get(null);
        map.put(id, rt);
    }

    @SuppressWarnings("unchecked")
    private Map<String, UpgradeNode> snapshotUpgradeNodes() throws Exception {
        Field f = UpgradeRegistry.class.getDeclaredField("NODES");
        f.setAccessible(true);
        return new HashMap<>((Map<String, UpgradeNode>) f.get(null));
    }

    @SuppressWarnings("unchecked")
    private void restoreUpgradeNodes(Map<String, UpgradeNode> snapshot) throws Exception {
        Field f = UpgradeRegistry.class.getDeclaredField("NODES");
        f.setAccessible(true);
        Map<String, UpgradeNode> cur = (Map<String, UpgradeNode>) f.get(null);
        cur.clear();
        if (snapshot != null) {
            cur.putAll(snapshot);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ResourceType> snapshotResources() throws Exception {
        Field f = ResourceRegistry.class.getDeclaredField("RESOURCES");
        f.setAccessible(true);
        return new HashMap<>((Map<String, ResourceType>) f.get(null));
    }

    @SuppressWarnings("unchecked")
    private void restoreResources(Map<String, ResourceType> snapshot) throws Exception {
        Field f = ResourceRegistry.class.getDeclaredField("RESOURCES");
        f.setAccessible(true);
        Map<String, ResourceType> cur = (Map<String, ResourceType>) f.get(null);
        cur.clear();
        if (snapshot != null) {
            cur.putAll(snapshot);
        }
    }

    private static class TestPlatformHelper implements PlatformHelper {
        private final Path configDir;

        TestPlatformHelper(Path configDir) {
            this.configDir = configDir;
        }

        @Override
        public Path getConfigDirectory() {
            return configDir;
        }

        @Override
        public String getModId() {
            return "businesscraft";
        }

        @Override
        public boolean isClientSide() {
            return false;
        }

        @Override
        public boolean isServerSide() {
            return true;
        }

        @Override
        public String getPlatformName() {
            return "test";
        }
    }

    private static class TestRegistryHelper implements RegistryHelper {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.Block> java.util.function.Supplier<T> registerBlock(
                String name, java.util.function.Supplier<T> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Item> java.util.function.Supplier<T> registerBlockItem(String name,
                java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.entity.EntityType<?>> java.util.function.Supplier<T> registerEntityType(
                String name, java.util.function.Supplier<T> entityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.entity.BlockEntityType<?>> java.util.function.Supplier<T> registerBlockEntityType(
                String name, java.util.function.Supplier<T> blockEntityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.MenuType<?>> java.util.function.Supplier<T> registerMenuType(
                String name, java.util.function.Supplier<T> menuType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.AbstractContainerMenu> java.util.function.Supplier<net.minecraft.world.inventory.MenuType<T>> registerExtendedMenuType(
                String name, MenuFactory<T> factory) {
            return () -> null;
        }

        @Override
        public Item getItem(ResourceLocation location) {
            if (location == null)
                return null;
            return BuiltInRegistries.ITEM.get(location);
        }

        @Override
        public ResourceLocation getItemKey(Item item) {
            if (item == null)
                return null;
            return BuiltInRegistries.ITEM.getKey(item);
        }

        @Override
        public Iterable<Item> getItems() {
            return List.of(Items.BREAD, Items.EMERALD, Items.OAK_LOG);
        }
    }
}
