package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.Yuri;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import ddlc.yuri.utils.render.animations.Direction;
import ddlc.yuri.utils.render.animations.impl.DecelerateAnimation;
import ddlc.yuri.utils.render.shader.impl.Shadow;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class YuriClickGUI extends GuiScreen {

    private final List<YuriPanel> panels = new CopyOnWriteArrayList<>();
    private final DecelerateAnimation openAnimation = new DecelerateAnimation(280, 1.0D, Direction.FORWARDS);
    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);
    private boolean closing;

    @Override
    public void initGui() {
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;

        if (panels.isEmpty()) {
            float x = 18f;
            for (ModuleCategory category : ModuleCategory.values()) {
                panels.add(new YuriPanel(category, x, 14f));
                x += YuriTheme.PANEL_WIDTH + YuriTheme.PANEL_GAP;
            }
            panels.add(new YuriConfigPanel(x, 14f));
        }

        for (YuriPanel panel : panels) {
            panel.resetAnimations();
        }
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float progress = openAnimation.getOutput().floatValue();

        if (progress < 0.06F) {
            if (closing) {
                mc.displayGuiScreen(null);
            }
            return;
        }

        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            mc.displayGuiScreen(null);
            return;
        }

        GL11.glPushMatrix();
        ScaledResolution sr = new ScaledResolution(mc);
        int[] scaledMouse = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaledMouse[0];
        int scaledMouseY = scaledMouse[1];
        ScaleUtils.scale(mc);

        drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(),
                RenderUtils.withAlpha(YuriTheme.OVERLAY, (int) (YuriTheme.OVERLAY.getAlpha() * progress)));

        applyPanelShadow(progress);

        String tooltip = null;
        for (YuriPanel panel : panels) {
            String panelTooltip = panel.drawScreen(scaledMouseX, scaledMouseY, progress);
            if (panelTooltip != null) {
                tooltip = panelTooltip;
            }
            panel.updateDrag(scaledMouseX, scaledMouseY);
        }

        if (tooltip != null) {
            drawTooltip(tooltip, scaledMouseX, scaledMouseY, progress, sr);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glPopMatrix();
    }

    private void applyPanelShadow(float progress) {
        if (progress < 0.12F) {
            return;
        }

        shadowFramebuffer = RenderUtils.createFrameBuffer(shadowFramebuffer, true);
        shadowFramebuffer.framebufferClear();
        shadowFramebuffer.bindFramebuffer(true);
        RenderUtils.resetColor();
        for (YuriPanel panel : panels) {
            panel.drawShaderMask(progress);
        }
        shadowFramebuffer.unbindFramebuffer();
        RenderUtils.resetColor();
        if (shadowFramebuffer.framebufferTexture > 0) {
            Shadow.renderShadow(shadowFramebuffer.framebufferTexture, 14, 1, 1.1f);
        }
    }

    private void drawTooltip(String description, int mouseX, int mouseY, float animationProgress, ScaledResolution sr) {
        int padding = 5;
        int width = FontUtils.getFont("sf", 12).getStringWidth(description) + padding * 2;
        int height = 12;
        int x = mouseX + 8;
        int y = mouseY + 8;

        if (x + width > sr.getScaledWidth()) {
            x = mouseX - width - 6;
        }
        if (y + height > sr.getScaledHeight()) {
            y = mouseY - height - 6;
        }

        RoundedUtils.drawRoundedRect(x, y, width, height, 3f, YuriTheme.fade(YuriTheme.TOOLTIP_BG, animationProgress));
        FontUtils.getFont("sf", 12).drawStringWithShadow(
                description,
                x + padding,
                y + 2.5f,
                YuriTheme.text(YuriTheme.TEXT_SECONDARY, animationProgress)
        );
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) {
            return;
        }

        int[] scaledMouse = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaledMouse[0];
        int scaledMouseY = scaledMouse[1];

        for (YuriPanel panel : panels) {
            if (panel.isHeaderHovered(scaledMouseX, scaledMouseY) && mouseButton == 0 && !anyDragging()) {
                panel.startDragging(scaledMouseX, scaledMouseY);
            }
            panel.mouseClicked(scaledMouseX, scaledMouseY, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && !areAnyTextFieldsHovered()) {
            beginClose();
            return;
        }

        panels.forEach(panel -> panel.keyTyped(typedChar, keyCode));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || closing) {
            return;
        }

        int guiMouseX = Mouse.getEventX() * this.width / mc.displayWidth;
        int guiMouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;
        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, guiMouseX, guiMouseY);

        float amount = wheel > 0 ? -16.0F : 16.0F;
        for (YuriPanel panel : panels) {
            if (panel.isMouseOver(scaled[0], scaled[1])) {
                panel.scroll(amount);
                break;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();
    }

    public boolean isClosing() {
        return closing;
    }

    private boolean areAnyTextFieldsHovered() {
        for (YuriPanel panel : panels) {
            if (panel.isTyping()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            panels.forEach(panel -> panel.dragging = false);
        }
        int[] scaledMouse = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        panels.forEach(panel -> panel.mouseReleased(scaledMouse[0], scaledMouse[1], state));
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean anyDragging() {
        for (YuriPanel panel : panels) {
            if (panel.dragging) {
                return true;
            }
        }
        return false;
    }

    public List<YuriPanel> getPanels() {
        return new ArrayList<>(panels);
    }
}
