package com.quackers29.businesscraft.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quackers29.businesscraft.client.model.TouristModel;
import com.quackers29.businesscraft.client.renderer.TouristTextures;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.tourist.TouristParts;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Camera, suitcase, backpack, or map. Index 0 draws nothing. */
public class TouristAccessoryLayer extends RenderLayer<TouristEntity, TouristModel> {
    public TouristAccessoryLayer(RenderLayerParent<TouristEntity, TouristModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TouristEntity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
            float headPitch) {
        int accessory = this.getParentModel().accessoryIndex();
        if (accessory == TouristParts.ACCESSORY_NONE || !TouristParts.knownAccessory(accessory)) {
            return;
        }
        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TouristTextures.accessory(accessory)));
        this.getParentModel().renderAccessory(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
