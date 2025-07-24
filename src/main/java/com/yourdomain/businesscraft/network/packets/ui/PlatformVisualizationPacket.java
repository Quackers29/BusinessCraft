package com.yourdomain.businesscraft.network.packets.ui;

import com.yourdomain.businesscraft.client.render.world.PlatformVisualizationRenderer;
import com.yourdomain.businesscraft.client.render.world.TownBoundaryVisualizationRenderer;
import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
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
            
            LOGGER.debug("Client received platform visualization enable for town at {}", pos);
        });
        context.setPacketHandled(true);
        return true;
    }
}