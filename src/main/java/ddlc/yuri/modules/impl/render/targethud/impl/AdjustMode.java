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

public final class AdjustMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    private static final int WIDTH = 110;
    private static final int HEIGHT = 31;
    private static final float FACE_X = 2.5f;
    private static final float FACE_Y = 2.5f;
    private static final float FACE_SIZE = 20f;
    private static final float TEXT_X = 24f;
    private static final float EDGE_PADDING = 3f;
    private static final float DIFF_Y = 18.5f;
    private static final float TRACK_X = 2f;
    private static final float TRACK_HEIGHT = 4f;
    private static final float TRACK_Y = HEIGHT - TRACK_HEIGHT - 2f;
    private static final int EQUIPMENT_Y = 12;

    public AdjustMode(TargetHudModule parentModule) {
        super("Adjust");
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
        FontAdapter nameFont = FontAdapter.of("sf-bold", 16, custom);
        FontAdapter diffFont = FontAdapter.of("sf", 14, custom);
        if (nameFont == null || diffFont == null) return;

        TargetHudHelper.update(state, targetEntity, now, delta);

        float progress = TargetHudHelper.hurtProgress(state, now);
        float tintAmount = TargetHudHelper.tintAmount(progress);

        float actualHealth = Math.max(0f, targetEntity.getHealth());
        float maxHealth = Math.max(1f, targetEntity.getMaxHealth());
        float healthPercentage = TargetHudHelper.clamp01(state.displayHealth / maxHealth);
        float staggerPercentage = Math.max(healthPercentage, TargetHudHelper.clamp01(state.previousDisplayHealth / maxHealth));

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

        Gui.drawRect(0, 0, WIDTH, HEIGHT, new Color(15, 15, 15, (int) (160f * alpha)).getRGB());

        if (targetEntity instanceof AbstractClientPlayer) {
            parentModule.renderPlayerFace((AbstractClientPlayer) targetEntity, FACE_X, FACE_Y, FACE_SIZE,
                    1f, tintAmount, alpha);
        } else {
            parentModule.render3DEntity(targetEntity, 12, 22, 10, 1f, tintAmount, alpha);
        }

        float trackWidth = WIDTH - 4f;

        TargetHudHelper.fillRect(TRACK_X, TRACK_Y, trackWidth, TRACK_HEIGHT,
                new Color(30, 30, 32, (int) (140f * alpha)));
        TargetHudHelper.fillRect(TRACK_X + 0.5f, TRACK_Y + 0.5f, trackWidth - 1f, TRACK_HEIGHT - 1f,
                new Color(30, 30, 32, (int) (255f * alpha)));

        float fillX = TRACK_X + 0.5f;
        float fillY = TRACK_Y + 0.5f;
        float fillHeight = TRACK_HEIGHT - 1f;

        float staggerWidth = Math.max(0f, trackWidth * staggerPercentage - 1f);
        Color staggerLeft = TargetHudHelper.withAlpha(first.darker(), alpha);
        Color staggerRight = TargetHudHelper.withAlpha(TargetHudHelper.lerp(first.darker(), second.darker(), staggerPercentage), alpha);
        TargetHudHelper.drawHorizontalGradient(fillX, fillY, staggerWidth, fillHeight, staggerLeft, staggerRight);

        float healthWidth = Math.max(0f, trackWidth * healthPercentage - 1f);
        Color healthLeft = TargetHudHelper.withAlpha(first, alpha);
        Color healthRight = TargetHudHelper.withAlpha(TargetHudHelper.lerp(first, second, healthPercentage), alpha);
        TargetHudHelper.drawHorizontalGradient(fillX, fillY, healthWidth, fillHeight, healthLeft, healthRight);

        nameFont.draw(nameText, TEXT_X, nameFont.height() / 2f, new Color(255, 255, 255, (int) (255f * alpha)).getRGB());

        if (healthDiff != 0f) {
            int diffColor = new Color(255, 255, 255, (int) (160f * alpha)).getRGB();
            diffFont.draw(diffText, WIDTH - EDGE_PADDING - diffFont.width(diffText), DIFF_Y, diffColor);
        }

        parentModule.renderTargetEquipment(targetEntity, (int) TEXT_X, EQUIPMENT_Y, alpha);

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }
}