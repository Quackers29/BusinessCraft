package com.quackers29.businesscraft.entity.ai.goal;

import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * AI goal for tourists to gaze out the window when riding in vehicles.
 * Makes them look perpendicular to the direction of travel.
 */
public class TouristGazeGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristGazeGoal.class);

    private final TouristEntity tourist;
    private int gazeDuration;
    private boolean lookingLeft;
    private float gazeYawOffset; // Head yaw angle offset (relative to entity direction)
    private int nextGazeTick;

    private static final int MIN_GAZE_DURATION = 60; // 3 seconds
    private static final int MAX_GAZE_DURATION = 100; // 5 seconds
    private static final int MIN_GAZE_INTERVAL = 200; // 10 seconds between gazes
    private static final int MAX_GAZE_INTERVAL = 400; // 20 seconds between gazes

    public TouristGazeGoal(TouristEntity tourist) {
        this.tourist = tourist;
        this.setFlags(EnumSet.of(Flag.LOOK));
        this.lookingLeft = tourist.getRandom().nextBoolean();
        this.nextGazeTick = tourist.getRandom().nextInt(MIN_GAZE_INTERVAL, MAX_GAZE_INTERVAL);
    }

    @Override
    public boolean canUse() {
        // Only gaze when riding and not gossiping
        if (!tourist.isPassenger() || tourist.isGossiping()) {
            return false;
        }

        // Check if gazing or waiting for next gaze
        if (gazeDuration > 0) {
            return true; // Continue current gaze
        }

        // Check if enough time passed since last gaze
        if (--nextGazeTick > 0) {
            return false; // Still waiting
        }

        // Time to start new gaze
        gazeDuration = tourist.getRandom().nextInt(MIN_GAZE_DURATION, MAX_GAZE_DURATION);
        lookingLeft = tourist.getRandom().nextBoolean();
        nextGazeTick = tourist.getRandom().nextInt(MIN_GAZE_INTERVAL, MAX_GAZE_INTERVAL);

        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
            "Tourist {} starting new gaze (duration: {} ticks, next in {} ticks)",
            tourist.getId(), gazeDuration, nextGazeTick);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return tourist.isPassenger() && !tourist.isGossiping() && gazeDuration > 0;
    }

    @Override
    public void start() {
        // Calculate head angle offset (perpendicular to body)
        gazeYawOffset = lookingLeft ? -90.0f : 90.0f;

        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
            "Tourist {} gazing {} while riding (yaw offset: {})",
            tourist.getId(), lookingLeft ? "left" : "right", gazeYawOffset);
    }

    @Override
    public void tick() {
        gazeDuration--;

        // Calculate head angle based on vehicle travel direction
        Entity vehicle = tourist.getVehicle();
        float targetYaw;

        if (vehicle != null && vehicle.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            // Use velocity direction when moving
            Vec3 velocity = vehicle.getDeltaMovement();
            Vec3 forward = new Vec3(velocity.x, 0, velocity.z).normalize();

            // Calculate yaw from velocity (in degrees)
            float velocityYaw = (float) Math.toDegrees(Math.atan2(forward.z, forward.x)) - 90.0f;
            targetYaw = velocityYaw + gazeYawOffset;
        } else {
            // Fallback to body rotation when stationary
            targetYaw = tourist.getYRot() + gazeYawOffset;
        }

        tourist.setYHeadRot(targetYaw);
    }

    @Override
    public void stop() {
        // Reset handled by other goals
    }
}
