package com.yourdomain.businesscraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.event.ModEvents;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.function.Supplier;

public class SetPathCreationModePacket extends BaseBlockEntityPacket {
    private final boolean mode;

    public SetPathCreationModePacket(BlockPos pos, boolean mode) {
        super(pos);
        this.mode = mode;
    }

    public static void encode(SetPathCreationModePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.getPos());
        buf.writeBoolean(msg.isEnteringMode());
    }

    public static SetPathCreationModePacket decode(FriendlyByteBuf buf) {
        return new SetPathCreationModePacket(buf.readBlockPos(), buf.readBoolean());
    }

    public boolean isEnteringMode() {
        return mode;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeBoolean(mode);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            handlePacket(context, (player, townBlock) -> {
                if (mode) {
                    // Start path creation mode
                    player.getPersistentData().putUUID("CurrentTownBlock", 
                            townBlock.getTownId());
                    // Set the active town block in ModEvents
                    ModEvents.setActiveTownBlock(pos);
                    // Enable path creation mode on the town block entity
                    townBlock.setPathCreationMode(true);
                    
                    // Reset any existing path to avoid confusion
                    townBlock.setPathStart(null);
                    townBlock.setPathEnd(null);
                    
                    // Notify player using proper formatting
                    player.sendSystemMessage(Component.literal("Entering path creation mode. Right-click blocks to set path points.")
                        .withStyle(ChatFormatting.GREEN));
                } else {
                    // Path creation complete
                    ITownDataProvider provider = townBlock.getTownDataProvider();
                    if (provider != null && townBlock.getPathStart() != null && townBlock.getPathEnd() != null) {
                        // Update provider with path data
                        provider.setPathStart(townBlock.getPathStart());
                        provider.setPathEnd(townBlock.getPathEnd());
                        provider.markDirty();
                    }
                    
                    // Disable path creation mode
                    townBlock.setPathCreationMode(false);
                    ModEvents.setActiveTownBlock(null);
                }
            });
        });
        context.setPacketHandled(true);
    }
} 