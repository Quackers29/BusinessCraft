package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;
import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet to request opening the Payment Board UI using proper Minecraft container system.
 * Sent from client to server to request opening the Payment Board.
 */
public class OpenPaymentBoardPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenPaymentBoardPacket.class);

    public OpenPaymentBoardPacket(BlockPos pos) {
        super(pos);
    }

    public OpenPaymentBoardPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
    }

    // Static methods for Forge network registration
    public static void encode(OpenPaymentBoardPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static OpenPaymentBoardPacket decode(FriendlyByteBuf buf) {
        return new OpenPaymentBoardPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            handlePacket(context, (player, townBlockEntity) -> {
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Opening Payment Board for player {} at position {}", player.getName().getString(), pos);
                
                // Use NetworkHooks to properly open the container with server-client sync
                NetworkHooks.openScreen(player, townBlockEntity.createPaymentBoardMenuProvider(), pos);
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Successfully opened Payment Board for player {}", player.getName().getString());
            });
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
}
