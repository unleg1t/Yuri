package ddlc.yuri.api.gui.click.sigma;

public final class SigmaEasing {

    private SigmaEasing() {
    }

    public static float easeOutBack(float progress, float start, float change, float duration) {
        float s = 1.70158F;
        float ratio = progress / duration - 1.0F;
        return change * (ratio * ratio * ((s + 1.0F) * ratio + s) + 1.0F) + start;
    }

    public static float easeOutQuad(float progress, float start, float change, float duration) {
        float ratio = progress / duration;
        return -change * ratio * (ratio - 2.0F) + start;
    }

    public static float backwardTransition(float progress, float start, float change, float duration) {
        float ratio = progress / duration;
        return change * ratio * ratio * ratio + start;
    }

    public static float jelly(float progress, float period) {
        if (progress <= 0f) {
            return 0f;
        }
        if (progress >= 1f) {
            return 1f;
        }
        return (float) (Math.pow(2.0, -10.0 * progress)
                * Math.sin((progress - period / 4.0) * (Math.PI * 2.0) / period) + 1.0);
    }
}
