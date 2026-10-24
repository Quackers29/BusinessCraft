package com.quackers29.businesscraft.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quackers29.businesscraft.client.model.TouristModel;
import com.quackers29.businesscraft.client.renderer.TouristTextures;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.tourist.TouristAppearance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Clothing overlay. One draw, parts already posed by the player model. */
public class TouristOutfitLayer extends RenderLayer<TouristEntity, TouristModel> {
    public TouristOutfitLayer(RenderLayerParent<TouristEntity, TouristModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TouristEntity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
            float headPitch) {
        int packed = entity.getLookPacked();
        int outfit = packed == TouristAppearance.UNSET ? 0 : TouristAppearance.outfit(packed);
        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TouristTextures.outfit(outfit)));
        this.getParentModel().renderOutfit(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
