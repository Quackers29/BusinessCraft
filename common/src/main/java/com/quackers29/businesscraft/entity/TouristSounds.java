package com.quackers29.businesscraft.entity;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Tourist-specific sounds that differ from vanilla villager defaults.
 */
public final class TouristSounds {
    /** One-shot boarding chime (replaces minecart rolling on board). */
    public static final SoundEvent BOARDING = unwrap(SoundEvents.NOTE_BLOCK_BELL);

    private TouristSounds() {
    }

    private static SoundEvent unwrap(Holder<SoundEvent> holder) {
        return holder.value();
    }
}
