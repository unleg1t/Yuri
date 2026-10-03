package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class SigmaRenderer {

    private static float scale = 1f;

    private SigmaRenderer() {
    }

    public static void setScale(float value) {
        scale = value;
    }

    public static int s(float value) {
        return Math.round(value * scale);
    }

    public static void glow(float x, float y, float width, float height, float radius, float alpha) {
        if (alpha <= 0.001f) {
            return;
        }
        int color = SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha);
        image(SigmaTheme.SHADOW_CORNER_1, x - radius, y - radius, radius, radius, color);
        image(SigmaTheme.SHADOW_CORNER_2, x + width, y - radius, radius, radius, color);
        image(SigmaTheme.SHADOW_CORNER_3, x - radius, y + height, radius, radius, color);
        image(SigmaTheme.SHADOW_CORNER_4, x + width, y + height, radius, radius, color);
        image(SigmaTheme.SHADOW_LEFT, x - radius, y, radius, height, color);
        image(SigmaTheme.SHADOW_RIGHT, x + width, y, radius, height, color);
        image(SigmaTheme.SHADOW_TOP, x, y - radius, width, radius, color);
        image(SigmaTheme.SHADOW_BOTTOM, x, y + height, width, radius, color);
    }

    public static void image(ResourceLocation texture, float x, float y, float width, float height, int color) {
        if (width <= 0f || height <= 0f) {
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0.0f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        renderer.pos(x * scale, (y + height) * scale, 0.0).tex(0.0, 1.0).color(color).endVertex();
        renderer.pos((x + width) * scale, (y + height) * scale, 0.0).tex(1.0, 1.0).color(color).endVertex();
        renderer.pos((x + width) * scale, y * scale, 0.0).tex(1.0, 0.0).color(color).endVertex();
        renderer.pos(x * scale, y * scale, 0.0).tex(0.0, 0.0).color(color).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    public static void image(ResourceLocation texture, float x, float y, float width, float height, float alpha) {
        image(texture, x, y, width, height, SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha));
    }

    public static void rect(float x1, float y1, float x2, float y2, int color) {
        Gui.drawRect(s(x1), s(y1), s(x2), s(y2), color);
    }

    public static void roundRect(float x, float y, float width, float height, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255f;
        if (alpha <= 0.001f || width <= 0f || height <= 0f) {
            return;
        }
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;

        float left = x * scale;
        float top = y * scale;
        float w = width * scale;
        float h = height * scale;
        float r = Math.max(0f, Math.min(radius * scale, Math.min(w, h) / 2f));

        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(red, green, blue, alpha);

        if (r > 0.5f) {
            fillQuad(left, top + r, left + w, top + h - r);
            fillQuad(left + r, top, left + w - r, top + r);
            fillQuad(left + r, top + h - r, left + w - r, top + h);
            quarterDisc(left + r, top + r, r, 180f, 270f);
            quarterDisc(left + w - r, top + r, r, 270f, 360f);
            quarterDisc(left + w - r, top + h - r, r, 0f, 90f);
            quarterDisc(left + r, top + h - r, r, 90f, 180f);
        } else {
            fillQuad(left, top, left + w, top + h);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void fillQuad(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
    }

    private static void quarterDisc(float cx, float cy, float radius, float startAngle, float endAngle) {
        int segments = 8;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / segments);
            GL11.glVertex2d(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    public static void filledCircle(float cx, float cy, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255f;
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        float x = cx * scale;
        float y = cy * scale;
        float r = radius * scale;
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            GL11.glVertex2d(x + Math.cos(angle) * r, y + Math.sin(angle) * r);
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void scissor(float x1, float y1, float x2, float y2) {
        Minecraft mc = Minecraft.getMinecraft();
        int minX = Math.round(Math.min(x1, x2));
        int maxX = Math.round(Math.max(x1, x2));
        int minY = Math.round(Math.min(y1, y2));
        int maxY = Math.round(Math.max(y1, y2));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(minX, Math.round(mc.displayHeight - maxY), Math.max(0, maxX - minX), Math.max(0, maxY - minY));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static float transformCoord(float value, float center, float scaleFactor, float offset) {
        return center + scaleFactor * (value - center + offset);
    }

    public static void font(String name, float size, float x, float y, String text, int color) {
        CustomFontRenderer font = FontUtils.getFont(name, fontAtlasSize(size));
        if (font == null) {
            return;
        }
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0.0f);
        GL11.glPushMatrix();
        GL11.glTranslatef(Math.round(x) * scale, (Math.round(y) + 2f) * scale, 0f);
        GL11.glScalef(scale * 2f, scale * 2f, 1f);
        font.drawString(text, 0f, 0f, color);
        GL11.glPopMatrix();
    }

    public static float fontWidth(String name, float size, String text) {
        CustomFontRenderer font = FontUtils.getFont(name, fontAtlasSize(size));
        return font == null ? 0f : font.getStringWidth(text) * 2f;
    }

    public static float fontHeight(String name, float size) {
        CustomFontRenderer font = FontUtils.getFont(name, fontAtlasSize(size));
        return font == null ? 0f : font.getHeight() * 2f + 8f;
    }

    private static int fontAtlasSize(float size) {
        return Math.max(1, Math.round(size * 0.75f));
    }
}
