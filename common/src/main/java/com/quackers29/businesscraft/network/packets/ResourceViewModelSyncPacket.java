package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.TownResourceViewModel;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NEW server-authoritative resource sync packet implementing the "View-Model" pattern.
 * 
 * This packet REPLACES the old ResourceSyncPacket which violated the server-authoritative
 * principle by sending raw data that required client-side calculations.
 * 
 * KEY DIFFERENCES FROM OLD PACKET:
 * - Sends pre-calculated display strings instead of raw numbers
 * - Client performs ZERO calculations - only renders received data  
 * - Implements true "dumb terminal" client architecture
 * - All business logic happens server-side in TownResourceViewModelBuilder
 */
public class ResourceViewModelSyncPacket extends BaseViewModelSyncPacket<TownResourceViewModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceViewModelSyncPacket.class);

    public ResourceViewModelSyncPacket(BlockPos pos, TownResourceViewModel viewModel) {
        super(pos, viewModel);
    }

    public ResourceViewModelSyncPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    protected TownResourceViewModel readViewModel(FriendlyByteBuf buf) {
        return new TownResourceViewModel(buf);
    }

    public static void encode(ResourceViewModelSyncPacket msg, FriendlyByteBuf buf) {
        BaseViewModelSyncPacket.encode(msg, buf);
    }

    public static ResourceViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new ResourceViewModelSyncPacket(buf);
    }

    @Override
    public void handle(Object context) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "[CLIENT] ResourceViewModelSyncPacket received for pos {}", pos);
        super.handle(context);
    }
}