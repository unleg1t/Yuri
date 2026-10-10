package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.adapters.FontAdapter;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Locale;

public final class PulsiveMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    private static final int WIDTH = 100;
    private static final int HEADER_HEIGHT = 16;
    private static final int BODY_HEIGHT = 36;
    private static final int HEIGHT = HEADER_HEIGHT + BODY_HEIGHT;
    private static final float PADDING_X = 8f;
    private static final float PADDING_TOP = 4f;
    private static final float FACE_SIZE = 22f;
    private static final float GAP_FACE_TEXT = 5f;
    private static final float GAP_NAME_SUB = 1f;
    private static final float GAP_FACE_BAR = 3f;
    private static final float BAR_HEIGHT = 5f;
    private static final float RADIUS = 6f;
    private static final float SEAM_FIX_OFFSET = 1.35f;

    private static final Color HEADER_COLOR = new Color(40, 40, 44, 100);
    private static final Color BODY_COLOR = new Color(18, 18, 20, 150);
    private static final Color BAR_BG_COLOR = new Color(255, 255, 255, 40);
    private static final Color FACE_PLACEHOLDER_COLOR = new Color(255, 255, 255, 25);
    private static final Color TEXT_SECONDARY_COLOR = new Color(220, 220, 220);
    private static final Color SUB_BASE_COLOR = new Color(190, 190, 190);

    private float cachedAlpha = -1f;
    private int cachedBaseRGB;
    private Color cachedHeaderColor;
    private Color cachedBodyColor;
    private Color cachedAccentColor;
    private Color cachedBarBgColor;
    private Color cachedFacePlaceholderColor;
    private Color cachedWhiteColor;
    private Color cachedSecondaryColor;
    private Color cachedSubColor;

    public PulsiveMode(TargetHudModule parentModule) {
        super("Pulsive");
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

        boolean custom = parentModule.useCustomFont.getValue();
        FontAdapter boldFont = FontAdapter.of("sf-bold", 18, custom);
        FontAdapter regularFont = FontAdapter.of("sf", 18, custom);
        FontAdapter bodyFont = FontAdapter.of("sf", 16, custom);
        if (boldFont == null || regularFont == null || bodyFont == null) return;

        float health = targetEntity.isEntityAlive() ? targetEntity.getHealth() : 0f;
        float maxHealth = targetEntity.getMaxHealth();

        if (state.displayHealth < 0f) {
            state.displayHealth = health;
        }
        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);
        float healthPercentage = Math.max(0f, Math.min(1f, state.displayHealth / maxHealth));

        float alpha = state.alpha;
        Color baseColor = ColorManager.getColor();
        int baseRGB = baseColor.getRGB();

        if (alpha != cachedAlpha || baseRGB != cachedBaseRGB) {
            cachedAlpha = alpha;
            cachedBaseRGB = baseRGB;
            cachedHeaderColor = RenderUtils.applyOpacity(HEADER_COLOR, alpha);
            cachedBodyColor = RenderUtils.applyOpacity(BODY_COLOR, alpha);
            cachedAccentColor = RenderUtils.applyOpacity(baseColor, alpha);
            cachedBarBgColor = RenderUtils.applyOpacity(BAR_BG_COLOR, alpha);
            cachedFacePlaceholderColor = RenderUtils.applyOpacity(FACE_PLACEHOLDER_COLOR, alpha);
            cachedWhiteColor = RenderUtils.applyOpacity(Color.WHITE, alpha);
            cachedSecondaryColor = RenderUtils.applyOpacity(TEXT_SECONDARY_COLOR, alpha);
            cachedSubColor = RenderUtils.applyOpacity(SUB_BASE_COLOR, alpha);
        }

        float width = WIDTH;

        float faceX = PADDING_X;
        float faceY = HEADER_HEIGHT + PADDING_TOP;
        float textX = faceX + FACE_SIZE + GAP_FACE_TEXT;
        float textMaxWidth = width - textX - PADDING_X;

        String nameText = targetEntity.getName();
        while (nameText.length() > 1 && boldFont.width(nameText) > textMaxWidth) {
            nameText = nameText.substring(0, nameText.length() - 1);
        }
        String healthText = String.format(Locale.US, "Health: %.1f", state.displayHealth);

        String targetWord = "target";
        String hudWord = "hud";
        float targetWidth = boldFont.width(targetWord) + 1;
        float hudWidth = regularFont.width(hudWord);
        float titleX = width / 2f - (targetWidth + hudWidth) / 2f;
        float titleY = (HEADER_HEIGHT - Math.max(boldFont.height(), regularFont.height())) / 2f;

        RoundedUtils.drawCustomRoundedRect((float) x, (float) y, width, HEADER_HEIGHT, RADIUS,
                true, true, false, false, cachedHeaderColor);
        RoundedUtils.drawCustomRoundedRect((float) x, (float) y + HEADER_HEIGHT + SEAM_FIX_OFFSET, width,
                BODY_HEIGHT + SEAM_FIX_OFFSET, RADIUS, false, false, true, true, cachedBodyColor);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        boldFont.draw(targetWord, titleX, titleY, cachedWhiteColor.getRGB());
        regularFont.draw(hudWord, titleX + targetWidth, titleY, cachedSecondaryColor.getRGB());

        if (targetEntity instanceof AbstractClientPlayer) {
            long elapsed = now - state.hurtAnimStart;
            float hurtProgress = elapsed >= 400 ? 1f : Math.max(0f, elapsed / 400f);
            float tintAmount = hurtProgress < 1f ? (1f - hurtProgress) * 0.7f : 0f;

            boolean stencilWasEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);

            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(false, false, false, false);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

            RoundedUtils.drawRoundedRect(faceX, faceY + 1, FACE_SIZE, FACE_SIZE + 1, 6f, Color.WHITE);

            GL11.glColorMask(true, true, true, true);
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

            parentModule.renderPlayerFace((AbstractClientPlayer) targetEntity, faceX, faceY, FACE_SIZE, 1f, tintAmount, alpha);

            if (!stencilWasEnabled) {
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            }
        } else {
            RoundedUtils.drawCustomRoundedRect(faceX, faceY, FACE_SIZE, FACE_SIZE, 3f,
                    true, true, true, true, cachedFacePlaceholderColor);
        }

        float nameHeight = boldFont.height();
        float subHeight = bodyFont.height();
        float stackHeight = nameHeight + GAP_NAME_SUB + subHeight;
        float textY = faceY + (FACE_SIZE - stackHeight) / 2f;

        boldFont.draw(nameText, textX, textY, cachedWhiteColor.getRGB());
        bodyFont.draw(healthText, textX + 1, textY + nameHeight + GAP_NAME_SUB, cachedSubColor.getRGB());

        float barX = faceX;
        float barY = faceY + FACE_SIZE + GAP_FACE_BAR;
        float barWidth = width - PADDING_X * 2f;

        RoundedUtils.drawCustomRoundedRect(barX, barY, barWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                true, true, true, true, cachedBarBgColor);
        if (healthPercentage > 0f) {
            float progressWidth = Math.min(barWidth, Math.max(BAR_HEIGHT, barWidth * healthPercentage));
            RoundedUtils.drawCustomRoundedRect(barX, barY, progressWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                    true, true, true, true, cachedAccentColor);
        }

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }
}