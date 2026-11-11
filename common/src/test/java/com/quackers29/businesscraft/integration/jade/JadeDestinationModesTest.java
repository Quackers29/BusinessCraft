package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.platform.DestinationMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JadeDestinationModesTest {

    @Test
    void configValue_matchesPlatformConfigEncoding() {
        assertEquals("any", JadeDestinationModes.configValue(DestinationMode.ANY));
        assertEquals("auto", JadeDestinationModes.configValue(DestinationMode.AUTO));
        assertEquals("specific", JadeDestinationModes.configValue(DestinationMode.SPECIFIC));
    }
}
