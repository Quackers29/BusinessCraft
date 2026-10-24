package com.quackers29.businesscraft.platform;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlatformLinkTest {
    private static final UUID TOWN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TOWN = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PLATFORM_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLATFORM_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private final BlockPos place = new BlockPos(0, 64, 0);

    @Test
    void noStops_rejects() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of());
        assertEquals(PlatformLink.Status.NO_STOP, choice.status());
        assertNull(choice.platformId());
    }

    @Test
    void stopInsideRadius_linksThatPlatform() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(4, 63, 0))));
        assertEquals(PlatformLink.Status.LINKED, choice.status());
        assertEquals(PLATFORM_A, choice.platformId());
        assertEquals(TOWN, choice.townId());
    }

    @Test
    void exactlyFiveBlocksHorizontal_isInside() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(5, 64, 0))));
        assertEquals(PlatformLink.Status.LINKED, choice.status());
    }

    @Test
    void sixBlocksHorizontal_isOutside() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(6, 64, 0))));
        assertEquals(PlatformLink.Status.NO_STOP, choice.status());
    }

    @Test
    void verticalWindow_includesThreeAndRejectsFour() {
        assertEquals(PlatformLink.Status.LINKED, PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(0, 61, 0)))).status());
        assertEquals(PlatformLink.Status.NO_STOP, PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(0, 60, 0)))).status());
    }

    @Test
    void nearerStopWins() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(4, 64, 0)),
                stop(OTHER_TOWN, PLATFORM_B, new BlockPos(1, 64, 0))));
        assertEquals(PlatformLink.Status.LINKED, choice.status());
        assertEquals(PLATFORM_B, choice.platformId());
        assertEquals(OTHER_TOWN, choice.townId());
    }

    @Test
    void samePlatformStartAndEnd_areNotAmbiguous() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(0, 63, 0)),
                stop(TOWN, PLATFORM_A, new BlockPos(2, 63, 0))));
        assertEquals(PlatformLink.Status.LINKED, choice.status());
        assertEquals(PLATFORM_A, choice.platformId());
    }

    @Test
    void twoPlatformsWithinOneBlock_areAmbiguous() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(2, 64, 0)),
                stop(TOWN, PLATFORM_B, new BlockPos(3, 64, 0))));
        assertEquals(PlatformLink.Status.AMBIGUOUS, choice.status());
        assertNull(choice.platformId());
    }

    @Test
    void platformsMoreThanOneBlockApart_pickTheNearer() {
        PlatformLink.Choice choice = PlatformLink.choose(place, List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(1, 64, 0)),
                stop(TOWN, PLATFORM_B, new BlockPos(4, 64, 0))));
        assertEquals(PlatformLink.Status.LINKED, choice.status());
        assertEquals(PLATFORM_A, choice.platformId());
    }

    @Test
    void outsideTownBoundary_stillLinksWhenTheStopIsClose() {
        PlatformLink.Choice choice = PlatformLink.choose(new BlockPos(1000, 64, 1000), List.of(
                stop(TOWN, PLATFORM_A, new BlockPos(1002, 64, 1000))));
        assertEquals(PlatformLink.Status.LINKED, choice.status());
        assertEquals(PLATFORM_A, choice.platformId());
    }

    private static PlatformLink.Stop stop(UUID town, UUID platform, BlockPos pos) {
        return new PlatformLink.Stop(town, platform, pos);
    }
}
