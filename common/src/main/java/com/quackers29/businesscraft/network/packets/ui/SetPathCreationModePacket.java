package com.quackers29.businesscraft.network.packets.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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

    private void write(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeBoolean(mode);
    }

    public static void encode(SetPathCreationModePacket msg, FriendlyByteBuf buf) {
        msg.write(buf);
    }

    public static SetPathCreationModePacket decode(FriendlyByteBuf buf) {
        return new SetPathCreationModePacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            handlePacket(context, (player, townInterface) -> {
                if (mode) {
                    PlatformAccess.getEntities().setPathCreationTarget(player, townInterface.getTownId());
                    // Set the active town block in ModEvents
                    PlatformAccess.getEvents().setActiveTownBlock(pos);
                    townInterface.setPathCreationMode(true);

                    // Reset any existing path to avoid confusion
                    townInterface.setPathStart(null);
                    townInterface.setPathEnd(null);

                    player.sendSystemMessage(
                            Component.literal("Entering path creation mode. Right-click blocks to set path points.")
                                    .withStyle(ChatFormatting.GREEN));
                } else {
                    ITownDataProvider provider = townInterface.getTownDataProvider();
                    if (provider != null && townInterface.getPathStart() != null
                            && townInterface.getPathEnd() != null) {
                        provider.setPathStart(townInterface.getPathStart());
                        provider.setPathEnd(townInterface.getPathEnd());
                        provider.markDirty();
                    }

                    townInterface.setPathCreationMode(false);
                    PlatformAccess.getEvents().clearActiveTownBlock();
                }
            });
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
}

