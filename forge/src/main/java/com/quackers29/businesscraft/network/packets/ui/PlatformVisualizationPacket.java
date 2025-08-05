package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.client.render.world.PlatformVisualizationRenderer;
import com.quackers29.businesscraft.client.render.world.TownBoundaryVisualizationRenderer;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.network.packets.ui.BoundarySyncRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to enable platform visualization
 * for a specific town block for 30 seconds.
 */
public class PlatformVisualizationPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformVisualizationPacket.class);

    public PlatformVisualizationPacket(BlockPos pos) {
        super(pos);
    }

    public PlatformVisualizationPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PlatformVisualizationPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PlatformVisualizationPacket decode(FriendlyByteBuf buf) {
        return new PlatformVisualizationPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-side handling
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            
            // Register the town block for platform visualization using the new modular system
            PlatformVisualizationRenderer.showPlatformVisualization(pos, level.getGameTime());
            
            // Also trigger boundary visualization at the same time as platforms
            TownBoundaryVisualizationRenderer.showTownBoundaryVisualization(pos, level.getGameTime());
            
            // Immediately request boundary data from server (don't wait for periodic sync)
            ModMessages.sendToServer(new BoundarySyncRequestPacket(pos));
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Client received platform visualization enable for town at {}, requested immediate boundary sync", pos);
        });
        context.setPacketHandled(true);
        return true;
    }
}