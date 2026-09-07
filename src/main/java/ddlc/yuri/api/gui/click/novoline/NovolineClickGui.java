package ddlc.yuri.api.gui.click.novoline;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.gui.click.novoline.config.ConfigTab;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import ddlc.yuri.utils.render.animations.Direction;
import ddlc.yuri.utils.render.animations.impl.DecelerateAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NovolineClickGui extends GuiScreen implements IMinecraft {

    private final List<CategoryTab> tabs = new CopyOnWriteArrayList<>();
    private final DecelerateAnimation openAnimation = new DecelerateAnimation(280, 1.0D, Direction.FORWARDS);

    private boolean closing;
    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void initGui() {
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;

        if (tabs.isEmpty()) {
            float x = 75;
            for (ModuleCategory category : ModuleCategory.values()) {
                tabs.add(new CategoryTab(category, x, 10));
                x += 110;
            }
            tabs.add(new ConfigTab(x, 10));
        }

        resetModuleAnimations();
        super.initGui();
    }

    private void resetModuleAnimations() {
        for (CategoryTab tab : tabs) {
            for (ModuleEntry module : tab.modules) {
                module.fraction = 0;
                module.settings.forEach(setting -> setting.setPercent(0));
            }
            if (tab instanceof ConfigTab) {
                ((ConfigTab) tab).refreshConfigs();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float progress = openAnimation.getOutput().floatValue();

        if (progress < 0.08F) {
            if (closing) {
                mc.displayGuiScreen(null);
                return;
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            mc.displayGuiScreen(null);
            return;
        }

        GL11.glPushMatrix();
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] scaledMouse = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaledMouse[0];
        int scaledMouseY = scaledMouse[1];
        ScaleUtils.scale(mc);

        int overlayAlpha = (int) (GuiTheme.OVERLAY.getAlpha() * progress);
        drawRect(
                0,
                0,
                scaledResolution.getScaledWidth(),
                scaledResolution.getScaledHeight(),
                RenderUtils.withAlpha(GuiTheme.OVERLAY, overlayAlpha)
        );

        GlStateManager.pushMatrix();
        float scale = 0.92F + (0.08F * progress);
        float centerX = scaledResolution.getScaledWidth() / 2.0F;
        float centerY = scaledResolution.getScaledHeight() / 2.0F;
        GlStateManager.translate(centerX, centerY, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(-centerX, -centerY, 0.0F);

        String tooltip = null;
        for (CategoryTab tab : tabs) {
            String tabTooltip = tab.drawScreen(scaledMouseX, scaledMouseY, progress);
            if (tabTooltip != null) {
                tooltip = tabTooltip;
            }
            tab.updatePhysics(scaledMouseX, scaledMouseY);
        }
        GlStateManager.popMatrix();

        if (tooltip != null) {
            drawTooltip(tooltip, scaledMouseX, scaledMouseY, progress, scaledResolution);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glPopMatrix();
    }

    private void drawTooltip(String description, int mouseX, int mouseY, float animationProgress, ScaledResolution scaledResolution) {
        int alpha = (int) (255 * animationProgress);
        int padding = 3;
        int width = FontUtils.getFont("sf", 18).getStringWidth(description) + padding * 2;
        int height = 12;
        int x = mouseX + 8;
        int y = mouseY + 8;

        if (x + width > scaledResolution.getScaledWidth()) {
            x = mouseX - width - 4;
        }
        if (y + height > scaledResolution.getScaledHeight()) {
            y = mouseY - height - 4;
        }

        drawRect(x, y, x + width, y + height, RenderUtils.withAlpha(new Color(15, 15, 15), alpha));
        FontUtils.getFont("sf", 18).drawStringWithShadow(
                description,
                x + padding,
                y + 2,
                new Color(255, 255, 255, alpha).getRGB()
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

        for (CategoryTab tab : tabs) {
            if (tab.isHovered(scaledMouseX, scaledMouseY) && mouseButton == 0) {
                if (!anyDragging()) {
                    tab.startDragging(scaledMouseX, scaledMouseY);
                }
            }
            tab.mouseClicked(scaledMouseX, scaledMouseY, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && !areAnyTextFieldsHovered()) {
            beginClose();
            return;
        }

        tabs.forEach(tab -> tab.keyTyped(typedChar, keyCode));
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
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        float amount = wheel > 0 ? -16.0F : 16.0F;
        for (CategoryTab tab : tabs) {
            if (tab.isMouseOver(scaledMouseX, scaledMouseY)) {
                tab.scroll(amount);
                break;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame()
    {
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
        for (CategoryTab tab : tabs) {
            for (ModuleEntry module : tab.modules) {
                for (PropertyEntry setting : module.settings) {
                    if (setting.isTextHovered()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            tabs.forEach(tab -> tab.dragging = false);
        }
        tabs.forEach(tab -> tab.mouseReleased(mouseX, mouseY, state));
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean anyDragging() {
        for (CategoryTab tab : tabs) {
            if (tab.dragging) {
                return true;
            }
        }
        return false;
    }

    public List<CategoryTab> getTabs() {
        return new ArrayList<>(tabs);
    }
}