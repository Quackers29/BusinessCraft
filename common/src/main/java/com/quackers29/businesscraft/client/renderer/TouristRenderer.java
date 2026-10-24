package com.quackers29.businesscraft.client.renderer;

import com.quackers29.businesscraft.client.model.TouristModel;
import com.quackers29.businesscraft.client.renderer.layer.TouristAccessoryLayer;
import com.quackers29.businesscraft.client.renderer.layer.TouristHeadwearLayer;
import com.quackers29.businesscraft.client.renderer.layer.TouristOutfitLayer;
import com.quackers29.businesscraft.entity.TouristDisplayFormat;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.tourist.TouristAppearance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Humanoid tourist. Appearance comes from the packed look (skin, outfit, headwear,
 * accessory). Name tags are the multi-line destination / origin / type lines.
 */
public class TouristRenderer extends MobRenderer<TouristEntity, TouristModel> {
    private static final float NAME_TAG_SCALE = 0.025F;
    private static final int LINE_SPACING = 10;

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

    @Override
    protected void renderNameTag(TouristEntity entity, Component displayName, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        double distSq = minecraft.player.distanceToSqr(entity);
        if (!TouristDisplayFormat.withinNameTagRange(distSq)) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(TouristDisplayFormat.primaryLine(entity.getSyncedDestLabel(), entity.isAnyTownDestination()));
        lines.add(TouristDisplayFormat.originLine(entity.getSyncedOriginLabel()));
        Component typeAndDistance = TouristDisplayFormat.typeAndDistanceLine(
                entity.getTouristType(), entity.getTotalDistanceTraveled());
        if (typeAndDistance != null) {
            lines.add(typeAndDistance);
        }

        float scale = NAME_TAG_SCALE;
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 1.05F, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-scale, -scale, scale);

        Font font = this.getFont();
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            float lineWidth = font.width(line);
            float y = i * LINE_SPACING;
            font.drawInBatch(line, -lineWidth / 2.0F, y, 0xFFFFFF, false, matrix, buffer,
                    Font.DisplayMode.SEE_THROUGH, 0x40000000, packedLight);
            font.drawInBatch(line, -lineWidth / 2.0F, y, 0xFFFFFF, false, matrix, buffer,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }

        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(TouristEntity entity) {
        return entity.isCustomNameVisible();
    }
}
