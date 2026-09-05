package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.ESPUtils;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.block.BlockChest;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

@ModuleInfo(label = "ESP", description = "Renders an ESP around entities", category = ModuleCategory.RENDER)
public class ESPModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.BOX);
    public final Property<Boolean> healthBars = new Property<>("Health Bars", true);
    public final Property<Boolean> heldItem = new Property<>("Held Item", true);
    public final Property<Boolean> chestEsp = new Property<>("Chest ESP", true);
    public final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    public final Property<Boolean> healthBarLeftAlign = new Property<>("Health Bar Left Align", true);
    public final Property<Boolean> outline = new Property<>("ESP Outline", true);
    public final Property<Boolean> healthOutline = new Property<>("Health Outline", true);
    public final Property<Boolean> background = new Property<>("ESP BG", true);
    public final NumberProperty espWidth = new NumberProperty("ESP Line Width", 2.0, 0.1, 4.0, 0.1);
    public final NumberProperty outlineWidth = new NumberProperty("ESP Outline Width", 2.0, 0.1, 4.0, 0.1, outline::getValue);
    public final NumberProperty outlineAlpha = new NumberProperty("ESP Outline Alpha", 1.0, 0.1, 1.0, 0.1, outline::getValue);
    public final NumberProperty bgAlpha = new NumberProperty("ESP BG Alpha", 0.2, 0.1, 1.0, 0.1, background::getValue);
    public final NumberProperty healthBarWidth = new NumberProperty("Health Line Width", 2.0, 0.7, 4.0, 0.1, healthBars::getValue);
    public final NumberProperty healthBarAlpha = new NumberProperty("Health Line Alpha", 1.0, 0.1, 1.0, 0.1, healthBars::getValue);
    public final NumberProperty healthOutlineWidth = new NumberProperty("Health Outline Width", 2.0, 0.1, 4.0, 0.1, healthBars::getValue);
    public final NumberProperty healthOutlineAlpha = new NumberProperty("Health Outline Alpha", 1.0, 0.1, 1.0, 0.1, healthBars::getValue);
    public final NumberProperty healthBgAlpha = new NumberProperty("Health BG Alpha", 0.5, 0.1, 1.0, 0.1, healthBars::getValue);
    public final Property<Boolean> heldItemCustomFont = new Property<>("Held Item Custom Font", false, heldItem::getValue);
    public final NumberProperty heldItemScale = new NumberProperty("Held Item Scale", 1.0, 0.5, 2.0, 0.1, heldItem::getValue);
    public final Property<Boolean> heldItemBackground = new Property<>("Held Item BG", true, heldItem::getValue);
    public final NumberProperty heldItemBackgroundAlpha = new NumberProperty("Held Item BG Alpha", 0.5, 0.1, 1.0, 0.1, () -> heldItem.getValue() && heldItemBackground.getValue());

    public enum Mode {
        BOX("Box"),
        CORNERS("Corners");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final NumberFormat df = new DecimalFormat("0.#");
    private final Color backgroundColor = new Color(10, 10, 10, 130);
    private static ScaledResolution sr = new ScaledResolution(mc);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        Color color = ColorManager.getColor();
        Color outlineColor = outline.getValue()
                ? new Color(0, 0, 0, outlineAlpha.getValue().floatValue())
                : null;

        int radius = 10;
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float espLineWidth = espWidth.getValue().floatValue();
        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        if (chestEsp.getValue()) {
            int px = MathHelper.floor_double(playerPos.xCoord);
            int py = MathHelper.floor_double(playerPos.yCoord);
            int pz = MathHelper.floor_double(playerPos.zCoord);

            for (int dx = -radius; dx < radius; dx++) {
                for (int dy = -radius; dy < radius; dy++) {
                    for (int dz = -radius; dz < radius; dz++) {
                        int bx = px + dx;
                        int by = py + dy;
                        int bz = pz + dz;
                        BlockPos pos = new BlockPos(bx, by, bz);
                        net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(pos);
                        if (state.getBlock() instanceof BlockChest) {
                            AxisAlignedBB bb = state.getBlock().getSelectedBoundingBox(mc.theWorld, pos);

                            if (bb == null) {
                                bb = new AxisAlignedBB(bx, by, bz, bx + 1, by + 1, bz + 1);
                            }

                            render2DESP(new AxisAlignedBB(
                                            bb.minX - viewerX, bb.minY - viewerY, bb.minZ - viewerZ,
                                            bb.maxX - viewerX, bb.maxY - viewerY, bb.maxZ - viewerZ),
                                    color, espLineWidth, null, scaledResolution, outlineColor);
                        }
                    }
                }
            }
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer) {
                if (!entity.equals(mc.thePlayer) || (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue())) {
                    AxisAlignedBB bb = entity.getEntityBoundingBox();
                    double x = interpolate(entity.lastTickPosX, entity.posX) - viewerX;
                    double y = interpolate(entity.lastTickPosY, entity.posY) - viewerY;
                    double z = interpolate(entity.lastTickPosZ, entity.posZ) - viewerZ;
                    render2DESP(new AxisAlignedBB(
                                    bb.minX + x, bb.minY + y, bb.minZ + z,
                                    bb.maxX + x, bb.maxY + y, bb.maxZ + z),
                            color, espLineWidth, (EntityLivingBase) entity, scaledResolution, outlineColor);
                }
            }
        }
    }

    private void render2DESP(AxisAlignedBB axisAlignedBB, Color color, float lineWidth, EntityLivingBase livingEntity, ScaledResolution scaledResolution, Color outlineColor) {
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        int validPoints = 0;

        ESPUtils.windPos.clear();

        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                for (int y = 0; y < 2; y++) {
                    if (GLU.gluProject((float) (x == 1 ? axisAlignedBB.minX : axisAlignedBB.maxX),
                            (float) (y == 1 ? axisAlignedBB.minY : axisAlignedBB.maxY),
                            (float) (z == 1 ? axisAlignedBB.minZ : axisAlignedBB.maxZ),
                            ActiveRenderInfo.MODELVIEW,
                            ActiveRenderInfo.PROJECTION,
                            ActiveRenderInfo.VIEWPORT,
                            ESPUtils.windPos)) {
                        if (ESPUtils.windPos.get(2) > 1) {
                            continue;
                        }

                        double screenX = (ESPUtils.windPos.get(0) / scaledResolution.getScaleFactor());
                        double screenY = (ESPUtils.windPos.get(1) / scaledResolution.getScaleFactor());

                        minX = Math.min(screenX, minX);
                        minY = Math.min(screenY, minY);
                        maxX = Math.max(screenX, maxX);
                        maxY = Math.max(screenY, maxY);
                        validPoints++;
                    }
                }
            }
        }

        if (validPoints == 0) {
            return;
        }

        double flippedMinY = screenHeight - minY;
        double flippedMaxY = screenHeight - maxY;
        double topY = Math.min(flippedMinY, flippedMaxY);
        double bottomY = Math.max(flippedMinY, flippedMaxY);

        if (maxX < 0 || minX > screenWidth || bottomY < 0 || topY > screenHeight) {
            return;
        }

        double drawMinX = Math.max(0, minX);
        double drawMinY = Math.max(0, topY);
        double drawMaxX = Math.min(screenWidth, maxX);
        double drawMaxY = Math.min(screenHeight, bottomY);

        double margin = 3;
        drawMinX -= margin;
        drawMinY -= margin;
        drawMaxX += margin;
        drawMaxY += margin;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);

        if (background.getValue()) {
            GL11.glColor4d(0, 0, 0, bgAlpha.getValue().floatValue());
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2d(drawMinX, drawMinY);
            GL11.glVertex2d(drawMaxX, drawMinY);
            GL11.glVertex2d(drawMaxX, drawMaxY);
            GL11.glVertex2d(drawMinX, drawMaxY);
            GL11.glEnd();
        }

        if (outline.getValue() && outlineColor != null) {
            if (mode.getValue() == Mode.CORNERS) {
                drawCorners(drawMinX, drawMinY, drawMaxX, drawMaxY, outlineColor, lineWidth + outlineWidth.getValue().floatValue());
            } else {
                drawBox(drawMinX, drawMinY, drawMaxX, drawMaxY, outlineColor, lineWidth + outlineWidth.getValue().floatValue());
            }
        }

        if (mode.getValue() == Mode.CORNERS) {
            drawCorners(drawMinX, drawMinY, drawMaxX, drawMaxY, color, lineWidth);
        } else {
            drawBox(drawMinX, drawMinY, drawMaxX, drawMaxY, color, lineWidth);
        }

        if (healthBars.getValue() && livingEntity != null) {
            drawHealthBar(livingEntity, drawMinX, drawMaxX, drawMinY, drawMaxY);
        }

        if (heldItem.getValue() && livingEntity != null && livingEntity.getHeldItem() != null) {
            drawHeldItem(livingEntity, drawMinX, drawMaxX, drawMaxY);
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GlStateManager.resetColor();
    }

    private void drawHealthBar(EntityLivingBase entity, double boxMinX, double boxMaxX, double boxTopY, double boxBottomY) {
        double ratio = MathHelper.clamp_double(entity.getHealth() / entity.getMaxHealth(), 0.0, 1.0);
        int hc = ratio < 0.3D ? Color.red.getRGB() : (ratio < 0.5D ? Color.orange.getRGB() : (ratio < 0.7D ? Color.yellow.getRGB() : Color.green.getRGB()));
        float hr = (hc >> 16 & 255) / 255f;
        float hg = (hc >> 8 & 255) / 255f;
        float hb = (hc & 255) / 255f;

        double barWidth = healthBarWidth.getValue().floatValue();
        double barX = boxMaxX + healthBarWidth.getValue().floatValue() + 1;
        double filledTop = boxBottomY - (boxBottomY - boxTopY) * ratio;
        double outLinedWidth = healthOutlineWidth.getValue();

        if (healthBarLeftAlign.getValue()) barX = boxMinX - barWidth - 3;

        if (healthOutline.getValue()) {
            GL11.glColor4d(0, 0, 0, healthOutlineAlpha.getValue().floatValue());
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2d(barX - outLinedWidth, boxTopY - outLinedWidth);
            GL11.glVertex2d(barX + barWidth + outLinedWidth, boxTopY - outLinedWidth);
            GL11.glVertex2d(barX + barWidth + outLinedWidth, boxBottomY + outLinedWidth);
            GL11.glVertex2d(barX - outLinedWidth, boxBottomY + outLinedWidth);
            GL11.glEnd();
        }

        GL11.glColor4d(Color.darkGray.getRed() / 255d, Color.darkGray.getGreen() / 255d, Color.darkGray.getBlue() / 255d, healthBgAlpha.getValue().floatValue());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(barX, boxTopY);
        GL11.glVertex2d(barX + barWidth, boxTopY);
        GL11.glVertex2d(barX + barWidth, boxBottomY);
        GL11.glVertex2d(barX, boxBottomY);
        GL11.glEnd();

        GL11.glColor4d(hr, hg, hb, healthBarAlpha.getValue().floatValue());
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(barX, filledTop);
        GL11.glVertex2d(barX + barWidth, filledTop);
        GL11.glVertex2d(barX + barWidth, boxBottomY);
        GL11.glVertex2d(barX, boxBottomY);
        GL11.glEnd();
    }

    private void drawHeldItem(EntityLivingBase entity, double minX, double maxX, double maxY) {
        String itemName = entity.getHeldItem().getDisplayName();
        float scale = heldItemScale.getValue().floatValue();
        double itemWidth = getStringWidth(itemName);

        double centerX = (minX + maxX) / 2.0;
        double itemY = maxY + 4.0;

        GL11.glPushMatrix();
        GL11.glTranslated(centerX, itemY, 0.0);
        GL11.glScaled(scale, scale, 1.0);

        double itemX = -itemWidth / 2.0;

        if (heldItemBackground.getValue()) {
            GL11.glColor4d(0, 0, 0, heldItemBackgroundAlpha.getValue().floatValue());
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2d(itemX - 2, -1);
            GL11.glVertex2d(itemX + itemWidth + 2, -1);
            GL11.glVertex2d(itemX + itemWidth + 2, 9);
            GL11.glVertex2d(itemX - 2, 9);
            GL11.glEnd();
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (heldItemCustomFont.getValue()) {
            drawString(itemName, itemX, 0, -1);
        } else {
            mc.fontRendererObj.drawStringWithShadow(itemName, (float) itemX, 0F, -1);
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glPopMatrix();
    }

    private void drawBox(double minX, double minY, double maxX, double maxY, Color color, float lineWidth) {
        GL11.glColor4d(color.getRed() / 255d, color.getGreen() / 255d, color.getBlue() / 255d, color.getAlpha() / 255d);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX, minY);
        GL11.glVertex2d(minX, maxY);
        GL11.glVertex2d(maxX, maxY);
        GL11.glVertex2d(maxX, minY);
        GL11.glEnd();
    }

    private void drawCorners(double minX, double minY, double maxX, double maxY, Color color, float lineWidth) {
        double width = maxX - minX;
        double height = maxY - minY;
        double length = Math.min(width, height) * 0.3;

        GL11.glColor4d(color.getRed() / 255d, color.getGreen() / 255d, color.getBlue() / 255d, color.getAlpha() / 255d);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINES);

        GL11.glVertex2d(minX, minY);
        GL11.glVertex2d(minX + length, minY);
        GL11.glVertex2d(minX, minY);
        GL11.glVertex2d(minX, minY + length);

        GL11.glVertex2d(maxX, minY);
        GL11.glVertex2d(maxX - length, minY);
        GL11.glVertex2d(maxX, minY);
        GL11.glVertex2d(maxX, minY + length);

        GL11.glVertex2d(minX, maxY);
        GL11.glVertex2d(minX + length, maxY);
        GL11.glVertex2d(minX, maxY);
        GL11.glVertex2d(minX, maxY - length);

        GL11.glVertex2d(maxX, maxY);
        GL11.glVertex2d(maxX - length, maxY);
        GL11.glVertex2d(maxX, maxY);
        GL11.glVertex2d(maxX, maxY - length);

        GL11.glEnd();
    }

    private double interpolate(double lastPos, double pos) {
        return lastPos + (pos - lastPos) * mc.timer.renderPartialTicks;
    }

    private void drawString(String text, double x, double y, int color) {
        if (heldItemCustomFont.getValue()) {
            CustomFontRenderer font = FontUtils.getFont("sf", 16);
            font.drawString(text, (float) x, (float) y, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, (float) x, (float) y, color);
        }
    }

    private int getStringWidth(String text) {
        if (heldItemCustomFont.getValue()) {
            return FontUtils.getFont("sf", 16).getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }
}