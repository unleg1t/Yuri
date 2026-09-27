package ddlc.yuri.utils.render.progress;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;

import java.awt.*;

public class ProgressBarUtils {
    private static final Color COLOR = new Color(0, 0, 0, 130);

    public static void draw(float progress, float alpha, float centerX, float centerY, float width, float thickness) {
        float percentage = Math.min(1.0f, Math.max(0.0f, progress));
        float half = width / 2f;
        float radius = thickness / 2f;

        Color trackColor = RenderUtils.applyOpacity(COLOR, alpha);
        Color fillColor = RenderUtils.applyOpacity(ColorManager.getColor(), alpha);

        float fillWidth = width * percentage;
        if (fillWidth <= 0f) return;

        float fillRadius = Math.min(radius, fillWidth / 2f);

        RoundedUtils.drawRoundOutline(centerX - half, centerY, width, 4f, radius, -0.4f, trackColor, fillColor.darker().darker());
        RoundedUtils.drawRoundedRect(centerX - half + 0.5f, centerY + 1, fillWidth - 2f, 4f - 2.5f, fillRadius, fillColor);
    }

}
