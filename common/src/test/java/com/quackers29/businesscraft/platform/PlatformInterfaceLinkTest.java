package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITouristHelper;
import com.quackers29.businesscraft.town.data.PlatformManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Platform Interface placement, town linking, the 10-platform cap, legacy migration,
 * and save/load of mixed legacy and interface-backed platforms.
 */
class PlatformInterfaceLinkTest {

    private static final UUID TOWN_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TOWN_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID DEST = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final BlockPos CENTRE = new BlockPos(0, 64, 0);

    private static PlatformPlacement.TownCandidate town(UUID id, BlockPos centre, int radius, int count,
            List<BlockPos> interfaces) {
        return new PlatformPlacement.TownCandidate(id, centre, radius, count, interfaces);
    }

    @Test
    void outsideBoundary_isRejected() {
        PlatformPlacement.Decision decision = PlatformPlacement.evaluate(
                new BlockPos(51, 64, 0),
                List.of(town(TOWN_A, CENTRE, 50, 0, List.of())));
        assertFalse(decision.accepted());
        assertEquals(PlatformPlacement.Reject.OUTSIDE, decision.reject());
        assertNull(decision.townId());
    }

    @Test
    void insideBoundary_andExactEdge_areAccepted() {
        List<PlatformPlacement.TownCandidate> towns = List.of(town(TOWN_A, CENTRE, 50, 0, List.of()));

        PlatformPlacement.Decision inside = PlatformPlacement.evaluate(new BlockPos(10, 64, 0), towns);
        assertTrue(inside.accepted());
        assertEquals(TOWN_A, inside.townId());
        assertEquals(CENTRE, inside.townCentre());

        PlatformPlacement.Decision edge = PlatformPlacement.evaluate(new BlockPos(50, 64, 0), towns);
        assertTrue(edge.accepted());
        assertEquals(TOWN_A, edge.townId());
    }

    @Test
    void overlappingTowns_linkToTheNearestCentre() {
        PlatformPlacement.Decision decision = PlatformPlacement.evaluate(
                new BlockPos(10, 64, 0),
                List.of(
                        town(TOWN_A, new BlockPos(0, 64, 0), 80, 0, List.of()),
                        town(TOWN_B, new BlockPos(12, 64, 0), 80, 0, List.of())));
        assertTrue(decision.accepted());
        assertEquals(TOWN_B, decision.townId());
    }

    @Test
    void atCap_isRejectedWithTheTownIdentified() {
        PlatformPlacement.Decision decision = PlatformPlacement.evaluate(
                new BlockPos(8, 64, 0),
                List.of(town(TOWN_A, CENTRE, 50, PlatformManager.MAX_PLATFORMS, List.of())));
        assertFalse(decision.accepted());
        assertEquals(PlatformPlacement.Reject.AT_CAP, decision.reject());
        assertEquals(TOWN_A, decision.townId());
    }

    @Test
    void separation_rejectsCloserThanFive_andAllowsExactlyFive() {
        BlockPos existing = new BlockPos(10, 64, 0);
        List<PlatformPlacement.TownCandidate> close = List.of(
                town(TOWN_A, CENTRE, 50, 1, List.of(existing)));

        PlatformPlacement.Decision tooClose = PlatformPlacement.evaluate(new BlockPos(14, 64, 0), close);
        assertEquals(PlatformPlacement.Reject.TOO_CLOSE, tooClose.reject());

        PlatformPlacement.Decision exact = PlatformPlacement.evaluate(new BlockPos(15, 64, 0), close);
        assertTrue(exact.accepted());
    }

    @Test
    void duplicateRecordedPosition_isRejectedBeforeDistance() {
        BlockPos existing = new BlockPos(10, 64, 0);
        PlatformPlacement.Decision decision = PlatformPlacement.evaluate(
                existing,
                List.of(town(TOWN_A, CENTRE, 50, 1, List.of(existing))));
        assertEquals(PlatformPlacement.Reject.DUPLICATE, decision.reject());
    }

    @Test
    void addInterfacePlatform_addsAndRemoveDropsIt() {
        PlatformManager manager = new PlatformManager();
        BlockPos pos = new BlockPos(4, 64, 0);
        UUID id = manager.addInterfacePlatform(pos);

        assertNotNull(id);
        assertEquals(1, manager.getPlatformCount());
        Platform platform = manager.getPlatform(id);
        assertFalse(platform.isLegacy());
        assertEquals(pos, platform.getInterfacePos());
        assertEquals(List.of(pos), manager.getInterfacePositions());

        assertFalse(manager.removeLegacyPlatform(id));
        assertEquals(1, manager.getPlatformCount());

        assertTrue(manager.removePlatform(id));
        assertEquals(0, manager.getPlatformCount());
        assertNull(manager.addInterfacePlatform(null));
    }

    @Test
    void addPlatform_andCreateLegacy_areLegacyAndRemovableFromTheTownList() {
        PlatformManager manager = new PlatformManager();
        assertTrue(manager.addPlatform());
        Platform added = manager.getPlatforms(false).get(0);
        assertTrue(added.isLegacy());
        assertNull(added.getInterfacePos());
        assertTrue(manager.removeLegacyPlatform(added.getId()));

        manager.createLegacyPlatform(new BlockPos(1, 64, 1), new BlockPos(6, 64, 1));
        Platform migrated = manager.getPlatforms(false).get(0);
        assertTrue(migrated.isLegacy());
        assertTrue(migrated.isComplete());
        assertTrue(manager.removeLegacyPlatform(migrated.getId()));
        assertEquals(0, manager.getPlatformCount());
    }

    @Test
    void addInterfacePlatform_respectsCapSeparationAndDuplicate() {
        PlatformManager manager = new PlatformManager();
        BlockPos first = new BlockPos(0, 64, 0);
        assertNotNull(manager.addInterfacePlatform(first));
        assertNull(manager.addInterfacePlatform(first));
        assertNull(manager.addInterfacePlatform(new BlockPos(4, 64, 0)));
        assertNotNull(manager.addInterfacePlatform(new BlockPos(5, 64, 0)));

        while (manager.getPlatformCount() < PlatformManager.MAX_PLATFORMS) {
            int i = manager.getPlatformCount();
            assertNotNull(manager.addInterfacePlatform(new BlockPos(i * 5, 70, 0)));
        }
        assertNull(manager.addInterfacePlatform(new BlockPos(200, 64, 0)));
        assertEquals(PlatformManager.MAX_PLATFORMS, manager.getPlatformCount());
    }

    @Test
    void oldSaveWithoutLegacyKey_loadsAsLegacy() {
        CompoundTag tag = new CompoundTag();
        UUID id = UUID.randomUUID();
        tag.putUUID("Id", id);
        tag.putString("Name", "Old Platform");
        tag.putBoolean("Enabled", true);
        tag.putInt("StartX", 2);
        tag.putInt("StartY", 64);
        tag.putInt("StartZ", 3);
        tag.putInt("EndX", 8);
        tag.putInt("EndY", 64);
        tag.putInt("EndZ", 3);

        Platform loaded = Platform.fromNBT(tag);
        assertTrue(loaded.isLegacy());
        assertNull(loaded.getInterfacePos());
        assertEquals(new BlockPos(2, 64, 3), loaded.getStartPos());
        assertEquals(new BlockPos(8, 64, 3), loaded.getEndPos());
        assertTrue(loaded.isComplete());
    }

    @Test
    void persistence_roundTripsMixedLegacyAndInterfacePlatforms() {
        PlatformManager manager = new PlatformManager();
        assertTrue(manager.addPlatform());
        Platform legacy = manager.getPlatforms(false).get(0);
        legacy.setEnabled(false);
        legacy.setStartPos(new BlockPos(1, 64, 2));
        legacy.setEndPos(new BlockPos(9, 64, 2));
        legacy.enableDestination(DEST);

        BlockPos interfacePos = new BlockPos(20, 65, -4);
        UUID interfaceId = manager.addInterfacePlatform(interfacePos);
        Platform bound = manager.getPlatform(interfaceId);
        bound.setStartPos(new BlockPos(20, 65, -2));
        bound.setEndPos(new BlockPos(20, 65, 4));
        bound.enableDestination(DEST);

        CompoundTag saved = new CompoundTag();
        manager.saveToNBT(saved);

        PlatformManager loaded = new PlatformManager();
        loaded.loadFromNBT(saved);

        assertEquals(2, loaded.getPlatformCount());
        Platform loadedLegacy = loaded.getPlatform(legacy.getId());
        assertTrue(loadedLegacy.isLegacy());
        assertNull(loadedLegacy.getInterfacePos());
        assertFalse(loadedLegacy.isEnabled());
        assertEquals(new BlockPos(1, 64, 2), loadedLegacy.getStartPos());
        assertEquals(new BlockPos(9, 64, 2), loadedLegacy.getEndPos());
        assertTrue(loadedLegacy.isDestinationEnabled(DEST));

        Platform loadedBound = loaded.getPlatform(interfaceId);
        assertFalse(loadedBound.isLegacy());
        assertEquals(interfacePos, loadedBound.getInterfacePos());
        assertEquals(new BlockPos(20, 65, -2), loadedBound.getStartPos());
        assertEquals(new BlockPos(20, 65, 4), loadedBound.getEndPos());
        assertTrue(loadedBound.isDestinationEnabled(DEST));
        assertEquals(List.of(interfacePos), loaded.getInterfacePositions());
    }

    @Test
    void relink_linksOwner_refusesSteal_recreatesMissing_andHonoursCap() {
        PlatformManager manager = new PlatformManager();
        BlockPos pos = new BlockPos(4, 64, 0);
        UUID id = manager.addInterfacePlatform(pos);

        assertEquals(PlatformManager.RelinkResult.LINKED, manager.relink(id, pos));

        assertTrue(manager.addPlatform());
        UUID legacyId = manager.getPlatforms(false).stream()
                .filter(Platform::isLegacy)
                .map(Platform::getId)
                .findFirst()
                .orElseThrow();
        assertEquals(PlatformManager.RelinkResult.NOT_OURS,
                manager.relink(legacyId, new BlockPos(30, 64, 0)));
        assertTrue(manager.getPlatform(legacyId).isLegacy());

        UUID other = UUID.randomUUID();
        assertEquals(PlatformManager.RelinkResult.RELINKED,
                manager.relink(other, new BlockPos(40, 64, 0)));
        assertEquals(new BlockPos(40, 64, 0), manager.getPlatform(other).getInterfacePos());
        assertFalse(manager.getPlatform(other).isLegacy());

        assertEquals(PlatformManager.RelinkResult.DUPLICATE, manager.relink(id, new BlockPos(9, 64, 0)));
        assertEquals(pos, manager.getPlatform(id).getInterfacePos());

        assertEquals(PlatformManager.RelinkResult.DUPLICATE, manager.relink(UUID.randomUUID(), pos));

        while (manager.getPlatformCount() < PlatformManager.MAX_PLATFORMS) {
            assertNotNull(manager.addInterfacePlatform(new BlockPos(manager.getPlatformCount() * 6, 80, 0)));
        }
        assertEquals(PlatformManager.RelinkResult.AT_CAP,
                manager.relink(UUID.randomUUID(), new BlockPos(500, 64, 0)));
    }

    @Test
    void touristTags_matchPlatformAndOrigin() {
        UUID platformId = UUID.randomUUID();
        UUID townId = UUID.randomUUID();
        Set<String> tags = Set.of(
                ITouristHelper.TAG_PLATFORM_PREFIX + platformId,
                ITouristHelper.TAG_FROM_TOWN_PREFIX + townId);

        assertTrue(PlatformTouristCleanup.taggedForPlatform(tags, platformId));
        assertFalse(PlatformTouristCleanup.taggedForPlatform(tags, UUID.randomUUID()));
        assertFalse(PlatformTouristCleanup.taggedForPlatform(null, platformId));
        assertEquals(townId, PlatformTouristCleanup.originTownId(tags));
        assertNull(PlatformTouristCleanup.originTownId(Set.of("from_town_not-a-uuid")));
        assertEquals(0, PlatformTouristCleanup.dismissLoaded(null, platformId));
    }
}
