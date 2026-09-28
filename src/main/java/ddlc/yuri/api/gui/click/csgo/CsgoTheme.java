package ddlc.yuri.api.gui.click.csgo;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.imgui.ImGuiManager;
import imgui.ImFont;
import imgui.ImGui;

public final class CsgoTheme {

    public static final float WINDOW_WIDTH = 620f;
    public static final float WINDOW_HEIGHT = 430f;
    public static final float MIN_WINDOW_WIDTH = 480f;
    public static final float MIN_WINDOW_HEIGHT = 330f;
    public static final float BASE_HEADER_HEIGHT = 26f;
    public static final float BASE_FOOTER_HEIGHT = 22f;
    public static final float BASE_PAGE_ITEM_HEIGHT = 20f;
    public static final float BASE_CHECKBOX_SIZE = 9f;
    public static final float BASE_BUTTON_HEIGHT = 24f;
    public static final float BASE_SLIDER_HEIGHT = 9f;
    public static final float BASE_FONT_SIZE = 13f;

    private static float scale = 1f;
    private static float fontSize = BASE_FONT_SIZE;

    public static final int SCHEME = ColorManager.getColor().getRGB();
    public static final int SCHEME2 = ColorManager.getColor().getRGB();
    public static final int FRAME_BG2 = rgb(16, 15, 16);

    private CsgoTheme() {
    }

    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    public static int rgba(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }

    public static int withAlpha(int color, float alpha) {
        float global = ImGui.getStyle().getAlpha();
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 0xFF) * alpha * global)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int scheme(float alpha) {
        return withAlpha(SCHEME, alpha);
    }

    public static int scheme2(float alpha) {
        return withAlpha(SCHEME2, alpha);
    }

    public static int col(int imguiCol) {
        return ImGui.getColorU32(imguiCol);
    }

    public static int col(int imguiCol, float alpha) {
        return ImGui.getColorU32(imguiCol, alpha);
    }

    public static int frameBg2() {
        return withAlpha(FRAME_BG2, 1f);
    }

    public static void updateScale(float width, float height) {
        float next = Math.min(width / WINDOW_WIDTH, height / WINDOW_HEIGHT);
        next = Math.max(0.75f, Math.min(2.4f, next));
        scale = next;
        fontSize = BASE_FONT_SIZE * scale;
        ImFont font = ImGuiManager.get().getCompactFont(fontSize);
        if (font != null) {
            fontSize = font.getFontSize();
            scale = fontSize / BASE_FONT_SIZE;
        }
    }

    public static float scale() {
        return scale;
    }

    public static float s(float value) {
        return value * scale;
    }

    public static float fontSize() {
        return fontSize;
    }

    public static float headerHeight() {
        return s(BASE_HEADER_HEIGHT);
    }

    public static float footerHeight() {
        return s(BASE_FOOTER_HEIGHT);
    }

    public static float pageItemHeight() {
        return s(BASE_PAGE_ITEM_HEIGHT);
    }

    public static float checkboxSize() {
        return s(BASE_CHECKBOX_SIZE);
    }

    public static float buttonHeight() {
        return s(BASE_BUTTON_HEIGHT);
    }

    public static float sliderHeight() {
        return s(BASE_SLIDER_HEIGHT);
    }
}
