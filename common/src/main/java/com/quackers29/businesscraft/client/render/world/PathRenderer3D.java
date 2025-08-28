package com.quackers29.businesscraft.client.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized renderer for 3D path visualization.
 * Supports various path types including straight lines, multi-point paths, and curved paths.
 * 
 * Uses the core LineRenderer3D for actual line rendering while providing
 * higher-level path construction and interpolation.
 */
public class PathRenderer3D {
    
    /**
     * Path interpolation types
     */
    public enum InterpolationType {
        LINEAR,     // Straight line segments between points
        STEPPED,    // Step-wise progression (matches original platform algorithm)
        CURVED,     // Smooth curves between points (future implementation)
        BEZIER      // Bezier curve paths (future implementation)
    }
    
    /**
     * Path animation types (future implementation)
     */
    public enum AnimationType {
        NONE,           // Static path
        FORWARD_FLOW,   // Animation flows from start to end
        BACKWARD_FLOW,  // Animation flows from end to start
        PULSE,          // Pulsing animation
        DASHED_FLOW     // Moving dashes
    }
    
    /**
     * Configuration class for path rendering
     */
    public static class PathConfig {
        private InterpolationType interpolation = InterpolationType.STEPPED;
        private AnimationType animation = AnimationType.NONE;
        private LineRenderer3D.LineConfig lineConfig = new LineRenderer3D.LineConfig();
        private boolean drawDirectionalIndicators = false;
        private float indicatorSpacing = 5.0f; // Blocks between directional indicators
        
        public PathConfig interpolation(InterpolationType interpolation) {
            this.interpolation = interpolation;
            return this;
        }
        
        public PathConfig animation(AnimationType animation) {
            this.animation = animation;
            return this;
        }
        
        public PathConfig lineConfig(LineRenderer3D.LineConfig lineConfig) {
            this.lineConfig = lineConfig;
            return this;
        }
        
        public PathConfig directionalIndicators(boolean drawIndicators, float spacing) {
            this.drawDirectionalIndicators = drawIndicators;
            this.indicatorSpacing = spacing;
            return this;
        }
        
        // Getters
        public InterpolationType getInterpolation() { return interpolation; }
        public AnimationType getAnimation() { return animation; }
        public LineRenderer3D.LineConfig getLineConfig() { return lineConfig; }
        public boolean shouldDrawDirectionalIndicators() { return drawDirectionalIndicators; }
        public float getIndicatorSpacing() { return indicatorSpacing; }
    }
    
    /**
     * Renders a simple path between two positions with default configuration
     * 
     * @param poseStack The pose stack for transformations
     * @param startPos Starting position
     * @param endPos Ending position
     * @param color Path color
     */
    public static void renderPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                LineRenderer3D.Color color) {
        renderPath(poseStack, startPos, endPos, color, new PathConfig());
    }
    
    /**
     * Renders a path between two positions using stepped interpolation (original platform algorithm)
     * 
     * @param poseStack The pose stack for transformations
     * @param startPos Starting position
     * @param endPos Ending position
     * @param color Path color
     * @param config Path configuration
     */
    public static void renderPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                LineRenderer3D.Color color, PathConfig config) {
        
        switch (config.getInterpolation()) {
            case LINEAR:
                renderLinearPath(poseStack, startPos, endPos, color, config);
                break;
            case STEPPED:
                renderSteppedPath(poseStack, startPos, endPos, color, config);
                break;
            case CURVED:
                // Future implementation: smooth curves
                renderSteppedPath(poseStack, startPos, endPos, color, config);
                break;
            case BEZIER:
                // Future implementation: bezier curves
                renderSteppedPath(poseStack, startPos, endPos, color, config);
                break;
        }
    }
    
    /**
     * Renders a multi-point path through a series of waypoints
     * 
     * @param poseStack The pose stack for transformations
     * @param waypoints List of waypoint positions
     * @param color Path color
     * @param config Path configuration
     */
    public static void renderMultiPointPath(PoseStack poseStack, List<BlockPos> waypoints, 
                                          LineRenderer3D.Color color, PathConfig config) {
        if (waypoints.size() < 2) {
            return; // Need at least 2 points for a path
        }
        
        // Render each segment of the multi-point path
        for (int i = 0; i < waypoints.size() - 1; i++) {
            BlockPos start = waypoints.get(i);
            BlockPos end = waypoints.get(i + 1);
            renderPath(poseStack, start, end, color, config);
        }
    }
    
    /**
     * Renders a path using world coordinates instead of block positions
     * 
     * @param poseStack The pose stack for transformations
     * @param waypoints List of world coordinate waypoints
     * @param color Path color
     * @param config Path configuration
     */
    public static void renderWorldPath(PoseStack poseStack, List<Vec3> waypoints, 
                                     LineRenderer3D.Color color, PathConfig config) {
        if (waypoints.size() < 2) {
            return;
        }
        
        // Render each segment using world coordinates
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Vec3 start = waypoints.get(i);
            Vec3 end = waypoints.get(i + 1);
            LineRenderer3D.renderLine(poseStack, start, end, color, config.getLineConfig());
        }
    }
    
    /**
     * Renders a simple linear path (direct line between points)
     */
    private static void renderLinearPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                       LineRenderer3D.Color color, PathConfig config) {
        LineRenderer3D.renderLine(poseStack, startPos, endPos, color, config.getLineConfig());
        
        if (config.shouldDrawDirectionalIndicators()) {
            renderDirectionalIndicators(poseStack, startPos, endPos, color, config);
        }
    }
    
    /**
     * Renders a stepped path using the original platform algorithm
     * Maintains compatibility with existing platform visualization
     */
    private static void renderSteppedPath(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                        LineRenderer3D.Color color, PathConfig config) {
        
        // Calculate path points using the same algorithm as the original platform system
        int x0 = startPos.getX();
        int y0 = startPos.getY();
        int z0 = startPos.getZ();
        int x1 = endPos.getX();
        int y1 = endPos.getY();
        int z1 = endPos.getZ();
        
        // Calculate differences
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        
        // Find the maximum difference to determine number of steps
        int maxSteps = Math.max(Math.max(dx, dy), dz);
        
        if (maxSteps == 0) {
            // Single point, just render a small line
            LineRenderer3D.renderLine(poseStack, startPos, startPos, color, config.getLineConfig());
            return;
        }
        
        // Generate connected thick line segments along the path
        for (int i = 0; i < maxSteps; i++) {
            double t1 = (double) i / maxSteps;
            double t2 = (double) (i + 1) / maxSteps;
            
            int x1_pos = x0 + (int) Math.round(t1 * (x1 - x0));
            int y1_pos = y0 + (int) Math.round(t1 * (y1 - y0));
            int z1_pos = z0 + (int) Math.round(t1 * (z1 - z0));
            
            int x2_pos = x0 + (int) Math.round(t2 * (x1 - x0));
            int y2_pos = y0 + (int) Math.round(t2 * (y1 - y0));
            int z2_pos = z0 + (int) Math.round(t2 * (z1 - z0));
            
            // Use thick line rendering for each segment
            BlockPos segmentStart = new BlockPos(x1_pos, y1_pos, z1_pos);
            BlockPos segmentEnd = new BlockPos(x2_pos, y2_pos, z2_pos);
            LineRenderer3D.renderLine(poseStack, segmentStart, segmentEnd, color, config.getLineConfig());
        }
        
        if (config.shouldDrawDirectionalIndicators()) {
            renderDirectionalIndicators(poseStack, startPos, endPos, color, config);
        }
    }
    
    /**
     * Renders directional indicators along the path (future implementation)
     */
    private static void renderDirectionalIndicators(PoseStack poseStack, BlockPos startPos, BlockPos endPos, 
                                                  LineRenderer3D.Color color, PathConfig config) {
        // Future implementation: render arrows or other directional indicators
        // Could use small perpendicular lines or arrow-shaped geometry
        
        Vec3 direction = Vec3.atCenterOf(endPos).subtract(Vec3.atCenterOf(startPos)).normalize();
        double pathLength = Vec3.atCenterOf(startPos).distanceTo(Vec3.atCenterOf(endPos));
        
        // Place indicators at regular intervals
        float spacing = config.getIndicatorSpacing();
        int numIndicators = (int) (pathLength / spacing);
        
        for (int i = 1; i <= numIndicators; i++) {
            double t = (i * spacing) / pathLength;
            if (t >= 1.0) break;
            
            Vec3 indicatorPos = Vec3.atCenterOf(startPos).lerp(Vec3.atCenterOf(endPos), t);
            
            // Simple directional indicator: small perpendicular line
            Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x).scale(0.5);
            Vec3 indicatorStart = indicatorPos.subtract(perpendicular);
            Vec3 indicatorEnd = indicatorPos.add(perpendicular);
            
            LineRenderer3D.LineConfig indicatorConfig = new LineRenderer3D.LineConfig()
                .thickness(config.getLineConfig().getThickness() * 0.5f)
                .yOffset(config.getLineConfig().getYOffset() + 0.1f);
            
            LineRenderer3D.renderLine(poseStack, indicatorStart, indicatorEnd, color, indicatorConfig);
        }
    }
    
    /**
     * Helper method to generate smooth curve points between waypoints (future implementation)
     */
    private static List<Vec3> generateCurvePoints(List<Vec3> controlPoints, int segmentsPerCurve) {
        List<Vec3> curvePoints = new ArrayList<>();
        
        // Future implementation: generate smooth curve points using spline interpolation
        // For now, return the original control points for linear interpolation
        curvePoints.addAll(controlPoints);
        
        return curvePoints;
    }
    
    /**
     * Helper method to generate bezier curve points (future implementation)
     */
    private static List<Vec3> generateBezierPoints(Vec3 start, Vec3 control1, Vec3 control2, Vec3 end, int segments) {
        List<Vec3> bezierPoints = new ArrayList<>();
        
        // Future implementation: generate bezier curve points
        // For now, return linear interpolation
        bezierPoints.add(start);
        bezierPoints.add(end);
        
        return bezierPoints;
    }
    
    /**
     * Utility class for path calculations
     */
    public static class PathUtils {
        
        /**
         * Calculates the total length of a multi-point path
         */
        public static double calculatePathLength(List<Vec3> waypoints) {
            if (waypoints.size() < 2) {
                return 0.0;
            }
            
            double totalLength = 0.0;
            for (int i = 0; i < waypoints.size() - 1; i++) {
                totalLength += waypoints.get(i).distanceTo(waypoints.get(i + 1));
            }
            
            return totalLength;
        }
        
        /**
         * Finds the closest point on a path to a given position
         */
        public static Vec3 findClosestPointOnPath(List<Vec3> waypoints, Vec3 targetPos) {
            if (waypoints.isEmpty()) {
                return targetPos;
            }
            
            Vec3 closestPoint = waypoints.get(0);
            double closestDistance = targetPos.distanceTo(closestPoint);
            
            for (Vec3 waypoint : waypoints) {
                double distance = targetPos.distanceTo(waypoint);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPoint = waypoint;
                }
            }
            
            return closestPoint;
        }
        
        /**
         * Simplifies a path by removing redundant waypoints
         */
        public static List<Vec3> simplifyPath(List<Vec3> waypoints, double tolerance) {
            if (waypoints.size() <= 2) {
                return new ArrayList<>(waypoints);
            }
            
            List<Vec3> simplified = new ArrayList<>();
            simplified.add(waypoints.get(0));
            
            // Douglas-Peucker algorithm implementation (simplified)
            // For now, just remove points that are too close together
            Vec3 lastPoint = waypoints.get(0);
            for (int i = 1; i < waypoints.size() - 1; i++) {
                Vec3 currentPoint = waypoints.get(i);
                if (lastPoint.distanceTo(currentPoint) > tolerance) {
                    simplified.add(currentPoint);
                    lastPoint = currentPoint;
                }
            }
            
            simplified.add(waypoints.get(waypoints.size() - 1));
            return simplified;
        }
    }
}