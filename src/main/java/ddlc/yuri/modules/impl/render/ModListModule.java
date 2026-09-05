package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.misc.Translate;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleInfo(label = "Mod List", category = ModuleCategory.RENDER, description = "Shows the enabled mods on your HUD")
public class ModListModule extends Module implements IMinecraft {

    private final Property<Boolean> bg = new Property<>("Background", true);
    public NumberProperty arrayBg = new NumberProperty("Background Opacity", 130, 0, 255, 1, bg::getValue);
    private final Property<Boolean> outline = new Property<>("Outline", false);
    private final Property<Boolean> line = new Property<>("Line", true, () -> !outline.getValue());
    public static final Property<Boolean> hideVisuals = new Property<>("Hide Visuals", false);
    public static final Property<Boolean> hideMisc = new Property<>("Hide Misc", false);
    private static final Property<Boolean> useCustomFont = new Property<>("Use Custom Font", true);
    private static final Property<Boolean> hideSuffix = new Property<>("Hide Suffix", false);
    private final ModeProperty<SuffixMode> suffixMode = new ModeProperty<>("Suffix Mode", SuffixMode.SPACE, () -> !hideSuffix.getValue());
    private final Property<Boolean> noSpaces = new Property<>("No Spaces", false);
    private final Property<Boolean> lowercase = new Property<>("Lowercase", false);
    private final Property<Boolean> bold = new Property<>("Bold", false);
    private final ModeProperty<ColorMode> colorMode = new ModeProperty<>("Color Mode", ColorMode.FADE);
    public static final NumberProperty padding = new NumberProperty("Padding", 2, 0, 6, 0.5);
    public static NumberProperty offset = new NumberProperty("Offset", 0, 0, 30, 1);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.0, 0.5, 1.0, 0.1);

    private static final float TEXT_HEIGHT = 8f;

    private static final int[][] MTF_COLORS = {
            {91, 206, 250},
            {245, 169, 184},
            {255, 255, 255}
    };

    private static final Map<Module, String> displayLabelCache = new HashMap<>();
    public static List<Module> moduleCache;

    private final Set<Module> seededModules = new HashSet<>();
    private final Map<Module, Boolean> previousVisibility = new HashMap<>();

    public enum SuffixMode {
        SPACE("Space"), DASH("Dash"), BRACKETS("Brackets"), PIPE("Pipe");
        public final String name;

        SuffixMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ColorMode {
        STATIC("Static"),
        FADE("Fade"),
        MTF("MTF");

        public final String name;

        ColorMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (moduleCache == null) {
            return;
        }

        CustomFontRenderer fr = getActiveFont();
        if (fr == null) {
            return;
        }

        for (Module module : moduleCache) {
            displayLabelCache.put(module, getDisplayLabel(module));
        }

        moduleCache.sort(new LengthComparator());
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onRender2D(Render2DEvent event) {
        renderArrayList();
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent event) {
        renderArrayList();
    }

    private CustomFontRenderer getActiveFont() {
        return FontUtils.getFont(bold.getValue() ? "sf-bold" : "sf", 18);
    }

    private boolean isMcFontActive() {
        return !useCustomFont.getValue();
    }

    private int getTextWidth(CustomFontRenderer fr, String text) {
        if (text == null) {
            return 0;
        }

        if (isMcFontActive()) {
            return mc.fontRendererObj.getStringWidth(text);
        }

        return fr == null ? 0 : fr.getStringWidth(text);
    }

    private float drawText(CustomFontRenderer fr, String text, float x, float y, int color) {
        if (text == null) {
            return x;
        }

        if (isMcFontActive()) {
            return mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }

        return fr.drawStringWithShadow(text, x, y, color);
    }

    private boolean shouldSkip(Module module) {
        return (hideVisuals.getValue() && module.getCategory() == ModuleCategory.RENDER)
                || (hideMisc.getValue() && module.getCategory() == ModuleCategory.MISC)
                || module instanceof ClickGUIModule
                || module instanceof WatermarkModule
                || module instanceof ModListModule;
    }

    private void renderArrayList() {
        CustomFontRenderer fr = getActiveFont();
        ScaledResolution sr = new ScaledResolution(mc);

        if (fr == null) {
            return;
        }

        if (moduleCache == null) {
            moduleCache = new ArrayList<>(Yuri.INSTANCE.getModuleManager().getModules());
        }

        float pad = padding.getValue().floatValue();
        float lw = lineWidth.getValue().floatValue();
        float off = offset.getValue().floatValue();
        float screenX = sr.getScaledWidth() - off;
        float screenRight = sr.getScaledWidth();
        float rowStep = TEXT_HEIGHT + (pad * 2);

        List<Module> filteredModules = new ArrayList<>();
        for (Module module : moduleCache) {
            if (shouldSkip(module)) {
                continue;
            }
            filteredModules.add(module);
        }

        updatePositions(filteredModules, fr, screenX, screenRight, off, pad, rowStep);

        final int moduleCacheSize = filteredModules.size();
        int lastVisibleModuleIndex = moduleCacheSize - 1;

        for (; lastVisibleModuleIndex > 0; lastVisibleModuleIndex--) {
            if (filteredModules.get(lastVisibleModuleIndex).isVisible()) {
                break;
            }
        }

        int firstVisibleModuleIndex = -1;
        int visibleModuleCount = 0;
        float previousModuleWidth = -1;

        for (int i = 0; i < moduleCacheSize; i++) {
            final Module module = filteredModules.get(i);
            final Translate translate = module.getTranslate();
            final String name = displayLabelCache.get(module);
            final float moduleWidth = getTextWidth(fr, name);
            final boolean visible = module.isVisible();

            if (visible && firstVisibleModuleIndex == -1) {
                firstVisibleModuleIndex = i;
            }

            double translateX = translate.getX();
            double translateY = translate.getY();

            if (visible || translateX < screenX) {
                int aColor = getColorForModule(visibleModuleCount);

                if (bg.getValue()) {
                    float bgLeft = (!line.getValue() && !outline.getValue()) ? (float) translateX - pad : (float) translateX - pad - lw;
                    float bgRight = (off > 0 && outline.getValue()) ? screenX + lw : screenX;
                    Gui.drawRect(bgLeft, (float) translateY - pad, bgRight, (float) translateY + TEXT_HEIGHT + pad, getColorForBG().getRGB());
                }

                float textX = line.getValue() && !outline.getValue() ? (float) (translateX - 1.0f - lw) : (float) ((float) offset.getValue().floatValue() == 0 ? translateX - 0.5f : (float) translateX);
                if (pad > 0) {
                    textX -= pad / getTextWidth(fr, name);
                }

                drawText(fr, name, useCustomFont.getValue() ? offset.getValue().intValue() > 0 ? textX - 1.0f : textX : textX + 0.8f, (float) translateY, aColor);

                if (outline.getValue()) {
                    Gui.drawRect((float) translateX - pad - lw, (float) translateY - pad, (float) translateX - pad, (float) translateY + TEXT_HEIGHT + pad, aColor);

                    double outlineTop = translateY - pad - lw;
                    double outlineBottom = translateY + TEXT_HEIGHT + pad;
                    float rightEdge = (off > 0) ? screenX + lw : screenX;

                    if (i != firstVisibleModuleIndex) {
                        Module prevModule = null;
                        for (int j = i - 1; j >= 0; j--) {
                            if (filteredModules.get(j).isVisible()) {
                                prevModule = filteredModules.get(j);
                                break;
                            }
                        }
                        if (prevModule != null) {
                            String prevModuleName = displayLabelCache.get(prevModule);
                            float prevModuleWidth = getTextWidth(fr, prevModuleName);
                            if (moduleWidth - prevModuleWidth > 0.5f) {
                                float prevLeftOutline = (float) prevModule.getTranslate().getX() - pad - lw;
                                Gui.drawRect((float) translateX - pad - lw, (float) outlineTop, prevLeftOutline, (float) outlineTop + lw, aColor);
                            }
                        }
                    } else {
                        Gui.drawRect((float) translateX - pad - lw, (float) outlineTop, rightEdge, (float) outlineTop + lw, aColor);
                    }

                    if (i != lastVisibleModuleIndex) {
                        Module nextModule = null;
                        for (int j = i + 1; j <= lastVisibleModuleIndex; j++) {
                            if (filteredModules.get(j).isVisible()) {
                                nextModule = filteredModules.get(j);
                                break;
                            }
                        }

                        if (nextModule != null) {
                            String nextModuleName = displayLabelCache.get(nextModule);
                            float nextModuleWidth = getTextWidth(fr, nextModuleName);

                            if (moduleWidth - nextModuleWidth > 0.5f) {
                                float nextLeftOutline = (float) nextModule.getTranslate().getX() - pad - lw;
                                Gui.drawRect((float) translateX - pad - lw, (float) outlineBottom, nextLeftOutline, (float) outlineBottom + lw, aColor);
                            }
                        }
                    } else {
                        Gui.drawRect((float) translateX - pad - lw, (float) outlineBottom, rightEdge, (float) outlineBottom + lw, aColor);
                    }

                    if (off > 0) {
                        Gui.drawRect(screenX, (float) translateY - pad, screenX + lw, (float) translateY + TEXT_HEIGHT + pad, aColor);
                    }
                }

                if (line.getValue() && !outline.getValue()) {
                    if (i == firstVisibleModuleIndex) {
                        Gui.drawRect(screenX - lw, off - pad, screenX, (float) translateY + TEXT_HEIGHT + pad, aColor);
                    } else {
                        Module prevModule = null;
                        for (int j = i - 1; j >= 0; j--) {
                            if (filteredModules.get(j).isVisible()) {
                                prevModule = filteredModules.get(j);
                                break;
                            }
                        }
                        if (prevModule != null) {
                            double prevY = prevModule.getTranslate().getY();
                            Gui.drawRect(screenX - lw, (float) prevY + TEXT_HEIGHT + pad, screenX, (float) translateY + TEXT_HEIGHT + pad, aColor);
                        }
                    }
                }

                previousModuleWidth = moduleWidth;
                visibleModuleCount++;
            }

            previousVisibility.put(module, visible);
        }
    }

    private void updatePositions(List<Module> filteredModules, CustomFontRenderer fr, float screenX, float screenRight, float startY, float pad, float rowStep) {
        float y = startY;

        for (Module module : filteredModules) {
            Translate translate = module.getTranslate();
            String name = displayLabelCache.get(module);
            float moduleWidth = getTextWidth(fr, name);
            float visibleTargetX = screenX - moduleWidth - pad;
            float hiddenTargetX = screenRight + moduleWidth + pad + 4f;

            if (!seededModules.contains(module)) {
                translate.setX(module.isVisible() ? visibleTargetX : hiddenTargetX);
                translate.setY(y);
                seededModules.add(module);
            } else if (module.isVisible()) {
                translate.animate(visibleTargetX, y);
            } else {
                translate.animate(hiddenTargetX, y);
            }

            if (module.isVisible()) {
                y += rowStep;
            }
        }
    }

    private Color getColorForBG() {
        return new Color(0, 0, 0, arrayBg.getValue().intValue());
    }

    private int getColorForModule(int visibleModuleIndex) {
        if (colorMode.getValue() == ColorMode.MTF) {
            int[] c = MTF_COLORS[visibleModuleIndex % MTF_COLORS.length];
            return new Color(c[0], c[1], c[2]).getRGB();
        }

        int index = colorMode.getValue() == ColorMode.FADE ? visibleModuleIndex : 0;

        if (ClickGUIModule.color.getValue() == ClickGUIModule.Color.ASTOLFO) {
            return RenderUtils.astolfoColors(0, index * 10).getRGB();
        }

        if (ClickGUIModule.color.getValue() == ClickGUIModule.Color.RAINBOW) {
            float hue = (System.currentTimeMillis() % 3000) / 3000f;
            if (colorMode.getValue() == ColorMode.FADE) {
                hue += index * 0.035f;
            }
            if (hue > 1.0f) {
                hue %= 1.0f;
            }
            return Color.getHSBColor(hue, 0.55f, 0.9f).getRGB();
        }

        if (ClickGUIModule.color.getValue() == ClickGUIModule.Color.NOVOLINE) {
            float hue = (System.currentTimeMillis() % 3000) / 3000f;
            if (colorMode.getValue() == ColorMode.FADE) {
                hue += index * 0.035f;
            }
            if (hue > 1.0f) {
                hue %= 1.0f;
            }
            return Color.getHSBColor(hue, 0.25f, 0.9f).getRGB();
        }

        return RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), index * 10, ColorManager.colors.getFirst(), ColorManager.colors.getSecond(), false).getRGB();
    }

    private String getDisplayLabel(Module m) {
        String label = m.getLabel();
        String suffix = m.getSuffix();

        if (noSpaces.getValue()) {
            label = label.replace(" ", "");
            if (suffix != null) {
                suffix = suffix.replace(" ", "");
            }
        }

        if (lowercase.getValue()) {
            label = label.toLowerCase();
            if (suffix != null) {
                suffix = suffix.toLowerCase();
            }
        }

        if (hideSuffix.getValue()) {
            suffix = null;
        }

        if (suffix != null) {
            return label + "\2477" + getFormattedSuffixString(suffix);
        }
        return label;
    }

    private String getFormattedSuffixString(String rawSuffix) {
        switch (suffixMode.getValue()) {
            case SPACE:
                return " " + rawSuffix;
            case DASH:
                return " - " + rawSuffix;
            case BRACKETS:
                return " [" + rawSuffix + "]";
            case PIPE:
                return " | " + rawSuffix;
            default:
                return rawSuffix;
        }
    }

    private class LengthComparator implements Comparator<Module> {
        @Override
        public int compare(Module o1, Module o2) {
            CustomFontRenderer fr = getActiveFont();
            return Float.compare(
                    getTextWidth(fr, displayLabelCache.get(o2)),
                    getTextWidth(fr, displayLabelCache.get(o1)));
        }
    }
}