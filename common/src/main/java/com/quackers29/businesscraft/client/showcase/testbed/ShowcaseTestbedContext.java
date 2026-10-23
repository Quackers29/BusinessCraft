package com.quackers29.businesscraft.client.showcase.testbed;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared world state for showcase testbed checks (two towns on the showcase pad).
 */
public final class ShowcaseTestbedContext {
    private BlockPos townAPos = BlockPos.ZERO;
    private BlockPos townBPos = BlockPos.ZERO;
    private UUID townAId;
    private UUID townBId;
    private int requiredCentreDistance;
    private final Map<String, Object> bag = new HashMap<>();

    public BlockPos townAPos() {
        return townAPos;
    }

    public void setTownAPos(BlockPos townAPos) {
        this.townAPos = townAPos;
    }

    public BlockPos townBPos() {
        return townBPos;
    }

    public void setTownBPos(BlockPos townBPos) {
        this.townBPos = townBPos;
    }

    public UUID townAId() {
        return townAId;
    }

    public void setTownAId(UUID townAId) {
        this.townAId = townAId;
    }

    public UUID townBId() {
        return townBId;
    }

    public void setTownBId(UUID townBId) {
        this.townBId = townBId;
    }

    public int requiredCentreDistance() {
        return requiredCentreDistance;
    }

    public void setRequiredCentreDistance(int requiredCentreDistance) {
        this.requiredCentreDistance = requiredCentreDistance;
    }

    public void put(String key, Object value) {
        bag.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) bag.get(key);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> copy = new HashMap<>();
        copy.put("townA", townAPos.toShortString());
        copy.put("townB", townBPos.toShortString());
        copy.put("townAId", townAId != null ? townAId.toString() : null);
        copy.put("townBId", townBId != null ? townBId.toString() : null);
        copy.put("requiredCentreDistance", requiredCentreDistance);
        copy.putAll(bag);
        return copy;
    }
}
