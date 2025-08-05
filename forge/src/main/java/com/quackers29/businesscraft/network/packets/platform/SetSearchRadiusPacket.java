package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
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
            handlePacket(context, (player, townInterface) -> {
                ITownDataProvider provider = townInterface.getTownDataProvider();
                if (provider != null) {
                    // Update the provider (single source of truth)
                    provider.setSearchRadius(radius);
                    provider.markDirty();
                    
                    // Also directly update the block entity
                    townInterface.setSearchRadius(radius);
                    
                    // If the player has the TownInterface menu open, refresh it
                    if (player.containerMenu instanceof com.quackers29.businesscraft.menu.TownInterfaceMenu townMenu) {
                        townMenu.refreshSearchRadius();
                    }
                    
                    // Sync with block entity
                    townInterface.syncTownData();
                }
            });
        });
        context.setPacketHandled(true);
    }
} 