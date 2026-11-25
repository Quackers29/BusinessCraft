package com.quackers29.businesscraft.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.PlatformHelper;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.data.parsers.Effect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-030: Upgrade Registry Loading and Lookup (Test + Docs Loop).
 *
 * Covers UpgradeRegistry:
 *   - load() resolves config/businesscraft/upgrades.csv via Platform, creates defaults when missing
 *   - loadNodes parses the 9-field CSV (skips header), splits prereqs on ";", delegates effects/costs to DataParser
 *   - minutes default to 0 on bad parse; short/invalid lines dropped
 *   - get(id) / getAll() surface the live NODES map
 *
 * Uses @TempDir + TestPlatformHelper (like T-019 ResourceRegistryTest) + reflection snapshot/restore
 * of the private static NODES map for isolation. No McBootstrap required (no Item/registries touched).
 * Documentation: vault/Production/Upgrades/Upgrade Registry Loading and Lookup.md
 */
class UpgradeRegistryTest {

    private Map<String, UpgradeNode> savedNodes;
    private PlatformHelper savedPlatform;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        savedNodes = snapshotNodes();
        savedPlatform = PlatformAccess.platform;
        // Install test double before any load() call
        PlatformAccess.platform = new TestPlatformHelper(tempDir);
        com.quackers29.businesscraft.config.ConfigWarnings.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        com.quackers29.businesscraft.config.ConfigWarnings.clear();
        // Restore platform first
        PlatformAccess.platform = savedPlatform;
        // Restore exact prior NODES contents
        restoreNodes(savedNodes);
    }

    // --- reflection helpers (pattern from T-019 and T-025) ---

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
        if (snapshot != null) {
            cur.putAll(snapshot);
        }
    }

    private void clearNodes() throws Exception {
        Field f = UpgradeRegistry.class.getDeclaredField("NODES");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, UpgradeNode> cur = (Map<String, UpgradeNode>) f.get(null);
        cur.clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, UpgradeNode> getNodesMap() throws Exception {
        Field f = UpgradeRegistry.class.getDeclaredField("NODES");
        f.setAccessible(true);
        return (Map<String, UpgradeNode>) f.get(null);
    }

    // --- test double ---

    private static class TestPlatformHelper implements PlatformHelper {
        private final Path configDir;
        TestPlatformHelper(Path configDir) { this.configDir = configDir; }
        @Override public Path getConfigDirectory() { return configDir; }
        @Override public String getModId() { return "businesscraft"; }
        @Override public boolean isClientSide() { return false; }
        @Override public boolean isServerSide() { return true; }
        @Override public String getPlatformName() { return "test"; }
    }

    // --- default load behavior ---

    @Test
    void load_missingFile_installsBundledCsvAndRegistersItsNodes() throws Exception {
        Path bcDir = tempDir.resolve("businesscraft");
        Path csv = bcDir.resolve("upgrades.csv");
        Files.deleteIfExists(csv);

        UpgradeRegistry.load();

        assertTrue(Files.exists(csv), "bundled upgrades.csv should be installed");
        String content = Files.readString(csv);
        assertTrue(content.startsWith(
                "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects"));

        UpgradeNode basic = UpgradeRegistry.get("basic_settlement");
        assertNotNull(basic);
        assertEquals("Basic Settlement", basic.getDisplayName());
        assertEquals(0f, basic.getResearchMinutes(), 1e-6f);
        assertTrue(basic.getCosts().isEmpty());
        assertFalse(basic.getEffects().isEmpty());

        UpgradeNode farm = UpgradeRegistry.get("farming_basic");
        assertNotNull(farm);
        assertEquals(1, farm.getCosts().size());
        assertEquals("food", farm.getCosts().get(0).resourceId);
        assertEquals(100, farm.getCosts().get(0).amount);
        // the old hardcoded fallback ids no longer exist
        assertNull(UpgradeRegistry.get("basic_taxes"));
    }

    @Test
    void load_existingFile_isNeverOverwritten() throws Exception {
        Path bcDir = tempDir.resolve("businesscraft");
        Files.createDirectories(bcDir);
        Path csv = bcDir.resolve("upgrades.csv");
        String custom = "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n"
                + "mine,cat,Mine,,,d,1,,e:1\n";
        Files.writeString(csv, custom);

        UpgradeRegistry.load();

        assertEquals(custom, Files.readString(csv));
        assertEquals(1, UpgradeRegistry.getAll().size());
    }

    @Test
    void load_bundledPrereqsAndEffects() throws Exception {
        UpgradeRegistry.load();

        UpgradeNode farm = UpgradeRegistry.get("farming_basic");
        assertNotNull(farm);
        assertEquals(List.of("basic_settlement"), farm.getPrereqNodes());
        assertEquals(1, farm.getEffects().size());
        assertEquals("farming", farm.getEffects().get(0).getTarget());
    }

    // --- get / getAll ---

    @Test
    void get_unknownId_returnsNull() throws Exception {
        UpgradeRegistry.load();
        assertNull(UpgradeRegistry.get("does_not_exist"));
    }

    @Test
    void getAll_returnsAllLoadedNodes() throws Exception {
        Path bcDir = tempDir.resolve("businesscraft");
        Files.createDirectories(bcDir);
        UpgradeRegistry.load();
        Collection<UpgradeNode> all = UpgradeRegistry.getAll();
        assertEquals(12, all.size());
        Set<String> ids = new HashSet<>();
        for (UpgradeNode n : all) ids.add(n.getId());
        assertTrue(ids.contains("basic_settlement"));
        assertTrue(ids.contains("farming_basic"));
    }

    // --- custom CSV + bad-line resilience (mirrors T-019 patterns) ---

    @Test
    void load_customCsv_parsesGoodRowsAndSkipsBadOnes() throws Exception {
        Path bcDir = tempDir.resolve("businesscraft");
        Files.createDirectories(bcDir);
        Path csv = bcDir.resolve("upgrades.csv");
        String custom =
                "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n" +
                "good_one,housing,Good Node,, ,Desc,2.5,iron:5,pop_cap:3\n" +
                "bad_short,only,three,fields\n" +                    // <9 columns -> skipped with a warning
                "weird_minutes,housing,Weird Min,, ,Desc,not_a_float, ,happiness:10\n" +  // bad minutes -> 0
                ",,Name,, , ,1, ,effect:1\n" +                       // empty id -> skipped with a warning
                "good_two,farming,Good Two,basic,good_one, ,0,wood:2, \n"; // prereqs + costs + empty effects ok
        Files.writeString(csv, custom);

        UpgradeRegistry.load();

        // good_one
        UpgradeNode g1 = UpgradeRegistry.get("good_one");
        assertNotNull(g1);
        assertEquals(2.5f, g1.getResearchMinutes(), 1e-6f);
        assertEquals(1, g1.getCosts().size());
        assertEquals("iron", g1.getCosts().get(0).resourceId);
        assertEquals(5, g1.getCosts().get(0).amount);
        assertEquals(1, g1.getEffects().size());
        assertEquals("pop_cap", g1.getEffects().get(0).getTarget());
        assertEquals(3f, g1.getEffects().get(0).getValue(), 1e-6f);

        // weird_minutes -> minutes treated as 0 (try/catch leaves the init value)
        UpgradeNode weird = UpgradeRegistry.get("weird_minutes");
        assertNotNull(weird);
        assertEquals(0f, weird.getResearchMinutes(), 1e-6f);
        assertEquals(1, weird.getEffects().size());

        // good_two has prereq list and cost
        UpgradeNode g2 = UpgradeRegistry.get("good_two");
        assertNotNull(g2);
        assertEquals(0f, g2.getResearchMinutes(), 1e-6f);
        assertEquals(1, g2.getPrereqNodes().size());
        assertEquals("good_one", g2.getPrereqNodes().get(0));
        assertEquals(1, g2.getCosts().size());

        // bad short line never registered
        assertNull(UpgradeRegistry.get("bad_short"));
        assertNull(UpgradeRegistry.get("only"));
    }

    @Test
    void load_replacesPriorNodesOnSubsequentCall() throws Exception {
        // Seed a node manually
        clearNodes();
        Path bcDir = tempDir.resolve("businesscraft");
        Files.createDirectories(bcDir);
        // We can't easily construct a full node without its internal parse, so load once then reload from empty
        // First load defaults
        UpgradeRegistry.load();
        assertNotNull(UpgradeRegistry.get("basic_settlement"));

        // Now remove the csv so next load would recreate defaults (but we just want to prove clear + repopulate)
        // Instead: write a csv with only one node and reload
        Path csv = bcDir.resolve("upgrades.csv");
        Files.createDirectories(bcDir);
        Files.writeString(csv,
                "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n" +
                "only_this,cat,Only,, ,d,3, ,e:1\n");

        UpgradeRegistry.load();

        assertNull(UpgradeRegistry.get("basic_settlement"), "prior nodes must be cleared on load");
        assertNotNull(UpgradeRegistry.get("only_this"));
        assertEquals(1, UpgradeRegistry.getAll().size());
    }

    // --- edge: empty effects/costs lists and prereq parsing ---

    @Test
    void load_prereqSemicolonSplit_andEmptyEffectsHandled() throws Exception {
        Path bcDir = tempDir.resolve("businesscraft");
        Files.createDirectories(bcDir);
        Path csv = bcDir.resolve("upgrades.csv");
        Files.writeString(csv,
                "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n" +
                "multi_prereq,cat,MP,,a;b;c ,d,1, ,e:2\n" +
                "no_prereq,cat,NP,,,,0,,effect:0\n");
        UpgradeRegistry.load();

        UpgradeNode mp = UpgradeRegistry.get("multi_prereq");
        assertNotNull(mp);
        assertEquals(3, mp.getPrereqNodes().size());
        assertEquals("a", mp.getPrereqNodes().get(0));
        assertEquals("c", mp.getPrereqNodes().get(2));

        UpgradeNode np = UpgradeRegistry.get("no_prereq");
        assertNotNull(np);
        assertTrue(np.getPrereqNodes().isEmpty());
        // effects/costs may be empty lists from DataParser on blank
        assertNotNull(np.getEffects());
        assertNotNull(np.getCosts());
    }

    // --- A3: same row rules as productions.csv ---

    private static final String UP_HEADER =
            "node_id,category,display_name,repeat,prereq_nodes,benefit_description,research_minutes,required_items,effects\n";

    @Test
    void load_rowWithEmptyEffectsColumn_isKept() throws Exception {
        Files.createDirectories(tempDir.resolve("businesscraft"));
        // trailing empty effects column: 9 columns, the last one empty
        Files.writeString(tempDir.resolve("businesscraft").resolve("upgrades.csv"),
                UP_HEADER + "no_effects,cat,No Effects,,,Just a marker,1,wood:5,\n");

        UpgradeRegistry.load();

        UpgradeNode n = UpgradeRegistry.get("no_effects");
        assertNotNull(n, "a node with an empty effects column must not be dropped");
        assertTrue(n.getEffects().isEmpty());
        assertEquals(1, n.getCosts().size());
    }

    @Test
    void load_quotedDescriptionWithComma_doesNotShiftColumns() throws Exception {
        Files.createDirectories(tempDir.resolve("businesscraft"));
        Files.writeString(tempDir.resolve("businesscraft").resolve("upgrades.csv"),
                UP_HEADER + "quoted,cat,Quoted,,,\"Bigger, better farms\",2.5,wood:5,pop_cap:3\n");

        UpgradeRegistry.load();

        UpgradeNode n = UpgradeRegistry.get("quoted");
        assertNotNull(n);
        assertEquals("Bigger, better farms", n.getDescription());
        assertEquals(2.5f, n.getResearchMinutes(), 1e-6f);
        assertEquals("pop_cap", n.getEffects().get(0).getTarget());
    }

    @Test
    void load_commentAndBlankLinesAreSkipped() throws Exception {
        Files.createDirectories(tempDir.resolve("businesscraft"));
        Files.writeString(tempDir.resolve("businesscraft").resolve("upgrades.csv"),
                UP_HEADER + "# === housing ===\n\nn1,cat,One,,,d,1,,pop_cap:1\n   # trailing note\n");

        UpgradeRegistry.load();

        assertEquals(1, UpgradeRegistry.getAll().size());
        assertTrue(com.quackers29.businesscraft.config.ConfigWarnings.all().isEmpty());
    }

    @Test
    void load_repeatColumnParsedIntoNode() throws Exception {
        Files.createDirectories(tempDir.resolve("businesscraft"));
        Files.writeString(tempDir.resolve("businesscraft").resolve("upgrades.csv"),
                UP_HEADER + "rep,cat,Rep,5:1.1:1.2,,d,1,,pop_cap:1\n");

        UpgradeRegistry.load();

        UpgradeNode n = UpgradeRegistry.get("rep");
        assertEquals(5, n.getMaxRepeats());
        assertEquals(1.1f, n.getCostMultiplier(), 1e-6f);
        assertEquals(1.2f, n.getBenefitMultiplier(), 1e-6f);
    }
}
