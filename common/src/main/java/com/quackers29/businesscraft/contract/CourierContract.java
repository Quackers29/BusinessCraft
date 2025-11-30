package com.quackers29.businesscraft.contract;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class CourierContract extends Contract {
    private String resourceId;
    private int quantity;
    private UUID destinationTownId;
    private float reward;
    private UUID courierId;

    public CourierContract(UUID issuerTownId, long duration, String resourceId, int quantity, UUID destinationTownId,
            float reward) {
        super(issuerTownId, duration);
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.destinationTownId = destinationTownId;
        this.reward = reward;
        this.courierId = null;
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

    public float getReward() {
        return reward;
    }

    public UUID getCourierId() {
        return courierId;
    }

    public void setCourierId(UUID courierId) {
        this.courierId = courierId;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("resourceId", resourceId);
        tag.putInt("quantity", quantity);
        tag.putUUID("destinationTownId", destinationTownId);
        tag.putFloat("reward", reward);
        if (courierId != null) {
            tag.putUUID("courierId", courierId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        resourceId = tag.getString("resourceId");
        quantity = tag.getInt("quantity");
        destinationTownId = tag.getUUID("destinationTownId");
        reward = tag.getFloat("reward");
        if (tag.hasUUID("courierId")) {
            courierId = tag.getUUID("courierId");
        }
    }

    @Override
    public String getType() {
        return "courier";
    }
}
