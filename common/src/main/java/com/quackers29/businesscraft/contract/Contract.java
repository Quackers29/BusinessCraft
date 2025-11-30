package com.quackers29.businesscraft.contract;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public abstract class Contract {
    protected UUID id;
    protected UUID issuerTownId;
    protected long creationTime;
    protected long expiryTime;
    protected boolean isCompleted;

    public Contract(UUID issuerTownId, long duration) {
        this.id = UUID.randomUUID();
        this.issuerTownId = issuerTownId;
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

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void complete() {
        this.isCompleted = true;
    }

    public void save(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putUUID("issuerTownId", issuerTownId);
        tag.putLong("creationTime", creationTime);
        tag.putLong("expiryTime", expiryTime);
        tag.putBoolean("isCompleted", isCompleted);
        saveAdditional(tag);
    }

    public void load(CompoundTag tag) {
        id = tag.getUUID("id");
        issuerTownId = tag.getUUID("issuerTownId");
        creationTime = tag.getLong("creationTime");
        expiryTime = tag.getLong("expiryTime");
        isCompleted = tag.getBoolean("isCompleted");
        loadAdditional(tag);
    }

    protected abstract void saveAdditional(CompoundTag tag);

    protected abstract void loadAdditional(CompoundTag tag);

    public abstract String getType();
}
