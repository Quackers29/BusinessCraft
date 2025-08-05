package com.yourdomain.businesscraft.network.packets.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.event.ModEvents;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.function.Supplier;

public class SetPathCreationModePacket extends BaseBlockEntityPacket {
    private final boolean mode;

    public SetPathCreationModePacket(BlockPos pos, boolean mode) {
        super(pos);
        this.mode = mode;
    }

    public SetPathCreationModePacket(FriendlyByteBuf buf) {
        super(buf);
        this.mode = buf.readBoolean();
    }

    public boolean isEnteringMode() {
        return mode;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeBoolean(mode);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(SetPathCreationModePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static SetPathCreationModePacket decode(FriendlyByteBuf buf) {
        return new SetPathCreationModePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            handlePacket(context, (player, townInterface) -> {
                if (mode) {
                    // Start path creation mode
                    player.getPersistentData().putUUID("CurrentTownBlock", 
                            townInterface.getTownId());
                    // Set the active town block in ModEvents
                    ModEvents.setActiveTownBlock(pos);
                    // Enable path creation mode on the town block entity
                    townInterface.setPathCreationMode(true);
                    
                    // Reset any existing path to avoid confusion
                    townInterface.setPathStart(null);
                    townInterface.setPathEnd(null);
                    
                    // Notify player using proper formatting
                    player.sendSystemMessage(Component.literal("Entering path creation mode. Right-click blocks to set path points.")
                        .withStyle(ChatFormatting.GREEN));
                } else {
                    // Path creation complete
                    ITownDataProvider provider = townInterface.getTownDataProvider();
                    if (provider != null && townInterface.getPathStart() != null && townInterface.getPathEnd() != null) {
                        // Update provider with path data
                        provider.setPathStart(townInterface.getPathStart());
                        provider.setPathEnd(townInterface.getPathEnd());
                        provider.markDirty();
                    }
                    
                    // Disable path creation mode
                    townInterface.setPathCreationMode(false);
                    ModEvents.setActiveTownBlock(null);
                }
            });
        });
        context.setPacketHandled(true);
    }
} 