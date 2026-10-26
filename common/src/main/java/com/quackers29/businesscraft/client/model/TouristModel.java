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
 *
 * <p>Part names are indexed by {@link TouristParts}. A recolor reuses a mesh and
 * keeps its own texture index.
 */
public class TouristModel extends PlayerModel<TouristEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation("businesscraft", "tourist"), "main");

    private static final String[] HAT_NAMES = {
            "", "pith", "cap", "beret", "sunhat", "fez", "headband", "tophat",
            "hood", "boater", "flower", "ushanka", "beanie", "cap_navy", "cap_dust", "fez_plum"
    };
    private static final String[] ACCESSORY_NAMES = {
            "", "accessory_camera", "accessory_suitcase", "accessory_backpack", "accessory_map",
            "accessory_umbrella", "accessory_book", "accessory_basket", "accessory_lantern",
            "accessory_backpack_red", "accessory_suitcase_brown", "accessory_camera_brass"
    };
    private static final int[] BODY_ACCESSORIES = {
            TouristParts.ACCESSORY_CAMERA, TouristParts.ACCESSORY_BACKPACK,
            TouristParts.ACCESSORY_BACKPACK_RED, TouristParts.ACCESSORY_CAMERA_BRASS
    };
    private static final int[] LEFT_ACCESSORIES = {
            TouristParts.ACCESSORY_SUITCASE, TouristParts.ACCESSORY_UMBRELLA,
            TouristParts.ACCESSORY_BASKET, TouristParts.ACCESSORY_SUITCASE_BROWN
    };
    private static final int[] RIGHT_ACCESSORIES = {
            TouristParts.ACCESSORY_MAP, TouristParts.ACCESSORY_BOOK, TouristParts.ACCESSORY_LANTERN
    };

    private final ModelPart headwearRoot;
    private final ModelPart[] hats = new ModelPart[HAT_NAMES.length];
    private final ModelPart[] accessories = new ModelPart[ACCESSORY_NAMES.length];
    private int headwear = TouristParts.HEADWEAR_NONE;
    private int accessory = TouristParts.ACCESSORY_NONE;

    public TouristModel(ModelPart root) {
        super(root, false);
        this.headwearRoot = root.getChild("headwear");
        for (int i = 1; i < HAT_NAMES.length; i++) {
            this.hats[i] = headwearRoot.getChild(HAT_NAMES[i]);
        }
        for (int i = 1; i < ACCESSORY_NAMES.length; i++) {
            this.accessories[i] = root.getChild(ACCESSORY_NAMES[i]);
        }
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
        addCap(headwear, "cap");
        headwear.addOrReplaceChild("beret", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -10, -5, 10, 2, 10)
                .texOffs(40, 0).addBox(-1, -11, -1, 2, 1, 2), PartPose.ZERO);
        headwear.addOrReplaceChild("sunhat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -13, -3, 6, 5, 6)
                .texOffs(0, 16).addBox(-8, -9, -8, 16, 1, 16), PartPose.ZERO);
        addFez(headwear, "fez");
        headwear.addOrReplaceChild("headband", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -7, -5, 8, 2, 1)
                .texOffs(16, 0).addBox(-4, -7, 4, 8, 2, 1)
                .texOffs(0, 8).addBox(-5, -7, -4, 1, 2, 8)
                .texOffs(16, 8).addBox(4, -7, -4, 1, 2, 8), PartPose.ZERO);
        headwear.addOrReplaceChild("tophat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -17, -3, 6, 9, 6)
                .texOffs(0, 20).addBox(-6, -9, -6, 12, 1, 12), PartPose.ZERO);
        headwear.addOrReplaceChild("hood", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -10.0F, -4.5F, 9, 5, 9)
                .texOffs(0, 20).addBox(-4.5F, -5.0F, 2.5F, 9, 7, 3), PartPose.ZERO);
        headwear.addOrReplaceChild("boater", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -10, -4, 8, 3, 8)
                .texOffs(0, 16).addBox(-7, -8, -7, 14, 1, 14), PartPose.ZERO);
        headwear.addOrReplaceChild("flower", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -9, -5, 10, 2, 10)
                .texOffs(0, 24).addBox(-2, -12, -6, 3, 3, 3)
                .texOffs(20, 24).addBox(3, -11, -2, 3, 3, 3)
                .texOffs(40, 24).addBox(-6, -11, -1, 3, 3, 3)
                .texOffs(0, 34).addBox(-1, -11, 3, 3, 3, 3), PartPose.ZERO);
        headwear.addOrReplaceChild("ushanka", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -10, -4.5F, 9, 4, 9)
                .texOffs(0, 20).addBox(-6.5F, -6, -3, 2, 5, 6)
                .texOffs(20, 20).addBox(4.5F, -6, -3, 2, 5, 6)
                .texOffs(40, 20).addBox(-3, -6, 3.5F, 6, 4, 2), PartPose.ZERO);
        headwear.addOrReplaceChild("beanie", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -11, -4.5F, 9, 5, 9)
                .texOffs(40, 0).addBox(-1.5F, -13, -1.5F, 3, 3, 3), PartPose.ZERO);
        addCap(headwear, "cap_navy");
        addCap(headwear, "cap_dust");
        addFez(headwear, "fez_plum");

        addCamera(root, "accessory_camera");
        addSuitcase(root, "accessory_suitcase");
        addBackpack(root, "accessory_backpack");
        PartDefinition mapRoot = root.addOrReplaceChild("accessory_map", CubeListBuilder.create(), PartPose.ZERO);
        mapRoot.addOrReplaceChild("sheet",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, 0, -3, 8, 1, 6),
                PartPose.offsetAndRotation(0.0F, 8.0F, -2.5F, -1.2F, 0.45F, 0.2F));

        PartDefinition umbrella = root.addOrReplaceChild("accessory_umbrella",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1, -4, -1, 2, 14, 2), PartPose.ZERO);
        umbrella.addOrReplaceChild("canopy", CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-6, 0, -6, 12, 1, 12)
                        .texOffs(48, 0).addBox(-2, -2, -2, 4, 2, 4),
                PartPose.offsetAndRotation(-1.0F, -4.0F, 0.0F, 0.1F, 0.3F, 0.8F));
        PartDefinition book = root.addOrReplaceChild("accessory_book", CubeListBuilder.create(), PartPose.ZERO);
        book.addOrReplaceChild("cover", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4, 0, -3, 8, 2, 6)
                        .texOffs(0, 12).addBox(-4, 0, 2, 8, 2, 1),
                PartPose.offsetAndRotation(1.0F, 9.0F, -2.0F, -1.05F, -0.35F, 0.15F));
        root.addOrReplaceChild("accessory_basket", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, 8, -5, 6, 5, 6)
                .texOffs(24, 0).addBox(-3, 5, -2, 1, 4, 1)
                .texOffs(28, 0).addBox(2, 5, -2, 1, 4, 1)
                .texOffs(32, 0).addBox(-2, 5, -2, 4, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("accessory_lantern", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, 8, -4, 4, 5, 4)
                .texOffs(16, 0).addBox(-1, 7, -3, 2, 1, 2)
                .texOffs(16, 4).addBox(-2, 13, -4, 4, 1, 4)
                .texOffs(24, 0).addBox(-0.5F, 6, -2.5F, 1, 2, 1), PartPose.ZERO);
        addBackpack(root, "accessory_backpack_red");
        addSuitcase(root, "accessory_suitcase_brown");
        addCamera(root, "accessory_camera_brass");
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addCap(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -12, -4, 8, 4, 8)
                .texOffs(0, 16).addBox(-4, -8, -9, 8, 1, 5), PartPose.ZERO);
    }

    private static void addFez(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -14, -3, 6, 6, 6)
                .texOffs(32, 0).addBox(2, -12, -4, 1, 4, 1), PartPose.ZERO);
    }

    private static void addCamera(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, 3, -6, 4, 3, 3)
                .texOffs(0, 12).addBox(-1, 4, -7, 2, 2, 1)
                .texOffs(16, 0).addBox(-1, 0, -3, 2, 4, 1), PartPose.ZERO);
    }

    private static void addSuitcase(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, 8, -5, 4, 5, 3)
                .texOffs(24, 0).addBox(-1, 7, -4, 2, 1, 1), PartPose.ZERO);
    }

    private static void addBackpack(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, 2, 2, 10, 8, 4)
                .texOffs(32, 0).addBox(-4, 2, 6, 8, 5, 1)
                .texOffs(0, 16).addBox(-3, 1, -3, 1, 7, 1)
                .texOffs(8, 16).addBox(2, 1, -3, 1, 7, 1), PartPose.ZERO);
    }

    public void prepare(int packed) {
        this.headwear = TouristAppearance.headwear(packed);
        this.accessory = TouristAppearance.accessory(packed);
        for (int i = 1; i < hats.length; i++) {
            hats[i].visible = headwear == i;
        }
        headwearRoot.visible = headwear > TouristParts.HEADWEAR_NONE && headwear < hats.length;
        // Hair is the vanilla second head layer. Wide hats cover it; a fez, headband, or flower crown leaves it.
        hat.visible = headwear == TouristParts.HEADWEAR_NONE
                || headwear == TouristParts.HEADWEAR_FEZ
                || headwear == TouristParts.HEADWEAR_FEZ_PLUM
                || headwear == TouristParts.HEADWEAR_HEADBAND
                || headwear == TouristParts.HEADWEAR_FLOWER;
        for (int i = 1; i < accessories.length; i++) {
            accessories[i].visible = accessory == i;
        }
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
        for (int index : BODY_ACCESSORIES) {
            accessories[index].copyFrom(body);
        }
        for (int index : LEFT_ACCESSORIES) {
            accessories[index].copyFrom(leftArm);
        }
        for (int index : RIGHT_ACCESSORIES) {
            accessories[index].copyFrom(rightArm);
        }
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
        for (int i = 1; i < accessories.length; i++) {
            if (accessories[i].visible) {
                accessories[i].render(poseStack, buffer, packedLight, packedOverlay);
            }
        }
    }
}
