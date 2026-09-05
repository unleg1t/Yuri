package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;

import java.awt.*;

public final class OldNovolineMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    public OldNovolineMode(TargetHudModule parentModule) {
        super("Old Novoline");
        this.parentModule = parentModule;
    }

    @Override
    public int getMinWidth() { return 90; }

    @Override
    public int getHudHeight() { return 44; }

    @Override
    public int getLabelHeight() { return 0; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        float health = targetEntity.isEntityAlive() ? targetEntity.getHealth() : 0f;
        float maxHealth = targetEntity.getMaxHealth();

        if (state.displayHealth < 0f) state.displayHealth = health;
        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);
        float healthPercentage = Math.max(0f, Math.min(1f, state.displayHealth / maxHealth));

        float currentAlpha = state.alpha;
        int borderAlpha = (int) (100 * currentAlpha);
        int textAlpha = (int) (255 * currentAlpha);

        Color baseColor = ColorManager.getColor();
        Color fadedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (baseColor.getAlpha() * currentAlpha));

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);

        int headSize = 24;
        int totalWidth = 2 + headSize + mc.fontRendererObj.getStringWidth(targetEntity.getName()) + 15;
        int totalHeight = 2 + headSize + 2;
        int barHeight = 2;

        Color backgroundColor = new Color(0, 0, 0, borderAlpha);
        Gui.drawRect(0, 0, totalWidth, totalHeight, backgroundColor.getRGB());

        long elapsed = now - state.hurtAnimStart;
        float hurtProgress = elapsed >= 400 ? 1f : Math.max(0f, elapsed / 400f);
        float tintAmount = hurtProgress < 1f ? (1f - hurtProgress) * 0.7f : 0f;

        parentModule.render3DEntity(targetEntity, 12, getLabelHeight() + 27, 14, 1.0f, tintAmount, currentAlpha);

        int whiteWithAlpha = (textAlpha << 24) | 0xFFFFFF;
        mc.fontRendererObj.drawString(targetEntity.getName(), 2 + headSize + 2, 4, whiteWithAlpha, true);

        // Armor row
        int armorStartX = headSize;
        int armorY = totalHeight - 2 - barHeight - 13;
        renderArmor(targetEntity, armorStartX, armorY, currentAlpha);

        // Health bar at the very bottom
        int filledWidth = (int) (totalWidth * healthPercentage);
        Gui.drawRect(0, totalHeight - barHeight, totalWidth, totalHeight, new Color(20, 20, 20, borderAlpha).getRGB());
        Gui.drawRect(0, totalHeight - barHeight, filledWidth, totalHeight, fadedColor.getRGB());

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    private void renderArmor(EntityLivingBase entity, int startX, int y, float alpha) {
        net.minecraft.item.ItemStack[] armor = new net.minecraft.item.ItemStack[]{
                entity.getEquipmentInSlot(4), // helmet
                entity.getEquipmentInSlot(3), // chestplate
                entity.getEquipmentInSlot(2), // leggings
                entity.getEquipmentInSlot(1), // boots
        };

        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableRescaleNormal();

        int slotX = startX;
        for (net.minecraft.item.ItemStack stack : armor) {
            if (stack != null) {
                mc.getRenderItem().renderItemIntoGUI(stack, slotX, y);
                slotX += 12;
            } else {
                slotX += 12;
            }
        }

        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }
}
