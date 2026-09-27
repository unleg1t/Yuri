package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.ESPUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.text.DecimalFormat;

@ModuleInfo(label = "Name Tags", description = "Renders name tags through walls", category = ModuleCategory.RENDER)
public class NameTagsModule extends Module {

    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("0.0");

    public final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    public final Property<Boolean> background = new Property<>("Background", true);
    public final Property<Boolean> customFont = new Property<>("Custom Font", false);
    public final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.5, 2.0, 0.1);

    private static final String CLIENT_NAME = "Yuri";
    private static final String CLIENT_TAG = System.getProperty("user.name", "unlegit");
    private static final int FONT_SIZE = 16;
    private static final int BADGE_PADDING = 4;
    private static final int BADGE_GAP = 4;
    private static final int BG_PADDING = 4;

    @EventHook
    public void onRender2D(Render2DEvent event) {
        ScaledResolution sr = new ScaledResolution(mc);
        Color bgColor = new Color(0, 0, 0, 130);
        float ticks = mc.timer.renderPartialTicks;
        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) {
                continue;
            }

            if (!entity.equals(mc.thePlayer) || (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue())) {
                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * ticks - viewerX;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * ticks - viewerY;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * ticks - viewerZ;
                renderNameTag((EntityPlayer) entity, x, y + entity.height + 0.2D, z, sr, bgColor);
            }
        }
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        ScaledResolution sr = new ScaledResolution(mc);
        Color bgColor = new Color(0, 0, 0, 130);
        float ticks = mc.timer.renderPartialTicks;
        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) {
                continue;
            }

            if (!entity.equals(mc.thePlayer) || (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue())) {
                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * ticks - viewerX;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * ticks - viewerY;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * ticks - viewerZ;
                renderNameTag((EntityPlayer) entity, x, y + entity.height + 0.2D, z, sr, bgColor);
            }
        }
    }

    private void renderNameTag(EntityPlayer player, double x, double y, double z, ScaledResolution sr, Color bgColor) {

        ESPUtils.windPos.clear();
        if (!GLU.gluProject((float) x, (float) y, (float) z,
                ActiveRenderInfo.MODELVIEW,
                ActiveRenderInfo.PROJECTION,
                ActiveRenderInfo.VIEWPORT,
                ESPUtils.windPos)) {
            return;
        }

        if (ESPUtils.windPos.get(2) > 1) {
            return;
        }

        double screenX = ESPUtils.windPos.get(0) / sr.getScaleFactor();
        double screenY = sr.getScaledHeight() - (ESPUtils.windPos.get(1) / sr.getScaleFactor());

        if (screenX < 0 || screenX > sr.getScaledWidth() || screenY < 0 || screenY > sr.getScaledHeight()) {
            return;
        }

        double distance = mc.thePlayer.getHealth();
        String distanceText = DISTANCE_FORMAT.format(distance);
        String heart = "\u2764";
        String tagText = CLIENT_TAG;
        double s = scale.getValue();

        boolean showClientTag = mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue() && player.equals(mc.thePlayer);

        int rawFontHeight = getFontHeight();
        int rawClientWidth = showClientTag ? getStringWidth(CLIENT_NAME) : 0;
        int rawNameWidth = getStringWidth(player.getName());
        int rawDistanceWidth = getStringWidth(distanceText);
        int rawHeartWidth = mc.fontRendererObj.getStringWidth(heart);
        int rawGap = 4;

        double fontHeight = rawFontHeight * s;
        double clientWidth = rawClientWidth * s;
        double nameWidth = rawNameWidth * s;
        double distanceWidth = rawDistanceWidth * s;
        double heartWidth = rawHeartWidth * s;
        double gap = rawGap * s;
        double badgePadding = BADGE_PADDING * s;
        double badgeGap = BADGE_GAP * s;
        double bgPadding = BG_PADDING * s;

        double mainTextWidth = clientWidth + gap + nameWidth + gap + distanceWidth + gap + heartWidth;
        double badgeWidth = showClientTag ? (getStringWidth(tagText) * s) + badgePadding * 2 : 0;
        double totalWidth = mainTextWidth + badgeGap + badgeWidth;

        double startX = screenX - totalWidth / 2.0;
        double topY = screenY - fontHeight - 4 * s;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();

        if (background.getValue()) {
            GlStateManager.disableTexture2D();
            RoundedUtils.drawRoundOutline(
                    (float) (startX - bgPadding),
                    (float) (topY - bgPadding),
                    (float) (mainTextWidth + bgPadding * 2),
                    (float) (fontHeight + bgPadding * 2),
                    6f, -0.4f, bgColor, ColorManager.getColor());
            GlStateManager.enableTexture2D();
        }

        double cursorX = startX;
        if (showClientTag) {
            drawString(CLIENT_NAME, cursorX, topY, ColorManager.getColor().getRGB(), s);
            cursorX += clientWidth + gap;
        }

        drawString(player.getName(), cursorX, topY, Color.white.getRGB(), s);
        cursorX += nameWidth + gap;

        drawString(distanceText, cursorX, topY, Color.white.getRGB(), s);
        cursorX += distanceWidth + gap;

        drawString(heart, cursorX, customFont.getValue() ? topY + 2.5f * s : topY, Color.red.getRGB(), s);

        if (showClientTag) {
            double badgeX = startX + mainTextWidth + badgeGap;
            drawBadge(tagText, badgeX, topY, fontHeight, badgeWidth, bgColor, s, bgPadding, badgePadding);
        }

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
    }

    private void drawBadge(String text, double x, double y, double fontHeight, double width, Color bgColor, double s, double bgPadding, double badgePadding) {
        double height = fontHeight + bgPadding * 2;

        if (background.getValue()) {
            GlStateManager.disableTexture2D();
            RoundedUtils.drawRoundOutline(
                    (float) x,
                    (float) (y - bgPadding),
                    (float) width,
                    (float) height,
                    6f, -0.4f, bgColor, ColorManager.getColor());
            GlStateManager.enableTexture2D();
        }

        drawString(text, x + badgePadding, y, ColorManager.getColor().getRGB(), s);
    }

    private void drawString(String text, double x, double y, int color, double s) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(customFont.getValue() ? x - 0.5f : x + 0.5f, customFont.getValue() ? y - 0.5f : y + 0.5f, 0);
        GlStateManager.scale(s, s, 1.0);

        if (customFont.getValue()) {
            CustomFontRenderer font = FontUtils.getFont("sf", FONT_SIZE);
            font.drawStringWithShadow(text, 0f, 0f, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, 0f, 0f, color);
        }

        GlStateManager.popMatrix();
    }

    private int getStringWidth(String text) {
        if (customFont.getValue()) {
            return FontUtils.getFont("sf", FONT_SIZE).getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    private int getFontHeight() {
        if (customFont.getValue()) {
            return FontUtils.getFont("sf", FONT_SIZE).getHeight();
        }
        return mc.fontRendererObj.FONT_HEIGHT;
    }
}