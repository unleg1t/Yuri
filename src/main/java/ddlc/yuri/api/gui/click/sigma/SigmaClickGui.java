package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.render.animations.Direction;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SigmaClickGui extends GuiScreen {

    private static final ResourceLocation BLUR_SHADER = new ResourceLocation("shaders/post/blur.json");
    private static final int PER_ROW = 4;
    private static final float WRAP_ADVANCE = 330f;

    private final List<SigmaPanel> panels = new CopyOnWriteArrayList<>();
    private final SigmaAnimation open = new SigmaAnimation(450, 125);

    private SigmaSettingPanel settings;
    private SigmaConfigPanel configPanel;

    private boolean closing;
    private boolean blurApplied;
    private boolean mouseDown;

    @Override
    public void initGui() {
        closing = false;
        mouseDown = false;
        settings = null;
        configPanel = null;
        open.changeDirection(Direction.FORWARDS);
        open.reset();

        if (panels.isEmpty()) {
            layoutPanels();
        }
        panels.forEach(SigmaPanel::resetAnimations);

        applyBlur();
        super.initGui();
    }

    private void layoutPanels() {
        float x = SigmaTheme.PANEL_START_X;
        float y = SigmaTheme.PANEL_START_Y;
        int count = 0;
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new SigmaPanel(category, x, y, this));
            count++;
            x += SigmaTheme.PANEL_WIDTH + SigmaTheme.PANEL_GAP;
            if (count % PER_ROW == 0) {
                x = SigmaTheme.PANEL_START_X;
                y += WRAP_ADVANCE;
            }
        }
    }

    private int guiScale() {
        return Math.max(1, new ScaledResolution(mc).getScaleFactor());
    }

    private int[] nativeMouse(int mouseX, int mouseY) {
        int scale = guiScale();
        return new int[]{mouseX * scale, mouseY * scale};
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        removeBlur();
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateOpenAnimation();
        float progress = open.calcPercent();
        if (closing && progress <= 0f) {
            mc.displayGuiScreen(null);
            return;
        }

        float var4 = closing
                ? SigmaEasing.easeOutQuad(progress, 0f, 1f, 1f)
                : SigmaEasing.jelly(progress, 0.8f) * 0.5f + 0.5f;

        updateBlurRadius(Math.min(1f, progress * 4f));

        float screenWidth = mc.displayWidth;
        float screenHeight = mc.displayHeight;
        int scale = guiScale();
        int nativeX = mouseX * scale;
        int nativeY = mouseY * scale;

        SigmaRenderer.setScale(1f / scale);
        GlStateManager.disableCull();
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0.0f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        for (SigmaPanel panel : panels) {
            panel.update(nativeX, nativeY, mouseDown);
        }

        float tint = 0.2f * var4;
        SigmaRenderer.rect(0, 0, screenWidth, screenHeight, SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, tint));

        float var7 = 1f;
        if (settings != null) {
            float var8 = SigmaEasing.easeOutBack(settings.getOpenAnimation().calcPercent(), 0f, 1f, 1f);
            if (settings.getOpenAnimation().getDirection() == Direction.BACKWARDS) {
                var8 = SigmaEasing.backwardTransition(settings.getOpenAnimation().calcPercent(), 0f, 1f, 1f);
            }
            var7 -= settings.getOpenAnimation().calcPercent() * 0.1f;
            var4 = var4 * (1f + var8 * 0.2f);
        }
        float panelAlpha = Math.min(1f, var4) * var7;

        for (SigmaPanel panel : panels) {
            float centerX = panel.getX() + SigmaTheme.PANEL_WIDTH / 2f;
            float centerY = panel.getY() + SigmaTheme.PANEL_HEIGHT / 2f;
            float offsetX = (centerX - screenWidth / 2f) * (1f - var4) * 0.5f;
            float offsetY = (centerY - screenHeight / 2f) * (1f - var4) * 0.5f;
            float panelScale = 1.5f - var4 * 0.5f;

            GL11.glPushMatrix();
            GL11.glTranslatef(SigmaRenderer.s(centerX), SigmaRenderer.s(centerY), 0f);
            GL11.glScalef(panelScale, panelScale, 1f);
            GL11.glTranslatef(-SigmaRenderer.s(centerX), -SigmaRenderer.s(centerY), 0f);
            GL11.glTranslatef(SigmaRenderer.s(offsetX), SigmaRenderer.s(offsetY), 0f);
            panel.draw(nativeX, nativeY, panelAlpha, centerX, centerY, panelScale, offsetX, offsetY);
            GL11.glPopMatrix();
        }

        if (settings != null) {
            settings.update(nativeX, nativeY, mouseDown);
            settings.draw(nativeX, nativeY);
            if (settings.isFinished()) {
                settings = null;
            }
        }

        drawMoreButton(nativeX, nativeY, Math.min(1f, var4));

        if (configPanel != null) {
            configPanel.update();
            configPanel.draw(nativeX, nativeY);
            if (configPanel.isFinished()) {
                configPanel = null;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void updateOpenAnimation() {
        if (!closing) {
            open.changeDirection(Direction.FORWARDS);
        }
    }

    private void drawMoreButton(float mouseX, float mouseY, float alpha) {
        float bx = mc.displayWidth - 69f;
        float by = mc.displayHeight - 55f;
        boolean hovered = mouseX >= bx && mouseX <= bx + 55f && mouseY >= by && mouseY <= by + 41f;
        float brightness = hovered ? 0.45f : 0.3f;
        SigmaRenderer.image(SigmaTheme.OPTIONS, bx, by, 55f, 41f, SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, brightness * alpha));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) {
            return;
        }
        mouseDown = true;
        int[] nativeMouse = nativeMouse(mouseX, mouseY);
        float nx = nativeMouse[0];
        float ny = nativeMouse[1];

        if (settings != null) {
            if (settings.contains(nx, ny)) {
                settings.mouseClicked(nx, ny, mouseButton);
            } else if (!settings.isTyping()) {
                settings.beginClose();
            }
            return;
        }

        if (isOverMoreButton(nx, ny) && mouseButton == 0) {
            if (configPanel == null) {
                configPanel = new SigmaConfigPanel(mc.displayWidth, mc.displayHeight);
            } else {
                configPanel.beginClose();
                configPanel = null;
            }
            return;
        }

        if (configPanel != null) {
            if (configPanel.isMouseOver(nx, ny)) {
                configPanel.mouseClicked(nx, ny, mouseButton);
                return;
            }
            configPanel.beginClose();
            configPanel = null;
        }

        for (SigmaPanel panel : panels) {
            panel.mouseClicked(nx, ny, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isOverMoreButton(float mouseX, float mouseY) {
        float bx = mc.displayWidth - 69f;
        float by = mc.displayHeight - 55f;
        return mouseX >= bx && mouseX <= bx + 55f && mouseY >= by && mouseY <= by + 41f;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        mouseDown = false;
        int[] nativeMouse = nativeMouse(mouseX, mouseY);
        if (settings != null) {
            settings.mouseReleased(nativeMouse[0], nativeMouse[1], state);
        }
        panels.forEach(SigmaPanel::mouseReleased);
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (settings != null && settings.isTyping()) {
            settings.keyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (settings != null) {
                settings.beginClose();
                return;
            }
            if (configPanel != null) {
                configPanel.beginClose();
                configPanel = null;
                return;
            }
            beginClose();
            return;
        }

        if (settings != null) {
            settings.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || closing) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int[] nativeMouse = nativeMouse(mouseX, mouseY);
        float amount = wheel > 0 ? -35f : 35f;

        if (settings != null) {
            settings.scroll(amount);
            return;
        }
        if (configPanel != null && configPanel.isMouseOver(nativeMouse[0], nativeMouse[1])) {
            configPanel.scroll(amount);
            return;
        }
        for (SigmaPanel panel : panels) {
            if (panel.isMouseOver(nativeMouse[0], nativeMouse[1])) {
                panel.scroll(amount);
                break;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void openSettings(Module module) {
        if (settings != null) {
            settings.beginClose();
        }
        settings = new SigmaSettingPanel(module, mc.displayWidth, mc.displayHeight);
    }

    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        open.changeDirection(Direction.BACKWARDS);
        if (settings != null) {
            settings.beginClose();
        }
        if (configPanel != null) {
            configPanel.beginClose();
        }
    }

    public boolean isClosing() {
        return closing;
    }

    private void applyBlur() {
        if (blurApplied) {
            return;
        }
        try {
            mc.entityRenderer.loadShader(BLUR_SHADER);
            blurApplied = true;
        } catch (Throwable ignored) {
            blurApplied = false;
        }
    }

    private void updateBlurRadius(float radius) {
        if (!blurApplied) {
            applyBlur();
        }
        if (!blurApplied) {
            return;
        }
        try {
            ShaderGroup group = mc.entityRenderer.getShaderGroup();
            if (group != null && group.listShaders.size() >= 2) {
                setRadius(group, 0, Math.round(radius * 20f));
                setRadius(group, 1, Math.round(radius * 20f));
            }
        } catch (Throwable ignored) {
        }
    }

    private void setRadius(ShaderGroup group, int index, float radius) {
        ShaderUniform uniform = group.listShaders.get(index).getShaderManager().getShaderUniform("Radius");
        if (uniform != null) {
            uniform.set(radius);
        }
    }

    private void removeBlur() {
        if (!blurApplied) {
            return;
        }
        try {
            mc.entityRenderer.stopUseShader();
        } catch (Throwable ignored) {
        }
        blurApplied = false;
    }
}
