package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class YuriModuleEntry {

    private final Module module;
    public float yPerModule;
    public float y;
    public final YuriPanel panel;
    public boolean opened;
    public final List<YuriPropertyEntry> settings = new CopyOnWriteArrayList<>();

    public float fraction;
    private float hoverFraction;
    private float expandProgress;

    public YuriModuleEntry(Module module, YuriPanel panel) {
        this.module = module;
        this.panel = panel;
        for (Property<?> property : module.getElements()) {
            settings.add(new YuriPropertyEntry(property, this));
        }
    }

    public void resetAnimations() {
        fraction = module.isEnabled() ? 1f : 0f;
        hoverFraction = 0f;
        expandProgress = opened ? 1f : 0f;
        yPerModule = getTargetHeight();
        settings.forEach(setting -> setting.setPercent(0));
    }

    public String drawScreen(int mouseX, int mouseY, float panelX, float panelY, float animationProgress) {
        Minecraft mc = Minecraft.getMinecraft();
        int debugFPS = Math.max(mc.getDebugFPS(), 1);
        float speed = 0.0028F * (2000.0F / debugFPS);

        if (module.isEnabled() && fraction < 1) {
            fraction += speed;
        } else if (!module.isEnabled() && fraction > 0) {
            fraction -= speed;
        }

        boolean hovered = isHovered(mouseX, mouseY);
        if (hovered && hoverFraction < 1) {
            hoverFraction += speed;
        } else if (!hovered && hoverFraction > 0) {
            hoverFraction -= speed;
        }

        fraction = MathHelper.clamp_float(fraction, 0.0F, 1.0F);
        hoverFraction = MathHelper.clamp_float(hoverFraction, 0.0F, 1.0F);

        float expandTarget = opened ? 1.0F : 0.0F;
        expandProgress = MathUtils.lerp(expandProgress, expandTarget, 0.18F);
        if (Math.abs(expandProgress - expandTarget) < 0.01F) {
            expandProgress = expandTarget;
        }

        float targetHeight = getTargetHeight();
        yPerModule = MathUtils.lerp(yPerModule, targetHeight, 0.22F);
        if (Math.abs(yPerModule - targetHeight) < 0.4F) {
            yPerModule = targetHeight;
        }

        y = panelY + YuriTheme.HEADER_HEIGHT;
        for (YuriModuleEntry entry : panel.modules) {
            if (entry == this) {
                break;
            }
            y += entry.yPerModule;
        }

        float rowX = panelX + 4f;
        float rowWidth = YuriTheme.PANEL_WIDTH - 8f;
        if (hoverFraction > 0.02f) {
            RoundedUtils.drawRoundedRect(rowX, y + 1f, rowWidth, YuriTheme.MODULE_HEIGHT - 2f, 3f,
                    YuriTheme.fade(YuriTheme.HOVER, animationProgress * hoverFraction));
        }

        Color nameColor = new Color(RenderUtils.interpolateColor(
                YuriTheme.fadeSolid(YuriTheme.TEXT_SECONDARY, animationProgress),
                YuriTheme.fadeSolid(YuriTheme.accent(), animationProgress),
                fraction
        ), true);
        FontUtils.getFont("sf", 13).drawStringWithShadow(module.getLabel(), rowX + 5f, y + 4.5f, nameColor.getRGB());

        if (!settings.isEmpty()) {
            FontUtils.getFont("sf", 11).drawStringWithShadow(
                    opened ? "-" : "+",
                    panelX + YuriTheme.PANEL_WIDTH - 13f,
                    y + 5f,
                    YuriTheme.text(YuriTheme.TEXT_MUTED, animationProgress)
            );
        }

        if (opened || yPerModule > YuriTheme.MODULE_HEIGHT + 0.5f) {
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            List<YuriPropertyEntry> visibleSettings = getVisibleSettings();
            if (yPerModule != getTargetHeight() && scaledResolution.getScaleFactor() != 1) {
                float clipTop = Math.max(panel.bodyTop, y);
                float clipBottom = Math.min(panel.bodyBottom, y + yPerModule);
                if (clipBottom > clipTop) {
                    drawSettingsClipped(mc, visibleSettings, panel.getPosX(), clipTop, panel.getPosX() + YuriTheme.PANEL_WIDTH, clipBottom,
                            mouseX, mouseY, animationProgress);
                }
                settings.stream()
                        .filter(setting -> !setting.property.isAvailable())
                        .forEach(setting -> setting.setPercent(0));
            } else {
                drawSettingsClipped(mc, visibleSettings, panel.getPosX(), panel.bodyTop, panel.getPosX() + YuriTheme.PANEL_WIDTH, panel.bodyBottom,
                        mouseX, mouseY, animationProgress);
            }
        } else {
            settings.forEach(setting -> setting.setPercent(0));
        }

        if (hovered && module.getDescription() != null && !module.getDescription().isEmpty()) {
            return module.getDescription();
        }
        return null;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            getVisibleSettings().forEach(setting -> setting.keyTyped(typedChar, keyCode));
        }
    }

    public float getTargetHeight() {
        if (expandProgress <= 0.01f) {
            return YuriTheme.MODULE_HEIGHT;
        }

        float settingsHeight = 0;
        for (YuriPropertyEntry setting : getVisibleSettings()) {
            settingsHeight += setting.getHeight();
        }
        return YuriTheme.MODULE_HEIGHT + settingsHeight * expandProgress;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY)) {
            switch (mouseButton) {
                case 0:
                    module.toggle();
                    break;
                case 1:
                    if (!module.getElements().isEmpty()) {
                        if (!opened && Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).getClosePrevious().getValue()) {
                            panel.modules.forEach(entry -> entry.opened = false);
                        }
                        opened = !opened;
                    }
                    break;
            }
        }

        if (opened) {
            for (YuriPropertyEntry setting : getVisibleSettings()) {
                setting.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            getVisibleSettings().forEach(setting -> setting.mouseReleased(mouseX, mouseY, state));
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        y = panel.getPosY() + YuriTheme.HEADER_HEIGHT - panel.getCurrentScrollOffset();
        for (YuriModuleEntry entry : panel.modules) {
            if (entry == this) {
                break;
            }
            y += entry.yPerModule;
        }

        return mouseX >= panel.getPosX() && mouseY >= y && mouseX <= panel.getPosX() + YuriTheme.PANEL_WIDTH && mouseY <= y + YuriTheme.MODULE_HEIGHT;
    }

    public Module getModule() {
        return module;
    }

    public boolean isTyping() {
        for (YuriPropertyEntry setting : settings) {
            if (setting.isTextHovered()) {
                return true;
            }
        }
        return false;
    }

    private List<YuriPropertyEntry> getVisibleSettings() {
        return settings.stream()
                .filter(setting -> setting.property.isAvailable())
                .collect(Collectors.toList());
    }

    private void drawSettingsClipped(Minecraft mc, List<YuriPropertyEntry> settingsToDraw, float clipX1, float clipY1,
                                     float clipX2, float clipY2, int mouseX, int mouseY, float animationProgress) {
        for (YuriPropertyEntry setting : settingsToDraw) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            ScaleUtils.applyScissor(mc, clipX1, clipY1, clipX2, clipY2);
            setting.drawScreen(mouseX, mouseY, animationProgress);
        }
    }
}
