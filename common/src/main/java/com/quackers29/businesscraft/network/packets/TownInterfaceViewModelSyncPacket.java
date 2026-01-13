package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel;
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
public class TownInterfaceViewModelSyncPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceViewModelSyncPacket.class);

    private final BlockPos pos;
    private final TownInterfaceViewModel viewModel;

    // Server-side constructor
    public TownInterfaceViewModelSyncPacket(BlockPos pos, TownInterfaceViewModel viewModel) {
        this.pos = pos;
        this.viewModel = viewModel;
    }

    // Client-side decoder
    public TownInterfaceViewModelSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.viewModel = new TownInterfaceViewModel(buf);
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        viewModel.toBytes(buf);
    }

    public static void encode(TownInterfaceViewModelSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static TownInterfaceViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new TownInterfaceViewModelSyncPacket(buf);
    }

    // Handler
    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null)
                return;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity entity) {
                // Update client cache
                entity.updateTownInterfaceViewModel(viewModel);

                LOGGER.debug("[CLIENT] Town Interface view-model updated: {}, Pop: {}, Happy: {}",
                        viewModel.getTownName(), viewModel.getPopulationDisplay(), viewModel.getHappinessDisplay());
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
