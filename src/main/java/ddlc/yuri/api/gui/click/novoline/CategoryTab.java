package ddlc.yuri.api.gui.click.novoline;

import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CategoryTab {

    private static final float MAX_TAB_HEIGHT = 260.0F;

    private final ModuleCategory category;
    private float posX;
    private float posY;
    public boolean dragging;
    public boolean opened = true;
    public final List<ModuleEntry> modules = new CopyOnWriteArrayList<>();

    private int dragX;
    private int dragY;
    private float lastMouseX;
    private float lastMouseY;
    private float angle;
    private float angularVelocity;
    private float scrollOffset;
    private float targetScrollOffset;
    public float bodyTop;
    public float bodyBottom;

    public CategoryTab(ModuleCategory category, float posX, float posY) {
        this.category = category;
        this.posX = posX;
        this.posY = posY;
        if (category != null) {
            for (Module module : Yuri.INSTANCE.getModuleManager().getModulesForCategory(category)) {
                modules.add(new ModuleEntry(module, this));
            }
        }
    }

    public String drawScreen(int mouseX, int mouseY, float animationProgress) {
        float offsetY = (1.0F - animationProgress) * -18.0F;
        float drawX = posX;
        float drawY = posY + offsetY;
        int headerAlpha = (int) (255 * animationProgress);

        float pivotX = drawX + 50;
        float pivotY = drawY + 7;

        GlStateManager.pushMatrix();
        GlStateManager.translate(pivotX, pivotY, 0.0F);
        GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(-pivotX, -pivotY, 0.0F);

        String l = "";
        if (category.name().equalsIgnoreCase("Combat")) {
            l = "D";
        } else if (category.name().equalsIgnoreCase("Movement")) {
            l = "A";
        } else if (category.name().equalsIgnoreCase("Player")) {
            l = "B";
        } else if (category.name().equalsIgnoreCase("Render")) {
            l = "C";
        } else if (category.name().equalsIgnoreCase("Misc")) {
            l = "F";
        }

        Gui.drawRect(drawX - 1, drawY, drawX + 101, drawY + 15,
                RenderUtils.withAlpha(GuiTheme.PANEL, headerAlpha));

        FontUtils.getFont("icons", 24).drawString(l, posX + 88, posY + 5, 0xffffffff);

        FontUtils.getFont("sf", 21).drawStringWithShadow(
                category.getName(),
                drawX + 4,
                drawY + 4,
                new Color(255, 255, 255, headerAlpha).getRGB()
        );

        String tooltip = null;
        if (opened) {
            float maxScroll = Math.max(0.0F, getTabHeight() - MAX_TAB_HEIGHT);
            targetScrollOffset = Math.max(0.0F, Math.min(maxScroll, targetScrollOffset));
            scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.25F);

            int bodyHeight = (int) Math.min(getTabHeight(), MAX_TAB_HEIGHT);
            float bodyTop = drawY + 15;
            float bodyBottom = bodyTop + bodyHeight;
            this.bodyTop = bodyTop;
            this.bodyBottom = bodyBottom;

            Gui.drawRect(drawX - 1, bodyTop, drawX + 101, bodyBottom + 1,
                    RenderUtils.withAlpha(GuiTheme.PANEL, headerAlpha));

            Minecraft mc = Minecraft.getMinecraft();

            for (ModuleEntry module : modules) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                ScaleUtils.applyScissor(mc, drawX - 1, bodyTop, drawX + 101, bodyBottom);
                String moduleTooltip = module.drawScreen(mouseX, mouseY, drawX, drawY - scrollOffset, animationProgress);
                if (moduleTooltip != null) {
                    tooltip = moduleTooltip;
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            modules.forEach(module -> module.yPerModule = 0);
        }

        GlStateManager.popMatrix();
        return tooltip;
    }

    public void startDragging(int mouseX, int mouseY) {
        dragging = true;
        dragX = mouseX - (int) posX;
        dragY = mouseY - (int) posY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void updatePhysics(int mouseX, int mouseY) {
        if (dragging) {
            posX = mouseX - dragX;
            posY = mouseY - dragY;
            float mouseDX = mouseX - lastMouseX;
            float targetAngle = Math.max(-14f, Math.min(14f, mouseDX * 1.25f));
            angle = MathUtils.lerp(angle, targetAngle, 0.25f);
        } else {
            float springConstant = 0.18f;
            float damping = 0.85f;
            float displacement = 0f - angle;
            angularVelocity += displacement * springConstant;
            angularVelocity *= damping;
            angle += angularVelocity;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public int getTabHeight() {
        int height = 0;
        for (ModuleEntry module : modules) {
            height += module.yPerModule;
        }
        return height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= posX && mouseY >= posY && mouseX <= posX + 101 && mouseY <= posY + 15;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        float bodyHeight = opened ? Math.min(getTabHeight(), MAX_TAB_HEIGHT) : 0.0F;
        return mouseX >= posX && mouseX <= posX + 101 && mouseY >= posY
                && mouseY <= posY + 15 + bodyHeight;
    }

    public void scroll(float amount) {
        if (!opened) {
            return;
        }
        if (getTabHeight() > MAX_TAB_HEIGHT) {
            float maxScroll = getTabHeight() - MAX_TAB_HEIGHT;
            targetScrollOffset = Math.max(0.0F, Math.min(maxScroll, targetScrollOffset + amount));
        }
    }

    public float getCurrentScrollOffset() {
        return scrollOffset;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            modules.forEach(module -> module.mouseReleased(mouseX, mouseY, state));
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        modules.forEach(module -> module.keyTyped(typedChar, keyCode));
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
            if (opened) {
                for (ModuleEntry module : modules) {
                    module.fraction = 0;
                }
            }
        }

        if (opened) {
            for (ModuleEntry module : modules) {
                module.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getPosY() {
        return posY;
    }

    public float getPosX() {
        return posX;
    }

    public ModuleCategory getCategory() {
        return category;
    }
}