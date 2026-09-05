package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;

import java.awt.*;

public final class ExhibitionMode extends TargetHudMode {

    private final TargetHudModule parentModule;
    private final CustomFontRenderer nameFont;
    private final CustomFontRenderer infoFont;

    private static final Color DARKEST_BASE = new Color(10, 10, 10);
    private static final Color SECOND_DARKEST_BASE = new Color(22, 22, 22);
    private static final Color LIGHTEST_BASE = new Color(44, 44, 44);
    private static final Color MIDDLE_BASE = new Color(34, 34, 34);

    private float cachedAlpha = -1f;
    private Color cachedDarkest;
    private Color cachedSecondDarkest;
    private Color cachedLightest;
    private Color cachedMiddleColor;
    private Color cachedTextColor;

    public ExhibitionMode(TargetHudModule parentModule) {
        super("Exhibition");
        this.parentModule = parentModule;
        this.nameFont = FontUtils.getFont("tahoma", 16);
        this.infoFont = FontUtils.getFont("tahoma", 12);
    }

    @Override
    public int getMinWidth() { return 130; }

    @Override
    public int getHudHeight() { return 36; }

    @Override
    public int getLabelHeight() { return 0; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        float alpha = state.alpha;
        int width = Math.max(getMinWidth(), nameFont.getStringWidth(targetEntity.getName()) + 60);
        int height = getHudHeight();
        float size = height - 6;
        float scale = size / 40f;

        if (alpha != cachedAlpha) {
            cachedAlpha = alpha;
            cachedDarkest = RenderUtils.applyOpacity(DARKEST_BASE, alpha);
            cachedSecondDarkest = RenderUtils.applyOpacity(SECOND_DARKEST_BASE, alpha);
            cachedLightest = RenderUtils.applyOpacity(LIGHTEST_BASE, alpha);
            cachedMiddleColor = RenderUtils.applyOpacity(MIDDLE_BASE, alpha);
            cachedTextColor = RenderUtils.applyOpacity(Color.WHITE, alpha);
        }

        Color darkest = cachedDarkest;
        Color secondDarkest = cachedSecondDarkest;
        Color lightest = cachedLightest;
        Color middleColor = cachedMiddleColor;
        Color textColor = cachedTextColor;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableBlend();

        Gui.drawRect(-4, -4, width + 4, height + 4, darkest.getRGB());
        Gui.drawRect(-3, -3, width + 3, height + 3, middleColor.getRGB());
        Gui.drawRect(-1, -1, width + 1, height + 1, lightest.getRGB());
        Gui.drawRect(0, 0, width, height, secondDarkest.getRGB());

        Gui.drawRect(3, 3, 4, (int) (3 + size), lightest.getRGB());
        Gui.drawRect(3, (int) (3 + size), (int) (3 + size), (int) (4 + size), lightest.getRGB());
        Gui.drawRect((int) (3 + size), 3, (int) (4 + size), (int) (4 + size), lightest.getRGB());
        Gui.drawRect(3, 3, (int) (3 + size), 4, lightest.getRGB());

        float nameY = 6 * scale;
        nameFont.drawString(targetEntity.getName(), 8 + size, nameY, textColor.getRGB());

        float healthValue = (targetEntity.getHealth() + targetEntity.getAbsorptionAmount())
                / (targetEntity.getMaxHealth() + targetEntity.getAbsorptionAmount());

        Color healthColor = healthValue > 0.5f
                ? RenderUtils.interpolateColorC(new Color(255, 255, 10), new Color(10, 255, 10), (healthValue - 0.5f) / 0.5f)
                : RenderUtils.interpolateColorC(new Color(255, 10, 10), new Color(255, 255, 10), healthValue * 2);
        healthColor = RenderUtils.applyOpacity(healthColor, alpha);

        float healthBarTop = 18 * scale;
        float healthBarBottom = healthBarTop + (5 * scale);
        float healthBarInnerBottom = healthBarBottom - scale;
        float healthBarWidth = width - (size + 12);

        Gui.drawRect((int) (8 + size), (int) healthBarTop, (int) (8 + size + healthBarWidth), (int) healthBarBottom, darkest.getRGB());
        Gui.drawRect((int) (8 + size + 0.5f), (int) healthBarTop, (int) (8 + size + healthBarWidth - 0.5f), (int) healthBarInnerBottom,
                RenderUtils.interpolateColorC(darkest, healthColor, 0.2f).getRGB());

        float healthBarActualWidth = healthBarWidth - 1;
        Gui.drawRect((int) (8 + size + 0.5f), (int) healthBarTop, (int) (8 + size + 0.5f + healthBarActualWidth * healthValue), (int) healthBarInnerBottom, healthColor.getRGB());

        float increment = healthBarActualWidth / 11;
        for (int i = 1; i < 11; i++) {
            Gui.drawRect((int) (8 + size + increment * i), (int) healthBarTop, (int) (8 + size + increment * i + 1), (int) healthBarInnerBottom, darkest.getRGB());
        }

        float infoTextY = 25 * scale;
        infoFont.drawString(
                "HP: " + Math.round(targetEntity.getHealth() + targetEntity.getAbsorptionAmount())
                        + " | Dist: " + Math.round(mc.thePlayer.getDistanceToEntity(targetEntity)),
                8 + size, infoTextY, textColor.getRGB());

        float separation = healthBarWidth / 5;

        long elapsed = now - state.hurtAnimStart;
        float hurtProgress = elapsed >= 400 ? 1f : Math.max(0f, elapsed / 400f);
        float tintAmount = hurtProgress < 1f ? (1f - hurtProgress) * 0.7f : 0f;

        parentModule.render3DEntity(targetEntity, (int) (3 + size / 2f), (int) (size + 1), (int) (18 * scale), 1.0f, tintAmount, alpha);

        float iconScale = Math.min(1f, scale);
        float iconSize = 16 * iconScale;
        float iconY = height - iconSize - 1;

        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i <= 3; i++) {
            if (targetEntity.getCurrentArmor(i) == null) continue;
            drawScaledItem(targetEntity.getCurrentArmor(i), size + 7 + (separation * (3 - i)), iconY, iconScale, alpha);
        }

        if (targetEntity.getHeldItem() != null) {
            drawScaledItem(targetEntity.getHeldItem(), size + 7 + (separation * 4), iconY, iconScale, alpha);
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.resetColor();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawScaledItem(net.minecraft.item.ItemStack item, float x, float y, float scale, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.color(1f, 1f, 1f, alpha);
        mc.getRenderItem().renderItemAndEffectIntoGUI(item, 0, 0);
        GlStateManager.popMatrix();
    }
}