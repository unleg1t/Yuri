package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
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
    private static final float MAX_BODY_HEIGHT = 257f;
    private static final float RADIUS = 8f;

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
        float total = 2f;
        for (ModuleRow module : visibleModules()) total += module.getHeight() + 2f;
        return total;
    }

    private static int scaledAlpha(Color base, float safeAlpha) {
        return MathHelper.clamp_int((int) (base.getAlpha() * safeAlpha), 0, 255);
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

        float totalHeight = HEADER_HEIGHT + animatedBodyHeight;

        Color panelBg = RenderUtils.withAlphaColor(Theme.WINDOW_BG, scaledAlpha(Theme.WINDOW_BG, safeAlpha));

        RoundedUtils.drawRoundOutline(x, y, WIDTH, totalHeight, RADIUS, -0.5f,
                panelBg, RenderUtils.withAlphaColor(Theme.accent(), argb));

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

            float scissoredBodyHeight = Math.max(0f, animatedBodyHeight - 3f);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) (x * scale), (int) ((mc.displayHeight - (y + HEADER_HEIGHT + scissoredBodyHeight) * scale)),
                    (int) (WIDTH * scale), (int) (scissoredBodyHeight * scale));

            float rowY = y + HEADER_HEIGHT + 2f - scrollOffset;
            int size = visible.size();
            for (int i = 0; i < size; i++) {
                ModuleRow module = visible.get(i);
                module.setY(rowY);
                boolean isLast = (i == size - 1);
                String moduleTooltip = module.drawScreen(mouseX, mouseY, safeAlpha, isLast);
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