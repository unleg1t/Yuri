package ddlc.yuri.api.gui.click.csgo;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.imgui.ImGuiManager;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

import java.awt.Color;

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
        return withAlpha(fromAwt(ColorManager.getColor()), alpha);
    }

    public static int scheme2(float alpha) {
        return withAlpha(fromAwt(ColorManager.getColor().darker()), alpha);
    }

    public static void applyAccents() {
        Color accent = ColorManager.getColor();
        Color darker = accent.darker();
        ImGuiStyle style = ImGui.getStyle();
        setStyle(style, ImGuiCol.CheckMark, accent, 255);
        setStyle(style, ImGuiCol.SliderGrab, accent, 255);
        setStyle(style, ImGuiCol.SliderGrabActive, darker, 255);
        setStyle(style, ImGuiCol.Header, accent, 80);
        setStyle(style, ImGuiCol.HeaderHovered, accent, 140);
        setStyle(style, ImGuiCol.HeaderActive, accent, 200);
        setStyle(style, ImGuiCol.SeparatorHovered, accent, 180);
        setStyle(style, ImGuiCol.SeparatorActive, accent, 255);
        setStyle(style, ImGuiCol.ResizeGrip, accent, 50);
        setStyle(style, ImGuiCol.ResizeGripHovered, accent, 170);
        setStyle(style, ImGuiCol.ResizeGripActive, accent, 240);
        setStyle(style, ImGuiCol.TabHovered, accent, 180);
        setStyle(style, ImGuiCol.TabActive, accent, 220);
        setStyle(style, ImGuiCol.PlotLinesHovered, accent, 255);
        setStyle(style, ImGuiCol.PlotHistogram, accent, 255);
        setStyle(style, ImGuiCol.PlotHistogramHovered, darker, 255);
        setStyle(style, ImGuiCol.TextSelectedBg, accent, 128);
        setStyle(style, ImGuiCol.DragDropTarget, accent, 230);
        setStyle(style, ImGuiCol.NavHighlight, accent, 255);
    }

    private static void setStyle(ImGuiStyle style, int slot, Color color, int alpha) {
        style.setColor(slot, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha / 255f);
    }

    private static int fromAwt(Color color) {
        return rgba(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
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
