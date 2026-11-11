package com.quackers29.businesscraft.client.showcase;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiplayerSmokePlanTest {

    @Test
    void ports_areSplitByLoader() {
        assertEquals(25565, MultiplayerSmokePlan.FORGE_PORT);
        assertEquals(25566, MultiplayerSmokePlan.FABRIC_PORT);
        assertEquals(MultiplayerSmokePlan.FORGE_PORT, MultiplayerSmokePlan.portFor("forge"));
        assertEquals(MultiplayerSmokePlan.FABRIC_PORT, MultiplayerSmokePlan.portFor("Fabric"));
        assertThrows(IllegalArgumentException.class, () -> MultiplayerSmokePlan.portFor("quilt"));
    }

    @Test
    void shots_areTheThreeClientViews() {
        assertEquals(List.of("mp_nametags", "mp_track", "mp_platform"), MultiplayerSmokePlan.SHOTS);
        assertEquals("showcase_mp_nametags.png", MultiplayerSmokePlan.fileName("mp_nametags"));
    }

    @Test
    void multiplayerSetup_pinsTheTownLine() {
        System.setProperty("businesscraft.multiplayerSmoke", "setup");
        try {
            ShowcaseTownNames.reset();
            assertTrue(ShowcaseTownNames.isActive());
            assertEquals(ShowcaseTownNames.MULTIPLAYER_LINE, List.of(
                    ShowcaseTownNames.next(), ShowcaseTownNames.next(), ShowcaseTownNames.next()));
        } finally {
            ShowcaseTownNames.reset();
            System.clearProperty("businesscraft.multiplayerSmoke");
        }
    }
}
