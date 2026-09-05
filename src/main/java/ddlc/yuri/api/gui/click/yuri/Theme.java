package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.managers.impl.ColorManager;

import java.awt.*;

public final class Theme {

    public static final Color WINDOW_BG = new Color(0, 0, 0, 180);
    public static final Color MODULE_BG = new Color(255, 255, 255, 1);
    public static final Color MODULE_HOVER = new Color(255, 255, 255, 20);
    public static final Color MODULE_ACTIVE = new Color(255, 255, 255, 25);
    public static final Color BAR_BG = new Color(255, 255, 255, 40);
    public static final Color TEXT = new Color(255, 255, 255);
    public static final Color TEXT_MUTED = new Color(190, 190, 190);
    public static final Color TOOLTIP_BG = new Color(0, 0, 0, 40);

    private Theme() {
    }

    public static Color accent() {
        return ColorManager.getColor();
    }
}