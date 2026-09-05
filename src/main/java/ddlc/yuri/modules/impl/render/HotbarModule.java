package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

@ModuleInfo(label = "Hotbar", description = "Renders a custom DDLC themed hotbar", category = ModuleCategory.RENDER)
public class HotbarModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.YURI);

    private enum Mode {
        YURI("Yuri"),
        DDLC("DDLC");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color HIGHLIGHT_FILL_COLOR = new Color(255, 255, 255, 45);
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final float SLOT_SIZE = 20f;
    private static final float SLOT_RADIUS = 4f;

    @EventHook(EventPriority.VERY_HIGH)
    public void onRender2D(Render2DEvent event) {
        renderHotbar();
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent event) {
        renderHotbar();
    }

    public void renderHotbar() {
        if (!(mc.getRenderViewEntity() instanceof EntityPlayer)) {
            return;
        }

        final ScaledResolution sr = new ScaledResolution(mc);
        final EntityPlayer entityplayer = (EntityPlayer) mc.getRenderViewEntity();

        final int posX = (int) (sr.getScaledWidth() / 2.0F - 95);
        final int posY = (int) (sr.getScaledHeight() - 21 - 2f - 18);
        final int scaleX = 95 * 2;
        final int scaleY = 22 + 18;

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (mode.getValue() == Mode.YURI) {
            RoundedUtils.drawRoundOutline(posX, posY + 18, scaleX, scaleY - 18, 5, -0.5f, BG_COLOR,
                    ColorManager.getColor());
        } else if (mode.getValue() == Mode.DDLC) {
            RenderUtils.drawImage(new ResourceLocation("yuri/gui/textbox.png"), posX + 1, posY + 18, scaleX, scaleY - 18);
        }

        for (int j = 0; j < 9; ++j) {
            final int k = sr.getScaledWidth() / 2 - 90 + j * 21 - 2;
            final int l = sr.getScaledHeight() - 16 - 3;
            renderSlotHighlight(j, k, l - 1, entityplayer);
        }

        for (int j = 0; j < 9; ++j) {
            final int k = sr.getScaledWidth() / 2 - 90 + j * 21 - 2;
            final int l = sr.getScaledHeight() - 16 - 3;
            renderHotBarItem(j, k, l - 1, mc.timer.renderPartialTicks, entityplayer);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }

    private void renderSlotHighlight(final int index, final int xPos, final int yPos, final EntityPlayer entityPlayer) {
        if (entityPlayer.inventory.currentItem != index) {
            return;
        }

        final float x = xPos - 2f;
        final float y = yPos - 2f;

        RoundedUtils.drawCustomRoundedRect(x, y, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS,
                true, true, true, true, HIGHLIGHT_FILL_COLOR);
        RoundedUtils.drawRoundOutline(x, y, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS, -0.5f,
                TRANSPARENT, ColorManager.getColor());
    }

    private void renderHotBarItem(final int index, final int xPos, final int yPos, final float partialTicks, final EntityPlayer entityPlayer) {
        final ItemStack itemstack = entityPlayer.inventory.mainInventory[index];
        final RenderItem itemRenderer = mc.getRenderItem();

        if (itemstack == null) {
            return;
        }

        final float f = (float) itemstack.animationsToGo - partialTicks;

        if (f > 0.0F) {
            GlStateManager.pushMatrix();
            final float f1 = 1.0F + f / 5.0F;
            GlStateManager.translate((float) (xPos + 8), (float) (yPos + 12), 0.0F);
            GlStateManager.scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
            GlStateManager.translate((float) (-(xPos + 8)), (float) (-(yPos + 12)), 0.0F);
        }

        RenderHelper.enableGUIStandardItemLighting();
        itemRenderer.renderItemAndEffectIntoGUI(itemstack, xPos, yPos);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (f > 0.0F) {
            GlStateManager.popMatrix();
        }

        itemRenderer.renderItemOverlays(mc.fontRendererObj, itemstack, xPos, yPos);
        RenderHelper.disableStandardItemLighting();
    }
}