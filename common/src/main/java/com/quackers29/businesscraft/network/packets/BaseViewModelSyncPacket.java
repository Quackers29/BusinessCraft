package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.quackers29.businesscraft.town.viewmodel.IViewModel;

public abstract class BaseViewModelSyncPacket<T extends IViewModel> {
    protected final BlockPos pos;
    protected final T viewModel;

    public BaseViewModelSyncPacket(BlockPos pos, T viewModel) {
        this.pos = pos;
        this.viewModel = viewModel;
    }

    public BaseViewModelSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.viewModel = readViewModel(buf);
    }

    protected abstract T readViewModel(FriendlyByteBuf buf);

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        viewModel.toBytes(buf);
    }

    public static void encode(BaseViewModelSyncPacket<?> msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static BaseViewModelSyncPacket<?> decode(FriendlyByteBuf buf) {
        throw new UnsupportedOperationException("Use subclass decode");
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity entity) {
                @SuppressWarnings("unchecked")
                Class<T> vmClass = (Class<T>) viewModel.getClass();
                entity.getVmCache().update(vmClass, viewModel);

                if (mc.screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen screen) {
                    screen.getMenu().refreshDataSlots();
                }
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}