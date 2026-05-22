package com.quackers29.businesscraft.client.renderer;

import com.quackers29.businesscraft.client.renderer.layer.TouristHatLayer;
import com.quackers29.businesscraft.entity.TouristEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

public class TouristRenderer extends MobRenderer<TouristEntity, VillagerModel<TouristEntity>> {
    private static final ResourceLocation[] SKIN_TEXTURES = {
        new ResourceLocation("businesscraft", "textures/entity/tourist_basic.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_experienced.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_luxury.png"),
    };
    private static final ResourceLocation FALLBACK_SKIN =
        new ResourceLocation("textures/entity/villager/villager.png");
    
    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, 
              new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 
              0.5F);
        
        this.addLayer(new VillagerProfessionLayer<>(this, context.getResourceManager(), "villager"));

        this.addLayer(new TouristHatLayer(this, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER))));
    }
    
    @Override
    public ResourceLocation getTextureLocation(TouristEntity entity) {
        int tier = entity.getSkinTier();
        if (tier >= 0 && tier < SKIN_TEXTURES.length) {
            return SKIN_TEXTURES[tier];
        }
        return FALLBACK_SKIN;
    }
} 
