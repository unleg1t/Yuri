package ddlc.yuri.utils.misc;

import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.notifications.Notification;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationHandler {
    private final int DEFAULT_DELAY = 2_000;
    private static final double ROW_SPACING = 4.0;
    private static final double ROW_HEIGHT = 28.0;
    private static final double ANIMATION_SPEED = 12.0;
    private static final double EXIT_ANIMATION_SPEED = 10.0;

    private final List<Notification> NOTIFICATIONS = new CopyOnWriteArrayList<>();

    public void update() {
        double delta = Math.min(Math.max(RenderUtils.delta, 1), 50) / 1000.0;
        double animation = 1.0 - Math.exp(-ANIMATION_SPEED * delta);
        double exitAnimation = 1.0 - Math.exp(-EXIT_ANIMATION_SPEED * delta);

        double currentY = 2.0;

        for (int index = 0; index < NOTIFICATIONS.size(); index++) {
            Notification notification = NOTIFICATIONS.get(index);

            if (!notification.isInitialized()) {
                notification.setInitialized(true);
                notification.setY(-ROW_HEIGHT - 10.0);
                notification.getTimer().reset();
            }

            if (notification.isExiting()) {
                double targetY = -ROW_HEIGHT - 10.0;
                notification.setY(approach(notification.getY(), targetY, exitAnimation));

                if (notification.getY() <= -ROW_HEIGHT - 5.0) {
                    remove(notification);
                }
                continue;
            }

            double targetY = currentY;
            currentY += ROW_HEIGHT + ROW_SPACING;

            notification.setY(approach(notification.getY(), targetY, animation));

            if (notification.isEntering() && Math.abs(notification.getY() - targetY) < 0.5) {
                notification.setEntering(false);
                notification.getTimer().reset();
            }

            if (!notification.isEntering() && notification.getTimer().hasTimeElapsed(notification.getDelay())) {
                notification.setExiting(true);
            }
        }
    }

    private static double approach(double current, double target, double amount) {
        return current + (target - current) * amount;
    }

    public void pop(@NonNull String message, int delay) {
        Notification notification = new Notification(message, delay);

        for (Notification prevNotification : NOTIFICATIONS) {
            if (notification.getMessage().equalsIgnoreCase(prevNotification.getMessage())) {
                prevNotification.getTimer().reset();
                prevNotification.setExiting(false);
                return;
            }
        }

        notification.setExtending(true);
        notification.getTimer().reset();

        add(notification);
    }

    public void pop(@NonNull String callReason, @NonNull String message, int delay) {
        Notification notification = new Notification(callReason, message, delay);

        notification.setExtending(true);
        notification.getTimer().reset();

        add(notification);
    }

    public void pop(@NonNull String message) {
        pop(message, DEFAULT_DELAY);
    }

    public void pop(@NonNull String callReason, @NonNull String message) {
        pop(callReason, message, DEFAULT_DELAY);
    }

    public void add(@NonNull Notification notification) {
        notification.setExtending(true);
        notification.getTimer().reset();

        NOTIFICATIONS.add(notification);
    }

    public void remove(@Nullable Notification notification) {
        NOTIFICATIONS.remove(notification);
    }

    public List<Notification> getNotifications() {
        return NOTIFICATIONS;
    }
}