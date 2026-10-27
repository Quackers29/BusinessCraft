package com.quackers29.businesscraft.entity.ai.goal;

import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * AI goal for tourists to gossip with each other.
 * Searches for nearby tourists and makes them look at each other while playing ambient sounds.
 */
public class TouristGossipGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristGossipGoal.class);

    private final TouristEntity tourist;
    private TouristEntity gossipPartner;
    private int gossipDuration;
    private int nextSearchTick;

    private static final double SEARCH_RADIUS = 4.0;
    private static final int MIN_GOSSIP_DURATION = 60; // 3 seconds
    private static final int MAX_GOSSIP_DURATION = 80; // 4 seconds
    private static final int MIN_SEARCH_INTERVAL = 40; // 2 seconds
    private static final int MAX_SEARCH_INTERVAL = 80; // 4 seconds
    private static final float SOUND_CHANCE = 0.0f; // 5% chance per tick

    public TouristGossipGoal(TouristEntity tourist) {
        this.tourist = tourist;
        this.setFlags(EnumSet.of(Flag.LOOK));
        this.nextSearchTick = tourist.getRandom().nextInt(MIN_SEARCH_INTERVAL, MAX_SEARCH_INTERVAL);
    }

    @Override
    public boolean canUse() {
        // Don't gossip if already has partner
        if (gossipPartner != null && gossipPartner.isAlive()) {
            return true;
        }

        // Only search periodically
        if (--nextSearchTick > 0) {
            return false;
        }

        // Reset search timer
        nextSearchTick = tourist.getRandom().nextInt(MIN_SEARCH_INTERVAL, MAX_SEARCH_INTERVAL);

        // Search for nearby tourists
        AABB searchBox = new AABB(
            tourist.getX() - SEARCH_RADIUS, tourist.getY() - 2, tourist.getZ() - SEARCH_RADIUS,
            tourist.getX() + SEARCH_RADIUS, tourist.getY() + 2, tourist.getZ() + SEARCH_RADIUS
        );

        List<TouristEntity> nearbyTourists = tourist.level().getEntitiesOfClass(
            TouristEntity.class,
            searchBox,
            other -> other != tourist && other.isAlive() && !other.isGossiping()
        );

        if (!nearbyTourists.isEmpty()) {
            gossipPartner = nearbyTourists.get(tourist.getRandom().nextInt(nearbyTourists.size()));
            gossipDuration = tourist.getRandom().nextInt(MIN_GOSSIP_DURATION, MAX_GOSSIP_DURATION);
            tourist.setGossipPartner(gossipPartner);

            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
                "Tourist {} started gossiping with {} for {} ticks",
                tourist.getId(), gossipPartner.getId(), gossipDuration);

            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return gossipPartner != null
            && gossipPartner.isAlive()
            && gossipDuration > 0
            && tourist.distanceToSqr(gossipPartner) < (SEARCH_RADIUS * SEARCH_RADIUS);
    }

    @Override
    public void start() {
        if (gossipPartner != null) {
            tourist.getLookControl().setLookAt(gossipPartner, 30.0f, 30.0f);
        }
    }

    @Override
    public void tick() {
        if (gossipPartner == null) {
            return;
        }

        gossipDuration--;

        // Keep looking at partner
        tourist.getLookControl().setLookAt(gossipPartner, 30.0f, 30.0f);

        // Occasionally play ambient villager sounds
        if (tourist.getRandom().nextFloat() < SOUND_CHANCE) {
            tourist.playSound(SoundEvents.VILLAGER_AMBIENT, 0.6f, 1.0f);
        }
    }

    @Override
    public void stop() {
        gossipPartner = null;
        tourist.setGossipPartner(null);
        DebugConfig.debug(LOGGER, DebugConfig.TOURIST_ENTITY,
            "Tourist {} stopped gossiping", tourist.getId());
    }
}
