package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class AstolfoMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    private float cachedAlpha = -1f;
    private int cachedBaseRGB;
    private Color cachedBgRectColor;
    private Color cachedBorderRectColor;
    private Color cachedFadedColor;

    public AstolfoMode(TargetHudModule parentModule) {
        super("Astolfo");
        this.parentModule = parentModule;
    }

    @Override
    public int getMinWidth() { return 125; }

    @Override
    public int getHudHeight() { return 36; }

    @Override
    public int getLabelHeight() { return 0; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        float health = targetEntity.isEntityAlive() ? targetEntity.getHealth() : 0f;
        float maxHealth = targetEntity.getMaxHealth();

        if (state.displayHealth < 0f) {
            state.displayHealth = health;
        }
        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);
        float healthPercentage = Math.max(0f, Math.min(1f, state.displayHealth / maxHealth));

        float currentAlpha = state.alpha;
        Color baseColor = ColorManager.getColor();
        int baseRGB = baseColor.getRGB();

        if (currentAlpha != cachedAlpha || baseRGB != cachedBaseRGB) {
            cachedAlpha = currentAlpha;
            cachedBaseRGB = baseRGB;
            int bgAlpha = (int) (150 * currentAlpha);
            int borderAlpha = (int) (255 * currentAlpha);
            cachedBgRectColor = new Color(0, 0, 0, bgAlpha);
            cachedBorderRectColor = new Color(0, 0, 0, borderAlpha);
            cachedFadedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (baseColor.getAlpha() * currentAlpha));
        }

        Color fadedColor = cachedFadedColor;
        int textAlpha = (int) (255 * currentAlpha);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Gui.drawRect(0, 0, 125, 36, cachedBgRectColor.getRGB());

        Gui.drawRect(37, 26, 89, 32, cachedBorderRectColor.getRGB());
        int healthWidth = (int) (52 * healthPercentage);
        Gui.drawRect(37, 26, 37 + healthWidth, 32, fadedColor.getRGB());

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, currentAlpha);
        try {
            GuiInventory.drawEntityOnScreen(15, 32, 16, -targetEntity.rotationYaw, targetEntity.rotationPitch, targetEntity);
        } catch (Exception ignored) {}
        GlStateManager.popMatrix();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();

        int nameColorRGBA = ((textAlpha & 0xFF) << 24) | 0x00FFFFFF;
        mc.fontRendererObj.drawString(targetEntity.getName(), 38, 2, nameColorRGBA, true);

        GlStateManager.pushMatrix();
        GlStateManager.translate(38, 10, 0);
        GlStateManager.scale(1.5f, 1.5f, 1.5f);

        String healthText = (String.format(java.util.Locale.US, "%.1f", health) + "\u2764").replace(".0", "");
        mc.fontRendererObj.drawStringWithShadow(healthText, 0, 1, fadedColor.getRGB());
        GlStateManager.popMatrix();

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }
}