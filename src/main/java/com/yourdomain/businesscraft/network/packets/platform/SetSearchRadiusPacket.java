package com.yourdomain.businesscraft.network.packets.platform;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetSearchRadiusPacket extends BaseBlockEntityPacket {
    private final int radius;

    public SetSearchRadiusPacket(BlockPos pos, int radius) {
        super(pos);
        this.radius = radius;
    }

    public SetSearchRadiusPacket(FriendlyByteBuf buf) {
        super(buf);
        this.radius = buf.readInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(radius);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            handlePacket(context, (player, townBlock) -> {
                ITownDataProvider provider = townBlock.getTownDataProvider();
                if (provider != null) {
                    // Update the provider (single source of truth)
                    provider.setSearchRadius(radius);
                    provider.markDirty();
                    
                    // Also directly update the block entity
                    townBlock.setSearchRadius(radius);
                    
                    // Sync with block entity
                    townBlock.syncTownData();
                }
            });
        });
        context.setPacketHandled(true);
    }
} 