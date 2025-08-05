package com.quackers29.businesscraft.network.packets.town;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class ToggleTouristSpawningPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToggleTouristSpawningPacket.class);

    public ToggleTouristSpawningPacket(BlockPos pos) {
        super(pos);
    }

    public ToggleTouristSpawningPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            handlePacket(context, (player, townBlock) -> {
                ITownDataProvider provider = townBlock.getTownDataProvider();
                if (provider != null) {
                    boolean newState = !provider.isTouristSpawningEnabled();
                    DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Toggling tourist spawning to {} for town {}", newState, provider.getTownId());
                    
                    // Update the provider (single source of truth)
                    provider.setTouristSpawningEnabled(newState);
                    provider.markDirty();
                    
                    // Sync with block entity
                    townBlock.syncTownData();
                }
            });
        });
        context.setPacketHandled(true);
    }
} 