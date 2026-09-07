package ddlc.yuri.api.events.impl.render;

import ddlc.yuri.api.events.CancellableEvent;

public class RenderSkyEvent extends CancellableEvent {

    private final float partialTicks;

    public RenderSkyEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}