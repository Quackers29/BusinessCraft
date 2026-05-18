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
 * Rare AI goal for tourists to spot and track a distant point of interest.
 * Tracks a world position (head moves as vehicle moves).
 */
public class TouristTargetGazeGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristTargetGazeGoal.class);

    private final TouristEntity tourist;
    private int gazeDuration;
    private Vec3 targetPosition;
    private int nextGazeTick;

    private static final int MIN_GAZE_DURATION = 40; // 2 seconds
    private static final int MAX_GAZE_DURATION = 80; // 4 seconds
    private static final int MIN_GAZE_INTERVAL = 400; // 20 seconds between target gazes
    private static final int MAX_GAZE_INTERVAL = 800; // 40 seconds between target gazes
    private static final double MIN_TARGET_DISTANCE = 2.0; // Min distance to pick target
    private static final double MAX_TARGET_DISTANCE = 5.0; // Max distance to pick target

    public TouristTargetGazeGoal(TouristEntity tourist) {
        this.tourist = tourist;
        this.setFlags(EnumSet.of(Flag.LOOK));
        this.nextGazeTick = tourist.getRandom().nextInt(MIN_GAZE_INTERVAL, MAX_GAZE_INTERVAL);
    }

    @Override
    public boolean canUse() {
        // Only when riding and not gossiping
        if (!tourist.isPassenger() || tourist.isGossiping()) {
            return false;
        }

        // Check if currently gazing at target
        if (gazeDuration > 0) {
            return true;
        }

        // Check cooldown
        if (--nextGazeTick > 0) {
            return false;
        }

        // Pick random distant target
        pickNewTarget();
        nextGazeTick = tourist.getRandom().nextInt(MIN_GAZE_INTERVAL, MAX_GAZE_INTERVAL);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return tourist.isPassenger() && !tourist.isGossiping() && gazeDuration > 0;
    }

    @Override
    public void start() {
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
            "Tourist {} spotted distant target at ({}, {}, {})",
            tourist.getId(),
            String.format("%.1f", targetPosition.x),
            String.format("%.1f", targetPosition.y),
            String.format("%.1f", targetPosition.z));
    }

    @Override
    public void tick() {
        gazeDuration--;

        // Track the target position (head follows as vehicle moves)
        if (targetPosition != null) {
            tourist.getLookControl().setLookAt(targetPosition.x, targetPosition.y, targetPosition.z);
        }
    }

    @Override
    public void stop() {
        targetPosition = null;
    }

    /**
     * Pick a random point in the distance to look at.
     * Biases toward forward/side based on vehicle movement direction.
     */
    private void pickNewTarget() {
        Vec3 position = tourist.position();
        Entity vehicle = tourist.getVehicle();

        double baseAngle;

        // If vehicle moving, bias toward forward/sides based on velocity
        if (vehicle != null && vehicle.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            Vec3 velocity = vehicle.getDeltaMovement();
            Vec3 forward = new Vec3(velocity.x, 0, velocity.z).normalize();

            // Calculate forward angle from velocity
            double forwardAngle = Math.atan2(forward.z, forward.x);

            // Bias angle within 180° arc (forward ±90°)
            // This avoids looking backwards
            double angleOffset = (tourist.getRandom().nextDouble() - 0.5) * Math.PI; // ±90°
            baseAngle = forwardAngle + angleOffset;
        } else {
            // Stationary or no vehicle - random 360°
            baseAngle = tourist.getRandom().nextDouble() * Math.PI * 2;
        }

        // Random distance
        double distance = MIN_TARGET_DISTANCE +
            tourist.getRandom().nextDouble() * (MAX_TARGET_DISTANCE - MIN_TARGET_DISTANCE);

        // Calculate target position
        double x = position.x + Math.cos(baseAngle) * distance;
        double y = position.y + tourist.getEyeHeight() +
            (tourist.getRandom().nextDouble() - 0.5) * 1; // Random height variation
        double z = position.z + Math.sin(baseAngle) * distance;

        targetPosition = new Vec3(x, y, z);
        gazeDuration = tourist.getRandom().nextInt(MIN_GAZE_DURATION, MAX_GAZE_DURATION);
    }
}
