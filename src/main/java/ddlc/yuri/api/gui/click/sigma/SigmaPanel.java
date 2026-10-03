package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.client.MathUtils;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SigmaPanel {

    private final ModuleCategory category;
    private final SigmaClickGui screen;
    private final List<SigmaModuleButton> modules = new CopyOnWriteArrayList<>();

    private float x;
    private float y;
    private boolean dragging;
    private boolean mouseDown;
    private float dragOffsetX;
    private float dragOffsetY;

    private float scrollOffset;
    private float targetScrollOffset;
    private float scrollbarVisibility;

    private float bodyTop;
    private float bodyBottom;

    public SigmaPanel(ModuleCategory category, float x, float y, SigmaClickGui screen) {
        this.category = category;
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.bodyTop = y + SigmaTheme.HEADER_HEIGHT;
        this.bodyBottom = y + SigmaTheme.PANEL_HEIGHT;
        for (Module module : Yuri.INSTANCE.getModuleManager().getModulesForCategory(category)) {
            modules.add(new SigmaModuleButton(module, this));
        }
    }

    public void resetAnimations() {
        scrollOffset = 0f;
        targetScrollOffset = 0f;
        modules.forEach(SigmaModuleButton::resetAnimations);
    }

    public void update(float mouseX, float mouseY, boolean mouseDown) {
        this.mouseDown = mouseDown && isMouseDownOver(mouseX, mouseY);
        if (dragging) {
            float nx = mouseX - dragOffsetX;
            float ny = mouseY - dragOffsetY;
            Minecraft mc = Minecraft.getMinecraft();
            x = Math.max(0f, Math.min(mc.displayWidth - SigmaTheme.PANEL_WIDTH, nx));
            y = Math.max(0f, Math.min(mc.displayHeight - SigmaTheme.PANEL_HEIGHT, ny));
        }
        bodyTop = y + SigmaTheme.HEADER_HEIGHT;
        bodyBottom = y + SigmaTheme.PANEL_HEIGHT;
    }

    public void draw(float mouseX, float mouseY, float alpha, float centerX, float centerY,
                     float panelScale, float offsetX, float offsetY) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        float visibleHeight = SigmaTheme.PANEL_HEIGHT - SigmaTheme.HEADER_HEIGHT;
        float contentHeight = modules.size() * SigmaTheme.MODULE_HEIGHT;
        float maxScroll = Math.max(0f, contentHeight - visibleHeight);
        targetScrollOffset = Math.max(0f, Math.min(maxScroll, targetScrollOffset));
        scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.35f);
        if (Math.abs(scrollOffset - targetScrollOffset) < 0.5f) {
            scrollOffset = targetScrollOffset;
        }

        SigmaRenderer.glow(x, y, SigmaTheme.PANEL_WIDTH, SigmaTheme.PANEL_HEIGHT, SigmaTheme.GLOW_RADIUS, alpha);
        SigmaRenderer.rect(x, y, x + SigmaTheme.PANEL_WIDTH, y + SigmaTheme.HEADER_HEIGHT,
                SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, Math.min(1f, alpha * 0.9f)));
        SigmaRenderer.rect(x, y + SigmaTheme.HEADER_HEIGHT, x + SigmaTheme.PANEL_WIDTH, y + SigmaTheme.PANEL_HEIGHT,
                SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha));

        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, x + 20f,
                y + SigmaTheme.HEADER_HEIGHT / 2f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 25) / 2f,
                category.getName(), SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, alpha * 0.5f));

        SigmaRenderer.scissor(
                SigmaRenderer.transformCoord(x, centerX, panelScale, offsetX),
                SigmaRenderer.transformCoord(bodyTop, centerY, panelScale, offsetY),
                SigmaRenderer.transformCoord(x + SigmaTheme.PANEL_WIDTH, centerX, panelScale, offsetX),
                SigmaRenderer.transformCoord(bodyTop + visibleHeight, centerY, panelScale, offsetY));
        float rowY = bodyTop - scrollOffset;
        for (SigmaModuleButton button : modules) {
            button.setY(rowY);
            button.draw(mouseX, mouseY, x, alpha);
            rowY += SigmaTheme.MODULE_HEIGHT;
        }
        SigmaRenderer.endScissor();

        drawScrollBar(hovered, visibleHeight, contentHeight, maxScroll, alpha);

        if (scrollOffset > 0f) {
            SigmaRenderer.image(SigmaTheme.SHADOW_BOTTOM, x, y + SigmaTheme.HEADER_HEIGHT,
                    SigmaTheme.PANEL_WIDTH, 18f, alpha * 0.5f);
        }
    }

    private void drawScrollBar(boolean hovered, float visibleHeight, float contentHeight, float maxScroll, float alpha) {
        boolean active = maxScroll > 0f && contentHeight > visibleHeight;
        scrollbarVisibility += active ? 0.05f : -0.05f;
        scrollbarVisibility = Math.min(Math.max(0f, scrollbarVisibility), 1f);
        if (scrollbarVisibility <= 0.01f) {
            return;
        }
        float trackX = x + SigmaTheme.PANEL_WIDTH - 11 - 5;
        float trackY = y + 5;
        float trackHeight = SigmaTheme.PANEL_HEIGHT - 10;
        SigmaRenderer.rect(trackX + 5, trackY, trackX + 8, trackY + trackHeight,
                SigmaTheme.applyAlpha(SigmaTheme.MID_GREY, 0.1f * scrollbarVisibility));
        float thumbHeight = Math.max(20f, trackHeight * (visibleHeight / contentHeight));
        float thumbY = trackY + (trackHeight - thumbHeight) * (maxScroll <= 0f ? 0f : scrollOffset / maxScroll);
        SigmaRenderer.rect(trackX + 4, thumbY, trackX + 8, thumbY + thumbHeight,
                SigmaTheme.applyAlpha(SigmaTheme.MID_GREY, (hovered || dragging ? 0.7f : 0.3f) * scrollbarVisibility));
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (isHeaderHovered(mouseX, mouseY) && mouseButton == 0) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return;
        }
        for (SigmaModuleButton button : modules) {
            button.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void mouseReleased() {
        dragging = false;
        mouseDown = false;
    }

    public void scroll(float amount) {
        float visibleHeight = SigmaTheme.PANEL_HEIGHT - SigmaTheme.HEADER_HEIGHT;
        float maxScroll = Math.max(0f, modules.size() * SigmaTheme.MODULE_HEIGHT - visibleHeight);
        targetScrollOffset = Math.max(0f, Math.min(maxScroll, targetScrollOffset + amount));
    }

    public boolean isMouseDownOver(float mouseX, float mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    public boolean isMouseDown() {
        return mouseDown;
    }

    public boolean isHeaderHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseY >= y && mouseX <= x + SigmaTheme.PANEL_WIDTH
                && mouseY <= y + SigmaTheme.HEADER_HEIGHT;
    }

    public boolean isMouseOver(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + SigmaTheme.PANEL_WIDTH && mouseY >= y
                && mouseY <= y + SigmaTheme.PANEL_HEIGHT;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void openSettings(Module module) {
        if (screen != null) {
            screen.openSettings(module);
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getBodyTop() {
        return bodyTop;
    }

    public float getBodyBottom() {
        return bodyBottom;
    }

    public ModuleCategory getCategory() {
        return category;
    }
}
