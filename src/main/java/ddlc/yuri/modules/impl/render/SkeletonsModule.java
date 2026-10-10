package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.ModalUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@ModuleInfo(label = "Skeletons", description = "Renders skeletons through walls", category = ModuleCategory.RENDER)
public class SkeletonsModule extends Module {

    // omg @flaily look I fixed the memory leak!!! so cool guys!

    public final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    public final Property<Boolean> useClientColors = new Property<>("Use Client Colors", false);
    public static final NumberProperty skeletonsWidth = new NumberProperty("Skeletons Width", 1.0, 0.5, 5, 0.1);

    private final Map<Integer, float[][]> skeletonAngles = new HashMap<>();

    @EventHook
    public void onModelUpdate(ModalUpdateEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        ModelPlayer model = event.getModel();
        int id = (event.getPlayer()).getEntityId();

        float[][] angles = skeletonAngles.computeIfAbsent(id, k -> new float[5][3]);

        store(angles[0], model.bipedHead.rotateAngleX, model.bipedHead.rotateAngleY, model.bipedHead.rotateAngleZ);
        store(angles[1], model.bipedRightArm.rotateAngleX, model.bipedRightArm.rotateAngleY, model.bipedRightArm.rotateAngleZ);
        store(angles[2], model.bipedLeftArm.rotateAngleX, model.bipedLeftArm.rotateAngleY, model.bipedLeftArm.rotateAngleZ);
        store(angles[3], model.bipedRightLeg.rotateAngleX, model.bipedRightLeg.rotateAngleY, model.bipedRightLeg.rotateAngleZ);
        store(angles[4], model.bipedLeftLeg.rotateAngleX, model.bipedLeftLeg.rotateAngleY, model.bipedLeftLeg.rotateAngleZ);
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            skeletonAngles.clear();
            return;
        }

        prune();

        float partialTicks = mc.timer.renderPartialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        for (EntityPlayer entity : mc.theWorld.playerEntities) {

            if (entity.isInvisible() || !entity.isEntityAlive()) {
                continue;
            }

            if (!entity.equals(mc.thePlayer) || (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue())) {
                drawSkeleton(entity, partialTicks);
            }
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void prune() {
        Iterator<Integer> it = skeletonAngles.keySet().iterator();
        while (it.hasNext()) {
            Entity e = mc.theWorld.getEntityByID(it.next());
            if (!(e instanceof EntityPlayer) || e.isDead) {
                it.remove();
            }
        }
    }

    private void store(float[] dest, float x, float y, float z) {
        dest[0] = x;
        dest[1] = y;
        dest[2] = z;
    }

    private void drawSkeleton(EntityPlayer entity, float partialTicks) {
        float[][] angles = skeletonAngles.get(entity.getEntityId());
        if (angles == null || entity.isDead || entity.isPlayerSleeping()) {
            return;
        }

        Color color = useClientColors.getValue() ? ColorManager.getColor().brighter() : Color.WHITE;
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F);
        GL11.glLineWidth(skeletonsWidth.getValue().floatValue());

        RenderManager renderManager = mc.getRenderManager();
        double interpX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double interpY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double interpZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(interpX - renderManager.renderPosX, interpY - renderManager.renderPosY, interpZ - renderManager.renderPosZ);

        float yawOffset = entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;
        GL11.glRotatef(-yawOffset, 0.0F, 1.0F, 0.0F);
        GL11.glTranslated(0.0, 0.0, entity.isSneaking() ? -0.235 : 0.0);

        float legHeight = entity.isSneaking() ? 0.6F : 0.75F;

        GL11.glPushMatrix();
        GL11.glTranslated(-0.125, legHeight, 0.0);
        applyAngles(angles[3]);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        GL11.glVertex3d(0.0, -legHeight, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.125, legHeight, 0.0);
        applyAngles(angles[4]);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        GL11.glVertex3d(0.0, -legHeight, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glTranslated(0.0, 0.0, entity.isSneaking() ? 0.25 : 0.0);

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, entity.isSneaking() ? -0.05 : 0.0, entity.isSneaking() ? -0.01725 : 0.0);

        GL11.glPushMatrix();
        GL11.glTranslated(-0.375, legHeight + 0.55, 0.0);
        applyAngles(angles[1]);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        GL11.glVertex3d(0.0, -0.5, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.375, legHeight + 0.55, 0.0);
        applyAngles(angles[2]);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        GL11.glVertex3d(0.0, -0.5, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glRotatef(yawOffset - entity.rotationYawHead, 0.0F, 1.0F, 0.0F);
        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight + 0.55, 0.0);
        if (angles[0][0] != 0.0F) {
            GL11.glRotatef(angles[0][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        }
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.3, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glPopMatrix();

        GL11.glRotatef(entity.isSneaking() ? 25.0F : 0.0F, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(0.0, entity.isSneaking() ? -0.16175 : 0.0, entity.isSneaking() ? -0.48025 : 0.0);

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight, 0.0);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(-0.125, 0.0, 0.0);
        GL11.glVertex3d(0.125, 0.0, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight, 0.0);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.55, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight + 0.55, 0.0);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(-0.375, 0.0, 0.0);
        GL11.glVertex3d(0.375, 0.0, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPopMatrix();
    }

    private void applyAngles(float[] angles) {
        if (angles[0] != 0.0F) {
            GL11.glRotatef(angles[0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        }
        if (angles[1] != 0.0F) {
            GL11.glRotatef(angles[1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        }
        if (angles[2] != 0.0F) {
            GL11.glRotatef(angles[2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        }
    }
}