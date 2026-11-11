package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.platform.DestinationMode;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JadePlatformTooltipHelperTest {

    @Test
    void readMode_parsesConfigValue() {
        CompoundTag tag = new CompoundTag();
        tag.putString(JadeTooltipNbt.MODE, "specific");
        assertEquals(DestinationMode.SPECIFIC, JadePlatformTooltipHelper.readMode(tag));
    }

    @Test
    void modeLineKey_coversAllModes() {
        assertEquals("businesscraft.jade.platform.mode_any",
                JadePlatformTooltipHelper.modeLineKey(DestinationMode.ANY));
        assertEquals("businesscraft.jade.platform.mode_auto",
                JadePlatformTooltipHelper.modeLineKey(DestinationMode.AUTO));
        assertEquals("businesscraft.jade.platform.mode_specific",
                JadePlatformTooltipHelper.modeLineKey(DestinationMode.SPECIFIC));
    }

    @Test
    void learnedSummary_readsTrimmedString() {
        CompoundTag tag = new CompoundTag();
        tag.putString(JadeTooltipNbt.LEARNED_SUMMARY, "  Oakvale (50%)  ");
        assertEquals("Oakvale (50%)", JadePlatformTooltipHelper.learnedSummary(tag));
    }
}
