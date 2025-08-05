package com.quackers29.businesscraft.client.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized renderer for 3D boundary visualization.
 * Supports various boundary shapes including rectangles, circles, and custom polygons.
 * 
 * Uses the core LineRenderer3D for actual line rendering while providing
 * higher-level boundary construction and management.
 */
public class BoundaryRenderer3D {
    
    /**
     * Boundary shape enumeration
     */
    public enum BoundaryShape {
        RECTANGLE,  // Rectangular boundary
        CIRCLE,     // Circular boundary (future implementation)
        POLYGON     // Custom polygon boundary (future implementation)
    }
    
    /**
     * Configuration class for boundary rendering
     */
    public static class BoundaryConfig {
        private BoundaryShape shape = BoundaryShape.RECTANGLE;
        private LineRenderer3D.LineConfig lineConfig = new LineRenderer3D.LineConfig();
        private boolean drawCorners = true;
        private float cornerStyle = 0.0f; // Future: rounded corners
        
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
        
        // Getters
        public BoundaryShape getShape() { return shape; }
        public LineRenderer3D.LineConfig getLineConfig() { return lineConfig; }
        public boolean shouldDrawCorners() { return drawCorners; }
        public float getCornerStyle() { return cornerStyle; }
    }
    
    /**
     * Renders a rectangular boundary around two positions with default configuration
     * 
     * @param poseStack The pose stack for transformations
     * @param startPos First corner position
     * @param endPos Second corner position  
     * @param radius Boundary expansion radius
     * @param color Boundary color
     */
    public static void renderRectangularBoundary(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                               int radius, LineRenderer3D.Color color) {
        renderRectangularBoundary(poseStack, startPos, endPos, radius, color, new BoundaryConfig());
    }
    
    /**
     * Renders a rectangular boundary around two positions with custom configuration
     * 
     * @param poseStack The pose stack for transformations
     * @param startPos First corner position
     * @param endPos Second corner position
     * @param radius Boundary expansion radius
     * @param color Boundary color
     * @param config Boundary configuration
     */
    public static void renderRectangularBoundary(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                               int radius, LineRenderer3D.Color color, BoundaryConfig config) {
        
        // Calculate bounding box (same algorithm as original platform system)
        int minX = Math.min(startPos.getX(), endPos.getX()) - radius;
        int minZ = Math.min(startPos.getZ(), endPos.getZ()) - radius;
        int maxX = Math.max(startPos.getX(), endPos.getX()) + radius;
        int maxZ = Math.max(startPos.getZ(), endPos.getZ()) + radius;
        
        // Use the same Y level as the platform positions for consistency
        int boundaryY = Math.min(startPos.getY(), endPos.getY());
        
        switch (config.getShape()) {
            case RECTANGLE:
                renderRectangleSegments(poseStack, minX, minZ, maxX, maxZ, boundaryY, color, config);
                break;
            case CIRCLE:
                renderCircularBoundary(poseStack, startPos, endPos, radius, color, config);
                break;
            case POLYGON:
                // Future implementation: custom polygon boundary
                renderRectangleSegments(poseStack, minX, minZ, maxX, maxZ, boundaryY, color, config);
                break;
        }
    }
    
    /**
     * Renders a boundary around a center point with given dimensions
     * 
     * @param poseStack The pose stack for transformations
     * @param center Center position
     * @param width Boundary width
     * @param height Boundary height
     * @param color Boundary color
     * @param config Boundary configuration
     */
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
    
    /**
     * Renders a custom polygon boundary from a list of points
     * 
     * @param poseStack The pose stack for transformations
     * @param points List of boundary points (must form a closed polygon)
     * @param color Boundary color
     * @param config Boundary configuration
     */
    public static void renderPolygonBoundary(PoseStack poseStack, List<Vec3> points, 
                                           LineRenderer3D.Color color, BoundaryConfig config) {
        if (points.size() < 3) {
            return; // Need at least 3 points for a polygon
        }
        
        // Render each edge of the polygon
        for (int i = 0; i < points.size(); i++) {
            Vec3 start = points.get(i);
            Vec3 end = points.get((i + 1) % points.size()); // Wrap to first point for last edge
            
            LineRenderer3D.renderLine(poseStack, start, end, color, config.getLineConfig());
        }
    }
    
    /**
     * Renders rectangular boundary segments using the core line renderer
     */
    private static void renderRectangleSegments(PoseStack poseStack, int minX, int minZ, int maxX, int maxZ, 
                                              int boundaryY, LineRenderer3D.Color color, BoundaryConfig config) {
        
        // Bottom edge (minX to maxX at minZ)
        for (int x = minX; x < maxX; x++) {
            BlockPos segmentStart = new BlockPos(x, boundaryY, minZ);
            BlockPos segmentEnd = new BlockPos(x + 1, boundaryY, minZ);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }
        
        // Top edge (minX to maxX at maxZ)
        for (int x = minX; x < maxX; x++) {
            BlockPos segmentStart = new BlockPos(x, boundaryY, maxZ);
            BlockPos segmentEnd = new BlockPos(x + 1, boundaryY, maxZ);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }
        
        // Left edge (minZ to maxZ at minX)
        for (int z = minZ; z < maxZ; z++) {
            BlockPos segmentStart = new BlockPos(minX, boundaryY, z);
            BlockPos segmentEnd = new BlockPos(minX, boundaryY, z + 1);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }
        
        // Right edge (minZ to maxZ at maxX)
        for (int z = minZ; z < maxZ; z++) {
            BlockPos segmentStart = new BlockPos(maxX, boundaryY, z);
            BlockPos segmentEnd = new BlockPos(maxX, boundaryY, z + 1);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }
    }
    
    /**
     * Renders a circular boundary around a center point with given radius
     * 
     * @param poseStack The pose stack for transformations
     * @param startPos First reference position (used for center calculation)
     * @param endPos Second reference position (used for center calculation)
     * @param radius Boundary radius
     * @param color Boundary color
     * @param config Boundary configuration
     */
    public static void renderCircularBoundary(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                            int radius, LineRenderer3D.Color color, BoundaryConfig config) {
        // Calculate center point from the two positions
        Vec3 center = new Vec3(
            (startPos.getX() + endPos.getX()) / 2.0,
            Math.min(startPos.getY(), endPos.getY()),
            (startPos.getZ() + endPos.getZ()) / 2.0
        );
        
        renderCircularBoundaryFromCenter(poseStack, center, radius, color, config);
    }
    
    /**
     * Renders a circular boundary around a center point with given radius
     * 
     * @param poseStack The pose stack for transformations
     * @param center The center position of the circle
     * @param radius Boundary radius
     * @param color Boundary color
     * @param config Boundary configuration
     */
    public static void renderCircularBoundaryFromCenter(PoseStack poseStack, Vec3 center, double radius, 
                                                      LineRenderer3D.Color color, BoundaryConfig config) {
        // Use 64 segments for smooth circles - higher quality for town boundaries
        int segments = 64;
        List<Vec3> points = createCircularBoundaryPoints(center, radius, segments);
        renderPolygonBoundary(poseStack, points, color, config);
    }
    
    /**
     * Renders a circular boundary around a BlockPos center with given radius
     * 
     * @param poseStack The pose stack for transformations
     * @param center The center position of the circle
     * @param radius Boundary radius
     * @param color Boundary color
     * @param config Boundary configuration
     */
    public static void renderCircularBoundaryFromCenter(PoseStack poseStack, BlockPos center, double radius, 
                                                      LineRenderer3D.Color color, BoundaryConfig config) {
        Vec3 centerVec = new Vec3(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        renderCircularBoundaryFromCenter(poseStack, centerVec, radius, color, config);
    }
    
    /**
     * Helper method to create boundary points for a circular boundary
     */
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
    
    /**
     * Helper method to calculate the bounding box for any set of positions
     */
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