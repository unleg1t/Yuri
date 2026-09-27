package ddlc.yuri.managers.impl;

import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.Pair;
import ddlc.yuri.utils.render.RenderUtils;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public class ColorManager {

    private static final Map<ClickGUIModule.Color, Color[]> STATIC_COLORS = new EnumMap<>(ClickGUIModule.Color.class);

    public static Color getColor() {
        ClickGUIModule.Color mode = ClickGUIModule.color.getValue();
        long now = System.currentTimeMillis();

        switch (mode) {
            case RAINBOW:
                return Color.getHSBColor((now % 3000) / 3000f, 0.55f, 0.9f);

            case ASTOLFO:
                return RenderUtils.astolfoColors(15, 75);

            case NOVOLINE:
                return Color.getHSBColor((now % 3000) / 3000f, 0.25f, 0.9f);

            default:
                return getColors().getFirst();
        }
    }

    public static Pair<Color, Color> getColors() {
        ClickGUIModule.Color mode = ClickGUIModule.color.getValue();

        switch (mode) {
            case RAINBOW:
            case ASTOLFO:
            case NOVOLINE:
                Color dynamicColor = getColor();
                return Pair.of(dynamicColor, dynamicColor);

            default:
                Color[] staticColors = staticColors(mode);
                return Pair.of(staticColors[0], staticColors[1]);
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
                    second = new Color(55, 18, 90);
                    break;
                case SUNSET:
                    first = new Color(255, 104, 69);
                    second = new Color(140, 35, 140);
                    break;
                case TENACITY:
                    first = new Color(236, 133, 209);
                    second = new Color(28, 167, 222);
                    break;
                case PURPLE:
                    first = new Color(170, 70, 255);
                    second = new Color(60, 15, 110);
                    break;
                case ROYAL_BLUE:
                    first = new Color(65, 120, 255);
                    second = new Color(15, 25, 90);
                    break;
                case PASTEL_PURPLE:
                    first = new Color(205, 175, 255);
                    second = new Color(90, 60, 140);
                    break;
                case PASTEL_BLUE:
                    first = new Color(140, 200, 255);
                    second = new Color(45, 90, 160);
                    break;
                case MIDNIGHT:
                    first = new Color(80, 90, 220);
                    second = new Color(15, 18, 55);
                    break;
                case OCEAN_BLUE:
                    first = new Color(0, 190, 245);
                    second = new Color(2, 45, 95);
                    break;
                case TURQUOISE:
                    first = new Color(80, 235, 220);
                    second = new Color(15, 95, 90);
                    break;
                case PINK:
                    first = new Color(255, 105, 180);
                    second = new Color(120, 20, 70);
                    break;
                case LIME:
                    first = new Color(160, 235, 40);
                    second = new Color(45, 90, 10);
                    break;
                case FOREST_GREEN:
                    first = new Color(45, 200, 105);
                    second = new Color(10, 65, 30);
                    break;
                case GOLD:
                    first = new Color(255, 200, 50);
                    second = new Color(130, 80, 10);
                    break;
                case ORANGE:
                    first = new Color(255, 120, 30);
                    second = new Color(110, 30, 5);
                    break;
                case RED:
                    first = new Color(245, 45, 70);
                    second = new Color(85, 10, 20);
                    break;
                case ICE_BLUE:
                    first = new Color(200, 245, 255);
                    second = new Color(60, 125, 160);
                    break;
                case MONOCHROME:
                    first = new Color(220, 225, 230);
                    second = new Color(35, 38, 45);
                    break;
            }
            cached = new Color[]{first, second};
            STATIC_COLORS.put(mode, cached);
        }
        return cached;
    }
}