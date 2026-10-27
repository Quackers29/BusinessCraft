package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-023: Platform Management (Test + Docs Loop).
 *
 * Covers the collection semantics, cap enforcement, mutation notification,
 * defensive copies, enabled+complete filtering, NBT save/load/updateClient
 * orchestration, and transient UI state in
 * PlatformManager.
 *
 * Pure logic using only BlockPos/CompoundTag/UUID (allowed per protocol,
 * same as PlatformTest T-015 and VisitBufferTest T-010). No McBootstrap needed.
 *
 * Documentation: vault/Town/Platforms/Platform Management.md
 */
class PlatformManagerTest {

    private PlatformManager manager;
    private AtomicInteger changeCount;
    private Runnable callback;

    private static final BlockPos START_A = new BlockPos(100, 64, 200);
    private static final BlockPos END_A = new BlockPos(150, 64, 200);
    private static final BlockPos START_B = new BlockPos(300, 64, 400);
    private static final BlockPos END_B = new BlockPos(320, 64, 400);

    @BeforeEach
    void setUp() {
        manager = new PlatformManager();
        changeCount = new AtomicInteger(0);
        callback = changeCount::incrementAndGet;
        manager.setChangeCallback(callback);
    }

    // --- add / cap / naming ---

    @Test
    void addPlatform_firstAdd_succeedsWithDefaultNameAndNotifies() {
        boolean added = PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        assertTrue(added);
        assertEquals(1, changeCount.get());
        assertEquals(1, manager.getPlatformCount());
        assertTrue(manager.canAddMorePlatforms());
        assertEquals(10, manager.getMaxPlatforms());

        Platform p = manager.getPlatforms(false).get(0);
        assertEquals("Platform 1", p.getName());
        assertTrue(p.isEnabled());
        assertNull(p.getStartPos());
        assertNull(p.getEndPos());
    }

    @Test
    void addPlatform_multiple_sequentialNamesAndGrowth() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));

        assertEquals(3, manager.getPlatformCount());
        assertEquals(3, changeCount.get());

        List<Platform> all = manager.getPlatforms(false);
        assertEquals("Platform 1", all.get(0).getName());
        assertEquals("Platform 2", all.get(1).getName());
        assertEquals("Platform 3", all.get(2).getName());
    }

    @Test
    void addPlatform_atMax_returnsFalseAndDoesNotGrowOrNotifyExtra() {
        for (int i = 0; i < 10; i++) {
            assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager), "add " + (i + 1) + " should succeed");
        }
        assertEquals(10, manager.getPlatformCount());
        int countAfterTen = changeCount.get();

        boolean eleventh = PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        assertFalse(eleventh);
        assertEquals(10, manager.getPlatformCount());
        assertEquals(countAfterTen, changeCount.get(), "no notify on rejected add");
        assertFalse(manager.canAddMorePlatforms());
    }

    // --- remove ---

    @Test
    void removePlatform_existing_removesAndNotifies() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID id = manager.getPlatforms(false).get(0).getId();

        boolean removed = manager.removePlatform(id);
        assertTrue(removed);
        assertEquals(2, changeCount.get());
        assertEquals(0, manager.getPlatformCount());
        assertNull(manager.getPlatform(id));
    }

    @Test
    void removePlatform_missing_returnsFalseAndNoExtraNotify() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        int before = changeCount.get();

        boolean removed = manager.removePlatform(UUID.randomUUID());
        assertFalse(removed);
        assertEquals(before, changeCount.get());
        assertEquals(1, manager.getPlatformCount());
    }

    // --- get / path / toggle (delegation + notify) ---

    @Test
    void getPlatform_present_returnsLiveInstance() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID id = manager.getPlatforms(false).get(0).getId();

        Platform p = manager.getPlatform(id);
        assertNotNull(p);
        assertEquals(id, p.getId());
    }

    @Test
    void getPlatform_absent_returnsNull() {
        assertNull(manager.getPlatform(UUID.randomUUID()));
    }

    @Test
    void setPlatformPathStartEnd_andToggle_affectPlatformAndNotify() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID id = manager.getPlatforms(false).get(0).getId();
        int before = changeCount.get();

        assertTrue(manager.setPlatformPathStart(id, START_A));
        assertTrue(manager.setPlatformPathEnd(id, END_A));
        assertTrue(manager.togglePlatformEnabled(id)); // now disabled

        assertEquals(before + 3, changeCount.get());

        Platform p = manager.getPlatform(id);
        assertEquals(START_A, p.getStartPos());
        assertEquals(END_A, p.getEndPos());
        assertFalse(p.isEnabled());
    }

    @Test
    void setPlatformPath_onUnknownId_returnsFalseNoNotify() {
        int before = changeCount.get();
        assertFalse(manager.setPlatformPathStart(UUID.randomUUID(), START_A));
        assertFalse(manager.setPlatformPathEnd(UUID.randomUUID(), END_A));
        assertEquals(before, changeCount.get());
    }

    // --- defensive copies ---

    @Test
    void getPlatforms_returnsDefensiveCopy_serverSide_mutateCopyDoesNotAffectManager() {
        PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        PlatformManagerTestFixtures.addSyntheticPlatform(manager);

        List<Platform> copy = manager.getPlatforms(false);
        copy.clear();

        assertEquals(2, manager.getPlatformCount());
        assertEquals(2, manager.getPlatforms(false).size());
    }

    @Test
    void getPlatforms_clientSide_returnsSeparateDefensiveCopy() {
        // populate server list
        PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        // simulate client sync with same tag
        CompoundTag tag = new CompoundTag();
        manager.saveToNBT(tag);
        manager.updateClientPlatforms(tag);

        List<Platform> serverView = manager.getPlatforms(false);
        List<Platform> clientView = manager.getPlatforms(true);

        assertNotSame(serverView, clientView);
        assertEquals(1, serverView.size());
        assertEquals(1, clientView.size());
    }

    // --- enabled + complete filter ---

    @Test
    void getEnabledPlatforms_onlyReturnsEnabledAndComplete() {
        // 1: good
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID good = manager.getPlatforms(false).get(0).getId();
        manager.setPlatformPathStart(good, START_A);
        manager.setPlatformPathEnd(good, END_A);
        // remains enabled

        // 2: enabled but incomplete (no end)
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID incomplete = manager.getPlatforms(false).get(1).getId();
        manager.setPlatformPathStart(incomplete, START_B);
        // no end

        // 3: complete but disabled
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID disabled = manager.getPlatforms(false).get(2).getId();
        manager.setPlatformPathStart(disabled, START_A);
        manager.setPlatformPathEnd(disabled, END_A);
        manager.togglePlatformEnabled(disabled); // now false

        List<Platform> enabled = manager.getEnabledPlatforms();
        assertEquals(1, enabled.size());
        assertEquals(good, enabled.get(0).getId());
    }

    @Test
    void getEnabledPlatforms_emptyWhenNoneQualify() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        // no path set -> incomplete
        assertTrue(manager.getEnabledPlatforms().isEmpty());
    }

    // --- NBT: save/load ---

    @Test
    void saveToNBT_emptyList_doesNotWritePlatformsKey() {
        CompoundTag tag = new CompoundTag();
        manager.saveToNBT(tag);
        assertFalse(tag.contains("platforms"));
    }

    @Test
    void saveToNBT_nonEmpty_writesListTagWithPlatformEntries() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID id = manager.getPlatforms(false).get(0).getId();
        manager.setPlatformPathStart(id, START_A);
        manager.setPlatformPathEnd(id, END_A);

        CompoundTag tag = new CompoundTag();
        manager.saveToNBT(tag);

        assertTrue(tag.contains("platforms"));
        // structure is a ListTag under the key; size check via roundtrip below
    }

    @Test
    void loadFromNBT_roundtrip_restoresAllPlatformState() {
        // seed two platforms with paths and one dest
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        List<Platform> seeded = manager.getPlatforms(false);
        UUID id1 = seeded.get(0).getId();
        UUID id2 = seeded.get(1).getId();
        UUID dest = UUID.fromString("11111111-1111-1111-1111-111111111111");

        manager.setPlatformPathStart(id1, START_A);
        manager.setPlatformPathEnd(id1, END_A);
        manager.getPlatform(id1).enableDestination(dest);

        manager.setPlatformPathStart(id2, START_B);
        manager.setPlatformPathEnd(id2, END_B);
        manager.togglePlatformEnabled(id2);

        CompoundTag tag = new CompoundTag();
        manager.saveToNBT(tag);

        PlatformManager loaded = new PlatformManager();
        loaded.loadFromNBT(tag);

        assertEquals(2, loaded.getPlatformCount());
        Platform p1 = loaded.getPlatform(id1);
        Platform p2 = loaded.getPlatform(id2);
        assertNotNull(p1);
        assertNotNull(p2);

        assertEquals(START_A, p1.getStartPos());
        assertEquals(END_A, p1.getEndPos());
        assertTrue(p1.isDestinationEnabled(dest));
        assertTrue(p1.isEnabled());

        assertEquals(START_B, p2.getStartPos());
        assertEquals(END_B, p2.getEndPos());
        assertFalse(p2.isEnabled());
    }

    @Test
    void loadFromNBT_missingKey_leavesEmptyAfterClear() {
        // first put something in
        PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        CompoundTag emptyish = new CompoundTag();
        // no "platforms" key

        manager.loadFromNBT(emptyish);
        assertEquals(0, manager.getPlatformCount());
    }

    // --- client snapshot update ---

    @Test
    void updateClientPlatforms_replacesClientListFromTag() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID id = manager.getPlatforms(false).get(0).getId();
        manager.setPlatformPathStart(id, START_A);
        manager.setPlatformPathEnd(id, END_A);

        CompoundTag tag = new CompoundTag();
        manager.saveToNBT(tag);

        // fresh manager to simulate client receiving tag
        PlatformManager clientMgr = new PlatformManager();
        clientMgr.updateClientPlatforms(tag);

        assertEquals(0, clientMgr.getPlatformCount()); // server list empty
        List<Platform> clientView = clientMgr.getPlatforms(true);
        assertEquals(1, clientView.size());
        assertEquals(START_A, clientView.get(0).getStartPos());
    }

    @Test
    void updateClientPlatforms_missingKey_clearsClientList() {
        // seed a client list somehow
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        CompoundTag tagWith = new CompoundTag();
        manager.saveToNBT(tagWith);

        PlatformManager clientMgr = new PlatformManager();
        clientMgr.updateClientPlatforms(tagWith);
        assertEquals(1, clientMgr.getPlatforms(true).size());

        CompoundTag tagWithout = new CompoundTag(); // no platforms key
        clientMgr.updateClientPlatforms(tagWithout);
        assertEquals(0, clientMgr.getPlatforms(true).size());
    }

    // --- counts and canAdd ---

    @Test
    void counts_reflectAddsAndRemoves() {
        assertEquals(0, manager.getPlatformCount());
        assertTrue(manager.canAddMorePlatforms());

        PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        assertEquals(2, manager.getPlatformCount());

        UUID id = manager.getPlatforms(false).get(0).getId();
        manager.removePlatform(id);
        assertEquals(1, manager.getPlatformCount());
    }

    // --- clear ---

    @Test
    void clear_resetsListsModeAndCount() {
        PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        PlatformManagerTestFixtures.addSyntheticPlatform(manager);
        UUID id = manager.getPlatforms(false).get(0).getId();
        manager.setPlatformPathStart(id, START_A);

        manager.clear();

        assertEquals(0, manager.getPlatformCount());
        assertEquals(0, manager.getPlatforms(true).size());
        // clear does not go through notify path in current impl; no assertion on count here
    }

    // --- getEnabledPlatforms after mutations ---

    @Test
    void getEnabledPlatforms_updatesWhenPlatformToggledOrPathCompleted() {
        assertTrue(PlatformManagerTestFixtures.addSyntheticPlatform(manager));
        UUID id = manager.getPlatforms(false).get(0).getId();

        assertTrue(manager.getEnabledPlatforms().isEmpty()); // incomplete

        manager.setPlatformPathStart(id, START_A);
        manager.setPlatformPathEnd(id, END_A);
        List<Platform> afterPath = manager.getEnabledPlatforms();
        assertEquals(1, afterPath.size());

        manager.togglePlatformEnabled(id);
        assertTrue(manager.getEnabledPlatforms().isEmpty()); // now disabled
    }
}
