package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.shader.impl.Blur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CategoryWindow {

    public static final float WIDTH = 115f;
    private static final float HEADER_HEIGHT = 22f;
    private static final float MAX_BODY_HEIGHT = 260f;

    private final ModuleCategory category;
    private float x, y;
    private float scrollOffset;
    private float targetScrollOffset;
    private float animatedBodyHeight;
    public boolean opened = true;
    public boolean dragging;
    private float dragOffsetX, dragOffsetY;
    public final List<ModuleRow> modules = new CopyOnWriteArrayList<>();

    public CategoryWindow(ModuleCategory category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module module : Yuri.INSTANCE.getModuleManager().getModulesForCategory(category)) {
            modules.add(new ModuleRow(module, this));
        }
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return WIDTH;
    }

    private boolean matchesSearch(ModuleRow module) {
        String query = YuriClickGUI.searchQuery;
        return query.isEmpty() || module.getModule().getLabel().toLowerCase().contains(query.toLowerCase());
    }

    private List<ModuleRow> visibleModules() {
        List<ModuleRow> result = new CopyOnWriteArrayList<>();
        for (ModuleRow module : modules) {
            if (matchesSearch(module)) result.add(module);
        }
        return result;
    }

    public float getBodyHeight() {
        float total = 4f;
        for (ModuleRow module : visibleModules()) total += module.getHeight() + 2f;
        return total;
    }

    public String drawScreen(int mouseX, int mouseY, float alpha) {
        float safeAlpha = MathHelper.clamp_float(alpha, 0.0f, 1.0f);
        if (safeAlpha < 0.08f) return null;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int argb = MathHelper.clamp_int((int) (255 * safeAlpha), 0, 255);

        float targetHeight = opened ? Math.min(getBodyHeight(), MAX_BODY_HEIGHT) : 0f;
        animatedBodyHeight = MathUtils.lerp(animatedBodyHeight, targetHeight, 0.3f);
        if (Math.abs(animatedBodyHeight - targetHeight) < 0.2f) animatedBodyHeight = targetHeight;

        float totalHeight = HEADER_HEIGHT + animatedBodyHeight + (opened ? 4f : 0f);

        Blur.startBlur();
        RoundedUtils.drawRoundedRect(x, y, WIDTH, totalHeight, 6f, Color.WHITE);
        Blur.endBlur(10f * safeAlpha, 2f, 1f);

        int panelBgAlpha = MathHelper.clamp_int((int) (80 * safeAlpha), 0, 255);
        Color windowBg = new Color(Theme.WINDOW_BG.getRed(), Theme.WINDOW_BG.getGreen(), Theme.WINDOW_BG.getBlue(), panelBgAlpha);

        RoundedUtils.drawRoundOutline(x, y, WIDTH, totalHeight, 6f, -0.5f,
                windowBg, RenderUtils.withAlphaColor(Theme.accent(), argb));

        CustomFontRenderer headerFont = FontUtils.getFont("sf-bold", 16);
        float headerTextY = y + (HEADER_HEIGHT - headerFont.getHeight()) / 2f;
        headerFont.drawCenteredStringWithShadow(category.getName(), x + WIDTH / 2f, headerTextY,
                RenderUtils.withAlpha(Theme.TEXT, argb));

        String tooltip = null;

        if (opened && animatedBodyHeight > 1f) {
            List<ModuleRow> visible = visibleModules();

            float maxScroll = Math.max(0f, getBodyHeight() - MAX_BODY_HEIGHT);
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0f, maxScroll);
            scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.25f);

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) (x * scale), (int) (mc.displayHeight - (y + HEADER_HEIGHT + animatedBodyHeight) * scale),
                    (int) (WIDTH * scale), (int) (animatedBodyHeight * scale));

            float rowY = y + HEADER_HEIGHT + 2f - scrollOffset;
            for (ModuleRow module : visible) {
                module.setY(rowY);
                String moduleTooltip = module.drawScreen(mouseX, mouseY, safeAlpha);
                if (moduleTooltip != null) tooltip = moduleTooltip;
                rowY += module.getHeight() + 2f;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        return tooltip;
    }

    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public void startDragging(int mouseX, int mouseY) {
        dragging = true;
        dragOffsetX = mouseX - x;
        dragOffsetY = mouseY - y;
    }

    public void updateDrag(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (isHeaderHovered(mouseX, mouseY) && button == 1) {
            opened = !opened;
            return;
        }

        float bodyTop = y + HEADER_HEIGHT;
        float bodyBottom = y + HEADER_HEIGHT + animatedBodyHeight;
        boolean insideBody = mouseX >= x && mouseX <= x + WIDTH && mouseY >= bodyTop && mouseY <= bodyBottom;

        if (opened && insideBody && animatedBodyHeight > 1f) {
            for (ModuleRow module : visibleModules()) {
                module.mouseClicked(mouseX, mouseY, button, bodyTop, bodyBottom);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
        if (opened) {
            for (ModuleRow module : modules) module.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            for (ModuleRow module : modules) module.keyTyped(typedChar, keyCode);
        }
    }

    public boolean isAnyTextFieldHovered() {
        for (ModuleRow module : modules) {
            if (module.isAnyTextFieldHovered()) return true;
        }
        return false;
    }

    public void scroll(float amount) {
        if (getBodyHeight() > MAX_BODY_HEIGHT) {
            float maxScroll = getBodyHeight() - MAX_BODY_HEIGHT;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + amount, 0f, maxScroll);
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y
                && mouseY <= y + HEADER_HEIGHT + (opened ? animatedBodyHeight : 0f);
    }
}