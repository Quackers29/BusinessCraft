package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-authoritative upgrade sync packet implementing the "View-Model" pattern.
 *
 * This packet eliminates client-side UpgradeRegistry access and config file loading.
 *
 * KEY DIFFERENCES FROM OLD APPROACH:
 * - Client never reads upgrades.csv config file
 * - Client never accesses UpgradeRegistry.get() or UpgradeRegistry.getAll()
 * - All upgrade names, costs, effects, and research times calculated server-side
 * - Implements true "dumb terminal" client architecture
 *
 * REPLACES CLIENT-SIDE BUSINESS LOGIC IN:
 * - ProductionTab lines 155-310 (research speed calculations, cost scaling, prerequisite checks)
 * - TownDataCacheManager lines 262-310 (getCachedResearchSpeed, getResearchSpeedTooltip)
 */
public class UpgradeViewModelSyncPacket extends BaseViewModelSyncPacket<UpgradeStatusViewModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeViewModelSyncPacket.class);

    public UpgradeViewModelSyncPacket(BlockPos pos, UpgradeStatusViewModel viewModel) {
        super(pos, viewModel);
    }

    public UpgradeViewModelSyncPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    protected UpgradeStatusViewModel readViewModel(FriendlyByteBuf buf) {
        return new UpgradeStatusViewModel(buf);
    }

    public static void encode(UpgradeViewModelSyncPacket msg, FriendlyByteBuf buf) {
        BaseViewModelSyncPacket.encode(msg, buf);
    }

    public static UpgradeViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new UpgradeViewModelSyncPacket(buf);
    }

    @Override
    public void handle(Object context) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "[CLIENT] UpgradeViewModelSyncPacket received for pos {}", pos);
        super.handle(context);
    }
}
