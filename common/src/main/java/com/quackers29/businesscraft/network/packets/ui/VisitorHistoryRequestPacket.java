package com.quackers29.businesscraft.network.packets.ui;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

/**
 * Simple packet to request visitor history with resolved town names
 * Uses town UUID instead of coordinates for proper town-based lookup
 */
public class VisitorHistoryRequestPacket {
    private final UUID townId;
    
    public VisitorHistoryRequestPacket(UUID townId) {
        this.townId = townId;
    }
    
    public VisitorHistoryRequestPacket(FriendlyByteBuf buffer) {
        this.townId = buffer.readUUID();
    }
    
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(townId);
    }
    
    public UUID getTownId() { return townId; }
}