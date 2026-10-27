package com.quackers29.businesscraft.event;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlatformPathSessionsTest {

    @AfterEach
    void tearDown() {
        PlatformPathSessions.clearAll();
    }

    @Test
    void twoPlayersHaveIndependentSessions() {
        UUID playerA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID playerB = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID platformA = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID platformB = UUID.fromString("20000000-0000-0000-0000-000000000002");
        BlockPos townA = new BlockPos(1, 2, 3);
        BlockPos townB = new BlockPos(9, 8, 7);

        PlatformPathSessions.begin(playerA, townA, platformA);
        PlatformPathSessions.begin(playerB, townB, platformB);

        PlatformPathSessions.Session sessionA = PlatformPathSessions.get(playerA);
        PlatformPathSessions.Session sessionB = PlatformPathSessions.get(playerB);

        assertNotNull(sessionA);
        assertNotNull(sessionB);
        assertEquals(townA, sessionA.townBlockPos);
        assertEquals(townB, sessionB.townBlockPos);
        assertEquals(platformA, sessionA.platformId);
        assertEquals(platformB, sessionB.platformId);

        sessionA.awaitingSecondClick = true;
        assertFalse(sessionB.awaitingSecondClick);

        PlatformPathSessions.clear(playerA);
        assertNull(PlatformPathSessions.get(playerA));
        assertNotNull(PlatformPathSessions.get(playerB));
    }
}
