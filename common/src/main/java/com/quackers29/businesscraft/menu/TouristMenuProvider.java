package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.entity.TouristEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Opens {@link TouristMenu} and sends the journey snapshot with it.
 */
public final class TouristMenuProvider implements MenuProvider, OpeningDataWriter {
    private final TouristEntity tourist;

    public TouristMenuProvider(TouristEntity tourist) {
        this.tourist = tourist;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Tourist");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TouristMenu(containerId, inventory, tourist);
    }

    @Override
    public void writeOpeningData(FriendlyByteBuf buf) {
        TouristScreenData.capture(tourist).write(buf);
    }
}
