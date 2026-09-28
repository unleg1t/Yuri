package ddlc.yuri.api.gui.click.csgo;

import ddlc.yuri.utils.render.imgui.ImGuiManager;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

public final class CsgoWidgets {

    private CsgoWidgets() {
    }

    public static ImVec2 textSize(String text) {
        ImFont font = font();
        if (font == null) {
            return ImGui.calcTextSize(text);
        }
        return font.calcTextSizeA(font.getFontSize(), Float.MAX_VALUE, -1f, text);
    }

    public static void addText(float x, float y, int color, String text) {
        ImDrawList draw = ImGui.getWindowDrawList();
        ImFont font = font();
        if (font != null) {
            draw.addText(font, font.getFontSize(), x, y, color, text);
        } else {
            draw.addText(x, y, color, text);
        }
    }

    public static void beginChild(String label, float width, float height) {
        float resolvedWidth = width <= 0f ? ImGui.getWindowWidth() : width;
        float resolvedHeight = height <= 0f ? ImGui.getWindowHeight() : height;
        ImGui.beginChild(label + "decor", resolvedWidth, resolvedHeight, false, ImGuiWindowFlags.NoScrollbar);

        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        float w = ImGui.getWindowWidth();
        float h = ImGui.getWindowHeight();
        float header = CsgoTheme.s(26f);
        float fade = CsgoTheme.s(20f);
        ImDrawList draw = ImGui.getWindowDrawList();

        draw.addRectFilledMultiColor(x + 1, y + 1, x + w - 1, y + header,
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0.4f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0.4f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0.05f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0.05f)));
        draw.addRectFilledMultiColor(x + 1, y + header, x + w / 2f - fade, y + header + 1,
                unsigned(CsgoTheme.col(ImGuiCol.Border)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border)));
        draw.addRectFilledMultiColor(x + w / 2f + fade, y + header, x + w - 1, y + header + 1,
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border)),
                unsigned(CsgoTheme.col(ImGuiCol.Border)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)));
        addText(x + CsgoTheme.s(7f), y + header / 2f - CsgoTheme.fontSize() / 2f - 1f, CsgoTheme.col(ImGuiCol.TextDisabled), label);
        draw.addRect(x, y, x + w, y + h, CsgoTheme.col(ImGuiCol.Border));

        ImGui.setCursorPosY(header + 2f);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, CsgoTheme.s(8f), CsgoTheme.s(8f));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, CsgoTheme.s(13f), CsgoTheme.s(10f));
        ImGui.beginChild(label, -1, height == 0f ? 0f : -1, false, ImGuiWindowFlags.AlwaysUseWindowPadding | ImGuiWindowFlags.NoBackground);
    }

    public static void endChild() {
        ImGui.popStyleVar(2);
        ImGui.endChild();
        ImGui.endChild();
    }

    public static boolean page(String label, boolean selected, float width) {
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float h = CsgoTheme.pageItemHeight();
        boolean pressed = ImGui.invisibleButton("##page" + label, width, h);
        boolean hovered = ImGui.isItemHovered();
        float select = CsgoAnim.approach("page-" + label, selected ? 1f : hovered ? 0.45f : 0.18f, 0.18f);
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilledMultiColor(x, y, x + width, y + h,
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0.18f + select * 0.22f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)),
                unsigned(CsgoTheme.col(ImGuiCol.Border, 0.18f + select * 0.22f)));
        draw.addLine(x, y, x, y + h, CsgoTheme.scheme(0.2f + select * 0.8f));
        addText(x + CsgoTheme.s(8f), y + h / 2f - CsgoTheme.fontSize() / 2f - 1f,
                lerpColor(CsgoTheme.col(ImGuiCol.TextDisabled), CsgoTheme.scheme(1f), select),
                label);
        return pressed;
    }

    public static boolean checkbox(String id, String label, boolean value) {
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImVec2 labelSize = textSize(label);
        float square = CsgoTheme.checkboxSize();
        float totalW = square + CsgoTheme.s(8f) + labelSize.x;
        float totalH = Math.max(square, labelSize.y);

        ImGui.pushID(id);
        boolean pressed = ImGui.invisibleButton("##box", totalW, totalH);
        boolean hovered = ImGui.isItemHovered();
        if (pressed) {
            value = !value;
        }

        float fill = CsgoAnim.approach(animKey("box", id), value ? 1f : hovered ? 0.28f : 0f, 0.22f);
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(x, y, x + square, y + square, CsgoTheme.col(ImGuiCol.FrameBg));
        if (fill > 0.01f) {
            draw.addRectFilledMultiColor(x, y, x + square, y + square,
                    unsigned(CsgoTheme.scheme(fill)),
                    unsigned(CsgoTheme.scheme(fill)),
                    unsigned(CsgoTheme.scheme2(fill)),
                    unsigned(CsgoTheme.scheme2(fill)));
        }
        draw.addRect(x, y, x + square, y + square, CsgoTheme.col(ImGuiCol.BorderShadow));
        int textCol = lerpColor(CsgoTheme.col(ImGuiCol.TextDisabled), CsgoTheme.col(ImGuiCol.Text), value ? 1f : hovered ? 0.55f : 0f);
        addText(x + square + CsgoTheme.s(8f), y + square / 2f - labelSize.y / 2f - 1f, textCol, label);
        ImGui.popID();
        return pressed;
    }

    public static boolean button(String id, String label, float width, float height) {
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImGui.pushID(id);
        boolean pressed = ImGui.invisibleButton("##btn", width, height);
        boolean hovered = ImGui.isItemHovered();
        boolean held = ImGui.isItemActive();
        float hover = CsgoAnim.approach(animKey("btn", id), held && hovered ? 1f : hovered ? 0.65f : 0f, 0.2f);
        ImDrawList draw = ImGui.getWindowDrawList();
        int top = lerpColor(CsgoTheme.col(ImGuiCol.Button), CsgoTheme.col(ImGuiCol.ButtonActive), hover);
        draw.addRectFilledMultiColor(x, y, x + width, y + height,
                unsigned(top),
                unsigned(CsgoTheme.col(ImGuiCol.FrameBg)),
                unsigned(CsgoTheme.frameBg2()),
                unsigned(CsgoTheme.frameBg2()));
        draw.addRect(x, y, x + width, y + height, lerpColor(CsgoTheme.col(ImGuiCol.BorderShadow), CsgoTheme.scheme(1f), hover));
        ImVec2 labelSize = textSize(label);
        int textCol = lerpColor(CsgoTheme.col(ImGuiCol.TextDisabled), CsgoTheme.scheme(1f), hover);
        addText(x + width / 2f - labelSize.x / 2f, y + height / 2f - labelSize.y / 2f, textCol, label);
        ImGui.popID();
        return pressed;
    }

    public static boolean selectable(String id, String label, boolean selected, float width, float height) {
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImGui.pushID(id);
        boolean pressed = ImGui.invisibleButton("##sel", width, height);
        boolean hovered = ImGui.isItemHovered();
        float select = CsgoAnim.approach(animKey("sel", id), selected ? 1f : hovered ? 0.35f : 0f, 0.2f);
        ImDrawList draw = ImGui.getWindowDrawList();
        if (select > 0.01f) {
            draw.addRectFilledMultiColor(x, y, x + width, y + height,
                    unsigned(CsgoTheme.col(ImGuiCol.Border, 0.5f * select)),
                    unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)),
                    unsigned(CsgoTheme.col(ImGuiCol.Border, 0f)),
                    unsigned(CsgoTheme.col(ImGuiCol.Border, 0.5f * select)));
            draw.addLine(x, y, x, y + height, CsgoTheme.scheme(select));
        }
        int textCol = lerpColor(CsgoTheme.col(ImGuiCol.TextDisabled), CsgoTheme.col(ImGuiCol.Text), selected ? 1f : hovered ? 0.6f : 0f);
        addText(x + CsgoTheme.s(11f), y + height / 2f - CsgoTheme.fontSize() / 2f - 1f, textCol, label);
        ImGui.popID();
        return pressed;
    }

    public static boolean slider(String id, String label, float[] value, float min, float max, String format) {
        float width = ImGui.calcItemWidth();
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float h = CsgoTheme.sliderHeight();
        String formatted = String.format(format, value[0]);
        ImVec2 valueSize = textSize(formatted);

        ImGui.pushID(id);
        boolean pressed = ImGui.invisibleButton("##slider", width, h + CsgoTheme.s(14f));
        boolean hovered = ImGui.isItemHovered();
        boolean held = ImGui.isItemActive();
        if (held) {
            float mouseX = ImGui.getMousePosX();
            float t = (mouseX - x) / Math.max(1f, width);
            t = Math.max(0f, Math.min(1f, t));
            value[0] = min + (max - min) * t;
        }

        float fraction = (value[0] - min) / Math.max(0.0001f, max - min);
        fraction = Math.max(0f, Math.min(1f, fraction));
        float fillFrac = CsgoAnim.approach(animKey("slider", id), fraction, held ? 1f : 0.28f);
        float fill = x + width * fillFrac;
        float glow = CsgoAnim.approach(animKey("slider-glow", id), hovered || held ? 1f : 0.75f, 0.18f);
        ImDrawList draw = ImGui.getWindowDrawList();
        float trackY = y + CsgoTheme.s(14f);
        draw.addRectFilled(x, trackY, x + width, trackY + h, CsgoTheme.col(ImGuiCol.FrameBg));
        draw.addRectFilledMultiColor(x, trackY, fill, trackY + h,
                unsigned(CsgoTheme.scheme(glow)),
                unsigned(CsgoTheme.scheme(glow)),
                unsigned(CsgoTheme.scheme2(glow)),
                unsigned(CsgoTheme.scheme2(glow)));
        draw.addRect(x, trackY, x + width, trackY + h, CsgoTheme.col(ImGuiCol.BorderShadow));
        addText(x, y - 1f, hovered || held ? CsgoTheme.col(ImGuiCol.Text) : CsgoTheme.col(ImGuiCol.TextDisabled), label);
        addText(x + width - valueSize.x, y - 1f, CsgoTheme.scheme(1f), formatted);
        ImGui.popID();
        return pressed || held;
    }

    public static boolean combo(String id, String label, String preview, String[] options, int[] current) {
        float width = ImGui.calcItemWidth();
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float h = CsgoTheme.s(20f);
        float labelH = CsgoTheme.fontSize() + CsgoTheme.s(3f);
        ImGui.pushID(id);
        boolean opened = ImGui.invisibleButton("##combo", width, h + labelH);
        boolean hovered = ImGui.isItemHovered();
        boolean active = ImGui.isItemActive();
        ImDrawList draw = ImGui.getWindowDrawList();
        float frameY = y + labelH;
        draw.addRectFilled(x, frameY, x + width, frameY + h, CsgoTheme.col(hovered || active ? ImGuiCol.FrameBgHovered : ImGuiCol.FrameBg));
        draw.addRect(x, frameY, x + width, frameY + h, CsgoTheme.col(ImGuiCol.BorderShadow));
        addText(x + CsgoTheme.s(6f), frameY + h / 2f - CsgoTheme.fontSize() / 2f - 1f,
                hovered || active ? CsgoTheme.col(ImGuiCol.Text) : CsgoTheme.col(ImGuiCol.TextDisabled),
                preview);
        addText(x + width - CsgoTheme.s(12f), frameY + h / 2f - CsgoTheme.fontSize() / 2f - 1f, CsgoTheme.col(ImGuiCol.TextDisabled), "v");
        addText(x, y, hovered || active ? CsgoTheme.col(ImGuiCol.Text) : CsgoTheme.col(ImGuiCol.TextDisabled), label);

        if (opened) {
            ImGui.openPopup("##combo-popup");
        }

        boolean changed = false;
        ImGui.setNextWindowSize(width, 0);
        if (ImGui.beginPopup("##combo-popup")) {
            for (int i = 0; i < options.length; i++) {
                if (selectable(id + i, options[i], current[0] == i, width - CsgoTheme.s(8f), CsgoTheme.s(20f))) {
                    current[0] = i;
                    changed = true;
                    ImGui.closeCurrentPopup();
                }
            }
            ImGui.endPopup();
        }
        ImGui.popID();
        return changed;
    }

    public static boolean input(String id, String label, ImString buffer) {
        float width = ImGui.calcItemWidth();
        ImGui.pushItemWidth(width);
        ImGui.pushID(id);
        boolean changed = ImGui.inputText("##" + label, buffer);
        ImGui.popID();
        ImGui.popItemWidth();
        ImGui.sameLine();
        ImGui.textDisabled(label);
        return changed;
    }

    public static void centerText(String text) {
        float width = ImGui.calcItemWidth();
        ImGui.setCursorPosX(ImGui.getCursorPosX() + width / 2f - textSize(text).x / 2f);
        ImGui.textUnformatted(text);
    }

    private static ImFont font() {
        return ImGuiManager.get().getCompactFont(CsgoTheme.fontSize());
    }

    private static String animKey(String prefix, String id) {
        return prefix + "-" + ImGui.getID(id);
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int fa = (from >>> 24) & 0xFF;
        int fr = from & 0xFF;
        int fg = (from >>> 8) & 0xFF;
        int fb = (from >>> 16) & 0xFF;
        int ta = (to >>> 24) & 0xFF;
        int tr = to & 0xFF;
        int tg = (to >>> 8) & 0xFF;
        int tb = (to >>> 16) & 0xFF;
        int a = Math.round(fa + (ta - fa) * t);
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static long unsigned(int color) {
        return color & 0xFFFFFFFFL;
    }
}
