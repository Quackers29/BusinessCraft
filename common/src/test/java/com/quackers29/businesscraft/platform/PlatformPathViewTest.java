package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.network.packets.ui.PlatformVisualizationPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformPathViewTest {
    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void select_nullIdKeepsEveryPlatform() {
        Platform first = new Platform(FIRST);
        Platform second = new Platform(SECOND);
        List<Platform> selected = PlatformPathView.select(List.of(first, second), null);
        assertEquals(List.of(first, second), selected);
        assertTrue(PlatformPathView.select(null, FIRST).isEmpty());
        assertTrue(PlatformPathView.select(List.of(), FIRST).isEmpty());
    }

    @Test
    void select_idKeepsOnlyThatPlatform() {
        Platform first = new Platform(FIRST);
        first.setName("North");
        Platform second = new Platform(SECOND);
        List<Platform> selected = PlatformPathView.select(List.of(first, second), FIRST);
        assertEquals(1, selected.size());
        assertEquals(FIRST, selected.get(0).getId());
        assertTrue(PlatformPathView.select(List.of(first), SECOND).isEmpty());
    }

    @Test
    void visualizationPacket_roundTripsTownWideAndSinglePlatform() {
        BlockPos town = new BlockPos(4, 64, 8);
        FriendlyByteBuf all = new FriendlyByteBuf(Unpooled.buffer());
        PlatformVisualizationPacket townPacket = new PlatformVisualizationPacket(town);
        PlatformVisualizationPacket.encode(townPacket, all);
        PlatformVisualizationPacket decodedAll = PlatformVisualizationPacket.decode(all);
        assertEquals(town, decodedAll.getPos());
        assertNull(decodedAll.getPlatformId());

        BlockPos machine = new BlockPos(8, 64, 8);
        FriendlyByteBuf one = new FriendlyByteBuf(Unpooled.buffer());
        PlatformVisualizationPacket.encode(new PlatformVisualizationPacket(machine, FIRST), one);
        PlatformVisualizationPacket decodedOne = PlatformVisualizationPacket.decode(one);
        assertEquals(machine, decodedOne.getPos());
        assertEquals(FIRST, decodedOne.getPlatformId());
        assertEquals(0, one.readableBytes());
    }
}
