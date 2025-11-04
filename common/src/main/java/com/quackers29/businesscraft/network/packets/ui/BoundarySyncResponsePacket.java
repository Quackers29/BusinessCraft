package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.client.render.world.TownBoundaryVisualizationRenderer;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet sent from server to client with updated boundary data
 * for boundary visualization during the active visualization window.
 */
public class BoundarySyncResponsePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoundarySyncResponsePacket.class);
    private final int boundaryRadius;

    public BoundarySyncResponsePacket(BlockPos pos, int boundaryRadius) {
        super(pos);
        this.boundaryRadius = boundaryRadius;
    }

    public BoundarySyncResponsePacket(FriendlyByteBuf buf) {
        super(buf);
        this.boundaryRadius = buf.readInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(boundaryRadius);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(BoundarySyncResponsePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static BoundarySyncResponsePacket decode(FriendlyByteBuf buf) {
        return new BoundarySyncResponsePacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Client-side handling - update the boundary visualization directly
            TownBoundaryVisualizationRenderer.updateBoundaryRadius(pos, boundaryRadius);
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Boundary sync response for town at {}: updated to boundary radius={}", pos, boundaryRadius);
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
