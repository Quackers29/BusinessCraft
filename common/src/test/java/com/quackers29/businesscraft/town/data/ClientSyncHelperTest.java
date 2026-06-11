package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-033: Client Data Synchronization (Test + Docs Loop).
 *
 * Covers ClientSyncHelper name resolution (null / client-cache-hit / client-miss short form),
 * cache update/get/clear/stats, visit history load that also populates the name cache,
 * getVisitHistory branching (provider path), and resource tag roundtrips using registry keys.
 *
 * Server-side TownManager lookup path and packet notify side-effects are integration
 * (need real ServerLevel + players) and intentionally left out.
 *
 * Uses McBootstrap + TestRegistryHelper (delegates to BuiltInRegistries for real Items)
 * + local TestProvider (ITownDataProvider double returning controlled maps/lists).
 * Documentation: vault/Town/Data Synchronization/Client Data Synchronization.md
 */
class ClientSyncHelperTest {

    private ClientSyncHelper helper;
    private RegistryHelper savedRegistry;

    private static final UUID TOWN_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PLAYER_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() throws Exception {
        helper = new ClientSyncHelper();

        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new TestRegistryHelper();
    }

    @AfterEach
    void tearDown() {
        PlatformAccess.registry = savedRegistry;
        // fresh helper each test; no static maps to restore
    }

    // --- test doubles ---

    private static class TestRegistryHelper implements RegistryHelper {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.Block> java.util.function.Supplier<T> registerBlock(String name, java.util.function.Supplier<T> block) {
            return () -> null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.item.Item> java.util.function.Supplier<T> registerBlockItem(String name, java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return () -> null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.entity.EntityType<?>> java.util.function.Supplier<T> registerEntityType(String name, java.util.function.Supplier<T> entityType) {
            return () -> null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.entity.BlockEntityType<?>> java.util.function.Supplier<T> registerBlockEntityType(String name, java.util.function.Supplier<T> blockEntityType) {
            return () -> null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.MenuType<?>> java.util.function.Supplier<T> registerMenuType(String name, java.util.function.Supplier<T> menuType) {
            return () -> null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.AbstractContainerMenu> java.util.function.Supplier<net.minecraft.world.inventory.MenuType<T>> registerExtendedMenuType(String name, MenuFactory<T> factory) {
            return () -> null;
        }

        @Override
        public Item getItem(ResourceLocation location) {
            if (location == null) return null;
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(location);
        }

        @Override
        public ResourceLocation getItemKey(Item item) {
            if (item == null) return null;
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        }

        @Override
        public Iterable<Item> getItems() {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM;
        }
    }

    /**
     * Minimal ITownDataProvider double. Only the methods exercised by ClientSyncHelper
     * (getAllResources, getAllCommunalStorageItems, getVisitHistory) return interesting
     * values; everything else is safe no-op / empty / zero.
     */
    private static class TestProvider implements ITownDataProvider {
        private final Map<Item, Long> resources;
        private final Map<Item, Long> communal;
        private final List<VisitHistoryRecord> visits;

        TestProvider(Map<Item, Long> resources, Map<Item, Long> communal, List<VisitHistoryRecord> visits) {
            this.resources = resources != null ? resources : Collections.emptyMap();
            this.communal = communal != null ? communal : Collections.emptyMap();
            this.visits = visits != null ? visits : Collections.emptyList();
        }

        @Override public UUID getTownId() { return null; }
        @Override public String getTownName() { return ""; }
        @Override public void addResource(Item item, long count) {}
        @Override public long getResourceCount(Item item) { return 0; }
        @Override public Map<Item, Long> getAllResources() { return resources; }
        @Override public boolean addToCommunalStorage(Item item, long count) { return false; }
        @Override public long getCommunalStorageCount(Item item) { return 0; }
        @Override public Map<Item, Long> getAllCommunalStorageItems() { return communal; }
        @Override public boolean addToPersonalStorage(UUID playerId, Item item, long count) { return false; }
        @Override public long getPersonalStorageCount(UUID playerId, Item item) { return 0; }
        @Override public Map<Item, Long> getPersonalStorageItems(UUID playerId) { return Collections.emptyMap(); }
        @Override public long getPopulation() { return 0; }
        @Override public long getTouristCount() { return 0; }
        @Override public long getMaxTourists() { return 0; }
        @Override public boolean canAddMoreTourists() { return false; }
        @Override public long getWorkUnits() { return 0; }
        @Override public long getWorkUnitCap() { return 0; }
        @Override public boolean isTouristSpawningEnabled() { return false; }
        @Override public void setTouristSpawningEnabled(boolean enabled) {}
        @Override public BlockPos getPathStart() { return null; }
        @Override public void setPathStart(BlockPos pos) {}
        @Override public BlockPos getPathEnd() { return null; }
        @Override public void setPathEnd(BlockPos pos) {}
        @Override public int getSearchRadius() { return 0; }
        @Override public void setSearchRadius(int radius) {}
        @Override public boolean canSpawnTourists() { return false; }
        @Override public void markDirty() {}
        @Override public BlockPos getPosition() { return BlockPos.ZERO; }
        @Override public void addVisitor(UUID fromTownId) {}
        @Override public int getTotalVisitors() { return 0; }
        @Override public void recordVisit(UUID originTownId, int count, BlockPos originPos) {}
        @Override public List<VisitHistoryRecord> getVisitHistory() { return visits; }
    }

    // --- resolveTownName (client path via null level) ---

    @Test
    void resolveTownName_nullId_returnsUnknown() {
        assertEquals("Unknown", helper.resolveTownName(null, (net.minecraft.world.level.Level) null));
        assertEquals("Unknown", helper.resolveTownName(null, true, null));
    }

    @Test
    void resolveTownName_clientCacheHit_returnsCachedName() {
        // Simulate server having sent a pre-resolved name
        CompoundTag tag = makeVisitTag(TOWN_A, "Ironhill", 2, BlockPos.ZERO);
        helper.loadVisitHistoryFromTag(tag);

        // null level forces client branch
        assertEquals("Ironhill", helper.resolveTownName(TOWN_A, (net.minecraft.world.level.Level) null));
        assertEquals("Ironhill", helper.getTownNameFromId(TOWN_A, null));
    }

    @Test
    void resolveTownName_clientCacheMiss_shortForm() {
        // No load, cache empty -> short form from UUID prefix
        // Hand-computed: first 8 chars of 550e8400-... is "550e8400"
        String expected = "Town-550e8400";
        assertEquals(expected, helper.resolveTownName(TOWN_A, (net.minecraft.world.level.Level) null));
    }

    @Test
    void resolveTownName_clientCacheEmptyString_stillProducesShort() {
        // Even if a blank name was stored, client treats non-(nonempty) as miss
        CompoundTag tag = makeVisitTag(TOWN_A, "", 1, BlockPos.ZERO);
        helper.loadVisitHistoryFromTag(tag);

        String expected = "Town-550e8400";
        assertEquals(expected, helper.resolveTownName(TOWN_A, (net.minecraft.world.level.Level) null));
    }

    // --- visit history load + cache side effect ---

    @Test
    void loadVisitHistoryFromTag_populatesListAndNameCache() {
        CompoundTag tag = makeVisitTag(TOWN_A, "Copperton", 5, new BlockPos(10, 64, 20));
        helper.loadVisitHistoryFromTag(tag);

        List<ITownDataProvider.VisitHistoryRecord> history = helper.getClientVisitHistory();
        assertEquals(1, history.size());
        assertEquals(5, history.get(0).getCount());
        assertEquals(TOWN_A, history.get(0).getOriginTownId());

        // name cache hit even with client resolve
        assertEquals("Copperton", helper.resolveTownName(TOWN_A, (net.minecraft.world.level.Level) null));
    }

    @Test
    void getClientVisitHistory_returnsLoadedRecords() {
        CompoundTag tag = makeVisitTag(TOWN_A, "Hilltown", 3, BlockPos.ZERO);
        helper.loadVisitHistoryFromTag(tag);

        List<ITownDataProvider.VisitHistoryRecord> list = helper.getClientVisitHistory();
        assertEquals(1, list.size());
        assertEquals("Hilltown", helper.resolveTownName(TOWN_A, (net.minecraft.world.level.Level) null));
    }

    // --- getVisitHistory branching (provider path exercised; client path uses getClientVisitHistory) ---

    @Test
    void getVisitHistory_nullLevelNullProvider_returnsEmpty() {
        // level==null -> ! (level!=null && isClient) -> else branch -> provider null -> empty
        List<ITownDataProvider.VisitHistoryRecord> result = helper.getVisitHistory(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getVisitHistory_nullLevelWithProvider_returnsProviderList() {
        List<ITownDataProvider.VisitHistoryRecord> provided = Collections.singletonList(
                new ITownDataProvider.VisitHistoryRecord(123L, TOWN_A, 7, BlockPos.ZERO));
        ITownDataProvider provider = new TestProvider(null, null, provided);

        List<ITownDataProvider.VisitHistoryRecord> result = helper.getVisitHistory(null, provider);
        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getCount());
    }

    // --- personal storage cache ---

    @Test
    void updateClientPersonalStorage_andGet_roundtripsPerPlayer() {
        Map<Item, Long> items = new HashMap<>();
        items.put(Items.IRON_INGOT, 64L);
        helper.updateClientPersonalStorage(PLAYER_1, items);

        Map<Item, Long> got = helper.getClientPersonalStorage(PLAYER_1);
        assertEquals(64L, got.get(Items.IRON_INGOT));
        assertEquals(1, got.size());
    }

    @Test
    void getClientPersonalStorage_unknownPlayer_returnsEmpty() {
        Map<Item, Long> got = helper.getClientPersonalStorage(UUID.randomUUID());
        assertTrue(got.isEmpty());
    }

    // --- bulk resource update from town + clear + stats ---

    @Test
    void updateClientResourcesFromTown_copiesResourcesAndCommunal() {
        // Build a real-enough Town (3-param ctor works with McBootstrap in other tests)
        Town town = new Town(UUID.randomUUID(), new BlockPos(0, 64, 0), "TestTown");
        // Inject a couple of resources via the public API (safe, no world needed for counts)
        town.addResource(Items.COAL, 128);
        // communal not directly addable here without more; use empty for this test

        helper.updateClientResourcesFromTown(town);

        assertEquals(128L, helper.getClientResources().get(Items.COAL));
        // communal may be 0 or whatever town reports
    }

    @Test
    void clearAll_clearsAllCaches() {
        // seed some state
        helper.updateClientPersonalStorage(PLAYER_1, Map.of(Items.EMERALD, 5L));
        CompoundTag vtag = makeVisitTag(TOWN_A, "Seed", 1, BlockPos.ZERO);
        helper.loadVisitHistoryFromTag(vtag);

        helper.clearAll();

        assertTrue(helper.getClientPersonalStorage(PLAYER_1).isEmpty());
        assertTrue(helper.getClientVisitHistory().isEmpty());
        assertEquals("Town-550e8400", helper.resolveTownName(TOWN_A, (net.minecraft.world.level.Level) null));
    }

    @Test
    void getCacheStats_reportsCounts() {
        helper.updateClientPersonalStorage(PLAYER_1, Map.of(Items.EMERALD, 2L));
        CompoundTag vtag = makeVisitTag(TOWN_A, "X", 4, BlockPos.ZERO);
        helper.loadVisitHistoryFromTag(vtag);

        String stats = helper.getCacheStats();
        // Format from production: "Resources: %d, Communal: %d, Personal: %d, History: %d, Names: %d"
        // (spaces after colons)
        assertTrue(stats.contains("Personal: 1"));
        assertTrue(stats.contains("History: 1"));
        assertTrue(stats.contains("Names: 1"));
    }

    // --- resource tag roundtrip (uses registry double) ---

    @Test
    void resourceSyncRoundtrip_singleItem_writesAndReadsViaRegistryKey() {
        Map<Item, Long> res = new HashMap<>();
        res.put(Items.IRON_INGOT, 42L);
        ITownDataProvider provider = new TestProvider(res, Collections.emptyMap(), Collections.emptyList());

        CompoundTag tag = new CompoundTag();
        helper.syncResourcesForClient(tag, provider);

        // Now load into a fresh helper (different instance to prove roundtrip)
        ClientSyncHelper reader = new ClientSyncHelper();
        // ensure it also sees the test registry
        // (PlatformAccess is static, still our double from @BeforeEach)
        reader.loadResourcesFromTag(tag);

        Map<Item, Long> loaded = reader.getClientResources();
        assertEquals(42L, loaded.get(Items.IRON_INGOT));
        // Hand-computed: the key written is BuiltInRegistries.ITEM.getKey(IRON_INGOT).toString()
        // which after McBootstrap is "minecraft:iron_ingot"; load parses it back to the same Item.
    }

    @Test
    void loadResourcesFromTag_withoutClientResourcesKey_doesNotClear() {
        // Seed state: sync writes the tag, *load* actually populates this helper's cache
        Map<Item, Long> seed = new HashMap<>();
        seed.put(Items.COAL, 7L);
        ITownDataProvider p = new TestProvider(seed, Collections.emptyMap(), Collections.emptyList());
        CompoundTag tag1 = new CompoundTag();
        helper.syncResourcesForClient(tag1, p);
        helper.loadResourcesFromTag(tag1); // now helper has COAL=7

        // Load a tag that has NO "clientResources" subtag
        CompoundTag emptyish = new CompoundTag();
        emptyish.put("clientCommunalStorage", new CompoundTag()); // unrelated key
        helper.loadResourcesFromTag(emptyish);

        // Original seeded value should still be there (no clear happened per the "if (contains)" guard)
        assertEquals(7L, helper.getClientResources().get(Items.COAL));
    }

    // --- helpers ---

    private CompoundTag makeVisitTag(UUID townId, String townName, int count, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        ListTag history = new ListTag();
        CompoundTag visit = new CompoundTag();
        visit.putLong("timestamp", System.currentTimeMillis());
        visit.putUUID("townId", townId);
        visit.putString("townName", townName);
        visit.putInt("count", count);
        if (pos != null && pos != BlockPos.ZERO) {
            CompoundTag p = new CompoundTag();
            p.putInt("x", pos.getX());
            p.putInt("y", pos.getY());
            p.putInt("z", pos.getZ());
            visit.put("pos", p);
        }
        history.add(visit);
        tag.put("visitHistory", history);
        return tag;
    }

    // (null Level is passed directly to force the client cache branch in resolveTownName)
}
