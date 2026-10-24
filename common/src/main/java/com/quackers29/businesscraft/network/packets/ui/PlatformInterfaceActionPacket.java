package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.platform.PlatformInterfaceService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Client → server action on the Town Interface the player is standing at.
 */
public class PlatformInterfaceActionPacket {
    public enum Action {
        TOGGLE_ENABLED,
        TOGGLE_DESTINATION,
        OPEN_TOWN
    }

    private final BlockPos kioskPos;
    private final Action action;
    private final UUID destinationId;

    public PlatformInterfaceActionPacket(BlockPos kioskPos, Action action, UUID destinationId) {
        this.kioskPos = kioskPos;
        this.action = action;
        this.destinationId = destinationId;
    }

    public PlatformInterfaceActionPacket(FriendlyByteBuf buf) {
        this.kioskPos = buf.readBlockPos();
        this.action = Action.values()[buf.readVarInt()];
        this.destinationId = buf.readBoolean() ? buf.readUUID() : null;
    }

    public static PlatformInterfaceActionPacket decode(FriendlyByteBuf buf) {
        return new PlatformInterfaceActionPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(kioskPos);
        buf.writeVarInt(action.ordinal());
        buf.writeBoolean(destinationId != null);
        if (destinationId != null) {
            buf.writeUUID(destinationId);
        }
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            Object sender = PlatformAccess.getNetwork().getSender(context);
            if (!(sender instanceof ServerPlayer player)) {
                return;
            }
            switch (action) {
                case TOGGLE_ENABLED -> PlatformInterfaceService.toggleEnabled(player, kioskPos);
                case TOGGLE_DESTINATION -> PlatformInterfaceService.toggleDestination(player, kioskPos, destinationId);
                case OPEN_TOWN -> PlatformInterfaceService.openTown(player, kioskPos);
            }
        });
    }
}
