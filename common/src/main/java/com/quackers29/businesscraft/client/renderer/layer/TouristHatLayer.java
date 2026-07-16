package com.quackers29.businesscraft.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quackers29.businesscraft.entity.TouristEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the villager {@code hat} (+ {@code hat_rim}) with a tourist hat texture.
 * Applies the parent head transform so the hat follows gaze / look yaw-pitch.
 */
public class TouristHatLayer extends RenderLayer<TouristEntity, VillagerModel<TouristEntity>> {
    private static final ResourceLocation[] HAT_TEXTURES = {
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat_red.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat_blue.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat_green.png")
    };

    public TouristHatLayer(RenderLayerParent<TouristEntity, VillagerModel<TouristEntity>> parent) {
        super(parent);
    }

    /** Level 1→red, 2→blue, 3→green. */
    private int getColorIndex(TouristEntity tourist) {
        int level = tourist.getVillagerData().getLevel();
        return Math.min(Math.max(level, 1), 3);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                      TouristEntity tourist, float limbSwing, float limbSwingAmount,
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (tourist.isBaby()) {
            return;
        }

        // Parent model is already animated for this frame — use its head pose.
        ModelPart head = this.getParentModel().getHead();
        ModelPart hat = head.getChild("hat");

        ResourceLocation hatTexture = HAT_TEXTURES[getColorIndex(tourist)];
        var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(hatTexture));

        poseStack.pushPose();
        // Critical: hat is a child of head; rendering hat alone skips head yaw/pitch.
        head.translateAndRotate(poseStack);
        hat.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
