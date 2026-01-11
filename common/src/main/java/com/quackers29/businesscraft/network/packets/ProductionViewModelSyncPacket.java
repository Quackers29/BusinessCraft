package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.viewmodel.ProductionStatusViewModel;
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
public class ProductionViewModelSyncPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionViewModelSyncPacket.class);

    private final BlockPos pos;
    private final ProductionStatusViewModel productionViewModel;

    /**
     * Creates a new production view-model sync packet (SERVER-SIDE)
     * @param pos Block position of the town interface
     * @param productionViewModel Pre-calculated view-model from server config
     */
    public ProductionViewModelSyncPacket(BlockPos pos, ProductionStatusViewModel productionViewModel) {
        this.pos = pos;
        this.productionViewModel = productionViewModel;
    }

    /**
     * Deserializes packet from network buffer (CLIENT-SIDE)
     */
    public ProductionViewModelSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.productionViewModel = new ProductionStatusViewModel(buf);
    }

    /**
     * Serializes packet to network buffer (SERVER-SIDE)
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        productionViewModel.toBytes(buf);
    }

    public static void encode(ProductionViewModelSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static ProductionViewModelSyncPacket decode(FriendlyByteBuf buf) {
        return new ProductionViewModelSyncPacket(buf);
    }

    /**
     * Handles packet on client side - PURE DISPLAY LOGIC ONLY
     * No config access, no calculations, no ProductionRegistry calls
     */
    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            LOGGER.debug("[CLIENT] ProductionViewModelSyncPacket received for pos {}", pos);

            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity entity) {
                // Update the client cache with pre-calculated view-model
                // NO CONFIG ACCESS - client is truly a "dumb terminal"
                entity.updateProductionViewModel(productionViewModel);

                // Refresh open UI if TownInterfaceScreen is active
                if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen screen) {
                    screen.getMenu().refreshDataSlots();
                }

                LOGGER.debug("[CLIENT] Production view-model updated: {} recipes, status: {}",
                    productionViewModel.getRecipeCount(),
                    productionViewModel.getOverallStatus());
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}