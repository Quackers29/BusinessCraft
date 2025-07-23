package com.yourdomain.businesscraft.network.packets.town;

import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.yourdomain.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.network.packets.ui.ClientTownMapCache;
import com.yourdomain.businesscraft.ui.screens.town.TownInterfaceScreen;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

public class SetTownNamePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetTownNamePacket.class);
    private final String newName;

    public SetTownNamePacket(BlockPos pos, String newName) {
        super(pos);
        this.newName = newName;
    }

    public SetTownNamePacket(FriendlyByteBuf buf) {
        super(buf);
        this.newName = buf.readUtf();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeUtf(newName);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(SetTownNamePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static SetTownNamePacket decode(FriendlyByteBuf buf) {
        return new SetTownNamePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            handlePacket(context, (player, townBlock) -> {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing SetTownNamePacket for position {} with new name: '{}'", 
                    pos, newName);
                    
                ITownDataProvider provider = townBlock.getTownDataProvider();
                if (provider != null && provider instanceof Town town) {
                    // Validate the new name
                    String trimmedName = newName.trim();
                    if (trimmedName.isEmpty()) {
                        player.sendSystemMessage(Component.literal("Town name cannot be empty").withStyle(ChatFormatting.RED));
                        return;
                    }
                    
                    if (trimmedName.length() > 30) {
                        player.sendSystemMessage(Component.literal("Town name cannot exceed 30 characters").withStyle(ChatFormatting.RED));
                        return;
                    }
                    
                    // Update the name in the Town object
                    town.setName(trimmedName);
                    
                    // Update the town data and make sure to mark it dirty
                    provider.markDirty();
                    
                    // Sync with block entity and client
                    townBlock.syncTownData();
                    
                    // Force a block state update to ensure all clients get the change
                    BlockState state = player.level().getBlockState(pos);
                    player.level().sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                    
                    // Only keep one debug log for verification
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Town renamed successfully from {} to {}", 
                        town.getName(), trimmedName);
                    
                    // Invalidate client-side caches to ensure UI shows updated name immediately
                    ClientTownMapCache.getInstance().clear();
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Cleared ClientTownMapCache after town rename");
                    
                    // Invalidate TownDataCache in the current screen if it's a TownInterfaceScreen
                    try {
                        if (Minecraft.getInstance().screen instanceof TownInterfaceScreen townScreen) {
                            townScreen.invalidateCache();
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Invalidated TownDataCache in current screen");
                        }
                    } catch (Exception e) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Could not invalidate screen cache: {}", e.getMessage());
                    }
                    
                    // Send confirmation message to player
                    player.sendSystemMessage(Component.literal("Town renamed to: ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(trimmedName).withStyle(ChatFormatting.GOLD)));
                }
            });
        });
        context.setPacketHandled(true);
    }
}