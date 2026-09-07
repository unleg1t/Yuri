package ddlc.yuri.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public final class ScaleUtils {

    private ScaleUtils() {
    }

    public static float getScale(Minecraft mc) {
        if (mc.gameSettings.guiScale <= 1) {
            return 1f;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        return resolution.getScaleFactor() / 2.0F;
    }

    public static void scale(Minecraft mc) {
        float scale = getScale(mc);
        if (scale == 1f) {
            return;
        }
        GlStateManager.scale(scale, scale, scale);
    }

    public static void applyScissor(Minecraft mc, float x1, float y1, float x2, float y2) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float scale = getScale(mc) * scaledResolution.getScaleFactor();
        int deviceX = (int) (x1 * scale);
        int deviceY = (int) ((scaledResolution.getScaledHeight() - y2) * scale);
        int deviceWidth = Math.max(0, (int) ((x2 - x1) * scale));
        int deviceHeight = Math.max(0, (int) ((y2 - y1) * scale));
        GL11.glScissor(deviceX, deviceY, deviceWidth, deviceHeight);
    }

    public static int[] getScaledMouseCoordinates(Minecraft mc, int mouseX, int mouseY) {
        float scale = getScale(mc);
        if (scale == 1f) {
            return new int[]{mouseX, mouseY};
        }
        return new int[]{(int) (mouseX / scale), (int) (mouseY / scale)};
    }
}