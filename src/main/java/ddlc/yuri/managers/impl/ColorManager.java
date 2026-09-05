package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.Pair;
import ddlc.yuri.utils.render.RenderUtils;
import lombok.Getter;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class ColorManager {

    private static final Map<ClickGUIModule.Color, Color[]> STATIC_COLORS = new EnumMap<>(ClickGUIModule.Color.class);

    private static final long UPDATE_INTERVAL_MS = 50L;

    private static ClickGUIModule.Color lastMode = null;
    private static long lastUpdate = 0L;

    @Getter
    public static Pair<Color, Color> colors = Pair.of(new Color(161, 82, 230), new Color(130, 58, 185));
    @Getter
    private static Color color = RenderUtils.interpolateColorsBackAndForth(5, 10, colors.getFirst(), colors.getSecond(), false);

    @EventHook
    public void onRender(Render2DEvent event) {
        ClickGUIModule.Color mode = ClickGUIModule.color.getValue();
        long now = System.currentTimeMillis();

        if (mode != lastMode) {
            lastMode = mode;
            lastUpdate = 0L;
            if (mode != ClickGUIModule.Color.RAINBOW
                    && mode != ClickGUIModule.Color.NOVOLINE
                    && mode != ClickGUIModule.Color.ASTOLFO) {
                Color[] staticColors = staticColors(mode);
                colors = Pair.of(staticColors[0], staticColors[1]);
            }
        }

        if (now - lastUpdate < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdate = now;

        switch (mode) {
            case RAINBOW: {
                float hue = (now % 3000) / 3000f;
                Color c = Color.getHSBColor(hue, 0.55f, 0.9f);
                color = c;
                colors = Pair.of(c, c);
                break;
            }

            case ASTOLFO: {
                Color c = RenderUtils.astolfoColors(15, 75);
                color = c;
                colors = Pair.of(c, c);
                break;
            }

            case NOVOLINE: {
                float hue = (now % 3000) / 3000f;
                Color c = Color.getHSBColor(hue, 0.25f, 0.9f);
                color = c;
                colors = Pair.of(c, c);
                break;
            }

            default: {
                Color[] staticColors = staticColors(mode);
                color = RenderUtils.interpolateColorsBackAndForth(
                        ClickGUIModule.colorSpeed.getValue().intValue(), 10,
                        staticColors[0], staticColors[1], false);
                break;
            }
        }
    }

    private static Color[] staticColors(ClickGUIModule.Color mode) {
        Color[] cached = STATIC_COLORS.get(mode);
        if (cached == null) {
            Color first;
            Color second;
            switch (mode) {
                case YURI:
                default:
                    first = new Color(161, 82, 230);
                    second = first.darker().darker();
                    break;
                case SUNSET:
                    first = new Color(161, 82, 230);
                    second = new Color(255, 104, 69);
                    break;
                case TENACITY:
                    first = new Color(236, 133, 209);
                    second = new Color(28, 167, 222);
                    break;
                case AMETHYST:
                    first = new Color(147, 61, 211);
                    second = new Color(79, 26, 122);
                    break;
                case ROYAL:
                    first = new Color(63, 81, 181);
                    second = new Color(26, 35, 126);
                    break;
                case LAVENDER:
                    first = new Color(181, 156, 214);
                    second = new Color(108, 92, 140);
                    break;
                case AZURE:
                    first = new Color(128, 128, 255);
                    second = new Color(168, 168, 255);
                    break;
                case INDIGO:
                    first = new Color(48, 63, 159);
                    second = new Color(17, 24, 84);
                    break;
                case OCEAN:
                    first = new Color(0, 150, 199);
                    second = new Color(0, 68, 105);
                    break;
                case CRYSTAL:
                    first = new Color(183, 235, 235);
                    second = new Color(94, 173, 173);
                    break;
                case PETAL:
                    first = new Color(255, 133, 191);
                    second = new Color(194, 59, 112);
                    break;
                case CITRUS:
                    first = new Color(176, 213, 41);
                    second = new Color(94, 122, 15);
                    break;
                case EVERGREEN:
                    first = new Color(34, 153, 84);
                    second = new Color(11, 74, 40);
                    break;
                case LEMON:
                    first = new Color(234, 219, 66);
                    second = new Color(145, 128, 20);
                    break;
                case EMBER:
                    first = new Color(219, 98, 33);
                    second = new Color(110, 40, 10);
                    break;
                case CRIMSON:
                    first = new Color(176, 32, 55);
                    second = new Color(79, 12, 22);
                    break;
                case ICE:
                    first = new Color(224, 247, 255);
                    second = new Color(137, 196, 214);
                    break;
                case GRAPHITE:
                    first = new Color(176, 180, 186);
                    second = new Color(68, 71, 77);
                    break;
            }
            cached = new Color[]{first, second};
            STATIC_COLORS.put(mode, cached);
        }
        return cached;
    }
}