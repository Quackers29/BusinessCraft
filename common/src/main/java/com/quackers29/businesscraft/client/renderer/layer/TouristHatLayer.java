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
 * Hat PNGs are overlay-only: opaque pixels match the villager hat UV island (around 32,0).
 */
public class TouristHatLayer extends RenderLayer<TouristEntity, VillagerModel<TouristEntity>> {
    private static final ResourceLocation[] HAT_TEXTURES = {
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat_red.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat_blue.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_hat_green.png")
    };

    private final VillagerModel<TouristEntity> model;

    public TouristHatLayer(RenderLayerParent<TouristEntity, VillagerModel<TouristEntity>> parent,
                          VillagerModel<TouristEntity> model) {
        super(parent);
        this.model = model;
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

        this.getParentModel().copyPropertiesTo(this.model);
        this.model.prepareMobModel(tourist, limbSwing, limbSwingAmount, partialTicks);
        this.model.setupAnim(tourist, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        ResourceLocation hatTexture = HAT_TEXTURES[getColorIndex(tourist)];
        var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(hatTexture));

        ModelPart hat = this.model.getHead().getChild("hat");
        hat.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        if (hat.hasChild("hat_rim")) {
            hat.getChild("hat_rim").render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        }
    }
}
