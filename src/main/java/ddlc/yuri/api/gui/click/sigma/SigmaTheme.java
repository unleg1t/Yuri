package ddlc.yuri.api.gui.click.sigma;

import net.minecraft.util.ResourceLocation;

import java.awt.Color;

public final class SigmaTheme {

    public static final Color DEEP_TEAL = new Color(1, 1, 1, 255);
    public static final Color LIGHT_GREYISH_BLUE = new Color(254, 254, 254, 255);
    public static final Color ENABLED_BLUE = new Color(41, 166, 255, 255);
    public static final Color ENABLED_BLUE_HOVER = new Color(41, 184, 255, 255);
    public static final Color DISABLED_BG = new Color(254, 254, 245, 112);
    public static final Color DISABLED_BG_HOVER = new Color(202, 202, 202, 0);
    public static final Color CHECKBOX_GREY = new Color(192, 192, 192, 255);
    public static final Color CHECKBOX_TRACK = new Color(59, 153, 253, 255);
    public static final Color MID_GREY = new Color(153, 153, 153, 255);
    public static final Color SCROLLBAR = new Color(1, 1, 1, 255);

    public static final float PANEL_WIDTH = 200f;
    public static final float PANEL_HEIGHT = 320f;
    public static final float PANEL_SOURCE_HEIGHT = 350f;
    public static final float HEADER_HEIGHT = 60f;
    public static final float MODULE_HEIGHT = 30f;
    public static final float GLOW_RADIUS = 20f;
    public static final float PANEL_GAP = 10f;
    public static final float PANEL_START_X = 30f;
    public static final float PANEL_START_Y = 30f;

    public static final float SETTINGS_WIDTH = 500f;
    public static final float SETTINGS_MAX_HEIGHT = 600f;
    public static final float SETTINGS_RADIUS = 10f;
    public static final float SETTING_X = 20f;
    public static final float SETTING_PAD = 20f;
    public static final float SETTING_START_Y = 20f;

    public static final String LIGHT_FONT = "jello-light";
    public static final String MEDIUM_FONT = "jello-medium";

    public static final ResourceLocation SHADOW_CORNER_1 = new ResourceLocation("yuri/images/sigma/shadow_corner.png");
    public static final ResourceLocation SHADOW_CORNER_2 = new ResourceLocation("yuri/images/sigma/shadow_corner_2.png");
    public static final ResourceLocation SHADOW_CORNER_3 = new ResourceLocation("yuri/images/sigma/shadow_corner_3.png");
    public static final ResourceLocation SHADOW_CORNER_4 = new ResourceLocation("yuri/images/sigma/shadow_corner_4.png");
    public static final ResourceLocation SHADOW_LEFT = new ResourceLocation("yuri/images/sigma/shadow_left.png");
    public static final ResourceLocation SHADOW_RIGHT = new ResourceLocation("yuri/images/sigma/shadow_right.png");
    public static final ResourceLocation SHADOW_TOP = new ResourceLocation("yuri/images/sigma/shadow_top.png");
    public static final ResourceLocation SHADOW_BOTTOM = new ResourceLocation("yuri/images/sigma/shadow_bottom.png");
    public static final ResourceLocation CHECK = new ResourceLocation("yuri/images/sigma/check.png");
    public static final ResourceLocation OPTIONS = new ResourceLocation("yuri/images/sigma/options.png");
    public static final ResourceLocation SCROLLBAR_TOP = new ResourceLocation("yuri/images/sigma/scrollbar_top.png");
    public static final ResourceLocation SCROLLBAR_BOTTOM = new ResourceLocation("yuri/images/sigma/scrollbar_bottom.png");

    private SigmaTheme() {
    }

    public static int applyAlpha(Color color, float alpha) {
        return applyAlpha(color.getRGB(), alpha);
    }

    public static int applyAlpha(int color, float alpha) {
        alpha = Math.max(0f, Math.min(1f, alpha));
        return (int) (alpha * 255f) << 24 | color & 0xFFFFFF;
    }

    public static int blend(int color1, int color2, float factor) {
        int a1 = color1 >> 24 & 0xFF;
        int r1 = color1 >> 16 & 0xFF;
        int g1 = color1 >> 8 & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = color2 >> 24 & 0xFF;
        int r2 = color2 >> 16 & 0xFF;
        int g2 = color2 >> 8 & 0xFF;
        int b2 = color2 & 0xFF;
        float inverse = 1f - factor;
        return (int) (a1 * factor + a2 * inverse) << 24
                | ((int) (r1 * factor + r2 * inverse) & 0xFF) << 16
                | ((int) (g1 * factor + g2 * inverse) & 0xFF) << 8
                | (int) (b1 * factor + b2 * inverse) & 0xFF;
    }

    public static int darker(int color, float factor) {
        return blend(color, 0xFF000000, factor);
    }

    public static int lighter(int color, float factor) {
        int alpha = color >> 24 & 0xFF;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int r = (int) (red + (255 - red) * factor);
        int g = (int) (green + (255 - green) * factor);
        int b = (int) (blue + (255 - blue) * factor);
        return alpha << 24 | r << 16 | g << 8 | b;
    }
}
