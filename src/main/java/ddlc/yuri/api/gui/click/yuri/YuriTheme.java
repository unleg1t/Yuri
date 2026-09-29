package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.managers.impl.ColorManager;

import java.awt.Color;

public final class YuriTheme {

    public static final float RADIUS = 12f;
    public static final float PANEL_WIDTH = 110f;
    public static final float HEADER_HEIGHT = 20f;
    public static final float MODULE_HEIGHT = 16f;
    public static final float SETTING_HEIGHT = 14f;
    public static final float MAX_BODY_HEIGHT = 232f;
    public static final float PANEL_GAP = 7f;
    public static final float PADDING_X = 8f;

    public static final Color BG = new Color(10, 8, 14, 210);
    public static final Color OVERLAY = new Color(4, 2, 8, 80);
    public static final Color HOVER = new Color(255, 255, 255, 14);
    public static final Color BAR_BG = new Color(255, 255, 255, 22);
    public static final Color TEXT_PRIMARY = new Color(245, 242, 248);
    public static final Color TEXT_SECONDARY = new Color(188, 182, 196);
    public static final Color TEXT_MUTED = new Color(118, 112, 128);
    public static final Color TOOLTIP_BG = new Color(10, 8, 14, 230);

    private YuriTheme() {
    }

    public static Color accent() {
        return ColorManager.getColor();
    }

    public static Color accentSoft(float alphaScale) {
        Color accent = accent();
        int alpha = Math.max(0, Math.min(255, Math.round(accent.getAlpha() * alphaScale)));
        return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha);
    }

    public static Color fade(Color color, float progress) {
        int alpha = Math.max(0, Math.min(255, Math.round(color.getAlpha() * progress)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color fadeSolid(Color color, float progress) {
        int alpha = Math.max(0, Math.min(255, Math.round(255 * progress)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static int text(Color color, float progress) {
        return fadeSolid(color, progress).getRGB();
    }
}
