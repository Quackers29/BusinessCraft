package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.JourneyProgress;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight server-side feedback for tourist lifecycle moments.
 */
public final class TouristEffects {
    private TouristEffects() {
    }

    public static void playSpawn(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.HAPPY_VILLAGER, 6);
        burst(level, pos, ParticleTypes.END_ROD, 3);
        level.playSound(null, pos.x, pos.y, pos.z, TouristSounds.GREET, SoundSource.NEUTRAL, 0.45f, 1.15f);
    }

    public static void playBoarding(ServerLevel level, Entity tourist, Entity vehicle) {
        Vec3 pos = tourist.position();
        burst(level, pos, ParticleTypes.CLOUD, 4);
        level.playSound(null, pos.x, pos.y, pos.z, TouristSounds.BOARDING, SoundSource.NEUTRAL, 0.5f, 1.25f);
    }

    /**
     * Arrival feedback scaled by trip length. Short hops stay quiet; a Voyager-length
     * trip (and anything longer) gets the totem burst.
     */
    public static void playArrival(ServerLevel level, Vec3 pos, double distance) {
        int rank = JourneyProgress.levelForDistance(distance, ConfigLoader.touristLevelDistances);
        if (distance < ConfigLoader.minPayoutDistance) {
            burst(level, pos, ParticleTypes.SMOKE, 4);
            level.playSound(null, pos.x, pos.y, pos.z, TouristSounds.FAREWELL, SoundSource.NEUTRAL, 0.3f, 0.9f);
            return;
        }
        burst(level, pos, ParticleTypes.HAPPY_VILLAGER, 6);
        burst(level, pos, ParticleTypes.END_ROD, 4);
        if (rank >= 4) {
            burst(level, pos, ParticleTypes.TOTEM_OF_UNDYING, 8);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.45f, 1.15f);
        } else if (rank >= 3) {
            burst(level, pos, ParticleTypes.TOTEM_OF_UNDYING, 4);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.3f, 1.45f);
        } else {
            level.playSound(null, pos.x, pos.y, pos.z, TouristSounds.CELEBRATE, SoundSource.NEUTRAL, 0.35f, 1.15f);
        }
    }

    public static void playRankUp(ServerLevel level, Vec3 pos, int rank) {
        burst(level, pos, ParticleTypes.HAPPY_VILLAGER, 8);
        burst(level, pos, ParticleTypes.END_ROD, 6);
        float pitch = Math.min(1.8f, 1.2f + rank * 0.08f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.35f, pitch);
    }

    private static void burst(ServerLevel level, Vec3 pos, net.minecraft.core.particles.ParticleOptions particle,
            int count) {
        level.sendParticles(particle, pos.x, pos.y + 1.0, pos.z, count, 0.25, 0.35, 0.25, 0.02);
    }
}
