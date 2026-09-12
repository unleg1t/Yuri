package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;


@ModuleInfo(label = "Pentagram", description = "Displays a pentagram under your feet", category = ModuleCategory.RENDER)
public class PentagramModule extends Module {

    private static final ResourceLocation DEMON_TEXTURE = new ResourceLocation("yuri/gui/demon.png");

    public enum Mode {
        ASTOLFO("Astolfo"),
        DEMON("Demon");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.DEMON);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mode.getValue() == Mode.DEMON) {
            renderDemon();
        } else {
            renderAstolfoPentagram();
        }
    }

    private void renderAstolfoPentagram() {
        if (mc.thePlayer == null) return;

        final float partialTicks = mc.timer.renderPartialTicks;
        final Color color = ColorManager.getColor();

        final double x = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
        final double y = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks - mc.getRenderManager().renderPosY + 0.02;
        final double z = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

        final double radius = 1.4;
        final double rotation = (System.currentTimeMillis() % 6000L) / 6000.0 * 360.0;

        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(2.0F);
        GL11.glDepthMask(false);
        GlStateManager.disableCull();

        RenderUtils.color(color.getRGB());
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(rotation + i * 144.0);
            double vecX = x + radius * Math.cos(angle);
            double vecZ = z + radius * Math.sin(angle);
            GL11.glVertex3d(vecX, y, vecZ);
        }

        GL11.glEnd();

        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glPopMatrix();
        RenderUtils.resetColor();
    }

    private void renderDemon() {
        if (mc.thePlayer == null) return;

        final float partialTicks = mc.timer.renderPartialTicks;

        final double x = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
        final double y = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks - mc.getRenderManager().renderPosY + 0.02;
        final double z = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

        final double size = 2.8;
        final double rotation = (System.currentTimeMillis() % 6000L) / 6000.0 * 360.0;
        final double rad = Math.toRadians(rotation);
        final double cos = Math.cos(rad);
        final double sin = Math.sin(rad);
        final double half = size / 2.0;

        final double[][] local = {
                {-half, -half, 0.0, 0.0},
                {half, -half, 1.0, 0.0},
                {half, half, 1.0, 1.0},
                {-half, half, 0.0, 1.0}
        };

        mc.getTextureManager().bindTexture(DEMON_TEXTURE);

        GL11.glPushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();
        GL11.glDepthMask(false);
        GlStateManager.color(ColorManager.getColor().getRed() / 255f, ColorManager.getColor().getGreen() / 255f, ColorManager.getColor().getBlue() / 255f, 0.5F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        for (double[] point : local) {
            double rx = point[0] * cos - point[1] * sin;
            double rz = point[0] * sin + point[1] * cos;
            worldrenderer.pos(x + rx, y, z + rz).tex(point[2], point[3]).endVertex();
        }

        tessellator.draw();

        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
        RenderUtils.resetColor();
    }
}