package com.quackers29.businesscraft.entity;

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
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.55f, 1.15f);
    }

    public static void playBoarding(ServerLevel level, Entity tourist, Entity vehicle) {
        Vec3 pos = tourist.position();
        burst(level, pos, ParticleTypes.CLOUD, 4);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.MINECART_INSIDE, SoundSource.NEUTRAL, 0.35f, 1.25f);
    }

    public static void playArrival(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.TOTEM_OF_UNDYING, 8);
        burst(level, pos, ParticleTypes.HAPPY_VILLAGER, 4);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.35f, 1.6f);
    }

    private static void burst(ServerLevel level, Vec3 pos, net.minecraft.core.particles.ParticleOptions particle,
            int count) {
        level.sendParticles(particle, pos.x, pos.y + 1.0, pos.z, count, 0.25, 0.35, 0.25, 0.02);
    }
}
