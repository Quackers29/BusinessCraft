package com.quackers29.businesscraft.client.renderer;

import com.quackers29.businesscraft.client.model.TouristModel;
import com.quackers29.businesscraft.client.renderer.layer.TouristAccessoryLayer;
import com.quackers29.businesscraft.client.renderer.layer.TouristHeadwearLayer;
import com.quackers29.businesscraft.client.renderer.layer.TouristOutfitLayer;
import com.quackers29.businesscraft.entity.TouristDisplayFormat;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristNames;
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

/**
 * Humanoid tourist. Appearance comes from the packed look (skin, outfit, headwear,
 * accessory). The only floating text is the first name, and only up close.
 * The full name stays on the tourist screen. The name is drawn see-through and
 * depth-tested, so it can show through walls.
 */
public class TouristRenderer extends MobRenderer<TouristEntity, TouristModel> {
    private static final float NAME_TAG_SCALE = 0.02F;
    /** Light grey, slightly transparent. No background is drawn with this. */
    private static final int NAME_COLOR = 0xD0D8D8D8;

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
        String personal = entity.getPersonalName();
        if (TouristNames.isPlaceholder(personal)) {
            return;
        }
        String first = TouristNames.firstOf(personal);
        if (first.isBlank()) {
            return;
        }
        Component line = Component.literal(first);
        float scale = NAME_TAG_SCALE;
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.62F, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-scale, -scale, scale);
        Font font = this.getFont();
        Matrix4f matrix = poseStack.last().pose();
        float x = -font.width(line) / 2.0F;
        // See-through first, then the depth-tested pass, the same order as a vanilla nameplate.
        font.drawInBatch(line, x, 0.0F, NAME_COLOR, false, matrix, buffer,
                Font.DisplayMode.SEE_THROUGH, 0, packedLight);
        font.drawInBatch(line, x, 0.0F, NAME_COLOR, false, matrix, buffer,
                Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(TouristEntity entity) {
        return entity.isCustomNameVisible();
    }
}
