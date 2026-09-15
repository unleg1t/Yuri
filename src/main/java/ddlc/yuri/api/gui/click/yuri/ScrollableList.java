package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.utils.client.MathUtils;
import net.minecraft.util.MathHelper;

public class ScrollableList {

    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;

    public void scroll(float amount) {
        targetScrollOffset += amount;
    }

    public float update(float totalContentHeight, float visibleHeight) {
        float maxScroll = Math.max(0f, totalContentHeight - visibleHeight);
        targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0f, maxScroll);
        scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.25f);
        return scrollOffset;
    }

    public float getScrollOffset() {
        return scrollOffset;
    }

    public void reset() {
        scrollOffset = 0f;
        targetScrollOffset = 0f;
    }
}
