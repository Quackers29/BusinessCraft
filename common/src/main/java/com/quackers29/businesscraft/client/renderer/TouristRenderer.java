package com.quackers29.businesscraft.client.renderer;

import com.quackers29.businesscraft.client.model.TouristModel;
import com.quackers29.businesscraft.client.renderer.layer.TouristAccessoryLayer;
import com.quackers29.businesscraft.client.renderer.layer.TouristHeadwearLayer;
import com.quackers29.businesscraft.client.renderer.layer.TouristOutfitLayer;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.tourist.TouristAppearance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Humanoid tourist. Name tags stay on {@link MobRenderer} so a name-tag change
 * can override {@code renderNameTag} without owning this model.
 */
public class TouristRenderer extends MobRenderer<TouristEntity, TouristModel> {
    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context, new TouristModel(context.bakeLayer(TouristModel.LAYER)), 0.5F);
        this.addLayer(new TouristOutfitLayer(this));
        this.addLayer(new TouristHeadwearLayer(this));
        this.addLayer(new TouristAccessoryLayer(this));
    }

    @Override
    public void render(TouristEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        int packed = entity.getLookPacked();
        if (packed == TouristAppearance.UNSET) {
            packed = 0;
        }
        this.model.prepare(packed);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TouristEntity entity) {
        int packed = entity.getLookPacked();
        int skin = packed == TouristAppearance.UNSET ? 0 : TouristAppearance.skin(packed);
        return TouristTextures.skin(skin);
    }

    @Override
    protected RenderType getRenderType(TouristEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        ResourceLocation texture = this.getTextureLocation(entity);
        if (!bodyVisible) {
            return glowing ? RenderType.outline(texture) : null;
        }
        if (translucent) {
            return RenderType.itemEntityTranslucentCull(texture);
        }
        return RenderType.entityCutoutNoCull(texture);
    }
}
