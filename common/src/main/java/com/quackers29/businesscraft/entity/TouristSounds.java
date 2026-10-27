package com.quackers29.businesscraft.entity;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

/**
 * Non-villager audio palette for tourists. Pitch varies slightly per entity id so
 * groups do not sound identical.
 */
public final class TouristSounds {
    /** Soft boarding chime (minecart / Create seat). */
    public static final SoundEvent BOARDING = unwrap(SoundEvents.NOTE_BLOCK_BELL);
    /** Rare ambient while gossiping. */
    public static final SoundEvent AMBIENT = SoundEvents.AMETHYST_BLOCK_CHIME;
    /** Light hurt feedback. */
    public static final SoundEvent HURT = unwrap(SoundEvents.NOTE_BLOCK_HARP);
    /** Death / removal. */
    public static final SoundEvent DEATH = unwrap(SoundEvents.NOTE_BLOCK_BASS);
    /** Trade screen open. */
    public static final SoundEvent GREET = unwrap(SoundEvents.NOTE_BLOCK_PLING);
    /** Trade screen close. */
    public static final SoundEvent FAREWELL = unwrap(SoundEvents.NOTE_BLOCK_FLUTE);
    /** Fast-ride reaction. */
    public static final SoundEvent CELEBRATE = SoundEvents.EXPERIENCE_ORB_PICKUP;

    private static SoundEvent unwrap(Holder<SoundEvent> holder) {
        return holder.value();
    }

    private TouristSounds() {
    }

    public static float pitch(Entity entity, float base) {
        int spread = Math.abs(entity.getId()) % 5;
        return base + spread * 0.04f;
    }

    public static void play(Entity entity, SoundEvent sound, float volume, float basePitch) {
        entity.playSound(sound, volume, pitch(entity, basePitch));
    }
}
