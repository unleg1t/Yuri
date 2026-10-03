package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.render.RenderUtils;

public class SigmaModuleButton {

    private final Module module;
    private final SigmaPanel panel;
    private float y;
    private float hover;
    private float enabled;
    private float textOffset;
    private long lastTime = System.currentTimeMillis();

    public SigmaModuleButton(Module module, SigmaPanel panel) {
        this.module = module;
        this.panel = panel;
        this.enabled = module.isEnabled() ? 1f : 0f;
        this.textOffset = module.isEnabled() ? 30f : 22f;
    }

    public Module getModule() {
        return module;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return y;
    }

    public float getHeight() {
        return SigmaTheme.MODULE_HEIGHT;
    }

    public void resetAnimations() {
        hover = 0f;
        enabled = module.isEnabled() ? 1f : 0f;
        textOffset = module.isEnabled() ? 30f : 22f;
        lastTime = System.currentTimeMillis();
    }

    public void draw(float mouseX, float mouseY, float panelX, float alpha) {
        long now = System.currentTimeMillis();
        float delta = Math.min(0.1f, Math.max(0.001f, (now - lastTime) / 1000f));
        lastTime = now;

        boolean hovered = isHovered(mouseX, mouseY);
        boolean mouseDown = hovered && panel.isMouseDown();
        hover = approach(hover, hovered ? 1f : 0f, delta / 0.09f);
        enabled = approach(enabled, module.isEnabled() ? 1f : 0f, delta / 0.14f);
        textOffset = approach(textOffset, module.isEnabled() ? 30f : 22f, (delta / 0.14f) * 8f);

        float hoverFactor;
        if (!hovered) {
            hoverFactor = 0.3f;
        } else if (panel.isDragging()) {
            hoverFactor = 0f;
        } else if (!mouseDown) {
            hoverFactor = hover;
        } else {
            hoverFactor = 1.5f;
        }

        int primary = RenderUtils.interpolateColor(SigmaTheme.DISABLED_BG.getRGB(), SigmaTheme.ENABLED_BLUE.getRGB(), enabled);
        int secondary = RenderUtils.interpolateColor(SigmaTheme.DISABLED_BG_HOVER.getRGB(), SigmaTheme.ENABLED_BLUE_HOVER.getRGB(), enabled);
        int background = SigmaTheme.blend(primary, secondary, 1f - hoverFactor);
        background = SigmaTheme.applyAlpha(background, ((primary >> 24 & 0xFF) / 255f) * alpha);
        SigmaRenderer.rect(panelX, y, panelX + SigmaTheme.PANEL_WIDTH, y + SigmaTheme.MODULE_HEIGHT, background);

        int textColor = RenderUtils.interpolateColor(SigmaTheme.DEEP_TEAL.getRGB(), SigmaTheme.LIGHT_GREYISH_BLUE.getRGB(), enabled);
        float textY = y + SigmaTheme.MODULE_HEIGHT / 2f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 20) / 2f;
        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 20, panelX + textOffset, textY, module.getLabel(),
                SigmaTheme.applyAlpha(textColor, alpha));
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= panel.getX() && mouseX <= panel.getX() + SigmaTheme.PANEL_WIDTH
                && mouseY >= y && mouseY <= y + SigmaTheme.MODULE_HEIGHT
                && mouseY >= panel.getBodyTop() && mouseY <= panel.getBodyBottom();
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (!isHovered(mouseX, mouseY)) {
            return;
        }
        if (mouseButton == 0) {
            module.toggle();
        } else if (mouseButton == 1) {
            panel.openSettings(module);
        }
    }

    private static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        if (current > target) {
            return Math.max(target, current - step);
        }
        return current;
    }
}
