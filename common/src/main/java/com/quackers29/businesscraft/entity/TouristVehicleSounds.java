package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Suppresses looping vehicle rumble when only tourists are aboard; players keep vanilla audio.
 */
public final class TouristVehicleSounds {
    private TouristVehicleSounds() {
    }

    public static boolean shouldSuppress(@Nullable SoundEvent sound, @Nullable Entity source) {
        if (sound == null) {
            return false;
        }
        if (source != null && shouldSuppress(sound.getLocation(), source)) {
            return true;
        }
        return false;
    }

    public static boolean shouldSuppressAt(ResourceLocation soundId, double x, double y, double z, Level level) {
        if (!isVehicleLoop(soundId) || level == null) {
            return false;
        }
        AABB box = new AABB(x - 2, y - 2, z - 2, x + 2, y + 2, z + 2);
        for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, box)) {
            if (touristsAloneOnVehicle(cart)) {
                return true;
            }
        }
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            String name = entity.getClass().getName();
            if (name.contains("create.content.trains") && touristsAloneOnVehicle(entity)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldSuppress(ResourceLocation soundId, Entity source) {
        if (!isVehicleLoop(soundId)) {
            return false;
        }
        return touristsAloneOnVehicle(source);
    }

    private static boolean isVehicleLoop(ResourceLocation soundId) {
        if (soundId.equals(SoundEvents.MINECART_INSIDE.getLocation())
                || soundId.equals(SoundEvents.MINECART_RIDING.getLocation())) {
            return true;
        }
        if ("create".equals(soundId.getNamespace())) {
            String path = soundId.getPath();
            return path.contains("train") || path.contains("cart") || path.contains("wheel")
                    || path.contains("whistle");
        }
        return false;
    }

    private static boolean touristsAloneOnVehicle(Entity vehicle) {
        boolean hasTourist = false;
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof Player) {
                return false;
            }
            if (passenger instanceof Villager villager
                    && PlatformAccess.getTouristHelper().isTourist(villager)) {
                hasTourist = true;
            }
        }
        return hasTourist;
    }
}
