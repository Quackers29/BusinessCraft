package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.ProductionStatusViewModel;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-authoritative production sync packet implementing the "View-Model" pattern.
 *
 * This packet eliminates client-side ProductionRegistry access and config file loading.
 *
 * KEY DIFFERENCES FROM OLD APPROACH:
 * - Client never reads productions.csv config file
 * - Client never accesses ProductionRegistry.get()
 * - All recipe names, rates, and formulas calculated server-side
 * - Implements true "dumb terminal" client architecture
 */
public class ProductionViewModelSyncPacket extends BaseViewModelSyncPacket<ProductionStatusViewModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionViewModelSyncPacket.class);

    public ProductionViewModelSyncPacket(BlockPos pos, ProductionStatusViewModel viewModel) {
        super(pos, viewModel);
    }

    public ProductionViewModelSyncPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    protected ProductionStatusViewModel readViewModel(FriendlyByteBuf buf) {
        return new ProductionStatusViewModel(buf);
    }

    public static void encode(ProductionViewModelSyncPacket msg, FriendlyByteBuf buf) {
        BaseViewModelSyncPacket.encode(msg, buf);
    }

    public static ProductionViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new ProductionViewModelSyncPacket(buf);
    }

    @Override
    public void handle(Object context) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "[CLIENT] ProductionViewModelSyncPacket received for pos {}", pos);
        super.handle(context);
    }
}