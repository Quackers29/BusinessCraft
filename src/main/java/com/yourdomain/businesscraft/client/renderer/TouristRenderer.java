package com.yourdomain.businesscraft.client.renderer;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.entity.TouristEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

public class TouristRenderer extends MobRenderer<TouristEntity, VillagerModel<TouristEntity>> {
    // Use vanilla villager texture
    private static final ResourceLocation VILLAGER_BASE_SKIN = 
        new ResourceLocation("textures/entity/villager/villager.png");
    
    // Custom hat/special features texture (if needed later)
    private static final ResourceLocation TOURIST_HAT_TEXTURE = 
        new ResourceLocation(BusinessCraft.MOD_ID, "textures/entity/tourist_hat.png");
    
    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, 
              new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 
              0.5F);
        
        // Add the standard villager layers
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        
        // Add the profession layer with type "villager" - this is what gives them their profession hats
        this.addLayer(new VillagerProfessionLayer<>(this, context.getResourceManager(), "villager"));
        
        // Add the crossed arms layer
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(TouristEntity entity) {
        // Use the vanilla villager texture
        return VILLAGER_BASE_SKIN;
    }
    
    @Override
    protected boolean isShaking(TouristEntity entity) {
        // Make tourists shake when they're about to expire (last 30 seconds)
        return entity.getExpiryTicks() < 600;
    }
} 