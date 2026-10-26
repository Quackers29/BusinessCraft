package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITouristHelper;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformAreaPacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.town.data.PlatformManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
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

    @Test
    void persistence_roundTripsEveryPlatformField() {
        UUID destOn = DEST;
        UUID destOff = UUID.fromString("22222222-2222-2222-2222-222222222222");
        PlatformManager manager = new PlatformManager();
        BlockPos interfacePos = new BlockPos(12, 70, -8);
        UUID id = manager.addInterfacePlatform(interfacePos);
        Platform platform = manager.getPlatform(id);
        platform.setName("North Gate");
        platform.setEnabled(false);
        platform.setStartPos(new BlockPos(12, 70, -6));
        platform.setEndPos(new BlockPos(18, 71, 2));
        platform.enableDestination(destOn);
        platform.enableDestination(destOff);
        platform.disableDestination(destOff);
        platform.setSearchRadius(25);
        platform.setSpread(7);

        CompoundTag saved = new CompoundTag();
        manager.saveToNBT(saved);
        CompoundTag raw = saved.getList("platforms", Tag.TAG_COMPOUND).getCompound(0);
        assertEquals(id, raw.getUUID("Id"));
        assertEquals("North Gate", raw.getString("Name"));
        assertFalse(raw.getBoolean("Enabled"));
        assertEquals(25, raw.getInt("SearchRadius"));
        assertEquals(7, raw.getInt("Spread"));
        assertEquals(12, raw.getInt("InterfaceX"));
        assertEquals(70, raw.getInt("InterfaceY"));
        assertEquals(-8, raw.getInt("InterfaceZ"));
        assertEquals(1, raw.getCompound("Destinations").getInt("Count"));

        PlatformManager loaded = new PlatformManager();
        loaded.loadFromNBT(saved);
        Platform again = loaded.getPlatform(id);
        assertEquals(id, again.getId());
        assertEquals("North Gate", again.getName());
        assertFalse(again.isEnabled());
        assertEquals(new BlockPos(12, 70, -6), again.getStartPos());
        assertEquals(new BlockPos(18, 71, 2), again.getEndPos());
        assertEquals(interfacePos, again.getInterfacePos());
        assertTrue(again.isDestinationEnabled(destOn));
        assertFalse(again.isDestinationEnabled(destOff));
        assertEquals(Set.of(destOn), again.getEnabledDestinations());
        assertEquals(25, again.getSearchRadius());
        assertEquals(7, again.getSpread());
    }

    @Test
    void loadAndSetters_clampRadiusAndSpreadToTheConfiguredMax() {
        int savedMax = ConfigLoader.maxPlatformSearchRadius;
        try {
            ConfigLoader.maxPlatformSearchRadius = 40;
            CompoundTag tag = new CompoundTag();
            UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
            tag.putUUID("Id", id);
            tag.putString("Name", "Wide");
            tag.putBoolean("Enabled", true);
            tag.putInt("StartX", 4);
            tag.putInt("StartY", 64);
            tag.putInt("StartZ", 1);
            tag.putInt("EndX", 9);
            tag.putInt("EndY", 64);
            tag.putInt("EndZ", 1);
            tag.putInt("InterfaceX", 4);
            tag.putInt("InterfaceY", 64);
            tag.putInt("InterfaceZ", 0);
            tag.putInt("SearchRadius", 90);
            tag.putInt("Spread", 55);
            CompoundTag destinations = new CompoundTag();
            destinations.putInt("Count", 1);
            destinations.putUUID("Dest0", DEST);
            tag.put("Destinations", destinations);

            Platform loaded = Platform.fromNBT(tag);
            assertEquals(40, loaded.getSearchRadius());
            assertEquals(40, loaded.getSpread());
            assertEquals(new BlockPos(4, 64, 1), loaded.getStartPos());
            assertEquals(new BlockPos(9, 64, 1), loaded.getEndPos());
            assertEquals(new BlockPos(4, 64, 0), loaded.getInterfacePos());
            assertTrue(loaded.isDestinationEnabled(DEST));

            loaded.setSearchRadius(200);
            loaded.setSpread(80);
            assertEquals(40, loaded.getSearchRadius());
            assertEquals(40, loaded.getSpread());
            CompoundTag written = loaded.save();
            assertEquals(40, written.getInt("SearchRadius"));
            assertEquals(40, written.getInt("Spread"));

            ConfigLoader.maxPlatformSearchRadius = 100;
            Platform remembered = new Platform(id);
            remembered.setSearchRadius(80);
            remembered.setSpread(30);
            ConfigLoader.maxPlatformSearchRadius = 20;
            assertEquals(20, remembered.getSearchRadius());
            assertEquals(20, remembered.getSpread());
            assertEquals(20, remembered.save().getInt("SearchRadius"));
            assertEquals(20, remembered.save().getInt("Spread"));
        } finally {
            ConfigLoader.maxPlatformSearchRadius = savedMax;
        }
    }

    @Test
    void syncPacket_roundTripsEveryField_andClientRestoreClamps() {
        int savedMax = ConfigLoader.maxPlatformSearchRadius;
        try {
            UUID platformId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
            UUID destOff = UUID.fromString("22222222-2222-2222-2222-222222222222");
            BlockPos start = new BlockPos(3, 64, 5);
            BlockPos end = new BlockPos(9, 64, 5);
            BlockPos interfacePos = new BlockPos(3, 64, 1);

            TownPlatformDataResponsePacket packet = new TownPlatformDataResponsePacket(TOWN_A);
            packet.setTownInfo("Hillcrest", 12, 4, 50);
            packet.addPlatform(platformId, "North Gate", false, start, end, Set.of(DEST), 25, 7, interfacePos);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.encode(buf);
            TownPlatformDataResponsePacket decoded = TownPlatformDataResponsePacket.decode(buf);
            assertEquals(TOWN_A, decoded.getTownId());
            assertEquals("Hillcrest", decoded.getTownInfo().name);
            assertEquals(12, decoded.getTownInfo().population);
            assertEquals(4, decoded.getTownInfo().touristCount);
            assertEquals(50, decoded.getTownInfo().boundaryRadius);
            TownPlatformDataResponsePacket.PlatformInfo info = decoded.getPlatforms().get(platformId);
            assertEquals("North Gate", info.name);
            assertFalse(info.enabled);
            assertEquals(start, info.startPos);
            assertEquals(end, info.endPos);
            assertEquals(Set.of(DEST), info.enabledDestinations);
            assertFalse(info.enabledDestinations.contains(destOff));
            assertEquals(25, info.searchRadius);
            assertEquals(7, info.spread);
            assertEquals(interfacePos, info.interfacePos);

            BlockPos centre = new BlockPos(8, 65, 2);
            SetPlatformAreaPacket area = new SetPlatformAreaPacket(centre, platformId, 90, 55);
            FriendlyByteBuf areaBuf = new FriendlyByteBuf(Unpooled.buffer());
            area.encode(areaBuf);
            assertEquals(centre, areaBuf.readBlockPos());
            assertEquals(platformId, areaBuf.readUUID());
            assertEquals(90, areaBuf.readInt());
            assertEquals(55, areaBuf.readInt());
            assertFalse(areaBuf.isReadable());

            ConfigLoader.maxPlatformSearchRadius = 15;
            TownPlatformDataResponsePacket oversized = new TownPlatformDataResponsePacket(TOWN_A);
            oversized.addPlatform(platformId, "North Gate", true, start, end, Set.of(DEST), 90, 55, interfacePos);
            FriendlyByteBuf raw = new FriendlyByteBuf(Unpooled.buffer());
            oversized.encode(raw);
            TownPlatformDataResponsePacket.PlatformInfo wide = TownPlatformDataResponsePacket.decode(raw)
                    .getPlatforms().get(platformId);
            Platform restored = new Platform(wide.id);
            restored.setName(wide.name);
            restored.setEnabled(wide.enabled);
            restored.setStartPos(wide.startPos);
            restored.setEndPos(wide.endPos);
            restored.setSearchRadius(wide.searchRadius);
            restored.setSpread(wide.spread);
            restored.setInterfacePos(wide.interfacePos);
            for (UUID destination : wide.enabledDestinations) {
                restored.enableDestination(destination);
            }
            assertEquals(15, restored.getSearchRadius());
            assertEquals(15, restored.getSpread());
            Platform fromDisk = Platform.fromNBT(restored.toNBT());
            assertEquals(platformId, fromDisk.getId());
            assertEquals("North Gate", fromDisk.getName());
            assertTrue(fromDisk.isEnabled());
            assertEquals(start, fromDisk.getStartPos());
            assertEquals(end, fromDisk.getEndPos());
            assertEquals(interfacePos, fromDisk.getInterfacePos());
            assertEquals(Set.of(DEST), fromDisk.getEnabledDestinations());
            assertEquals(15, fromDisk.getSearchRadius());
            assertEquals(15, fromDisk.getSpread());
        } finally {
            ConfigLoader.maxPlatformSearchRadius = savedMax;
        }
    }

    @Test
    void interfaceLink_roundTripsTownPlatformCentreAndState() {
        UUID platformId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        BlockPos centre = new BlockPos(4, 64, 9);
        CompoundTag tag = new CompoundTag();
        PlatformInterfaceEntity.writeLink(tag, TOWN_A, platformId, centre, false,
                "businesscraft.platform_interface.too_close");

        PlatformInterfaceEntity.LinkRecord link = PlatformInterfaceEntity.readLinkRecord(tag);
        assertEquals(TOWN_A, link.townId);
        assertEquals(platformId, link.platformId);
        assertEquals(centre, link.townCentre);
        assertFalse(link.active);
        assertEquals("businesscraft.platform_interface.too_close", link.inactiveReason);

        CompoundTag active = new CompoundTag();
        PlatformInterfaceEntity.writeLink(active, TOWN_B, platformId, centre, true, null);
        PlatformInterfaceEntity.LinkRecord linked = PlatformInterfaceEntity.readLinkRecord(active);
        assertTrue(linked.active);
        assertNull(linked.inactiveReason);
        assertEquals(TOWN_B, linked.townId);

        CompoundTag bare = new CompoundTag();
        bare.putBoolean("Active", false);
        PlatformInterfaceEntity.LinkRecord missing = PlatformInterfaceEntity.readLinkRecord(bare);
        assertFalse(missing.active);
        assertEquals(PlatformInterfaceEntity.REASON_NOT_LINKED, missing.inactiveReason);
        assertNull(missing.townId);
        assertNull(missing.platformId);
        assertNull(missing.townCentre);
    }
}
