package com.yourdomain.businesscraft.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.npc.Villager;

public class NoHatProfessionLayer<T extends Villager, M extends VillagerModel<T>> extends VillagerProfessionLayer<T, M> {
    
    public NoHatProfessionLayer(RenderLayerParent<T, M> parent, ResourceManager resourceManager, String professionName) {
        super(parent, resourceManager, professionName);
    }
    
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, 
                      T villager, float limbSwing, float limbSwingAmount, 
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        
        // Only render if the villager is not a baby (same logic as parent)
        if (!villager.isBaby()) {
            // We need to render the profession overlay but skip the hat parts
            // Since we can't easily modify the parent's behavior, we'll override the entire render method
            // but exclude any hat-related rendering by not calling the parent render method
            
            // For now, we'll skip profession rendering entirely since our custom hat layer handles the visual
            // This prevents conflicts between the profession hat and our custom tourist hat
            // If you want profession-based clothing but no hat, you'd need to implement custom profession rendering here
        }
    }
}