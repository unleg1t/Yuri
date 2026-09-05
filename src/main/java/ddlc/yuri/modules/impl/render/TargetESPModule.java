package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.render.GLUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.animations.Animation;
import ddlc.yuri.utils.render.animations.Direction;
import ddlc.yuri.utils.render.animations.impl.DecelerateAnimation;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@ModuleInfo(label = "Target ESP", category = ModuleCategory.RENDER, description = "Visualizes your current target with various markers")
public final class TargetESPModule extends Module {

    private final ModeProperty<MarkMode> mode = new ModeProperty<>("Mark Mode", MarkMode.POINTS);
    private final ModeProperty<ImageMode> imageMode = new ModeProperty<>("Image Mode", ImageMode.RECTANGLE, () -> mode.getValue() == MarkMode.IMAGE);
    private final Property<Boolean> onlyPlayer = new Property<>("Only Players", true);

    public enum MarkMode {
        POINTS("Points"),
        GHOST("Ghost"),
        IMAGE("Image"),
        SIGMA("Sigma");

        private final String name;

        MarkMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ImageMode {
        RECTANGLE("Rectangle"),
        QUADSTAPPLE("QuadStapple"),
        TRIANGLESTAPPLE("TriangleStapple"),
        TRIANGLESTIPPLE("TriangleStipple");

        private final String name;

        ImageMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Static buffers to prevent direct buffer memory leaks every frame
    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer SCREEN_COORDS = BufferUtils.createFloatBuffer(3);

    private EntityLivingBase target;
    private final TimerUtils timerUtils = new TimerUtils();
    private final long lastTime = System.currentTimeMillis();
    private final Animation alphaAnim = new DecelerateAnimation(400, 1);
    private final ResourceLocation glowCircle = new ResourceLocation("yuri/gui/glow_circle.png");
    private final ResourceLocation rectangle = new ResourceLocation("yuri/gui/rectangle.png");
    private final ResourceLocation quadstapple = new ResourceLocation("yuri/gui/quadstapple.png");
    private final ResourceLocation trianglestapple = new ResourceLocation("yuri/gui/trianglestapple.png");
    private final ResourceLocation trianglestipple = new ResourceLocation("yuri/gui/trianglestipple.png");
    private double[] cachedScreenPos;
    private final float[] viewRotations = new float[2];

    @EventHook
    public void onPlayerAttack(PlayerAttackEvent event) {
        if (event.target != null && (!onlyPlayer.getValue() || event.target instanceof EntityPlayer)) {
            target = event.target;
            alphaAnim.setDirection(Direction.FORWARDS);
            timerUtils.reset();
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        boolean auraActive = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled()
                && AuraModule.target != null
                && (!onlyPlayer.getValue() || AuraModule.target instanceof EntityPlayer);

        if (auraActive) {
            target = AuraModule.target;
            alphaAnim.setDirection(Direction.FORWARDS);
            timerUtils.reset();
        }

        // Handle target death or timeout
        boolean isTargetInvalid = target != null && (target.isDead || target.getHealth() <= 0.0F);

        if (isTargetInvalid || timerUtils.hasTimeElapsed(100)) {
            alphaAnim.setDirection(Direction.BACKWARDS);
            if (alphaAnim.isDone()) {
                target = null;
            }
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (target == null || alphaAnim.getOutput().floatValue() <= 0.001f) {
            cachedScreenPos = null;
            return;
        }

        switch (mode.getValue()) {
            case POINTS:
                points();
                break;
            case IMAGE:
                // Target exact center Y coordinate
                double interpX = target.lastTickPosX + (target.posX - target.lastTickPosX) * mc.timer.renderPartialTicks;
                double interpY = target.lastTickPosY + (target.posY - target.lastTickPosY) * mc.timer.renderPartialTicks + (target.height / 2.0);
                double interpZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * mc.timer.renderPartialTicks;

                cachedScreenPos = projectToScreen(interpX, interpY, interpZ);
                break;
            case GHOST:
                renderGhostESP();
                break;
            case SIGMA:
                renderSigmaTargetESP(mc.timer.renderPartialTicks);
                break;
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mode.getValue() == MarkMode.IMAGE && target != null && cachedScreenPos != null) {
            // Check depth to ensure target is in front of the camera before rendering
            if (cachedScreenPos[2] >= 0.0 && cachedScreenPos[2] < 1.0) {
                drawTargetESP(cachedScreenPos);
            }
        }
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        if (mode.getValue() == MarkMode.IMAGE && target != null && cachedScreenPos != null) {
            // Check depth to ensure target is in front of the camera before rendering
            if (cachedScreenPos[2] >= 0.0 && cachedScreenPos[2] < 1.0) {
                drawTargetESP(cachedScreenPos);
            }
        }
    }

    private void renderGhostESP() {
        float alpha = alphaAnim.getOutput().floatValue();
        if (alpha <= 0.005f) return;

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.shadeModel(7425);
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 1, 0, 1);

        double radius = 0.67;
        float speed = 45;
        float size = 0.4f;
        double distance = 19;
        int length = 20;

        Vec3 interpolated = MathUtils.interpolate(new Vec3(target.lastTickPosX, target.lastTickPosY, target.lastTickPosZ), target.getPositionVector(), mc.timer.renderPartialTicks);
        interpolated.yCoord += 0.75f;

        RenderUtils.setupOrientationMatrix(interpolated.xCoord, interpolated.yCoord + 0.5f, interpolated.zCoord);

        float[] viewRotations = new float[]{mc.getRenderManager().playerViewY, mc.getRenderManager().playerViewX};

        GL11.glRotated(-viewRotations[0], 0.0, 1.0, 0.0);
        GL11.glRotated(viewRotations[1], 1.0, 0.0, 0.0);

        int interfaceColor = ColorManager.getColor().getRGB();

        for (int i = 0; i < length; i++) {
            double angle = 0.15f * (System.currentTimeMillis() - lastTime - (i * distance)) / speed;
            double s = Math.sin(angle) * radius;
            double c = Math.cos(angle) * radius;

            GlStateManager.translate(s, c, -c);
            int color = RenderUtils.applyOpacity(new Color(interfaceColor), alpha).getRGB();
            RenderUtils.drawImage(glowCircle, -size / 2f, -size / 2f, size, size, color);
            GlStateManager.translate(-s, -c, c);
        }

        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }

    private void renderSigmaTargetESP(float partialTicks) {
        if (target == null || mc.getRenderManager() == null) return;

        float alpha = alphaAnim.getOutput().floatValue();
        if (alpha <= 0.005f) return;

        final double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks - mc.getRenderManager().renderPosX;
        final double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + Math.sin(System.currentTimeMillis() / 2E+2) + 1 - mc.getRenderManager().renderPosY;
        final double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

        Color color = ColorManager.getColor();

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glDepthMask(false);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        for (float i = 0; i <= Math.PI * 2 + ((Math.PI * 2) / 25); i += (float) ((Math.PI * 2) / 25)) {
            double vecX = x + 0.67 * Math.cos(i);
            double vecZ = z + 0.67 * Math.sin(i);

            RenderUtils.color(RenderUtils.applyOpacity(color, 0.25f * alpha).getRGB());
            GL11.glVertex3d(vecX, y, vecZ);
        }

        for (float i = 0; i <= Math.PI * 2 + (Math.PI * 2) / 25; i += (Math.PI * 2) / 25) {
            double vecX = x + 0.67 * Math.cos(i);
            double vecZ = z + 0.67 * Math.sin(i);

            RenderUtils.color(RenderUtils.applyOpacity(color, 0.25f * alpha).getRGB());
            GL11.glVertex3d(vecX, y, vecZ);

            RenderUtils.color(RenderUtils.applyOpacity(color, 0.0f).getRGB());
            GL11.glVertex3d(vecX, y - Math.cos(System.currentTimeMillis() / 2E+2) / 2.0F, vecZ);
        }

        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
        RenderUtils.color(Color.WHITE.getRGB());
    }

    private void points() {
        if (target == null) return;

        float alpha = alphaAnim.getOutput().floatValue();
        if (alpha <= 0.005f) return;

        double markerX = MathUtils.interpolate(target.lastTickPosX, target.posX, mc.timer.renderPartialTicks);
        double markerY = MathUtils.interpolate(target.lastTickPosY, target.posY, mc.timer.renderPartialTicks) + target.height / 1.6f;
        double markerZ = MathUtils.interpolate(target.lastTickPosZ, target.posZ, mc.timer.renderPartialTicks);

        float time = (float) ((((System.currentTimeMillis() - lastTime) / 1500F)) + (Math.sin((((System.currentTimeMillis() - lastTime) / 1500F))) / 10f));
        float pl = 0;
        boolean fa = false;

        for (int iteration = 0; iteration < 3; iteration++) {
            for (float i = time * 360; i < time * 360 + 90; i += 2) {
                float max = time * 360 + 90;
                float dc = MathUtils.normalize(i, time * 360 - 45, max);
                float rf = 0.6f;
                double radians = Math.toRadians(i);
                double plY = pl + Math.sin(radians * 1.2f) * 0.1f;

                Color firstColor = RenderUtils.applyOpacity(ColorManager.getColors().getFirst(), alpha * 0.25f);
                Color secondColor = RenderUtils.applyOpacity(ColorManager.getColors().getSecond(), alpha * 0.25f);

                RenderUtils.setupOrientationMatrix(markerX, markerY, markerZ);

                float[] viewRotations = new float[]{mc.getRenderManager().playerViewY, mc.getRenderManager().playerViewX};

                GL11.glRotated(-viewRotations[0], 0.0, 1.0, 0.0);
                GL11.glRotated(viewRotations[1], 1.0, 0.0, 0.0);

                GlStateManager.depthMask(false);
                float q = (!fa ? 0.25f : 0.15f) * (Math.max(fa ? 0.25f : 0.15f, fa ? dc : (1f + (0.4f - dc)) / 2f) + 0.45f);
                float size = q * (2f + ((0.5f - (alpha * 0.5f)) * 2));

                RenderUtils.drawImage(
                        glowCircle,
                        Math.cos(radians) * rf - size / 2f,
                        plY - 0.7,
                        Math.sin(radians) * rf - size / 2f, size, size,
                        firstColor,
                        secondColor,
                        secondColor,
                        firstColor);

                GlStateManager.depthMask(true);
                GlStateManager.popMatrix();
            }
            time *= -1.025f;
            fa = !fa;
            pl += 0.45f;
        }
    }

    private void drawTargetESP(double[] screen) {
        float iconSize = 48F;
        float screenX = (float) screen[0];
        float screenY = (float) screen[1];

        float rotation = (System.currentTimeMillis() % 7200L) / 20.0F;
        ResourceLocation icon = null;

        switch (imageMode.getValue()) {
            case RECTANGLE:
                icon = rectangle;
                break;
            case QUADSTAPPLE:
                icon = quadstapple;
                break;
            case TRIANGLESTAPPLE:
                icon = trianglestapple;
                break;
            case TRIANGLESTIPPLE:
                icon = trianglestipple;
                break;
        }

        if (icon == null) return;

        float alpha = alphaAnim.getOutput().floatValue();
        int color = RenderUtils.applyOpacity(ColorManager.getColor(), alpha).getRGB();

        GL11.glPushMatrix();
        // Translate directly to the projected center point on screen
        GL11.glTranslatef(screenX, screenY, 0F);
        GL11.glRotatef(rotation, 0F, 0F, 1F);

        // Render centered around (0,0)
        RenderUtils.drawImage(icon, -iconSize, -iconSize, iconSize, iconSize, color);

        GL11.glPopMatrix();
    }

    private double[] projectToScreen(double x, double y, double z) {
        double rx = x - mc.getRenderManager().renderPosX;
        double ry = y - mc.getRenderManager().renderPosY;
        double rz = z - mc.getRenderManager().renderPosZ;

        try {
            MODELVIEW.clear();
            PROJECTION.clear();
            VIEWPORT.clear();
            SCREEN_COORDS.clear();

            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
            GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);

            boolean success = GLU.gluProject((float) rx, (float) ry, (float) rz, MODELVIEW, PROJECTION, VIEWPORT, SCREEN_COORDS);

            if (!success) {
                return null;
            }

            double screenX = SCREEN_COORDS.get(0);
            double screenY = mc.displayHeight - SCREEN_COORDS.get(1);
            double depth = SCREEN_COORDS.get(2);

            ScaledResolution sr = new ScaledResolution(mc);
            return new double[]{screenX / sr.getScaleFactor(), screenY / sr.getScaleFactor(), depth};
        } catch (Exception ex) {
            return null;
        }
    }

    private float[] targetESPSPos(EntityLivingBase entity) {
        EntityRenderer entityRenderer = mc.entityRenderer;
        float partialTicks = mc.timer.renderPartialTicks;
        double x = MathUtils.interpolate(entity.prevPosX, entity.posX, partialTicks);
        double y = MathUtils.interpolate(entity.prevPosY, entity.posY, partialTicks);
        double z = MathUtils.interpolate(entity.prevPosZ, entity.posZ, partialTicks);
        double height = entity.height / (entity.isChild() ? 1.75f : 1.0f) / 2.0f;
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x, y + height, z);
        final double[][] vectors = {{bb.minX, bb.minY, bb.minZ},
                {bb.minX, bb.maxY, bb.minZ},
                {bb.minX, bb.maxY, bb.maxZ},
                {bb.minX, bb.minY, bb.maxZ},
                {bb.maxX, bb.minY, bb.minZ},
                {bb.maxX, bb.maxY, bb.minZ},
                {bb.maxX, bb.maxY, bb.maxZ},
                {bb.maxX, bb.minY, bb.maxZ}};
        entityRenderer.setupCameraTransform(partialTicks, 0);
        float[] projection;
        final float[] position = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, -1.0F, -1.0F};
        for (final double[] vec : vectors) {
            projection = GLUtils.project2D((float) (vec[0] - mc.getRenderManager().viewerPosX), (float) (vec[1] - mc.getRenderManager().viewerPosY), (float) (vec[2] - mc.getRenderManager().viewerPosZ), new ScaledResolution(mc).getScaleFactor());
            if (projection != null && projection[2] >= 0.0F && projection[2] < 1.0F) {
                position[0] = Math.min(projection[0], position[0]);
                position[1] = Math.min(projection[1], position[1]);
                position[2] = Math.max(projection[0], position[2]);
                position[3] = Math.max(projection[1], position[3]);
            }
        }
        entityRenderer.setupOverlayRendering();
        if (position[2] < 0.0F) return null;
        return new float[]{(position[0] + position[2]) / 2f, (position[1] + position[3]) / 2f};
    }
}