package com.quackers29.businesscraft.contract;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class CourierContract extends Contract {
    private net.minecraft.core.BlockPos sourceTownPos;
    private int sourceTownRadius;
    private String resourceId;
    private int quantity;
    private UUID destinationTownId;
    private String destinationTownName;
    private float reward;
    private UUID courierId;
    private long acceptedTime;

    public CourierContract(UUID issuerTownId, String issuerTownName, net.minecraft.core.BlockPos sourceTownPos,
            int sourceTownRadius, long duration, String resourceId, int quantity,
            UUID destinationTownId, String destinationTownName,
            float reward) {
        super(issuerTownId, issuerTownName, duration);
        this.sourceTownPos = sourceTownPos;
        this.sourceTownRadius = sourceTownRadius;
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.destinationTownId = destinationTownId;
        this.destinationTownName = destinationTownName;
        this.reward = reward;
        this.courierId = null;
        this.acceptedTime = 0;
    }

    public CourierContract(CompoundTag tag) {
        super(tag);
    }

    public String getResourceId() {
        return resourceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public UUID getDestinationTownId() {
        return destinationTownId;
    }

    public String getDestinationTownName() {
        return destinationTownName;
    }

    public float getReward() {
        return reward;
    }

    public UUID getCourierId() {
        return courierId;
    }

    public void setCourierId(UUID courierId) {
        this.courierId = courierId;
    }

    public long getAcceptedTime() {
        return acceptedTime;
    }

    public void setAcceptedTime(long acceptedTime) {
        this.acceptedTime = acceptedTime;
    }

    public boolean isAccepted() {
        return courierId != null;
    }

    public net.minecraft.core.BlockPos getSourceTownPos() {
        return sourceTownPos;
    }

    public int getSourceTownRadius() {
        return sourceTownRadius;
    }

    // UI convenience methods
    public int getAmount() {
        return quantity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("resourceId", resourceId);
        tag.putInt("quantity", quantity);
        tag.putUUID("destinationTownId", destinationTownId);
        if (destinationTownName != null) {
            tag.putString("destinationTownName", destinationTownName);
        }
        tag.putFloat("reward", reward);
        if (courierId != null) {
            tag.putUUID("courierId", courierId);
        }
        tag.putLong("acceptedTime", acceptedTime);
        if (sourceTownPos != null) {
            tag.putInt("sourceX", sourceTownPos.getX());
            tag.putInt("sourceY", sourceTownPos.getY());
            tag.putInt("sourceZ", sourceTownPos.getZ());
        }
        tag.putInt("sourceRadius", sourceTownRadius);
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        resourceId = tag.getString("resourceId");
        quantity = tag.getInt("quantity");
        destinationTownId = tag.getUUID("destinationTownId");
        if (tag.contains("destinationTownName")) {
            destinationTownName = tag.getString("destinationTownName");
        }
        reward = tag.getFloat("reward");
        if (tag.hasUUID("courierId")) {
            courierId = tag.getUUID("courierId");
        }
        if (tag.contains("acceptedTime")) {
            acceptedTime = tag.getLong("acceptedTime");
        }
        if (tag.contains("sourceX")) {
            sourceTownPos = new net.minecraft.core.BlockPos(tag.getInt("sourceX"), tag.getInt("sourceY"),
                    tag.getInt("sourceZ"));
        }
        sourceTownRadius = tag.getInt("sourceRadius");
    }

    @Override
    public String getType() {
        return "courier";
    }
}
