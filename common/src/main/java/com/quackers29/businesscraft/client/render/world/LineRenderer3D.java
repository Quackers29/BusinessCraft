package com.quackers29.businesscraft.client.render.world;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Core 3D line rendering system for world-space visualizations.
 * Renders thick 3D rectangular prism lines using quad-based geometry.
 * 
 * This modular renderer can be used throughout the mod for various visualization needs:
 * - Platform paths and boundaries
 * - Route visualization systems
 * - Debug overlays
 * - Territory marking
 * - Quest path indicators
 */
public class LineRenderer3D {
    
    // Default rendering parameters
    public static final float DEFAULT_THICKNESS = 0.05f;
    public static final float DEFAULT_Y_OFFSET = 1.1f; // 0.1 blocks above surface
    
    /**
     * Configuration class for line rendering parameters
     */
    public static class LineConfig {
        private float thickness = DEFAULT_THICKNESS;
        private float yOffset = DEFAULT_Y_OFFSET;
        private LineStyle style = LineStyle.SOLID;
        private boolean enableDepthTest = true;
        private boolean enableBlend = true;
        
        public LineConfig thickness(float thickness) {
            this.thickness = thickness;
            return this;
        }
        
        public LineConfig yOffset(float yOffset) {
            this.yOffset = yOffset;
            return this;
        }
        
        public LineConfig style(LineStyle style) {
            this.style = style;
            return this;
        }
        
        public LineConfig depthTest(boolean enable) {
            this.enableDepthTest = enable;
            return this;
        }
        
        public LineConfig blend(boolean enable) {
            this.enableBlend = enable;
            return this;
        }
        
        // Getters
        public float getThickness() { return thickness; }
        public float getYOffset() { return yOffset; }
        public LineStyle getStyle() { return style; }
        public boolean isDepthTestEnabled() { return enableDepthTest; }
        public boolean isBlendEnabled() { return enableBlend; }
    }
    
    /**
     * Line style enumeration for future extensibility
     */
    public enum LineStyle {
        SOLID,      // Standard solid line
        DASHED,     // Dashed line (future implementation)
        ANIMATED    // Animated line (future implementation)
    }
    
    /**
     * Color class for RGBA values
     */
    public static class Color {
        public final float r, g, b, a;
        
        public Color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
        
        public Color(float r, float g, float b) {
            this(r, g, b, 1.0f);
        }
        
        // Common colors
        public static final Color RED = new Color(1.0f, 0.0f, 0.0f, 0.8f);
        public static final Color GREEN = new Color(0.0f, 1.0f, 0.0f, 0.8f);
        public static final Color BLUE = new Color(0.0f, 0.0f, 1.0f, 0.8f);
        public static final Color ORANGE = new Color(1.0f, 0.3f, 0.0f, 0.8f);
        public static final Color WHITE = new Color(1.0f, 1.0f, 1.0f, 0.8f);
        public static final Color YELLOW = new Color(1.0f, 1.0f, 0.0f, 0.8f);
    }
    
    /**
     * Renders a thick 3D line between two block positions using default configuration
     * 
     * @param poseStack The pose stack for transformations
     * @param startPos Starting block position
     * @param endPos Ending block position
     * @param color Line color
     */
    public static void renderLine(PoseStack poseStack, BlockPos startPos, BlockPos endPos, Color color) {
        renderLine(poseStack, startPos, endPos, color, new LineConfig());
    }
    
    /**
     * Renders a thick 3D line between two block positions with world coordinates
     * 
     * @param poseStack The pose stack for transformations
     * @param startWorld Starting world position
     * @param endWorld Ending world position  
     * @param color Line color
     * @param config Line configuration
     */
    public static void renderLine(PoseStack poseStack, Vec3 startWorld, Vec3 endWorld, Color color, LineConfig config) {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) return;
        
        Object minecraftObj = clientHelper.getMinecraft();
        if (!(minecraftObj instanceof net.minecraft.client.Minecraft minecraft)) return;
        
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        
        // Apply camera offset and Y positioning
        double startX = startWorld.x - cameraPos.x;
        double startY = startWorld.y + config.getYOffset() - cameraPos.y;
        double startZ = startWorld.z - cameraPos.z;
        double endX = endWorld.x - cameraPos.x;
        double endY = endWorld.y + config.getYOffset() - cameraPos.y;
        double endZ = endWorld.z - cameraPos.z;
        
        renderLine3D(poseStack, startX, startY, startZ, endX, endY, endZ, color, config);
    }
    
    /**
     * Renders a thick 3D line between two block positions with custom configuration
     * 
     * @param poseStack The pose stack for transformations
     * @param startPos Starting block position
     * @param endPos Ending block position
     * @param color Line color
     * @param config Line configuration
     */
    public static void renderLine(PoseStack poseStack, BlockPos startPos, BlockPos endPos, Color color, LineConfig config) {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) return;
        
        Object minecraftObj = clientHelper.getMinecraft();
        if (!(minecraftObj instanceof net.minecraft.client.Minecraft minecraft)) return;
        
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        
        // Convert block positions to world coordinates, offset by camera position
        double startX = startPos.getX() + 0.5 - cameraPos.x;
        double startY = startPos.getY() + config.getYOffset() - cameraPos.y;
        double startZ = startPos.getZ() + 0.5 - cameraPos.z;
        double endX = endPos.getX() + 0.5 - cameraPos.x;
        double endY = endPos.getY() + config.getYOffset() - cameraPos.y;
        double endZ = endPos.getZ() + 0.5 - cameraPos.z;
        
        renderLine3D(poseStack, startX, startY, startZ, endX, endY, endZ, color, config);
    }
    
    /**
     * Core 3D line rendering implementation using rectangular prism geometry
     * 
     * @param poseStack The pose stack for transformations
     * @param startX Starting X coordinate (camera-relative)
     * @param startY Starting Y coordinate (camera-relative)
     * @param startZ Starting Z coordinate (camera-relative)
     * @param endX Ending X coordinate (camera-relative)
     * @param endY Ending Y coordinate (camera-relative)
     * @param endZ Ending Z coordinate (camera-relative)
     * @param color Line color
     * @param config Line configuration
     */
    private static void renderLine3D(PoseStack poseStack, double startX, double startY, double startZ,
                                    double endX, double endY, double endZ, Color color, LineConfig config) {
        
        // Setup rendering state
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        if (config.isBlendEnabled()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
        
        RenderSystem.disableCull();
        
        if (config.isDepthTestEnabled()) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }

        // Create buffer and tessellator
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        // Calculate direction and perpendiculars for 3D quad thickness
        double thickness = config.getThickness();
        Vec3 direction = new Vec3(endX - startX, endY - startY, endZ - startZ);
        
        // Prevent zero-length direction vector
        if (direction.length() < 0.001) {
            direction = new Vec3(1, 0, 0); // Default direction
        } else {
            direction = direction.normalize();
        }
        
        // Create two perpendicular vectors for true 3D thickness
        Vec3 perp1, perp2;
        
        // First perpendicular: horizontal (X-Z plane)
        if (Math.abs(direction.y) < 0.9) {
            // For non-vertical lines, use horizontal perpendicular
            perp1 = new Vec3(-direction.z, 0, direction.x).normalize().scale(thickness / 2.0);
        } else {
            // For near-vertical lines, use X-axis perpendicular
            perp1 = new Vec3(1, 0, 0).normalize().scale(thickness / 2.0);
        }
        
        // Second perpendicular: ensure it's truly perpendicular to both direction and perp1
        perp2 = direction.cross(perp1).normalize().scale(thickness / 2.0);
        
        // Debug: ensure we have proper perpendiculars
        if (perp1.length() < 0.001 || perp2.length() < 0.001) {
            // Fallback to axis-aligned perpendiculars
            perp1 = new Vec3(thickness / 2.0, 0, 0);
            perp2 = new Vec3(0, thickness / 2.0, 0);
        }

        // Render based on style
        switch (config.getStyle()) {
            case SOLID:
                renderSolidLine(buffer, matrix, startX, startY, startZ, endX, endY, endZ, 
                               perp1, perp2, color);
                break;
            case DASHED:
                // Future implementation: render dashed line
                renderSolidLine(buffer, matrix, startX, startY, startZ, endX, endY, endZ, 
                               perp1, perp2, color);
                break;
            case ANIMATED:
                // Future implementation: render animated line
                renderSolidLine(buffer, matrix, startX, startY, startZ, endX, endY, endZ, 
                               perp1, perp2, color);
                break;
        }

        tesselator.end();
        poseStack.popPose();

        // Restore rendering state
        if (config.isBlendEnabled()) {
            RenderSystem.disableBlend();
        }
        RenderSystem.enableCull();
        if (!config.isDepthTestEnabled()) {
            RenderSystem.enableDepthTest();
        }
    }
    
    /**
     * Renders a solid rectangular prism line using 6 quad faces
     */
    private static void renderSolidLine(BufferBuilder buffer, Matrix4f matrix, 
                                       double startX, double startY, double startZ,
                                       double endX, double endY, double endZ,
                                       Vec3 perp1, Vec3 perp2, Color color) {
        
        // Render as 3D quad (rectangular prism) by drawing multiple faces
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Calculate all 8 vertices of the rectangular prism
        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = new Vec3(endX, endY, endZ);
        
        Vec3 v1 = start.add(perp1).add(perp2);
        Vec3 v2 = start.add(perp1).subtract(perp2);
        Vec3 v3 = start.subtract(perp1).subtract(perp2);
        Vec3 v4 = start.subtract(perp1).add(perp2);
        
        Vec3 v5 = end.add(perp1).add(perp2);
        Vec3 v6 = end.add(perp1).subtract(perp2);
        Vec3 v7 = end.subtract(perp1).subtract(perp2);
        Vec3 v8 = end.subtract(perp1).add(perp2);

        // Render 6 faces of the rectangular prism with consistent counter-clockwise winding
        // Face 1: Top (perp2 positive)
        buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v5.x, (float)v5.y, (float)v5.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v8.x, (float)v8.y, (float)v8.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).color(color.r, color.g, color.b, color.a).endVertex();

        // Face 2: Bottom (perp2 negative)  
        buffer.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v7.x, (float)v7.y, (float)v7.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v6.x, (float)v6.y, (float)v6.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color.r, color.g, color.b, color.a).endVertex();

        // Face 3: Right (perp1 positive)
        buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v6.x, (float)v6.y, (float)v6.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v5.x, (float)v5.y, (float)v5.z).color(color.r, color.g, color.b, color.a).endVertex();

        // Face 4: Left (perp1 negative)
        buffer.vertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v8.x, (float)v8.y, (float)v8.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v7.x, (float)v7.y, (float)v7.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color.r, color.g, color.b, color.a).endVertex();

        // Face 5: Start cap
        buffer.vertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color.r, color.g, color.b, color.a).endVertex();

        // Face 6: End cap
        buffer.vertex(matrix, (float)v5.x, (float)v5.y, (float)v5.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v6.x, (float)v6.y, (float)v6.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v7.x, (float)v7.y, (float)v7.z).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.vertex(matrix, (float)v8.x, (float)v8.y, (float)v8.z).color(color.r, color.g, color.b, color.a).endVertex();
    }
}
