package com.quackers29.businesscraft.client.renderer;

import com.quackers29.businesscraft.client.renderer.layer.TouristHatLayer;
import com.quackers29.businesscraft.entity.TouristDisplayFormat;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class TouristRenderer extends MobRenderer<TouristEntity, VillagerModel<TouristEntity>> {
    private static final ResourceLocation[] SKIN_TEXTURES = {
        new ResourceLocation("businesscraft", "textures/entity/tourist_basic.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_experienced.png"),
        new ResourceLocation("businesscraft", "textures/entity/tourist_luxury.png"),
    };
    private static final ResourceLocation FALLBACK_SKIN =
        new ResourceLocation("textures/entity/villager/villager.png");

    public TouristRenderer(EntityRendererProvider.Context context) {
        super(context,
              new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)),
              0.5F);
        // No VillagerProfessionLayer — tourists use Profession.NONE and custom body textures.
        this.addLayer(new TouristHatLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TouristEntity entity) {
        int tier = entity.getSkinTier();
        if (tier >= 0 && tier < SKIN_TEXTURES.length) {
            return SKIN_TEXTURES[tier];
        }
        return FALLBACK_SKIN;
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

        Component primary = TouristDisplayFormat.primaryLine(entity.getSyncedDestLabel(), entity.isAnyTownDestination());
        Component secondary = TouristDisplayFormat.secondaryLine(
                entity.getSyncedOriginLabel(),
                entity.getTouristType(),
                entity.getTotalDistanceTraveled());

        float scale = 0.025F;
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.65F, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        var font = this.getFont();
        float primaryWidth = font.width(primary);
        font.drawInBatch(primary, -primaryWidth / 2.0F, 0, 0xFFFFFF, false, matrix, buffer,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0x40000000, packedLight);
        float secondaryWidth = font.width(secondary);
        font.drawInBatch(secondary, -secondaryWidth / 2.0F, 10, 0xFFFFFF, false, matrix, buffer,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0x40000000, packedLight);
        font.drawInBatch(primary, -primaryWidth / 2.0F, 0, 0xFFFFFF, false, matrix, buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
        font.drawInBatch(secondary, -secondaryWidth / 2.0F, 10, 0xFFFFFF, false, matrix, buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(TouristEntity entity) {
        return entity.isCustomNameVisible();
    }
}
