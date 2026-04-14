package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-authoritative sync packet for the main Town Interface.
 * 
 * Transfers the TownInterfaceViewModel to the client.
 */
public class TownInterfaceViewModelSyncPacket extends BaseViewModelSyncPacket<TownInterfaceViewModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceViewModelSyncPacket.class);

    public TownInterfaceViewModelSyncPacket(BlockPos pos, TownInterfaceViewModel viewModel) {
        super(pos, viewModel);
    }

    public TownInterfaceViewModelSyncPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    protected TownInterfaceViewModel readViewModel(FriendlyByteBuf buf) {
        return new TownInterfaceViewModel(buf);
    }

    public static void encode(TownInterfaceViewModelSyncPacket msg, FriendlyByteBuf buf) {
        BaseViewModelSyncPacket.encode(msg, buf);
    }

    public static TownInterfaceViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new TownInterfaceViewModelSyncPacket(buf);
    }

    @Override
    public void handle(Object context) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "[CLIENT] Town Interface view-model updated: {}, Pop: {}, Happy: {}",
                ((TownInterfaceViewModel) viewModel).getTownName(),
                ((TownInterfaceViewModel) viewModel).getPopulationDisplay(),
                ((TownInterfaceViewModel) viewModel).getHappinessDisplay());
        super.handle(context);
    }
}
