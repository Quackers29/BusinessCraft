package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.client.render.world.PlatformVisualizationRenderer;
import com.quackers29.businesscraft.client.render.world.TownBoundaryVisualizationRenderer;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private void write(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }

    public static void encode(PlatformVisualizationPacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static PlatformVisualizationPacket decode(FriendlyByteBuf buf) {
        return new PlatformVisualizationPacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper == null) return;

            Object levelObj = clientHelper.getClientLevel();
            if (!(levelObj instanceof Level level)) return;

            PlatformVisualizationRenderer.showPlatformVisualization(pos, level.getGameTime());
            
            // Also trigger boundary visualization at the same time as platforms
            TownBoundaryVisualizationRenderer.showTownBoundaryVisualization(pos, level.getGameTime());
            
            // Immediately request boundary data from server (don't wait for periodic sync)
            PlatformAccess.getNetworkMessages().sendToServer(new BoundarySyncRequestPacket(pos));
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Client received platform visualization enable for town at {}, requested immediate boundary sync", pos);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}

