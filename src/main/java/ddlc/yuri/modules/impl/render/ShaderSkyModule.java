package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.RenderSkyEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.shader.ShaderUtils;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

@ModuleInfo(label = "Shader Sky", description = "Overrides the minecraft sky with a GLSL shader", category = ModuleCategory.RENDER)
public class ShaderSkyModule extends Module {

    private static final float SKY_RADIUS = 100.0f;
    private static final int RINGS = 12;
    private static final int SEGMENTS = 24;

    private final ModeProperty<SkyMode> mode = new ModeProperty<>("Mode", SkyMode.YURI);
    private final long startTime = System.currentTimeMillis();

    private ShaderUtils nebulaShader;
    private ShaderUtils yuriShader;

    private enum SkyMode {
        YURI("Yuri"),
        NEBULA("Nebula");

        public final String name;

        SkyMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onRenderSky(RenderSkyEvent event) {
        if (mc.theWorld == null) {
            return;
        }

        switch (mode.getValue()) {
            case YURI:
                renderShaderSky(event, () -> renderYuriSky());
                break;
            case NEBULA:
                renderShaderSky(event, () -> renderNebulaSky());
                break;
        }

        event.setCancelled(true);
    }

    private void renderShaderSky(RenderSkyEvent event, Runnable draw) {
        GlStateManager.pushMatrix();

        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();

        draw.run();

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);

        GlStateManager.popMatrix();
    }

    private void renderYuriSky() {
        if (yuriShader == null) {
            yuriShader = new ShaderUtils("yuri");
        }

        float elapsed = (System.currentTimeMillis() - startTime) / 1000.0f;

        yuriShader.init();
        yuriShader.setUniformf("time", elapsed);
        drawSkySphere();
        yuriShader.unload();
    }

    private void renderNebulaSky() {
        if (nebulaShader == null) {
            nebulaShader = new ShaderUtils("nebula");
        }

        float elapsed = (System.currentTimeMillis() - startTime) / 1000.0f;

        nebulaShader.init();
        nebulaShader.setUniformf("time", elapsed);
        drawSkySphere();
        nebulaShader.unload();
    }

    private void drawSkySphere() {
        GL11.glBegin(GL11.GL_QUADS);

        for (int i = 0; i < RINGS; i++) {
            double theta1 = i * Math.PI / RINGS;
            double theta2 = (i + 1) * Math.PI / RINGS;

            for (int j = 0; j < SEGMENTS; j++) {
                double phi1 = j * 2.0 * Math.PI / SEGMENTS;
                double phi2 = (j + 1) * 2.0 * Math.PI / SEGMENTS;

                skyVertex(theta1, phi1);
                skyVertex(theta2, phi1);
                skyVertex(theta2, phi2);
                skyVertex(theta1, phi2);
            }
        }

        GL11.glEnd();
    }

    private void skyVertex(double theta, double phi) {
        float x = (float) (Math.sin(theta) * Math.cos(phi));
        float y = (float) Math.cos(theta);
        float z = (float) (Math.sin(theta) * Math.sin(phi));

        GL11.glTexCoord3f(x, y, z);
        GL11.glVertex3f(x * SKY_RADIUS, y * SKY_RADIUS, z * SKY_RADIUS);
    }
}