package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.render.RenderSkyEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.shader.ShaderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import org.lwjgl.opengl.GL11;

import java.time.LocalTime;

@ModuleInfo(label = "Ambience", category = ModuleCategory.RENDER, description = "Changes the world appearance properties: time, color fog and an overridden shader sky")
public class AmbienceModule extends Module {

    public final Property<Boolean> realTime = new Property<Boolean>("Real World Time", false);
    public final NumberProperty time = new NumberProperty("Time", 6000.0f, 0.0f, 24000.0f, 100.0f, () -> !realTime.getValue());
    public static final Property<Boolean> clientColorFog = new Property<Boolean>("Client Color Fog", false);

    public final Property<Boolean> shaderSky = new Property<Boolean>("Shader Sky", false);
    public final ModeProperty<SkyMode> mode = new ModeProperty<>("Mode", SkyMode.YURI, shaderSky::getValue);

    private static final float SKY_RADIUS = 100.0f;
    private static final int RINGS = 12;
    private static final int SEGMENTS = 24;

    private ShaderUtils nebulaShader;
    private ShaderUtils yuriShader;
    private final long startTime = System.currentTimeMillis();

    public enum SkyMode {
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
    public void onTick(ClientTickEvent event) {
        if (mc.theWorld == null) return;

        if (!realTime.getValue()) {
            mc.theWorld.setWorldTime(time.getValue().longValue());
        } else {
            long mTime;

            final LocalTime localTime = LocalTime.now();
            final int hour = localTime.getHour();
            final int minute = localTime.getMinute();

            final long totalMinutes = hour * 60L + minute;
            long minecraftTime = (totalMinutes * 1000L / 1440L) * 24L;
            mTime = (minecraftTime + 18000L) % 24000L;

            mc.theWorld.setWorldTime(mTime);
        }
    }

    @EventHook
    public void onPacketReceive(PacketReceivedEvent event) {
        if (mc.theWorld == null) return;

        if (event.getPacket() instanceof S03PacketTimeUpdate) {
            event.setCancelled(true);
        }
    }

    @EventHook
    public void onRenderSky(RenderSkyEvent event) {
        if (!shaderSky.getValue() || mc.theWorld == null) {
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