package com.quackers29.businesscraft.contract;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Contract {
    protected UUID id;
    protected UUID issuerTownId;
    protected String issuerTownName;
    protected long creationTime;
    protected long expiryTime;
    protected boolean isCompleted;
    protected Map<UUID, Float> bids = new HashMap<>();

    public Contract(UUID issuerTownId, String issuerTownName, long duration) {
        this.id = UUID.randomUUID();
        this.issuerTownId = issuerTownId;
        this.issuerTownName = issuerTownName;
        this.creationTime = System.currentTimeMillis();
        this.expiryTime = this.creationTime + duration;
        this.isCompleted = false;
    }

    public Contract(CompoundTag tag) {
        load(tag);
    }

    public UUID getId() {
        return id;
    }

    public UUID getIssuerTownId() {
        return issuerTownId;
    }

    public String getIssuerTownName() {
        return issuerTownName;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void complete() {
        this.isCompleted = true;
    }

    public void extendExpiry(long additionalMillis) {
        this.expiryTime = System.currentTimeMillis() + additionalMillis;
    }

    public Map<UUID, Float> getBids() {
        return bids;
    }

    public void addBid(UUID bidder, float amount) {
        bids.put(bidder, Math.max(bids.getOrDefault(bidder, 0f), amount));
    }

    public float getHighestBid() {
        return bids.isEmpty() ? 0f : bids.values().stream().max(Float::compare).orElse(0f);
    }

    public UUID getHighestBidder() {
        return bids.entrySet().stream()
                .max(Map.Entry.comparingByValue(Float::compare))
                .map(Map.Entry::getKey).orElse(null);
    }

    public void save(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putUUID("issuerTownId", issuerTownId);
        if (issuerTownName != null) {
            tag.putString("issuerTownName", issuerTownName);
        }
        tag.putLong("creationTime", creationTime);
        tag.putLong("expiryTime", expiryTime);
        tag.putBoolean("isCompleted", isCompleted);
        saveAdditional(tag);
    }

    public void load(CompoundTag tag) {
        id = tag.getUUID("id");
        issuerTownId = tag.getUUID("issuerTownId");
        if (tag.contains("issuerTownName")) {
            issuerTownName = tag.getString("issuerTownName");
        } else {
            issuerTownName = "Unknown Town";
        }
        creationTime = tag.getLong("creationTime");
        expiryTime = tag.getLong("expiryTime");
        isCompleted = tag.getBoolean("isCompleted");
        loadAdditional(tag);
    }

    protected abstract void saveAdditional(CompoundTag tag);

    protected abstract void loadAdditional(CompoundTag tag);

    public abstract String getType();
}
