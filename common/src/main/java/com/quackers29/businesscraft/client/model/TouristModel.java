package com.quackers29.businesscraft.client.model;

import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.tourist.TouristAppearance;
import com.quackers29.businesscraft.entity.tourist.TouristParts;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Wide-arm humanoid with a clothing overlay plus swappable headwear and accessories.
 * Skin and outfit use the vanilla player UV on 64×64 textures. Hats and accessories
 * are separate parts with their own textures; UVs are listed on each box.
 */
public class TouristModel extends PlayerModel<TouristEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation("businesscraft", "tourist"), "main");

    private final ModelPart headwearRoot;
    private final ModelPart pith;
    private final ModelPart cap;
    private final ModelPart beret;
    private final ModelPart sunhat;
    private final ModelPart fez;
    private final ModelPart headband;
    private final ModelPart tophat;
    private final ModelPart camera;
    private final ModelPart suitcase;
    private final ModelPart backpack;
    private final ModelPart map;
    private int headwear = TouristParts.HEADWEAR_NONE;
    private int accessory = TouristParts.ACCESSORY_NONE;

    public TouristModel(ModelPart root) {
        super(root, false);
        this.headwearRoot = root.getChild("headwear");
        this.pith = headwearRoot.getChild("pith");
        this.cap = headwearRoot.getChild("cap");
        this.beret = headwearRoot.getChild("beret");
        this.sunhat = headwearRoot.getChild("sunhat");
        this.fez = headwearRoot.getChild("fez");
        this.headband = headwearRoot.getChild("headband");
        this.tophat = headwearRoot.getChild("tophat");
        this.camera = root.getChild("accessory_camera");
        this.suitcase = root.getChild("accessory_suitcase");
        this.backpack = root.getChild("accessory_backpack");
        this.map = root.getChild("accessory_map");
        root.getChild("ear").visible = false;
        root.getChild("cloak").visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot();
        PartDefinition headwear = root.addOrReplaceChild("headwear", CubeListBuilder.create(), PartPose.ZERO);
        headwear.addOrReplaceChild("pith", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -14, -5, 10, 5, 10)
                .texOffs(0, 20).addBox(-7, -9, -7, 14, 1, 14), PartPose.ZERO);
        headwear.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -12, -4, 8, 4, 8)
                .texOffs(0, 16).addBox(-4, -8, -9, 8, 1, 5), PartPose.ZERO);
        headwear.addOrReplaceChild("beret", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -10, -5, 10, 2, 10)
                .texOffs(40, 0).addBox(-1, -11, -1, 2, 1, 2), PartPose.ZERO);
        headwear.addOrReplaceChild("sunhat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -13, -3, 6, 5, 6)
                .texOffs(0, 16).addBox(-8, -9, -8, 16, 1, 16), PartPose.ZERO);
        headwear.addOrReplaceChild("fez", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -14, -3, 6, 6, 6)
                .texOffs(32, 0).addBox(2, -12, -4, 1, 4, 1), PartPose.ZERO);
        headwear.addOrReplaceChild("headband", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -7, -5, 8, 2, 1)
                .texOffs(16, 0).addBox(-4, -7, 4, 8, 2, 1)
                .texOffs(0, 8).addBox(-5, -7, -4, 1, 2, 8)
                .texOffs(16, 8).addBox(4, -7, -4, 1, 2, 8), PartPose.ZERO);
        headwear.addOrReplaceChild("tophat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -17, -3, 6, 9, 6)
                .texOffs(0, 20).addBox(-6, -9, -6, 12, 1, 12), PartPose.ZERO);

        root.addOrReplaceChild("accessory_camera", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, 3, -6, 4, 3, 3)
                .texOffs(0, 12).addBox(-1, 4, -7, 2, 2, 1)
                .texOffs(16, 0).addBox(-1, 0, -3, 2, 4, 1), PartPose.ZERO);
        root.addOrReplaceChild("accessory_suitcase", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, 8, -5, 4, 5, 3)
                .texOffs(24, 0).addBox(-1, 7, -4, 2, 1, 1), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("accessory_backpack", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, 2, 3, 6, 8, 3)
                .texOffs(24, 0).addBox(-2, 2, 6, 4, 4, 1)
                .texOffs(32, 0).addBox(-4, 1, 0, 1, 7, 3)
                .texOffs(40, 0).addBox(3, 1, 0, 1, 7, 3), PartPose.ZERO);
        PartDefinition mapRoot = root.addOrReplaceChild("accessory_map", CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        mapRoot.addOrReplaceChild("sheet",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3, 0, -2, 6, 1, 4),
                PartPose.offsetAndRotation(0.0F, 9.0F, -1.0F, -0.95F, 0.25F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public void prepare(int packed) {
        this.headwear = TouristAppearance.headwear(packed);
        this.accessory = TouristAppearance.accessory(packed);
        pith.visible = headwear == TouristParts.HEADWEAR_PITH;
        cap.visible = headwear == TouristParts.HEADWEAR_CAP;
        beret.visible = headwear == TouristParts.HEADWEAR_BERET;
        sunhat.visible = headwear == TouristParts.HEADWEAR_SUNHAT;
        fez.visible = headwear == TouristParts.HEADWEAR_FEZ;
        headband.visible = headwear == TouristParts.HEADWEAR_HEADBAND;
        tophat.visible = headwear == TouristParts.HEADWEAR_TOPHAT;
        headwearRoot.visible = TouristParts.knownHeadwear(headwear) && headwear != TouristParts.HEADWEAR_NONE;
        // Hair is the vanilla second head layer. Wide hats cover it; a fez or headband leaves it showing.
        hat.visible = headwear == TouristParts.HEADWEAR_NONE
                || headwear == TouristParts.HEADWEAR_FEZ
                || headwear == TouristParts.HEADWEAR_HEADBAND;
        camera.visible = accessory == TouristParts.ACCESSORY_CAMERA;
        suitcase.visible = accessory == TouristParts.ACCESSORY_SUITCASE;
        backpack.visible = accessory == TouristParts.ACCESSORY_BACKPACK;
        map.visible = accessory == TouristParts.ACCESSORY_MAP;
    }

    public int headwearIndex() {
        return headwear;
    }

    public int accessoryIndex() {
        return accessory;
    }

    @Override
    public void setupAnim(TouristEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        headwearRoot.copyFrom(head);
        camera.copyFrom(body);
        backpack.copyFrom(body);
        suitcase.copyFrom(leftArm);
        map.copyFrom(rightArm);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        boolean jacketWas = jacket.visible;
        boolean leftSleeveWas = leftSleeve.visible;
        boolean rightSleeveWas = rightSleeve.visible;
        boolean leftPantsWas = leftPants.visible;
        boolean rightPantsWas = rightPants.visible;
        jacket.visible = false;
        leftSleeve.visible = false;
        rightSleeve.visible = false;
        leftPants.visible = false;
        rightPants.visible = false;
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        jacket.visible = jacketWas;
        leftSleeve.visible = leftSleeveWas;
        rightSleeve.visible = rightSleeveWas;
        leftPants.visible = leftPantsWas;
        rightPants.visible = rightPantsWas;
    }

    public void renderOutfit(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        jacket.visible = true;
        leftSleeve.visible = true;
        rightSleeve.visible = true;
        leftPants.visible = true;
        rightPants.visible = true;
        jacket.render(poseStack, buffer, packedLight, packedOverlay);
        rightSleeve.render(poseStack, buffer, packedLight, packedOverlay);
        leftSleeve.render(poseStack, buffer, packedLight, packedOverlay);
        rightPants.render(poseStack, buffer, packedLight, packedOverlay);
        leftPants.render(poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderHeadwear(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        if (headwearRoot.visible) {
            headwearRoot.render(poseStack, buffer, packedLight, packedOverlay);
        }
    }

    public void renderAccessory(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        if (camera.visible) {
            camera.render(poseStack, buffer, packedLight, packedOverlay);
        }
        if (suitcase.visible) {
            suitcase.render(poseStack, buffer, packedLight, packedOverlay);
        }
        if (backpack.visible) {
            backpack.render(poseStack, buffer, packedLight, packedOverlay);
        }
        if (map.visible) {
            map.render(poseStack, buffer, packedLight, packedOverlay);
        }
    }
}
