package com.quackers29.businesscraft.client.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class BoundaryRenderer3D {

    public enum BoundaryShape {
        RECTANGLE,
        CIRCLE,
        POLYGON
    }

    public static class BoundaryConfig {
        private BoundaryShape shape = BoundaryShape.RECTANGLE;
        private LineRenderer3D.LineConfig lineConfig = new LineRenderer3D.LineConfig();
        private boolean drawCorners = true;
        private float cornerStyle = 0.0f;

        public BoundaryConfig shape(BoundaryShape shape) {
            this.shape = shape;
            return this;
        }

        public BoundaryConfig lineConfig(LineRenderer3D.LineConfig lineConfig) {
            this.lineConfig = lineConfig;
            return this;
        }

        public BoundaryConfig drawCorners(boolean drawCorners) {
            this.drawCorners = drawCorners;
            return this;
        }

        public BoundaryConfig cornerStyle(float cornerStyle) {
            this.cornerStyle = cornerStyle;
            return this;
        }

        public BoundaryShape getShape() { return shape; }
        public LineRenderer3D.LineConfig getLineConfig() { return lineConfig; }
        public boolean shouldDrawCorners() { return drawCorners; }
        public float getCornerStyle() { return cornerStyle; }
    }

    public static void renderRectangularBoundary(PoseStack poseStack, BlockPos startPos, BlockPos endPos,
                                               int radius, LineRenderer3D.Color color) {
        renderRectangularBoundary(poseStack, startPos, endPos, radius, color, new BoundaryConfig());
    }

    public static void renderRectangularBoundary(PoseStack poseStack, BlockPos startPos, BlockPos endPos,
                                               int radius, LineRenderer3D.Color color, BoundaryConfig config) {

        int minX = Math.min(startPos.getX(), endPos.getX()) - radius;
        int minZ = Math.min(startPos.getZ(), endPos.getZ()) - radius;
        int maxX = Math.max(startPos.getX(), endPos.getX()) + radius;
        int maxZ = Math.max(startPos.getZ(), endPos.getZ()) + radius;

        int boundaryY = Math.min(startPos.getY(), endPos.getY());

        switch (config.getShape()) {
            case RECTANGLE:
                renderRectangleSegments(poseStack, minX, minZ, maxX, maxZ, boundaryY, color, config);
                break;
            case CIRCLE:
                renderCircularBoundary(poseStack, startPos, endPos, radius, color, config);
                break;
            case POLYGON:
                renderRectangleSegments(poseStack, minX, minZ, maxX, maxZ, boundaryY, color, config);
                break;
        }
    }

    public static void renderCenteredBoundary(PoseStack poseStack, BlockPos center, int width, int height,
                                            LineRenderer3D.Color color, BoundaryConfig config) {
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        int minX = center.getX() - halfWidth;
        int minZ = center.getZ() - halfHeight;
        int maxX = center.getX() + halfWidth;
        int maxZ = center.getZ() + halfHeight;

        renderRectangleSegments(poseStack, minX, minZ, maxX, maxZ, center.getY(), color, config);
    }

    public static void renderPolygonBoundary(PoseStack poseStack, List<Vec3> points,
                                           LineRenderer3D.Color color, BoundaryConfig config) {
        if (points.size() < 3) {
            return;
        }

        for (int i = 0; i < points.size(); i++) {
            Vec3 start = points.get(i);
            Vec3 end = points.get((i + 1) % points.size());

            LineRenderer3D.renderLine(poseStack, start, end, color, config.getLineConfig());
        }
    }

    private static void renderRectangleSegments(PoseStack poseStack, int minX, int minZ, int maxX, int maxZ,
                                              int boundaryY, LineRenderer3D.Color color, BoundaryConfig config) {

        for (int x = minX; x < maxX; x++) {
            BlockPos segmentStart = new BlockPos(x, boundaryY, minZ);
            BlockPos segmentEnd = new BlockPos(x + 1, boundaryY, minZ);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }

        for (int x = minX; x < maxX; x++) {
            BlockPos segmentStart = new BlockPos(x, boundaryY, maxZ);
            BlockPos segmentEnd = new BlockPos(x + 1, boundaryY, maxZ);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }

        for (int z = minZ; z < maxZ; z++) {
            BlockPos segmentStart = new BlockPos(minX, boundaryY, z);
            BlockPos segmentEnd = new BlockPos(minX, boundaryY, z + 1);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }

        for (int z = minZ; z < maxZ; z++) {
            BlockPos segmentStart = new BlockPos(maxX, boundaryY, z);
            BlockPos segmentEnd = new BlockPos(maxX, boundaryY, z + 1);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }
    }

    public static void renderCircularBoundary(PoseStack poseStack, BlockPos startPos, BlockPos endPos,
                                            int radius, LineRenderer3D.Color color, BoundaryConfig config) {
        Vec3 center = new Vec3(
            (startPos.getX() + endPos.getX()) / 2.0,
            Math.min(startPos.getY(), endPos.getY()),
            (startPos.getZ() + endPos.getZ()) / 2.0
        );

        renderCircularBoundaryFromCenter(poseStack, center, radius, color, config);
    }

    public static void renderCircularBoundaryFromCenter(PoseStack poseStack, Vec3 center, double radius,
                                                      LineRenderer3D.Color color, BoundaryConfig config) {
        int segments = 64;
        List<Vec3> points = createCircularBoundaryPoints(center, radius, segments);
        renderPolygonBoundary(poseStack, points, color, config);
    }

    public static void renderCircularBoundaryFromCenter(PoseStack poseStack, BlockPos center, double radius,
                                                      LineRenderer3D.Color color, BoundaryConfig config) {
        Vec3 centerVec = new Vec3(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        renderCircularBoundaryFromCenter(poseStack, centerVec, radius, color, config);
    }

    private static List<Vec3> createCircularBoundaryPoints(Vec3 center, double radius, int segments) {
        List<Vec3> points = new ArrayList<>();

        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            points.add(new Vec3(x, center.y, z));
        }

        return points;
    }

    public static class BoundingBox {
        public final int minX, minY, minZ, maxX, maxY, maxZ;

        public BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public static BoundingBox fromPositions(List<BlockPos> positions, int padding) {
            if (positions.isEmpty()) {
                return new BoundingBox(0, 0, 0, 0, 0, 0);
            }

            int minX = positions.get(0).getX();
            int minY = positions.get(0).getY();
            int minZ = positions.get(0).getZ();
            int maxX = minX;
            int maxY = minY;
            int maxZ = minZ;

            for (BlockPos pos : positions) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }

            return new BoundingBox(minX - padding, minY - padding, minZ - padding,
                                 maxX + padding, maxY + padding, maxZ + padding);
        }
    }
}
