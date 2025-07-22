package com.yourdomain.businesscraft.client.renderer;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.client.renderer.layer.TouristHatLayer;
import com.yourdomain.businesscraft.entity.TouristEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

public class TouristRenderer extends MobRenderer<TouristEntity, VillagerModel<TouristEntity>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN = 
        new ResourceLocation("textures/entity/villager/villager.png");
    
    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, 
              new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 
              0.5F);
        
        // Add profession layer for base unemployed villager clothing
        this.addLayer(new VillagerProfessionLayer<>(this, context.getResourceManager(), "villager"));
        
        // Add tourist hat layer
        this.addLayer(new TouristHatLayer(this, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER))));
    }
    
    @Override
    public ResourceLocation getTextureLocation(TouristEntity entity) {
        return VILLAGER_BASE_SKIN;
    }
    
    @Override
    protected boolean isShaking(TouristEntity entity) {
        // Make tourists shake when they're about to expire (last 30 seconds)
        return entity.getExpiryTicks() < 600;
    }
} 