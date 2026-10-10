package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class OldAstolfoMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    private static final int WIDTH = 127;
    private static final int HEIGHT = 45;
    private static final float TEXT_X = 28f;
    private static final float NAME_Y = 4f;
    private static final float BAR_Y = 14f;
    private static final float BAR_WIDTH = 94f;
    private static final float BAR_HEIGHT = 12f;
    private static final float HEALTH_TEXT_X = 65f;
    private static final float ARMOR_Y = 28f;
    private static final float ARMOR_SPACING = 18f;
    private static final float ARMOR_SCALE = 0.9f;
    private static final int BG_ALPHA = 170;

    public OldAstolfoMode(TargetHudModule parentModule) {
        super("Old Astolfo");
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

        float health = targetEntity.isEntityAlive() ? targetEntity.getHealth() : 0f;
        float maxHealth = targetEntity.getMaxHealth();

        if (state.displayHealth < 0f) {
            state.displayHealth = health;
        }
        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);
        float healthPercentage = Math.max(0f, Math.min(1f, state.displayHealth / maxHealth));

        float alpha = state.alpha;
        int healthRGB = Color.HSBtoRGB(healthPercentage / 3f, 1f, 1f);
        int healthColor = withAlpha(healthRGB, alpha);
        int whiteColor = withAlpha(0xFFFFFF, alpha);
        int bgColor = (int) (BG_ALPHA * alpha) << 24;

        long elapsed = now - state.hurtAnimStart;
        float hurtProgress = elapsed >= 400 ? 1f : Math.max(0f, elapsed / 400f);
        float tintAmount = hurtProgress < 1f ? (1f - hurtProgress) * 0.7f : 0f;

        String nameText = mc.fontRendererObj.trimStringToWidth(targetEntity.getName(), (int) (WIDTH - TEXT_X - 4f));
        String healthText = String.valueOf(Math.round(health / 2f * 100f) / 100.0);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Gui.drawRect(0, 0, WIDTH, HEIGHT, bgColor);

        Gui.drawRect((int) TEXT_X, (int) BAR_Y, (int) (TEXT_X + BAR_WIDTH * healthPercentage),
                (int) (BAR_Y + BAR_HEIGHT), healthColor);

        parentModule.render3DEntity(targetEntity, 14, 41, 18, 1f, tintAmount, alpha);

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        mc.fontRendererObj.drawStringWithShadow(nameText, TEXT_X, NAME_Y, whiteColor);
        mc.fontRendererObj.drawStringWithShadow(healthText, HEALTH_TEXT_X, BAR_Y + 2f, healthColor);

        renderArmor(targetEntity, alpha);

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    private void renderArmor(EntityLivingBase entity, float alpha) {
        if (alpha <= 0.05f) return;

        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().zLevel = 0.0F;

        for (int slot = 0; slot < 4; slot++) {
            ItemStack armor = entity.getCurrentArmor(3 - slot);
            if (armor == null) continue;

            GlStateManager.pushMatrix();
            GlStateManager.translate(TEXT_X + slot * ARMOR_SPACING, ARMOR_Y, 0);
            GlStateManager.scale(ARMOR_SCALE, ARMOR_SCALE, 1f);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

            mc.getRenderItem().renderItemAndEffectIntoGUI(armor, 0, 0);

            GlStateManager.disableAlpha();
            GlStateManager.popMatrix();
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(4, Math.min(255, (int) (255 * alpha)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}