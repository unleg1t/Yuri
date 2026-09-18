package ddlc.yuri.api.gui.initalization;

import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

public class YuriInitializationScreen {
    public static void drawInitScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, sr.getScaledWidth(), sr.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);

        RenderUtils.drawImage(new ResourceLocation("yuri/gui/yuri_splash.jpg"), 0, 0, sr.getScaledWidth(), sr.getScaledHeight());

        GlStateManager.disableBlend();
        mc.updateDisplay();
    }
}
