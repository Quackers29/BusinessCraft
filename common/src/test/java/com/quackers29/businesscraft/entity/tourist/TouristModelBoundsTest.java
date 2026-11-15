package com.quackers29.businesscraft.entity.tourist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quackers29.businesscraft.client.model.TouristModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every tourist hat and accessory stays in its own body region.
 *
 * <p>Boxes come from the baked model itself: cubes of the vanilla bones are the region
 * volumes, and each hat or accessory is the set of cubes under its own part. Parts are
 * posed through {@link TouristModel#attachParts()} (the same call {@code setupAnim} makes),
 * so a wrong bone shows up here as it does in game. Rotated cubes are compared as oriented
 * boxes (separating axis test); touching faces are fine, penetration is not.
 */
class TouristModelBoundsTest {
    /** Penetration below this depth (blocks) counts as touching. */
    private static final float TOUCH = 1.0e-3f;
    /** Mob.getMaxHeadYRot(): the head turns up to this far from the body. */
    private static final float MAX_HEAD_YAW = (float) Math.toRadians(75);
    /** Just past Mob.getMaxHeadXRot() (40): looking at a player standing close by, above or below. */
    private static final float MAX_HEAD_PITCH = (float) Math.toRadians(45);
    /** HumanoidModel at full limb swing: arms swing by 2 * 0.5 rad, legs by 1.4 rad. */
    private static final float ARM_SWING = 1.0f;
    private static final float LEG_SWING = 1.4f;

    private static TouristModel model;

    @BeforeAll
    static void bake() {
        model = new TouristModel(TouristModel.createBodyLayer().bakeRoot());
    }

    /** A cube in model space, as its eight corners (blocks). */
    private record Box(String label, Vector3f[] corners) {
        Vector3f axis(int i) {
            return new Vector3f(corners[i == 0 ? 4 : i == 1 ? 2 : 1]).sub(corners[0]);
        }
    }

    private record Pose(float pitch, float yaw, float armSwing, float legSwing) {
        @Override
        public String toString() {
            return String.format("pitch %.0f yaw %.0f arms %.1f legs %.1f", Math.toDegrees(pitch),
                    Math.toDegrees(yaw), armSwing, legSwing);
        }
    }

    private static List<Box> boxes(ModelPart part, String onlyUnder) {
        List<Box> out = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (onlyUnder != null && !path.equals("/" + onlyUnder)) {
                return;
            }
            Matrix4f m = pose.pose();
            float[] x = {cube.minX / 16f, cube.maxX / 16f};
            float[] y = {cube.minY / 16f, cube.maxY / 16f};
            float[] z = {cube.minZ / 16f, cube.maxZ / 16f};
            Vector3f[] corners = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                corners[i] = m.transformPosition(new Vector3f(x[i & 1], y[(i >> 1) & 1], z[(i >> 2) & 1]));
            }
            out.add(new Box(path + index, corners));
        });
        return out;
    }

    /** Overlap along the axis that separates two oriented boxes best; at or below zero means apart. */
    private static float penetration(Box a, Box b) {
        List<Vector3f> axes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            axes.add(a.axis(i));
            axes.add(b.axis(i));
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                axes.add(new Vector3f(a.axis(i)).cross(b.axis(j)));
            }
        }
        float least = Float.MAX_VALUE;
        for (Vector3f axis : axes) {
            if (axis.lengthSquared() < 1.0e-8f) {
                continue;
            }
            axis.normalize();
            float[] ra = range(a, axis);
            float[] rb = range(b, axis);
            float overlap = Math.min(ra[1], rb[1]) - Math.max(ra[0], rb[0]);
            least = Math.min(least, overlap);
        }
        return least;
    }

    private static float[] range(Box box, Vector3f axis) {
        float lo = Float.MAX_VALUE;
        float hi = -Float.MAX_VALUE;
        for (Vector3f corner : box.corners) {
            float d = corner.dot(axis);
            lo = Math.min(lo, d);
            hi = Math.max(hi, d);
        }
        return new float[]{lo, hi};
    }

    private static List<Pose> poses() {
        List<Pose> poses = new ArrayList<>();
        float[] pitches = {-MAX_HEAD_PITCH, -MAX_HEAD_PITCH / 2, 0, MAX_HEAD_PITCH / 2, MAX_HEAD_PITCH};
        float[] yaws = {-MAX_HEAD_YAW, 0, MAX_HEAD_YAW};
        float[] swings = {-1, 0, 1};
        for (float pitch : pitches) {
            for (float yaw : yaws) {
                for (float swing : swings) {
                    poses.add(new Pose(pitch, yaw, swing * ARM_SWING, -swing * LEG_SWING));
                }
            }
        }
        return poses;
    }

    private static void apply(Pose pose) {
        model.head.xRot = pose.pitch;
        model.head.yRot = pose.yaw;
        model.rightArm.xRot = pose.armSwing;
        model.leftArm.xRot = -pose.armSwing;
        model.rightLeg.xRot = pose.legSwing;
        model.leftLeg.xRot = -pose.legSwing;
        model.attachParts();
    }

    /** Vanilla bones other than the part's own bone. */
    private static Map<String, ModelPart> otherRegions(String home) {
        Map<String, ModelPart> regions = new LinkedHashMap<>();
        regions.put("head", model.head);
        regions.put("body", model.body);
        regions.put("rightArm", model.rightArm);
        regions.put("leftArm", model.leftArm);
        regions.put("rightLeg", model.rightLeg);
        regions.put("leftLeg", model.leftLeg);
        regions.remove(home);
        return regions;
    }

    private static void checkPart(String name, String home, java.util.function.Supplier<List<Box>> partBoxes,
            List<String> problems) {
        Map<String, Float> worst = new LinkedHashMap<>();
        Map<String, String> where = new LinkedHashMap<>();
        for (Pose pose : poses()) {
            apply(pose);
            List<Box> mine = partBoxes.get();
            for (Map.Entry<String, ModelPart> region : otherRegions(home).entrySet()) {
                List<Box> regionBoxes = boxes(region.getValue(), null);
                for (Box other : regionBoxes) {
                    // The head itself dips into the chest or shoulder at extreme tilts (vanilla).
                    // A hat may follow it there, but must not go deeper than the head does.
                    float allowed = home.equals("head") ? Math.max(0f, penetration(boxes(model.head, null).get(0), other)) : 0f;
                    for (Box box : mine) {
                        float depth = penetration(box, other) - allowed;
                        String key = "own cube " + box.label + " enters " + region.getKey();
                        if (depth > TOUCH && depth > worst.getOrDefault(key, 0f)) {
                            worst.put(key, depth);
                            where.put(key, String.format("%.1fpx at %s", depth * 16f, pose));
                        }
                    }
                }
            }
        }
        where.forEach((key, what) -> problems.add(name + " (on " + home + ") " + key + ": " + what));
    }

    @Test
    void headwear_staysOnTheHead() {
        List<String> problems = new ArrayList<>();
        for (int i = 1; i <= TouristParts.HEADWEAR_MAX; i++) {
            String name = TouristModel.headwearName(i);
            checkPart("hat " + name, "head", () -> boxes(model.headwearPart(), name), problems);
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void accessories_stayInTheirRegion() {
        List<String> problems = new ArrayList<>();
        for (int i = 1; i <= TouristParts.ACCESSORY_MAX; i++) {
            if (!TouristParts.knownAccessory(i)) {
                continue;
            }
            int index = i;
            checkPart("accessory " + i, TouristModel.accessoryBone(i),
                    () -> boxes(model.accessoryPart(index), null), problems);
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void parts_touchTheirOwnRegion() {
        // A part that floats clear of its bone is attached to the wrong place or offset wrongly.
        // Cubes may also hang from other cubes of the same part (a strap off a camera).
        apply(new Pose(0, 0, 0, 0));
        List<String> problems = new ArrayList<>();
        for (int i = 1; i <= TouristParts.HEADWEAR_MAX; i++) {
            String name = TouristModel.headwearName(i);
            checkConnected("hat " + name, boxes(model.headwearPart(), name), boxes(model.head, null), problems);
        }
        for (int i = 1; i <= TouristParts.ACCESSORY_MAX; i++) {
            if (!TouristParts.knownAccessory(i)) {
                continue;
            }
            String bone = TouristModel.accessoryBone(i);
            ModelPart home = switch (bone) {
                case "body" -> model.body;
                case "leftArm" -> model.leftArm;
                default -> model.rightArm;
            };
            checkConnected("accessory " + i, boxes(model.accessoryPart(i), null), boxes(home, null), problems);
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static void checkConnected(String name, List<Box> cubes, List<Box> home, List<String> problems) {
        List<Box> attached = new ArrayList<>();
        List<Box> loose = new ArrayList<>(cubes);
        for (Box cube : cubes) {
            for (Box region : home) {
                if (touches(cube, region)) {
                    attached.add(cube);
                    loose.remove(cube);
                    break;
                }
            }
        }
        boolean grew = true;
        while (grew && !loose.isEmpty()) {
            grew = false;
            for (Box cube : new ArrayList<>(loose)) {
                for (Box anchor : attached) {
                    if (touches(cube, anchor)) {
                        attached.add(cube);
                        loose.remove(cube);
                        grew = true;
                        break;
                    }
                }
            }
        }
        for (Box cube : loose) {
            problems.add(name + " cube " + cube.label + " floats clear of its region");
        }
    }

    /** Overlapping or face-to-face within {@link #TOUCH}. */
    private static boolean touches(Box a, Box b) {
        return penetration(a, b) > -TOUCH;
    }
}
