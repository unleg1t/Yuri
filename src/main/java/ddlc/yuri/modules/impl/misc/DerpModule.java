package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(
        label = "Derp",
        description = "Makes you spin constantly",
        category = ModuleCategory.MISC)
public class DerpModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CLIENT);
    private final NumberProperty pitch = new NumberProperty("Pitch", 90, 0, 90, 1);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 1, 1, 5, 1);
    private final Property<Boolean> moveFix = new Property<>("Move Fix", true, () -> mode.getValue() == Mode.SERVER);

    private int yaw;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {

        setSuffix(mode.getValue().toString());

        yaw += rotationSpeed.getValue().intValue() * 10 % 360;

        if (mode.getValue() == Mode.SERVER) {
            RotationManager.setRotations(yaw, pitch.getValue().intValue(), rotationSpeed.getValue(), moveFix.getValue() ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.OFF);
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;
        if (mode.getValue() == Mode.CLIENT) {
            if (mc.gameSettings.thirdPersonView != 0) {
                RotationManager.showRotations = false;
                mc.thePlayer.rotationYawHead = mc.thePlayer.renderYawOffset = yaw;
                mc.thePlayer.renderPitchHead = pitch.getValue().intValue();
            }
        }
    }

    public enum Mode {
        CLIENT("Client"),
        SERVER("Server");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public void onDisable() {
        RotationManager.showRotations = true;
        super.onDisable();
    }
}
