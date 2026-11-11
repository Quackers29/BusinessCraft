package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.entity.TouristEntity;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JadeTouristTooltipHelperTest {

    @Test
    void resolveOrigin_prefersServerNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString(JadeTooltipNbt.ORIGIN, "Riverside");
        assertEquals("Riverside", JadeTouristTooltipHelper.resolveOrigin(tag, "ClientOnly"));
    }

    @Test
    void resolveDestination_prefersServerNbtIncludingAnyTown() {
        CompoundTag tag = new CompoundTag();
        tag.putString(JadeTooltipNbt.DESTINATION, TouristEntity.ANY_TOWN_NAME);
        assertEquals(TouristEntity.ANY_TOWN_NAME,
                JadeTouristTooltipHelper.resolveDestination(tag, "Wrong"));
    }

    @Test
    void resolveDestination_fallsBackToSyncedLabel() {
        assertEquals("Hillcrest", JadeTouristTooltipHelper.resolveDestination(new CompoundTag(), "Hillcrest"));
    }
}
