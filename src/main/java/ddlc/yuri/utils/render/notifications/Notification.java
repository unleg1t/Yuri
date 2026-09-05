package ddlc.yuri.utils.render.notifications;

import ddlc.yuri.utils.client.TimerUtils;
import lombok.Setter;
import net.minecraft.util.MathHelper;

public final class Notification {

    private final String message;

    final TimerUtils timer = new TimerUtils();
    @Setter
    boolean extending;
    boolean initialized;
    boolean entering = true;
    boolean exiting;
    public final int delay;
    private String callReason;
    @Setter
    public double x;
    @Setter
    public double y;

    public Notification(String message, int delay) {
        this.message = message;
        this.x = 0;
        this.y = 0;
        this.delay = delay;
        this.extending = false;
    }

    public Notification(String callReason, String message, int delay) {
        this.message = message;
        this.x = 0;
        this.y = 0;
        this.delay = delay;
        this.extending = false;
        this.callReason = callReason;
    }

    public String getMessage() {
        return this.message;
    }

    public TimerUtils getTimer() {
        return timer;
    }

    public int getDelay() {
        return delay;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean isExtending() {
        return extending;
    }

    public boolean isEntering() {
        return entering;
    }

    public boolean isExiting() {
        return exiting;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }


    public void setEntering(boolean entering) {
        this.entering = entering;
    }

    public void setExiting(boolean exiting) {
        this.exiting = exiting;
    }

    public double getCount() {
        return MathHelper.clamp_float(System.currentTimeMillis() - getTimer().getTime(), 0, (float) getDelay());
    }

    public String getCallReason() {
        return callReason;
    }

}