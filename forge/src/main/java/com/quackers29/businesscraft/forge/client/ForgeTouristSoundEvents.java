package com.quackers29.businesscraft.forge.client;

import com.quackers29.businesscraft.entity.TouristVehicleSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only filter for tourist vehicle loop sounds (Forge).
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgeTouristSoundEvents {
    private ForgeTouristSoundEvents() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance instance = event.getSound();
        if (instance == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (TouristVehicleSounds.shouldSuppressAt(
                instance.getLocation(),
                instance.getX(),
                instance.getY(),
                instance.getZ(),
                minecraft.level)) {
            event.setCanceled(true);
        }
    }
}
