package com.quackers29.businesscraft.town.viewmodel;

import net.minecraft.network.FriendlyByteBuf;

public interface IViewModel {
    void toBytes(FriendlyByteBuf buf);
}