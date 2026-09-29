package ddlc.yuri.api.gui.main.api;

import ddlc.yuri.api.gui.click.novoline.GuiTheme;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;

import java.awt.Color;

public class MenuButton {

    public static final float DEFAULT_WIDTH = 85f;
    private static final Color BG_COLOR = new Color(0, 0, 0, 130);

    public final String label;
    public final Runnable action;
    private Float widthOverride;
    private Float heightOverride;
    private Color textColor = GuiTheme.TEXT;

    public float x, y, width, height;
    public float hoverAnim;

    public MenuButton(String label, Runnable action) {
        this.label = label;
        this.action = action;
        this.widthOverride = DEFAULT_WIDTH;
    }

    public MenuButton(String label, Runnable action, float width) {
        this(label, action);
        this.widthOverride = width;
    }

    public MenuButton(String label, Runnable action, float width, float height) {
        this(label, action, width);
        this.heightOverride = height;
        this.width = width;
        this.height = height;
    }

    public float getLayoutWidth(float textWidth) {
        return widthOverride == null ? textWidth + 16f : widthOverride;
    }

    public float getLayoutHeight(float textHeight) {
        return heightOverride == null ? textHeight + 10f : heightOverride;
    }

    public void layout(float textX, float textY, float textWidth, float textHeight) {
        this.x = textX - 8f;
        this.y = textY - 5f;
        this.width = widthOverride == null ? textWidth + 16f : widthOverride;
        this.height = getLayoutHeight(textHeight);
    }

    public void layoutBox(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateHover(float mouseX, float mouseY) {
        float target = this.isHovered(mouseX, mouseY) ? 1.00f : 0.00f;
        float speed = 12f / 1000f;
        hoverAnim += (target - hoverAnim) * (1f - (float) Math.exp(-speed * RenderUtils.delta));
    }

    public int getTextColor() {
        return RenderUtils.interpolateColor(textColor, ColorManager.getColor(), hoverAnim);
    }

    public void renderBox() {
        RoundedUtils.drawRoundedRect(this.x, this.y, this.width, this.height, 2f, BG_COLOR);

        if (this.hoverAnim > 0.001f) {
            float ease = 1f - (1f - this.hoverAnim) * (1f - this.hoverAnim);
            float animatedWidth = this.width * ease;
            float animatedX = this.x + (this.width - animatedWidth) / 2f;
            float animatedHeight = 1.5f;
            float animatedY = this.y + this.height - animatedHeight;

            RoundedUtils.drawRoundedRect(animatedX, animatedY, animatedWidth, animatedHeight, 0.2f, ColorManager.getColor());
        }
    }

    public void mouseClicked() {
        if (action != null) {
            action.run();
        }
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x &&
                mouseX <= x + width &&
                mouseY >= y &&
                mouseY <= y + height;
    }
}