package ddlc.yuri.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

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

    public static int[] getScaledMouseCoordinates(Minecraft mc, int mouseX, int mouseY) {
        float scale = getScale(mc);
        if (scale == 1f) {
            return new int[]{mouseX, mouseY};
        }
        return new int[]{(int) (mouseX / scale), (int) (mouseY / scale)};
    }
}