package com.quackers29.businesscraft.client.renderer;

// BusinessCraft moved to platform-specific module
import com.quackers29.businesscraft.client.renderer.layer.TouristHatLayer;
import com.quackers29.businesscraft.entity.TouristEntity;
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
} 
