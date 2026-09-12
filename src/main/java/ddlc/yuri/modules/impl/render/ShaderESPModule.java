package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.shader3d.FramebufferShader;
import ddlc.yuri.utils.render.shader3d.impl.GlowShader;
import ddlc.yuri.utils.render.shader3d.impl.OutlineShader;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@ModuleInfo(label = "Shader ESP", description = "Renders a shader effect around entities", category = ModuleCategory.RENDER)
public class ShaderESPModule extends Module {

    private final ModeProperty<ShaderMode> mode = new ModeProperty<>("Mode", ShaderMode.OUTLINE);

    private enum ShaderMode {
        GLOW("Glow"),
        OUTLINE("Outline");

        public final String name;

        ShaderMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private boolean render = true;
    private final ICamera frustum = new Frustum();

    private boolean shouldRender(Entity entity) {
        return entity instanceof EntityPlayer && (!(entity instanceof EntityPlayerSP) || mc.gameSettings.thirdPersonView != 0);
    }

    public boolean isRenderingESP() {
        return this.render;
    }

    @Override
    public void onDisable() {
        render = true;
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        render = true;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        final boolean glow = mode.getValue() == ShaderMode.GLOW;
        final FramebufferShader shader = glow ? GlowShader.GLOW_SHADER : OutlineShader.OUTLINE_SHADER;

        shader.startDraw(event.partialTicks);

        render = false;
        try {
            Entity view = mc.getRenderViewEntity();
            if (view != null) {
                double x = view.lastTickPosX + (view.posX - view.lastTickPosX) * mc.timer.renderPartialTicks;
                double y = view.lastTickPosY + (view.posY - view.lastTickPosY) * mc.timer.renderPartialTicks;
                double z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * mc.timer.renderPartialTicks;
                frustum.setPosition(x, y, z);
            }

            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (shouldRender(player) && frustum.isBoundingBoxInFrustum(player.getEntityBoundingBox())) {
                    mc.getRenderManager().renderEntityStatic(player, mc.timer.renderPartialTicks, true);
                }
            }
        } finally {
            render = true;
        }

        float radius = glow ? 3f : 1.3f;
        float intensity = glow ? 1.5f : 1f;

        shader.stopDraw(ColorManager.getColor(), radius, intensity);
    }
}
