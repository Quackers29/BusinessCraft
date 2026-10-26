package com.quackers29.businesscraft.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformLampTest {

    @Test
    void lampFollowsEnabledAndTheInterfaceLink() {
        assertTrue(PlatformLamp.isLit(true, true));
        assertFalse(PlatformLamp.isLit(true, false));
        assertFalse(PlatformLamp.isLit(false, true));
        assertFalse(PlatformLamp.isLit(false, false));
        assertEquals(PlatformLamp.LIT_LEVEL, PlatformLamp.emission(true));
        assertEquals(0, PlatformLamp.emission(false));
    }
}
