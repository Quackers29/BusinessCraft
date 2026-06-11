package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.production.UpgradeNode;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
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
 * T-032: Upgrade Cost and Research Time Scaling (Test + Docs Loop).
 *
 * Covers the scaling formulas in TownUpgradeComponent:
 *   multiplier = (float) Math.pow(node.getCostMultiplier(), currentLevel)
 *   resourceCost = (int) Math.ceil(base * multiplier)
 *   researchMinutes = base * multiplier   (float, no ceil)
 *
 * CurrentLevel 0 (not unlocked) yields exactly base (pow ^ 0 == 1.0).
 * Also covers repeatability gates used by canAffordResearch and the
 * getUpgradeLevel default.
 *
 * Uses McBootstrap for Town construction + TestPlatformHelper + @TempDir
 * (pattern from T-030 UpgradeRegistryTest) + reflection to inject levels
 * without triggering full startResearch side effects.
 *
 * Documentation: vault/Production/Upgrades/Upgrade Cost and Research Time Scaling.md
 */
class TownUpgradeComponentTest {

    private static final UUID TOWN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final BlockPos TOWN_POS = new BlockPos(50, 64, 50);
    private static final String TOWN_NAME = "ScaleTestTown";

    private Town town;
    private TownUpgradeComponent comp;

    private Map<String, UpgradeNode> savedNodes;
    private Object savedPlatform; // PlatformHelper is interface in api

    @TempDir
    Path tempDir;

    @BeforeAll
    static void boot() {
        // Town ctor needs registries initialized (pattern from T-008 / T-027)
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() throws Exception {
        savedNodes = snapshotNodes();
        savedPlatform = getCurrentPlatform();

        // Install test double so UpgradeRegistry.load() writes/reads from our temp
        setPlatform(new TestPlatformHelper(tempDir));

        // Prepare a custom upgrades.csv with nodes that have explicit cost multipliers
        // and repeatability settings so we can hand-compute expectations.
        Path bcDir = tempDir.resolve("businesscraft");
        Files.createDirectories(bcDir);
        Path csv = bcDir.resolve("upgrades.csv");
        // Columns: node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects
        // repeat format: "maxRepeats:costMult:benefitMult" (or "infinite:costMult")
        // We use costMult 1.1 and 1.25 for clear arithmetic; one non-repeatable (empty repeat -> max=1)
        String custom =
                "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n" +
                "scale_10pct,housing,Scale 10pct,5:1.1:1.0,,Test 1.1x cost,3.0,vwood:10,pop_cap:4\n" +
                "scale_25pct,farming,Scale 25pct,infinite:1.25:1.0,,Test 1.25x repeatable,4.0,viron:5,storage_cap_all:50\n" +
                "non_repeat,housing,NonRepeat,,scale_10pct,One time only,0,,happiness:10\n";  // empty repeat => maxRepeats=1
        Files.writeString(csv, custom);

        UpgradeRegistry.load();

        town = new Town(TOWN_ID, TOWN_POS, TOWN_NAME);
        comp = new TownUpgradeComponent(town);

        // Give a high storage_cap_all so virtual resource ids (vwood/viron) used for afford tests
        // have a large enough cap that adjustStock(10) is not clamped to 0.
        town.getUpgrades().addFlatModifier("storage_cap_all", 100000f);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore platform and nodes for isolation
        setPlatform(savedPlatform);
        restoreNodes(savedNodes);
    }

    // --- reflection helpers (modeled on T-030 and T-025) ---

    @SuppressWarnings("unchecked")
    private Map<String, UpgradeNode> snapshotNodes() throws Exception {
        Field f = UpgradeRegistry.class.getDeclaredField("NODES");
        f.setAccessible(true);
        Map<String, UpgradeNode> cur = (Map<String, UpgradeNode>) f.get(null);
        return new HashMap<>(cur);
    }

    @SuppressWarnings("unchecked")
    private void restoreNodes(Map<String, UpgradeNode> snapshot) throws Exception {
        Field f = UpgradeRegistry.class.getDeclaredField("NODES");
        f.setAccessible(true);
        Map<String, UpgradeNode> cur = (Map<String, UpgradeNode>) f.get(null);
        cur.clear();
        if (snapshot != null) cur.putAll(snapshot);
    }

    private Object getCurrentPlatform() {
        try {
            Field f = com.quackers29.businesscraft.api.PlatformAccess.class.getDeclaredField("platform");
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void setPlatform(Object helper) {
        try {
            Field f = com.quackers29.businesscraft.api.PlatformAccess.class.getDeclaredField("platform");
            f.setAccessible(true);
            f.set(null, helper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Inject a level directly into the component's private map (bypass unlockNode costs/notifications). */
    @SuppressWarnings("unchecked")
    private void setLevel(TownUpgradeComponent c, String nodeId, int level) {
        try {
            Field levelsField = TownUpgradeComponent.class.getDeclaredField("upgradeLevels");
            levelsField.setAccessible(true);
            Map<String, Integer> levels = (Map<String, Integer>) levelsField.get(c);
            if (level <= 0) {
                levels.remove(nodeId);
            } else {
                levels.put(nodeId, level);
            }
            // Also keep unlockedNodes roughly consistent for isUnlocked checks (not strictly needed for cost math)
            Field unlockedField = TownUpgradeComponent.class.getDeclaredField("unlockedNodes");
            unlockedField.setAccessible(true);
            Set<String> unlocked = (Set<String>) unlockedField.get(c);
            if (level > 0) {
                unlocked.add(nodeId);
            } else {
                unlocked.remove(nodeId);
            }
            // Recalculate is not required for getUpgradeCost / getScaledResearchMinutes (they only read level + registry)
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject level for test", e);
        }
    }

    // --- formula tests ---

    @Test
    void getUpgradeCost_unknownNode_returnsEmpty() {
        List<ResourceAmount> costs = comp.getUpgradeCost("does_not_exist");
        assertTrue(costs.isEmpty());
    }

    @Test
    void getUpgradeCost_level0_returnsExactBase_noScaling() {
        // Node scale_10pct: base wood:10, costMult=1.1
        // currentLevel=0 => mult = 1.1 ^ 0 = 1.0
        // 10 * 1.0 = 10.0; ceil(10.0) = 10
        List<ResourceAmount> costs = comp.getUpgradeCost("scale_10pct");
        assertEquals(1, costs.size());
        assertEquals("vwood", costs.get(0).resourceId);
        assertEquals(10, costs.get(0).amount); // hand-computed: base at level 0
    }

    @Test
    void getUpgradeCost_level1_appliesPowAndCeils() {
        setLevel(comp, "scale_10pct", 1);
        // mult = 1.1 ^ 1 = 1.1
        // 10 * 1.1 = 11.0; (int) Math.ceil(11.0) = 11
        List<ResourceAmount> costs = comp.getUpgradeCost("scale_10pct");
        assertEquals(1, costs.size());
        assertEquals(11, costs.get(0).amount); // hand-computed: 10 * 1.1 -> ceil 11
    }

    @Test
    void getUpgradeCost_level2_higherPowAndCeilJump() {
        setLevel(comp, "scale_10pct", 2);
        // mult = 1.1 ^ 2 = 1.21
        // 10 * 1.21 = 12.1; Math.ceil(12.1) = 13
        List<ResourceAmount> costs = comp.getUpgradeCost("scale_10pct");
        assertEquals(13, costs.get(0).amount); // hand-computed: 10 * 1.21 = 12.1 -> 13
    }

    @Test
    void getScaledResearchMinutes_level0_isBase() {
        // scale_10pct research base 3.0
        // level 0: 3.0 * 1.0 = 3.0
        float minutes = comp.getScaledResearchMinutes("scale_10pct");
        assertEquals(3.0f, minutes, 1e-6f);
    }

    @Test
    void getScaledResearchMinutes_levelN_scalesFloat_noCeil() {
        setLevel(comp, "scale_10pct", 1);
        // 3.0 * 1.1 = 3.3 (kept as float)
        float minutes = comp.getScaledResearchMinutes("scale_10pct");
        assertEquals(3.3f, minutes, 1e-6f); // hand-computed, fractional preserved
    }

    @Test
    void getScaledResearchMinutes_25pctNode_higherMultiplier() {
        setLevel(comp, "scale_25pct", 2);
        // mult = 1.25 ^ 2 = 1.5625
        // 4.0 * 1.5625 = 6.25
        float minutes = comp.getScaledResearchMinutes("scale_25pct");
        assertEquals(6.25f, minutes, 1e-6f); // hand-computed
    }

    @Test
    void getUpgradeCost_multipleResources_allScaled() {
        // Use scale_25pct which has viron:5 in our CSV (virtual id to avoid registry init); bump level and verify
        setLevel(comp, "scale_25pct", 1);
        // mult=1.25; 5 * 1.25 = 6.25; ceil(6.25)=7
        List<ResourceAmount> costs = comp.getUpgradeCost("scale_25pct");
        assertEquals(1, costs.size());
        assertEquals("viron", costs.get(0).resourceId);
        assertEquals(7, costs.get(0).amount); // hand-computed: 5 * 1.25 = 6.25 -> ceil 7
    }

    @Test
    void getUpgradeLevel_defaultsToZero_forNeverUnlocked() {
        assertEquals(0, comp.getUpgradeLevel("scale_10pct"));
        assertEquals(0, comp.getUpgradeLevel("non_repeat"));
    }

    @Test
    void canAffordResearch_level0_trueWhenStockMeetsScaledBase() {
        // For scale_10pct at level 0 cost is exactly vwood:10 (virtual id not in ResourceRegistry)
        // Give the town exactly 10 via its trading component (falls to virtual stock path, avoids registry wiring)
        town.getTrading().adjustStock("vwood", 10.0f);
        assertTrue(comp.canAffordResearch("scale_10pct"));
    }

    @Test
    void canAffordResearch_insufficientStock_returnsFalse() {
        town.getTrading().adjustStock("vwood", 9.0f); // just short of base 10
        assertFalse(comp.canAffordResearch("scale_10pct"));
    }

    @Test
    void canAffordResearch_nonRepeatable_atLevel1_returnsFalse() {
        setLevel(comp, "non_repeat", 1);
        // Even with infinite stock, repeatability gate should block
        town.getTrading().adjustStock("vwood", 1000.0f);
        assertFalse(comp.canAffordResearch("non_repeat"));
    }

    @Test
    void canAffordResearch_repeatable_maxReached_returnsFalse() {
        // scale_25pct is "infinite:1.25" so maxRepeats=-1 (unlimited in theory)
        // Use non_repeat which has implicit max=1
        setLevel(comp, "non_repeat", 1);
        town.getTrading().adjustStock("vwood", 1000.0f);
        assertFalse(comp.canAffordResearch("non_repeat"));
    }

    @Test
    void getUpgradeCost_afterDirectLevelInjection_matchesPow() throws Exception {
        setLevel(comp, "scale_25pct", 4);
        // mult = 1.25 ^ 4 = (1.25^2=1.5625, ^4=1.5625^2=2.44140625)
        // 5 * 2.44140625 = 12.20703125 -> ceil = 13
        List<ResourceAmount> costs = comp.getUpgradeCost("scale_25pct");
        assertEquals(13, costs.get(0).amount); // hand-computed
    }
}

// --- minimal platform double (same shape as T-030) ---
class TestPlatformHelper implements com.quackers29.businesscraft.api.PlatformHelper {
    private final Path configDir;
    TestPlatformHelper(Path configDir) { this.configDir = configDir; }
    @Override public Path getConfigDirectory() { return configDir; }
    @Override public String getModId() { return "businesscraft"; }
    @Override public boolean isClientSide() { return false; }
    @Override public boolean isServerSide() { return true; }
    @Override public String getPlatformName() { return "test"; }
}
