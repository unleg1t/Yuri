package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

import java.time.LocalTime;

@ModuleInfo(label = "Ambience", category = ModuleCategory.RENDER, description = "Changes the world appearance properties")
public class AmbienceModule extends Module {

    public final Property<Boolean> realTime = new Property<Boolean>("Real World Time", false);
    public final NumberProperty time = new NumberProperty("Time", 6000.0f, 0.0f, 24000.0f, 100.0f, () -> !realTime.getValue());
    public static final Property<Boolean> clientColorFog = new Property<Boolean>("Client Color Fog", false);

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc.theWorld == null) return;

        if (!realTime.getValue()) {
            mc.theWorld.setWorldTime(time.getValue().longValue());
        } else {
            long mTime;

            final LocalTime localTime = LocalTime.now();
            final int hour = localTime.getHour();
            final int minute = localTime.getMinute();

            final long totalMinutes = hour * 60L + minute;
            long minecraftTime = (totalMinutes * 1000L / 1440L) * 24L;
            mTime = (minecraftTime + 18000L) % 24000L;

            mc.theWorld.setWorldTime(mTime);
        }
    }

    @EventHook
    public void onPacketReceive(PacketReceivedEvent event) {
        if (mc.theWorld == null) return;

        if (event.getPacket() instanceof S03PacketTimeUpdate) {
            event.setCancelled(true);
        }
    }
}
