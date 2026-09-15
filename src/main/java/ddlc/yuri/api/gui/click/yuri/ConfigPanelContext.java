package ddlc.yuri.api.gui.click.yuri;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ConfigPanelContext {

    public final Minecraft mc;
    public final float panelX;
    public final float panelY;
    public final float contentX;
    public final float contentY;
    public final float contentWidth;
    public final float contentHeight;
    public final float effectiveWidth;
    public final float effectiveHeight;
    public final int mouseX;
    public final int mouseY;
    public final float safeAlpha;
    public final float slideAnimation;
    public final int animAlpha;

    public ConfigPanelContext(Minecraft mc, float panelX, float panelY, float contentX, float contentY,
                               float contentWidth, float contentHeight, float effectiveWidth, float effectiveHeight,
                               int mouseX, int mouseY, float safeAlpha, float slideAnimation, int animAlpha) {
        this.mc = mc;
        this.panelX = panelX;
        this.panelY = panelY;
        this.contentX = contentX;
        this.contentY = contentY;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.effectiveWidth = effectiveWidth;
        this.effectiveHeight = effectiveHeight;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.safeAlpha = safeAlpha;
        this.slideAnimation = slideAnimation;
        this.animAlpha = animAlpha;
    }

    public int scaledAlpha(Color base) {
        return MathHelper.clamp_int((int) (base.getAlpha() * safeAlpha * slideAnimation), 0, 255);
    }

    public void beginScissor(float x, float y, float width, float height) {
        float rawScaleX = (float) mc.displayWidth / effectiveWidth;
        float rawScaleY = (float) mc.displayHeight / effectiveHeight;

        int scissorX = (int) (x * rawScaleX);
        int scissorY = (int) ((effectiveHeight - (y + height)) * rawScaleY);
        int scissorW = (int) (width * rawScaleX);
        int scissorH = (int) (height * rawScaleY);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(Math.max(0, scissorX), Math.max(0, scissorY), Math.max(1, scissorW), Math.max(1, scissorH));
    }

    public void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
