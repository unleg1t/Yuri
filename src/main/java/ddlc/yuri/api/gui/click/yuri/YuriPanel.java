package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class YuriPanel {

    private final ModuleCategory category;
    private final String title;
    protected float posX;
    protected float posY;
    public boolean dragging;
    public boolean opened = true;
    public final List<YuriModuleEntry> modules = new CopyOnWriteArrayList<>();

    private int dragX;
    private int dragY;
    protected float scrollOffset;
    protected float targetScrollOffset;
    public float bodyTop;
    public float bodyBottom;

    public YuriPanel(ModuleCategory category, float posX, float posY) {
        this.category = category;
        this.title = category == null ? "Configs" : category.getName();
        this.posX = posX;
        this.posY = posY;
        if (category != null) {
            for (Module module : Yuri.INSTANCE.getModuleManager().getModulesForCategory(category)) {
                modules.add(new YuriModuleEntry(module, this));
            }
        }
    }

    public void resetAnimations() {
        for (YuriModuleEntry module : modules) {
            module.resetAnimations();
        }
    }

    public String drawScreen(int mouseX, int mouseY, float animationProgress) {
        float offsetY = (1.0F - animationProgress) * -8.0F;
        float drawX = posX;
        float drawY = posY + offsetY;

        float extra = opened ? 4.0F : 0.0F;
        float bodyHeight = opened ? Math.min(getContentHeight(), YuriTheme.MAX_BODY_HEIGHT) + extra : 0.0F;
        float totalHeight = YuriTheme.HEADER_HEIGHT + bodyHeight;
        RoundedUtils.drawRoundedRect(drawX, drawY, YuriTheme.PANEL_WIDTH, totalHeight, YuriTheme.RADIUS,
                YuriTheme.fade(YuriTheme.BG, animationProgress));
        RoundedUtils.drawRoundedRect(drawX + YuriTheme.PADDING_X, drawY + YuriTheme.HEADER_HEIGHT - 3f,
                YuriTheme.PANEL_WIDTH - YuriTheme.PADDING_X * 2f, 1.1f, 0.6f,
                YuriTheme.fade(YuriTheme.accent(), animationProgress));

        FontUtils.getFont("sf-bold", 14).drawStringWithShadow(
                title,
                drawX + YuriTheme.PADDING_X,
                drawY + 6f,
                YuriTheme.text(YuriTheme.TEXT_PRIMARY, animationProgress)
        );

        String tooltip = null;
        if (opened) {
            float maxScroll = Math.max(0.0F, getContentHeight() - YuriTheme.MAX_BODY_HEIGHT);
            targetScrollOffset = Math.max(0.0F, Math.min(maxScroll, targetScrollOffset));
            scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.25F);

            bodyTop = drawY + YuriTheme.HEADER_HEIGHT;
            bodyBottom = bodyTop + Math.min(getContentHeight(), YuriTheme.MAX_BODY_HEIGHT);

            Minecraft mc = Minecraft.getMinecraft();
            for (YuriModuleEntry module : modules) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                ScaleUtils.applyScissor(mc, drawX, bodyTop, drawX + YuriTheme.PANEL_WIDTH, bodyBottom);
                String moduleTooltip = module.drawScreen(mouseX, mouseY, drawX, drawY - scrollOffset, animationProgress);
                if (moduleTooltip != null) {
                    tooltip = moduleTooltip;
                }
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            modules.forEach(module -> module.yPerModule = 0);
        }

        return tooltip;
    }

    public void drawShaderMask(float animationProgress) {
        if (animationProgress < 0.08F) {
            return;
        }
        float offsetY = (1.0F - animationProgress) * -8.0F;
        float extra = opened ? 4.0F : 0.0F;
        float bodyHeight = opened ? Math.min(getContentHeight(), YuriTheme.MAX_BODY_HEIGHT) + extra : 0.0F;
        float totalHeight = YuriTheme.HEADER_HEIGHT + bodyHeight;
        RoundedUtils.drawRoundedRect(posX, posY + offsetY, YuriTheme.PANEL_WIDTH, totalHeight, YuriTheme.RADIUS, Color.WHITE);
    }

    public void startDragging(int mouseX, int mouseY) {
        dragging = true;
        dragX = mouseX - (int) posX;
        dragY = mouseY - (int) posY;
    }

    public void updateDrag(int mouseX, int mouseY) {
        if (dragging) {
            posX = mouseX - dragX;
            posY = mouseY - dragY;
        }
    }

    public float getContentHeight() {
        float height = 0;
        for (YuriModuleEntry module : modules) {
            height += module.yPerModule;
        }
        return height;
    }

    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= posX && mouseY >= posY && mouseX <= posX + YuriTheme.PANEL_WIDTH && mouseY <= posY + YuriTheme.HEADER_HEIGHT;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        float extra = opened ? 4.0F : 0.0F;
        float bodyHeight = opened ? Math.min(getContentHeight(), YuriTheme.MAX_BODY_HEIGHT) + extra : 0.0F;
        return mouseX >= posX && mouseX <= posX + YuriTheme.PANEL_WIDTH && mouseY >= posY
                && mouseY <= posY + YuriTheme.HEADER_HEIGHT + bodyHeight;
    }

    public void scroll(float amount) {
        if (!opened) {
            return;
        }
        if (getContentHeight() > YuriTheme.MAX_BODY_HEIGHT) {
            float maxScroll = getContentHeight() - YuriTheme.MAX_BODY_HEIGHT;
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
        if (isHeaderHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
            if (opened) {
                for (YuriModuleEntry module : modules) {
                    module.fraction = 0;
                }
            }
        }

        if (opened) {
            for (YuriModuleEntry module : modules) {
                module.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public boolean isTyping() {
        for (YuriModuleEntry module : modules) {
            if (module.isTyping()) {
                return true;
            }
        }
        return false;
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
