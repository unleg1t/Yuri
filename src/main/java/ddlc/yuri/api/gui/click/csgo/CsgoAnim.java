package ddlc.yuri.api.gui.click.csgo;

import java.util.HashMap;
import java.util.Map;

public final class CsgoAnim {

    private static final Map<String, Float> VALUES = new HashMap<String, Float>();

    private CsgoAnim() {
    }

    public static float approach(String key, float target, float speed) {
        Float current = VALUES.get(key);
        float value = current == null ? target : current;
        float delta = target - value;
        float step = Math.max(0.02f, speed);
        if (Math.abs(delta) <= 0.0015f) {
            value = target;
        } else {
            value += delta * Math.min(1f, step);
        }
        VALUES.put(key, value);
        return value;
    }

    public static float get(String key, float fallback) {
        Float value = VALUES.get(key);
        return value == null ? fallback : value;
    }

    public static void set(String key, float value) {
        VALUES.put(key, value);
    }

    public static void reset() {
        VALUES.clear();
    }
}
