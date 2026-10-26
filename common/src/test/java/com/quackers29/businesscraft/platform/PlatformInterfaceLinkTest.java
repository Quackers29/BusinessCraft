package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITouristHelper;
import com.quackers29.businesscraft.platform.PlatformSpawn;
import com.quackers29.businesscraft.town.data.PlatformManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Platform Interface placement, town linking, the 10-platform cap, and save/load.
 * Platforms without an interface block are not kept.
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
        assertEquals(PlatformSpawn.DEFAULT_RADIUS, platform.getSearchRadius());
        assertEquals(PlatformSpawn.DEFAULT_SPREAD, platform.getSpread());
        assertEquals(pos, platform.getInterfacePos());
        assertEquals(List.of(pos), manager.getInterfacePositions());

        assertTrue(manager.removePlatform(id));
        assertEquals(0, manager.getPlatformCount());
        assertNull(manager.addInterfacePlatform(null));
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
    void platformWithoutInterfaceBlock_isDroppedOnLoad() {
        CompoundTag platform = new CompoundTag();
        platform.putUUID("Id", UUID.randomUUID());
        platform.putString("Name", "Old Platform");
        platform.putBoolean("Enabled", true);
        platform.putInt("StartX", 2);
        platform.putInt("StartY", 64);
        platform.putInt("StartZ", 3);
        platform.putInt("EndX", 8);
        platform.putInt("EndY", 64);
        platform.putInt("EndZ", 3);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        list.add(platform);
        CompoundTag saved = new CompoundTag();
        saved.put("platforms", list);

        PlatformManager manager = new PlatformManager();
        manager.loadFromNBT(saved);
        assertEquals(0, manager.getPlatformCount());
    }

    @Test
    void persistence_roundTripsInterfacePlatformIncludingRadiusAndSpread() {
        PlatformManager manager = new PlatformManager();
        BlockPos interfacePos = new BlockPos(20, 65, -4);
        UUID interfaceId = manager.addInterfacePlatform(interfacePos);
        Platform bound = manager.getPlatform(interfaceId);
        bound.setEnabled(false);
        bound.setStartPos(new BlockPos(20, 65, -2));
        bound.setEndPos(new BlockPos(20, 65, 4));
        bound.enableDestination(DEST);
        bound.setSearchRadius(8);
        bound.setSpread(20);

        CompoundTag saved = new CompoundTag();
        manager.saveToNBT(saved);

        PlatformManager loaded = new PlatformManager();
        loaded.loadFromNBT(saved);

        assertEquals(1, loaded.getPlatformCount());
        Platform loadedBound = loaded.getPlatform(interfaceId);
        assertEquals(interfacePos, loadedBound.getInterfacePos());
        assertFalse(loadedBound.isEnabled());
        assertEquals(new BlockPos(20, 65, -2), loadedBound.getStartPos());
        assertEquals(new BlockPos(20, 65, 4), loadedBound.getEndPos());
        assertTrue(loadedBound.isDestinationEnabled(DEST));
        assertEquals(8, loadedBound.getSearchRadius());
        assertEquals(8, loadedBound.getSpread());
        assertEquals(List.of(interfacePos), loaded.getInterfacePositions());
    }

    @Test
    void relink_linksOwner_refusesSteal_recreatesMissing_andHonoursCap() {
        PlatformManager manager = new PlatformManager();
        BlockPos pos = new BlockPos(4, 64, 0);
        UUID id = manager.addInterfacePlatform(pos);

        assertEquals(PlatformManager.RelinkResult.LINKED, manager.relink(id, pos));

        UUID other = UUID.randomUUID();
        assertEquals(PlatformManager.RelinkResult.RELINKED,
                manager.relink(other, new BlockPos(40, 64, 0)));
        assertEquals(new BlockPos(40, 64, 0), manager.getPlatform(other).getInterfacePos());

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
