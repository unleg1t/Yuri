package ddlc.yuri.utils.render.adapters;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public final class FontAdapter {

    private final CustomFontRenderer custom;
    private final boolean vanilla;
    private final float scale;

    private FontAdapter(CustomFontRenderer custom, boolean vanilla, int size) {
        this.custom = custom;
        this.vanilla = vanilla;
        if (vanilla && !(size >= 18 && size <= 21)) {
            this.scale = custom.getHeight() / (float) Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        } else {
            this.scale = 1f;
        }
    }

    public static FontAdapter of(String name, int size, boolean useCustom) {
        CustomFontRenderer font = FontUtils.getFont(name, size);
        if (font == null) return null;
        return new FontAdapter(font, !useCustom, size);
    }

    public float width(String text) {
        if (vanilla) return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) * scale;
        return custom.getStringWidth(text);
    }

    public float height() {
        if (vanilla) return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT * scale;
        return custom.getHeight();
    }

    public void draw(String text, float x, float y, int color) {
        if (!vanilla) {
            custom.drawStringWithShadow(text, x, y, color);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(scale, scale, 1f);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, 0f, 0f, color);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}