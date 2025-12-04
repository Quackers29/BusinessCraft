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

    public long getCreationTime() {
        return creationTime;
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

    protected Map<UUID, String> bidderNames = new HashMap<>();

    public void addBid(UUID bidder, String name, float amount) {
        bids.put(bidder, Math.max(bids.getOrDefault(bidder, 0f), amount));
        if (name != null) {
            bidderNames.put(bidder, name);
        }
    }

    public String getBidderName(UUID bidder) {
        return bidderNames.getOrDefault(bidder, "Unknown Town");
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

        // Save bids
        net.minecraft.nbt.ListTag bidsList = new net.minecraft.nbt.ListTag();
        for (Map.Entry<UUID, Float> entry : bids.entrySet()) {
            CompoundTag bidTag = new CompoundTag();
            bidTag.putUUID("bidder", entry.getKey());
            bidTag.putFloat("amount", entry.getValue());
            String name = bidderNames.get(entry.getKey());
            if (name != null) {
                bidTag.putString("name", name);
            }
            bidsList.add(bidTag);
        }
        tag.put("bids", bidsList);

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

        // Load bids
        bids.clear();
        bidderNames.clear();
        if (tag.contains("bids")) {
            net.minecraft.nbt.ListTag bidsList = tag.getList("bids", 10); // 10 = CompoundTag
            for (int i = 0; i < bidsList.size(); i++) {
                CompoundTag bidTag = bidsList.getCompound(i);
                UUID bidder = bidTag.getUUID("bidder");
                bids.put(bidder, bidTag.getFloat("amount"));
                if (bidTag.contains("name")) {
                    bidderNames.put(bidder, bidTag.getString("name"));
                }
            }
        }

        loadAdditional(tag);
    }

    protected abstract void saveAdditional(CompoundTag tag);

    protected abstract void loadAdditional(CompoundTag tag);

    public abstract String getType();
}
