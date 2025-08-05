package com.quackers29.businesscraft.town;

import net.minecraft.core.BlockPos;
import java.util.UUID;

/**
 * Forge-specific implementation of visit history record.
 * Uses Minecraft's BlockPos for position storage.
 */
public class ForgeVisitHistoryRecord {
    private final long timestamp;
    private final UUID originTownId;
    private final int count;
    private final BlockPos originPos;

    public ForgeVisitHistoryRecord(long timestamp, UUID originTownId, int count, BlockPos originPos) {
        this.timestamp = timestamp;
        this.originTownId = originTownId;
        this.count = count;
        this.originPos = originPos;
    }

    public long getTimestamp() { 
        return timestamp; 
    }
    
    public UUID getOriginTownId() { 
        return originTownId; 
    }
    
    public int getCount() { 
        return count; 
    }
    
    public BlockPos getOriginPos() { 
        return originPos; 
    }
}