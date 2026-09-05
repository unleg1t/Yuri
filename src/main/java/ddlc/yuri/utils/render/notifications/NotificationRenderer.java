package ddlc.yuri.utils.render.notifications;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.misc.NotificationHandler;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

public final class NotificationRenderer implements IMinecraft {

    private static final NotificationHandler notificationManager = Yuri.INSTANCE.getNotificationHandler();
    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color SUB_COLOR = new Color(190, 190, 190);
    private static final float RADIUS = 6f;

    public static void update() {
        if (!notificationManager.getNotifications().isEmpty()) {
            notificationManager.update();
        }
    }

    public static void draw() {
        ScaledResolution resolution = new ScaledResolution(mc);

        for (Notification notification : notificationManager.getNotifications()) {
            renderNotification(notification, resolution);
        }
    }

    private static void renderNotification(Notification notification, ScaledResolution resolution) {
        float padding = 12f;
        float height = 28f;

        String title = notification.getCallReason() == null ? "Notification" : notification.getCallReason();
        String message = notification.getMessage();

        CustomFontRenderer nameFont = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer bodyFont = FontUtils.getFont("sf", 14);

        if (nameFont == null || bodyFont == null) return;

        float titleWidth = nameFont.getStringWidth(title);
        float messageWidth = bodyFont.getStringWidth(message);
        float width = Math.max(130f, Math.max(titleWidth, messageWidth) + padding * 2f);

        float x = (resolution.getScaledWidth() - width) / 2f;
        float y = (float) notification.getY();

        Color accentColor = ColorManager.getColor();

        RoundedUtils.drawRoundOutline(x, y, width, height, RADIUS, -0.5f, BG_COLOR, accentColor);

        float titleX = x + (width - titleWidth) / 2f;
        float messageX = x + (width - messageWidth) / 2f;

        nameFont.drawStringWithShadow(title, titleX, y + 4f, Color.WHITE.getRGB());
        bodyFont.drawStringWithShadow(message, messageX, y + 15f, SUB_COLOR.getRGB());
    }
}