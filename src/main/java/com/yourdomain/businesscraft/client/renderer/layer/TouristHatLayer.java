package com.yourdomain.businesscraft.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.entity.TouristEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class TouristHatLayer extends RenderLayer<TouristEntity, VillagerModel<TouristEntity>> {
    private static final ResourceLocation HAT_TEXTURE = 
        new ResourceLocation(BusinessCraft.MOD_ID, "textures/entity/tourist_hat.png");
    
    private final VillagerModel<TouristEntity> model;
    
    public TouristHatLayer(RenderLayerParent<TouristEntity, VillagerModel<TouristEntity>> parent, 
                          VillagerModel<TouristEntity> model) {
        super(parent);
        this.model = model;
    }
    
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, 
                      TouristEntity tourist, float limbSwing, float limbSwingAmount, 
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        
        // Only render hat on adults
        if (!tourist.isBaby()) {
            // Copy entity model state but only render the head
            this.getParentModel().copyPropertiesTo(this.model);
            
            // Prepare the model for rendering
            this.model.prepareMobModel(tourist, limbSwing, limbSwingAmount, partialTicks);
            this.model.setupAnim(tourist, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            
            // Push matrix state to isolate transformations
            poseStack.pushPose();
            
            // Move the hat slightly up to sit on top of head
            poseStack.translate(0.0F, -0.05F, 0.0F);
            
            // Render just the hat using our custom texture
            this.model.getHead().render(poseStack, 
                buffer.getBuffer(RenderType.entityCutoutNoCull(HAT_TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY);
            
            // Restore matrix state
            poseStack.popPose();
        }
    }
} 