package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import ddlc.yuri.utils.render.animations.Direction;
import ddlc.yuri.utils.render.animations.impl.DecelerateAnimation;
import ddlc.yuri.utils.render.shader.impl.Blur;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class YuriClickGUI extends GuiScreen {

    private static final List<CategoryWindow> windows = new CopyOnWriteArrayList<>();
    private static boolean firstOpen = true;

    public static String searchQuery = "";
    private static boolean searching = false;

    private final DecelerateAnimation openAnimation = new DecelerateAnimation(220, 1.0D, Direction.FORWARDS);
    @Getter
    private boolean closing;

    @Override
    public void initGui() {
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;
        searching = false;
        searchQuery = "";

        if (firstOpen) {
            float gap = 8f;
            float startX = 20f;
            float y = 20f;
            for (ModuleCategory category : ModuleCategory.values()) {
                windows.add(new CategoryWindow(category, startX, y));
                startX += CategoryWindow.WIDTH + gap;
            }
            firstOpen = false;
        }

        super.initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    public void beginClose() {
        if (closing) return;
        closing = true;
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float progress = MathHelper.clamp_float(openAnimation.getOutput().floatValue(), 0.0f, 1.0f);

        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        if (progress < 0.08f) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        Blur.startBlur();
        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), -1);
        Blur.endBlur(12f * progress, 2f, 1f);

        GL11.glPushMatrix();
        ScaleUtils.scale(mc);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int backgroundAlpha = MathHelper.clamp_int((int) (130 * progress), 0, 255);
        drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), RenderUtils.withAlpha(new Color(0, 0, 0), backgroundAlpha));

        CategoryWindow topmostHovered = null;
        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.isMouseOver(scaledMouseX, scaledMouseY)) {
                topmostHovered = window;
                break;
            }
        }

        String tooltip = null;
        for (CategoryWindow window : windows) {
            window.updateDrag(scaledMouseX, scaledMouseY);
            String windowTooltip = window.drawScreen(scaledMouseX, scaledMouseY, progress);
            if (window == topmostHovered && windowTooltip != null) {
                tooltip = windowTooltip;
            }
        }

        drawSearchBar(sr, progress);

        if (tooltip != null) {
            drawTooltip(tooltip, scaledMouseX, scaledMouseY, progress, sr);
        }

        GL11.glPopMatrix();
    }

    private void drawSearchBar(ScaledResolution sr, float progress) {
        int argb = MathHelper.clamp_int((int) (255 * progress), 0, 255);
        float width = 140f;
        float height = 18f;
        float x = sr.getScaledWidth() / 2f - width / 2f;
        float y = sr.getScaledHeight() - 38f;

        Blur.startBlur();
        RoundedUtils.drawRoundedRect(x, y, width, height, 5f, Color.WHITE);
        Blur.endBlur(8f * progress, 2f, 1f);

        int panelBgAlpha = MathHelper.clamp_int((int) (80 * progress), 0, 255);
        Color searchBg = new Color(Theme.WINDOW_BG.getRed(), Theme.WINDOW_BG.getGreen(), Theme.WINDOW_BG.getBlue(), panelBgAlpha);

        RoundedUtils.drawRoundOutline(x, y, width, height, 5f, -0.5f,
                searchBg, RenderUtils.withAlphaColor(Theme.accent(), argb));

        String text = searchQuery.isEmpty() ? "Search..." : searchQuery;
        Color color = searchQuery.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT;
        CustomFontRenderer font = FontUtils.getFont("sf", 14);
        float textY = y + (height - font.getHeight()) / 2f;
        font.drawCenteredStringWithShadow(text, sr.getScaledWidth() / 2f, textY, RenderUtils.withAlpha(color, argb));
    }

    private void drawTooltip(String description, int mouseX, int mouseY, float progress, ScaledResolution sr) {
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int argb = MathHelper.clamp_int((int) (255 * progress), 0, 255);
        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        int padding = 4;
        int width = font.getStringWidth(description) + padding * 2;
        int height = 14;
        int x = mouseX + 8;
        int y = mouseY + 8;
        if (x + width > sr.getScaledWidth()) x = mouseX - width - 4;
        if (y + height > sr.getScaledHeight()) y = mouseY - height - 4;

        RoundedUtils.drawRoundOutline(x, y, width, height, 4f, -0.5f,
                RenderUtils.withAlphaColor(Theme.TOOLTIP_BG, argb),
                RenderUtils.withAlphaColor(Theme.accent(), argb));

        float textY = y + (height - font.getHeight()) / 2f;
        font.drawString(description, x + padding - 1f, textY, RenderUtils.withAlpha(Theme.TEXT, argb));

        GlStateManager.enableDepth();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        float searchW = 140f;
        float searchX = sr.getScaledWidth() / 2f - searchW / 2f;
        float searchY = sr.getScaledHeight() - 38f;

        if (scaledMouseX >= searchX && scaledMouseX <= searchX + searchW && scaledMouseY >= searchY && scaledMouseY <= searchY + 18) {
            searching = true;
        } else if (searching) {
            searching = false;
        }

        CategoryWindow clickedWindow = null;
        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.isMouseOver(scaledMouseX, scaledMouseY)) {
                clickedWindow = window;
                break;
            }
        }

        if (clickedWindow != null) {
            windows.remove(clickedWindow);
            windows.add(clickedWindow);

            if (clickedWindow.isHeaderHovered(scaledMouseX, scaledMouseY) && mouseButton == 0 && !anyDragging()) {
                clickedWindow.startDragging(scaledMouseX, scaledMouseY);
            }
            clickedWindow.mouseClicked(scaledMouseX, scaledMouseY, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        Minecraft mc = Minecraft.getMinecraft();
        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        for (CategoryWindow window : windows) window.mouseReleased(scaled[0], scaled[1], state);
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean anyDragging() {
        for (CategoryWindow window : windows) if (window.dragging) return true;
        return false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        int guiMouseX = Mouse.getEventX() * this.width / mc.displayWidth;
        int guiMouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;

        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, guiMouseX, guiMouseY);
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.isMouseOver(scaledMouseX, scaledMouseY)) {
                window.scroll(wheel > 0 ? -16f : 16f);
                break;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

        if (searching) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                searching = false;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            } else if (!isIgnoredKey(keyCode)) {
                searchQuery += typedChar;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_F && ctrl) {
            searching = true;
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE && !anyTextFieldHovered()) {
            beginClose();
            return;
        }

        for (CategoryWindow window : windows) window.keyTyped(typedChar, keyCode);
    }

    private boolean anyTextFieldHovered() {
        for (CategoryWindow window : windows) if (window.isAnyTextFieldHovered()) return true;
        return false;
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB;
    }

    public static List<CategoryWindow> getWindows() {
        return new ArrayList<>(windows);
    }
}