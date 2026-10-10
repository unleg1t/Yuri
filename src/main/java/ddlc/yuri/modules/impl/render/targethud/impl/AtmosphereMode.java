package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudHelper;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import ddlc.yuri.utils.render.adapters.FontAdapter;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Locale;

public final class AtmosphereMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    private static final int WIDTH = 135;
    private static final int HEIGHT = 31;
    private static final float FACE_X = 2f;
    private static final float FACE_Y = 2f;
    private static final float FACE_SIZE = 27f;
    private static final float TEXT_X = 32f;
    private static final float NAME_Y = 2f;
    private static final float DIFF_Y = 15f;
    private static final float EDGE_PADDING = 3f;
    private static final float BAR_Y = 25f;
    private static final float BAR_HEIGHT = 4f;
    private static final int EQUIPMENT_Y = 11;

    public AtmosphereMode(TargetHudModule parentModule) {
        super("Atmosphere");
        this.parentModule = parentModule;
    }

    @Override
    public int getMinWidth() { return WIDTH; }

    @Override
    public int getHudHeight() { return HEIGHT; }

    @Override
    public int getLabelHeight() { return 0; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        float alpha = state.alpha;
        if (alpha <= 0.02f) return;

        boolean custom = parentModule.useCustomFont.getValue();
        FontAdapter nameFont = FontAdapter.of("sf-bold", 18, custom);
        FontAdapter diffFont = FontAdapter.of("sf", 14, custom);
        if (nameFont == null || diffFont == null) return;

        TargetHudHelper.update(state, targetEntity, now, delta);

        float progress = TargetHudHelper.hurtProgress(state, now);
        float tintAmount = TargetHudHelper.tintAmount(progress);
        float pushAmount = TargetHudHelper.pushAmount(progress);

        float actualHealth = Math.max(0f, targetEntity.getHealth());
        float maxHealth = Math.max(1f, targetEntity.getMaxHealth());
        float healthPercentage = TargetHudHelper.clamp01(state.displayHealth / maxHealth);
        float staggerPercentage = TargetHudHelper.clamp01(state.previousDisplayHealth / maxHealth);

        Color first = ColorManager.getColors().getFirst();
        Color second = ColorManager.getColors().getSecond();

        String nameText = TargetHudHelper.trim(nameFont, targetEntity.getName(), WIDTH - TEXT_X - EDGE_PADDING);
        float healthDiff = mc.thePlayer.getHealth() - actualHealth;
        String diffText = String.format(Locale.US, "%+.1f", healthDiff);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        Gui.drawRect(0, 0, WIDTH, HEIGHT, new Color(35, 35, 35, (int) (170f * alpha)).getRGB());

        if (targetEntity instanceof AbstractClientPlayer) {
            parentModule.renderPlayerFace((AbstractClientPlayer) targetEntity, FACE_X, FACE_Y, FACE_SIZE,
                    1f - pushAmount, tintAmount, alpha);
        } else {
            parentModule.render3DEntity(targetEntity, 18, 29, 14, 1f - pushAmount, tintAmount, alpha);
        }

        float barX = TEXT_X;
        float barWidth = WIDTH - EDGE_PADDING - barX;

        TargetHudHelper.fillRect(barX, BAR_Y, barWidth, BAR_HEIGHT, new Color(0, 0, 0, (int) (180f * alpha)));

        float staggerWidth = barWidth * Math.max(healthPercentage, staggerPercentage);
        TargetHudHelper.drawHorizontalGradient(barX, BAR_Y, staggerWidth, BAR_HEIGHT,
                TargetHudHelper.withAlpha(first.darker(), alpha),
                TargetHudHelper.withAlpha(second.darker(), alpha));

        float healthWidth = barWidth * healthPercentage;
        TargetHudHelper.drawHorizontalGradient(barX, BAR_Y, healthWidth, BAR_HEIGHT,
                TargetHudHelper.withAlpha(first, alpha),
                TargetHudHelper.withAlpha(second, alpha));

        int flashAlpha = (int) (tintAmount * 90f * alpha);
        if (flashAlpha > 0) {
            TargetHudHelper.fillRect(barX, BAR_Y, healthWidth, BAR_HEIGHT, new Color(255, 60, 60, flashAlpha));
        }

        nameFont.draw(nameText, TEXT_X, NAME_Y, new Color(255, 255, 255, (int) (255f * alpha)).getRGB());

        int diffColor = new Color(255, 255, 255, (int) (160f * alpha)).getRGB();
        diffFont.draw(diffText, WIDTH - EDGE_PADDING - diffFont.width(diffText), DIFF_Y, diffColor);

        parentModule.renderTargetEquipment(targetEntity, (int) TEXT_X, EQUIPMENT_Y, alpha);

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }
}