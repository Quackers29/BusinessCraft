package com.quackers29.businesscraft.menu;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Menu provider that writes its own opening payload.
 * Forge and Fabric both deliver that payload to the client menu factory.
 */
public interface OpeningDataWriter {
    void writeOpeningData(FriendlyByteBuf buf);
}
