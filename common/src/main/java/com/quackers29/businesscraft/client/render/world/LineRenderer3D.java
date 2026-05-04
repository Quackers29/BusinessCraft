package com.quackers29.businesscraft.client.render.world;

import com.quackers29.businesscraft.api.ClientHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class LineRenderer3D {

    public static final float DEFAULT_THICKNESS = 0.05f;
    public static final float DEFAULT_Y_OFFSET = 1.1f;

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

        public float getThickness() { return thickness; }
        public float getYOffset() { return yOffset; }
        public LineStyle getStyle() { return style; }
        public boolean isDepthTestEnabled() { return enableDepthTest; }
        public boolean isBlendEnabled() { return enableBlend; }
    }

    public enum LineStyle {
        SOLID,
        DASHED,
        ANIMATED
    }

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

        public static final Color RED = new Color(1.0f, 0.0f, 0.0f, 0.8f);
        public static final Color GREEN = new Color(0.0f, 1.0f, 0.0f, 0.8f);
        public static final Color BLUE = new Color(0.0f, 0.0f, 1.0f, 0.8f);
        public static final Color ORANGE = new Color(1.0f, 0.3f, 0.0f, 0.8f);
        public static final Color WHITE = new Color(1.0f, 1.0f, 1.0f, 0.8f);
        public static final Color YELLOW = new Color(1.0f, 1.0f, 0.0f, 0.8f);
    }

    public static void renderLine(PoseStack poseStack, BlockPos startPos, BlockPos endPos, Color color) {
        renderLine(poseStack, startPos, endPos, color, new LineConfig());
    }

    public static void renderLine(PoseStack poseStack, Vec3 startWorld, Vec3 endWorld, Color color, LineConfig config) {
        Minecraft minecraft = clientMinecraftOrNull();
        if (minecraft == null) return;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        double startX = startWorld.x - cameraPos.x;
        double startY = startWorld.y + config.getYOffset() - cameraPos.y;
        double startZ = startWorld.z - cameraPos.z;
        double endX = endWorld.x - cameraPos.x;
        double endY = endWorld.y + config.getYOffset() - cameraPos.y;
        double endZ = endWorld.z - cameraPos.z;

        renderLine3D(poseStack, startX, startY, startZ, endX, endY, endZ, color, config);
    }

    public static void renderLine(PoseStack poseStack, BlockPos startPos, BlockPos endPos, Color color, LineConfig config) {
        Minecraft minecraft = clientMinecraftOrNull();
        if (minecraft == null) return;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        double startX = startPos.getX() + 0.5 - cameraPos.x;
        double startY = startPos.getY() + config.getYOffset() - cameraPos.y;
        double startZ = startPos.getZ() + 0.5 - cameraPos.z;
        double endX = endPos.getX() + 0.5 - cameraPos.x;
        double endY = endPos.getY() + config.getYOffset() - cameraPos.y;
        double endZ = endPos.getZ() + 0.5 - cameraPos.z;

        renderLine3D(poseStack, startX, startY, startZ, endX, endY, endZ, color, config);
    }

    private static Minecraft clientMinecraftOrNull() {
        ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) return null;
        Object minecraftObj = clientHelper.getMinecraft();
        if (!(minecraftObj instanceof Minecraft minecraft)) return null;
        return minecraft;
    }

    private static void renderLine3D(PoseStack poseStack, double startX, double startY, double startZ,
                                    double endX, double endY, double endZ, Color color, LineConfig config) {

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

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        double thickness = config.getThickness();
        Vec3 direction = new Vec3(endX - startX, endY - startY, endZ - startZ);

        if (direction.length() < 0.001) {
            direction = new Vec3(1, 0, 0);
        } else {
            direction = direction.normalize();
        }

        Vec3 perp1;
        Vec3 perp2;

        if (Math.abs(direction.y) < 0.9) {
            perp1 = new Vec3(-direction.z, 0, direction.x).normalize().scale(thickness / 2.0);
        } else {
            perp1 = new Vec3(1, 0, 0).normalize().scale(thickness / 2.0);
        }

        perp2 = direction.cross(perp1).normalize().scale(thickness / 2.0);

        if (perp1.length() < 0.001 || perp2.length() < 0.001) {
            perp1 = new Vec3(thickness / 2.0, 0, 0);
            perp2 = new Vec3(0, thickness / 2.0, 0);
        }

        renderSolidLine(buffer, matrix, startX, startY, startZ, endX, endY, endZ, perp1, perp2, color);

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
