package ddlc.yuri.utils.render;

import ddlc.yuri.managers.impl.ColorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static ddlc.yuri.utils.misc.IMinecraft.mc;
import static org.lwjgl.opengl.GL11.*;

public class RenderUtils {
    public static float delta = 0f;

    private static float scissorTransformScale = 1.0f;
    private static float scissorTransformOriginX;
    private static float scissorTransformOriginY;

    // 3d !! (I hate this fucking bullshit)

    public static void setupOrientationMatrix(double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();

        // Translate relative to the player's current interpolated view coordinates
        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x - renderPosX, y - renderPosY, z - renderPosZ);

        // Rotate to face the camera (billboarding effect)
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
    }

    public static void drawBoundingBox(final AxisAlignedBB a) {
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.endVertex();
        tessellator.draw();
    }

    public static void drawOutlinedBoundingBox(final AxisAlignedBB a) {
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.minY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.maxZ).endVertex();
        worldrenderer.pos((float) a.maxX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.pos((float) a.minX, (float) a.maxY, (float) a.minZ).endVertex();
        worldrenderer.endVertex();
        tessellator.draw();
    }

    public static void renderBed(BlockPos[] array) {
        Color ct = ColorManager.getColor();
        drawFilledBlock(array[0], ct.getRGB());
        drawFilledBlock(array[1], ct.getRGB());
    }

    public static void drawFilledBlock(BlockPos blockPos, int color) {
        double deltaX = blockPos.getX();
        double deltaY = blockPos.getY();
        double deltaZ = blockPos.getZ();
        drawFilledAABB(new AxisAlignedBB(deltaX, deltaY, deltaZ, deltaX + 1.0, deltaY + 1.0, deltaZ + 1.0), color);
    }

    public static void drawFilledAABB(AxisAlignedBB aabb, int color) {
        aabb = aabb.offset(- mc.getRenderManager().viewerPosX, - mc.getRenderManager().viewerPosY, - mc.getRenderManager().viewerPosZ);

        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        RenderHelper.drawCompleteBoxFilled(aabb, 1.0F, color);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    public static void renderPlayerPosition(double x, double y, double z) {
        AxisAlignedBB bb = new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
        GLUtils.start3D();
        GlStateManager.pushMatrix();
        GlStateManager.color(ColorManager.getColor().getRed() / 255f, ColorManager.getColor().getGreen() / 255f,
                ColorManager.getColor().getBlue() / 255f, 80 / 255f);
        RenderUtils.drawBoundingBox(bb);
        GlStateManager.popMatrix();
        GLUtils.stop3D();
    }

    // 2d rendering

    public static void customRotatedObject2D(float x, float y, float width, float height, double rotate) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + width / 2f, y + height / 2f, 0);
        GlStateManager.rotate((float) rotate, 0.0f, 0.0f, 1.0f);
        GlStateManager.translate(-(x + width / 2f), -(y + height / 2f), 0);
    }

    public static void drawGradientRect(double left, double top, double right, double bottom,
                                        boolean sideways,
                                        int startColor, int endColor) {

        float sa = (startColor >> 24 & 255) / 255F;
        float sr = (startColor >> 16 & 255) / 255F;
        float sg = (startColor >> 8 & 255) / 255F;
        float sb = (startColor & 255) / 255F;

        float ea = (endColor >> 24 & 255) / 255F;
        float er = (endColor >> 16 & 255) / 255F;
        float eg = (endColor >> 8 & 255) / 255F;
        float eb = (endColor & 255) / 255F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);

        if (sideways) {
            GlStateManager.color(sr, sg, sb, sa);
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(left, bottom);

            GlStateManager.color(er, eg, eb, ea);
            GL11.glVertex2d(right, bottom);
            GL11.glVertex2d(right, top);
        } else {
            GlStateManager.color(sr, sg, sb, sa);
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(right, top);

            GlStateManager.color(er, eg, eb, ea);
            GL11.glVertex2d(right, bottom);
            GL11.glVertex2d(left, bottom);
        }

        GL11.glEnd();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();

        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawCenteredGradientRect(double left, double top, double right, double bottom,
                                                int edgeColor, int centerColor) {
        float ea = (edgeColor >> 24 & 255) / 255F;
        float er = (edgeColor >> 16 & 255) / 255F;
        float eg = (edgeColor >> 8 & 255) / 255F;
        float eb = (edgeColor & 255) / 255F;

        float ca = (centerColor >> 24 & 255) / 255F;
        float cr = (centerColor >> 16 & 255) / 255F;
        float cg = (centerColor >> 8 & 255) / 255F;
        float cb = (centerColor & 255) / 255F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);

        double midX = left + (right - left) / 2.0;

        GlStateManager.color(er, eg, eb, ea);
        GL11.glVertex2d(left, top);
        GL11.glVertex2d(left, bottom);

        GlStateManager.color(cr, cg, cb, ca);
        GL11.glVertex2d(midX, bottom);
        GL11.glVertex2d(midX, top);

        GlStateManager.color(cr, cg, cb, ca);
        GL11.glVertex2d(midX, top);
        GL11.glVertex2d(midX, bottom);

        GlStateManager.color(er, eg, eb, ea);
        GL11.glVertex2d(right, bottom);
        GL11.glVertex2d(right, top);

        GL11.glEnd();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float imgWidth, float imgHeight) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(resourceLocation);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
        GlStateManager.disableBlend();
    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float croppedX, float croppedY, float croppedWidth, float croppedHeight, float imgWidth, float imgHeight) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(resourceLocation);
        Gui.drawModalRectWithCustomSizedTexture(x, y, croppedX, croppedY, croppedWidth, croppedHeight, imgWidth, imgHeight);
        GlStateManager.disableBlend();
    }

    public static void drawImage(ResourceLocation image, double x, double y, double z, float width, float height, Color color1, Color color2, Color color3, Color color4) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(image);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH); // Enables smooth gradient coloring across vertices

        // If you are rendering this in world-space (since it uses a z coordinate and angles),
        // ensure your setupOrientationMatrix is called before this.
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        // Top-Left
        worldrenderer.pos(x, y + height, z).tex(0.0, 1.0).color(color1.getRed(), color1.getGreen(), color1.getBlue(), color1.getAlpha()).endVertex();
        // Top-Right
        worldrenderer.pos(x + width, y + height, z).tex(1.0, 1.0).color(color2.getRed(), color2.getGreen(), color2.getBlue(), color2.getAlpha()).endVertex();
        // Bottom-Right
        worldrenderer.pos(x + width, y, z).tex(1.0, 0.0).color(color3.getRed(), color3.getGreen(), color3.getBlue(), color3.getAlpha()).endVertex();
        // Bottom-Left
        worldrenderer.pos(x, y, z).tex(0.0, 0.0).color(color4.getRed(), color4.getGreen(), color4.getBlue(), color4.getAlpha()).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    public static void drawImage(ResourceLocation resource, float x, float y, float x2, float y2, int c) {
        mc.getTextureManager().bindTexture(resource);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(9, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldRenderer.pos(x, y2, 0.0).tex(0.0, 1.0).color(c).endVertex();
        worldRenderer.pos(x2, y2, 0.0).tex(1.0, 1.0).color(c).endVertex();
        worldRenderer.pos(x2, y, 0.0).tex(1.0, 0.0).color(c).endVertex();
        worldRenderer.pos(x, y, 0.0).tex(0.0, 0.0).color(c).endVertex();
        GL11.glShadeModel(7425);
        GL11.glDepthMask(false);
        tessellator.draw();
        GL11.glDepthMask(true);
        GL11.glShadeModel(7424);
    }


    public static void drawGif(GifTexture gif, float x, float y, float imgWidth, float imgHeight) {
        if (gif == null) return;
        drawImage(gif.getCurrentFrame(), x, y, imgWidth, imgHeight);
    }

    public static void drawArrow(float x, float y, float size, int color, double rotation) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = (color >> 24 & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.rotate((float) rotation, 0.0F, 0.0F, 1.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(-size, -size / 2.0F);
        GL11.glVertex2f(size, 0.0F);
        GL11.glVertex2f(-size, size / 2.0F);
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static void drawCheck(float x, float y, float size, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
        GL11.glLineWidth(1.5F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + size, y + size);
        GL11.glVertex2f(x + size * 2.5F, y - size);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
    }

    public static void drawBorderedRect(float x, float y, float width, float height, final float outlineThickness, int rectColor, int outlineColor, boolean top, boolean right, boolean bottom, boolean left) {
        Gui.drawRect2(x, y, width, height, rectColor);
        glEnable(GL_LINE_SMOOTH);
        RenderUtils.color(outlineColor);

        GLUtils.setup2DRendering();

        glLineWidth(outlineThickness);
        float cornerValue = (float) (outlineThickness * .19);

        glBegin(GL_LINES);
        glVertex2d(x, y);
        glVertex2d(x, left ? y + height + cornerValue : y);
        glVertex2d(x + width, y + height + cornerValue);
        glVertex2d(x + width, right ? y - cornerValue : y + height + cornerValue);
        glVertex2d(x, y);
        glVertex2d(top ? x + width : x, y);
        glVertex2d(x, y + height);
        glVertex2d(bottom ? x + width : x, y + height);
        glEnd();

        GLUtils.end2DRendering();

        glDisable(GL_LINE_SMOOTH);
    }

    public static void drawBorderedGradientRect(float x, float y, float width, float height, float radius, float thickness, int bgColor, Color gradientStart, Color gradientEnd) {
        RoundedUtils.drawCustomRoundedRect(x, y, width, height, radius, true, true, true, true, new Color(bgColor, true));

        List<float[]> outerPath = buildRoundedRectPath(x, y, width, height, radius);
        List<float[]> innerPath = buildRoundedRectPath(x + thickness, y + thickness, width - thickness * 2, height - thickness * 2, Math.max(0, radius - thickness));

        float totalLength = pathLength(outerPath);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);

        float distance = 0f;
        int size = outerPath.size();
        for (int i = 0; i < size; i++) {
            float[] outerA = outerPath.get(i);
            float[] outerB = outerPath.get((i + 1) % size);
            float[] innerA = innerPath.get(i % innerPath.size());
            float[] innerB = innerPath.get((i + 1) % innerPath.size());

            float segLength = dist(outerA, outerB);
            float tA = distance / totalLength;
            distance += segLength;
            float tB = distance / totalLength;

            Color colorA = interpolateColorC(gradientStart, gradientEnd, triangleWave(tA));
            Color colorB = interpolateColorC(gradientStart, gradientEnd, triangleWave(tB));

            setGlColor(colorA);
            GL11.glVertex2f(outerA[0], outerA[1]);
            GL11.glVertex2f(innerA[0], innerA[1]);

            setGlColor(colorB);
            GL11.glVertex2f(innerB[0], innerB[1]);
            GL11.glVertex2f(outerB[0], outerB[1]);
        }

        GL11.glEnd();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static List<float[]> buildRoundedRectPath(float x, float y, float width, float height, float radius) {
        List<float[]> points = new ArrayList<>();
        int segments = 12;
        radius = Math.min(radius, Math.min(width, height) / 2f);

        float[][] corners = {
                {x + radius, y + radius, 180, 270},
                {x + width - radius, y + radius, 270, 360},
                {x + width - radius, y + height - radius, 0, 90},
                {x + radius, y + height - radius, 90, 180}
        };

        for (float[] corner : corners) {
            float cx = corner[0];
            float cy = corner[1];
            float startAngle = corner[2];
            float endAngle = corner[3];
            for (int i = 0; i <= segments; i++) {
                float angle = (float) Math.toRadians(startAngle + (endAngle - startAngle) * i / segments);
                points.add(new float[]{cx + radius * (float) Math.cos(angle), cy + radius * (float) Math.sin(angle)});
            }
        }

        return points;
    }

    private static float pathLength(List<float[]> path) {
        float length = 0f;
        for (int i = 0; i < path.size(); i++) {
            length += dist(path.get(i), path.get((i + 1) % path.size()));
        }
        return length;
    }

    private static float dist(float[] a, float[] b) {
        float dx = b[0] - a[0];
        float dy = b[1] - a[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float triangleWave(float t) {
        float wrapped = t % 1f;
        return wrapped <= 0.5f ? wrapped * 2f : (1f - wrapped) * 2f;
    }

    public static void drawCircle(float cx, float cy, float r, int num_segments, int c) {
        GL11.glPushMatrix();
        cx *= 2.0F;
        cy *= 2.0F;
        float f = (c >> 24 & 0xFF) / 255.0F;
        float f1 = (c >> 16 & 0xFF) / 255.0F;
        float f2 = (c >> 8 & 0xFF) / 255.0F;
        float f3 = (c & 0xFF) / 255.0F;
        float theta = (float) (6.2831852D / num_segments);
        float p = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);
        float x = r *= 2.0F;
        float y = 0.0F;
        GLUtils.setup2DRendering();
        GL11.glLineWidth(1.5F);
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        GL11.glColor4f(f1, f2, f3, f);
        GL11.glBegin(GL_LINE_LOOP);
        int ii = 0;
        while (ii < num_segments) {
            GL11.glVertex2f(x + cx, y + cy);
            float t = x;
            x = p * x - s * y;
            y = s * t + p * y;
            ii++;
        }
        GL11.glEnd();
        GL11.glScalef(2.0F, 2.0F, 2.0F);
        GLUtils.end2DRendering();
        GL11.glPopMatrix();
    }

    // color shit

    private static void setGlColor(Color color) {
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public static void color(int color, float alpha) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, alpha);
    }

    public static void color(int color) {
        color(color, (float) (color >> 24 & 255) / 255.0F);
    }

    public static Color astolfoColors(int yOffset, int yTotal) {
        float speed = 2900F;
        float hue = (float) (System.currentTimeMillis() % (int) speed) + ((yTotal - yOffset) * 9);
        while (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5) {
            hue = 0.5F - (hue - 0.5f);
        }
        hue += 0.5F;
        return new Color(Color.HSBtoRGB(hue, 0.5f, 1F));
    }

    public static float interpolate(float old,
                                    float now,
                                    float partialTicks) {

        return old + (now - old) * partialTicks;
    }


    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int applyOpacity(int color, float opacity) {
        Color old = new Color(color);
        return applyOpacity(old, opacity).getRGB();
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount),
                interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        return applyOpacity(resultColor, interpolateInt(color1.getAlpha(), color2.getAlpha(), amount) / 255f);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        // Clamp speed between 1 and 10 to avoid invalid inputs or divide-by-zero
        int clampedSpeed = Math.max(1, Math.min(10, speed));

        // Convert 1-10 speed to a step multiplier (e.g., speed 1 = 0.05x, speed 10 = 0.50x)
        double speedMultiplier = clampedSpeed * 0.05;

        int angle = (int) ((System.currentTimeMillis() * speedMultiplier + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;

        return trueColor ? interpolateColorHue(start, end, angle / 360f) : interpolateColorC(start, end, angle / 360f);
    }

    public static int interpolateColor(int from, int to, float fraction) {
        return interpolateColor(new Color(from, true), new Color(to, true), fraction);
    }

    public static int interpolateColor(Color from, Color to, float fraction) {
        fraction = clamp(fraction, 0.0F, 1.0F);
        int red = (int) (from.getRed() + (to.getRed() - from.getRed()) * fraction);
        int green = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * fraction);
        int blue = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * fraction);
        int alpha = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * fraction);
        return new Color(red, green, blue, alpha).getRGB();
    }

    public static Color withAlphaColor(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static int withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB();
    }

    public static double incValue(double value, double increment) {
        if (increment <= 0.0D) {
            return value;
        }
        return Math.round(value / increment) * increment;
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    // gay ass gl shiz

    public static void setAlphaLimit(float limit) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (limit * .01));
    }

    public static void setScissorTransform(float scale, float originX, float originY) {
        scissorTransformScale = scale;
        scissorTransformOriginX = originX;
        scissorTransformOriginY = originY;
    }

    public static void clearScissorTransform() {
        scissorTransformScale = 1.0f;
        scissorTransformOriginX = 0.0f;
        scissorTransformOriginY = 0.0f;
    }

    public static float getScissorTransformScale() {
        return scissorTransformScale;
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        if (needsNewFramebuffer(framebuffer)) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
        }
        return framebuffer;
    }

    public static boolean needsNewFramebuffer(Framebuffer framebuffer) {
        return framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight;
    }

    public static void resetColor() {
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void bindTexture(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);
    }

    public static class GifTexture {
        private final List<ResourceLocation> frames = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private long lastFrameTime = System.currentTimeMillis();
        private int currentFrame = 0;

        public GifTexture(InputStream inputStream) {
            try (ImageInputStream stream = ImageIO.createImageInputStream(inputStream)) {
                ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
                reader.setInput(stream);

                int count = reader.getNumImages(true);
                for (int i = 0; i < count; i++) {
                    BufferedImage frame = reader.read(i);
                    DynamicTexture dynamicTex = new DynamicTexture(frame);
                    ResourceLocation loc = mc.getTextureManager()
                            .getDynamicTextureLocation("gif_frame_" + System.nanoTime() + "_" + i, dynamicTex);
                    frames.add(loc);

                    int delayMs = 100;
                    IIOMetadata metadata = reader.getImageMetadata(i);
                    String metaFormat = metadata.getNativeMetadataFormatName();
                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormat);
                    IIOMetadataNode gce = findNode(root, "GraphicControlExtension");

                    if (gce != null && gce.hasAttribute("delayTime")) {
                        int delayVal = Integer.parseInt(gce.getAttribute("delayTime")) * 10;
                        if (delayVal > 0) delayMs = delayVal;
                    }
                    delays.add(delayMs);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public int getFrameCount() {
            return frames.size();
        }

        public ResourceLocation getCurrentFrame() {
            if (frames.isEmpty()) return null;

            long now = System.currentTimeMillis();
            if (now - lastFrameTime >= delays.get(currentFrame)) {
                currentFrame = (currentFrame + 1) % frames.size();
                lastFrameTime = now;
            }

            return frames.get(currentFrame);
        }

        public void clear() {
            frames.clear();
            delays.clear();
        }

        private IIOMetadataNode findNode(IIOMetadataNode root, String name) {
            for (int i = 0; i < root.getLength(); i++) {
                if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                    return (IIOMetadataNode) root.item(i);
                }
            }
            return null;
        }
    }
}