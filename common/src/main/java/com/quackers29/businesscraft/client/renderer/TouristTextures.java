package com.quackers29.businesscraft.client.renderer;

import net.minecraft.resources.ResourceLocation;

/** 64×64 textures. Index 0 headwear and accessory have no texture (those parts are not drawn). */
public final class TouristTextures {
    private TouristTextures() {
    }

    public static ResourceLocation skin(int index) {
        return texture("skin_" + index);
    }

    public static ResourceLocation outfit(int index) {
        return texture("outfit_" + index);
    }

    public static ResourceLocation headwear(int index) {
        return texture("headwear_" + index);
    }

    public static ResourceLocation accessory(int index) {
        return texture("accessory_" + index);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("businesscraft", "textures/entity/tourist/" + name + ".png");
    }
}
