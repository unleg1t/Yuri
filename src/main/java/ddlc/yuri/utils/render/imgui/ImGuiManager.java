package ddlc.yuri.utils.render.imgui;

import com.github.koxx12dev.fuckyou.ImGuiGL3;
import com.github.koxx12dev.fuckyou.ImGuiLwjgl2;
import ddlc.yuri.utils.render.imgui.style.ImGuiStyleSheet;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import lombok.Getter;
import imgui.flag.ImGuiKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.io.InputStream;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class ImGuiManager {

    private static ImGuiManager instance;

    private boolean initialized;
    private final ImGuiGL3 imGuiGl = new ImGuiGL3();
    private final ImGuiLwjgl2 imGuiLwjgl = new ImGuiLwjgl2();
    @Getter
    private ImFont compactFont;
    private final NavigableMap<Integer, ImFont> compactFonts = new TreeMap<Integer, ImFont>();

    private ImGuiManager() {
    }

    public static ImGuiManager get() {
        if (instance == null) {
            instance = new ImGuiManager();
        }
        return instance;
    }

    public void init(ImGuiStyleSheet style) {
        if (!initialized) {
            ImGui.createContext();
            imGuiLwjgl.init();
            setupKeyMap();
            buildFontAtlas();
            imGuiGl.init("#version 120");
            initialized = true;
        }

        applyStyle(style);
    }

    private void setupKeyMap() {
        ImGuiIO io = ImGui.getIO();

        // Navigation & Editing key mapping
        io.setKeyMap(ImGuiKey.Tab, Keyboard.KEY_TAB);
        io.setKeyMap(ImGuiKey.LeftArrow, Keyboard.KEY_LEFT);
        io.setKeyMap(ImGuiKey.RightArrow, Keyboard.KEY_RIGHT);
        io.setKeyMap(ImGuiKey.UpArrow, Keyboard.KEY_UP);
        io.setKeyMap(ImGuiKey.DownArrow, Keyboard.KEY_DOWN);
        io.setKeyMap(ImGuiKey.PageUp, Keyboard.KEY_PRIOR);
        io.setKeyMap(ImGuiKey.PageDown, Keyboard.KEY_NEXT);
        io.setKeyMap(ImGuiKey.Home, Keyboard.KEY_HOME);
        io.setKeyMap(ImGuiKey.End, Keyboard.KEY_END);
        io.setKeyMap(ImGuiKey.Insert, Keyboard.KEY_INSERT);
        io.setKeyMap(ImGuiKey.Delete, Keyboard.KEY_DELETE);
        io.setKeyMap(ImGuiKey.Backspace, Keyboard.KEY_BACK);
        io.setKeyMap(ImGuiKey.Space, Keyboard.KEY_SPACE);
        io.setKeyMap(ImGuiKey.Enter, Keyboard.KEY_RETURN);
        io.setKeyMap(ImGuiKey.Escape, Keyboard.KEY_ESCAPE);
        io.setKeyMap(ImGuiKey.KeyPadEnter, Keyboard.KEY_NUMPADENTER);

        // Shortcut key mapping (Ctrl + key actions)
        io.setKeyMap(ImGuiKey.A, Keyboard.KEY_A); // Select All
        io.setKeyMap(ImGuiKey.C, Keyboard.KEY_C); // Copy
        io.setKeyMap(ImGuiKey.V, Keyboard.KEY_V); // Paste
        io.setKeyMap(ImGuiKey.X, Keyboard.KEY_X); // Cut
        io.setKeyMap(ImGuiKey.Y, Keyboard.KEY_Y); // Redo
        io.setKeyMap(ImGuiKey.Z, Keyboard.KEY_Z); // Undo
    }

    private void updateModifiers() {
        ImGuiIO io = ImGui.getIO();
        io.setKeyCtrl(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
        io.setKeyShift(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        io.setKeyAlt(Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU));
        io.setKeySuper(Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA));
    }

    public void applyStyle(ImGuiStyleSheet style) {
        ImGuiStyle imStyle = ImGui.getStyle();
        imStyle.setAlpha(style.getAlpha());
        imStyle.setWindowRounding(style.getWindowRounding());
        imStyle.setFrameRounding(style.getFrameRounding());
        imStyle.setTabRounding(style.getTabRounding());
        imStyle.setWindowPadding(style.getWindowPaddingX(), style.getWindowPaddingY());
        imStyle.setWindowMinSize(style.getWindowMinSizeX(), style.getWindowMinSizeY());
        imStyle.setFramePadding(style.getFramePaddingX(), style.getFramePaddingY());
        imStyle.setItemSpacing(style.getItemSpacingX(), style.getItemSpacingY());
        imStyle.setItemInnerSpacing(style.getItemInnerSpacingX(), style.getItemInnerSpacingY());
        imStyle.setIndentSpacing(style.getIndentSpacing());
        imStyle.setColumnsMinSpacing(style.getColumnsMinSpacing());
        imStyle.setGrabMinSize(style.getGrabMinSize());
        imStyle.setGrabRounding(style.getGrabRounding());
        imStyle.setScrollbarSize(style.getScrollbarSize());
        imStyle.setScrollbarRounding(style.getScrollbarRounding());
        imStyle.setWindowBorderSize(style.getWindowRounding() == 0f ? 0f : 1f);
        imStyle.setChildRounding(style.getFrameRounding());
        imStyle.setPopupRounding(style.getFrameRounding());
        imStyle.setAntiAliasedFill(style.getWindowRounding() != 0f);

        for (Map.Entry<Integer, ImGuiStyleSheet.ColorValue> entry : style.getColors().entrySet()) {
            setColor(imStyle, entry.getKey(), entry.getValue());
        }
    }

    private void setColor(ImGuiStyle style, int slot, ImGuiStyleSheet.ColorValue value) {
        Color color = value.getColor();
        style.setColor(
                slot,
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                value.getAlpha() / 255f
        );
    }

    private void buildFontAtlas() {
        ImGuiIO io = ImGui.getIO();
        ResourceLocation fontLocation = new ResourceLocation("yuri/fonts/sf.ttf");

        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(fontLocation).getInputStream()) {
            byte[] bytes = readAllBytes(stream);
            ImFontConfig config = new ImFontConfig();
            config.setOversampleH(2);
            config.setOversampleV(2);
            io.getFonts().addFontFromMemoryTTF(bytes, 18f, config);
            config.destroy();

            int[] sizes = {11, 13, 15, 17, 19, 21, 24, 28, 32};
            for (int size : sizes) {
                ImFontConfig compactConfig = new ImFontConfig();
                compactConfig.setOversampleH(2);
                compactConfig.setOversampleV(2);
                compactConfig.setPixelSnapH(true);
                ImFont font = io.getFonts().addFontFromMemoryTTF(bytes, size, compactConfig);
                compactFonts.put(size, font);
                if (size == 13) {
                    compactFont = font;
                }
                compactConfig.destroy();
            }
        } catch (Exception e) {
            io.getFonts().addFontDefault();
        }

        imGuiGl.updateFontsTexture();
    }

    public ImFont getCompactFont(float size) {
        if (compactFonts.isEmpty()) {
            return compactFont;
        }
        int target = Math.max(1, Math.round(size));
        Map.Entry<Integer, ImFont> floor = compactFonts.floorEntry(target);
        Map.Entry<Integer, ImFont> ceil = compactFonts.ceilingEntry(target);
        if (floor == null) {
            return ceil.getValue();
        }
        if (ceil == null) {
            return floor.getValue();
        }
        return target - floor.getKey() <= ceil.getKey() - target ? floor.getValue() : ceil.getValue();
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    public void mouseClicked(int button) {
        if (button >= 0 && button < 5) {
            ImGui.getIO().setMouseDown(button, true);
        }
    }

    public void mouseScrolled(float delta) {
        imGuiLwjgl.scrollCallback(delta);
    }

    public void charTyped(char character) {
        if (character >= 32 && character != 127) {
            imGuiLwjgl.charCallback((int) character);
        }
    }

    public void keyEvent(int lwjglKeyCode, boolean down) {
        if (lwjglKeyCode >= 0 && lwjglKeyCode < 512) {
            ImGui.getIO().setKeysDown(lwjglKeyCode, down);
            updateModifiers();
        }
    }

    public boolean wantsMouse() {
        return ImGui.getIO().getWantCaptureMouse();
    }

    public boolean wantsKeyboard() {
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    public void newFrame(float width, float height) {
        updateModifiers();
        float delta = 1.0f / Math.max(Minecraft.getDebugFPS(), 1);
        imGuiLwjgl.newFrame(width, height, delta);
        ImGui.newFrame();
    }

    public void render() {
        ImGui.render();

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GL20.glUseProgram(0);

        imGuiGl.renderDrawData(ImGui.getDrawData());

        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }
}