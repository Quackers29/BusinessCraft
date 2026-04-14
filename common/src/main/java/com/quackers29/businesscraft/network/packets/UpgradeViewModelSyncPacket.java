package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel;
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
public class UpgradeViewModelSyncPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeViewModelSyncPacket.class);

    private final BlockPos pos;
    private final UpgradeStatusViewModel upgradeViewModel;

    /**
     * Creates a new upgrade view-model sync packet (SERVER-SIDE)
     * @param pos Block position of the town interface
     * @param upgradeViewModel Pre-calculated view-model from server config
     */
    public UpgradeViewModelSyncPacket(BlockPos pos, UpgradeStatusViewModel upgradeViewModel) {
        this.pos = pos;
        this.upgradeViewModel = upgradeViewModel;
    }

    /**
     * Deserializes packet from network buffer (CLIENT-SIDE)
     */
    public UpgradeViewModelSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.upgradeViewModel = new UpgradeStatusViewModel(buf);
    }

    /**
     * Serializes packet to network buffer (SERVER-SIDE)
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        upgradeViewModel.toBytes(buf);
    }

    public static void encode(UpgradeViewModelSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static UpgradeViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new UpgradeViewModelSyncPacket(buf);
    }

    /**
     * Handles packet on client side - PURE DISPLAY LOGIC ONLY
     * No config access, no calculations, no UpgradeRegistry calls
     */
    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            LOGGER.debug("[CLIENT] UpgradeViewModelSyncPacket received for pos {}", pos);

            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity entity) {
                // Update the client cache with pre-calculated view-model
                // NO CONFIG ACCESS - client is truly a "dumb terminal"
                entity.getVmCache().update(UpgradeStatusViewModel.class, upgradeViewModel);

                // Refresh open UI if TownInterfaceScreen is active
                if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen screen) {
                    screen.getMenu().refreshDataSlots();
                }

                LOGGER.debug("[CLIENT] Upgrade view-model updated: {} upgrades, {} unlocked, status: {}",
                    upgradeViewModel.getUpgradeCount(),
                    upgradeViewModel.getUnlockedCount(),
                    upgradeViewModel.getCurrentResearchStatus());
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}
