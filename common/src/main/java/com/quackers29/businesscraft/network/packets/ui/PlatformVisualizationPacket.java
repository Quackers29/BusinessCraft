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

import java.util.UUID;

/**
 * Packet sent from server to client to enable platform visualization
 * for 30 seconds. A null platform id draws every platform at a Town Centre.
 * A platform id draws only that platform, anchored on its Platform Interface.
 */
public class PlatformVisualizationPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformVisualizationPacket.class);
    private final UUID platformId;

    public PlatformVisualizationPacket(BlockPos pos) {
        this(pos, null);
    }

    public PlatformVisualizationPacket(BlockPos pos, UUID platformId) {
        super(pos);
        this.platformId = platformId;
    }

    public PlatformVisualizationPacket(FriendlyByteBuf buf) {
        super(buf);
        this.platformId = buf.readBoolean() ? buf.readUUID() : null;
    }

    public UUID getPlatformId() {
        return platformId;
    }

    private void write(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeBoolean(platformId != null);
        if (platformId != null) {
            buf.writeUUID(platformId);
        }
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

            if (platformId != null) {
                PlatformVisualizationRenderer.showPlatformPath(pos, platformId, level.getGameTime());
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Client received path visualization for platform {} at {}", platformId, pos);
            } else {
                PlatformVisualizationRenderer.showPlatformVisualization(pos, level.getGameTime());
                TownBoundaryVisualizationRenderer.showTownBoundaryVisualization(pos, level.getGameTime());
                PlatformAccess.getNetworkMessages().sendToServer(new BoundarySyncRequestPacket(pos));
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Client received platform visualization enable for town at {}, requested immediate boundary sync", pos);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}

