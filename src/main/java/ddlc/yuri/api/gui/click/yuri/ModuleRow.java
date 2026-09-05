package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ModuleRow {

    private static final IntBuffer SCISSOR_BUFFER = BufferUtils.createIntBuffer(16);

    private final Module module;
    private final CategoryWindow window;
    public final List<PropertyRow> settings = new CopyOnWriteArrayList<>();
    public boolean opened;
    private float y;
    private float height = 15f;
    private float fraction;
    private float hoverFraction;

    public ModuleRow(Module module, CategoryWindow window) {
        this.module = module;
        this.window = window;
        for (Property<?> property : module.getElements()) {
            settings.add(new PropertyRow(property, this));
        }
    }

    public Module getModule() {
        return module;
    }

    public float getX() {
        return window.getX() + 3f;
    }

    public float getWidth() {
        return window.getWidth() - 6f;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getHeight() {
        return height;
    }

    public float getTargetHeight() {
        if (!opened) return 15f;
        float h = 17f;
        for (PropertyRow row : visibleRows()) h += row.getHeight() + 2f;
        return h;
    }

    private void updateHeight() {
        float target = getTargetHeight();
        height = MathUtils.lerp(height, target, 0.35f);
        if (Math.abs(height - target) < 0.1f) height = target;
    }

    private List<PropertyRow> visibleRows() {
        return settings.stream().filter(row -> row.property.isAvailable()).collect(Collectors.toList());
    }

    public String drawScreen(int mouseX, int mouseY, float alpha) {
        float safeAlpha = MathHelper.clamp_float(alpha, 0.0f, 1.0f);
        if (safeAlpha < 0.08f) return null;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int debugFPS = Math.max(Minecraft.getMinecraft().getDebugFPS(), 1);
        float speed = 0.0025f * (2000f / debugFPS);

        fraction = MathHelper.clamp_float(fraction + (module.isEnabled() ? speed : -speed), 0f, 1f);
        boolean hovered = isHeaderHovered(mouseX, mouseY);
        hoverFraction = MathHelper.clamp_float(hoverFraction + (hovered ? speed : -speed), 0f, 1f);

        updateHeight();

        int argb = MathHelper.clamp_int((int) (255 * safeAlpha), 0, 255);
        Color baseBg = RenderUtils.interpolateColorC(Theme.MODULE_BG, Theme.MODULE_HOVER, hoverFraction);
        Color rowBg = RenderUtils.interpolateColorC(baseBg, Theme.MODULE_ACTIVE, fraction);

        int bgAlpha = MathHelper.clamp_int((int) (rowBg.getAlpha() * safeAlpha), 0, 255);
        Color finalBg = new Color(rowBg.getRed(), rowBg.getGreen(), rowBg.getBlue(), bgAlpha);

        RoundedUtils.drawCustomRoundedRect(getX(), y, getWidth(), height, 0, true, true, true, true, finalBg);

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 14);
        CustomFontRenderer iconFont = FontUtils.getFont("sf", 14);

        float textY = y + (15f - titleFont.getHeight()) / 2f;
        Color textColor = RenderUtils.interpolateColorC(Theme.TEXT_MUTED, Theme.TEXT, Math.max(fraction, hoverFraction));
        titleFont.drawString(module.getLabel(), getX() + 4f, textY, RenderUtils.withAlpha(textColor, argb));

        if (!settings.isEmpty()) {
            float iconY = y + (15f - iconFont.getHeight()) / 2f;
            iconFont.drawString(opened ? "-" : "+", getX() + getWidth() - 11f, iconY,
                    RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));
        }

        if (height > 15.5f) {
            float settingsH = height - 15f;
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();

            boolean wasScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            int parentX = 0, parentY = 0, parentW = 0, parentH = 0;

            if (wasScissor) {
                SCISSOR_BUFFER.rewind();
                GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BUFFER);
                parentX = SCISSOR_BUFFER.get(0);
                parentY = SCISSOR_BUFFER.get(1);
                parentW = SCISSOR_BUFFER.get(2);
                parentH = SCISSOR_BUFFER.get(3);
            }

            int newX = (int) (getX() * scale);
            int newY = (int) (mc.displayHeight - (y + height) * scale);
            int newW = (int) (getWidth() * scale);
            int newH = (int) (settingsH * scale);

            if (wasScissor) {
                int intersectX = Math.max(parentX, newX);
                int intersectY = Math.max(parentY, newY);
                int intersectRight = Math.min(parentX + parentW, newX + newW);
                int intersectTop = Math.min(parentY + parentH, newY + newH);
                int intersectW = Math.max(0, intersectRight - intersectX);
                int intersectH = Math.max(0, intersectTop - intersectY);
                GL11.glScissor(intersectX, intersectY, intersectW, intersectH);
            } else {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(newX, newY, newW, newH);
            }

            visibleRows().forEach(row -> row.drawScreen(mouseX, mouseY, safeAlpha));

            if (wasScissor) {
                GL11.glScissor(parentX, parentY, parentW, parentH);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        if (hovered && module.getDescription() != null && !module.getDescription().isEmpty()) {
            return module.getDescription();
        }
        return null;
    }

    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= y && mouseY <= y + 15;
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        mouseClicked(mouseX, mouseY, button, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    public void mouseClicked(int mouseX, int mouseY, int button, float bodyTop, float bodyBottom) {
        if (isHeaderHovered(mouseX, mouseY) && y >= bodyTop - 1f && y + 15f <= bodyBottom + 1f) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1 && !settings.isEmpty()) {
                opened = !opened;
            }
        }
        if (opened && height > 15.5f) {
            for (PropertyRow row : visibleRows()) {
                float rowY = row.getY();
                if (rowY >= bodyTop - 1f && rowY + row.getHeight() <= bodyBottom + 1f && rowY + row.getHeight() <= y + height) {
                    row.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            for (PropertyRow row : visibleRows()) row.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            for (PropertyRow row : visibleRows()) row.keyTyped(typedChar, keyCode);
        }
    }

    public boolean isAnyTextFieldHovered() {
        for (PropertyRow row : settings) {
            if (row.isTextHovered()) return true;
        }
        return false;
    }
}