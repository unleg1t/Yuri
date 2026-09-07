package ddlc.yuri.api.gui.click.novoline;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ModuleEntry {

    private final Module module;
    public int yPerModule;
    public int y;
    public final CategoryTab tab;
    public boolean opened;
    public final List<PropertyEntry> settings = new CopyOnWriteArrayList<>();

    private double length = 3;
    private double anim = 5;
    public float fraction;
    private float fractionBackground;

    public ModuleEntry(Module module, CategoryTab tab) {
        this.module = module;
        this.tab = tab;
        for (Property<?> property : module.getElements()) {
            settings.add(new PropertyEntry(property, this));
        }
    }

    public String drawScreen(int mouseX, int mouseY, float tabX, float tabY, float animationProgress) {
        Minecraft mc = Minecraft.getMinecraft();
        int debugFPS = Math.max(mc.getDebugFPS(), 1);

        if (module.isEnabled() && fraction < 1) {
            fraction += 0.0025F * (2000.0F / debugFPS);
        }
        if (!module.isEnabled() && fraction > 0) {
            fraction -= 0.0025F * (2000.0F / debugFPS);
        }

        if (!module.isEnabled()) {
            if (isHovered(mouseX, mouseY) && fractionBackground < 1) {
                fractionBackground += 0.0025F * (2000.0F / debugFPS);
            }
            if (!isHovered(mouseX, mouseY) && fractionBackground > 0) {
                fractionBackground -= 0.0025F * (2000.0F / debugFPS);
            }
        }

        fraction = MathHelper.clamp_float(fraction, 0.0F, 1.0F);
        fractionBackground = MathHelper.clamp_float(fractionBackground, 0.0F, 1.0F);

        if (yPerModule < getTargetHeight()) {
            yPerModule = (int) (yPerModule + (getTargetHeight() + 9 - yPerModule) * 0.1);
        } else if (yPerModule > getTargetHeight()) {
            yPerModule = (int) (yPerModule + (getTargetHeight() - yPerModule) * 0.1);
        }

        y = (int) (tabY + 15);
        for (ModuleEntry tabModule : tab.modules) {
            if (tabModule == this) {
                break;
            }
            y += tabModule.yPerModule;
        }

        int alpha = (int) (255 * animationProgress);
        Color accent = GuiTheme.getAccent();
        Color white = new Color(255, 255, 255, alpha);

        Gui.drawRect(tabX, y, tabX + 100, y + yPerModule, RenderUtils.withAlpha(GuiTheme.MODULE_BG, alpha));
        if (!module.isEnabled() && fraction == 0) {
            Gui.drawRect(tabX, y, tabX + 100, y + 14,
                    RenderUtils.interpolateColor(
                            RenderUtils.withAlpha(GuiTheme.MODULE_BG, alpha),
                            RenderUtils.withAlpha(GuiTheme.MODULE_HOVER, alpha),
                            fractionBackground
                    ));
        } else {
            Gui.drawRect(tabX, y, tabX + 100, y + 14,
                    RenderUtils.interpolateColor(
                            RenderUtils.withAlpha(GuiTheme.MODULE_BG, alpha),
                            RenderUtils.withAlpha(accent, alpha),
                            fraction
                    ));
        }

        FontUtils.getFont("sf", 18).drawStringWithShadow(
                module.getLabel(),
                tabX + 2,
                y + 4,
                white.getRGB()
        );

        if (!settings.isEmpty()) {
            double val = debugFPS / 8.3D;
            if (opened && length > -3) {
                length -= 3 / val;
            } else if (!opened && length < 3) {
                length += 3 / val;
            }
            if (opened && anim < 8) {
                anim += 3 / val;
            } else if (!opened && anim > 5) {
                anim -= 3 / val;
            }
            RenderUtils.drawArrow(tabX + 92, y + (float) anim, 2, white.getRGB(), length);
        }

        if (opened || yPerModule != 14) {
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            List<PropertyEntry> visibleSettings = getVisibleSettings();
            if (yPerModule != getTargetHeight() && scaledResolution.getScaleFactor() != 1) {
                float clipTop = Math.max(tab.bodyTop, y);
                float clipBottom = Math.min(tab.bodyBottom, y + yPerModule);
                if (clipBottom > clipTop) {
                    drawSettingsClipped(mc, visibleSettings, tab.getPosX() - 1, clipTop, tab.getPosX() + 101, clipBottom,
                            mouseX, mouseY, animationProgress);
                }
                settings.stream()
                        .filter(setting -> !setting.property.isAvailable())
                        .forEach(setting -> setting.setPercent(0));
            } else {
                drawSettingsClipped(mc, visibleSettings, tab.getPosX() - 1, tab.bodyTop, tab.getPosX() + 101, tab.bodyBottom,
                        mouseX, mouseY, animationProgress);
            }
        } else {
            settings.forEach(setting -> setting.setPercent(0));
        }

        if (isHovered(mouseX, mouseY)) {
            ModuleInfo info = module.getClass().getAnnotation(ModuleInfo.class);
            if (info != null && !info.description().isEmpty()) {
                return info.description();
            }
        }
        return null;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            getVisibleSettings().forEach(setting -> setting.keyTyped(typedChar, keyCode));
        }
    }

    public int getTargetHeight() {
        if (!opened) {
            return 14;
        }

        int height = 17;
        for (PropertyEntry setting : getVisibleSettings()) {
            height += setting.getHeight();
        }
        return height;
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
                            tab.modules.forEach(entry -> entry.opened = false);
                        }
                        opened = !opened;
                    }
                    break;
            }
        }

        if (opened) {
            for (PropertyEntry setting : getVisibleSettings()) {
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
        y = (int) (tab.getPosY() + 15 - tab.getCurrentScrollOffset());
        for (ModuleEntry tabModule : tab.modules) {
            if (tabModule == this) {
                break;
            }
            y += tabModule.yPerModule;
        }

        if (opened) {
            return mouseX >= tab.getPosX() && mouseY >= y && mouseX <= tab.getPosX() + 101 && mouseY <= y + 14;
        }
        return mouseX >= tab.getPosX() && mouseY >= y && mouseX <= tab.getPosX() + 101 && mouseY <= y + yPerModule;
    }

    public Module getModule() {
        return module;
    }

    private List<PropertyEntry> getVisibleSettings() {
        return settings.stream()
                .filter(setting -> setting.property.isAvailable())
                .collect(Collectors.toList());
    }

    private void drawSettingsClipped(Minecraft mc, List<PropertyEntry> settingsToDraw, float clipX1, float clipY1,
                                     float clipX2, float clipY2, int mouseX, int mouseY, float animationProgress) {
        for (PropertyEntry setting : settingsToDraw) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            ScaleUtils.applyScissor(mc, clipX1, clipY1, clipX2, clipY2);
            setting.drawScreen(mouseX, mouseY, animationProgress);
        }
    }
}