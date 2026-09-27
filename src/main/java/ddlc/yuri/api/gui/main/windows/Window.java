package ddlc.yuri.api.gui.main.windows;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.render.PostProcessingModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.shader.impl.Blur;
import ddlc.yuri.utils.render.shader.impl.Shadow;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

/**
 * @author Surge
 * @since 21/08/2022
 */
public class Window {

    @Getter
    private float x;

    @Getter
    private float y;

    @Getter
    private final float width;

    @Getter
    @Setter
    private float height;

    @Getter
    private final float header;

    @Getter
    private boolean dragging = false;

    boolean shouldClose;

    private float lastX;
    private float lastY;

    private final Color BG = new Color(0, 0,0,130);

    public Window(float x, float y, float width, float height, float header) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.header = header;
    }

    public void render(float mouseX, float mouseY) {
        if (dragging) {
            x = mouseX - lastX;
            y = mouseY - lastY;
        }

        RoundedUtils.drawRoundOutline(x, y, width, height, 6, -0.4f, BG, ColorManager.getColor());

        PostProcessingModule.stencilFramebuffer = RenderUtils.createFrameBuffer(PostProcessingModule.stencilFramebuffer, true);
        PostProcessingModule.stencilFramebuffer.framebufferClear();
        PostProcessingModule.stencilFramebuffer.bindFramebuffer(true);
        RenderUtils.resetColor();
        RoundedUtils.drawRoundOutline(x, y, width, height, 6, -0.4f, BG, ColorManager.getColor());
        PostProcessingModule.stencilFramebuffer.unbindFramebuffer();
        RenderUtils.resetColor();

        if (PostProcessingModule.stencilFramebuffer.framebufferTexture > 0) {
            Shadow.renderShadow(
                    PostProcessingModule.stencilFramebuffer.framebufferTexture,
                    18,
                    1,
                    PostProcessingModule.shadowStrength.getValue().floatValue()
            );
        }

        FontUtils.getFont("icons", 18).drawString("I", x + width - FontUtils.getFont("icons", 18).getStringWidth("I") - 5, y + 5, Color.WHITE.getRGB());
    }

    public void mouseClicked(float mouseX, float mouseY, int click) {
        boolean closeHovered = (mouseX >= x + width - 12.5f && mouseY >= y + 1.5f && mouseX <= x + width - 12.5f + 10.5f && mouseY <= y + 1.5f + 10.5f);
        if (click == 0 && mouseOverHeader(mouseX, mouseY)) {
            if (closeHovered) {
                shouldClose = true;
            }

            dragging = true;

            lastX = mouseX - x;
            lastY = mouseY - y;
        }
    }

    public void mouseReleased() {
        dragging = false;
    }

    public void keyTyped(char typedChar, int keyCode) {

    }

    public boolean mouseOverHeader(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + header;
    }

    public boolean shouldWindowClose() {
        return shouldClose;
    }
}