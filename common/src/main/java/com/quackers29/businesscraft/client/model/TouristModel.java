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
            "hood", "boater", "flower", "ushanka", "beanie", "cap_navy", "cap_dust", "fez_plum",
            "visor", "bucket", "turban", "helmet", "fedora",
            "cap_scarlet", "sunhat_sand", "beanie_plum", "beret_ink", "boater_cream",
            "hood_slate", "ushanka_grey", "headband_rose", "pith_olive", "cap_olive", "beret_crimson"
    };
    /** Hair overlay stays visible for these hats. Wide crowns hide it. Indexed with {@link #HAT_NAMES}. */
    private static final boolean[] SHOW_HAIR = new boolean[HAT_NAMES.length];
    private static final String[] ACCESSORY_NAMES = {
            "", "accessory_camera", "accessory_suitcase", "accessory_backpack", "accessory_map",
            "", "accessory_book", "accessory_basket", "accessory_lantern",
            "accessory_backpack_red", "accessory_suitcase_brown", "accessory_camera_brass",
            "accessory_binoculars", "accessory_briefcase", "accessory_canteen", "accessory_newspaper",
            "accessory_tote", "accessory_book_navy", "accessory_backpack_olive", "accessory_basket_apple",
            "accessory_lantern_copper"
    };
    private static final int[] BODY_ACCESSORIES = {
            TouristParts.ACCESSORY_CAMERA, TouristParts.ACCESSORY_BACKPACK,
            TouristParts.ACCESSORY_BACKPACK_RED, TouristParts.ACCESSORY_CAMERA_BRASS,
            TouristParts.ACCESSORY_BACKPACK_OLIVE
    };
    private static final int[] LEFT_ACCESSORIES = {
            TouristParts.ACCESSORY_SUITCASE, TouristParts.ACCESSORY_BASKET,
            TouristParts.ACCESSORY_SUITCASE_BROWN, TouristParts.ACCESSORY_BRIEFCASE,
            TouristParts.ACCESSORY_TOTE, TouristParts.ACCESSORY_BASKET_APPLE
    };
    private static final int[] RIGHT_ACCESSORIES = {
            TouristParts.ACCESSORY_MAP, TouristParts.ACCESSORY_BOOK, TouristParts.ACCESSORY_LANTERN,
            TouristParts.ACCESSORY_BINOCULARS, TouristParts.ACCESSORY_CANTEEN,
            TouristParts.ACCESSORY_NEWSPAPER, TouristParts.ACCESSORY_BOOK_NAVY,
            TouristParts.ACCESSORY_LANTERN_COPPER
    };

    static {
        int[] showHair = {
                TouristParts.HEADWEAR_NONE,
                TouristParts.HEADWEAR_FEZ,
                TouristParts.HEADWEAR_FEZ_PLUM,
                TouristParts.HEADWEAR_HEADBAND,
                TouristParts.HEADWEAR_FLOWER,
                TouristParts.HEADWEAR_VISOR,
                TouristParts.HEADWEAR_HEADBAND_ROSE
        };
        for (int index : showHair) {
            SHOW_HAIR[index] = true;
        }
    }

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
            if (ACCESSORY_NAMES[i].isEmpty()) {
                continue; // retired index, kept so other accessory indices stay stable
            }
            this.accessories[i] = root.getChild(ACCESSORY_NAMES[i]);
        }
        root.getChild("ear").visible = false;
        root.getChild("cloak").visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot();
        PartDefinition headwear = root.addOrReplaceChild("headwear", CubeListBuilder.create(), PartPose.ZERO);
        addPith(headwear, "pith");
        addCap(headwear, "cap");
        addBeret(headwear, "beret");
        addSunhat(headwear, "sunhat");
        addFez(headwear, "fez");
        addHeadband(headwear, "headband");
        addTophat(headwear, "tophat");
        addHood(headwear, "hood");
        addBoater(headwear, "boater");
        addFlower(headwear, "flower");
        addUshanka(headwear, "ushanka");
        addBeanie(headwear, "beanie");
        addCap(headwear, "cap_navy");
        addCap(headwear, "cap_dust");
        addFez(headwear, "fez_plum");
        addVisor(headwear, "visor");
        addBucket(headwear, "bucket");
        addTurban(headwear, "turban");
        addHelmet(headwear, "helmet");
        addFedora(headwear, "fedora");
        addCap(headwear, "cap_scarlet");
        addSunhat(headwear, "sunhat_sand");
        addBeanie(headwear, "beanie_plum");
        addBeret(headwear, "beret_ink");
        addBoater(headwear, "boater_cream");
        addHood(headwear, "hood_slate");
        addUshanka(headwear, "ushanka_grey");
        addHeadband(headwear, "headband_rose");
        addPith(headwear, "pith_olive");
        addCap(headwear, "cap_olive");
        addBeret(headwear, "beret_crimson");

        addCamera(root, "accessory_camera");
        addSuitcase(root, "accessory_suitcase");
        addBackpack(root, "accessory_backpack");
        addMap(root, "accessory_map");
        addBook(root, "accessory_book");
        addBasket(root, "accessory_basket");
        addLantern(root, "accessory_lantern");
        addBackpack(root, "accessory_backpack_red");
        addSuitcase(root, "accessory_suitcase_brown");
        addCamera(root, "accessory_camera_brass");
        addBinoculars(root, "accessory_binoculars");
        addBriefcase(root, "accessory_briefcase");
        addCanteen(root, "accessory_canteen");
        addNewspaper(root, "accessory_newspaper");
        addTote(root, "accessory_tote");
        addBook(root, "accessory_book_navy");
        addBackpack(root, "accessory_backpack_olive");
        addBasket(root, "accessory_basket_apple");
        addLantern(root, "accessory_lantern_copper");
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addPith(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -14, -5, 10, 5, 10)
                .texOffs(0, 20).addBox(-7, -9, -7, 14, 1, 14), PartPose.ZERO);
    }

    private static void addCap(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -12, -4, 8, 4, 8)
                .texOffs(0, 16).addBox(-4, -8, -9, 8, 1, 5), PartPose.ZERO);
    }

    private static void addBeret(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -10, -5, 10, 2, 10)
                .texOffs(40, 0).addBox(-1, -11, -1, 2, 1, 2), PartPose.ZERO);
    }

    private static void addSunhat(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -13, -3, 6, 5, 6)
                .texOffs(0, 16).addBox(-8, -9, -8, 16, 1, 16), PartPose.ZERO);
    }

    private static void addFez(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -14, -3, 6, 6, 6)
                .texOffs(32, 0).addBox(2, -12, -4, 1, 4, 1), PartPose.ZERO);
    }

    private static void addHeadband(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -7, -5, 8, 2, 1)
                .texOffs(16, 0).addBox(-4, -7, 4, 8, 2, 1)
                .texOffs(0, 8).addBox(-5, -7, -4, 1, 2, 8)
                .texOffs(16, 8).addBox(4, -7, -4, 1, 2, 8), PartPose.ZERO);
    }

    private static void addTophat(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -17, -3, 6, 9, 6)
                .texOffs(0, 20).addBox(-6, -9, -6, 12, 1, 12), PartPose.ZERO);
    }

    private static void addHood(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -10.0F, -4.5F, 9, 5, 9)
                .texOffs(0, 20).addBox(-3.0F, -9.0F, 4.5F, 6, 3.5F, 1), PartPose.ZERO);
    }

    private static void addBoater(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -10, -4, 8, 3, 8)
                .texOffs(0, 16).addBox(-7, -8, -7, 14, 1, 14), PartPose.ZERO);
    }

    private static void addFlower(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -9, -5, 10, 2, 10)
                .texOffs(0, 24).addBox(-2, -12, -6, 3, 3, 3)
                .texOffs(20, 24).addBox(3, -11, -2, 3, 3, 3)
                .texOffs(40, 24).addBox(-6, -11, -1, 3, 3, 3)
                .texOffs(0, 34).addBox(-1, -11, 3, 3, 3, 3), PartPose.ZERO);
    }

    private static void addUshanka(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -10, -4.5F, 9, 4, 9)
                .texOffs(0, 20).addBox(-6.5F, -6, -2.5F, 2, 3, 5)
                .texOffs(20, 20).addBox(4.5F, -6, -2.5F, 2, 3, 5)
                .texOffs(40, 20).addBox(-3, -6, 3.5F, 6, 2, 1.5F), PartPose.ZERO);
    }

    private static void addBeanie(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -11, -4.5F, 9, 5, 9)
                .texOffs(40, 0).addBox(-1.5F, -13, -1.5F, 3, 3, 3), PartPose.ZERO);
    }

    /** Band on the upper head plus the cap's forward brim. Hair stays visible. */
    private static void addVisor(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 2, 8)
                .texOffs(0, 16).addBox(-4, -8, -9, 8, 1, 5), PartPose.ZERO);
    }

    /** Short crown and a brim smaller than the boater, sitting on the top of the head. */
    private static void addBucket(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -12, -4, 8, 4, 8)
                .texOffs(0, 16).addBox(-6, -8, -6, 12, 1, 12), PartPose.ZERO);
    }

    /** Wrapped bands on the upper head, the same footprint as the flower crown's base. */
    private static void addTurban(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -9, -5, 10, 2, 10)
                .texOffs(0, 16).addBox(-4, -11, -4, 8, 2, 8)
                .texOffs(32, 0).addBox(-2, -13, -2, 4, 2, 4), PartPose.ZERO);
    }

    /** Shell on the crown and a short brim. The brim stops short of the cap's. */
    private static void addHelmet(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -12, -4, 8, 4, 8)
                .texOffs(0, 16).addBox(-4, -8, -7, 8, 1, 3), PartPose.ZERO);
    }

    /** Narrow crown and a brim no wider than the boater's. */
    private static void addFedora(PartDefinition headwear, String name) {
        headwear.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -13, -3, 6, 5, 6)
                .texOffs(0, 20).addBox(-6, -8, -6, 12, 1, 12), PartPose.ZERO);
    }

    private static void addCamera(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, 5, -5, 4, 3, 3)
                .texOffs(0, 12).addBox(-1, 6, -6, 2, 2, 1)
                .texOffs(16, 0).addBox(-1, 3, -3, 2, 2, 1), PartPose.ZERO);
    }

    private static void addSuitcase(PartDefinition root, String name) {
        // Handle buried in the left fist (arm-local y 8–10). The case hangs below it.
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, 9.5F, -4.0F, 4, 5, 3)
                .texOffs(24, 0).addBox(0.0F, 8.5F, -1.6F, 2, 1, 1), PartPose.ZERO);
    }

    private static void addBackpack(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, 3, 2, 8, 6, 4)
                .texOffs(32, 0).addBox(-3, 3, 6, 6, 4, 1)
                .texOffs(0, 16).addBox(-3, 3, -3, 1, 5, 1)
                .texOffs(8, 16).addBox(2, 3, -3, 1, 5, 1), PartPose.ZERO);
    }

    /** Right-hand map. The pivot is in the palm; the sheet extends out of the fist. */
    private static void addMap(PartDefinition root, String name) {
        PartDefinition mapRoot = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        mapRoot.addOrReplaceChild("sheet",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, 0.0F, -5.0F, 8, 1, 6),
                PartPose.offsetAndRotation(-1.0F, 9.2F, -1.0F, -0.55F, 0.20F, -0.05F));
    }

    /** Closed book. The inner half sits in the right fist; the rest sticks out the front. */
    private static void addBook(PartDefinition root, String name) {
        PartDefinition book = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        book.addOrReplaceChild("cover", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, 0.0F, -5.0F, 4, 2, 6)
                        .texOffs(0, 12).addBox(-2.0F, 0.0F, -5.0F, 4, 2, 1),
                PartPose.offsetAndRotation(-1.5F, 8.6F, -0.4F, -0.30F, 0.15F, 0.10F));
    }

    private static void addBasket(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, 9.5F, -3.5F, 6, 5, 6)
                .texOffs(24, 0).addBox(-1.0F, 7.0F, -1.4F, 1, 4, 1)
                .texOffs(28, 0).addBox(4.0F, 7.0F, -1.4F, 1, 4, 1)
                .texOffs(32, 0).addBox(0.0F, 7.0F, -1.4F, 4, 1, 1), PartPose.ZERO);
    }

    private static void addLantern(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, 10.0F, -3.5F, 4, 5, 4)
                .texOffs(16, 0).addBox(-2.0F, 9.0F, -2.5F, 2, 1, 2)
                .texOffs(16, 4).addBox(-3.0F, 15.0F, -3.5F, 4, 1, 4)
                .texOffs(24, 0).addBox(-1.5F, 8.0F, -1.4F, 1, 2, 1), PartPose.ZERO);
    }

    /** Right fist. Tubes point forward from the palm, the same grip as the book. */
    private static void addBinoculars(PartDefinition root, String name) {
        PartDefinition held = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        held.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, 0.0F, -4.0F, 4, 2, 4)
                        .texOffs(16, 0).addBox(-1.0F, 0.0F, -1.0F, 2, 2, 2),
                PartPose.offsetAndRotation(-1.2F, 8.7F, -0.5F, -0.35F, 0.12F, 0.08F));
    }

    /** Handle in the left fist. The case hangs just below, in front of the palm. */
    private static void addBriefcase(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, 9.5F, -3.5F, 5, 4, 2)
                .texOffs(20, 0).addBox(0.5F, 8.5F, -1.8F, 2, 1, 1), PartPose.ZERO);
    }

    /** Flask in the right fist, cap toward the wrist, body hanging just past the fingers. */
    private static void addCanteen(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, 9.0F, -3.0F, 3, 4, 3)
                .texOffs(16, 0).addBox(-2.0F, 8.0F, -2.0F, 2, 1, 2), PartPose.ZERO);
    }

    /** Folded sheet in the right fist, pitched forward the way the map is. Kept on the outer half of the palm so it does not cross into the torso. */
    private static void addNewspaper(PartDefinition root, String name) {
        PartDefinition held = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        held.addOrReplaceChild("sheet",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 0.0F, -2.5F, 3, 1, 3),
                PartPose.offsetAndRotation(-1.6F, 9.1F, -1.0F, -0.30F, 0.05F, 0.0F));
    }

    /** Soft bag in the left fist. Straps meet the palm; the bag hangs below it. */
    private static void addTote(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, 9.0F, -3.0F, 5, 5, 3)
                .texOffs(20, 0).addBox(0.0F, 7.0F, -2.0F, 1, 3, 1)
                .texOffs(24, 0).addBox(3.0F, 7.0F, -2.0F, 1, 3, 1)
                .texOffs(28, 0).addBox(0.0F, 7.0F, -2.0F, 4, 1, 1), PartPose.ZERO);
    }

    public void prepare(int packed) {
        this.headwear = TouristAppearance.headwear(packed);
        this.accessory = TouristAppearance.accessory(packed);
        for (int i = 1; i < hats.length; i++) {
            hats[i].visible = headwear == i;
        }
        headwearRoot.visible = headwear > TouristParts.HEADWEAR_NONE && headwear < hats.length;
        // Hair is the vanilla second head layer. Wide crowns hide it. SHOW_HAIR lists the ones that leave it.
        hat.visible = headwear >= 0 && headwear < SHOW_HAIR.length && SHOW_HAIR[headwear];
        for (int i = 1; i < accessories.length; i++) {
            if (accessories[i] != null) {
                accessories[i].visible = accessory == i;
            }
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
        attachParts();
    }

    /**
     * Parents headwear and accessories to the bone they follow. Accessories are root
     * children that copy a bone's pose, so their boxes are written in that bone's local space.
     */
    public void attachParts() {
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

    /** Root of every hat mesh. Hats are its children, named by {@link #headwearName(int)}. */
    public ModelPart headwearPart() {
        return headwearRoot;
    }

    public ModelPart accessoryPart(int index) {
        return accessories[index];
    }

    public static int headwearCount() {
        return HAT_NAMES.length;
    }

    public static int accessoryCount() {
        return ACCESSORY_NAMES.length;
    }

    public static String headwearName(int index) {
        return HAT_NAMES[index];
    }

    /** Bone an accessory follows: {@code "body"}, {@code "leftArm"} or {@code "rightArm"}. */
    public static String accessoryBone(int index) {
        if (contains(BODY_ACCESSORIES, index)) {
            return "body";
        }
        if (contains(LEFT_ACCESSORIES, index)) {
            return "leftArm";
        }
        if (contains(RIGHT_ACCESSORIES, index)) {
            return "rightArm";
        }
        throw new IllegalArgumentException("accessory " + index + " has no bone");
    }

    private static boolean contains(int[] values, int wanted) {
        for (int value : values) {
            if (value == wanted) {
                return true;
            }
        }
        return false;
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
            if (accessories[i] != null && accessories[i].visible) {
                accessories[i].render(poseStack, buffer, packedLight, packedOverlay);
            }
        }
    }
}
