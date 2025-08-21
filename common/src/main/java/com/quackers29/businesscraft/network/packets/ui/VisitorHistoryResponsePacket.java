package com.quackers29.businesscraft.network.packets.ui;

import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simple response packet with visitor history entries and resolved town names
 * Follows payment board pattern for unified architecture
 */
public class VisitorHistoryResponsePacket {
    private final List<VisitorEntry> entries;
    
    public VisitorHistoryResponsePacket(List<VisitorEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }
    
    public VisitorHistoryResponsePacket(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        this.entries = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            long timestamp = buffer.readLong();
            UUID townId = buffer.readUUID();
            int count = buffer.readInt();
            int x = buffer.readInt();
            int y = buffer.readInt();
            int z = buffer.readInt();
            String resolvedName = buffer.readUtf();
            
            entries.add(new VisitorEntry(timestamp, townId, count, x, y, z, resolvedName));
        }
    }
    
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entries.size());
        
        for (VisitorEntry entry : entries) {
            buffer.writeLong(entry.timestamp);
            buffer.writeUUID(entry.townId);
            buffer.writeInt(entry.count);
            buffer.writeInt(entry.x);
            buffer.writeInt(entry.y);
            buffer.writeInt(entry.z);
            buffer.writeUtf(entry.resolvedName);
        }
    }
    
    public List<VisitorEntry> getEntries() { return entries; }
    
    /**
     * Simple data class for visitor history entry with resolved name
     */
    public static class VisitorEntry {
        public final long timestamp;
        public final UUID townId;
        public final int count;
        public final int x, y, z;
        public final String resolvedName;
        
        public VisitorEntry(long timestamp, UUID townId, int count, int x, int y, int z, String resolvedName) {
            this.timestamp = timestamp;
            this.townId = townId;
            this.count = count;
            this.x = x;
            this.y = y;
            this.z = z;
            this.resolvedName = resolvedName;
        }
    }
}