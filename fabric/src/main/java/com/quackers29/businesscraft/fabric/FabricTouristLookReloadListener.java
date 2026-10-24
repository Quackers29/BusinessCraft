package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.entity.tourist.TouristLookReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

/** Fabric id for the shared server-data tourist look listener. */
public final class FabricTouristLookReloadListener extends TouristLookReloadListener
        implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation("businesscraft", "tourist_looks");
    }
}
