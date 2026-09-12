package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.FastUseModule;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Trajectories", description = "Shows the predicted landing of projectiles", category = ModuleCategory.RENDER)
public class TrajectoriesModule extends Module {

    private static final int MAX_STEPS = 300;
    private static final int START_ALPHA = 235;
    private static final int END_ALPHA = 40;
    private static final float LINE_WIDTH = 2.25F;

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null) return;

        Item item = heldItem.getItem();

        float motionFactor = 1.5F;
        float motionSlowdown = 0.99F;
        float gravity;
        float size;

        float partialTicks = mc.timer.renderPartialTicks;

        if (item instanceof ItemBow) {
            if (!mc.thePlayer.isUsingItem()) return;

            gravity = 0.05F;
            size = 0.3F;

            int maxUseDuration = heldItem.getMaxItemUseDuration();
            int count = Yuri.INSTANCE.getModuleManager().getModule(FastUseModule.class).isEnabled() && FastUseModule.bow.getValue() ? 31 : mc.thePlayer.getItemInUseCount();
            float useTicks = (float) (maxUseDuration - count) + partialTicks;

            float power = useTicks / 20.0F;
            power = (power * power + power * 2.0F) / 3.0F;

            if (power < 0.1F) return;
            if (power > 1.0F) power = 1.0F;

            motionFactor = power * 3.0F;
        } else if (item instanceof ItemFishingRod) {
            gravity = 0.04F;
            size = 0.25F;
            motionSlowdown = 0.92F;
        } else if (item instanceof ItemPotion && ItemPotion.isSplash(heldItem.getMetadata())) {
            gravity = 0.05F;
            size = 0.25F;
            motionFactor = 0.5F;
        } else if (item instanceof ItemSnowball || item instanceof ItemEnderPearl || item instanceof ItemEgg) {
            gravity = 0.03F;
            size = 0.25F;
        } else {
            return;
        }

        float yaw = RotationManager.isActive() ? RotationManager.rotations.x : mc.thePlayer.rotationYaw;
        float pitch = RotationManager.isActive() ? RotationManager.rotations.y : mc.thePlayer.rotationPitch;
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double playerY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;

        double posX = playerX - Math.cos(yawRad) * 0.16D;
        double posY = playerY + mc.thePlayer.getEyeHeight() - 0.1D;
        double posZ = playerZ - Math.sin(yawRad) * 0.16D;

        double motionX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double motionY = -Math.sin(pitchRad);
        double motionZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double dist = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX = (motionX / dist) * motionFactor;
        motionY = (motionY / dist) * motionFactor;
        motionZ = (motionZ / dist) * motionFactor;

        List<Vec3> points = new ArrayList<>();
        Vec3 hitVec = null;

        for (int step = 0; step < MAX_STEPS && posY > 0.0; step++) {
            Vec3 current = new Vec3(posX, posY, posZ);
            Vec3 next = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);

            MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(current, next, false, true, false);
            if (blockHit != null) {
                hitVec = blockHit.hitVec;
                points.add(hitVec);
                break;
            }

            points.add(current);

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            AxisAlignedBB sweepBox = new AxisAlignedBB(posX - size, posY - size, posZ - size,
                    posX + size, posY + size, posZ + size).addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D);

            List<Entity> nearby = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, sweepBox);
            boolean intercepted = false;
            for (Entity entity : nearby) {
                if (!entity.canBeCollidedWith()) continue;

                AxisAlignedBB entityBox = entity.getEntityBoundingBox().expand(size, size, size);
                MovingObjectPosition entityHit = entityBox.calculateIntercept(current, next);
                if (entityHit != null) {
                    hitVec = entityHit.hitVec;
                    points.add(hitVec);
                    intercepted = true;
                    break;
                }
            }

            if (intercepted) break;

            Material material = mc.theWorld.getBlockState(new BlockPos(posX, posY, posZ)).getBlock().getMaterial();
            if (material == Material.water) {
                motionX *= 0.6D;
                motionY *= 0.6D;
                motionZ *= 0.6D;
            } else {
                motionX *= motionSlowdown;
                motionY *= motionSlowdown;
                motionZ *= motionSlowdown;
            }

            motionY -= gravity;
        }

        if (points.size() < 2) return;

        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;

        Color color = ColorManager.getColor();

        GlStateManager.pushMatrix();
        enableGL();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int last = points.size() - 1;
        for (int i = 0; i <= last; i++) {
            Vec3 point = points.get(i);
            float progress = last == 0 ? 1F : (float) i / last;
            int alpha = (int) (START_ALPHA + (END_ALPHA - START_ALPHA) * progress);

            buffer.pos(point.xCoord - renderX, point.yCoord - renderY, point.zCoord - renderZ)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
                    .endVertex();
        }

        tessellator.draw();

        if (hitVec != null) {
            drawImpactMarker(hitVec, renderX, renderY, renderZ, color);
        }

        disableGL();
        GlStateManager.popMatrix();
    }

    private void drawImpactMarker(Vec3 hitVec, double renderX, double renderY, double renderZ, Color color) {
        double outerSize = 0.28D;
        double innerSize = 0.14D;

        AxisAlignedBB outer = new AxisAlignedBB(
                hitVec.xCoord - outerSize - renderX, hitVec.yCoord - outerSize - renderY, hitVec.zCoord - outerSize - renderZ,
                hitVec.xCoord + outerSize - renderX, hitVec.yCoord + outerSize - renderY, hitVec.zCoord + outerSize - renderZ
        );
        AxisAlignedBB inner = new AxisAlignedBB(
                hitVec.xCoord - innerSize - renderX, hitVec.yCoord - innerSize - renderY, hitVec.zCoord - innerSize - renderZ,
                hitVec.xCoord + innerSize - renderX, hitVec.yCoord + innerSize - renderY, hitVec.zCoord + innerSize - renderZ
        );

        RenderUtils.color(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50).getRGB());
        RenderUtils.drawBoundingBox(outer);

        RenderUtils.color(new Color(color.getRed(), color.getGreen(), color.getBlue(), 190).getRGB());
        RenderUtils.drawBoundingBox(inner);
    }

    private void enableGL() {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(LINE_WIDTH);
    }

    private void disableGL() {
        GL11.glPopAttrib();
    }
}