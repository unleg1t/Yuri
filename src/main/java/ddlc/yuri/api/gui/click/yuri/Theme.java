package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.managers.impl.ColorManager;

import java.awt.*;

public final class Theme {

    public static final Color WINDOW_BG = new Color(0, 0, 0, 130);
    public static final Color MODULE_BG = new Color(0, 0, 0, 20);
    public static final Color MODULE_HOVER = new Color(255, 255, 255, 18);
    public static final Color BAR_BG = new Color(0, 0, 0, 120);
    public static final Color BAR_BORDER = new Color(255, 255, 255, 24);
    public static final Color SLIDER_TRACK = new Color(255, 255, 255, 22);
    public static final Color TEXT = new Color(240, 238, 245);
    public static final Color TEXT_MUTED = new Color(158, 152, 168);
    public static final Color TOOLTIP_BG = new Color(0, 0, 0, 140);

    private Theme() {
    }

    public static Color accent() {
        return ColorManager.getColor();
    }
}