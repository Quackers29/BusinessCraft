package com.quackers29.businesscraft.network.packets;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.ClientPacketDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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

    public BlockPos getPos() {
        return pos;
    }

    public T getViewModel() {
        return viewModel;
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> ClientPacketDispatcher.dispatch(this));
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}